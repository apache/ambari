#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
#TODO: these scripts called should be converted to native puppet
define hdp-ganglia::config::generate_daemon(
  $ganglia_service,
  $role,
  $owner = 'root',
  $group = $hdp::params::user_group
)
{
  $shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $cmd = $ganglia_service ? {
    'gmond'  => $role ? {
      'server' => "${shell_cmds_dir}/setupGanglia.sh -c ${name} -m -o ${owner} -g ${group}",
       default =>  "${shell_cmds_dir}/setupGanglia.sh -c ${name} -o ${owner} -g ${group}"
    },
    'gmetad' => "${shell_cmds_dir}/setupGanglia.sh -t -o ${owner} -g ${group}",
     default => hdp_fail("Unexpected ganglia service: ${$ganglia_service}")	
  }

  #TODO: put in test condition
  hdp::exec { $cmd:
    command => $cmd
 }
}
