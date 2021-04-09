1. Start kafka (using community strimzi containers)
   `````
   $ podman-compose -f etc/docker-compose.yaml up -d
   `````

2. Start RH-PAM postgresql
   `````
   $ podman start postgresql
   `````

3. Clone, build and deploy pneumonia-patient-processing-kjar
   `````
   $ git clone https://github.com/redhat-naps-da/pneumonia-patient-processing-kjar.git
   $ cd pneumonia-patient-processing-kjar
   $ mvn clean install
   $ mvn deploy \
        -DaltDeploymentRepository="nexus::default::http://admin:admin123@$NEXUS_ROUTE/repository/redhat-naps/"
   `````

4. Build and Start app
   `````
   $ mvn clean package -DskipTests && \
         java -jar target/pneumonia-patient-processing-pam-0.0.1.jar
   `````

## Test

1. Set environment variables to support testing:
   `````
   $ export KJAR_VERSION=1.0.3
   $ export KIE_SERVER_CONTAINER_NAME=fhir-bpm
   `````

2. Health Check Report
   `````
   $ curl -X GET -H 'Content-Type:application/json' localhost:8080/rest/server/healthcheck?report=true
   `````

3. Create a container in kie-server:
   `````
   $ sed "s/{KIE_SERVER_CONTAINER_NAME}/$KIE_SERVER_CONTAINER_NAME/g" config/kie_container.json \
     | sed "s/{KJAR_VERSION}/$KJAR_VERSION/g" \
     > /tmp/kie_container.json && \
     curl -X PUT -H 'Content-type:application/json' localhost:8080/rest/server/containers/$KIE_SERVER_CONTAINER_ID-$KJAR_VERSION -d '@/tmp/kie_container.json'
   `````

4. List containers
   `````
   $ curl -X GET http://localhost:8080/rest/server/containers
   `````

5. Start a business process
   `````
   $ curl -X POST localhost:8080/fhir/processes/sendSampleCloudEvent/azra12350
   `````

6. List cases in JSON representation:
   `````
   $ curl -X GET -H 'Content-type:application/json' localhost:8080/rest/server/queries/cases/
   `````

7. List process definitions in JSON representation:
   `````
   $ curl -X GET -H 'Content-type:application/json' localhost:8080/rest/server/containers/$KIE_SERVER_CONTAINER_NAME-$KJAR_VERSION/processes/
   `````
