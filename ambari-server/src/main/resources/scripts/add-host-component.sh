#!/bin/sh

CLUSTER_NAME=$1
HOST_NAME=$2
COMPONENT_NAME=$3

curl -i -u admin:admin -X POST -d "{
\"host_components\": [
{\"HostRoles\" : { \"component_name\": \"${COMPONENT_NAME}\"} }
]}" http://localhost:8080/api/v1/clusters/${CLUSTER_NAME}/hosts?Hosts/host_name=${HOST_NAME}

curl -i -u admin:admin -X PUT -d '{ "HostRoles": {"state": "INSTALLED"
} }' http://localhost:8080/api/v1/clusters/${CLUSTER_NAME}/host_components?HostRoles/host_name=${HOST_NAME}\&HostRoles/component_name=${COMPONENT_NAME}\&HostRoles/state=INIT

