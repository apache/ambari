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
# HAWQ Monitoring View for Ambari
This view provides a UI to monitor HAWQ queries.

### Overview and Examples
You may find instructive the [Ambari Views Overview], which demonstrates how Ambari uses third-party views and how to create your own view. [Here][view example] you may find a view example.

### Build All Views (must be done at least once)
```sh
cd $AMBARI_DIR/contrib/views
mvn install -DskipTests
```

### Build HAWQ View
```sh
cd $AMBARI_DIR/contrib/views/hawq
mvn install [-DskipTests]
```

### Setting-Up The Enviornment
In order to prepare a vagrant environment, firstly follow the instructions in the [Ambari Dev Quick Start Guide].

### Deploy JAR file
```sh
vagrant ssh <Ambari Server Host>
sudo -i
ln -s /vagrant/ambari/contrib/views/hawq/target/hawq-view-X.Y.Z.Q-SNAPSHOT.jar /var/lib/ambari-server/resources/views/hawq-view-X.Y.Z.Q-SNAPSHOT.jar
ambari-server restart
```
- Create an instance of view from “Manage Ambari” category in Ambari.

If you wish to overwrite an installation of a view, then enter the vagrant box as root and
```sh
rm -rf /var/lib/ambari-server/resources/views/work/HAWQ\{X.Y.Z\}
```
(note that there is no trailing `/`) before restarting the Ambari server.  If you have made changes to the view, and those changes have not been reflected in the UI, then create a temporary throwaway view.  This may prompt Ambari to remove any stale references to the old view JAR in place of what you have just uploaded.

### Ember Development
The Hawq Monitoring View has been implemented using Ember 2.4.2; the tooling framework relies on Node 4.3.2.  There are a number of tools which you may need to install locally, starting with `nvm` (Node Version Manager).  You may wish to install the following tools while located in `$AMBARI_DIR`:

```sh
nvm install 4.3.2
nvm use 4.3.2
npm install ember-cli
```

This set of tools should allow you to use the Ember CLI tools for creating stub-files for controllers, routes, models, etc., in addition to `ember` for compiling and testing.  At the moment, `npm build`, `npm start`, and `npm test` all invoke the `ember` CLI tool.

### Local Javascript testing without the overhead of Maven
To do iterative unit testing while coding, firstly make a build using maven.  Afterward,
```sh
cd $AMBARI_DIR/contrib/views/hawq/src/main/resources/ui/
npm start # To continuously test that your code compiles
```
and, when you want to test the code, open another terminal and
```sh
npm test
```

### Ambari Versions
Be careful when moving this code from branch to branch:  the Ambari version referenced in pom.xml must match the branch.  You may have to reference other views (e.g. hive or pig) in the destination branch to get some idea of what you must change.

### Debug Setup
On the machine hosting vagrant:
```sh
vagrant ssh <Ambari Server Host>
sudo -i
cd /var/lib/ambari-server/resources/views/work  # if this directory does not exist, you have not started ambari-server; run "ambari-server start" to start it
rm -rf HAWQ\{X.Y.Z\}
ln -s /vagrant/ambari/contrib/views/hawq/src/main/resources/ui/dist HAWQ\{X.Y.Z\}
ln -s /vagrant/ambari/contrib/views/hawq/target/classes/org/ HAWQ\{X.Y.Z\}/org
ln -s /vagrant/ambari/contrib/views/hawq/target/classes/WEB-INF/ HAWQ\{X.Y.Z\}/WEB-INF
ln -s /vagrant/ambari/contrib/views/hawq/src/main/resources/view.xml HAWQ\{X.Y.Z\}/view.xml
ambari-server restart
```

Note:  if you want to remove the symbolic link `/var/lib/ambari-server/resources/views/work/HAWQ\{X.Y.Z\}`, use `rm` and not `rm -rf`.

### Incremental Builds For Java Proxy
The symbolic links generated in the Debug Setup section allow for the incremental updating of the Java proxy.  Each build with `mvn` deletes the symlinks from Debug Setup.  They must be recreated, and then the Ambari server must be restarted.  Additionally, each invocation of `npm start` or `ember serve` will destroy the links and require them to be recreated using the instructions in Debug Setup.  However, while the local Ember server is runnig, the links will not be removed by the server.

[//]: #

[ambari views overview]: <http://www.slideshare.net/hortonworks/ambari-views-overview>
[view example]: <https://github.com/apache/ambari/blob/trunk/ambari-views/examples/helloworld-view/docs/index.md>
[ambari dev quick start guide]: <https://cwiki.apache.org/confluence/display/AMBARI/Quick+Start+Guide>
