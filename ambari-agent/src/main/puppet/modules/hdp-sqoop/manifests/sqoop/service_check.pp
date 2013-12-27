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
class hdp-sqoop::sqoop::service_check() 
{
  include hdp-sqoop::params
  $smoke_test_user = $hdp::params::smokeuser

  # TODO:SUHAS Move this to hdp::params
  $security_enabled=$hdp::params::security_enabled
  $smoke_user_keytab = $hdp::params::smokeuser_keytab
  if ($security_enabled == true) {
    $smoke_user_kinitcmd="${hdp::params::kinit_path_local}  -kt ${smoke_user_keytab} ${smoke_test_user}; "
  } else {
    $smoke_user_kinitcmd=""
  }

  $cmd = "${smoke_user_kinitcmd}su - ${smoke_test_user} -c 'sqoop version'"
  
  anchor { 'hdp-sqoop::sqoop::service_check::begin':}

  exec { 'sqoop_smoke':
    command   => $cmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true",
    require   => Anchor['hdp-sqoop::sqoop::service_check::begin'],
    before    => Anchor['hdp-sqoop::sqoop::service_check::end']
  }

  anchor{ 'hdp-sqoop::sqoop::service_check::end':}
}
