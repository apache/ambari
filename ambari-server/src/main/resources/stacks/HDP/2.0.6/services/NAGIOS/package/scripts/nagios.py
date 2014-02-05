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

from resource_management import *
from nagios_server_config import nagios_server_config

def nagios():
  import params

  File( params.nagios_httpd_config_file,
    owner = params.nagios_user,
    group = params.nagios_group,
    content = Template("nagios.conf.j2"),
    mode   = 0644
  )

  # enable snmpd
  Execute( "service snmpd start; chkconfig snmpd on",
    path = "/usr/local/bin/:/bin/:/sbin/"
  )
  
  Directory( params.conf_dir,
    owner = params.nagios_user,
    group = params.nagios_group
  )

  Directory( [params.plugins_dir, params.nagios_obj_dir])

  Directory( params.nagios_pid_dir,
    owner = params.nagios_user,
    group = params.nagios_group,
    mode = 0755,
    recursive = True
  )

  Directory( [params.nagios_var_dir, params.check_result_path, params.nagios_rw_dir],
    owner = params.nagios_user,
    group = params.nagios_group,
    recursive = True
  )
  
  Directory( [params.nagios_log_dir, params.nagios_log_archives_dir],
    owner = params.nagios_user,
    group = params.nagios_group,
    mode = 0755
  )

  nagios_server_config()

  set_web_permisssions()

  File( format("{conf_dir}/command.cfg"),
    owner = params.nagios_user,
    group = params.nagios_group
  )

  File(format("{nagios_var_dir}/ignore.dat"),
    owner = params.nagios_user,
    group = params.nagios_group,
    mode = 0664)
  
  
def set_web_permisssions():
  import params

  cmd = format("{htpasswd_cmd} -c -b  /etc/nagios/htpasswd.users {nagios_web_login} {nagios_web_password!p}")
  test = format("grep {nagios_web_login} /etc/nagios/htpasswd.users")
  Execute( cmd,
    not_if = test
  )

  File( "/etc/nagios/htpasswd.users",
    owner = params.nagios_user,
    group = params.nagios_group,
    mode  = 0640
  )

  if System.get_instance().os_family == "suse":
    command = format("usermod -G {nagios_group} wwwrun")
  else:
    command = format("usermod -a -G {nagios_group} apache")
  
  Execute( command)
