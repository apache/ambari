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
import common
import hawq_constants
from utils import exec_psql_cmd, exec_ssh_cmd
from resource_management.libraries.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


class HAWQServiceCheck(Script):
  """
  Runs a set of HAWQ tests to verify if the service has been setup correctly
  """

  def service_check(self, env):
    """
    Runs service check for HAWQ.
    """
    import params

    self.active_master_host = params.hawqmaster_host
    self.active_master_port = params.hawq_master_address_port
    self.checks_failed = 0
    self.total_checks = 2

    # Checks HAWQ cluster state
    self.check_state()

    # Runs check for writing and reading tables on HAWQ
    self.check_hawq()

    # Runs check for writing and reading external tables on HDFS using PXF, if PXF is installed
    if params.is_pxf_installed:
      self.total_checks += 1
      self.check_hawq_pxf_hdfs()
    else:
      Logger.info("PXF not installed. Skipping HAWQ-PXF checks...")

    if self.checks_failed != 0:
      Logger.error("** FAILURE **: Service check failed {0} of {1} checks".format(self.checks_failed, self.total_checks))
      sys.exit(1)

    Logger.info("Service check completed successfully")


  def check_state(self):
    """
    Checks state of HAWQ cluster
    """
    import params
    Logger.info("--- Check state of HAWQ cluster ---")
    try:
      command = "source {0} && hawq state -d {1}".format(hawq_constants.hawq_greenplum_path_file, params.hawq_master_dir)
      Logger.info("Executing hawq status check...")
      (retcode, out, err) = exec_ssh_cmd(self.active_master_host, command)
      if retcode:
        Logger.error("SERVICE CHECK FAILED: hawq state command returned non-zero result: {0}. Out: {1} Error: {2}".format(retcode, out, err))
        raise Fail("Unexpected result of hawq state command.")
      Logger.info("Output of command:\n{0}".format(str(out) + "\n"))
    except:
      self.checks_failed += 1


  def check_hawq(self):
    """
    Tests to check HAWQ
    """
    import params
    Logger.info("--- Check if HAWQ can write and query from a table ---")
    table = params.table_definition['HAWQ']
    try:
      self.drop_table(table)
      self.create_table(table)
      self.insert_data(table)
      self.query_data(table)
      self.validate_data(table)
    except:
      Logger.error("SERVICE CHECK FAILED: HAWQ was not able to write and query from a table")
      self.checks_failed += 1
    finally:
      self.drop_table(table)


  def check_hawq_pxf_hdfs(self):
    """
    Tests to check if HAWQ can write and read external tables on HDFS using PXF
    """
    import params
    Logger.info("--- Check if HAWQ can write and query from HDFS using PXF External Tables ---")
    table_writable = params.table_definition['EXTERNAL_HDFS_WRITABLE']
    table_readable = params.table_definition['EXTERNAL_HDFS_READABLE']
    try:
      self.delete_pxf_hdfs_test_dir()
      self.drop_table(table_writable)
      self.create_table(table_writable)
      self.insert_data(table_writable)
      self.drop_table(table_readable)
      self.create_table(table_readable)
      self.query_data(table_readable)
      self.validate_data(table_readable)
    except:
      Logger.error("SERVICE CHECK FAILED: HAWQ was not able to write and query from HDFS using PXF External Tables")
      self.checks_failed += 1
    finally:
      self.drop_table(table_readable)
      self.drop_table(table_writable)
      self.delete_pxf_hdfs_test_dir()


  def drop_table(self, table):
    Logger.info("Dropping {0} table if exists".format(table['name']))
    sql_cmd = "DROP {0} TABLE IF EXISTS {1}".format(table['drop_type'], table['name'])
    exec_psql_cmd(sql_cmd, self.active_master_host, self.active_master_port)


  def create_table(self, table):
    Logger.info("Creating table {0}".format(table['name']))
    sql_cmd = "CREATE {0} TABLE {1} {2}".format(table['create_type'], table['name'], table['description'])
    exec_psql_cmd(sql_cmd, self.active_master_host, self.active_master_port)


  def insert_data(self, table):
    Logger.info("Inserting data to table {0}".format(table['name']))
    sql_cmd = "INSERT INTO  {0} SELECT * FROM generate_series(1,10)".format(table['name'])
    exec_psql_cmd(sql_cmd, self.active_master_host, self.active_master_port)


  def query_data(self, table):
    Logger.info("Querying data from table {0}".format(table['name']))
    sql_cmd = "SELECT * FROM {0}".format(table['name'])
    exec_psql_cmd(sql_cmd, self.active_master_host, self.active_master_port)


  def validate_data(self, table):
    expected_data = "55"
    Logger.info("Validating data inserted, finding sum of all the inserted entries. Expected output: {0}".format(expected_data))
    sql_cmd = "SELECT sum(col1) FROM {0}".format(table['name'])
    _, stdout, _ = exec_psql_cmd(sql_cmd, self.active_master_host, self.active_master_port, tuples_only=False)
    if expected_data != stdout.strip():
      Logger.error("Incorrect data returned. Expected Data: {0} Actual Data: {1}".format(expected_data, stdout))
      raise Fail("Incorrect data returned.")


  def delete_pxf_hdfs_test_dir(self):
    import params
    params.HdfsResource(hawq_constants.pxf_hdfs_test_dir,
                        type="directory",
                        action="delete_on_execute")


if __name__ == "__main__":
  HAWQServiceCheck().execute()