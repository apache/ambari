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
class hdp::testing_env_patch()
{
  $cmd = "mkdir /tmp/repos; mv /etc/yum.repos.d/* /tmp/repos"
  $repo_target = "/etc/yum.repos.d/${hdp::params::hdp_yum_repo}"

  anchor { 'hdp::testing_env_patch::begin' :}
  exec { '/bin/echo 0 > /selinux/enforce':
    require => Anchor['hdp::testing_env_patch::begin']
  }
  hdp::testing_env_patch::packages { 'common' :
    require => Exec['/bin/echo 0 > /selinux/enforce']
  }
  hdp::exec { $cmd :
    command => $cmd,
    unless => "test -e ${repo_target}",
    require => Hdp::Testing_env_patch::Packages['common']
  }  
  anchor { 'hdp::testing_env_patch::end' :
    require => Exec[$cmd]
  }
}

define hdp::testing_env_patch::packages(
  $needed = false)
{
 if ($needed == true) {
   package { ['perl-Digest-HMAC','perl-Socket6','perl-Crypt-DES','xorg-x11-fonts-Type1','libdbi'] :} 
 }
}
