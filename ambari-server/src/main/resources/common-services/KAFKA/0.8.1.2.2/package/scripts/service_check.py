#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from resource_management import *

class ServiceCheck(Script):
  def service_check(self, env):
      import params
      env.set_params(params)
      
      kafka_config = self.read_kafka_config()
      
      create_topic_cmd_created_output = "Created topic \"ambari_kafka_service_check\"."
      create_topic_cmd_exists_output = "Topic \"ambari_kafka_service_check\" already exists."
      
      source_cmd = format("source {conf_dir}/kafka-env.sh")
      create_topic_cmd = format("{kafka_home}/bin/kafka-topics.sh --zookeeper {kafka_config[zookeeper.connect]} --create --topic ambari_kafka_service_check --partitions 1 --replication-factor 1")
      
      print "Running kafka create topic command"
      Execute(format("{source_cmd} ; {create_topic_cmd} | grep '{create_topic_cmd_created_output}\|{create_topic_cmd_exists_output}'"),
              logoutput=True,
      )

  def read_kafka_config(self):
    import params
    
    kafka_config = {}
    with open(params.conf_dir+"/server.properties","r") as conf_file:
      for line in conf_file:
          key,value = line.split("=")
          kafka_config[key] = value.replace("\n","")
    
    return kafka_config

if __name__ == "__main__":
    ServiceCheck().execute()
