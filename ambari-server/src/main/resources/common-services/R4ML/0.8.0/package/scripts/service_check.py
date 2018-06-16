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
from resource_management.libraries.functions.format import format
import os

class R4MLServiceCheck(Script):
    def service_check(self, env):
        import params
        env.set_params(params)

        # generate the service check file
        scR = os.path.join(params.exec_tmp_dir, "ServiceCheck.R")
        File(format(scR),
             content = StaticFile("ServiceCheck.R"),
             mode = 0755)

        Execute(("Rscript", scR),
                tries=120,
                try_sleep=20,
                path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                logoutput=True,
                user=params.smokeuser)

if __name__ == "__main__":
    R4MLServiceCheck().execute()
