#!/usr/bin/env python2.6
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

Ambari Agent

"""

from resource_management import *
import sys


def zookeeper(type = None):
  import params

  Directory(params.config_dir,
            owner=params.zk_user,
            recursive=True,
            group=params.user_group
  )

  configFile("zoo.cfg", template_name="zoo.cfg.j2")
  configFile("zookeeper-env.sh", template_name="zookeeper-env.sh.j2")
  configFile("configuration.xsl", template_name="configuration.xsl.j2")

  Directory(params.zk_pid_dir,
            owner=params.zk_user,
            recursive=True,
            group=params.user_group
  )

  Directory(params.zk_log_dir,
            owner=params.zk_user,
            recursive=True,
            group=params.user_group
  )

  Directory(params.zk_data_dir,
            owner=params.zk_user,
            recursive=True,
            group=params.user_group
  )

  if type == 'server':
    myid = str(sorted(params.zookeeper_hosts).index(params.hostname) + 1)

    File(format("{zk_data_dir}/myid"),
         mode = 0644,
         content = myid
    )

  configFile("log4j.properties", template_name="log4j.properties.j2")

  if params.security_enabled:
    if type == "server":
      configFile("zookeeper_jaas.conf", template_name="zookeeper_jaas.conf.j2")
      configFile("zookeeper_client_jaas.conf", template_name="zookeeper_client_jaas.conf.j2")
    else:
      configFile("zookeeper_client_jaas.conf", template_name="zookeeper_client_jaas.conf.j2")

  File(format("{config_dir}/zoo_sample.cfg"),
       owner=params.zk_user,
       group=params.user_group
  )


def configFile(name, template_name=None):
  import params

  File(format("{config_dir}/{name}"),
       content=Template(template_name),
       owner=params.zk_user,
       group=params.user_group
  )




