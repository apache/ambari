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

import json
import os
import signal

from resource_management import *
from os.path import isfile


def nagios_service(action='start'): # start or stop
  import params
  
  nagios_pid_file = format("{nagios_pid_file}")

  if action == 'start':
    command = format("service {nagios_service_name} start")
    Execute(command)   
  elif action == 'stop':
    # attempt to grab the pid in case we need it later
    nagios_pid = 0    
    if isfile(nagios_pid_file):   
      with open(nagios_pid_file, "r") as file:
        try:
          nagios_pid = int(file.read())
          Logger.info("Nagios is running with a PID of {0}".format(nagios_pid))
        except:
          Logger.info("Unable to read PID file {0}".format(nagios_pid_file))
        finally:
          file.close()

    command = format("service {nagios_service_name} stop")  
    Execute(command)

    # on SUSE, there is a bug where Nagios doesn't kill the process 
    # but this could also affect any OS, so don't restrict this to SUSE
    if nagios_pid > 0:
      try:
        os.kill(nagios_pid, 0)
      except:
        Logger.info("The Nagios process has successfully terminated")
      else:
        Logger.info("The Nagios process with ID {0} failed to terminate; explicitly killing.".format(nagios_pid))
        os.kill(nagios_pid, signal.SIGKILL)

    # in the event that the Nagios scripts don't remove the pid file
    if isfile( nagios_pid_file ):   
      Execute(format("rm -f {nagios_pid_file}"))
        
  MonitorWebserver("restart")

def update_active_alerts():
  import status_params

  alerts = None
  if 'alerts' in status_params.config and status_params.config['alerts'] is not None:
    alerts = status_params.config['alerts']

  if alerts is None:
    return

  output = {}

  for a in alerts:
    alert_name = a['name']
    alert_text = a['text']
    alert_state = a['state']
    alert_host = a['host']
    if not output.has_key(alert_name):
      output[alert_name] = {}

    if not output[alert_name].has_key(alert_host):
      output[alert_name][alert_host] = []

    host_items = output[alert_name][alert_host]
    alert_out = {}
    alert_out['state'] = alert_state
    alert_out['text'] = alert_text
    host_items.append(alert_out)

  with open(os.path.join(status_params.nagios_var_dir, 'ambari.json'), 'w') as f:
    json.dump(output, f)

