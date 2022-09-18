1、Build image ambari/develop:trunk-centos-7(build-image.sh)
2、Build containers for cluster env(build-containers.sh)
3、Clear containers after tests done(clear-containers.sh)
4、Ambari UI、Ambari Server Debug Port、MariaDB Server are also exposed to local ports: 8080、5005、3306
5、Docker host names are: ambari-server、ambari-agent-01、ambari-agent-02
6、Extra configurations are in `build-containers.sh` last few lines, eg. Kerberos Configuration、Hive DB Configuration