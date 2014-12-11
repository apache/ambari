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
from __future__ import print_function
from resource_management import *
import  sys,subprocess,os

class ServiceCheck(Script):
    def service_check(self, env):
        import params
        env.set_params(params)
        kafka_config=self.read_kafka_config(params.conf_dir)
        self.set_env(params.conf_dir)
        create_topic_cmd_created_output = "Created topic \"ambari_kafka_service_check\"."
        create_topic_cmd_exists_output = "Topic \"ambari_kafka_service_check\" already exists."
	print("Running kafka create topic command", file=sys.stdout)
        create_topic_cmd = [params.kafka_home+'/bin/kafka-topics.sh', '--zookeeper '+kafka_config['zookeeper.connect'],
                            '--create --topic ambari_kafka_service_check', '--partitions 1 --replication-factor 1']
	print(" ".join(create_topic_cmd), file=sys.stdout)
        create_topic_process = subprocess.Popen(create_topic_cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        out, err = create_topic_process.communicate()
        if out.find(create_topic_cmd_created_output) != -1:
	    print(out, file=sys.stdout)
            sys.exit(0)
        elif out.find(create_topic_cmd_exists_output) != -1:
            print("Topic ambari_kafka_service_check exists", file=sys.stdout)
            sys.exit(0)
        else:
	    print(out, file=sys.stderr)
            sys.exit(1)

    def read_kafka_config(self,kafka_conf_dir):
        conf_file = open(kafka_conf_dir+"/server.properties","r")
        kafka_config = {}
        for line in conf_file:
            key,value = line.split("=")
            kafka_config[key] = value.replace("\n","")
        return kafka_config

    def set_env(self, kafka_conf_dir):
        command = ['bash', '-c', 'source '+kafka_conf_dir+'/kafka-env.sh && env']
        proc = subprocess.Popen(command, stdout = subprocess.PIPE)
        for line in proc.stdout:
            (key, _, value) = line.partition("=")
            os.environ[key] = value.replace("\n","")
        proc.communicate()

if __name__ == "__main__":
    ServiceCheck().execute()
