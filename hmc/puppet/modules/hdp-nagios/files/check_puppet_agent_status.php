#!/usr/bin/php
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

 /* This plugin check if puppet agent is alive */

  $options = getopt ("h:");
  if (!array_key_exists('h', $options)) {
    usage();
    exit(3);
  }

  $host=$options['h'];

  /* Give puppet kick --ping to check if agent is working */
  $out_arr = array();
  $cmd = "puppet kick -f --host $host --ping 2>/dev/null";
  exec ($cmd, $out_arr, $err);
  if ($err == 0 && check_error($out_arr, "status is success", 0) == 0) {
    // success
    echo "OK: Puppet agent is active on [$host]" . "\n";
    exit(0);
  } else {
    // Fail
    echo "WARN: Puppet agent is down on [$host]" . "\n";
    exit(1);
  }

  /* check error function */
  function check_error ($output, $pattern, $ret) {
    $ret1=($ret+1)%2;
    for ($i=0; $i<count($output); $i++) {
      if (preg_match ("/$pattern/", $output[$i])) {
        return $ret;
      }
    }
    return $ret1;
  }

  /* print usage */
  function usage () {
    echo "Usage: $0 -h <host>\n";
  }
?>
