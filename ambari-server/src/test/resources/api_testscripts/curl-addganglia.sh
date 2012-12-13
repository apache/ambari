curl -i -X POST http://localhost:8080/api/clusters/c1/services/GANGLIA
curl -i -X POST http://localhost:8080/api/clusters/c1/services/GANGLIA/components/GANGLIA_SERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/GANGLIA/components/GANGLIA_MONITOR
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/GANGLIA_SERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/GANGLIA_MONITOR
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/GANGLIA/
