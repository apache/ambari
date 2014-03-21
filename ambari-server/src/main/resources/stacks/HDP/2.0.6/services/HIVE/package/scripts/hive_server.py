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
from hive import hive
from hive_service import hive_service
import os
import fnmatch

class HiveServer(Script):

  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)

    hive(name='hiveserver2')

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    self.install_tez_jars(params) # Put tez jars in hdfs
    self.install_hive_exec_jar(params) # Put hive exec jar in hdfs
    hive_service( 'hiveserver2',
                  action = 'start'
    )

  def stop(self, env):
    import params
    env.set_params(params)

    hive_service( 'hiveserver2',
                  action = 'stop'
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = format("{hive_pid_dir}/{hive_pid}")
    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)

  def install_hive_exec_jar(self, params):
    hdfs_path_prefix = 'hdfs://'
    if params.tez_lib_uris:
      hdfs_path = params.hive_exec_hdfs_path

      if hdfs_path.strip().find(hdfs_path_prefix, 0) != -1:
        hdfs_path = hdfs_path.replace(hdfs_path_prefix, '')
      pass

      params.HdfsDirectory(hdfs_path,
                           action="create",
                           owner=params.hive_user,
                           mode=0755
      )

      if params.security_enabled:
        kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_user};")
      else:
        kinit_if_needed = ""

      if kinit_if_needed:
        Execute(kinit_if_needed,
                user=params.tez_user,
                path='/bin'
        )

      hive_exec_jar_path = self.find_hive_exec_jar_path(params.hive_lib)
      if hive_exec_jar_path is None:
        hive_exec_jar_path = params.hive_exec_jar_path
      pass

      CopyFromLocal(hive_exec_jar_path,
                    mode=0755,
                    owner=params.hive_user,
                    dest_dir=hdfs_path,
                    kinnit_if_needed=kinit_if_needed,
                    hdfs_user=params.hdfs_user
      )
    pass

  def find_hive_exec_jar_path(self, hive_lib_dir):
    if os.path.exists(hive_lib_dir) and os.path.isdir(hive_lib_dir):
      for file in os.listdir(hive_lib_dir):
        file_path = os.path.join(hive_lib_dir, file)
        if fnmatch.fnmatch(file, 'hive-exec*.jar') and not os.path.islink(file_path):
          return file_path
      pass
    pass

  def install_tez_jars(self, params):
    destination_hdfs_dirs = get_tez_hdfs_dir_paths(params.tez_lib_uris)

    # If tez libraries are to be stored in hdfs
    if destination_hdfs_dirs:
      for hdfs_dir in destination_hdfs_dirs:
        params.HdfsDirectory(hdfs_dir,
                            action="create_delayed",
                            owner=params.tez_user,
                            mode=0755
        )
      pass
      params.HdfsDirectory(None, action="create")

      if params.security_enabled:
        kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_user};")
      else:
        kinit_if_needed = ""

      if kinit_if_needed:
        Execute(kinit_if_needed,
                user=params.tez_user,
                path='/bin'
        )
      pass

      app_dir_path = None
      lib_dir_path = None

      if len(destination_hdfs_dirs) > 1:
        for path in destination_hdfs_dirs:
          if 'lib' in path:
            lib_dir_path = path
          else:
            app_dir_path = path
          pass
        pass
      pass

      if app_dir_path:
        CopyFromLocal(params.tez_local_api_jars,
                      mode=0755,
                      owner=params.tez_user,
                      dest_dir=app_dir_path,
                      kinnit_if_needed=kinit_if_needed,
                      hdfs_user=params.hdfs_user
        )
      pass

      if lib_dir_path:
        CopyFromLocal(params.tez_local_lib_jars,
                      mode=0755,
                      owner=params.tez_user,
                      dest_dir=lib_dir_path,
                      kinnit_if_needed=kinit_if_needed,
                      hdfs_user=params.hdfs_user
        )
      pass


def get_tez_hdfs_dir_paths(tez_lib_uris = None):
  hdfs_path_prefix = 'hdfs://'
  lib_dir_paths = []
  if tez_lib_uris and tez_lib_uris.strip().find(hdfs_path_prefix, 0) != -1:
    dir_paths = tez_lib_uris.split(',')
    for path in dir_paths:
      lib_dir_path = path.replace(hdfs_path_prefix, '')
      lib_dir_path = lib_dir_path if lib_dir_path.endswith(os.sep) else lib_dir_path + os.sep
      lib_dir_paths.append(lib_dir_path)
    pass
  pass

  return lib_dir_paths


if __name__ == "__main__":
  HiveServer().execute()
