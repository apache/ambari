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
from ambari_commons import subprocess32
import os

class SystemMLServiceCheck(Script):
    def service_check(self, env):
        import params
        env.set_params(params)
        
        if os.path.exists(params.systemml_lib_dir):
            cp = format("{params.stack_root}/current/hadoop-client/*:{params.stack_root}/current/hadoop-mapreduce-client/*:{params.stack_root}/current/hadoop-client/lib/*:{params.systemml_lib_dir}/systemml.jar")
            java = format("{params.java_home}/bin/java")
            command = [java, "-cp", cp, "org.apache.sysml.api.DMLScript", "-s", "print('Apache SystemML');"]
            process = subprocess32.Popen(command, stdout=subprocess32.PIPE)
            output = process.communicate()[0]
            print output
        
            if 'Apache SystemML' not in output:
                raise Fail("Expected output Apache SystemML not found.")

if __name__ == "__main__":
    SystemMLServiceCheck().execute()
