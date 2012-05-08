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

include_once("../common/global_configs.inc");
include_once("../common/common.inc");
include_once("../common/cluster_configuration.inc");
include_once("../common/response.inc");
include_once("hdp_mon_jmx_helpers.inc");

function hdp_mon_jmx_generate_error_response($message, $code) {
  $result = array(
    "result" => "Error",
    "error_message" => $message);

  header('Content-type: application/json');
  if ($code == 400) {
    header("HTTP/1.0 400 Bad Request");
  }
  else {
    header("HTTP/1.0 500 Internal Server Error");
  }
  print(json_encode($result));
}

function hdp_mon_jmx_validate_info_type($type) {
  if ($type != "hdfs"
 	&& $type != "mapreduce"
    && $type != "hbase"
    && $type != "cluster") {
    hdp_mon_jmx_generate_error_response("Invalid info type provided", 400);
    return false;
  }
  return true;
}

function hdp_mon_jmx_validate_info_level($level) {
  if ($level != "summary"
  && $level != "detailed") {
    hdp_mon_jmx_generate_error_response("Invalid info level provided", 400);
    return false;
  }
  return true;
}


/*
 * $_GET["info_type"]: { "cluster", "hdfs", "mapreduce", "hbase" }
 * $_GET["info_level"]: { "summary", "detail" }
 *   - default summary
 */
if (!isset($_GET["info_type"])) {
  hdp_mon_jmx_generate_error_response("info_type not set", 400);
  exit;
}

$info_type = $_GET["info_type"];

$info_level = "summary";
if (isset($_GET["info_level"])) {
  $info_level = $_GET["info_level"];
}

if (!hdp_mon_jmx_validate_info_type($info_type)) {
  exit;
}

if (!hdp_mon_jmx_validate_info_level($info_level)) {
  exit;
}

if (!hdp_mon_load_cluster_configuration()
    || !isset($GLOBALS["HDP_MON_CONFIG"])) {
  hdp_mon_error_log("Could not find global configuration");
  hdp_mon_jmx_generate_error_response("Invalid Configuration Setup", 500);
  exit;
}


$info = "";
if ($info_type == "cluster") {
  $info = hdp_mon_jmx_get_cluster_info();
}
else if ($info_type == "hdfs") {
  $info = hdp_mon_jmx_get_hdfs_info(TRUE);
}
else if ($info_type == "mapreduce") {
  $info = hdp_mon_jmx_get_mapreduce_info(TRUE);
}
else if ($info_type == "hbase") {
  $info = hdp_mon_jmx_get_hbase_info(TRUE);
}

if ($info != "") {
  hdp_mon_generate_response($info);
}
else {
  hdp_mon_jmx_generate_error_response("No JMX data found", 500);
}

?>
