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
import os
from resource_management import *

def setup_hdp_install_directory():
  # This is a name of marker file.
  SELECT_ALL_PERFORMED_MARKER = "/var/lib/ambari-agent/data/hdp-select-set-all.performed"
  import params
  if params.hdp_stack_version != "" and compare_versions(params.stack_version_unformatted, '2.2') >= 0:
    Execute(as_sudo(['touch', SELECT_ALL_PERFORMED_MARKER]) + ' ; ' +
                   format('{sudo} /usr/bin/hdp-select set all `ambari-python-wrap /usr/bin/hdp-select versions | grep ^{stack_version_unformatted} | tail -1`'),
            only_if=format('ls -d /usr/hdp/{stack_version_unformatted}*'),   # If any HDP version is installed
            not_if=format("test -f {SELECT_ALL_PERFORMED_MARKER}")           # Do that only once (otherwise we break rolling upgrade logic)
    )

def setup_config():
  import params
  stackversion = params.stack_version_unformatted
  if params.has_namenode or stackversion.find('Gluster') >= 0:
    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configuration_attributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group
    )
