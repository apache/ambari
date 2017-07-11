
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
from resource_management import *
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script

class KafkaUpgrade(Script):

  def copy_kerberos_param(self,env):
    import params
    kafka_run_path = "/usr/iop/4.1.0.0/kafka/bin/kafka-run-class.sh"
    if params.upgrade_direction is not None and params.upgrade_direction == Direction.UPGRADE:
      Execute(("sed", "-i", "s/\$CLASSPATH \$KAFKA_OPTS/\$CLASSPATH \$KAFKA_OPTS \$KAFKA_KERBEROS_PARAMS/", kafka_run_path), logoutput=True)

if __name__ == "__main__":
  KafkaUpgrade().execute()
