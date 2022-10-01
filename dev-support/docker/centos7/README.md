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

1、Build image ambari/develop:trunk-centos-7(build-image.sh)
2、Build containers for cluster env(build-containers.sh)
3、Clear containers after tests done(clear-containers.sh)
4、Ambari UI、Ambari Server Debug Port、MariaDB Server are also exposed to local ports: 8080、5005、3306
5、Docker host names are: ambari-server、ambari-agent-01、ambari-agent-02
6、Extra configurations are in `build-containers.sh` last few lines, eg. Kerberos Configuration、Hive DB Configuration
7、Re-build Ambari without re-creating clusters when code updates(build-ambari.sh)
8、Distribute stack scripts without re-creating clusters(distribute-scripts.sh)