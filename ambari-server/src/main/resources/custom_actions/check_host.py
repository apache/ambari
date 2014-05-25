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

from resource_management import Script, Execute

CHECK_JAVA_HOME = "java_home_check"
CHECK_DB_CONNECTION = "db_connection_check"
CHECK_HOST_RESOLUTION = "host_resolution_check"

DB_NAME_MYSQL = "mysql"
DB_NAME_ORACLE = "oracle"
DB_NAME_POSTGRESQL = "postgresql"

JDBC_DRIVER_MYSQL = "com.mysql.jdbc.Driver"
JDBC_DRIVER_ORACLE = "oracle.jdbc.driver.OracleDriver"
JDBC_DRIVER_POSTGRESQL = "org.postgresql.Driver"


class CheckHost(Script):
  def actionexecute(self, env):
    config = Script.get_config()

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
        structured_output[CHECK_JAVA_HOME] = {"exit_code" : "1", "message": str(exception)}

    if CHECK_DB_CONNECTION in check_execute_list:
      try :
        db_connection_check_structured_output = self.execute_db_connection_check(config)
        structured_output[CHECK_DB_CONNECTION] = db_connection_check_structured_output
      except Exception, exception:
        print "There was an unknown error while checking database connectivity: " + str(exception)
        structured_output[CHECK_DB_CONNECTION] = {"exit_code" : "1", "message": str(exception)}

    if CHECK_HOST_RESOLUTION in check_execute_list:
      try : 
        host_resolution_structured_output = self.execute_host_resolution_check(config)
        structured_output[CHECK_HOST_RESOLUTION] = host_resolution_structured_output
      except Exception, exception :
        print "There was an unknown error while checking IP address lookups: " + str(exception)
        structured_output[CHECK_HOST_RESOLUTION] = {"exit_code" : "1", "message": str(exception)}

    self.put_structured_out(structured_output)


  def execute_java_home_available_check(self, config):
    print "Java home check started."
    java64_home = config['hostLevelParams']['java_home']
  
    if not os.path.isfile(os.path.join(java64_home, "bin", "java")):
      print "Java home doesn't exist!"
      java_home_check_structured_output = {"exit_code" : "1", "message": "Java home doesn't exist!"}
    else:
      print "Java home exists!"
      java_home_check_structured_output = {"exit_code" : "0", "message": "Java home exists!"}
  
    return java_home_check_structured_output


  def execute_db_connection_check(self, config):
    print "DB connection check started."
  
    # initialize needed data
  
    ambari_server_hostname = config['clusterHostInfo']['ambari_server_host']
    check_db_connection_jar_name = "DBConnectionVerification.jar"
    jdk_location = config['hostLevelParams']['jdk_location']
    java64_home = config['hostLevelParams']['java_home']
    db_name = config['commandParams']['db_name']
  
    if db_name == DB_NAME_MYSQL:
      jdbc_url = config['hostLevelParams']['mysql_jdbc_url']
      jdbc_driver = JDBC_DRIVER_MYSQL
    elif db_name == DB_NAME_ORACLE:
      jdbc_url = config['hostLevelParams']['oracle_jdbc_url']
      jdbc_driver = JDBC_DRIVER_ORACLE
    elif db_name == DB_NAME_POSTGRESQL:
      jdbc_url = config['hostLevelParams']['postgresql_jdbc_url']
      jdbc_driver = JDBC_DRIVER_POSTGRESQL
  
    path, jdbc_name = os.path.split(jdbc_url)
    db_connection_url = config['commandParams']['db_connection_url']
    user_name = config['commandParams']['user_name']
    user_passwd = config['commandParams']['user_passwd']
  
    environment = { "no_proxy": format("{ambari_server_hostname}") }
  
    # download DBConnectionVerification.jar from ambari-server resources
  
    try:
      cmd = format("/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf "
                   "--retry 5 {jdk_location}{check_db_connection_jar_name} "
                   "-o {check_db_connection_jar_name}'")
      Execute(cmd, not_if=format("[ -f /usr/lib/ambari-agent/{check_db_connection_jar_name}]"), environment = environment)
    except Exception, e:
      message = "Error downloading DBConnectionVerification.jar from Ambari Server resources. Check network access to " \
                "Ambari Server.\n" + str(e)
      print message
      db_connection_check_structured_output = {"exit_code" : "1", "message": message}
      return db_connection_check_structured_output
  
    # download jdbc driver from ambari-server resources
  
    try:
      cmd = format("/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf "
                   "--retry 5 {jdbc_url} -o {jdbc_name}'")
      Execute(cmd, not_if=format("[ -f /usr/lib/ambari-agent/{jdbc_name}]"), environment = environment)
    except Exception, e:
      message = "Error downloading JDBC connector from Ambari Server resources. Check network access to " \
                "Ambari Server.\n" + str(e)
      print message
      db_connection_check_structured_output = {"exit_code" : "1", "message": message}
      return db_connection_check_structured_output
  
  
    # try to connect to db
  
    db_connection_check_command = format("{java64_home}/bin/java -cp /usr/lib/ambari-agent/{check_db_connection_jar_name}:" \
           "/usr/lib/ambari-agent/{jdbc_name} org.apache.ambari.server.DBConnectionVerification {db_connection_url} " \
           "{user_name} {user_passwd!p} {jdbc_driver}")
  
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
      db_connection_check_structured_output = {"exit_code" : "0", "message": "DB connection check completed successfully!" }
    else:
      db_connection_check_structured_output = {"exit_code" : "1", "message":  stdoutdata }
  
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
      "exit_code" : "0",
      "message" : message,                                          
      "failed_count" : failedCount, 
      "success_count" : successCount,
      "failures" : failures
      }
    
    return host_resolution_check_structured_output

if __name__ == "__main__":
  CheckHost().execute()
