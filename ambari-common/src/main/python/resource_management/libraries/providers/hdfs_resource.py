# !/usr/bin/env python
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
import json
from resource_management.core.environment import Environment
from resource_management.core.base import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format


JSON_PATH = '/var/lib/ambari-agent/data/hdfs_resources.json'
JAR_PATH = '/var/lib/ambari-agent/lib/fast-hdfs-resource.jar'

RESOURCE_TO_JSON_FIELDS = {
  'target': 'target',
  'type': 'type',
  'action': 'action',
  'source': 'source',
  'owner': 'owner',
  'group': 'group',
  'mode': 'mode',
  'recursive_chown': 'recursiveChown',
  'recursive_chmod': 'recursiveChmod',
  'change_permissions_for_parents': 'changePermissionforParents'
}


class HdfsResourceProvider(Provider):
  def action_delayed(self, action_name):
    resource = {}
    env = Environment.get_instance()
    if not 'hdfs_files' in env.config:
      env.config['hdfs_files'] = []

    # Check required parameters
    if not self.resource.type or not self.resource.action:
      raise Fail("Resource parameter type or action is not set.")

    # Put values in dictionary-resource
    for field_name, json_field_name in RESOURCE_TO_JSON_FIELDS.iteritems():
      if field_name == 'action':
        resource[json_field_name] = action_name
      elif field_name == 'mode' and self.resource.mode:
        resource[json_field_name] = oct(self.resource.mode)[1:]
      elif getattr(self.resource, field_name):
        resource[json_field_name] = getattr(self.resource, field_name)

    # Add resource to create
    env.config['hdfs_files'].append(resource)

  def action_create_on_execute(self):
    self.action_delayed("create")

  def action_delete_on_execute(self):
    self.action_delayed("delete")

  def action_execute(self):
    env = Environment.get_instance()

    # Check required parameters
    if not self.resource.user:
      raise Fail("Resource parameter 'user' is not set.")

    if not 'hdfs_files' in env.config or not env.config['hdfs_files']:
      Logger.info("No resources to create. 'create_on_execute' or 'delete_on_execute' wasn't triggered before this 'execute' action.")
      return

    hadoop_bin_dir = self.resource.hadoop_bin_dir
    hadoop_conf_dir = self.resource.hadoop_conf_dir
    user = self.resource.user
    security_enabled = self.resource.security_enabled
    keytab_file = self.resource.keytab
    kinit_path = self.resource.kinit_path_local
    logoutput = self.resource.logoutput
    jar_path=JAR_PATH
    json_path=JSON_PATH

    if security_enabled:
      Execute(format("{kinit_path} -kt {keytab_file} {hdfs_principal_name}"),
              user=user
      )

    # Write json file to disk
    File(JSON_PATH,
         owner = user,
         content = json.dumps(env.config['hdfs_files'])
    )

    # Execute jar to create/delete resources in hadoop
    Execute(format("hadoop --config {hadoop_conf_dir} jar {jar_path} {json_path}"),
            user=user,
            path=[hadoop_bin_dir],
            logoutput=logoutput,
    )

    # Clean
    env.config['hdfs_files'] = []
