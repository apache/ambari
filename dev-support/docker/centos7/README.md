<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
## Build and install Ambari by dev-support
Dev support is used to quickly develop and test ambari, which runs on the docker containers.

### **Step 1**: Install build tools: Git、Docker
The scripts require docker to be installed, since the compile process will run in a docker container and Ambari cluster also deploys on containers.

**RHEL (CentOS 7) :**
```shell
yum install -y git docker
```
### **Step 2**: Download Ambari source
```shell
git clone https://github.com/apache/ambari.git
```
### **Step 3**: Enter workspace
**RHEL (CentOS 7) :**
```shell
cd ambari/dev-support/docker/centos7/
```
### **Step 4**: Build develop basic image
Run the setup command, you will get `ambari/develop:trunk-centos-7` image. It has the environment needed to compile Ambari and run servers such as Ambari Server, Ambari Agent, Mysql, etc.

**RHEL (CentOS 7) :**
```shell
./build-image.sh
```
### **Step 5**: Build Ambari source & create Ambari cluster
* The first compilation will take about 1 hour to download resources, and the next compilation will directly use the maven cache.
* Ambari UI、Ambari Server Debug Port、MariaDB Server are also exposed to local ports: 8080、5005、3306.
* Docker host names are: ambari-server、ambari-agent-01、ambari-agent-02.
* Access admin page via http://localhost:8080 on your web browser. Log in with username `admin` and password `admin`.
* Extra configurations are in `build-containers.sh` last few lines, eg. Kerberos Configuration、Hive DB Configuration.

**RHEL (CentOS 7) :**
```shell
./build-containers.sh
```
### **Step 6**: Re-build Ambari Server
Re-compile Ambari without re-create and deploy clusters.

**RHEL (CentOS 7) :**
```shell
./build-ambari.sh
```

### **Step 7**: Redistribution stack
Re-distribute stack scripts without re-create clusters.

**RHEL (CentOS 7) :**
```shell
./distribute-scripts.sh
```
### **Step 8**: Clean Ambari cluster
Clean up the containers of Ambari cluster when you are done developing or testing.

**RHEL (CentOS 7) :**
```shell
./clear-containers.sh
```
### Step 9: Clean up the build environment
**Note :** This operation will completely delete maven cache.

**RHEL (CentOS 7) :**
```shell
docker rm -f ambari-rpm-build
```
