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

The Ambari API provides access to monitoring and metrics information of a Apache Hadoop cluster. This document describes the resources used in the Ambari API and is intended for developers who want to integrate with Ambari.

- [Release Version](#release-version)
- [Authentication](#authentication)
- [Resources](#resources)
- [Partial Response](#partial-response)
- [Query Parameters](#query-parameters)
- [Errors](#errors)


Release Version
----
_Last Updated December 28, 2012_

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

        /clusters/MyCluster

  _Refers to the cluster resource identified by the id "MyCluster"_

### Clusters

- [List clusters](clusters.md)
- [View cluster information](clusters-cluster.md)

### Services

- [List services](services.md)
- [View service information](services-service.md)
- [View service components](components.md)
- [View component information](components-component.md)

### Hosts

- [List hosts](hosts.md)
- [View host information](hosts-host.md)
- [List host components](host-components.md)
- [View host component information](host-component.md)

Partial Response
----

A mechanism used to control which fields are returned by a query.  Partial response can be used to restrict which fields are returned and additionally, it allows a query to reach down and return data from sub-resources.  The keyword “fields” is used to specify a partial response.  Only the fields listed will be returned to the client.  To specify sub-elements, use the notation “a/b/c”.  The wildcard ‘*’ can be used to show all fields for a resource.  This can be combined to provide ‘expand’ functionality for sub-components.  Some fields are always returned for a resource regardless of the specified partial response fields.  These fields are the fields which uniquely identify the resource.  This would be the primary id field of the resource and the foreign keys to the primary id fields of all ancestors of the resource.

**Example: Partial Response (Name and All metrics)*

    GET    /api/v1/clusters/MyCluster/services/HDFS/components/NAMENODE?fields=name,metrics


    200 OK
    {
      “href” :”.../api/v1/clusters/MyCluster/services/HDFS/components/NAMENODE?fields=name,metrics”,
      “name”: “NAMENODE”,
      “metrics”: [
        {
        ...
        }
      ]
    }

Query Parameters
----

This mechanism limits which data is returned by a query based on a predicate(s). Providing query parameters does not result in any link expansion in the data that is returned to the client although it may result in expansion on the server to apply predicates on sub-objects.

_Note: Only applies to collection resources. And all URLs must be properly URL encoded_

**Query Operators**

<table>
  <tr>
    <th>Operator</th>
    <th>Example</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>=</td>
    <td>name=host1</th>
    <td>String or numerical equals</td>
  </tr>
  <tr>
    <td>!=</td>
    <td>host!=host1</th>
    <td>String or numerical not equals</td>
  </tr>
  <tr>
    <td>&lt;</td>
    <td>disk_total&lt;50</th>
    <td>Numerical less than</td>
  </tr>
  <tr>
    <td>&gt;</td>
    <td>disk_total&gt;50</th>
    <td>Numerical greater than</td>
  </tr>
  <tr>
    <td>&lt;=</td>
    <td>disk_total&lt;=50</th>
    <td>Numerical less than or equals</td>
  </tr>
  <tr>
    <td>&gt;=</td>
    <td>disk_total&gt;=50</th>
    <td>Numerical greater than or equals</td>
  </tr>
  <tr>
    <td>or</td>
    <td>disk_total&gt;50 or disk_free&lt;100</th>
    <td>Logial 'or'</td>
  </tr>
</table>

**Example: Get all hosts with less than 100 "disk_total"**

    GET  /api/v1/clusters/c1/hosts?metrics/disk/disk_total<100

Errors
----

This section describes how errors are represented in a response.

**Response**

    404 Not Found
    {
      “status”: 404,
      “message”: “standard message”,
      “developerMessage”: “verbose developers message”,
      “code”: 1234,
      “moreInfo”, “...”
    }

