curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE
curl -i -X POST -d '{"type": "core-site", "tag": "version2", "properties" : { "fs.default.name" : "localhost:8020"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "mapred-site", "tag": "version1", "properties" : { "mapred.job.tracker" : "localhost:50300", "mapreduce.history.server.embedded": "false", "mapreduce.history.server.http.address": "localhost:51111"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"core-site": "version2", "mapred-site": "version1"}}'  http://localhost:8080/api/clusters/c1/services/MAPREDUCE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE/components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE/components/TASKTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/TASKTRACKER
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}'   http://localhost:8080/api/clusters/c1/services/MAPREDUCE/
