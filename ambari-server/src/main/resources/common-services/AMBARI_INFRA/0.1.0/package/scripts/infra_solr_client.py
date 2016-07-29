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

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.libraries.script.script import Script
from setup_infra_solr import setup_infra_solr

class InfraSolrClient(Script):

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    setup_infra_solr(name ='client')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

  def status(self, env):
    raise ClientComponentHasNoStatus()


if __name__ == "__main__":
  InfraSolrClient().execute()