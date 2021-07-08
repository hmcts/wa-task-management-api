#!/usr/bin/env bash

#setting encoding for Python 2 / 3 compatibilities
echo "${TEST_URL}"
echo "${Rules}"
zap-api-scan.py -t "${TEST_URL}"/v2/api-docs -f openapi -S -d -u "${Rules}" -P 1001 -l FAIL
curl --fail http://0.0.0.0:1001/OTHER/core/other/jsonreport/?formMethod=GET --output report.json
echo "ZAP has successfully started"
#setting encoding for Python 2 / 3 compatibilities
export LC_ALL=C.UTF-8
export LANG=C.UTF-8
zap-cli --zap-url http://0.0.0.0 -p 1001 report -o api-report.html -f html
zap-cli --zap-url http://0.0.0.0 -p 1001 alerts -l Informational --exit-code False
mkdir -p functional-output

cp api-report.html functional-output
cp *.* functional-output/
chmod -R 777 functional-output
