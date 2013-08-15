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

/* This plugin makes call to namenode, get the jmx-json document
 * check the NameDirStatuses to find any offline (failed) directories
 * check_jmx -H hostaddress -p port
 */

  $options = getopt ("h:p:");
  if (!array_key_exists('h', $options) || !array_key_exists('p', $options)) {
    usage();
    exit(3);
  }

  $host=$options['h'];
  $port=$options['p'];

  /* Get the json document */
  $json_string = file_get_contents("http://".$host.":".$port."/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo");
  $json_array = json_decode($json_string, true);
  $object = $json_array['beans'][0];
  if ($object['NameDirStatuses'] == "") {
    echo "WARNING: NameNode directory status not available via http://".$host.":".$port."/jmx url" . "\n";
    exit(1);
  }
  $NameDirStatuses = json_decode($object['NameDirStatuses'], true);
  $failed_dir_count = count($NameDirStatuses['failed']);
  $out_msg = "CRITICAL: Offline NameNode directories: ";
  if ($failed_dir_count > 0) {
    foreach ($NameDirStatuses['failed'] as $key => $value) {
      $out_msg = $out_msg . $key . ":" . $value . ", ";
    }
    echo $out_msg . "\n";
    exit (2);
  }
  echo "OK: All NameNode directories are active" . "\n";
  exit(0);

  /* print usage */
  function usage () {
    echo "Usage: $0 -h <host> -p port\n";
  }
?>
