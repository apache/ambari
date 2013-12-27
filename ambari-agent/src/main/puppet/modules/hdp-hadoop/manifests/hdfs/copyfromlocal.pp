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
define hdp-hadoop::hdfs::copyfromlocal(
  $service_state,
  $owner = unset,
  $group = unset,
  $recursive_chown = false,
  $mode = undef,
  $recursive_chmod = false,
  $dest_dir = undef,
  $kinit_if_needed = undef
) 
{
 
  if ($service_state == 'running') {
    $copy_cmd = "fs -copyFromLocal ${name} ${dest_dir}"
    if ($kinit_if_needed == undef) {
      $unless_cmd = "hadoop fs -ls ${dest_dir} >/dev/null 2>&1"
    } else {
      $unless_cmd = "${kinit_if_needed} hadoop fs -ls ${dest_dir} >/dev/null 2>&1"
    }
    ## exec-hadoop does a kinit based on user, but unless does not
    hdp-hadoop::exec-hadoop { $copy_cmd:
      command => $copy_cmd,
      unless => $unless_cmd,
      user => $owner
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
        $chown_cmd = "fs -chown -R ${chown} ${dest_dir}"
      } else {
        $chown_cmd = "fs -chown ${chown} ${dest_dir}"
      }
      hdp-hadoop::exec-hadoop {$chown_cmd :
        command => $chown_cmd,
        user => $owner
      }
      Hdp-hadoop::Exec-hadoop[$copy_cmd] -> Hdp-hadoop::Exec-hadoop[$chown_cmd]
    }
  
    if ($mode != undef) {
      #TODO: see if there is a good 'unless test'
      if ($recursive_mode == true) {
        $chmod_cmd = "fs -chmod -R ${mode} ${dest_dir}"
      } else {
        $chmod_cmd = "fs -chmod ${mode} ${dest_dir}"
      }
      hdp-hadoop::exec-hadoop {$chmod_cmd :
        command => $chmod_cmd,
        user => $owner
      }
      Hdp-hadoop::Exec-hadoop[$copy_cmd] -> Hdp-hadoop::Exec-hadoop[$chmod_cmd]
    }
  }       
}
