<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

View Component Information
=====

[Back to Resources](index.md#resources)

Refers to a specific component identified by ":componentName" for a given service.

    GET /clusters/:name/services/:serviceName/components/:componentName

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
    	"metrics" : {
    		"process" : {
    			...    
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		},
      		"dfs" : {
        		"datanode" : {
          		...
        		}
      		},
      		"disk" : {
        		...
      		},
      		"cpu" : {
        		...
      		},
      		"rpcdetailed" : {
      			...
      		},
      		"jvm" : {
        		...
      		},
      		"load" : {
        		...
      		},
      		"memory" : {
        		...
      		},
      		"network" : {
        		...
      		},
    	},
    	"ServiceComponentInfo" : {
      		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"service_name" : "HDFS"
      		"state" : "STARTED"
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		}
       	]
    }
