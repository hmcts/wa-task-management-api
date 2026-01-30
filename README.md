# Task Management API

[![GitHub branch status](https://img.shields.io/github/checks-status/hmcts/wa-task-management-api/master?label=Build%20Status)](https://github.com/hmcts/wa-task-management-api)
![Codecov](https://img.shields.io/codecov/c/github/hmcts/wa-task-management-api)

[![License: MIT](https://img.shields.io/github/license/hmcts/wa-task-management-api)](https://opensource.org/licenses/MIT)

Last reviewed on: **15/04/2025**

#### What does this app do?

Provides API endpoints that enable clients to create and manage Tasks in the CFT Task Database.

<!--
      Sequence Diagram Source:
    http://www.plantuml.com/plantuml/uml/lPLHQzim4CUVSv_Yo9vDdbT8QDfiOz2nGFi5BjB55hPEddHQLag_-vAORKxcCTgaqQF-Vtx7_MJmTLAAcdq3bRECwn1q5Nu0fDf9Iv4yfefis3WfKMFcWuI_PHR3-0IZUOnXSnkQ4771tDxNZpTDvkszqH1lGhGSkzHIh5VxwCwj-Su9_oSbKvSnPB5Tuzty_U9syH56C5fIL6TSIw9zHdz-ltDCsbHE6Pu1r6d3-42fnYyaLB9dKPmlFmDGra16tNTInY3G_i7Xs3IEHGjgs_5Xe5jKuJjKrt173KC-YwLYrVgYRKZN8Ven04odQ1fo7gTJWFw0OZP8nIjSVhr_kCB9BfqyEViysGC0Xll5R7XuZyHIEk4YSjA_gBWzaJL7mPIDuPJ-EQYO7Hh2xR7yM-Qmf9s0B2ShoF8s4h9SJeC9lcrO-HGFlnZd5T47Ny84fUHAESL3HpYxXGvx4GU9APeALu_ex0jCVFfm8E8r1Zh4e83-vhlBDrjxuxJ2_K-7bMDwtQTmnkS_NlpC3txDOhDlVW80
    See: https://plantuml.com/ docs for reference
-->

![task-management](task-management-api.png)

#### Access Management Process

All requests to Task Management will check the requester has the appropriate permissions to access the Task resource they are interacting with.
If the required permissions are not present then the request will not be fulfilled.

<!--
    Sequence Diagram Source:
    http://www.plantuml.com/plantuml/uml/XPBDRjGm4CVFdQUmojaF05ANfhlggKWL8EuJPpPhnVQOyQGBGhmxJWmgsHQHGmxp-t_CIBujYg9p373o0vaZi_Ry3Q1CFcKKZAQSSE2pJwDHcMb3wEjCoP7v0LUT29_t75ZCWIX_chxVXPdgt2dB7Sj0qkY0ClKhUl17Ul29_aFHJQFmd8QcUDEzFUmFzt05LuyewftFcBHblEpVQ2wIpYUl13y1r6iWyndBP3vWmf4Y9JNMTCvHAMssZ01mLaQd_WcL32V8p-dcsWLVHHPplZPOZ0jRh3NVnfRVT7xLQTpgC5hXG1PByMDQfCKMaYVlQDNZXTvXh2UXxNEqEQ0UMM9Re2h11RlJQDwPJBAGvShEgUS4eD7kS64ZwSTQsJqtWf3EaeninytZ_fYMi2ye7lj01KpzVjjq0nPJM-A4PSyYvFIH9FeQpAcy1y2WhxERc_RegjlkZCFNB_ch6TV9dcCEfBHMruL3jLqI2eN-LNh42b-QxrbwFmoKgZiDS5j_Smo_0000
    See: https://plantuml.com/ docs for reference
-->

![task-management](access-management-process.png)

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

To perform appropriate checks with the build:

```bash
  ./gradlew check
```

### Running the application

#### Prerequisites:
- The Task Management minikube local environment will be required to run the application fully.
- If minikube is running follow the README in https://github.com/hmcts/wa-kube-environment
- If you are running minikube with Apple silicone with ARM Architecture e.g. m1-4 make sure to set the environment with the below command
   ```
   export environment=local-arm-arch
   ```
- Check if minikube IP is set as environment variable.
    ```
    echo $OPEN_ID_IDAM_URL
    ```
  You should see the ip and port as output, eg: http://192.168.64.14:30196.
  If you do not see, then from your wa-kube-environment map environment variables
    ```
    source .env
    ```

#### Logical Replication:
- Switch on logical replication running with the following environment variable:
     ```
     export SPRING_PROFILES_ACTIVE=replica;
     ```

#### Run:

- You can either run as Java Application from your IDE or use gradle:

    ```bash
      ./gradlew clean bootRun
    ```

- In order to test if the application is up, you can call its health endpoint:

    ```bash
      curl http://localhost:8087/health
    ```

  You should get a response similar to this:

    ```
      {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
    ```

- To access any service endpoint, you must set headers Service Authorization, Authorization
    - To set Service Authorization header, from wa-kube-environment Goto wa-kube-environment/scripts/actions
      and execute command
       ```
        ./idam-service-token.sh wa_task_management_api
       ```
      The command will generate a long token prefixed with your name. Copy the token till the name and set in Service
      Authorization header
      Service Authorization: Bearer 'your token'
    - To set Authorization header, from the same path execute command
       ```
        ./idam-user-token.sh "${TEST_CASEOFFICER_USERNAME}" "${TEST_CASEOFFICER_PASSWORD}"
       ```
      The command should generate a long token, copy the whole token and set in Authorization header
      Authorization: Bearer 'your token'
      Note: if the command returns null, then make sure the environment variable is set and
      you have sourced the environment variables.


#### Run tests
- To run integration tests you must have Docker running on your machine.
- To run integration tests, you can run the command:
  ```bash
       ./gradlew integration
  ```

- To run all functional tests or single test you can run as Junit, make sure the env is set
    ```
        OPEN_ID_IDAM_URL=http://'minikubeIP:port'
        Using simulator: OPEN_ID_IDAM_URL=http://sidam-simulator
    ```
  - Your minikube environment should be running correctly.
  - WA CCD definition should be uploaded into your CCD environment: https://github.com/hmcts/wa-ccd-definitions
  - Ensure wa_workflow_api is running: https://github.com/hmcts/wa-workflow-api
  - Make sure the BPMN and DMN are deployed onto Camunda locally which should have been completed with the minikube setup steps.
    - WA BPMN project is wa-standalone-task-bpmn: https://github.com/hmcts/wa-standalone-task-bpmn
    - WA DMN project is wa-task-configuration-template: https://github.com/hmcts/wa-task-configuration-template

- To run all tests including junit, integration and functional. You can run the command
   ```
       ./gradlew test integration functional
   ```
  or
  ```
  ./gradlew tests
  ```

### Running contract or pact tests:

You can run contract or pact tests as follows:

```
./gradlew contract
```

You can then publish your pact tests locally by first running the pact docker-compose:

```
docker-compose -f docker-pactbroker-compose.yml up

```

and then using it to publish your tests:

```
./gradlew pactPublish
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
