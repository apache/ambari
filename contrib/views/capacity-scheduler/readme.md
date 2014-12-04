<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Capacity Scheduler View
============

Description
-----
This View provides a UI to manage queues for the YARN Capacity Scheduler.

Requirements
-----

- Ambari 1.7.0
- YARN

Build
-----

The view can be built as a maven project.

    mvn clean install

The build will produce the view archive.

    target/capacity-scheduler-0.2.0-SNAPSHOT.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp capacity-scheduler-0.2.0-SNAPSHOT /var/lib/ambari-server/resources/views/
    ambari-server restart

Deploying the View
-----

Use the [Ambari Vagrant](https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide) setup to create a cluster:

Deploy the Capacity Scheduler view into Ambari.

    cp capacity-scheduler-0.2.0-SNAPSHOT /var/lib/ambari-server/resources/views/
    ambari-server restart

From the Ambari Administration interface, create a view instance.

|Property|Value|
|---|---|
| Details: Instance Name | CS_1 |
| Details: Display Name | Queue Manager |
| Details: Description | Browse and manage YARN Capacity Scheduler queues |
| Properties: ambari.server.url | http://c6401.ambari.apache.org:8080/api/v1/clusters/MyCluster |
| Properties: ambari.server.username | admin |
| Properties: ambari.server.password | password |

Login to Ambari and browse to the view instance.

    http://c6401.ambari.apache.org:8080/#/main/views/CAPACITY-SCHEDULER/0.2.0/CS_1
