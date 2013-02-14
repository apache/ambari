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

Ambari API Reference v1
=========

The Ambari API provides access to monitoring and metrics information of a Apache Hadoop cluster. This document describes the resources and syntax used in the Ambari API and is intended for developers who want to integrate with Ambari.

- [Release Version](#release-version)
- [Authentication](#authentication)
- [Resources](#resources)
- [Partial Response](#partial-response)
- [Query Parameters](#query-parameters)
- [Errors](#errors)


Release Version
----
_Last Updated February 13, 2013_

Authentication
----

The operations you perform against the Ambari API require authentication. Your access to the API requires the use of **Basic Authentication**. To use Basic Authentication, you need to send the **Authorization: Basic** header with your requests. For example, this can be handled when using curl and the --user option.

    curl --user name:password http://{your.ambari.server}/api/v1/clusters

_Note: The authentication method and source is configured at the Ambari Server. Changing and configuring the authentication method and source is not covered in this document._

Resources
----

There are 2 types of resources in the Ambari API:

- **Collection Resource:** This resource type refers to a collection of resources, rather than any specific resource. For example:

        /clusters  

  _Returns a collection of clusters_

- **Instance Resource:** This resource type refers to a single specific resource. For example:

        /clusters/c1

  _Refers to the cluster resource identified by the id "c1"_

### clusters
- [List clusters](clusters.md)
- [View cluster information](clusters-cluster.md)

### services
- [List services](services.md)
- [View service information](services-service.md)

### components
- [List service components](components.md)
- [View component information](components-component.md)

### hosts
- [List hosts](hosts.md)
- [View host information](hosts-host.md)

### host_components
- [List host components](host-components.md)
- [View host component information](host-component.md)

Partial Response
----

Used to control which fields are returned by a query.  Partial response can be used to restrict which fields are returned and additionally, it allows a query to reach down and return data from sub-resources.  The keyword “fields” is used to specify a partial response.  Only the fields specified will be returned to the client.  To specify sub-elements, use the notation “a/b/c”.  Properties, categories and sub-resources can be specified.  The wildcard ‘*’ can be used to show all categories, fields and sub-resources for a resource.  This can be combined to provide ‘expand’ functionality for sub-components.  Some fields are always returned for a resource regardless of the specified partial response fields.  These fields are the fields, which uniquely identify the resource.  This would be the primary id field of the resource and the foreign keys to the primary id fields of all ancestors of the resource.

**Example: Using Partial Response to restrict response to a specific field**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total

    200 OK	{    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total”,    	“ServiceComponentInfo” : {        	“cluster_name” : “c1”,        	“component_name” : NAMENODE”,        	“service_name” : “HDFS”    	},    	“metrics” : {        	"disk" : {                   	"disk_total" : 100000        	}    	}    }
**Example: Using Partial Response to restrict response to specified category**

    GET    /api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk

    200 OK	{    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk”,    	“ServiceComponentInfo” : {        	“cluster_name” : “c1”,        	“component_name” : NAMENODE”,        	“service_name” : “HDFS”    	},    	“metrics” : {        	"disk" : {                   	"disk_total" : 100000,            	“disk_free” : 50000,            	“part_max_used” : 1010        	}    	}	}

**Example – Using Partial Response to restrict response to multiple fields/categories**

	GET	/api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu
	
	200 OK	{    	“href”: “.../api/v1/clusters/c1/services/HDFS/components/NAMENODE?fields=metrics/disk/disk_total,metrics/cpu”,    	“ServiceComponentInfo” : {        	“cluster_name” : “c1”,        	“component_name” : NAMENODE”,        	“service_name” : “HDFS”    	},    	“metrics” : {        	"disk" : {                   	"disk_total" : 100000        	},        	“cpu” : {            	“cpu_speed” : 10000000,            	“cpu_num” : 4,            	“cpu_idle” : 999999,            	...        	}    	}	}**Example – Using Partial Response to restrict response to a sub-resource**
	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components
	200 OK	{    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components”,    	“Hosts” : {        	“cluster_name” : “c1”,        	“host_name” : “host1”    	},    	“host_components”: [        	{            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”            	“HostRoles” : {                	“cluster_name” : “c1”,                	“component_name” : “NAMENODE”,                	“host_name” : “host1”            	}        	},        	{            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”            	“HostRoles” : {                	“cluster_name” : “c1”,                	“component_name” : DATANODE”,                	“host_name” : “host1”            	}        	},
            ...     	]	}**Example – Using Partial Response to expand a sub-resource one level deep**
	GET	/api/v1/clusters/c1/hosts/host1?fields=host_components/*
	200 OK	{    	“href”: “.../api/v1/clusters/c1/hosts/host1?fields=host_components/*”,    	“Hosts” : {        	“cluster_name” : “c1”,        	“host_name” : “host1”        },
        “host_components”: [        	{            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”            	“HostRoles” : {                	“cluster_name” : “c1”,               		“component_name” : DATANODE”,                	“host_name” : “host1”,                	“state” : “RUNNING”,                	...            	},                    	"host" : {                     	"href" : ".../api/v1/clusters/c1/hosts/host1"              	},            	“metrics” : {                	"disk" : {                           	"disk_total" : 100000000,                           	"disk_free" : 5000000,                           	"part_max_used" : 10101                     	},                	...            	},            	"component" : {                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE",                 	“ServiceComponentInfo” : {                    	"cluster_name" : "c1",                             	"component_name" : "NAMENODE",                             	"service_name" : "HDFS"                       	}            	}          	},        	...    	]	}
**Example – Using Partial Response for multi-level expansion of sub-resources**
	
	GET /api/v1/clusters/c1/hosts/host1?fields=host_components/component/*
	
	200 OK	{    	“href”: “http://ambari.server/api/v1/clusters/c1/hosts/host1?fields=host_components/*”,    	“Hosts” : {        	“cluster_name” : “c1”,        	“host_name” : “host1”        	...    	},
    	“host_components”: [
    		{            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”,            	“HostRoles” : {                	“cluster_name” : “c1”,                	“component_name” : DATANODE”,                	“host_name” : “host1”            	},             	"component" : {                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",                 	“ServiceComponentInfo” : {                   		"cluster_name" : "c1",                             	"component_name" : "DATANODE",                             	"service_name" : "HDFS"                      	...                     	},             		“metrics”: {                   		“dfs”: {                       		“datanode” : {          	                	“blocks_written " :  10000,          	                	“blocks_read" : 5000,                             	...                        	}                    	},                    	“disk”: {                       		"disk_total " :  1000000,                        	“disk_free" : 50000,                        	...                    	},                   		... 	
					}            	}        	},        	{            	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”,            	“HostRoles” : {                	“cluster_name” : “c1”,                	“component_name” : NAMENODE”,                	“host_name” : “host1”            	},             	"component" : {                	"href" : "http://ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE",                 	“ServiceComponentInfo” : {                   		"cluster_name" : "c1",                             	"component_name" : "NAMENODE",                             	"service_name" : "HDFS"                       	},             		“metrics”: {                    	“dfs”: {                       		“namenode” : {          	            		“FilesRenamed " :  10,          	            		“FilesDeleted" : 5                         		…                    		}
						},	                    	“disk”: {                       		"disk_total " :  1000000,                       		“disk_free" : 50000,                        	...                    	}                	},                	...            	}        	},        	...    	]	}**Example: Using Partial Response to expand collection resource instances one level deep**
	GET /api/v1/clusters/c1/hosts?fields=*
	200 OK	{    	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/?fields=*”,        	“items”: [         	{            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host1”,            	“Hosts” : {                	“cluster_name” :  “c1”,                	“host_name” : “host1”            	},            	“metrics”: {                	“process”: {          	                       		"proc_total" : 1000,          	       		"proc_run" : 1000                	},                	...            	},            	“host_components”: [                	{                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/NAMENODE”                    	“HostRoles” : {                       		“cluster_name” : “c1”,                         	“component_name” : “NAMENODE”,                        	“host_name” : “host1”                    	}                	},                	{                    	“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”                    	“HostRoles” : {                       		“cluster_name” : “c1”,                        	“component_name” : DATANODE”,                        	“host_name” : “host1”                    	}                	},                	...            	},            	...        	},        	{            	“href” : “http://ambari.server/api/v1/clusters/c1/hosts/host2”,            	“Hosts” : {                	“cluster_name” :  “c1”,                	“host_name” : “host2”            	},            	“metrics”: {               		“process”: {          	                       		"proc_total" : 555,          	     		"proc_run" : 55                	},                	...            	},            	“host_components”: [                	{                   		“href”: “…/api/v1/clusters/c1/hosts/host1/host_components/DATANODE”                    	“HostRoles” : {                       		“cluster_name” : “c1”,                        	“component_name” : “DATANODE”,                        	“host_name” : “host2”                    	}                	},                	...            	],            	...        	},        	...    	]	}### Additional Partial Response Examples
**Example – For each cluster, get cluster name, all hostname’s and all service names**
	GET   /api/v1/clusters?fields=Clusters/cluster_name,hosts/Hosts/host_name,services/ServiceInfo/service_name
**Example - Get all hostname’s for a given component**
	GET	/api/v1/clusters/c1/services/HDFS/components/DATANODE?fields=host_components/HostRoles/host_name
**Example - Get all hostname’s and component names for a given service**
	GET	/api/v1/clusters/c1/services/HDFS?fields=components/host_components/HostRoles/host_name,
                                      	          components/host_components/HostRoles/component_name

Query Predicates
----

Used to limit which data is returned by a query.  This is synonymous to the “where” clause in a SQL query.  Providing query parameters does not result in any link expansion in the data that is returned, with the exception of the fields used in the predicates.  Query predicates can only be applied to collection resources.  A predicate consists of at least one relational expression.  Predicates with multiple relational expressions also contain logical operators, which connect the relational expressions.  Predicates may also use brackets for explicit grouping of expressions. 

###Relational Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>=</td>
    <td>name=host1</td>
    <td>String or numerical EQUALS</td>
  </tr>
  <tr>
    <td>!=</td>
    <td>name!=host1</td>
    <td>String or numerical NOT EQUALS</td>
  </tr>
  <tr>
    <td>&lt;</td>
    <td>disk_total&lt;50</td>
    <td>Numerical LESS THAN</td>
  </tr>
  <tr>
    <td>&gt;</td>
    <td>disk_total&gt;50</td>
    <td>Numerical GREATER THAN</td>
  </tr>
  <tr>
    <td>&lt;=</td>
    <td>disk_total&lt;=50</td>
    <td>Numerical LESS THAN OR EQUALS</td>
  </tr>
  <tr>
    <td>&gt;=</td>
    <td>disk_total&gt;=50</td>
    <td>Numerical GREATER THAN OR EQUALS</td>
  </tr>  
</table>

###Logical Query Operators

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>|</td>
    <td>name=host1|name=host2</td>
    <td>Logical OR operator</td>
  </tr>
  <tr>
    <td>&</td>
    <td>prop1=foo&prop2=bar</td>
    <td>Logical AND operator</td>
  </tr>
  <tr>
    <td>!</td>
    <td>!prop<50</td>
    <td>Logical NOT operator</td>
  </tr>
</table>

**Logical Operator Precedence**

Standard logical operator precedence rules apply.  The above logical operators are listed in order of precedence starting with the lowest priority.  

###Brackets

<table>
  <tr>
    <th>Bracket</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>(</td>
    <td>Opening Bracket</td>
  </tr>
  <tr>
    <td>)</td>
    <td>Closing Bracket</td>
  </tr>

</table>
  
Brackets can be used to provide explicit grouping of expressions. Expressions within brackets have the highest precedence.

###Operator Functions
 
<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>in()</td>
    <td>name.in(foo,bar)</td>
    <td>IN function.  More compact form of name=foo|name=bar. </td>
  </tr>
  <tr>
    <td>isEmpty()</td>
    <td>category.isEmpty()</td>
    <td>Used to determine if a category contains any properties. </td>
  </tr>
</table>
Operator functions behave like relational operators and provide additional functionality.  Some operator functions, such as in(), act as binary operators like the above relational operators, where there is a left and right operand.  Some operator functions are unary operators, such as isEmpty(), where there is only a single operand.

###Query Examples

**Example – Get all hosts with “HEALTHY” status that have 2 or more cpu**
	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_status=HEALTHY&Hosts/cpu_count>=2
	
**Example – Get all hosts with less than 2 cpu or host status != HEALTHY**	
	GET	/api/v1/clusters/c1/hosts?Hosts/cpu_count<2|Hosts/host_status!=HEALTHY**Example – Get all “rhel6” hosts with less than 2 cpu or “centos6” hosts with 3 or more cpu**  
	GET	/api/v1/clusters/c1/hosts?Hosts/os_type=rhel6&Hosts/cpu_count<2|Hosts/os_type=centos6&Hosts/cpu_count>=3**Example – Get all hosts where either state != “HEALTHY” or last_heartbeat_time < 1360600135905 and rack_info=”default_rack”**	GET	/api/v1/clusters/c1/hosts?(Hosts/host_status!=HEALTHY|Hosts/last_heartbeat_time<1360600135905)
                                  &Hosts/rack_info=default_rack**Example – Get hosts with host name of host1 or host2 or host3 using IN operator**	
	GET	/api/v1/clusters/c1/hosts?Hosts/host_name.in(host1,host2,host3)**Example – Get and expand all HDFS components, which have at least 1 property in the “metrics/jvm” category (combines query and partial response syntax)**	GET	/api/v1/clusters/c1/services/HDFS/components?!metrics/jvm.isEmpty()&fields=*Temporal Metrics
----

Some metrics have values that are available across a range in time.  To query a metric for a range of values, the following partial response syntax is used.  To get temporal data for a single property:?fields=category/property[start-time,end-time,step]	To get temporal data for all properties in a category:?fields=category[start-time,end-time,step]start-time: Required field.  The start time for the query in Unix epoch time format.end-time: Optional field, defaults to now.  The end time for the query in Unix epoch time format.step: Optional field, defaults to the corresponding metrics system’s default value.  If provided, end-time must also be provided. The interval of time between returned data points specified in seconds. The larger the value provided, the fewer data points returned so this can be used to limit how much data is returned for the given time range.  This is only used as a suggestion so the result interval may differ from the one specified.The returned result is a list of data points over the specified time range.  Each data point is a value / timestamp pair.**Note**: It is important to understand that requesting large amounts of temporal data may result in severe performance degradation.  **Always** request the minimal amount of information necessary.  If large amounts of data are required, consider splitting the request up into multiple smaller requests.
**Example – Temporal Query for a single property using only start-time**
	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]
	
	200 OK	{    	“href” : …/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm/gcCount[1360610225]”,    	...    	“metrics”: [        	{            	“jvm”: {          	    	"gcCount" : [                   		[10, 1360610165],                     	[12, 1360610180],                     	[13, 1360610195],                     	[14, 1360610210],                     	[15, 1360610225]                  	]             	}         	}    	]	}**Example – Temporal Query for a category using start-time, end-time and step**
	GET	/api/v1/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]
	200 OK	{    	“href” : …/clusters/c1/hosts/host1?fields=metrics/jvm[1360610200,1360610500,100]”,    	...    	“metrics”: [        	{            	“jvm”: {          	    	"gcCount" : [                   		[10, 1360610200],                     	[12, 1360610300],                     	[13, 1360610400],                     	[14, 1360610500]                  	],                	"gcTimeMillis" : [                   		[1000, 1360610200],                     	[2000, 1360610300],                     	[5000, 1360610400],                     	[9500, 1360610500]                  	],                  	...             	}         	}    	]	}	

HTTP Return Codes
----

The following HTTP codes may be returned by the API.
<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>


Errors
----

**Example errors responses**

    404 Not Found	{       	"status" : 404,       	"message" : "The requested resource doesn't exist: Cluster not found, clusterName=someInvalidCluster" 	} 
&nbsp;	400 Bad Request	{       	"status" : 400,       	"message" : "The properties [foo] specified in the request or predicate are not supported for the 
                	 resource type Cluster."	}

