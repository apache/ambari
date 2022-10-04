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
Dev-support is used to quickly compile and test ambari, which runs on the docker. The official version has not been released yet, so it is not recommended to deploy to the production environment.

### **Step 1**: Install build tools: Git、Docker
You need to install Docker, because of compile in Docker and Ambari cluster run on it.

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
Run the setup command, you will get `ambari/develop:trunk-centos-7` image. It has the tools needed to compile ambari and run servers such as Ambari-server, Ambari-agent, Mysql, etc.

**RHEL (CentOS 7) :**
```shell
./build-image.sh
```
### **Step 5**: Build Ambari source & create Ambari cluster
* The first compilation will take about 1 hour to download resources, and the next compilation will directly use the maven cache.
* Ambari UI、Ambari Server Debug Port、MariaDB Server are also exposed to local ports: 8080、5005、3306.
* Docker host names are: ambari-server、ambari-agent-01、ambari-agent-02.
* Script execution end, log will be print Ambari Server RSA Private Key.
* Open up a web browser and go to http://localhost:8080. Log in with username `admin` and password `admin`.
* Extra configurations are in `build-containers.sh` last few lines, eg. Kerberos Configuration、Hive DB Configuration.

**RHEL (CentOS 7) :**
```shell
./build-containers.sh
```
### **Step 6**: Re-build Ambari Server
This operation without re-creating clusters when you only to update code of Ambari.

**RHEL (CentOS 7) :**
```shell
./build-ambari.sh
```

### **Step 7**: Redistribution stack
This operation without re-creating clusters when you only to redistribute stack.

**RHEL (CentOS 7) :**
```shell
./distribute-scripts.sh
```
### **Step 8**: Clean Ambari cluster
Clear containers of Ambari cluster after tests done.

**RHEL (CentOS 7) :**
```shell
./clear-containers.sh
```
### Step 9: Clean build environment
**Note :** This operation will completely delete maven cache.

**RHEL (CentOS 7) :**
```shell
docker rm -f ambari-rpm-build
```
