curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_CLIENT
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_CLIENT
curl -i -X POST -d '{"type": "hbase-site", "tag": "version1", "properties" : { "hbase.rootdir" : "hdfs://localhost:8020/apps/hbase/", "hbase.cluster.distributed" : "true", "hbase.zookeeper.quorum": "localhost", "zookeeper.session.timeout": "60000" }}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "hbase-env", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"hbase-site": "version1", "hbase-env": "version1"}}'  http://localhost:8080/api/clusters/c1/services/HBASE
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/HBASE/
