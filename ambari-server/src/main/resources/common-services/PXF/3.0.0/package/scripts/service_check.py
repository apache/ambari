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
import json
import os
import sys

from resource_management.libraries.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.system import System
from resource_management.core.resources.system import Execute, File
from resource_management.core.environment import Environment
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.core.source import InlineTemplate
from resource_management.libraries.functions.default import default
from pxf_utils import makeHTTPCall, runLocalCmd
import pxf_constants

GP_PROFILE= "X-GP-profile"
GP_DATA_DIR = "X-GP-DATA-DIR"

class PXFServiceCheck(Script):
  """
  Runs a set of simple PXF tests to verify if the service has been setup correctly
  """
  pxf_version = None
  gp_profile = "X-GP-profile"
  gp_data_dir = "X-GP-DATA-DIR"
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
    """
    Runs the service check for PXF
    """
    import params
    self.total_tests = 0
    self.checks_failed = 0
    Logger.info("Starting PXF service checks")

    # Get delegation token if security is enabled
    if params.security_enabled:
      self.total_tests +=1
      token = self.__get_delegation_token(params.hdfs_user, params.hdfs_user_keytab,
                                          params.hdfs_principal_name, params.kinit_path_local)
      self.commonPXFHeaders.update({"X-GP-TOKEN": token})

    self.total_tests +=1
    self.pxf_version = self.__get_pxf_protocol_version()

    self.total_tests +=1
    self.run_hdfs_tests()
    if params.is_hbase_installed:
      self.total_tests +=1
      self.run_hbase_tests()
    if params.is_hive_installed:
      self.total_tests +=1
      self.run_hive_tests()

    if self.checks_failed:
      Logger.error("***FAILURE*** - {0} out of {1} tests failed.".format(self.checks_failed,self.total_tests))
      sys.exit(1)
    Logger.info("Service check completed successfully")


  def __get_pxf_protocol_version(self):
    """
    Gets the pxf protocol version number
    """
    Logger.info("Fetching PXF protocol version")
    url = self.base_url + "ProtocolVersion"
    response = makeHTTPCall(url)
    Logger.info(response)
    # Sample response: 'PXF protocol version v14'
    if not response:
      error_msg = "Fetch PXF version: Could not get response from \nurl = {0}".format(url)
      Logger.error(error_msg)

    import re
    # Extract the v14 from the output
    match =  re.search('.*(v\d*).*', response)
    if match:
      return match.group(1)

    error_msg = "Fetch PXF Version: Could not find version in \nresponse ={0} \nurl = {1}".format(response, url)
    Logger.error(error_msg)

  def __check_pxf_read(self, headers):
    """
    Performs a generic PXF read
    """
    url = self.base_url + self.pxf_version + "/Fragmenter/getFragments?path="
    try:
      response = makeHTTPCall(url, headers)
      if not "PXFFragments" in response:
        Logger.error("Unable to find PXFFragments in the response. Response received from the server:\n{0}".format(response))
        raise
    except Exception, ex:
      raise Fail("PXF data read failed: {0}".format(ex))
    Logger.info("PXF data read successful")


  def __get_delegation_token(self, user, keytab, principal, kinit_path):
    """
    Gets the kerberos delegation token from name node
    """
    import params
    url = params.namenode_path + "/webhdfs/v1/?op=GETDELEGATIONTOKEN"
    Logger.info("Getting delegation token from {0}".format(url))
    response, _, _  = curl_krb_request(Environment.get_instance().tmp_dir, keytab, principal,
        url, "get_delegation_token", kinit_path, False, "Delegation Token", user)
    json_response = json.loads(response)
    if json_response['Token'] and json_response['Token']['urlString']:
      return json_response['Token']['urlString']

    error_msg = "Get Token: Unable to get kerberos delegation token from webhdfs: \nurl = {0}, user = {1}, keytab = {2}, principal = {3}, kinit-path = {4} \nresponse = {5}".format(url, user, keytab, principal, kinit_path, json_response)
    Logger.error(error_msg)
    self.checks_failed += 1

  def __check_yarn_installed(self):
    """
    Checks if any YARN/MAPREDUCE2 component is installed on this host
    """
    component_list = default("/localComponents", [])
    Logger.info("Checking for YARN/MAPREDUCE2 components in {0}".format(str(component_list)))
    return any([component in ["RESOURCEMANAGER", "NODEMANAGER", "HISTORYSERVER", "YARN_CLIENT", "MAPREDUCE2_CLIENT"] for component in component_list])

  # HDFS Routines
  def run_hdfs_tests(self):
    """
    Runs a set of PXF HDFS checks
    """
    import params
    Logger.info("Running PXF HDFS service checks")

    # YARN is required to access HDFS through PXF if security is enabled
    if params.security_enabled and not self.__check_yarn_installed():
      self.checks_failed += 1
      Logger.error("HDFS test prerequisite Failed: PXF in a Kerberos-secured cluster requires a YARN component to be installed on this host due to a dependency on YARN libraries.")
      return

    try:
      self.__check_if_client_exists("Hadoop-HDFS")
      self.__cleanup_hdfs_data()
      self.__write_hdfs_data()
      self.__check_pxf_hdfs_read()
      self.__check_pxf_hdfs_write()

    except Exception, ex:
      self.checks_failed += 1
      Logger.error("HDFS test Failed: Exception occurred in HDFS test: {0}".format(ex))

    finally:
      self.__cleanup_hdfs_data()

  def __write_hdfs_data(self):
    """
    Writes some test HDFS data for the tests
    """
    Logger.info("Writing temporary HDFS test data")
    try:
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
      params.HdfsResource(None, action="execute")
    except Exception, ex:
      raise Fail("HDFS Write: Exception occurred when writing to hdfs:  {0} ".format(ex))

  def __check_pxf_hdfs_read(self):
    """
    Reads the test HDFS data through PXF
    """
    Logger.info("Testing PXF HDFS read")
    headers = {
        GP_DATA_DIR: pxf_constants.pxf_hdfs_test_dir,
        GP_PROFILE: "HdfsTextSimple",
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
        error_msg = "PXF HDFS data write: Could not find write in response : \nurl = {0} \nresponse = {1}".format(url, response)
        raise Fail(error_msg)
    except:
      error_msg = "PXF HDFS data write test failed with url= {0}".format(url)
      raise Fail(error_msg)

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
    params.HdfsResource(None, action="execute")

  # HBase Routines
  def run_hbase_tests(self):
    """
    Runs a set of PXF HBase checks
    """
    import params
    Logger.info("Running PXF HBase service checks")
    self.__check_if_client_exists("HBase")
    self.__create_hbase_scripts()
    kinit_cmd = "{0} -kt {1} {2};".format(params.kinit_path_local, params.hbase_user_keytab, params.hbase_principal_name) if params.security_enabled else ""
    try:
      message = "Creating temporary HBase smoke test table with data"
      self.__run_hbase_script(pxf_constants.hbase_populate_data_script, kinit_cmd, message)
      self.__check_pxf_hbase_read()

    except Exception, ex:
      self.checks_failed += 1
      Logger.error("HBASE test Failed: Exception occurred in HBASE test: {0}".format(ex))

    finally:
      message = "Cleaning up HBase smoke test table"
      self.__run_hbase_script(pxf_constants.hbase_cleanup_data_script, kinit_cmd, message)

  def __create_hbase_scripts(self):
    """
    Create file holding hbase commands
    """
    try:
      import params
      hbase_populate_data_cmds = "disable '{0}'\n" \
                                 "drop '{0}'\n" \
                                 "create '{0}', 'cf'\n" \
                                 "put '{0}', 'row1', 'cf:a', 'value1'\n" \
                                 "put '{0}', 'row1', 'cf:b', 'value2'".format(pxf_constants.pxf_hbase_test_table)

      File("{0}".format(os.path.join(params.exec_tmp_dir, pxf_constants.hbase_populate_data_script)),
           content=InlineTemplate("{0}".format(hbase_populate_data_cmds)))

      hbase_cleanup_data_cmds = "disable '{0}'\n" \
                                "drop '{0}'".format(pxf_constants.pxf_hbase_test_table)

      File("{0}".format(os.path.join(params.exec_tmp_dir, pxf_constants.hbase_cleanup_data_script)),
           content=InlineTemplate("{0}".format(hbase_cleanup_data_cmds)))

    except Exception, ex:
      raise Fail("Create HBASE Script: Could not create hbase_scripts: {0}".format(ex))

  def __run_hbase_script(self, script, kinit_cmd, message):
    """
    Executes hbase shell command
    """
    import params
    Logger.info(message)
    hbase_shell_cmd = "{0} cat {1} ; hbase shell {1}".format(kinit_cmd, os.path.join(params.exec_tmp_dir, script))
    Execute(hbase_shell_cmd, user=params.hbase_user, logoutput=True)

  def __check_pxf_hbase_read(self):
    """
    Checks reading HBase data through PXF
    """
    Logger.info("Testing PXF HBase data read")
    headers = {
        GP_DATA_DIR: pxf_constants.pxf_hbase_test_table,
        GP_PROFILE: "HBase",
        }
    headers.update(self.commonPXFHeaders)
    self.__check_pxf_read(headers)

  # Hive Routines
  def run_hive_tests(self):
    """
    Runs a set of PXF Hive checks
    """
    import params
    Logger.info("Running PXF Hive service checks")
    self.__check_if_client_exists("Hive")

    # Create file holding hive query commands
    hive_populate_data_cmds = "DROP TABLE IF EXISTS {0};\n" \
                         "CREATE TABLE {0} (id INT);\n" \
                         "INSERT INTO {0} VALUES (1);".format(pxf_constants.pxf_hive_test_table)
    File("{0}/{1}".format(params.exec_tmp_dir, pxf_constants.hive_populate_data_script),
         content=InlineTemplate("{0}".format(hive_populate_data_cmds)))

    # Get the parameters required to create jdbc url for beeline
    hive_server_port = default("/configurations/hive-site/hive.server2.thrift.port", None)
    hive_server_host = default("/clusterHostInfo/hive_server_host", None)
    if hive_server_host is None or hive_server_port is None:
      raise Fail("Input parameters are invalid for beeline connection string, both hive_server_host and " \
            "hive.server2.thrift.port should be not None. Current values are:\nhive_server_host={0}\n" \
            "hive.server2.thrift.port={1}".format(hive_server_host, hive_server_port))
    jdbc_url = "jdbc:hive2://{0}:{1}/default".format(hive_server_host[0], hive_server_port)
    beeline_conn_cmd = "beeline -u '{0}'".format(jdbc_url)

    if params.security_enabled:
      hive_server_principal = default('/configurations/hive-site/hive.server2.authentication.kerberos.principal', None)
      if hive_server_principal is None:
        raise Fail("Input parameter invalid for beeline connection string, hive.server2.authentication.kerberos.principal " \
              "should be not None")
      beeline_conn_cmd = "beeline -u '{0};principal={1}'".format(jdbc_url, hive_server_principal)

    try:
      self.__write_hive_data(beeline_conn_cmd)
      self.__check_pxf_hive_read()

    except Exception, ex:
      self.checks_failed += 1
      Logger.error("HIVE test Failed: Exception occurred in HIVE test: {0}".format(ex))

    finally:
      self.__cleanup_hive_data(beeline_conn_cmd)

  def __write_hive_data(self, beeline_conn_cmd):
    """
    Creates a temporary Hive table for the service checks
    """
    try:
      import params
      Logger.info("Creating temporary Hive smoke test table with data")
      cmd = "{0} -f {1}".format(beeline_conn_cmd, os.path.join(params.exec_tmp_dir, pxf_constants.hive_populate_data_script))
      Execute(cmd, logoutput=True, user=params.hdfs_user)

    except Exception, ex:
      raise Fail("HIVE write: Could not write hive data: {0} \n command = {1}".format(ex, cmd))

  def __check_pxf_hive_read(self):
    """
    Checks reading Hive data through PXF
    """
    Logger.info("Testing PXF Hive data read")
    headers = {
        GP_DATA_DIR: pxf_constants.pxf_hive_test_table,
        GP_PROFILE: "Hive",
        }
    headers.update(self.commonPXFHeaders)
    self.__check_pxf_read(headers)

  def __cleanup_hive_data(self, beeline_conn_cmd):
    """
    Cleans up the test Hive data
    """
    import params
    Logger.info("Cleaning up Hive test data")
    cmd = "{0} -e 'DROP TABLE IF EXISTS {1};'".format(beeline_conn_cmd, pxf_constants.pxf_hive_test_table)
    Execute(cmd, logoutput=True, user=params.hdfs_user)

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
      raise Fail(error_msg)

if __name__ == "__main__":
  PXFServiceCheck().execute()
