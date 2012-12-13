curl -i -X POST http://localhost:8080/api/clusters/c1/services/HDFS
curl -i -X POST -d '{"type": "core-site", "tag": "version1", "properties" : { "fs.default.name" : "localhost:8020"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "hdfs-site", "tag": "version1", "properties" : { "dfs.datanode.data.dir.perm" : "750"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "global", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"core-site": "version1", "hdfs-site": "version1", "global" : "version1" }}'  http://localhost:8080/api/clusters/c1/services/HDFS
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HDFS/components/NAMENODE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HDFS/components/SECONDARY_NAMENODE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HDFS/components/DATANODE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HDFS/components/HDFS_CLIENT
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/NAMENODE
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/SECONDARY_NAMENODE
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/DATANODE
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HDFS_CLIENT
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/HDFS
