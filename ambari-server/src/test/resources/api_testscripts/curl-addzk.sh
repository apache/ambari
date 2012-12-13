curl -i -X POST -d '{"type": "zoo", "tag": "version1", "properties" : { "tickTime" : "20"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST http://localhost:8080/api/clusters/c1/services/ZOOKEEPER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/ZOOKEEPER/components/ZOOKEEPER_SERVER
curl -i -X PUT -d '{"config": {"zoo": "version1"}}'  http://localhost:8080/api/clusters/c1/services/ZOOKEEPER/components/ZOOKEEPER_SERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/ZOOKEEPER_SERVER
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/ZOOKEEPER/
