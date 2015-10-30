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

import sys
from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from hbase import hbase
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HbaseClient(Script):
  def install(self, env):
    import params
    
    self.install_packages(env, params.exclude_packages)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hbase(name='client')

  def status(self, env):
    raise ClientComponentHasNoStatus()


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HbaseClientWindows(HbaseClient):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseClientDefault(HbaseClient):
  def get_stack_to_component(self):
    return {"HDP": "hbase-client"}

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "hbase", params.version)
      hdp_select.select("hbase-client", params.version)

      # phoenix may not always be deployed
      try:
        hdp_select.select("phoenix-client", params.version)
      except Exception as e:
        print "Ignoring error due to missing phoenix-client"
        print str(e)


      # set all of the hadoop clients since hbase client is upgraded as part
      # of the final "CLIENTS" group and we need to ensure that hadoop-client
      # is also set
      conf_select.select(params.stack_name, "hadoop", params.version)
      hdp_select.select("hadoop-client", params.version)


if __name__ == "__main__":
  HbaseClient().execute()
