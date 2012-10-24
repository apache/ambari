#!/usr/bin/env python

'''
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
'''

import optparse
import sys
import os
import signal
import subprocess
import re

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
SETUP_DB_CMD = "sudo -u postgres psql -f {1} -d {0}"
GET_SE_LINUX_ST_CMD = "sestatus"
SE_STATUS_DISABLED="disabled"
SE_STATUS_ENABLED="enabled"
IP_TBLS_ST_CMD = "service iptables status"
IP_TBLS_STOP_CMD = "service iptables stop"
IP_TBLS_ENABLED="Firewall is running"
IP_TBLS_DISABLED="Firewall is not running"
IP_TBLS_SRVC_NT_FND="iptables: unrecognized service"
SERVER_START_CMD="java -cp {0}"+ os.pathsep + ".." + os.sep + "lib" + os.sep + "ambari-server" + os.sep + "* org.apache.ambari.server.controller.AmbariServer"
AMBARI_CONF_VAR="AMBARI_CONF_DIR"
PG_ST_CMD = "service postgresql status"
PG_START_CMD = "service postgresql start"
PG_STATUS_RUNNING = "running"
PID_DIR="/var/run/ambari-server"
PID_NAME="ambari-server.pid"

def run_os_command(cmd):
  print 'about to run command: ' + cmd
  process = subprocess.Popen(cmd.split(' '),
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE
                            )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata

def setup_db(args):
  dbname = args.postgredbname
  file = args.init_script_file
  command = SETUP_DB_CMD.format(dbname, file)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode

def check_se_down():
  retcode, out, err = run_os_command(GET_SE_LINUX_ST_CMD)
  se_status = re.search('(disabled|enabled)', out).group(0)
  print 'SELinux status:' + se_status
  if se_status == SE_STATUS_DISABLED:
    return 0
  else:
    return 1

def ip_tables_down():
  retcode, out, err = run_os_command(IP_TBLS_ST_CMD)
  if out == IP_TBLS_ENABLED:
    print 'iptables is enabled now'
    print 'Stopping iptables service'
    retcode, out, err = run_os_command(IP_TBLS_STOP_CMD)
  print 'iptables is disabled now'
  
  if not retcode == 0:
    print err
  
  if err.strip() == IP_TBLS_SRVC_NT_FND:
    return 0
  else:
   return retcode

def check_postgre_up():
  retcode, out, err = run_os_command(PG_ST_CMD)
  try:
    pg_status = re.search('(stopped|running)', out).group(0)
  except AttributeError:
    pg_status = None
  if pg_status == PG_STATUS_RUNNING:
    print "Postgre is running"
    return 0
  else:
    print "About to start postgre"
    retcode, out, err = run_os_command(PG_START_CMD)
    return retcode
  
def setup(args):
    
  print 'About to check Postgre'
  retcode = check_postgre_up()
  if not retcode == 0:
    print 'Error! Unable to start postgre server'
    sys.exit(retcode)
    
  print 'About to setup database'
  retcode = setup_db(args)
  if not retcode == 0:
    print 'Error! Running database init script was failed'
    sys.exit(retcode)
  
  print 'About to check SELinux down'
  retcode = check_se_down()
  if not retcode == 0:
    print 'Error! SELinux is enabled, unable to proceed'
    sys.exit(retcode)
   
  print 'About to check iptables down'
  retcode = ip_tables_down()
  if (not retcode == 0):
    print 'Error! Failed to stop iptables'
    sys.exit(retcode)
    
  print 'Setup was finished sucessfully'
  
def start(args):
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
  except KeyError:
    print "Please set value of " + AMBARI_CONF_VAR + "!"
    sys.exit(1)
      
  retcode = check_postgre_up()
  if not retcode == 0:
    print 'Error! Unable to start postgre server'
    sys.exit(retcode)
  
  command = SERVER_START_CMD.format(conf_dir)
      
  server_process = subprocess.Popen(command.split(' '))
  f = open(PID_DIR + os.sep + PID_NAME, "w")
  f.write(str(server_process.pid))
  f.close()
  
def stop(args):
  f = open(PID_DIR + os.sep + PID_NAME, "r")
  pid = int(f.readline())
  os.kill(pid, signal.SIGKILL)
  f.close()
  os.remove(f.name)
  
  
  
  
  
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action",)
  parser.add_option('-d', '--postgredbname', default='postgres',
                      help="Database name in postgresql")
  parser.add_option('-f', '--init-script-file', default='setup_db.sql',
                      help="File with setup script")

  (options, args) = parser.parse_args()
  
  if not len(args) == 1:
    print parser.print_help()
    parser.error("Invalid number of arguments")
	
  action = args[0]

  if action == SETUP_ACTION:
    setup(options)
  elif action == START_ACTION:
    start(options)
  elif action == STOP_ACTION:
    stop(options)
  else:
    parser.error("Invalid action")

if __name__ == "__main__":
  main()
