[](Licensed to the Apache Software Foundation (ASF) under one)
[](or more contributor license agreements.  See the NOTICE file)
[](distributed with this work for additional information)
[](regarding copyright ownership.  The ASF licenses this file)
[](to you under the Apache License, Version 2.0 (the)
[]("License"); you may not use this file except in compliance)
[](with the License.  You may obtain a copy of the License at)
[]()
[](    http://www.apache.org/licenses/LICENSE-2.0)
[]()
[](Unless required by applicable law or agreed to in writing, software)
[](distributed under the License is distributed on an "AS IS" BASIS,)
[](WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.)
[](See the License for the specific language governing permissions and)
[](limitations under the License.)

# HAWQ View

**HAWQ View** provides a **Query Monitor** for HAWQ which displays the current running queries.

The HAWQ View frontend is built based on EmberJS framework. The Java backend extends the framework provided by the ambari-views project.

The frontend polls the REST API periodically (5 seconds). The REST endpoint is responsible for querying the data from the *pg_stat_activity* table for every ```GET``` request on ```/queries``` resource.


## Building and Deploying HAWQ View

#### Building

The HAWQ View is dependent on the ambari-views artifact. As a pre-requisite, build the *ambari-views* project.

```$AMBARI_DIR``` refers to the top-level directory for Ambari source code.

```sh
# Build ambari-views project
cd $AMBARI_DIR/ambari-views
mvn install [-DskipTests]
# Build HAWQ View
cd $AMBARI_DIR/contrib/views/hawq
mvn install [-DskipTests]
```

#### Deploying

Copy the hawq-view jar to the ambari-server host and restart ambari-server.
```sh
scp $AMBARI_DIR/contrib/views/hawq/target/hawq-view-${version}.jar ambari.server.host:/var/lib/ambari-server/resources/views/
ambari-server restart
```

## Creating an Instance of HAWQ View

The HAWQ View instance connects to the HAWQ Master through JDBC. Ssh into the HAWQ Master host and update *pg_hba.conf* to allow connections from the ambari-server host for the user which has access to the *pg_stat_activity* table.

By default the *gpadmin* user has aceess to the *pg_stat_activity* table. Restart HAWQ Master from Ambari dashboard for changes to take effect.

Example of entry in *pg_hba.conf*, where ```192.168.64.101``` is my ambari-server host:
```
host  all	gpadmin 	192.168.64.101/32       trust
```

Navigate to the *Views* tab on *Manage Ambari* page. Click on *Create Instance* under *HAWQ* tab. Under *Settings* section, provide the HAWQ database username and password of the user who has access to the *pg_stat_activity* table. (The same user that was added to the *pg_hba.conf* for the ambari-server host entry)

Upon clicking *Save*, the view will be created.
