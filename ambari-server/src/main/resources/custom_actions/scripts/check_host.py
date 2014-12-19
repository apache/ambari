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

Ambari Agent

"""

import os
import subprocess
import socket

from ambari_commons import os_utils
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.inet_utils import download_file
from resource_management import Script, Execute, format
from ambari_agent.HostInfo import HostInfo

CHECK_JAVA_HOME = "java_home_check"
CHECK_DB_CONNECTION = "db_connection_check"
CHECK_HOST_RESOLUTION = "host_resolution_check"
CHECK_LAST_AGENT_ENV = "last_agent_env_check"

DB_MYSQL = "mysql"
DB_ORACLE = "oracle"
DB_POSTGRESQL = "postgres"
DB_MSSQL = "mssql"

JDBC_DRIVER_MYSQL = "com.mysql.jdbc.Driver"
JDBC_DRIVER_ORACLE = "oracle.jdbc.driver.OracleDriver"
JDBC_DRIVER_POSTGRESQL = "org.postgresql.Driver"
JDBC_DRIVER_MSSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

JDBC_DRIVER_SYMLINK_MYSQL = "mysql-jdbc-driver.jar"
JDBC_DRIVER_SYMLINK_ORACLE = "oracle-jdbc-driver.jar"
JDBC_DRIVER_SYMLINK_POSTGRESQL = "postgres-jdbc-driver.jar"
JDBC_DRIVER_SYMLINK_MSSQL = "sqljdbc4.jar"
JDBC_AUTH_SYMLINK_MSSQL = "sqljdbc_auth.dll"

class CheckHost(Script):
  def actionexecute(self, env):
    config = Script.get_config()
    tmp_dir = Script.get_tmp_dir()

    #print "CONFIG: " + str(config)

    check_execute_list = config['commandParams']['check_execute_list']
    structured_output = {}

    # check each of the commands; if an unknown exception wasn't handled
    # by the functions, then produce a generic exit_code : 1
    if CHECK_JAVA_HOME in check_execute_list:
      try :
        java_home_check_structured_output = self.execute_java_home_available_check(config)
        structured_output[CHECK_JAVA_HOME] = java_home_check_structured_output
      except Exception, exception:
        print "There was an unexpected error while checking for the Java home location: " + str(exception)
        structured_output[CHECK_JAVA_HOME] = {"exit_code" : 1, "message": str(exception)}

    if CHECK_DB_CONNECTION in check_execute_list:
      try :
        db_connection_check_structured_output = self.execute_db_connection_check(config, tmp_dir)
        structured_output[CHECK_DB_CONNECTION] = db_connection_check_structured_output
      except Exception, exception:
        print "There was an unknown error while checking database connectivity: " + str(exception)
        structured_output[CHECK_DB_CONNECTION] = {"exit_code" : 1, "message": str(exception)}

    if CHECK_HOST_RESOLUTION in check_execute_list:
      try :
        host_resolution_structured_output = self.execute_host_resolution_check(config)
        structured_output[CHECK_HOST_RESOLUTION] = host_resolution_structured_output
      except Exception, exception :
        print "There was an unknown error while checking IP address lookups: " + str(exception)
        structured_output[CHECK_HOST_RESOLUTION] = {"exit_code" : 1, "message": str(exception)}
    if CHECK_LAST_AGENT_ENV in check_execute_list:
      try :
        last_agent_env_structured_output = self.execute_last_agent_env_check()
        structured_output[CHECK_LAST_AGENT_ENV] = last_agent_env_structured_output
      except Exception, exception :
        print "There was an unknown error while checking last host environment details: " + str(exception)
        structured_output[CHECK_LAST_AGENT_ENV] = {"exit_code" : 1, "message": str(exception)}

    self.put_structured_out(structured_output)


  def execute_java_home_available_check(self, config):
    print "Java home check started."
    java64_home = config['commandParams']['java_home']

    print "Java home to check: " + java64_home
    java_bin = "java"
    if OSCheck.is_windows_family():
      java_bin = "java.exe"
  
    if not os.path.isfile(os.path.join(java64_home, "bin", java_bin)):
      print "Java home doesn't exist!"
      java_home_check_structured_output = {"exit_code" : 1, "message": "Java home doesn't exist!"}
    else:
      print "Java home exists!"
      java_home_check_structured_output = {"exit_code" : 0, "message": "Java home exists!"}
  
    return java_home_check_structured_output


  def execute_db_connection_check(self, config, tmp_dir):
    print "DB connection check started."
  
    # initialize needed data
  
    ambari_server_hostname = config['commandParams']['ambari_server_host']
    check_db_connection_jar_name = "DBConnectionVerification.jar"
    jdk_location = config['commandParams']['jdk_location']
    java64_home = config['commandParams']['java_home']
    db_name = config['commandParams']['db_name']

    if db_name == DB_MYSQL:
      jdbc_url = jdk_location + JDBC_DRIVER_SYMLINK_MYSQL
      jdbc_driver = JDBC_DRIVER_MYSQL
      jdbc_name = JDBC_DRIVER_SYMLINK_MYSQL
    elif db_name == DB_ORACLE:
      jdbc_url = jdk_location + JDBC_DRIVER_SYMLINK_ORACLE
      jdbc_driver = JDBC_DRIVER_ORACLE
      jdbc_name = JDBC_DRIVER_SYMLINK_ORACLE
    elif db_name == DB_POSTGRESQL:
      jdbc_url = jdk_location + JDBC_DRIVER_SYMLINK_POSTGRESQL
      jdbc_driver = JDBC_DRIVER_POSTGRESQL
      jdbc_name = JDBC_DRIVER_SYMLINK_POSTGRESQL
    elif db_name == DB_MSSQL:
      jdbc_url = jdk_location + JDBC_DRIVER_SYMLINK_MSSQL
      jdbc_driver = JDBC_DRIVER_MSSQL
      jdbc_name = JDBC_DRIVER_SYMLINK_MSSQL
  
    db_connection_url = config['commandParams']['db_connection_url']
    user_name = config['commandParams']['user_name']
    user_passwd = config['commandParams']['user_passwd']
    agent_cache_dir = os.path.abspath(config["hostLevelParams"]["agentCacheDir"])
    check_db_connection_url = jdk_location + check_db_connection_jar_name
    jdbc_path = os.path.join(agent_cache_dir, jdbc_name)
    check_db_connection_path = os.path.join(agent_cache_dir, check_db_connection_jar_name)

    java_bin = "java"
    class_path_delimiter = ":"
    if OSCheck.is_windows_family():
      java_bin = "java.exe"
      class_path_delimiter = ";"

    java_exec = os.path.join(java64_home, "bin",java_bin)

    if ('jdk_name' not in config['commandParams'] or config['commandParams']['jdk_name'] == None \
        or config['commandParams']['jdk_name'] == '') and not os.path.isfile(java_exec):
      message = "Custom java is not available on host. Please install it. Java home should be the same as on server. " \
                "\n"
      print message
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output

    environment = { "no_proxy": format("{ambari_server_hostname}") }
    # download and install java if it doesn't exists
    if not os.path.isfile(java_exec):
      jdk_name = config['commandParams']['jdk_name']
      jdk_url = "{}/{}".format(jdk_location, jdk_name)
      jdk_download_target = os.path.join(agent_cache_dir, jdk_name)
      java_dir = os.path.dirname(java64_home)
      try:
        download_file(jdk_url, jdk_download_target)
      except Exception, e:
        message = "Error downloading JDK from Ambari Server resources. Check network access to " \
                  "Ambari Server.\n" + str(e)
        print message
        db_connection_check_structured_output = {"exit_code" : 1, "message": message}
        return db_connection_check_structured_output

      if jdk_name.endswith(".bin"):
        install_cmd = format("mkdir -p {java_dir} ; chmod +x {jdk_download_target}; cd {java_dir} ; echo A | " \
                           "{jdk_curl_target} -noregister > /dev/null 2>&1")
        install_path = ["/bin","/usr/bin/"]
      elif jdk_name.endswith(".gz"):
        install_cmd = format("mkdir -p {java_dir} ; cd {java_dir} ; tar -xf {jdk_download_target} > /dev/null 2>&1")
        install_path = ["/bin","/usr/bin/"]
      elif jdk_name.endswith(".exe"):
        install_cmd = "{} /s INSTALLDIR={} STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-agent".format(
          os_utils.quote_path(jdk_download_target), os_utils.quote_path(java64_home),
        )
        install_path = [java_dir]

      try:
        Execute(install_cmd, path = install_path)
      except Exception, e:
        message = "Error installing java.\n" + str(e)
        print message
        db_connection_check_structured_output = {"exit_code" : 1, "message": message}
        return db_connection_check_structured_output

    # download DBConnectionVerification.jar from ambari-server resources
    try:
      download_file(check_db_connection_url, check_db_connection_path)

    except Exception, e:
      message = "Error downloading DBConnectionVerification.jar from Ambari Server resources. Check network access to " \
                "Ambari Server.\n" + str(e)
      print message
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output
  
    # download jdbc driver from ambari-server resources
    try:
      download_file(jdbc_url, jdbc_path)
      if db_name == DB_MSSQL:
        jdbc_auth_path = os.path.join(agent_cache_dir, JDBC_AUTH_SYMLINK_MSSQL)
        jdbc_auth_url = jdk_location + JDBC_AUTH_SYMLINK_MSSQL
        download_file(jdbc_auth_url, jdbc_auth_path)
    except Exception, e:
      message = format("Error: Ambari Server cannot download the database JDBC driver and is unable to test the " \
                "database connection. You must run ambari-server setup --jdbc-db={db_name} " \
                "--jdbc-driver=/path/to/your/{db_name}/driver.jar on the Ambari Server host to make the JDBC " \
                "driver available for download and to enable testing the database connection.\n") + str(e)
      print message
      db_connection_check_structured_output = {"exit_code" : 1, "message": message}
      return db_connection_check_structured_output
  
  
    # try to connect to db
    db_connection_check_command = format("{java_exec} -cp {check_db_connection_path}{class_path_delimiter}" \
           "{jdbc_path} -Djava.library.path={agent_cache_dir} org.apache.ambari.server.DBConnectionVerification {db_connection_url} " \
           "{user_name} {user_passwd!p} {jdbc_driver}")
    print "INFO db_connection_check_command: " + db_connection_check_command
    process = subprocess.Popen(db_connection_check_command,
                               stdout=subprocess.PIPE,
                               stdin=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               shell=True)
    (stdoutdata, stderrdata) = process.communicate()
    print "INFO stdoutdata: " + stdoutdata
    print "INFO stderrdata: " + stderrdata
    print "INFO returncode: " + str(process.returncode)
  
    if process.returncode == 0:
      db_connection_check_structured_output = {"exit_code" : 0, "message": "DB connection check completed successfully!" }
    else:
      db_connection_check_structured_output = {"exit_code" : 1, "message":  stdoutdata + stderrdata }
  
    return db_connection_check_structured_output

  # check whether each host in the command can be resolved to an IP address
  def execute_host_resolution_check(self, config):
    print "IP address forward resolution check started."
    
    FORWARD_LOOKUP_REASON = "FORWARD_LOOKUP"
    
    failedCount = 0
    failures = []
   
    if config['commandParams']['hosts'] is not None :
      hosts = config['commandParams']['hosts'].split(",")
      successCount = len(hosts)
    else :
      successCount = 0
      hosts = ""
          
    socket.setdefaulttimeout(3)          
    for host in hosts:
      try:
        host = host.strip()        
        socket.gethostbyname(host)
      except socket.error,exception:
        successCount -= 1
        failedCount += 1
        
        failure = { "host": host, "type": FORWARD_LOOKUP_REASON, 
          "cause": exception.args }
        
        failures.append(failure)
  
    if failedCount > 0 :
      message = "There were " + str(failedCount) + " host(s) that could not resolve to an IP address."
    else :
      message = "All hosts resolved to an IP address." 
        
    print message
        
    host_resolution_check_structured_output = {
      "exit_code" : 0,
      "message" : message,                                          
      "failed_count" : failedCount, 
      "success_count" : successCount,
      "failures" : failures
      }
    
    return host_resolution_check_structured_output

  # computes and returns the host information of the agent
  def execute_last_agent_env_check(self):
    print "Last Agent Env check started."
    hostInfo = HostInfo()
    last_agent_env_check_structured_output = { }
    hostInfo.register(last_agent_env_check_structured_output)
    print "Last Agent Env check completed successfully."

    return last_agent_env_check_structured_output

if __name__ == "__main__":
  CheckHost().execute()
