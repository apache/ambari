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

/** Constants. */
define("HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES", "Properties");
define("HDP_MON_RESPONSE_OPTION_KEY__TYPE", "Type");

define("HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE", "Uncacheable");
define("HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON", "JSON");
define("HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT", "JAVASCRIPT");

define("HDP_MON_QUERY_ARG__JSONP", "jsonp");

/** Spits out appropriate response headers, as per the options passed in. */
function hdp_mon_generate_response_headers( $response_options )
{
  if( $response_options[HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES] == HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE )
  {
    // Make the response uncache-able.
    header("Expires: Mon, 26 Jul 1997 05:00:00 GMT"); // Date in the past
    header("Last-Modified: " . gmdate("D, d M Y H:i:s") . " GMT"); // Always modified
    header("Cache-Control: no-cache, must-revalidate"); // HTTP/1.1
    header("Pragma: no-cache"); // HTTP/1.0
  }

  switch( $response_options[HDP_MON_RESPONSE_OPTION_KEY__TYPE] )
  {
    case HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON:
      {
        header('Content-type: application/json');
      }
      break;

    case HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT:
      {
        header('Content-type: application/javascript');
      }
      break;
  }
}

/** Given $response_data (which we expect to be a JSON string), generate an
 *  HTTP response, which includes emitting the necessary HTTP response headers
 *  followed by the response body (that is either plain ol' $response_data,
 *  or a JSONP wrapper around it).
 */
function hdp_mon_generate_response( $response_data )
{
  $jsonpFunctionName = NULL;
  if (isset($_GET[HDP_MON_QUERY_ARG__JSONP])) {
    $jsonpFunctionName = $_GET[HDP_MON_QUERY_ARG__JSONP];
  }

  hdp_mon_generate_response_headers( array
  ( HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES => HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE,
  HDP_MON_RESPONSE_OPTION_KEY__TYPE =>
  isset( $jsonpFunctionName )  && $jsonpFunctionName != "" ?
  HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT :
  HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON ) );

  if( isset( $jsonpFunctionName ) )
  {
    echo "$jsonpFunctionName( $response_data );";
  }
  else
  {
    echo $response_data;
  }
}

  /* alert_type { ok, non-ok, warning, critical, all } */
  define ("all", "-2");
  define ("nok", "-1");
  define ("ok", "0");
  define ("warn", "1");
  define ("critical", "2");

  define ("HDFS_SERVICE_CHECK", "NAMENODE::NameNode process down");
  define ("MAPREDUCE_SERVICE_CHECK", "JOBTRACKER::JobTracker process down");
  define ("HBASE_SERVICE_CHECK", "HBASEMASTER::HBaseMaster process down");
  define ("ZOOKEEPER_SERVICE_CHECK", "ZOOKEEPER::Percent ZooKeeper Servers down");
  define ("HIVE_SERVICE_CHECK", "HIVE-METASTORE::Hive Metastore status check");
  define ("OOZIE_SERVICE_CHECK", "OOZIE::Oozie Server status check");
  define ("WEBHCAT_SERVICE_CHECK", "WEBHCAT::WebHCat Server status check");
  define ("PUPPET_SERVICE_CHECK", "PUPPET::Puppet agent down");

  // on SUSE, some versions of Nagios stored data in /var/lib
  $status_file = "/var/nagios/status.dat";
  if (!file_exists($status_file) && file_exists("/etc/SuSE-release")) {
    $status_file = "/var/lib/nagios/status.dat";
  }
  
  $q1="";
  if (array_key_exists('q1', $_GET)) {
    $q1=$_GET["q1"];
  }
  $q2="";
  if (array_key_exists('q2', $_GET)) {
    $q2=$_GET["q2"];
  }
  $alert_type="";
  if (array_key_exists('alert_type', $_GET)) {
    $alert_type=$_GET["alert_type"];
  }
  $host="";
  if (array_key_exists('host_name', $_GET)) {
    $host=$_GET["host_name"];
  }
  $indent="";
  if (array_key_exists('indent', $_GET)) {
    $indent=$_GET["indent"];
  }

  $result = array();
  $status_file_content = file_get_contents($status_file);

  if ($q1 == "alerts") {
    /* Add the service status object to result array */
    $result['alerts'] = query_alerts ($status_file_content, $alert_type, $host);
  }

  if ($q2 == "hosts") {
    /* Add the service status object to result array */
    $result['hosts'] = query_hosts ($status_file_content, $alert_type, $host);
  }

  /* Add host count object to the results */
  $result['hostcounts'] = query_host_count ($status_file_content);

  /* Add services runtime states */
  $result['servicestates'] = query_service_states ($status_file_content);

  /* Return results */
  if ($indent == "true") {
    hdp_mon_generate_response(indent(json_encode($result)));
  } else {
    hdp_mon_generate_response(json_encode($result));
  }

  # Functions
  /* Query service states */
  function query_service_states ($status_file_content) {
    $num_matches = preg_match_all("/servicestatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    $services_object = array ();
    $services_object["PUPPET"] = 0;
    foreach ($matches[0] as $object) {

      if (getParameter($object, "service_description") == HDFS_SERVICE_CHECK) {
        $services_object["HDFS"] = getParameter($object, "last_hard_state");
        if ($services_object["HDFS"] >= 1) {
          $services_object["HDFS"] = 1;
        }
        continue;
      }
      if (getParameter($object, "service_description") == MAPREDUCE_SERVICE_CHECK) {
        $services_object["MAPREDUCE"] = getParameter($object, "last_hard_state");
        if ($services_object["MAPREDUCE"] >= 1) {
          $services_object["MAPREDUCE"] = 1;
        }
        continue;
      }
      if (getParameter($object, "service_description") == HBASE_SERVICE_CHECK) {
        $services_object["HBASE"] = getParameter($object, "last_hard_state");
        if ($services_object["HBASE"] >= 1) {
          $services_object["HBASE"] = 1;
        }
        continue;
      }
      if (getParameter($object, "service_description") == HIVE_SERVICE_CHECK) {
        $services_object["HIVE"] = getParameter($object, "last_hard_state");
        if ($services_object["HIVE"] >= 1) {
          $services_object["HIVE"] = 1;
        }
        continue;
      }
      if (getParameter($object, "service_description") == OOZIE_SERVICE_CHECK) {
        $services_object["OOZIE"] = getParameter($object, "last_hard_state");
        if ($services_object["OOZIE"] >= 1) {
          $services_object["OOZIE"] = 1;
        }
        continue;
      }
      if (getParameter($object, "service_description") == WEBHCAT_SERVICE_CHECK) {
        $services_object["WEBHCAT"] = getParameter($object, "last_hard_state");
        if ($services_object["WEBHCAT"] >= 1) {
          $services_object["WEBHCAT"] = 1;
        }
        continue;
      }
      /* In case of zookeeper, service is treated running if alert is ok or warning (i.e partial
       * instances of zookeepers are running
       */
      if (getParameter($object, "service_description") == ZOOKEEPER_SERVICE_CHECK) {
        $services_object["ZOOKEEPER"] = getParameter($object, "last_hard_state");
        if ($services_object["ZOOKEEPER"] <= 1) {
          $services_object["ZOOKEEPER"] = 0;
        }
        continue;
      }
      if (getParameter($object, "service_description") == PUPPET_SERVICE_CHECK) {
        $state = getParameter($object, "last_hard_state");
        if ($state >= 1) {
          $services_object["PUPPET"]++;
        }
        continue;
      }
    }
    if ($services_object["PUPPET"] >= 1) {
      $services_object["PUPPET"] = 1;
    }
    $services_object = array_map('strval', $services_object);
    return $services_object;
  }

  /* Query host count */
  function query_host_count ($status_file_content) {
    $num_matches = preg_match_all("/hoststatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    $hostcounts_object = array ();
    $up_hosts = 0;
    $down_hosts = 0;

    foreach ($matches[0] as $object) {
      if (getParameter($object, "last_hard_state") != ok) {
        $down_hosts++;
      } else {
        $up_hosts++;
      }
    }
    $hostcounts_object['up_hosts'] = $up_hosts;
    $hostcounts_object['down_hosts'] = $down_hosts;
    $hostcounts_object = array_map('strval', $hostcounts_object);
    return $hostcounts_object;
  }

  /* Query Hosts */
  function query_hosts ($status_file_content, $alert_type, $host) {
    $hoststatus_attributes = array ("host_name", "current_state", "last_hard_state",
                              "plugin_output", "last_check", "current_attempt",
                              "last_hard_state_change", "last_time_up", "last_time_down",
                              "last_time_unreachable", "is_flapping", "last_check");

    $num_matches = preg_match_all("/hoststatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    $hosts_objects = array ();
    $i = 0;
    foreach ($matches[0] as $object) {
      $hoststatus = array ();
      $chost = getParameter($object, "host_name");
      if (empty($host) || $chost == $host) {
        foreach ($hoststatus_attributes as $attrib) {
          $hoststatus[$attrib] = htmlentities(getParameter($object, $attrib), ENT_COMPAT);
        }
        $hoststatus['alerts'] = query_alerts ($status_file_content, $alert_type, $chost);
        if (!empty($host)) {
          $hosts_objects[$i] = $hoststatus;
          $i++;
          break;
        }
      }
      if (!empty($hoststatus)) {
        $hosts_objects[$i] = $hoststatus;
        $i++;
      }
    }
    /* echo "COUNT : " . count ($services_objects) . "\n"; */
    return $hosts_objects;
  }

  /* Query Alerts */
  function query_alerts ($status_file_content, $alert_type, $host) {

    $servicestatus_attributes = array ("service_description", "host_name", "current_attempt",
                                       "current_state", "plugin_output", "last_hard_state_change", "last_hard_state",
                                       "last_time_ok", "last_time_warning", "last_time_unknown",
                                       "last_time_critical", "is_flapping", "last_check",
                                       "long_plugin_output");

    $num_matches = preg_match_all("/servicestatus \{([\S\s]*?)\}/", $status_file_content, $matches, PREG_PATTERN_ORDER);
    #echo $matches[0][0] . ", " . $matches[0][1] . "\n";
    #echo $matches[1][0] . ", " . $matches[1][1] . "\n";
    $services_objects = array ();
    $i = 0;
    foreach ($matches[1] as $object) {      
      $servicestatus = getParameterMap($object, $servicestatus_attributes);
      switch ($alert_type) {
      case "all":
        if (empty($host) || $servicestatus['host_name'] == $host) {
          $servicestatus['service_type'] = get_service_type($servicestatus['service_description']);
          $srv_desc = explode ("::",$servicestatus['service_description'],2);

          $servicestatus['service_description'] = $srv_desc[1];
        }
        break;
      case "nok":
        if (getParameterMapValue($map, "last_hard_state") != ok &&
           (empty($host) || getParameterMapValue($map, "host_name") == $host)) {
          foreach ($servicestatus_attributes as $attrib) {
            $servicestatus[$attrib] = htmlentities(getParameterMapValue($map, $attrib), ENT_COMPAT);
          }
          $servicestatus['service_type'] = get_service_type($servicestatus['service_description']);
          $srv_desc = explode ("::",$servicestatus['service_description'],2);
          $servicestatus['service_description'] = $srv_desc[1];
        }
        break;
      case "ok":
        if (getParameterMapValue($map, "last_hard_state") == ok &&
           (empty($host) || getParameterMapValue($map, "host_name") == $host)) {
          foreach ($servicestatus_attributes as $attrib) {
            $servicestatus[$attrib] = htmlentities(getParameterMapValue($map, $attrib), ENT_COMPAT);
          }
          $servicestatus['service_type'] = get_service_type($servicestatus['service_description']);
          $srv_desc = explode ("::",$servicestatus['service_description'],2);
          $servicestatus['service_description'] = $srv_desc[1];
        }
        break;
      case "warn":
        if (getParameterMapValue($map, "last_hard_state") == warn &&
           (empty($host) || getParameterMapValue($map, "host_name") == $host)) {
          foreach ($servicestatus_attributes as $attrib) {
            $servicestatus[$attrib] = htmlentities(getParameterMapValue($map, $attrib), ENT_COMPAT);
          }
          $servicestatus['service_type'] = get_service_type($servicestatus['service_description']);
          $srv_desc = explode ("::",$servicestatus['service_description'],2);
          $servicestatus['service_description'] = $srv_desc[1];
        }
        break;
      case "critical":
        if (getParameterMapValue($map, "last_hard_state") == critical &&
           (empty($host) || getParameterMapValue($map, "host_name") == $host)) {
          foreach ($servicestatus_attributes as $attrib) {
            $servicestatus[$attrib] = htmlentities(getParameterMapValue($map, $attrib), ENT_COMPAT);
          }
          $servicestatus['service_type'] = get_service_type($servicestatus['service_description']);
          $srv_desc = explode ("::",$servicestatus['service_description'],2);
          $servicestatus['service_description'] = $srv_desc[1];
        }
        break;
      }
      
      if (!empty($servicestatus)) {
        $services_objects[$i] = $servicestatus;
        $i++;
      }
    }

    // echo "COUNT : " . count ($services_objects) . "\n";
    return $services_objects;
  }

  function get_service_type($service_description)
  {
    $pieces = explode("::", $service_description);
    switch ($pieces[0]) {
	  case "DATANODE":
      case "NAMENODE":
      case "JOURNALNODE":
        $pieces[0] = "HDFS";
        break;
      case "JOBTRACKER":
	  case "TASKTRACKER":
        $pieces[0] = "MAPREDUCE";
        break;
      case "HBASEMASTER":
      case "REGIONSERVER":
        $pieces[0] = "HBASE";
        break;
      case "HIVE-METASTORE":
      case "HIVE-SERVER":
      case "WEBHCAT":
        $pieces[0] = "HIVE";
        break;
      case "ZKSERVERS":
	    $pieces[0] = "ZOOKEEPER";
        break;
      case "AMBARI":
	    $pieces[0] = "AMBARI";
      break;
      case "FLUME":
            $pieces[0] = "FLUME";
      break;      
      case "JOBHISTORY":
        $pieces[0] = "MAPREDUCE2";
        break;
      case "RESOURCEMANAGER":
      case "APP_TIMELINE_SERVER":
      case "NODEMANAGER":
        $pieces[0] = "YARN";
        break;
      case "STORM_UI_SERVER":
      case "NIMBUS":
      case "DRPC_SERVER":
      case "SUPERVISOR":
      case "STORM_REST_API":
        $pieces[0] = "STORM";
        break;
      case "NAGIOS":
      case "HDFS":
      case "MAPREDUCE":
      case "HBASE":
      case "ZOOKEEPER":
      case "OOZIE":
      case "GANGLIA":
      case "STORM":
      case "FALCON":
      case "KNOX":
      case "KAFKA":
      case "PUPPET":
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

  function getParameterMapValue($map, $key) {
    $value = $map[$key];

    if (!is_null($value))
      return "" . $value;

    return "";
  }


  function getParameterMap($object, $keynames) {

    $cnt = preg_match_all('/\t([\S]*)=[\n]?[\t]?([\S= ]*)/', $object, $matches, PREG_PATTERN_ORDER);

    $tmpmap = array_combine($matches[1], $matches[2]);

    $map = array();
    foreach ($keynames as $key) {
      $map[$key] = htmlentities($tmpmap[$key], ENT_COMPAT);
    }

    return $map;
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
?>
