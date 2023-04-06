<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

# Log Search Server

## Start locally from maven / IDE

Other services (like zookeeper, solr, logfeeder) can be started with `docker-compose`
```bash
cd ambari/ambari-logsearch/docker
docker-compose up -d zookeeper solr logfeeder
```

Then you can start Log Search server from maven 

```bash
cd ambari/ambari-logsearch/ambari-logsearch-server
./run.sh
# or
mvn clean package -DskipTests spring-boot:run
```

You can also start Log Search server from an IDE as well. One thing is important: the config set location that the server tries to upload to ZooKeeper. By default config sets are located at `${LOGSEARCH_SERVER_RELATIVE_LOCATION:}src/main/configsets` in `logsearch.properties`. Based or from where you run `LogSearch.java`, you need to set `LOGSEARCH_SERVER_RELATIVE_LOCATION` env variable properly. 
