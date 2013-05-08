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
class hdp-yarn::smoketest(
  $component_name = undef
)
{
  if ($component_name == 'resourcemanager') {
    $component_type = 'rm' 
  } elsif ($component_name == 'nodemanger') {
    $component_type = 'nm' 
  } elsif ($component_name == 'historyserver') {
    $component_type = 'hs' 
  } else {
    hdp_fail("Unsupported component name: $component_name")
  }

  $smoke_test_user = $hdp::params::smokeuser
  
  $validateStatusFileName = "validateYarnComponentStatus.py"
  $validateStatusFilePath = "/tmp/$validateStatusFileName"
  $yarn_webui_port = $hdp-yarn::params::yarn_webui_port
  $validateStatusCmd = "su - ${smoke_test_user} -c 'python $validateStatusFilePath $component_type -p $yarn_webui_port'"

  file { $validateStatusFilePath:
    ensure => present,
    source => "puppet:///modules/hdp-yarn/$validateStatusFileName",
    mode => '0755'
  }

  exec { $validateStatusFilePath:
    command   => $validateStatusCmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
  File[$validateStatusFilePath] -> Exec[$validateStatusFilePath]
}
