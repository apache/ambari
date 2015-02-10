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


class SparkServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    self.check_spark_job_history_server()
    # self.check_spark_client()

  def check_spark_job_history_server(self):
    cmd = 'ps -ef | grep org.apache.spark.deploy.history.HistoryServer | grep -v grep'
    code, output = shell.call(cmd, timeout=100)
    if code == 0:
      Logger.info('Spark job History Server up and running')
    else:
      Logger.debug('Spark job History Server not running')
      raise ComponentIsNotRunning()

  pass

  # def check_spark_client(self):
  # import params
  #   smoke_cmd = params.spark_service_check_cmd
  #   code, output = shell.call(smoke_cmd, timeout=100)
  #   if code == 0:
  #     Logger.info('Spark on Yarn Job can be submitted')
  #   else:
  #     Logger.debug('Spark on Yarn Job cannot be submitted')
  #     raise ComponentIsNotRunning()
  # pass

if __name__ == "__main__":
  SparkServiceCheck().execute()
