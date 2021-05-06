#!/bin/bash
## Usage: ./camunda-deployment [SERVICE_TOKEN]
##
## Options:
##    - SERVICE_TOKEN: a service token for a whitelisted service in camunda which is generated with the idam-service-token.
##

microservice="${1:-wa_task_configuration_api}"

serviceToken=$(curl --silent --show-error -X POST \
  -H "Content-Type: application/json" \
  -d '{"microservice":"'${microservice}'"}' \
  ${S2S_URL}/testing-support/lease
)

echo $serviceToken

# GET TASKS
response=$(curl --request POST \
  --url http://camunda-api-aat.service.core-compute-aat.internal/engine-rest/task \
  --header 'Content-Type: application/json' \
  --cookie dtCookie=v_4_srv_2_sn_708A526309E3A599F638913244C50A40_perc_100000_ol_0_mul_1_app-3Aea7c4b59f27d43eb_1_rcs-3Acss_0 \
  --data '{
    "orQueries": [
        {
            "processVariables": [
                {
                    "name": "taskState",
                    "operator": "eq",
                    "value": "unconfigured"
                }
            ]
        }
    ],
    "taskDefinitionKey": "processTask",
    "processDefinitionKey": "wa-task-initiation-ia-asylum"
}')

# CREATE FILE WITH TASKS WITH NUMBER OF TASKS
tmpfile=$(mktemp /tmp/tasks.json)
echo ${response} > /tmp/tasks.json

# LOOP OVER ALL IDS
for ID in $(seq 0 $(jq length /tmp/tasks.json)); do
ID_RESPONSE=$(cat /tmp/tasks.json | jq -r ".[${ID}].id")
response=$(curl --request POST \
  --url http://wa-task-configuration-api-aat.service.core-compute-aat.internal/task/${ID_RESPONSE} \
  --header 'Authorization: Basic Og==' \
  --header 'Content-Type: application/json' \
  --header "ServiceAuthorization: Bearer ${serviceToken}" \
  --cookie dtCookie=v_4_srv_2_sn_708A526309E3A599F638913244C50A40_perc_100000_ol_0_mul_1_app-3Aea7c4b59f27d43eb_1_rcs-3Acss_0
)
done




# DELETE FILE FROM TEMP AFTER
rm "$tmpfile"

