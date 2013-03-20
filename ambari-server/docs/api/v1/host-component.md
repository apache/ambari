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

View Host Component Information
=====

[Back to Resources](index.md#resources)

Returns information for a specific role on the given host.

    GET /clusters/:name/hosts/:hostName/host_components/:componentName

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
    	"HostRoles" : {
    		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"host_name" : "host1",
      		"state" : "STARTED"
    	},
    	"host" : {
    		"href" : "http://localhost:8080/api/v1/clusters/c1/hosts/dev.hortonworks.com"
    	},
    	"metrics" : {
    		"process" : {
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
    	"component" : [
      		{
    	      	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
      			"ServiceComponentInfo" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"service_name" : "HDFS"
        		}
      		}
       	]
    }

