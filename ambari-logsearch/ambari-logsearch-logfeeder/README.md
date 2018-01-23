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

# Log Search - Log Feeder:

Log Feeder is a component of the Log Search service that reads logs, parses them and stores them in Apache Solr for the purpose
of later analysis.

## Start locally from maven / IDE

First you need to start every required service (except logfeeder), go to `ambari-logsearch/docker` folder and run:
```bash
docker-compose up -d zookeeper solr logsearch
```

Secondly, if you are planning to run Log Feeder from an IDE, for running the LogFeeder main methoud, you will need to set the working directory to `ambari/ambari-logsearch/ambari-logsearch-logfeeder` or set `LOGFEEDER_RELATIVE_LOCATION` env variable.
With Maven, you won't need these steps, just run this command from the ambari-logsearch-logfeeder folder:

```bash
mvn clean package -DskipTests spring-boot:run
```

# Input Configuration

The configuration for the log feeder contains
* description of the log files
* description of the filters that parse the data of the log entries
* description of the mappers that modify the parsed fields

The element description can be found [here](docs/inputConfig.md)

All these data are stored in json files, which should be named in the directory /etc/ambari-logsearch-logfeeder/conf, and the
name of the files should be input.config-<service\_name>.json