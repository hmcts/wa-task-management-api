# wa-task-management-api

[![Build Status](https://travis-ci.org/hmcts/wa-workflow-api.svg?branch=master)](https://travis-ci.org/hmcts/wa-task-management-api)

#### What does this app do?

Provides API endpoints that enable clients manage Tasks in the Camunda Task Database.

<!--
    Sequence Diagram Source:
    http://www.plantuml.com/plantuml/uml/lPLHQzim4CUVSv_Yo9vDdbT8QDfiOz2nGFi5BjB55hPEddHQLag_-vAORKxcCTgaqQF-Vtx7_MJmTLAAcdq3bRECwn1q5Nu0fDf9Iv4yfefis3WfKMFcWuI_PHR3-0IZUOnXSnkQ4771tDxNZpTDvkszqH1lGhGSkzHIh5VxwCwj-Su9_oSbKvSnPB5Tuzty_U9syH56C5fIL6TSIw9zHdz-ltDCsbHE6Pu1r6d3-42fnYyaLB9dKPmlFmDGra16tNTInY3G_i7Xs3IEHGjgs_5Xe5jKuJjKrt173KC-YwLYrVgYRKZN8Ven04odQ1fo7gTJWFw0OZP8nIjSVhr_kCB9BfqyEViysGC0Xll5R7XuZyHIEk4YSjA_gBWzaJL7mPIDuPJ-EQYO7Hh2xR7yM-Qmf9s0B2ShoF8s4h9SJeC9lcrO-HGFlnZd5T47Ny84fUHAESL3HpYxXGvx4GU9APeALu_ex0jCVFfm8E8r1Zh4e83-vhlBDrjxuxJ2_K-7bMDwtQTmnkS_NlpC3txDOhDlVW80
    See: https://plantuml.com/ docs for reference
-->

![task-management](task-management-api.png)

#### Access Management Process

<!--
    Sequence Diagram Source:
    http://www.plantuml.com/plantuml/uml/XPBDRjGm4CVFdQUmojaF05ANfhlggKWL8EuJPpPhnVQOyQGBGhmxJWmgsHQHGmxp-t_CIBujYg9p373o0vaZi_Ry3Q1CFcKKZAQSSE2pJwDHcMb3wEjCoP7v0LUT29_t75ZCWIX_chxVXPdgt2dB7Sj0qkY0ClKhUl17Ul29_aFHJQFmd8QcUDEzFUmFzt05LuyewftFcBHblEpVQ2wIpYUl13y1r6iWyndBP3vWmf4Y9JNMTCvHAMssZ01mLaQd_WcL32V8p-dcsWLVHHPplZPOZ0jRh3NVnfRVT7xLQTpgC5hXG1PByMDQfCKMaYVlQDNZXTvXh2UXxNEqEQ0UMM9Re2h11RlJQDwPJBAGvShEgUS4eD7kS64ZwSTQsJqtWf3EaeninytZ_fYMi2ye7lj01KpzVjjq0nPJM-A4PSyYvFIH9FeQpAcy1y2WhxERc_RegjlkZCFNB_ch6TV9dcCEfBHMruL3jLqI2eN-LNh42b-QxrbwFmoKgZiDS5j_Smo_0000
    See: https://plantuml.com/ docs for reference
-->

![task-management](access-management-process.png)



Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/wa-task-management-api` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `8090` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8090/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

