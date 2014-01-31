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

"""

from resource_management import *


def hbase_decommission(env):
  import params

  env.set_params(params)

  if params.hbase_drain_only == True:
    print "TBD: Remove host from draining"
    pass

  else:

    kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_user};") if params.security_enabled else ""

    hosts = params.hbase_excluded_hosts.split(",")
    for host in hosts:
      if host:
        print "TBD: Add host to draining"
        regionmover_cmd = format(
          "{kinit_cmd} {hbase_cmd} --config {conf_dir} org.jruby.Main {region_mover} unload {host}")

        Execute(regionmover_cmd,
                user=params.hbase_user,
                logoutput=True
        )
      pass
    pass
  pass


pass