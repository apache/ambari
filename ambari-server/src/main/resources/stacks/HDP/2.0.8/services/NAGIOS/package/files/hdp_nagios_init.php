<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Common functions called from other alerts
 *
 */
 
 /*
 * Function for kinit. Checks if security enabled and klist for this principal doesn't returns nothing,
 * make kinit call in this case.
 */
  function kinit_if_needed($security_enabled, $kinit_path_local, $keytab_path, $principal_name) {
    if($security_enabled === 'true') {
    
      $is_logined = is_logined($principal_name);
      
      if (!$is_logined)
        $status = kinit($kinit_path_local, $keytab_path, $principal_name);
      else
        $status = array(0, '');
    } else {
      $status = array(0, '');
    }
  
    return $status;
  }
  
  
  /*
  * Checks if user is logined on kerberos
  */
  function is_logined($principal_name) {
    $check_cmd = "klist|grep $principal_name 1> /dev/null 2>/dev/null ; [[ $? != 0 ]] && echo 1";
    $check_output =  shell_exec($check_cmd);
    
    if ($check_output)
      return false;
    else
      return true;
  }

  /*
  * Runs kinit command.
  */
  function kinit($kinit_path_local, $keytab_path, $principal_name) {
    $init_cmd = "$kinit_path_local -kt $keytab_path $principal_name 2>&1";
    $kinit_output = shell_exec($init_cmd);
    if ($kinit_output) 
      $status = array(1, $kinit_output);
    else
      $status = array(0, '');
      
    return $status;
  }

  function logout() {
    if (shell_exec("rm -f /tmp/krb5cc_".trim(shell_exec('id -u'))) == "" ) 
      $status = true;
    else
      $status = false;
      
    return $status;
  }
 
 ?>