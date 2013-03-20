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

View Cluster Information
=====

[Back to Resources](index.md#resources)

Returns information for the specified cluster identified by ":name"

    GET /clusters/:name

**Response**

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1",
      	"Clusters" : {
        	"cluster_name" : "c1",
        	"cluster_id" : 1,
        	"version" : "HDP-1.2.0"
      	},
      	"services" : [
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/NAGIOS",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "NAGIOS"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HCATALOG",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "HCATALOG"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/PIG",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
         			"service_name" : "PIG"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "MAPREDUCE"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/GANGLIA",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "GANGLIA"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HIVE",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "HIVE"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS",
        		"ServiceInfo" : {
          			"cluster_name" : "MyIE9",
          			"service_name" : "HDFS"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/ZOOKEEPER",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
         	 		"service_name" : "ZOOKEEPER"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HBASE",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "HBASE"
          		}
        	},
        	{
        		"href" : "http://your.ambari.server/api/v1/clusters/c1/services/OOZIE",
        		"ServiceInfo" : {
          			"cluster_name" : "c1",
          			"service_name" : "OOZIE"
          		}
        	} 
    	],
    	"hosts" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1",
      			"Hosts" : {
        			"cluster_name" : "c1",
        			"host_name" : "some.cluster.host"
        		}
      		},
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host2",
      		"Hosts" : {
        		"cluster_name" : "c1",
        		"host_name" : "another.cluster.host"
        	}
        ]
    }

