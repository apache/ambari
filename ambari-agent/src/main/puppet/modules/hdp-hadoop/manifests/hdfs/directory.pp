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
#TODO: unset should br changed to undef; just to be consistent
define hdp-hadoop::hdfs::directory(
  $service_state = 'running',
  $owner = unset,
  $group = unset,
  $recursive_chown = false,
  $mode = undef,
  $recursive_chmod = false
) 
{
  $dir_exists = "hadoop fs -ls ${name} >/dev/null 2>&1"
  $tries = 30
  $try_sleep = 10
 
  if ($service_state == 'running') {
  
  
    if $stack_version in ("2.0.1") {
      $mkdir_cmd = "fs -mkdir -p ${name}"
    } else {
      $mkdir_cmd = "fs -mkdir ${name}"
    }
    hdp-hadoop::exec-hadoop { $mkdir_cmd:
      command   => $mkdir_cmd,
      unless    => $dir_exists,
      try_sleep => $try_sleep,
      tries     => $tries
    }
    if ($owner == unset) {
      $chown = ""
    } else {
      if ($group == unset) {
        $chown = $owner
      } else {
        $chown = "${owner}:${group}"
     } 
    }  
 
    if (chown != "") {
      #TODO: see if there is a good 'unless test'
      if ($recursive_chown == true) {
        $chown_cmd = "fs -chown -R ${chown} ${name}"
      } else {
        $chown_cmd = "fs -chown ${chown} ${name}"
      }
      hdp-hadoop::exec-hadoop {$chown_cmd :
        command   => $chown_cmd,
        onlyif    => $dir_exists,
        try_sleep => $try_sleep,
        tries     => $tries
      }
      Hdp-hadoop::Exec-hadoop[$mkdir_cmd] -> Hdp-hadoop::Exec-hadoop[$chown_cmd]
    }
  
    if ($mode != undef) {
      #TODO: see if there is a good 'unless test'
      if ($recursive_chmod == true) {
        $chmod_cmd = "fs -chmod -R ${mode} ${name}"
      } else {
        $chmod_cmd = "fs -chmod ${mode} ${name}"
      }
      hdp-hadoop::exec-hadoop {$chmod_cmd :
        command   => $chmod_cmd,
        onlyif    => $dir_exists,
        try_sleep => $try_sleep,
        tries     => $tries
      }
      Hdp-hadoop::Exec-hadoop[$mkdir_cmd] -> Hdp-hadoop::Exec-hadoop[$chmod_cmd]
    }
  }       
}
