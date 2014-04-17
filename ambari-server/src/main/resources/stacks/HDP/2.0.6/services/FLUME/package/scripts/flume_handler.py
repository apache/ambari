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
from flume import flume
from flume import flume_status

class FlumeHandler(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)

  def start(self, env):
    import params

    env.set_params(params)
    self.configure(env)

    flume(action='start')

  def stop(self, env):
    import params

    env.set_params(params)

    flume(action='stop')

  def configure(self, env):
    import params

    env.set_params(params)

    flume(action='config')

  def status(self, env):
    import params

    env.set_params(params)

    processes = flume_status()

    json = {}
    json['processes'] = processes

    self.put_structured_out(json)

    if 0 == len(processes):
      raise ComponentIsNotRunning()
    else:
      for proc in processes:
        if not proc.has_key('status') or proc['status'] == 'NOT_RUNNING':
          raise ComponentIsNotRunning()

if __name__ == "__main__":
  FlumeHandler().execute()
