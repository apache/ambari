curl -i -X POST -d '{"Clusters": {"version" : "HDP-1.2.0"}}' http://localhost:8080/api/clusters/c1
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain
