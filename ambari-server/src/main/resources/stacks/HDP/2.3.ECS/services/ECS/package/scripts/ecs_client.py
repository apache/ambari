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
import sys
import os
from resource_management import *

class ECSClient(Script):

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    self.setup_config(env)
    self.setup_hadoop_env(env)

  def createdirs(self, env):
    self.create_dirs(env)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def setup_config(self, env):
    import params
    env.set_params(params)
    stackversion = params.stack_version_unformatted

    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configuration_attributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              only_if=format("ls {hadoop_conf_dir}"))

    XmlConfig("hdfs-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['hdfs-site'],
              configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              only_if=format("ls {hadoop_conf_dir}"))

    File(format("{ambari_libs_dir}/fast-hdfs-resource.jar"),
           mode=0644,
           content=StaticFile("/var/lib/ambari-agent/cache/stacks/HDP/2.0.6/hooks/before-START/files/fast-hdfs-resource.jar")
    )

  def setup_hadoop_env(self, env):
    import params
    env.set_params(params)
    stackversion = params.stack_version_unformatted
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user

    # create /etc/hadoop
    Directory(params.hadoop_dir, mode=0755)

    # write out hadoop-env.sh, but only if the directory exists
    if os.path.exists(params.hadoop_conf_dir):
      File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'), owner=tc_owner,
        group=params.user_group,
        content=InlineTemplate(params.hadoop_env_sh_template))

    # Create tmp dir for java.io.tmpdir
    # Handle a situation when /tmp is set to noexec
    Directory(params.hadoop_java_io_tmpdir,
              owner=params.hdfs_user,
              group=params.user_group,
              mode=0777
    )

  def create_dirs(self,env):
    import params
    env.set_params(params)
    params.HdfsResource(params.hdfs_tmp_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       mode=0777
    )
    params.HdfsResource(params.smoke_hdfs_user_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode
    )
    params.HdfsResource(None,
                      action="execute"
    )

if __name__ == "__main__":
  ECSClient().execute()

