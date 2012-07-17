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

#TODO: just quick and dirt for testing
class hdp-java-jce() 
{
  
  $jce_policy = "jce_policy-6.zip"
  $jce_policy_tmp_file = "/tmp/${jce_policy}"
  file { $jce_policy_tmp_file:
    ensure => present,
    source => "puppet:///modules/hdp-java-jce/jce_policy-6.zip"
  }

  hdp-java-jce::install { 32:
    source  => $jce_policy_tmp_file,
    require => File[$jce_policy_tmp_file]
  }
  hdp-java-jce::install { 64:
    source  => $jce_policy_tmp_file,
    require => File[$jce_policy_tmp_file]
  }
}

define hdp-java-jce::install(
 $source
)
{
  include hdp::params
  $size = $name
  if ($size == 32) {
    $java_home = $hdp::params::java32_home
  } else {
    $java_home = $hdp::params::java64_home
  }
  #TODO: can make idempotent by uniping in puppet file server and doing adiff with local_policy.jar and/or US_export_policy.jar
  $security_dir = "${java_home}/jre/lib/security"
  $cmd = "rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q ${source}"
  exec { "jce-install ${name}":
    command => $cmd,
    onlyif  => "test -e ${security_dir}",
    cwd     => $security_dir,
    path    => ['/bin/','/usr/bin']
  }
}
