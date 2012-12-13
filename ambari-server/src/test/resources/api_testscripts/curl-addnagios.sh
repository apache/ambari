curl -i -X POST http://localhost:8080/api/clusters/c1/services/NAGIOS
curl -i -X POST http://localhost:8080/api/clusters/c1/services/NAGIOS/components/NAGIOS_SERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/NAGIOS_SERVER
curl -i -X POST -d '{"type": "nagios-global", "tag": "version1", "properties" : { "nagios_web_login" : "nagiosadmin", "nagios_web_password" : "password", "nagios_contact": "a\u0040b.c" }}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"nagios-global": "version1" }}'  http://localhost:8080/api/clusters/c1/services/NAGIOS
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/NAGIOS/
