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


// The includes are required to be included beforehand by whichever
// endpoint script using functionality from this file
// include_once("global_configs.inc");
// include_once("common.inc");

/**
 * Read contents from configuration json blob and return decoded json
 * as a mixed array
 * @param string filename Configuration file name or full path to file
 * @param boolean use_include Whether to use PHP include path
 * @return decoded json as a mixed array
 */
function hdp_mon_get_configuration_json($filename, $use_include) {
  $debug = hdp_mon_get_debug_mode();
  if ($debug) {
    hdp_mon_error_log("Loading configuration from file=".$filename
        .", use_include=".$use_include);
  }
  $file_contents = file_get_contents($filename, $use_include);
  if (!$file_contents) {
    hdp_mon_error_log("Error when trying to read configuration from "
        ."cluster_configuration.json");
    return NULL;
  }
  $config_data = json_decode($file_contents, TRUE);
  if (!$config_data) {
    hdp_mon_error_log("Error when trying to decode json from configuration file"
        .", file=".$filename);
  }
  if ($debug) {
    $json_dump = print_r($config_data, true);
    hdp_mon_error_log("Configuration dump [ START ]: \n".$json_dump
        ."\nConfiguration dump [ END ]\n");
  }
  return $config_data;
}

/**
 * Parse cluster configuration json into a global array
 * @param mixed_array $cfg_json
 * @return Configuration in a mixed array
 */
function hdp_mon_parse_configuration($cfg_json) {
  $debug = hdp_mon_get_debug_mode();
  if ($cfg_json["config_version"] != 1) {
    hdp_mon_error_log("Found config with invalid version, cannot handle version="
        .$cfg_json["config_version"]);
    return NULL;
  }
  $CONFIG = array();

  // overall info
  $CONFIG["STACK_VERSION"] = $cfg_json["stack_version"];
  $CONFIG["CLUSTER_NAME"] = $cfg_json["overall"]["cluster_name"];

  // dashboard specifics
  $CONFIG["HDP_MON"] = array();
  $CONFIG["HDP_MON"]["DASHBOARD_HOST"] =
      $cfg_json["overall"]["dashboard_host"];
  $CONFIG["HDP_MON"]["DASHBOARD_PORT"] =
      $cfg_json["overall"]["dashboard_port"];

  // ganglia info
  $CONFIG["GANGLIA"] = array();
  $CONFIG["GANGLIA"]["WEB_HOST"] =
      $cfg_json["overall"]["ganglia"]["web_host"];
  $CONFIG["GANGLIA"]["WEB_PORT"] =
      $cfg_json["overall"]["ganglia"]["web_port"];
  $CONFIG["GANGLIA"]["WEB_ROOT"] =
      $cfg_json["overall"]["ganglia"]["web_root"];
  if (isset($cfg_json["overall"]["ganglia"]["grid_name"])) {
    $CONFIG["GANGLIA"]["GRID_NAME"] =
        $cfg_json["overall"]["ganglia"]["grid_name"];
  } else {
    $CONFIG["GANGLIA"]["GRID_NAME"] = $CONFIG["CLUSTER_NAME"];
  }

  // Nagios info
  $CONFIG["NAGIOS"] = array();
  $CONFIG["NAGIOS"]["NAGIOSSERVER_HOST"] =
      $cfg_json["overall"]["nagios"]["nagiosserver_host"];
  $CONFIG["NAGIOS"]["NAGIOSSERVER_PORT"] =
      $cfg_json["overall"]["nagios"]["nagiosserver_port"];
  $CONFIG["NAGIOS"]["WEB_ROOT"] =
      $cfg_json["overall"]["nagios"]["web_root"];

  // jmx specific info
  $CONFIG["JMX"] = array();
  $CONFIG["JMX"]["TIMEOUT"] = 2;
  if (isset($cfg_json["overall"]["jmx"]["timeout"])
      && is_numeric($cfg_json["overall"]["jmx"]["timeout"])
      && $cfg_json["overall"]["jmx"]["timeout"] > 0) {
    $CONFIG["JMX"]["TIMEOUT"] = $cfg_json["overall"]["jmx"]["timeout"];
  }

  // service specific information
  // for now assuming only one service
  $CONFIG["SERVICES"] = array();

  // HDFS
  if (array_key_exists("HDFS", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["HDFS"]) == 1) {
    if ($cfg_json["overall"]["services"]["HDFS"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: HDFS Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["HDFS"] = array();
      $CONFIG["SERVICES"]["HDFS"]["NAMENODE_HOST"] =
          $cfg_json["overall"]["services"]["HDFS"][0]["namenode_host"];
      $CONFIG["SERVICES"]["HDFS"]["NAMENODE_PORT"] =
          $cfg_json["overall"]["services"]["HDFS"][0]["namenode_port"];
      $CONFIG["SERVICES"]["HDFS"]["NAMENODE_ADDR"] =
          $CONFIG["SERVICES"]["HDFS"]["NAMENODE_HOST"].":"
          .$CONFIG["SERVICES"]["HDFS"]["NAMENODE_PORT"];
      $CONFIG["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_HOST"] =
          $cfg_json["overall"]["services"]["HDFS"][0]["snamenode_host"];
      $CONFIG["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_PORT"] =
          $cfg_json["overall"]["services"]["HDFS"][0]["snamenode_port"];
      $CONFIG["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_ADDR"] =
          $CONFIG["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_HOST"].":"
          .$CONFIG["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_PORT"];
      $CONFIG["SERVICES"]["HDFS"]["TOTAL_DATANODES"] =
          $cfg_json["overall"]["services"]["HDFS"][0]["total_datanodes"];

      if (isset($cfg_json["overall"]["services"]["HDFS"][0]["ganglia_clusters"])){
        $CONFIG["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"] = array();
        $CONFIG["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"]["NAMENODE"] =
            $cfg_json["overall"]["services"]["HDFS"][0]
              ["ganglia_clusters"]["namenode"];
        $CONFIG["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"]["SLAVES"] =
            $cfg_json["overall"]["services"]["HDFS"][0]
              ["ganglia_clusters"]["slaves"];
      }
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: HDFS Service not found in configs");
  }

  // MAPREDUCE
  if (array_key_exists("MAPREDUCE", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["MAPREDUCE"]) > 0) {
    if ($cfg_json["overall"]["services"]["MAPREDUCE"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: MAPREDUCE Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["MAPREDUCE"] = array();
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBTRACKER_HOST"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["jobtracker_host"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBTRACKER_PORT"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["jobtracker_port"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBTRACKER_ADDR"] =
          $CONFIG["SERVICES"]["MAPREDUCE"]["JOBTRACKER_HOST"].":"
          .$CONFIG["SERVICES"]["MAPREDUCE"]["JOBTRACKER_PORT"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["TOTAL_TASKTRACKERS"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["total_tasktrackers"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBHISTORY_HOST"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["jobhistory_host"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBHISTORY_PORT"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["jobhistory_port"];
      $CONFIG["SERVICES"]["MAPREDUCE"]["JOBHISTORY_ADDR"] =
          $CONFIG["SERVICES"]["MAPREDUCE"]["JOBHISTORY_HOST"]
          .":".$CONFIG["SERVICES"]["MAPREDUCE"]["JOBHISTORY_PORT"];
      if (isset($cfg_json["overall"]["services"]["MAPREDUCE"]
          [0]["ganglia_clusters"])){
        $CONFIG["SERVICES"]["MAPREDUCE"]["GANGLIA_CLUSTERS"] = array();
        $CONFIG["SERVICES"]["MAPREDUCE"]["GANGLIA_CLUSTERS"]["JOBTRACKER"] =
            $cfg_json["overall"]["services"]["MAPREDUCE"][0]
              ["ganglia_clusters"]["jobtracker"];
        $CONFIG["SERVICES"]["MAPREDUCE"]["GANGLIA_CLUSTERS"]["SLAVES"] =
            $cfg_json["overall"]["services"]["MAPREDUCE"][0]
              ["ganglia_clusters"]["slaves"];
      }
      $CONFIG["SERVICES"]["MAPREDUCE"]["SCHEDULER_TYPE"] =
          $cfg_json["overall"]["services"]["MAPREDUCE"][0]["scheduler_type"];
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: MAPREDUCE Service not found in configs");
  }

  // HBASE
  if (array_key_exists("HBASE", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["HBASE"]) > 0) {
    if ($cfg_json["overall"]["services"]["HBASE"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: HBASE Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["HBASE"] = array();
      $CONFIG["SERVICES"]["HBASE"]["HBASEMASTER_HOST"] =
          $cfg_json["overall"]["services"]["HBASE"][0]["hbasemaster_host"];
      $CONFIG["SERVICES"]["HBASE"]["HBASEMASTER_PORT"] =
          $cfg_json["overall"]["services"]["HBASE"][0]["hbasemaster_port"];
      $CONFIG["SERVICES"]["HBASE"]["HBASEMASTER_ADDR"] =
          $CONFIG["SERVICES"]["HBASE"]["HBASEMASTER_HOST"].":"
          .    $CONFIG["SERVICES"]["HBASE"]["HBASEMASTER_PORT"];
      $CONFIG["SERVICES"]["HBASE"]["TOTAL_REGIONSERVERS"] =
          $cfg_json["overall"]["services"]["HBASE"][0]["total_regionservers"];

      if (isset($cfg_json["overall"]["services"]["HBASE"]
          [0]["ganglia_clusters"])){
        $CONFIG["SERVICES"]["HBASE"]["GANGLIA_CLUSTERS"] = array();
        $CONFIG["SERVICES"]["HBASE"]["GANGLIA_CLUSTERS"]["HBASEMASTER"] =
            $cfg_json["overall"]["services"]["HBASE"][0]
              ["ganglia_clusters"]["hbasemaster"];
        $CONFIG["SERVICES"]["HBASE"]["GANGLIA_CLUSTERS"]["SLAVES"] =
            $cfg_json["overall"]["services"]["HBASE"][0]
              ["ganglia_clusters"]["slaves"];
      }
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: HBASE Service not found in configs");
  }

  // ZK
  if (array_key_exists("ZOOKEEPER", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["ZOOKEEPER"]) > 0) {
    if ($cfg_json["overall"]["services"]["ZOOKEEPER"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: ZOOKEEPER Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["ZOOKEEPER"] = array();
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: ZOOKEEPER Service not found in configs");
  }

  // HIVE-METASTORE
  if (array_key_exists("HIVE-METASTORE", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["HIVE-METASTORE"]) > 0) {
    if ($cfg_json["overall"]["services"]["HIVE-METASTORE"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: HIVE-METASTORE Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["HIVE-METASTORE"] = array();
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: HIVE-METASTORE Service not found in configs");
  }

  // OOZIE
  if (array_key_exists("OOZIE", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["OOZIE"]) > 0) {
    if ($cfg_json["overall"]["services"]["OOZIE"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: OOZIE Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["OOZIE"] = array();
      $CONFIG["SERVICES"]["OOZIE"]["OOZIE_HOST"] =
      $cfg_json["overall"]["services"]["OOZIE"][0]["oozie_host"];
        
      $CONFIG["SERVICES"]["OOZIE"]["OOZIE_PORT"] =
      $cfg_json["overall"]["services"]["OOZIE"][0]["oozie_port"];
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: OOZIE Service not found in configs");
  }

  // TEMPLETON
  if (array_key_exists("TEMPLETON", $cfg_json["overall"]["services"])
      && count($cfg_json["overall"]["services"]["TEMPLETON"]) > 0) {
    if ($cfg_json["overall"]["services"]["TEMPLETON"][0]["installed"] == FALSE) {
      if ($debug) {
        hdp_mon_error_log("DEBUG: TEMPLETON Service installed flag set to false");
      }
    }
    else {
      $CONFIG["SERVICES"]["TEMPLETON"] = array();
    }
  }
  else if ($debug) {
    hdp_mon_error_log("DEBUG: TEMPLETON Service not found in configs");
  }

  return $CONFIG;
}

/**
 * Load cluster config into $GLOBALS["HDP_MON_CONFIG"]
 * @return true/false on whether loaded successfully
 */
function hdp_mon_load_cluster_configuration() {
  if (isset($GLOBALS["HDP_MON_CONFIG_INITIALIZED"])) {
    // initialized set to true/false based on whether init successful
    return $GLOBALS["HDP_MON_CONFIG_INITIALIZED"];
  }

  $cfg_file = "";
  if (isset($GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"])) {
    // use prefix ../ iff path is not absolute
    if (substr($GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"], 0, 1) != "/") {
      $cfg_file = "../".$GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"];
    }
    else {
      $cfg_file = $GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"];
    }
  }
  if ($cfg_file == "") {
    $cfg_file = "../conf/cluster_configuration.json";
  }
  $use_include = true;
  if (substr($cfg_file, 0, 1) == "/") {
    $use_include = false;
  }

  $cfg_json = hdp_mon_get_configuration_json($cfg_file, $use_include);
  if ($cfg_json == NULL) {
    hdp_mon_error_log("Could not load configuration json from cfg_file"
        .", file=".$cfg_file.", use_include=".$use_include);
    $GLOBALS["HDP_MON_CONFIG_INITIALIZED"] = FALSE;
    return NULL;
  }
  $conf = hdp_mon_parse_configuration($cfg_json);
  if ($conf == NULL) {
    hdp_mon_error_log("Could not parse configuration json from cfg_file"
        .", file=".$cfg_file.", use_include=".$use_include
        .", json_dump=".$cfg_json);
    $GLOBALS["HDP_MON_CONFIG_INITIALIZED"] = FALSE;
    return NULL;
  }
  $GLOBALS["HDP_MON_CONFIG"] = $conf;
  $GLOBALS["HDP_MON_CONFIG_INITIALIZED"] = TRUE;
  return TRUE;
}


?>
