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
This View provides a React and TypeScript UI to manage queues for the YARN
Capacity Scheduler. It supports queue hierarchy changes, scheduler settings,
user queue mappings, node-label capacities, configuration history, raw custom
properties, and save/refresh/restart operations.

Requirements
-----

- Ambari 3.0.0 or later
- YARN
- Node.js 20 and npm 10 for UI development

Build
-----

The View can be built as a Maven project. Maven installs the pinned Node and
npm versions, runs `npm ci`, builds the Vite application, and packages the
generated `dist` resources in the View archive.

    mvn clean install

The build will produce the view archive.

    target/capacity-scheduler-<Ambari version>.jar

Place the view archive on the Ambari Server and restart to deploy.    

    cp target/capacity-scheduler-<Ambari version>.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

Deploying the View
-----

Use the [Ambari Vagrant](https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide) setup to create a cluster:

Deploy the Capacity Scheduler View into Ambari.

    cp target/capacity-scheduler-<Ambari version>.jar /var/lib/ambari-server/resources/views/
    ambari-server restart

From the Ambari Administration interface, create a view instance.

|Property|Value|
|---|---|
| Details: Instance Name | CS_1 |
| Details: Display Name | Queue Manager |
| Details: Description | Browse and manage YARN Capacity Scheduler queues |
Login to Ambari and browse to the view instance.

    http://c6401.ambari.apache.org:8080/#/main/views/CAPACITY-SCHEDULER/1.0.0/CS_1

Local Development
-----
The UI is under `src/main/resources/ui`:

    npm ci
    npm test
    npm run build

The production application derives the View name, version, and instance from
the Ambari URL. A standalone Vite development server therefore requires an
Ambari API proxy or mocked View REST responses.

After building and deploying the View, delete the view work directory on the Ambari Server.

    cd /var/lib/ambari-server/resources/views/work
    rm -rf CAPACITY-SCHEDULER\{1.0.0\}/

Create a symlink from the vagrant machine running your Ambari Server to your local machine.

    ln -s /vagrant/ambari/contrib/views/capacity-scheduler/target/classes/ CAPACITY-SCHEDULER\{1.0.0\}
    
Restart Ambari Server, login and browse to the view.

    ambari-server restart
    http://c6401.ambari.apache.org:8080/#/main/views/CAPACITY-SCHEDULER/1.0.0/CS_1
    
If you modify the view UI code on your machine and re-build, the UI will pickup
the changes on browser refresh.
