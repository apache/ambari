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

  $options = getopt ("f:s:n:w:c:t:");
  if (!array_key_exists('t', $options) || !array_key_exists('f', $options) || !array_key_exists('w', $options)
      || !array_key_exists('c', $options) || !array_key_exists('s', $options)) {
    usage();
    exit(3);
  }
  $status_file=$options['f'];
  $status_code=$options['s'];
  $type=$options['t'];
  $warn=$options['w']; $warn = preg_replace('/%$/', '', $warn);
  $crit=$options['c']; $crit = preg_replace('/%$/', '', $crit);
  if ($type == "service" && !array_key_exists('n', $options)) {
    echo "Service description not provided -n option\n";
    exit(3);
  }
  if ($type == "service") {
    $service_name=$options['n'];
    /* echo "DESC: " . $service_name . "\n"; */
  }

  $result = array();
  $status_file_content = file_get_contents($status_file);

  $counts;
  if ($type == "service") {
    $counts=query_alert_count($status_file_content, $service_name, $status_code);
  } else {
    $counts=query_host_count($status_file_content, $status_code);
  }

  if ($counts['total'] == 0) {
    $percent = 0;
  } else {
    $percent = ($counts['actual']/$counts['total'])*100;
  }
  if ($percent >= $crit) {
    echo "CRITICAL: total:<" . $counts['total'] . ">, affected:<" . $counts['actual'] . ">\n";
    exit (2);
  }
  if ($percent >= $warn) {
    echo "WARNING: total:<" . $counts['total'] . ">, affected:<" . $counts['actual'] . ">\n";
    exit (1);
  }
  echo "OK: total:<" . $counts['total'] . ">, affected:<" . $counts['actual'] . ">\n";
  exit(0);


  # Functions
  /* print usage */
  function usage () {
    echo "Usage: $0 -f <status_file_path> -t type(host/service) -s <status_codes> -n <service description> -w <warn%> -c <crit%>\n";
  }

  /* Query host count */
  function query_host_count ($status_file_content, $status_code) {
    $num_matches = preg_match_all("/hoststatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    $hostcounts_object = array ();
    $total_hosts = 0;
    $hosts = 0;
    foreach ($matches[0] as $object) {
      $total_hosts++;
      if (getParameter($object, "current_state") == $status_code) {
        $hosts++;
      }
    }
    $hostcounts_object['total'] = $total_hosts;
    $hostcounts_object['actual'] = $hosts;
    return $hostcounts_object;
  }

  /* Query Alert counts */
  function query_alert_count ($status_file_content, $service_name, $status_code) {
    $num_matches = preg_match_all("/servicestatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    $alertcounts_objects = array ();
    $total_alerts=0;
    $alerts=0;
    foreach ($matches[0] as $object) {
      if (getParameter($object, "service_description") == $service_name) {
        $total_alerts++;
        if (getParameter($object, "current_state") >= $status_code) {
          $alerts++;
        }
      }
    }
    $alertcounts_objects['total'] = $total_alerts;
    $alertcounts_objects['actual'] = $alerts;
    return $alertcounts_objects;
  }

  function get_service_type($service_description)
  {
    $pieces = explode("::", $service_description);
    switch ($pieces[0]) {
      case "NAMENODE":
        $pieces[0] = "HDFS";
        break;
      case "JOBTRACKER":
        $pieces[0] = "MAPREDUCE";
        break;
      case "HBASEMASTER":
        $pieces[0] = "HBASE";
        break;
      case "SYSTEM":
      case "HDFS":
      case "MAPREDUCE":
      case "HBASE":
        break;
      default:
        $pieces[0] = "UNKNOWN";
    }
    return $pieces[0];
  }

  function getParameter($object, $key)
  {
    $pattern="/\s" . $key . "[\s= ]*([\S, ]*)\n/";
    $num_mat = preg_match($pattern, $object, $matches);
    $value = "";
    if ($num_mat) {
      $value = $matches[1];
    }
    return $value;
  }

function indent($json) {

    $result      = '';
    $pos         = 0;
    $strLen      = strlen($json);
    $indentStr   = '  ';
    $newLine     = "\n";
    $prevChar    = '';
    $outOfQuotes = true;

    for ($i=0; $i<=$strLen; $i++) {

        // Grab the next character in the string.
        $char = substr($json, $i, 1);

        // Are we inside a quoted string?
        if ($char == '"' && $prevChar != '\\') {
            $outOfQuotes = !$outOfQuotes;

        // If this character is the end of an element,
        // output a new line and indent the next line.
        } else if(($char == '}' || $char == ']') && $outOfQuotes) {
            $result .= $newLine;
            $pos --;
            for ($j=0; $j<$pos; $j++) {
                $result .= $indentStr;
            }
        }

        // Add the character to the result string.
        $result .= $char;

        // If the last character was the beginning of an element,
        // output a new line and indent the next line.
        if (($char == ',' || $char == '{' || $char == '[') && $outOfQuotes) {
            $result .= $newLine;
            if ($char == '{' || $char == '[') {
                $pos ++;
            }

            for ($j = 0; $j < $pos; $j++) {
                $result .= $indentStr;
            }
        }

        $prevChar = $char;
    }

    return $result;
}

/* JSON documment format */
/*
{
  "programstatus":{
    "last_command_check":"1327385743"
  },
  "hostcounts":{
    "up_nodes":"",
    "down_nodes":""
  },
  "hoststatus":[
    {
      "host_name"="ip-10-242-191-48.ec2.internal",
      "current_state":"0",
      "last_hard_state":"0",
      "plugin_output":"PING OK - Packet loss = 0%, RTA = 0.04 ms",
      "last_check":"1327385564",
      "current_attempt":"1",
      "last_hard_state_change":"1327362079",
      "last_time_up":"1327385574",
      "last_time_down":"0",
      "last_time_unreachable":"0",
      "is_flapping":"0",
      "last_check":"1327385574",
      "servicestatus":[
      ]
    }
  ],
  "servicestatus":[
    {
      "service_type":"HDFS",  {HBASE, MAPREDUCE, HIVE, ZOOKEEPER}
      "service_description":"HDFS Current Load",
      "host_name"="ip-10-242-191-48.ec2.internal",
      "current_attempt":"1",
      "current_state":"0",
      "plugin_output":"PING OK - Packet loss = 0%, RTA = 0.04 ms",
      "last_hard_state_change":"1327362079",
      "last_time_ok":"1327385479",
      "last_time_warning":"0",
      "last_time_unknown":"0",
      "last_time_critical":"0",
      "last_check":"1327385574",
      "is_flapping":"0"
    }
  ]
}
*/

?>
