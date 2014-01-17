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

/* This plugin makes call to master node, get the jmx-json document
 * check the corrupt or missing blocks % is > threshod
 * check_jmx -H hostaddress -p port -w 1% -c 1%
 */

  include "hdp_nagios_init.php";

  $options = getopt ("h:p:w:c:s:e:k:r:t:u:");
  if (!array_key_exists('h', $options) || !array_key_exists('p', $options) || !array_key_exists('w', $options)
      || !array_key_exists('c', $options) || !array_key_exists('s', $options)) {
    usage();
    exit(3);
  }

  $hosts=$options['h'];
  $port=$options['p'];
  $warn=$options['w']; $warn = preg_replace('/%$/', '', $warn);
  $crit=$options['c']; $crit = preg_replace('/%$/', '', $crit);
  $nn_jmx_property=$options['s'];
  $keytab_path=$options['k'];
  $principal_name=$options['r'];
  $kinit_path_local=$options['t'];
  $security_enabled=$options['u'];
  $ssl_enabled=$options['e'];

  /* Kinit if security enabled */
  $status = kinit_if_needed($security_enabled, $kinit_path_local, $keytab_path, $principal_name);
  $retcode = $status[0];
  $output = $status[1];
  
  if ($output != 0) {
    echo "CRITICAL: Error doing kinit for nagios. $output";
    exit (2);
  }

  $protocol = ($ssl_enabled == "true" ? "https" : "http");


  foreach (preg_split('/,/', $hosts) as $host) {
    /* Get the json document */

    $ch = curl_init();
    $username = rtrim(`id -un`, "\n");
    curl_setopt_array($ch, array( CURLOPT_URL => $protocol."://".$host.":".$port."/jmx?qry=Hadoop:service=NameNode,name=".$nn_jmx_property,
                                  CURLOPT_RETURNTRANSFER => true,
                                  CURLOPT_HTTPAUTH => CURLAUTH_ANY,
                                  CURLOPT_USERPWD => "$username:",
                                  CURLOPT_SSL_VERIFYPEER => FALSE ));
    $json_string = curl_exec($ch);
    $info = curl_getinfo($ch);
    if (intval($info['http_code']) == 401){
      logout();
      $json_string = curl_exec($ch);
    }
    $info = curl_getinfo($ch);
    curl_close($ch);
    $json_array = json_decode($json_string, true);
    $m_percent = 0;
    $c_percent = 0;
    $object = $json_array['beans'][0];
    $missing_blocks = $object['MissingBlocks'];
    $corrupt_blocks = $object['CorruptBlocks'];
    $total_blocks = $object['BlocksTotal'];
    if (count($object) == 0) {
      echo "CRITICAL: Data inaccessible, Status code = ". $info['http_code'] ."\n";
      exit(2);
    }    
    if($total_blocks == 0) {
      $m_percent = 0;
      $c_percent = 0;
    } else {
      $m_percent = ($missing_blocks/$total_blocks)*100;
      $c_percent = ($corrupt_blocks/$total_blocks)*100;
      break;
    }
  }
  $out_msg = "corrupt_blocks:<" . $corrupt_blocks .
             ">, missing_blocks:<" . $missing_blocks .
             ">, total_blocks:<" . $total_blocks . ">";

  if ($m_percent > $crit || $c_percent > $crit) {
    echo "CRITICAL: " . $out_msg . "\n";
    exit (2);
  }
  if ($m_percent > $warn || $c_percent > $warn) {
    echo "WARNING: " . $out_msg . "\n";
    exit (1);
  }
  echo "OK: " . $out_msg . "\n";
  exit(0);

  /* print usage */
  function usage () {
    echo "Usage: $0 -h <host> -p port -w <warn%> -c <crit%> -s <namenode bean name> -k keytab path -r principal name -t kinit path -s security enabled -e ssl enabled\n";
  }
?>
