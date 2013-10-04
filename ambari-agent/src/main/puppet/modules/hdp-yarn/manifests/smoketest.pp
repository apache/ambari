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
define hdp-yarn::smoketest(
  $component_name = undef
)
{
  $rm_webui_address = $hdp-yarn::params::rm_webui_address
  $rm_webui_https_address = $hdp-yarn::params::rm_webui_https_address
  $nm_webui_address = $hdp-yarn::params::nm_webui_address
  $hs_webui_address = $hdp-yarn::params::hs_webui_address
  
  $hadoop_ssl_enabled = $hdp-hadoop::params::hadoop_ssl_enabled

  if ($component_name == 'resourcemanager') {
    $component_type = 'rm'
    if ($hadoop_ssl_enabled == "true") {
      $component_address = $rm_webui_https_address
    } else {
      $component_address = $rm_webui_address
    }
  } elsif ($component_name == 'nodemanager') {
    $component_type = 'nm'
    $component_address = $nm_webui_address
  } elsif ($component_name == 'historyserver') {
    $component_type = 'hs'
    $component_address = $hs_webui_address
  } else {
    hdp_fail("Unsupported component name: $component_name")
  }

  $security_enabled = $hdp::params::security_enabled
  $smoke_user_keytab = $hdp::params::smokeuser_keytab
  $smoke_test_user = $hdp::params::smokeuser
  $kinit_cmd = "${hdp::params::kinit_path_local} -kt ${smoke_user_keytab} ${smoke_test_user};"


  $validateStatusFileName = "validateYarnComponentStatus.py"
  $validateStatusFilePath = "/tmp/$validateStatusFileName"

  $validateStatusCmd = "$validateStatusFilePath $component_type -p $component_address -s $hadoop_ssl_enabled"

    if ($security_enabled == true) {
         $smoke_cmd = "${kinit_cmd}  $validateStatusCmd"
        } else {
          $smoke_cmd = $validateStatusCmd
        }


  file { $validateStatusFilePath:
    ensure => present,
    source => "puppet:///modules/hdp-yarn/$validateStatusFileName",
    mode => '0755'
  }

  exec { $validateStatusFilePath:
    command   => $smoke_cmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true",
    user     =>  $smoke_test_user
}
  anchor{"hdp-yarn::smoketest::begin":} -> File[$validateStatusFilePath] -> Exec[$validateStatusFilePath] -> anchor{"hdp-yarn::smoketest::end":}
}
