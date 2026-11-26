#!/usr/bin/env bash

set -euo pipefail
# Jenkins may enable tracing; authentication material must never be traced.
set +x
umask 077

: "${CAMUNDA_URL:?Set CAMUNDA_URL to the preview Camunda /engine-rest URL}"
: "${S2S_URL:?Set S2S_URL to the service authorisation provider}"
: "${S2S_SECRET_WORKFLOW_API:?Set S2S_SECRET_WORKFLOW_API for wa_workflow_api}"

dmn_branch=${1:-master}
bpmn_branch=${2:-master}
camunda_url=${CAMUNDA_URL%/}
deployment_dir=$(mktemp -d)
trap 'rm -rf "$deployment_dir"' EXIT

git clone --quiet --depth 1 --single-branch --branch "$dmn_branch" \
  https://github.com/hmcts/wa-task-configuration-template.git "$deployment_dir/dmn"
git clone --quiet --depth 1 --single-branch --branch "$bpmn_branch" \
  https://github.com/hmcts/wa-standalone-task-bpmn.git "$deployment_dir/bpmn"

# Python 3's standard library generates the same six-digit TOTP used by S2S clients.
python3 - <<'PY' > "$deployment_dir/lease.json"
import base64
import hashlib
import hmac
import json
import os
import struct
import time

secret = os.environ["S2S_SECRET_WORKFLOW_API"].strip()
key = base64.b32decode(secret + "=" * (-len(secret) % 8), casefold=True)
counter = struct.pack(">Q", int(time.time()) // 30)
digest = hmac.new(key, counter, hashlib.sha1).digest()
offset = digest[-1] & 15
code = (struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7fffffff) % 1000000
print(json.dumps({"microservice": "wa_workflow_api", "oneTimePassword": f"{code:06d}"}))
PY

service_token=$(curl --fail --silent --show-error --connect-timeout 10 --max-time 30 \
  --request POST "${S2S_URL%/}/lease" \
  --header 'Content-Type: application/json' \
  --data-binary "@$deployment_dir/lease.json")
if [[ -z "$service_token" ]]; then
  echo 'S2S returned an empty service token' >&2
  exit 1
fi
printf 'ServiceAuthorization: Bearer %s\n' "$service_token" > "$deployment_dir/headers"
unset service_token

ready=false
for ((attempt = 1; attempt <= 30; attempt++)); do
  if curl --fail --silent --show-error --connect-timeout 5 --max-time 5 \
    --header "@$deployment_dir/headers" "$camunda_url/version" --output /dev/null; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  echo 'Preview Camunda did not become ready after 30 attempts' >&2
  exit 1
fi

deploy_definitions() {
  local resource_dir=$1
  local extension=$2
  local count=0
  local definition
  local deployment_arguments=(--form 'deploy-changed-only=true')
  if [[ "$extension" == dmn ]]; then
    deployment_arguments+=(--form 'tenant-id=wa' --form 'deployment-source=wa')
  fi

  while IFS= read -r -d '' definition; do
    echo "Deploying $(basename "$definition") to preview Camunda"
    curl --fail --silent --show-error --connect-timeout 10 --max-time 60 \
      --request POST "$camunda_url/deployment/create" \
      --header "@$deployment_dir/headers" \
      --form "deployment-name=$(basename "$definition")" \
      "${deployment_arguments[@]}" \
      --form "file=@$definition" --output /dev/null
    count=$((count + 1))
  done < <(find "$resource_dir" -type f -name "*.$extension" -print0)

  if [[ "$count" -eq 0 ]]; then
    echo "No $extension definitions found in $resource_dir" >&2
    exit 1
  fi
}

deploy_definitions "$deployment_dir/dmn/src/main/resources" dmn
deploy_definitions "$deployment_dir/bpmn/src/main/resources" bpmn
