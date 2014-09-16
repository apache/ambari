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
 * check the storage capacity remaining on local datanode storage
 */

  include "hdp_nagios_init.php";

  $options = getopt ("h:p:w:c:d:x:e:k:r:t:u:");
  if (!array_key_exists('h', $options) || !array_key_exists('p', $options) ||
      !array_key_exists('w', $options) || !array_key_exists('c', $options) ||
      !array_key_exists('d', $options) || !array_key_exists('x', $options)) {
    usage();
    exit(3);
  }
  $host=split(',', $options['h']);
  $port=$options['p'];
  # Default 200 - Percent for warning alert
  $warning=$options['w'];
  # Default 200 - Percent for critical alert
  $crit=$options['c'];
  # Default 21600 - Period time
  $period=$options['d'];
  # Default 1000000 - CheckpointNode will create a checkpoint of the namespace every 'dfs.namenode.checkpoint.txns'
  $txns=$options['x'];
  $ssl_enabled=$options['e'];
  $keytab_path=$options['k'];
  $principal_name=$options['r'];
  $kinit_path_local=$options['t'];
  $security_enabled=$options['u'];

  /* Kinit if security enabled */
  $status = kinit_if_needed($security_enabled, $kinit_path_local, $keytab_path, $principal_name);
  $retcode = $status[0];
  $output = $status[1];
  
  if ($output != 0) {
    echo "CRITICAL: Error doing kinit for nagios. $output";
    exit (2);
  }

  $protocol = ($ssl_enabled == "true" ? "https" : "http");
  date_default_timezone_set('UTC');

  # get_last_checkpoint_time
  $ch1 = curl_init();
  $username = rtrim(`id -un`, "\n");
  
  curl_setopt_array($ch1, array( CURLOPT_URL => $protocol."://".$host.":".$port."/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem",
                                CURLOPT_RETURNTRANSFER => true,
                                CURLOPT_HTTPAUTH => CURLAUTH_ANY,
                                CURLOPT_USERPWD => "$username:",
                                CURLOPT_SSL_VERIFYPEER => FALSE ));
  $json_string = curl_exec($ch1);
  $info = curl_getinfo($ch1);
  if (intval($info['http_code']) == 401){
    logout();
    $json_string = curl_exec($ch1);
  }
  $info = curl_getinfo($ch1);
  curl_close($ch1);
  $json_array = json_decode($json_string, true);
  $last_checkpoint_time = (int) $json_array['beans'][0]['LastCheckpointTime'];
  
  # get_journal_transaction_info
  $ch2 = curl_init();
  $username = rtrim(`id -un`, "\n");
  curl_setopt_array($ch2, array( CURLOPT_URL => $protocol."://".$host.":".$port."/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo",
                                CURLOPT_RETURNTRANSFER => true,
                                CURLOPT_HTTPAUTH => CURLAUTH_ANY,
                                CURLOPT_USERPWD => "$username:",
                                CURLOPT_SSL_VERIFYPEER => FALSE ));
  $json_string = curl_exec($ch2);
  $info = curl_getinfo($ch2);
  if (intval($info['http_code']) == 401){
    logout();
    $json_string = curl_exec($ch2);
  }
  $info = curl_getinfo($ch2);
  curl_close($ch2);
  $json_array = json_decode($json_string, true);
  
  $journal_transaction_info = json_decode($json_array['beans'][0]['JournalTransactionInfo'], true);
  $last_txid = (int) $journal_transaction_info['LastAppliedOrWrittenTxId'];
  $most_txid = (int) $journal_transaction_info['MostRecentCheckpointTxId'];
  
  $delta = (time() * 1000 - $last_checkpoint_time)/1000;
  
  if (($last_txid - $most_txid) > $txns && $delta / $period * 100 >= $crit){
    $h = date('H', $delta);
    $m = date('i', $delta);
    echo "CRITICAL: Last checkpoint time is below acceptable. Checkpoint was done ${h}h. ${m}m. ago";
    exit(2);
  }else if (($last_txid - $most_txid) > $txns && $delta / $period * 100 >= $warning){
    $h = date('H', $delta);
    $m = date('i', $delta);
    echo "WARNING: Last checkpoint time is below acceptable. Checkpoint was done ${h}h. ${m}m. ago";
    exit(1);
  }else{
    print "OK: Last checkpoint time";
    exit(0);
  }

  /* print usage */
  function usage () {
    echo "Usage: $0 -h host -p port -w warn -c crit -d period -x txns -e ssl enabled -k keytab path -r principal name -t kinit path -u security enabled\n";
  }
  
?>