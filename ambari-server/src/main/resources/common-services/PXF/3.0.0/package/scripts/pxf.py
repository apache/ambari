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

from resource_management import Script

from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script.config_dictionary import ConfigDictionary
from resource_management.core.resources.accounts import User
from resource_management.core.resources.system import Directory, File, Execute
from resource_management.core.source import Template



class Pxf(Script):
  """
  Contains the interface definitions for methods like install,
  start, stop, status, etc. for the PXF
  """

  def install(self, env):
    self.install_packages(env)
    self.configure(env)


  def configure(self, env):
    import params
    env.set_params(params)
    self.__setup_user_group()
    self.__generate_config_files()
    # pxf-service init exits safely when it is already initialized
    self.__execute_service_command("init")


  def start(self, env):
    self.configure(env)
    self.__grant_permissions()
    self.__execute_service_command("restart")


  def stop(self, env):
    self.__execute_service_command("stop")


  def restart(self, env):
    self.start(env)


  def status(self, env):
    try:
      self.__execute_service_command("status")
    except Exception:
      raise ComponentIsNotRunning()


  def __execute_service_command(self, command):
    import pxf_constants
    Execute("service {0} {1}".format(pxf_constants.pxf_service_name, command),
              timeout=pxf_constants.default_exec_timeout,
              logoutput=True)


  def __setup_user_group(self):
    """
    Creates PXF user with the required groups and bash as default shell
    """
    import params
    User(params.pxf_user,
         groups=[params.hdfs_superuser_group, params.user_group, params.tomcat_group],
         shell="/bin/bash")


  def __generate_config_files(self):
    """
    Generates pxf-env.sh file from jinja template and sets the classpath for HDP
    """
    import params
    import shutil

    hdp_stack = "HDP"

    # Create file pxf-env.sh from jinja template
    File("{0}/pxf-env.sh".format(params.pxf_conf_dir),
         content = Template("pxf-env.j2"))

    # Classpath is set for PHD by default. If stack is HDP, set classpath for HDP
    if(params.stack_name == hdp_stack):
      shutil.copy2("{0}/pxf-privatehdp.classpath".format(params.pxf_conf_dir),
                   "{0}/pxf-private.classpath".format(params.pxf_conf_dir))

    File('{0}/pxf-public.classpath'.format(params.pxf_conf_dir),
         content = params.config['configurations']['pxf-public-classpath']['content'].lstrip())

    File('{0}/pxf-profiles.xml'.format(params.pxf_conf_dir),
         content = params.config['configurations']['pxf-profiles']['content'].lstrip())
         
    # Default_value of principal => pxf/_HOST@{realm}
    XmlConfig("pxf-site.xml",
              conf_dir=params.pxf_conf_dir,
              configurations=params.config['configurations']['pxf-site'],
              configuration_attributes=params.config['configuration_attributes']['pxf-site'])


  def __grant_permissions(self):
    """
    Grants permission to pxf:pxf for PXF instance directory
    """
    import params
    Directory(params.pxf_instance_dir,
              owner=params.pxf_user,
              group=params.pxf_group,
              create_parents = True)


if __name__ == "__main__":
  Pxf().execute()
