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

import sys
from resource_management import *
from nagios import nagios
from nagios_service import nagios_service
from nagios_service import update_active_alerts

         
class NagiosServer(Script):
  def install(self, env):
    remove_conflicting_packages()
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)
    nagios()

    
  def start(self, env):
    import params
    env.set_params(params)

    update_ignorable(params)

    self.configure(env) # done for updating configs after Security enabled
    nagios_service(action='start')

    
  def stop(self, env):
    import params
    env.set_params(params)
    
    nagios_service(action='stop')


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.nagios_pid_file)

    # check for alert structures
    update_active_alerts()

    
def remove_conflicting_packages():  
  Package('hdp_mon_nagios_addons', action = "remove")

  Package('nagios-plugins', action = "remove")
  
  if System.get_instance().os_family in ["redhat","suse"]:
    Execute("rpm -e --allmatches --nopostun nagios",
      path  = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      ignore_failures = True)

def update_ignorable(params):
  if not params.config.has_key('passiveInfo'):
    return
  else:
    buf = ""
    count = 0
    for define in params.config['passiveInfo']:
      try:
        host = str(define['host'])
        service = str(define['service'])
        component = str(define['component'])
        buf += host + " " + service + " " + component + "\n"
        count += 1
      except KeyError:
        pass

    f = None
    try:
      f = open('/var/nagios/ignore.dat', 'w')
      f.write(buf)
      if 1 == count:
        Logger.info("Persisted '/var/nagios/ignore.dat' with 1 entry")
      elif count > 1:
        Logger.info("Persisted '/var/nagios/ignore.dat' with " + str(count) + " entries")
    except:
      Logger.info("Could not persist '/var/nagios/ignore.dat'")
      pass
    finally:
      if f is not None:
        f.close()


if __name__ == "__main__":
  NagiosServer().execute()
