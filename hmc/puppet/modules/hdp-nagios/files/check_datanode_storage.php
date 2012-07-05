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

  $options = getopt ("h:p:w:c:");
  if (!array_key_exists('h', $options) || !array_key_exists('p', $options) || !array_key_exists('w', $options) 
      || !array_key_exists('c', $options)) {
    usage();
    exit(3);
  }

  $host=$options['h'];
  $port=$options['p'];
  $warn=$options['w']; $warn = preg_replace('/%$/', '', $warn);
  $crit=$options['c']; $crit = preg_replace('/%$/', '', $crit);

  /* Get the json document */
  $json_string = file_get_contents("http://".$host.":".$port."/jmx?qry=Hadoop:service=DataNode,name=FSDatasetState-DS-*");
  $json_array = json_decode($json_string, true);
  $object = $json_array['beans'][0];
  $cap_remain = $object['Remaining']; /* Total capacity - any extenal files created in data directories by non-hadoop app */
  $cap_total = $object['Capacity']; /* Capacity used by all data partitions minus space reserved for M/R */
  $percent_full = ($cap_total - $cap_remain)/$cap_total * 100;

  $out_msg = "Capacity:[" . $cap_total . 
             "], Remaining Capacity:[" . $cap_remain . 
             "], percent_full:[" . $percent_full  . "]";
  
  if ($percent_full > $crit) {
    echo "CRITICAL: " . $out_msg . "\n";
    exit (2);
  }
  if ($percent_full > $warn) {
    echo "WARNING: " . $out_msg . "\n";
    exit (1);
  }
  echo "OK: " . $out_msg . "\n";
  exit(0);

  /* print usage */
  function usage () {
    echo "Usage: $0 -h <host> -p port -w <warn%> -c <crit%>\n";
  }
?>
