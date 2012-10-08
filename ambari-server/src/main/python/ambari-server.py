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

import argparse
import sys
import os
import signal
import subprocess
import re

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
SETUP_DB_CMD = "psql -W -f{2} -d{0} --username {1}"
GET_SE_LINUX_ST_CMD = "sestatus"
SE_STATUS_DISABLED="disabled"
SE_STATUS_ENABLED="enabled"
IP_TBLS_ST_CMD = "service iptables status"
IP_TBLS_STOP_CMD = "service iptables stop"
IP_TBLS_ENABLED="Firewall is running"
IP_TBLS_DISABLED="Firewall is not running"
IP_TBLS_SRVC_NT_FND="iptables: unrecognized service"
SERVER_START_CMD="java -cp ../etc/ambari-server/conf:../lib/ambari-server/* org.apache.ambari.server.controller.AmbariServer"

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
  username = args.postgreuser
  file = args.init_script_file
  command = SETUP_DB_CMD.format(dbname, username, file)
  retcode, outdata, errdata = run_os_command(command)
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
  
def setup(args):
    
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
  server_process = subprocess.Popen(SERVER_START_CMD.split(' '))
  f = open("pid", "w")
  f.write(str(server_process.pid))
  f.close()
  
def stop(args):
  f = open("pid", "r")
  pid = int(f.readline())
  os.kill(pid, signal.SIGKILL)
  f.close()
  
  
  
  
def main():
  parser = argparse.ArgumentParser()
  parser.add_argument("action", help="action to perform with ambari server")
  parser.add_argument('-d', '--postgredbname', default='postgres',
                      help="Database name in postgresql")
  parser.add_argument('-u', '--postgreuser', default='postgres',
                      help="User in postgresql to run init scripts")
  parser.add_argument('-f', '--init-script-file', default='setup_db.sql',
                      help="File with setup script")
  args = parser.parse_args()
  action = args.action
  if action == SETUP_ACTION:
    setup(args)
  elif action == START_ACTION:
    start(args)
  elif action == STOP_ACTION:
    stop(args)
  else:
    print "Incorrect action"
    sys.exit(1)

if __name__ == "__main__":
  main()