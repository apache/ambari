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
from resource_management.libraries.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.system import System
from resource_management.core.resources.system import Execute

from pxf_utils import makeHTTPCall, runLocalCmd
import pxf_constants

class PXFServiceCheck(Script):
  """
  Runs a set of simple PXF tests to verify if the service has been setup correctly
  """
  pxf_version = None
  base_url = "http://" + pxf_constants.service_check_hostname + ":" + str(pxf_constants.PXF_PORT) + "/pxf/"
  commonPXFHeaders = {
    "X-GP-SEGMENT-COUNT": "1",
    "X-GP-URL-PORT": pxf_constants.PXF_PORT,
    "X-GP-SEGMENT-ID": "-1",
    "X-GP-HAS-FILTER": "0",
    "Accept": "application/json",
    "X-GP-ALIGNMENT": "8",
    "X-GP-ATTRS": "0",
    "X-GP-FORMAT": "TEXT",
    "X-GP-URL-HOST": pxf_constants.service_check_hostname
  }


  def service_check(self, env):
    Logger.info("Starting PXF service checks..")

    import params
    self.pxf_version = self.__get_pxf_protocol_version()
    try:
      self.run_hdfs_tests()
      if params.is_hbase_installed:
        self.run_hbase_tests()
      if params.is_hive_installed:
        self.run_hive_tests()
    except:
      msg = "PXF service check failed"
      Logger.error(msg)
      raise Fail(msg)
    finally:
      self.cleanup_test_data()

    Logger.info("Service check completed successfully")


  def cleanup_test_data(self):
    """
    Cleans up the temporary test data generated for service check
    """
    Logger.info("Cleaning up PXF smoke check temporary data")

    import params
    self.__cleanup_hdfs_data()
    if params.is_hbase_installed:
      self.__cleanup_hbase_data()
    if params.is_hive_installed:
      self.__cleanup_hive_data()


  def __get_pxf_protocol_version(self):
    """
    Gets the pxf protocol version number
    """
    Logger.info("Fetching PXF protocol version")
    url = self.base_url + "ProtocolVersion"
    response = makeHTTPCall(url)
    Logger.info(response)
    # Sample response: 'PXF protocol version v14'
    if response:
      import re
      # Extract the v14 from the output
      match =  re.search('.*(v\d*).*', response)
      if match:
         return match.group(1)      

    msg = "Unable to determine PXF version"
    Logger.error(msg)
    raise Fail(msg)


  def __check_pxf_read(self, headers):
    """
    Performs a generic PXF read
    """
    url = self.base_url + self.pxf_version + "/Fragmenter/getFragments?path="
    try:
      response = makeHTTPCall(url, headers)
      if not "PXFFragments" in response:
        Logger.error("Unable to find PXFFragments in the response")
        raise
    except:
      msg = "PXF data read failed"
      raise Fail(msg)


  # HDFS Routines
  def run_hdfs_tests(self):
    """
    Runs a set of PXF HDFS checks
    """
    Logger.info("Running PXF HDFS checks")
    self.__check_if_client_exists("Hadoop-HDFS")
    self.__cleanup_hdfs_data()
    self.__write_hdfs_data()
    self.__check_pxf_hdfs_read()
    self.__check_pxf_hdfs_write()

  def __write_hdfs_data(self):
    """
    Writes some test HDFS data for the tests
    """
    Logger.info("Writing temporary HDFS test data")
    import params
    params.HdfsResource(pxf_constants.pxf_hdfs_test_dir,
        type="directory",
        action="create_on_execute",
        mode=0777
        )

    params.HdfsResource(pxf_constants.pxf_hdfs_read_test_file,
        type="file",
        source="/etc/passwd",
        action="create_on_execute"
        )

  def __check_pxf_hdfs_read(self):
    """
    Reads the test HDFS data through PXF
    """
    Logger.info("Testing PXF HDFS read")
    headers = { 
        "X-GP-DATA-DIR": pxf_constants.pxf_hdfs_test_dir,
        "X-GP-profile": "HdfsTextSimple",
        }
    headers.update(self.commonPXFHeaders)
    self.__check_pxf_read(headers)

  def __check_pxf_hdfs_write(self):
    """
    Writes some test HDFS data through PXF
    """
    Logger.info("Testing PXF HDFS write")
    headers = self.commonPXFHeaders.copy()
    headers.update({
      "X-GP-Profile" : "HdfsTextSimple",
      "Content-Type":"application/octet-stream",
      "Expect": "100-continue",
      "X-GP-ALIGNMENT": "4",
      "X-GP-SEGMENT-ID": "0",
      "X-GP-SEGMENT-COUNT": "3",
      "X-GP-URI": "pxf://" + pxf_constants.service_check_hostname + ":" + str(pxf_constants.PXF_PORT) + pxf_constants.pxf_hdfs_test_dir + "/?Profile=HdfsTextSimple",
      "X-GP-DATA-DIR": pxf_constants.pxf_hdfs_test_dir + "/" 
    })

    body = {"Sample" : " text"}
    url = self.base_url + self.pxf_version + "/Writable/stream?path=" + pxf_constants.pxf_hdfs_write_test_file
    try:
      response = makeHTTPCall(url, headers, body)
      if not "wrote" in response:
        Logger.error("Unable to confirm write from the response")
        raise 
    except:
      msg = "PXF HDFS data write test failed"
      raise Fail(msg)

  def __cleanup_hdfs_data(self):
    """
    Cleans up the test HDFS data
    """
    Logger.info("Cleaning up temporary HDFS test data")
    import params
    params.HdfsResource(pxf_constants.pxf_hdfs_read_test_file,
        type="file",
        action="delete_on_execute"
        )
    params.HdfsResource(pxf_constants.pxf_hdfs_test_dir,
        type="directory",
        action="delete_on_execute"
        )


  # HBase Routines
  def run_hbase_tests(self):
    """
    Runs a set of PXF HBase checks
    """
    Logger.info("Running PXF HBase checks")
    self.__cleanup_hbase_data()
    self.__check_if_client_exists("HBase")
    self.__write_hbase_data()
    self.__check_pxf_hbase_read()

  def __write_hbase_data(self):
    """
    Creates a temporary HBase table for the service checks
    """
    Logger.info("Creating temporary HBase test data")
    Execute("echo \"create '" + pxf_constants.pxf_hbase_test_table + "', 'cf'\"|hbase shell", logoutput = True)
    Execute("echo \"put '" + pxf_constants.pxf_hbase_test_table + "', 'row1', 'cf:a', 'value1'; put '" + pxf_constants.pxf_hbase_test_table + "', 'row1', 'cf:b', 'value2'\" | hbase shell", logoutput = True)

  def __check_pxf_hbase_read(self):
    """
    Checks reading HBase data through PXF
    """
    Logger.info("Testing PXF HBase data read")
    headers = { 
        "X-GP-DATA-DIR": pxf_constants.pxf_hbase_test_table,
        "X-GP-profile": "HBase",
        }
    headers.update(self.commonPXFHeaders)

    self.__check_pxf_read(headers)

  def __cleanup_hbase_data(self):
    """
    Cleans up the test HBase data
    """
    Logger.info("Cleaning up HBase test data")
    Execute("echo \"disable '" + pxf_constants.pxf_hbase_test_table + "'\"|hbase shell > /dev/null 2>&1", logoutput = True)
    Execute("echo \"drop '" + pxf_constants.pxf_hbase_test_table + "'\"|hbase shell > /dev/null 2>&1", logoutput = True)


  # Hive Routines
  def run_hive_tests(self):
    """
    Runs a set of PXF Hive checks
    """
    Logger.info("Running PXF Hive checks")
    self.__check_if_client_exists("Hive")
    self.__cleanup_hive_data()
    self.__write_hive_data()
    self.__check_pxf_hive_read()

  def __write_hive_data(self):
    """
    Creates a temporary Hive table for the service checks
    """
    import params
    Logger.info("Creating temporary Hive test data")
    cmd = "hive -e 'CREATE TABLE IF NOT EXISTS {0} (id INT); INSERT INTO {0} VALUES (1);'".format(pxf_constants.pxf_hive_test_table)
    Execute(cmd, logoutput = True, user = params.hive_user)

  def __check_pxf_hive_read(self):
    """
    Checks reading Hive data through PXF
    """
    Logger.info("Testing PXF Hive data read")
    headers = {
        "X-GP-DATA-DIR": pxf_constants.pxf_hive_test_table,
        "X-GP-profile": "Hive",
        }
    headers.update(self.commonPXFHeaders)
    self.__check_pxf_read(headers)

  def __cleanup_hive_data(self):
    """
    Cleans up the test Hive data
    """
    import params
    Logger.info("Cleaning up Hive test data")
    cmd = "hive -e 'DROP TABLE IF EXISTS {0};'".format(pxf_constants.pxf_hive_test_table)
    Execute(cmd, logoutput = True, user = params.hive_user)


  # Package Routines
  def __package_exists(self, pkg):
    """
    Low level function to check if a rpm is installed
    """
    if System.get_instance().os_family == "suse":
      return not runLocalCmd("zypper search " + pkg)
    else:
      return not runLocalCmd("yum list installed | egrep -i ^" + pkg)


  def __check_if_client_exists(self, serviceName):
    Logger.info("Checking if " + serviceName + " client libraries exist")
    if not self.__package_exists(serviceName):
      error_msg = serviceName + " client libraries do not exist on the PXF node"
      Logger.error(error_msg)
      raise Fail(error_msg)


if __name__ == "__main__":
  PXFServiceCheck().execute()

