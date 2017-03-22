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

#Compilation
mvn clean compile package

#Deploy
##Copy to remote
copy target/logsearch-portal.tar.gz to host machine
##Setup environment
```bash
mkdir /opt/logsearch
cd /opt/logsearch
tar xfz ~/logsearch-portal.tar.gz 
```
#Create Solr Collection
*Edit for log retention days (default is 7 days)*
```bash
vi solr_configsets/hadoop_logs/conf/solrconfig.xml
```
```
    <processor class="solr.DefaultValueUpdateProcessorFactory">
        <str name="fieldName">_ttl_</str>
        <str name="value">+7DAYS</str>
    </processor>
```
```bash
./create_collections.sh $SOLR_HOME $NUM_SHARDS $NUM_OF_REPLICATIONS `pwd`/solr_configsets
```
```bash
vi classes/logsearch.properties
```
```
solr.zkhosts=$ZK1:2181,$ZK2:2181,$ZK3:2181/solr
```
*This script will stop logsearch if it is running and restart it*
```bash
./run.sh
```
