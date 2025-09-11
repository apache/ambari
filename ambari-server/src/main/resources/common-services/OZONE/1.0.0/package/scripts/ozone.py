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

import os
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import StaticFile, Template
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.exceptions import ComponentIsNotRunning

def ozone_service(name, action=None, upgrade_type=None):
  import params

  if action == 'configure':
    setup_ozone()
  elif action == 'start':
    start_ozone_service(name)
  elif action == 'stop':
    stop_ozone_service(name)

def setup_ozone():
  """
  Setup the Ozone service configuration
  """
  import params

  Logger.info("Setting up Ozone configuration")

  # Create ozone directories
  Directory([params.ozone_conf_dir, params.ozone_log_dir, params.ozone_pid_dir],
            owner=params.ozone_user,
            group=params.user_group,
            mode=0755,
            create_parents=True)

  # Create ozone-site.xml
  File(format("{ozone_conf_dir}/ozone-site.xml"),
       content=Template("ozone-site.xml.j2"),
       owner=params.ozone_user,
       group=params.user_group,
       mode=0644)

  # Create ozone-env.sh
  File(format("{ozone_conf_dir}/ozone-env.sh"),
       content=Template("ozone-env.sh.j2"),
       owner=params.ozone_user,
       group=params.user_group,
       mode=0755)

  # Create log4j configuration
  File(format("{ozone_conf_dir}/log4j.properties"),
       content=Template("log4j.properties.j2"),
       owner=params.ozone_user,
       group=params.user_group,
       mode=0644)

def setup_ozone_client():
  """
  Setup Ozone client configuration
  """
  import params
  
  Logger.info("Setting up Ozone client")
  
  # Create client configuration
  Directory(params.ozone_conf_dir,
            owner=params.ozone_user,
            group=params.user_group,
            mode=0755,
            create_parents=True)

def start_ozone_service(name):
  """
  Start the specified Ozone service
  """
  import params
  
  Logger.info(format("Starting Ozone {name}"))
  
  if name == 'om':
    pid_file = params.ozone_manager_pid_file
    start_cmd = format("{ozone_bin_dir}/ozone --daemon start om")
  elif name == 'datanode':
    pid_file = params.ozone_datanode_pid_file
    start_cmd = format("{ozone_bin_dir}/ozone --daemon start datanode")
  elif name == 'scm':
    pid_file = params.ozone_scm_pid_file
    start_cmd = format("{ozone_bin_dir}/ozone --daemon start scm")
  else:
    raise Exception(format("Unknown Ozone service: {name}"))

  Execute(start_cmd,
          user=params.ozone_user,
          logoutput=True)

def stop_ozone_service(name):
  """
  Stop the specified Ozone service
  """
  import params
  
  Logger.info(format("Stopping Ozone {name}"))
  
  if name == 'om':
    pid_file = params.ozone_manager_pid_file
    stop_cmd = format("{ozone_bin_dir}/ozone --daemon stop om")
  elif name == 'datanode':
    pid_file = params.ozone_datanode_pid_file
    stop_cmd = format("{ozone_bin_dir}/ozone --daemon stop datanode")
  elif name == 'scm':
    pid_file = params.ozone_scm_pid_file
    stop_cmd = format("{ozone_bin_dir}/ozone --daemon stop scm")
  else:
    raise Exception(format("Unknown Ozone service: {name}"))

  Execute(stop_cmd,
          user=params.ozone_user,
          logoutput=True,
          ignore_failures=True)

def get_ozone_datanode_status():
  """
  Get the status of Ozone DataNodes for decommission checking
  """
  import params
  
  try:
    # Execute ozone admin command to get DataNode status
    cmd = format("{ozone_bin_dir}/ozone admin -report -live")
    result = Execute(cmd,
                    user=params.ozone_user,
                    logoutput=True,
                    returns=[0])
    return result
  except Exception as e:
    Logger.warning(format("Failed to get Ozone DataNode status: {e}"))
    return None

def refresh_ozone_nodes():
  """
  Refresh the list of Ozone nodes
  """
  import params
  
  try:
    cmd = format("{ozone_bin_dir}/ozone admin -refreshNodes")
    Execute(cmd,
            user=params.ozone_user,
            logoutput=True)
    Logger.info("Successfully refreshed Ozone nodes")
  except Exception as e:
    Logger.error(format("Failed to refresh Ozone nodes: {e}"))
    raise e
