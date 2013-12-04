#!/usr/bin/env python2.6
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
import sys
from mysql_service import *

def mysql():
  import params

  if System.get_instance().platform == "suse":
    Directory('/var/run/mysqld/',
              owner=params.mysql_user,
              group=params.mysql_group,
              recursive=True
    )

    Link('/var/run/mysqld/mysqld.pid',
         to='/var/lib/mysql/mysqld.pid')

  mysql_service(action='start')

  File(params.mysql_adduser_path,
       mode=0755,
       content=StaticFile('addMysqlUser.sh')
  )

  #TODO auto escaping for hive_metastore_user_passwd
  cmd = format("bash -x {mysql_adduser_path} {service_name} {hive_metastore_user_name} \"{hive_metastore_user_passwd}\" {mysql_host}")

  Execute(cmd,
          tries=3,
          try_sleep=5,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
          logoutput=True
  )

  mysql_service(action='stop')
