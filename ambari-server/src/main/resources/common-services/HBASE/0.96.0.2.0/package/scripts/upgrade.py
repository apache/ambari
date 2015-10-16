
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
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.decorator import retry

def prestart(env, hdp_component):
  import params

  if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
    conf_select.select(params.stack_name, "hbase", params.version)
    hdp_select.select(hdp_component, params.version)

def post_regionserver(env):
  import params
  env.set_params(params)

  check_cmd = "echo 'status \"simple\"' | {0} shell".format(params.hbase_cmd)

  exec_cmd = "{0} {1}".format(params.kinit_cmd, check_cmd)
  call_and_match(exec_cmd, params.hbase_user, params.hostname + ":", re.IGNORECASE)

@retry(times=15, sleep_time=2, err_class=Fail)
def call_and_match(cmd, user, regex, regex_search_flags):

  code, out = shell.call(cmd, user=user)

  if not (out and re.search(regex, out, regex_search_flags)):
    raise Fail("Could not verify RS available")
