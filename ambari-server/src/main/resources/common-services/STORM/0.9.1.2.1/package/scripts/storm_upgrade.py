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
import json
import os

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.core.resources.system import Execute
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default

class StormUpgrade(Script):
  """
  This class helps perform some of the upgrade tasks needed for Storm during
  a non-rolling upgrade. Storm writes data to disk locally and to ZooKeeper.
  If any HDP 2.2 bits exist in these directories when an HDP 2.3 instance
  starts up, it will fail to start properly. Because the upgrade framework in
  Ambari doesn't yet have a mechanism to say "stop all" before starting to
  upgrade each component, we need to rely on a Storm trick to bring down
  running daemons. By removing the ZooKeeper data with running daemons, those
  daemons will die.
  """

  def delete_storm_zookeeper_data(self, env):
    """
    Deletes the Storm data from ZooKeeper, effectively bringing down all
    Storm daemons.
    :return:
    """
    import params

    Logger.info('Clearing Storm data from ZooKeeper')

    storm_zookeeper_root_dir = params.storm_zookeeper_root_dir
    if storm_zookeeper_root_dir is None:
      raise Fail("The storm ZooKeeper directory specified by storm-site/storm.zookeeper.root must be specified")

    # create the ZooKeeper delete command
    if params.version is not None:
      command = "/usr/hdp/{0}/zookeeper/bin/zkCli.sh rmr /storm".format(params.version)
    else:
      command = "/usr/hdp/current/zookeeper-client/bin/zkCli.sh rmr /storm"

    if params.security_enabled:
      kinit_command=format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
      Execute(kinit_command,user=params.smokeuser)

    # clean out ZK
    Execute(command, user=params.storm_user, tries=1)


  def delete_storm_local_data(self, env):
    """
    Deletes Storm data from local directories. This will create a marker file
    with JSON data representing the upgrade stack and request/stage ID. This
    will prevent multiple Storm components on the same host from removing
    the local directories more than once.
    :return:
    """
    import params

    Logger.info('Clearing Storm data from local directories...')

    storm_local_directory = params.local_dir
    if storm_local_directory is None:
      raise Fail("The storm local directory specified by storm-site/storm.local.dir must be specified")

    request_id = default("/requestId", None)
    stage_id = default("/stageId", None)
    stack_version = params.version
    stack_name = params.stack_name

    json_map = {}
    json_map["requestId"] = request_id
    json_map["stageId"] = stage_id
    json_map["stackVersion"] = stack_version
    json_map["stackName"] = stack_name

    temp_directory = params.tmp_dir
    upgrade_file = os.path.join(temp_directory, "storm-upgrade-{0}.json".format(stack_version))

    if os.path.exists(upgrade_file):
      try:
        with open(upgrade_file) as file_pointer:
          existing_json_map = json.load(file_pointer)

        if cmp(json_map, existing_json_map) == 0:
          Logger.info("The storm upgrade has already removed the local directories for {0}-{1} for request {2} and stage {3}".format(
            stack_name, stack_version, request_id, stage_id))

          # nothing else to do here for this as it appears to have already been
          # removed by another component being upgraded
          return

      except:
        Logger.error("The upgrade file {0} appears to be corrupt; removing...".format(upgrade_file))
        File(upgrade_file, action="delete")
    else:
      # delete the upgrade file since it does not match
      File(upgrade_file, action="delete")

    # delete from local directory
    Directory(storm_local_directory, action="delete", recursive=True)

    # recreate storm local directory
    Directory(storm_local_directory, mode=0755, owner = params.storm_user,
      group = params.user_group, recursive = True)

    # the file doesn't exist, so create it
    with open(upgrade_file, 'w') as file_pointer:
      json.dump(json_map, file_pointer, indent=2)

if __name__ == "__main__":
  StormUpgrade().execute()