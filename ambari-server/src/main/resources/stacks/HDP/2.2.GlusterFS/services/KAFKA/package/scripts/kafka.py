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
from properties_config import properties_config
import sys
from copy import deepcopy

def kafka():
    import params

    Directory([params.log_dir, params.pid_dir, params.conf_dir],
              owner=params.kafka_user,
              group=params.user_group,
              recursive=True
          )
    brokerid = str(sorted(params.kafka_hosts).index(params.hostname))
    kafka_server_config = mutable_config_dict(params.config['configurations']['kafka-broker'])
    kafka_server_config['broker.id'] = brokerid
    kafka_server_config['host.name'] = params.hostname
    kafka_data_dir = kafka_server_config['log.dirs']
    Directory(filter(None,kafka_data_dir.split(",")),
              owner=params.kafka_user,
              group=params.user_group,
              recursive=True)

    conf_dir = params.conf_dir
    properties_config("server.properties",
                      conf_dir=params.conf_dir,
                      configurations=kafka_server_config,
                      owner=params.kafka_user,
                      group=params.user_group,
                      brokerid=brokerid)

    File(format("{conf_dir}/kafka-env.sh"),
          owner=params.kafka_user,
          content=InlineTemplate(params.kafka_env_sh_template)
     )

    if (params.log4j_props != None):
        File(format("{conf_dir}/log4j.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.kafka_user,
             content=params.log4j_props
         )


def mutable_config_dict(kafka_broker_config):
    kafka_server_config = {}
    for key, value in kafka_broker_config.iteritems():
        kafka_server_config[key] = value
    return kafka_server_config
