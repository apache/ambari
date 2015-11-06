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
import common
import constants
from utils import exec_psql_cmd, exec_ssh_cmd
from resource_management.libraries.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

import sys

class HAWQServiceCheck(Script):
  """
  Runs a set of simple HAWQ tests to verify if the service has been setup correctly
  """

  def __init__(self):
    self.active_master_host = common.get_local_hawq_site_property("hawq_master_address_host")


  def service_check(self, env):
    Logger.info("Starting HAWQ service checks..")
    # All the tests are run on the active_master_host using ssh irrespective of the node on which service check
    # is executed by Ambari
    try:
      self.check_state()
      self.drop_table()
      self.create_table()
      self.insert_data()
      self.query_data()
      self.check_data_correctness()
    except:
      Logger.error("Service check failed")
      sys.exit(1)
    finally:
      self.drop_table()

    Logger.info("Service check completed successfully")


  def drop_table(self):
    Logger.info("Dropping {0} table if exists".format(constants.smoke_check_table_name))
    sql_cmd = "drop table if exists {0}".format(constants.smoke_check_table_name)
    exec_psql_cmd(sql_cmd, self.active_master_host)


  def create_table(self):
    Logger.info("Creating table {0}".format(constants.smoke_check_table_name))
    sql_cmd = "create table {0} (col1 int) distributed randomly".format(constants.smoke_check_table_name)
    exec_psql_cmd(sql_cmd, self.active_master_host)


  def insert_data(self):
    Logger.info("Inserting data to table {0}".format(constants.smoke_check_table_name))
    sql_cmd = "insert into {0} select * from generate_series(1,10)".format(constants.smoke_check_table_name)
    exec_psql_cmd(sql_cmd, self.active_master_host)


  def query_data(self):
    Logger.info("Querying data from table {0}".format(constants.smoke_check_table_name))
    sql_cmd = "select * from {0}".format(constants.smoke_check_table_name)
    exec_psql_cmd(sql_cmd, self.active_master_host)


  def check_data_correctness(self):
    expected_data = "55"
    Logger.info("Validating data inserted, finding sum of all the inserted entries. Expected output: {0}".format(expected_data))
    sql_cmd = "select sum(col1) from {0}".format(constants.smoke_check_table_name)
    _, stdout, _ = exec_psql_cmd(sql_cmd, self.active_master_host, tuples_only=False)
    if expected_data != stdout.strip():
      Logger.error("Incorrect data returned. Expected Data: {0} Actual Data: {1}".format(expected_data, stdout))
      raise Fail("Incorrect data returned.")


  def check_state(self):
    import params
    command = "source {0} && hawq state -d {1}".format(constants.hawq_greenplum_path_file, params.hawq_master_dir)
    Logger.info("Executing hawq status check..")
    (retcode, out, err) = exec_ssh_cmd(self.active_master_host, command)
    if retcode:
      Logger.error("hawq state command returned non-zero result: {0}. Out: {1} Error: {2}".format(retcode, out, err))
      raise Fail("Unexpected result of hawq state command.")
    Logger.info("Output of command:\n{0}".format(str(out) + "\n"))


if __name__ == "__main__":
  HAWQServiceCheck().execute()
