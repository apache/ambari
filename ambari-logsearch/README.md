# Log Search

## Setup local environment with docker

### Prerequisites

Install [docker] (https://docs.docker.com/)
For Mac OS X use [Docker Machine] (https://docs.docker.com/machine/)

### Build and start Log Search in docker container
```bash
# to see available commands: run start-logsearch without arguments
cd docker
./logsearch-docker build-and-run
```
If you run the script at first time, it will generate you a new Profile file inside docker directory, in that file you should set AMBARI_LOCATION (point to the local ambari root folder) and MAVEN_REPOSITORY_LOCATION (point to local maven repository location). These will be used as volumes for the docker container. If Profile is generated for first time use `./logsearch-docker start` command to start container without rebuild the project/container again.

After the logsearch container is started you can enter to it with following command:
```bash
docker exec -it logsearch bash
```
## Package build process


1. Check out the code from GIT repository

2. On the logsearch root folder (ambari/ambari-logsearch), please execute the following Maven command to build RPM/DPKG:
```bash
mvn -Dbuild-rpm clean package
```
  or
```bash
mvn -Dbuild-deb clean package
```
3. Generated RPM/DPKG files will be found in ambari-logsearch-assembly/target folder

## Running Integration Tests

By default integration tests are not a part of the build process, you need to set ${it.skip} variable to true (docker needed here too)

```bash
# from ambari-logsearch folder
mvn clean integration-test -Dit.skip=false
```
Also you can run from the IDE, but make sure all of the ambari logsearch modules are built.
