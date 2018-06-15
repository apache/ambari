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
# Python Imports
import os
import shutil


# Ambari Commons & Resource Management Imports
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, Directory
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.format import format
from resource_management.libraries.script import Script

class HivePreUpgrade(Script):

  def backup_hive_metastore_database_local(self, env):
    import params
    env.set_params(params)

    self.__dump_mysql_db(True)

  def backup_hive_metastore_database_external(self, env):
    import params
    env.set_params(params)

    is_db_here = params.hostname in params.hive_jdbc_connection_url
    if params.hive_metastore_db_type == "mysql":
      self.__dump_mysql_db(is_db_here)
    elif params.hive_metastore_db_type == "postgres":
      self.__dump_postgres_db(is_db_here)
    elif params.hive_metastore_db_type == "oracle":
      self.__dump_oracle_db(is_db_here)
    else:
      raise Fail(format("Unknown db type: {hive_metastore_db_type}. Please create a db backup manually, then click on 'IGNORE AND PROCEED'"))

  def __dump_mysql_db(self, is_db_here):
    command = format("mysqldump {hive_db_schma_name} > {{dump_file}}")
    self.__dump_db(command, "mysql", is_db_here)

  def __dump_postgres_db(self, is_db_here):
    command = format("export PGPASSWORD='{hive_metastore_user_passwd}'; pg_dump -U {hive_metastore_user_name} {{hive_db_schma_name}} > {{dump_file}}")
    self.__dump_db(command, "postgres", is_db_here)

  def __dump_oracle_db(self, is_db_here):
    command = format("exp userid={hive_metastore_user_name}/{hive_metastore_user_passwd} full=y file={{dump_file}}")
    self.__dump_db(command, "oracle", is_db_here)

  def __dump_db(self, command, type, is_db_here):
    dump_dir = "/etc/hive/dbdump"
    dump_file = format("{dump_dir}/hive-{stack_version_formatted}-{type}-dump.sql")
    if is_db_here:
      if not os.path.exists(dump_dir):
        Directory(dump_dir)
      Execute(format(command), user = "root")
      Logger.info(format("Hive Metastore database backup created at {dump_file}"))
    else:
      Logger.warning(format("Hive Metastore is using an external {hive_metastore_db_type} database, the connection url is {hive_jdbc_connection_url}."))
      Logger.warning(format("Please log in to that host, and create a db backup manually by executing the following command: \"{command}\", then click on 'IGNORE AND PROCEED'"))
      raise Fail()

  def convert_tables(self, env):
    import params
    env.set_params(params)
    
    source_version = upgrade_summary.get_source_version(service_name = "HIVE")
    target_version = upgrade_summary.get_target_version(service_name = "HIVE")
    
    source_dir = format("/usr/hdp/{source_version}");
    target_dir = format("/usr/hdp/{target_version}")
    
    classpath = format("{source_dir}/hive2/lib/*:{source_dir}/hadoop/*:{source_dir}/hadoop/lib/*:{source_dir}/hadoop-mapreduce/*:{source_dir}/hadoop-mapreduce/lib/*:{target_dir}/hive/lib/hive-pre-upgrade.jar:{source_dir}/hive/conf")
    cmd = format("{java64_home}/bin/java -cp {classpath} org.apache.hadoop.hive.upgrade.acid.PreUpgradeTool -execute")
    Execute(cmd, user = "hive")

if __name__ == "__main__":
  HivePreUpgrade().execute()
