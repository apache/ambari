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

from resource_management.libraries.script.script import Script
from resource_management.core.shell import as_user
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries import functions
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.core.logger import Logger
from resource_management.core.source import StaticFile
from resource_management.core.resources.system import Execute, File


class HadoopRouterServiceCheck(Script):
    pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HadoopRouterServiceCheckDefault(HadoopRouterServiceCheck):
    def service_check(self, env):
        import params

        env.set_params(params)

        checkWebUIFileName = "checkWebUI.py"
        checkWebUIFilePath = format("{params.tmp_dir}/{checkWebUIFileName}")
        router_port = params.router_addr.split(":")[1]
        https_only= False
        comma_sep_router_hosts = ",".join(params.hadoop_router_hosts)

        checkWebUICmd = f"ambari-python-wrap {checkWebUIFilePath} -m {comma_sep_router_hosts} -p {router_port} -s {https_only}"
        File(checkWebUIFilePath,
             content=StaticFile(checkWebUIFileName),
             mode=0o775)

        Execute(checkWebUICmd,
                logoutput=True,
                try_sleep=3,
                tries=5,
                user=params.smoke_user
                )

if __name__ == "__main__":
    HadoopRouterServiceCheck().execute()
