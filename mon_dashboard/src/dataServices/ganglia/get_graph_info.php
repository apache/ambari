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

if (!hdp_mon_load_cluster_configuration()
    || !isset($GLOBALS["HDP_MON_CONFIG"])) {
  error_log("Could not find global configuration");
  hdp_mon_jmx_generate_error_response("Invalid Configuration Setup", 500);
  exit;
}

$GANGLIA_WEB_HOST_NAME = $GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_HOST"];
$GANGLIA_WEB_PORT = $GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_PORT"];
$GRID_NAME = $GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["GRID_NAME"];
// For now, all the slaves belong to the same one cluster, so pick any one service's slaves.
$GRID_SLAVES_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"]["SLAVES"];
$HDFS_SLAVES_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"]["SLAVES"];
$NAME_NODE_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["GANGLIA_CLUSTERS"]["NAMENODE"];
$JOB_TRACKER_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["GANGLIA_CLUSTERS"]["JOBTRACKER"];
$HBASE_MASTER_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["GANGLIA_CLUSTERS"]["HBASEMASTER"];
$HBASE_SLAVES_CLUSTER_NAME = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["GANGLIA_CLUSTERS"]["SLAVES"];

/** (Virtual) Constants. */
$PLACEHOLDERS = array( "%GangliaWebHostName%", "%GangliaWebPort%", "%GridName%", "%GridSlavesClusterName%",
    "%HDFSSlavesClusterName%", "%NameNodeClusterName%", "%JobTrackerClusterName%", "%HBaseMasterClusterName%",
    "%HBaseSlavesClusterName%" );

$ACTUALS = array( $GANGLIA_WEB_HOST_NAME, $GANGLIA_WEB_PORT, $GRID_NAME, $GRID_SLAVES_CLUSTER_NAME,
    $HDFS_SLAVES_CLUSTER_NAME, $NAME_NODE_CLUSTER_NAME, $JOB_TRACKER_CLUSTER_NAME, $HBASE_MASTER_CLUSTER_NAME,
    $HBASE_SLAVES_CLUSTER_NAME );

/** True-blue constants. */
define("HDP_MON_GRAPH_INFO_DIR_NAME", "graph_info/");
define("HDP_MON_GRAPH_INFO_COLLECTIONS_SUBDIR_NAME", "custom/");
define("HDP_MON_GRAPH_INFO_FILE_EXTENSION", ".json");

define("HDP_MON_QUERY_ARG__CONTEXT", "context");
define("HDP_MON_QUERY_ARG__COLLECTION", "collection");

define("HDP_MON_COLLECTION__ALL", "all");

include_once("../common/response.inc");

/** main() */

/* What we're here to serve - this will be a JSON string that represents
 * all the information for all the graphs needed by a particular context.
 *
 * To get a feel for the structure of the returned JSON, look at some of
 * the files in HDP_MON_GRAPH_INFO_DIR_NAME.
 */
$graphInfo = "";

/* Supported values of HDP_MON_QUERY_ARG__CONTEXT can be gotten by looking at
 * the subdirectories in HDP_MON_GRAPH_INFO_DIR_NAME - if it's in there, it's supported.
 */
$graphContext = $_GET[HDP_MON_QUERY_ARG__CONTEXT];

if( isset($graphContext) )
{
  $graphCollection = $_GET[HDP_MON_QUERY_ARG__COLLECTION];

  if( isset($graphCollection) )
  {
    if( $graphCollection == HDP_MON_COLLECTION__ALL )
    {
      $graphRawJson = file_get_contents( HDP_MON_GRAPH_INFO_DIR_NAME . "${graphContext}/${graphCollection}" .
          HDP_MON_GRAPH_INFO_FILE_EXTENSION );
    }
    else
    {
      $graphRawJson = file_get_contents( HDP_MON_GRAPH_INFO_DIR_NAME . "${graphContext}/" .
          HDP_MON_GRAPH_INFO_COLLECTIONS_SUBDIR_NAME . $graphCollection . HDP_MON_GRAPH_INFO_FILE_EXTENSION );
    }

    if( !empty($graphRawJson) )
    {
      $graphInfo = str_replace( $PLACEHOLDERS, $ACTUALS, $graphRawJson );
    }
  }
}

if( !empty($graphInfo) )
{
  hdp_mon_generate_response($graphInfo);
}
else
{
  header("HTTP/1.0 400 Bad Request");
}

?>
