ARG APP_INSIGHTS_AGENT_VERSION=3.5.2

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/wa-task-management-api.jar /opt/app/

EXPOSE 8087
CMD [ "wa-task-management-api.jar" ]
