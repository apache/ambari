
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

class KafkaUpgrade(Script):
  def copy_config(self, env):
    """
    Copy Kafka Config files from  /usr/iop/4.2.0.0/etc/kafka/conf.dist/ to  /usr/iop/4.2.0.0/kafka/conf.
    cp -r /usr/iop/4.2.0.0/etc/kafka/conf.dist/* /usr/iop/4.2.0.0/kafka/conf.
    """
    import params
    if params.upgrade_direction is not None and params.upgrade_direction == Direction.UPGRADE:
      kafka_src_config_dir="/usr/iop/4.2.0.0/etc/kafka/conf.dist"
      kafka_dest_config_dir="/usr/iop/4.2.0.0/kafka/conf"
      if os.path.isdir(kafka_src_config_dir) and os.path.islink(kafka_dest_config_dir):
        Execute(('cp', '-r', kafka_src_config_dir +"/.", kafka_dest_config_dir),sudo=True,logoutput=True)

if __name__ == "__main__":
  KafkaUpgrade().execute()