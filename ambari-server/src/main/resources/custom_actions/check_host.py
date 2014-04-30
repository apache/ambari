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

Ambari Agent

"""

import os, sys
from resource_management import *


class CheckHost(Script):
  def actionexecute(self, env):
    config = Script.get_config()

    java_home = config['hostLevelParams']['java_home']

    if not os.path.isfile(os.path.join(java_home, "bin", "java")):
      print "Java home not exists"
      sys.exit(1)

    print "Java home exists"
    structured_output_example = {
        'result': 'Host check completed.'
    }

    self.put_structured_out(structured_output_example)


if __name__ == "__main__":
  CheckHost().execute()
