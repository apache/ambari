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

import os

from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.core.resources import File
from resource_management.core.resources import Execute
from resource_management.libraries.script import Script
from resource_management.core.source import StaticFile
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

class ServiceCheck(Script):
  pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ServiceCheckWindows(ServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "STORM"
    Execute(format("cmd /C {smoke_cmd} {service}", smoke_cmd=smoke_cmd, service=service), user=params.storm_user, logoutput=True)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ServiceCheckDefault(ServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    unique = get_unique_id_and_date()

    File("/tmp/wordCount.jar",
         content=StaticFile("wordCount.jar"),
         owner=params.storm_user
    )

    cmd = ""
    if params.nimbus_seeds_supported:
      # Because this command is guaranteed to run on one of the hosts with storm client, there is no need
      # to specify "-c nimbus.seeds={nimbus_seeds}"
      cmd = format("storm jar /tmp/wordCount.jar storm.starter.WordCountTopology WordCount{unique}")
    elif params.nimbus_host is not None:
      cmd = format("storm jar /tmp/wordCount.jar storm.starter.WordCountTopology WordCount{unique} -c nimbus.host={nimbus_host}")

    Execute(cmd,
            logoutput=True,
            path=params.storm_bin_dir,
            user=params.storm_user
    )

    Execute(format("storm kill WordCount{unique}"),
            path=params.storm_bin_dir,
            user=params.storm_user
    )

if __name__ == "__main__":
  ServiceCheck().execute()
