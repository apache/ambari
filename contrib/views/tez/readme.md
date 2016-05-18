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

TEZ View
============

Description
-----
This view provides a web interface for TEZ.

Requirements
-----

- Ambari 1.7.0
- TEZ 0.6.0 or above

Build
-----

The view can be built as a maven project.

    mvn clean install

The build will produce the view archive.

    target/tez-view-0.6.0-SNAPSHOT.jar

Configuration
-----

  For the UI to work as expected with all the required data, the following configurations
must be set.

In tez-site.xml
  * tez.runtime.convert.user-payload.to.history-text
      Should be enabled to get the configuration options. If enabled, the config options are set
    as userpayload per input/output.

In yarn-site.xml
  * yarn.timeline-service.http-cross-origin.enabled
      Enable CORS in timeline.
  * yarn.resourcemanager.system-metrics-publisher.enabled
      Enable generic history service in timeline server
  * yarn.timeline-service.enabled
      Enabled the timeline server for logging details
  * yarn.timeline-service.webapp.address
      Value must be the IP:PORT on which timeline server is running

In configs.js
  * Visibility of table columns can be controlled using the column selector in the UI. Also an optional set
    of file system counters can be made visible as columns for most of the tables. For adding more
    counters refer configs.js.


Deploying the View
-----

Use the [Ambari Vagrant](https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide) setup to create a cluster:

Deploy the TEZ view into Ambari.

    cp tez-view-0.6.0-SNAPSHOT.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

From the Ambari Administration interface, create a TEZ view instance.

|Property|Value|
|---|---|
| Details: Instance Name | TEZUI |
| Details: Display Name | TEZ |
| Details: Description | A web interface for TEZ |
| Properties: yarn.ats.url | http://yarn.timeline-service.hostname:8188 |
| Properties: yarn.resourcemanager.url | http://yarn.resourcemanager.hostname:8088 |

Login to Ambari as **"admin"** and browse to the view instance.

    http://c6401.ambari.apache.org:8080/#/main/views/TEZ/0.6.0/TEZUI

