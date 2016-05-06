
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


from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import format
from resource_management.libraries.functions import Direction
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

def run_migration(env, upgrade_type):
  """
  If the acl migration script is present, then run it for either upgrade or downgrade.
  That script was introduced in HDP 2.3.4.0 and requires stopping all Kafka brokers first.
  Requires configs to be present.
  :param env: Environment.
  :param upgrade_type: "rolling" or "nonrolling
  """
  import params

  if upgrade_type is None:
    raise Fail('Parameter "upgrade_type" is missing.')

  if params.upgrade_direction is None:
    raise Fail('Parameter "upgrade_direction" is missing.')

  if params.upgrade_direction == Direction.DOWNGRADE and params.downgrade_from_version is None:
    raise Fail('Parameter "downgrade_from_version" is missing.')

  if not params.security_enabled:
    Logger.info("Skip running the Kafka ACL migration script since cluster security is not enabled.")
    return
  
  Logger.info("Upgrade type: {0}, direction: {1}".format(str(upgrade_type), params.upgrade_direction))

  # If the schema upgrade script exists in the version upgrading to, then attempt to upgrade/downgrade it while still using the present bits.
  kafka_acls_script = None
  command_suffix = ""
  if params.upgrade_direction == Direction.UPGRADE:
    kafka_acls_script = format("{stack_root}/{version}/kafka/bin/kafka-acls.sh")
    command_suffix = "--upgradeAcls"
  elif params.upgrade_direction == Direction.DOWNGRADE:
    kafka_acls_script = format("{stack_root}/{downgrade_from_version}/kafka/bin/kafka-acls.sh")
    command_suffix = "--downgradeAcls"

  if kafka_acls_script is not None:
    if os.path.exists(kafka_acls_script):
      Logger.info("Found Kafka acls script: {0}".format(kafka_acls_script))
      if params.zookeeper_connect is None:
        raise Fail("Could not retrieve property kafka-broker/zookeeper.connect")

      acls_command = "{0} --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect={1} {2}".\
        format(kafka_acls_script, params.zookeeper_connect, command_suffix)

      Execute(acls_command,
              user=params.kafka_user,
              logoutput=True)
    else:
      Logger.info("Did not find Kafka acls script: {0}".format(kafka_acls_script))
