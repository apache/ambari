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

// Expected to be included by endpoint script
// include_once("../common/common.inc");

/**
 * Function to get jmx info from a given url
 * @param jmx_url  URL
 * @param jmx_url_timeout curl http get timeout
 * @return json decoded associative array with all required data
 * @return NULL if there is a failure
 */
function hdp_mon_jmx_get_jmx_data($jmx_url, $jmx_url_timeout) {
  $debug = hdp_mon_get_debug_mode();

  // Get data from given url
  $handle = curl_init($jmx_url);

  // Set required curl options
  curl_setopt($handle, CURLOPT_TIMEOUT, $jmx_url_timeout);
  curl_setopt($handle, CURLOPT_HEADER, false);
  curl_setopt($handle, CURLOPT_RETURNTRANSFER, true);

  // Get data
  $content = curl_exec($handle);

  if(!$content || curl_errno($handle)) {
    // error occurred
    hdp_mon_error_log("Error when accessing jmx info: "
        .", url=".$jmx_url
        .", errno=".curl_errno($handle)
        .", error=".curl_error($handle));
    return NULL;
  }

  $info = curl_getinfo($handle);
  if ($debug) {
    hdp_mon_error_log("Took ".$info["total_time"]
              ." seconds to send a request to "
              .$info["url"]."\n");
    hdp_mon_error_log(print_r($info, true));
  }
  curl_close($handle);

  $jmx=json_decode($content, true);
  if ($debug) {
    hdp_mon_error_log("JMX Dump ".$jmx_url." [ START ]\n"
      .print_r($jmx, true)
      ."\nJMX Dump [ END ]\n");
  }
  if (!$jmx) {
    hdp_mon_error_log("Error when trying to decode json data"
        .", url=".$jmx_url.", content=".$content);
  }
  return $jmx;
}

/**
 * Function to parse hdfs namenode jmx output
 * @param data json-decoded data as associative array
 * @return mixed array of hdfs info
 */
function hdp_mon_jmx_parse_hdfs_info($data) {
  $debug = hdp_mon_get_debug_mode();

  // parse jmx data for NN specifics
  $hdfs_info = array();
  $hdfs_info["service_type"] = "HDFS";
  $hdfs_info["installed"] = TRUE;

  $hdfs_info["namenode_addr"] = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]
      ["HDFS"]["NAMENODE_ADDR"];
  $hdfs_info["secondary_namenode_addr"] = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]
      ["HDFS"]["SECONDARY_NAMENODE_ADDR"];
  $hdfs_info["total_nodes"] = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]
  ["HDFS"]["TOTAL_DATANODES"];

  foreach ($data["beans"] as $mbean) {
    if ($mbean["name"] == "Hadoop:service=NameNode,name=NameNodeInfo") {
      $livenodesinfo = json_decode(stripslashes($mbean["LiveNodes"]), true);
      $deadnodesinfo = json_decode(stripslashes($mbean["DeadNodes"]), true);
      $decomnodesinfo = json_decode(stripslashes($mbean["DecomNodes"]), true);
      $hdfs_info["live_nodes"] = count($livenodesinfo);
      $hdfs_info["dead_nodes"] = count($deadnodesinfo);
      $hdfs_info["decommissioning_nodes"] = count($decomnodesinfo);
      $hdfs_info["version"] = $mbean["Version"];
      $hdfs_info["safemode"] = ($mbean["Safemode"] == "") ? FALSE : TRUE;
      if ($hdfs_info["safemode"]) {
        $hdfs_info["safemode_reason"] = $mbean["Safemode"];
      }
      $hdfs_info["pending_upgrades"] = (!$mbean["UpgradeFinalized"])?
          TRUE : FALSE;
      $hdfs_info["dfs_configured_capacity"] = $mbean["Total"];
      $hdfs_info["dfs_percent_used"] = round($mbean["PercentUsed"], 2);
      $hdfs_info["dfs_percent_remaining"] =
          round($mbean["PercentRemaining"], 2);
      $hdfs_info["dfs_total_bytes"] = $mbean["Total"];
      $hdfs_info["dfs_used_bytes"] = $mbean["Used"];
      $hdfs_info["nondfs_used_bytes"] = $mbean["NonDfsUsedSpace"];
      $hdfs_info["dfs_free_bytes"] = $mbean["Free"];
    }
    elseif ($mbean["name"] == "java.lang:type=Runtime") {
      $hdfs_info["start_time"] = round($mbean["StartTime"]/1000, 0);
    }
    elseif ($mbean["name"] ==
	            "Hadoop:service=NameNode,name=FSNamesystemMetrics") {
    $hdfs_info["dfs_dirfiles_count"] = $mbean["FilesTotal"];
    $hdfs_info["dfs_blocks_total"] = $mbean["BlocksTotal"];
    $hdfs_info["dfs_blocks_underreplicated"] = $mbean["UnderReplicatedBlocks"];
    $hdfs_info["dfs_blocks_missing"] = $mbean["MissingBlocks"];
    $hdfs_info["dfs_blocks_corrupt"] = $mbean["CorruptBlocks"];
    }
    elseif ($mbean["name"] ==
	            "Hadoop:service=NameNode,name=FSNamesystemState") {
    $hdfs_info["dfs_state"] = $mbean["FSState"];
    }
    elseif ($mbean["name"] == "java.lang:type=Memory") {
      $hdfs_info["memory_heap_used"] =
      $mbean["HeapMemoryUsage"]["used"];
      $hdfs_info["memory_heap_max"] = $mbean["HeapMemoryUsage"]["max"];
    }
  }
  if ($debug) {
    hdp_mon_error_log("HDFS Info Dump [ START ]\n"
        .print_r($hdfs_info, true)
        ."\nHDFS Info [ END ]\n");
  }
  return $hdfs_info;
}

/*
 * Helper function to parse scheduler json from jmx
 */
function hdp_mon_jmx_parse_mr_queue_info($queue_info_json) {
  $debug = hdp_mon_get_debug_mode();

  $queue_info = array();

  $queue_type = $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]
      ["MAPREDUCE"]["SCHEDULER_TYPE"];
  $index = strrpos($queue_type, ".");
  if ($index === FALSE) {
    $queue_info["type"] = $queue_type;
  }
  else {
    $queue_info["type"] = substr($queue_type, $index+1);
  }

  if ($queue_info["type"] != "CapacityTaskScheduler") {
      return $queue_info;
  }

  $queue_info["type"] = "CapacityTaskScheduler";

  $qjson_str = str_replace("\\\"", "", $queue_info_json);
  $qdata = json_decode($qjson_str, true);
  if (!$qdata) {
    hdp_mon_error_log("WARN: error when parsing queue info data, queue_info="
        .$queue_info_json);
  }

  $queue_info["queues"] = array();
  foreach ($qdata as $qname => $qinfo) {
    if ($qinfo["info"] == "N/A") {
      continue;
    }
    $qspecs = explode("\n", $qinfo["info"]);
    if (count($qspecs) <= 1) {
      continue;
    }

    $parsed_info = array();
    $parsed_info["state"] = $qinfo["state"];

    if ($debug) {
      hdp_mon_error_log("Queue specifications"
          .", queue_name=".$qname.", info="
          .print_r($qspecs, true));
    }

    // overall = 0, maps = 1, reduces = 2, jobs = 3
    $state = -1;
    $prefix = array ( 1 => "map_", 2 => "reduce_");
    foreach ($qspecs as $line) {
      if (strpos($line, "-----") === 0) {
        continue;
      }
      else if (strpos($line, "Queue configuration") === 0) {
        $state = 0;
        continue;
      }
      else if (strpos($line, "Map tasks") === 0) {
        $state = 1;
        continue;
      }
      else if (strpos($line, "Reduce tasks") === 0) {
        $state = 2;
        continue;
      }
      else if (strpos($line, "Job info") === 0) {
        $state = 3;
        continue;
      }
      if ($state == 0) {
        if (strpos($line, "Capacity Percentage") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info["capacity_percentage"] = str_replace("%", "",
                $tokens[1]);
          }
        }
        else if (strpos($line, "User Limit") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info["user_limit"] = str_replace("%", "",
                $tokens[1]);
          }
        }
        else if (strpos($line, "Priority Supported") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            if (strtolower($tokens[1]) == "no") {
              $parsed_info["priority_supported"] = FALSE;
            }
            else {
              $parsed_info["priority_supported"] = TRUE;
            }
          }
        }
      }
      else if ($state == 1 || $state == 2) {
        if (strpos($line, "Capacity") === 0) {
          $tokens = explode(" ", $line);
          if (count($tokens) == 3 && isset($tokens[1])
              && is_numeric($tokens[1])) {
            $parsed_info[$prefix[$state]."capacity"] = $tokens[1];
          }
        }
        else if (strpos($line, "Used capacity") === 0) {
          $tokens = explode(" ", $line);
          if (count($tokens) > 3 && isset($tokens[1])
          && is_numeric($tokens[1])) {
            $parsed_info[$prefix[$state]."used_capacity"] = $tokens[1];
          }
        }
        else if (strpos($line, "Running tasks") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info[$prefix[$state]."running_tasks"] = $tokens[1];
          }
        }
      }
      else if ($state == 3) {
        if (strpos($line, "Number of Waiting") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info["waiting_jobs"] = $tokens[1];
          }
        }
        else if (strpos($line, "Number of Initializing") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info["initializing_jobs"] = $tokens[1];
          }
        }
        else if (strpos($line, "Number of users") === 0) {
          $tokens = explode(":", $line);
          if (isset($tokens[1])) {
            $parsed_info["users_with_submitted_jobs"] = $tokens[1];
          }
        }
      }
    }
    $queue_info["queues"][$qname] = $parsed_info;
  }
  return $queue_info;
}

/**
* Function to parse mapreduce jobtracker jmx output
* @param data json-decoded data as associative array
* @return mixed array of mapreduce info
*/
function hdp_mon_jmx_parse_mapreduce_info($data) {
  $debug = hdp_mon_get_debug_mode();

  // parse jmx data for JT specifics
  $mr_info = array();
  $mr_info["service_type"] = "MAPREDUCE";
  $mr_info["installed"] = TRUE;

  $mr_info["jobtracker_addr"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_ADDR"];
  $mr_info["trackers_total"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["TOTAL_TASKTRACKERS"];
  $mr_info["jobhistory_addr"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBHISTORY_ADDR"];

  foreach ($data["beans"] as $mbean) {
    if ($mbean["name"] == "Hadoop:service=JobTracker,name=JobTrackerInfo") {
      $trackerinfo = json_decode($mbean["SummaryJson"], true);
      $mr_info["trackers_live"] = $trackerinfo["alive"];
      $mr_info["trackers_graylisted"] = $trackerinfo["graylisted"];
      $mr_info["trackers_blacklisted"] = $trackerinfo["blacklisted"];
      $mr_info["version"] = $mbean["Version"];
      $mr_info["queue_info"] =
          hdp_mon_jmx_parse_mr_queue_info($mbean["QueueInfoJson"]);
      $mr_info["waiting_jobs"] = 0;
      if (isset($mr_info["queue_info"]["queues"])
          && is_array($mr_info["queue_info"]["queues"])
          && !empty($mr_info["queue_info"]["queues"])) {
        foreach ($mr_info["queue_info"]["queues"] as $qname=>$qinfo) {
          if (isset($qinfo["waiting_jobs"])
              && is_numeric($qinfo["waiting_jobs"])) {
            $mr_info["waiting_jobs"] += $qinfo["waiting_jobs"];
          }
        }
      }
    }
    elseif ($mbean["name"] ==
              "Hadoop:service=JobTracker,name=JobTrackerMetrics") {
      $mr_info["trackers_excluded"] = $mbean["trackers_decommissioned"];
      $mr_info["map_task_capacity"] = $mbean["map_slots"];
      $mr_info["reduce_task_capacity"] = $mbean["reduce_slots"];
      $mr_info["job_total_submissions"] = $mbean["jobs_submitted"];
      $mr_info["job_total_completions"] = $mbean["jobs_completed"];
      $mr_info["running_jobs"] = $mbean["jobs_running"];
      $mr_info["running_map_tasks"] = $mbean["running_maps"];
      $mr_info["running_reduce_tasks"] = $mbean["running_reduces"];
      $mr_info["occupied_map_slots"] = $mbean["occupied_map_slots"];
      $mr_info["occupied_reduce_slots"] = $mbean["occupied_reduce_slots"];
      $mr_info["reserved_map_slots"] = $mbean["reserved_map_slots"];
      $mr_info["reserved_reduce_slots"] = $mbean["reserved_reduce_slots"];
      $mr_info["waiting_maps"] = $mbean["waiting_maps"];
      $mr_info["waiting_reduces"] = $mbean["waiting_reduces"];
    }
    elseif ($mbean["name"] == "java.lang:type=Runtime") {
      $mr_info["start_time"] = round($mbean["StartTime"]/1000, 0);
    }
    elseif ($mbean["name"] == "java.lang:type=Memory") {
      $mr_info["memory_heap_used"] =
          $mbean["HeapMemoryUsage"]["used"];
      $mr_info["memory_heap_max"] = $mbean["HeapMemoryUsage"]["max"];
    }
  }

  $mr_info["average_node_capacity"] = 0;
  if ($mr_info["trackers_live"] > 0) {
    $mr_info["average_node_capacity"] =
    round((($mr_info["map_task_capacity"] + $mr_info["reduce_task_capacity"])/
    $mr_info["trackers_live"]),0);
  }
  if ($debug) {
    hdp_mon_error_log("MAPREDUCE Info Dump [ START ]\n"
        .print_r($mr_info, true)
        ."\nMAPREDUCE Info [ END ]\n");
  }
  return $mr_info;
}

/**
* Function to parse hbase master jmx output
* @param data json-decoded data as associative array
* @return mixed array of hbase info
*/
function hdp_mon_jmx_parse_hbase_info($data) {
  $debug = hdp_mon_get_debug_mode();

  // parse jmx data for HBase Master specifics
  $hbase_info = array();
  $hbase_info["service_type"] = "HBASE";
  $hbase_info["installed"] = TRUE;

  $hbase_info["hbasemaster_addr"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_ADDR"];
  $hbase_info["total_regionservers"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["TOTAL_REGIONSERVERS"];

  foreach ($data["beans"] as $mbean) {
    if ($mbean["name"] == "hadoop:service=HBase,name=Info") {
      $hbase_info["version"] = $mbean["version"].", r".$mbean["revision"];
    }
    elseif ($mbean["name"] == "java.lang:type=Runtime") {
      $hbase_info["start_time"] = round($mbean["StartTime"]/1000, 0);
    }
    elseif ($mbean["name"] == "hadoop:service=Master,name=Master") {
      $hbase_info["cluster_id"] = $mbean["ClusterId"];
      $hbase_info["start_time"] = round($mbean["MasterStartTime"]/1000, 0);
      $hbase_info["active_time"] = round($mbean["MasterActiveTime"]/1000, 0);
      $hbase_info["coprocessors"] = $mbean["Coprocessors"];
      $hbase_info["average_load"] = $mbean["AverageLoad"];
      $hbase_info["regions_in_transition_count"] =
          count($mbean["RegionsInTransition"]);
      $hbase_info["live_regionservers"] =
          count($mbean["RegionServers"]);
      $hbase_info["zookeeper_quorum"] =
          explode(",", $mbean["ZookeeperQuorum"]);
      $hbase_info["dead_regionservers"] = count($mbean["DeadRegionServers"]);
    }
    elseif ($mbean["name"] == "java.lang:type=Memory") {
      $hbase_info["memory_heap_used"] =
          $mbean["HeapMemoryUsage"]["used"];
      $hbase_info["memory_heap_max"] = $mbean["HeapMemoryUsage"]["max"];
    }
  }
  if ($debug) {
    hdp_mon_error_log("HBASE Info Dump [ START ]\n"
        .print_r($hbase_info, true)
        ."\nHBASE Info [ END ]\n");
  }
  return $hbase_info;
}

/**
 * Function to get HDFS related info from
 * the Namenode's JMX servlet.
 * @return json string with all required data
 * @return NULL if there is a failure
 */
function hdp_mon_jmx_get_hdfs_info($in_json) {
  $debug = hdp_mon_get_debug_mode();
  $hdfs_info = array();
  if (array_key_exists("HDFS", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $namenode_jmx_url = "http://".
        $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["NAMENODE_ADDR"]
        ."/jmx";
    $jmx_url_timeout = $GLOBALS["HDP_MON_CONFIG"]["JMX"]["TIMEOUT"];

    $data = hdp_mon_jmx_get_jmx_data($namenode_jmx_url,
                         $jmx_url_timeout, false);
    if (!$data) {
      hdp_mon_error_log("Could not retrieve any data from NameNode"
          .", url=".$namenode_jmx_url);
      return NULL;
    }
    $hdfs_info = hdp_mon_jmx_parse_hdfs_info($data);
  }
  else {
    // hdfs not installed
    if ($debug) {
      hdp_mon_error_log("HDFS Service is not available in configuration");
    }
    $hdfs_info["installed"] = FALSE;
  }

  $result = array("hdfs" => $hdfs_info,
      "overall" => hdp_mon_helper_get_overall_info());

  if ($in_json) {
    return json_encode($result);
  }
  else {
    return $result;
  }
}

function hdp_mon_jmx_get_mapreduce_info($in_json) {
  $debug = hdp_mon_get_debug_mode();
  $mr_info = array();
  if (array_key_exists("MAPREDUCE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $jt_jmx_url = "http://"
        .$GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_ADDR"]
        ."/jmx";
    $jmx_url_timeout = $GLOBALS["HDP_MON_CONFIG"]["JMX"]["TIMEOUT"];

    $data = hdp_mon_jmx_get_jmx_data($jt_jmx_url,
                         $jmx_url_timeout, false);
    if (!$data) {
      hdp_mon_error_log("Could not retrieve any data from JobTracker"
          .", url=".$jt_jmx_url);
      return NULL;
    }

    $mr_info = hdp_mon_jmx_parse_mapreduce_info($data);
  }
  else {
    // mr not installed
    if ($debug) {
      hdp_mon_error_log("MAPREDUCE Service is not available in configuration");
    }
    $mr_info["installed"] = FALSE;
  }

  $result = array("mapreduce" => $mr_info,
      "overall" => hdp_mon_helper_get_overall_info());

  if ($in_json) {
    return json_encode($result);
  }
  else {
    return $result;
  }
}

function hdp_mon_jmx_get_hbase_info($in_json) {
  $debug = hdp_mon_get_debug_mode();
  $hbase_info = array();
  if (array_key_exists("HBASE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $hbm_jmx_url = "http://"
    .$GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_ADDR"]
    ."/jmx";
    $jmx_url_timeout = $GLOBALS["HDP_MON_CONFIG"]["JMX"]["TIMEOUT"];

    $data = hdp_mon_jmx_get_jmx_data($hbm_jmx_url,
    $jmx_url_timeout, false);
    if (!$data) {
      hdp_mon_error_log("Could not retrieve any data from HBase Master"
          .", url=".$hbm_jmx_url);
      return NULL;
    }

    $hbase_info = hdp_mon_jmx_parse_hbase_info($data);
  }
  else {
    // hbase not installed
    if ($debug) {
      hdp_mon_error_log("HBASE Service is not available in configuration");
    }
    $hbase_info["installed"] = FALSE;
  }

  $result = array("hbase" => $hbase_info,
      "overall" => hdp_mon_helper_get_overall_info());

  if ($in_json) {
    return json_encode($result);
  }
  else {
    return $result;
  }
}

function hdp_mon_helper_get_overall_info() {
  $overall = array();
  $overall["ganglia_url"] = hdp_mon_helper_get_ganglia_url();
  $overall["nagios_url"] = hdp_mon_helper_get_nagios_url();
  if (array_key_exists("OOZIE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])){
    $overall["oozie_url"] = hdp_mon_helper_get_oozie_url();
  }else {
    $overall["oozie_url"] = "";
  }
  $overall["hdfs_installed"] =
      array_key_exists("HDFS", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["mapreduce_installed"] =
      array_key_exists("MAPREDUCE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["hbase_installed"] =
      array_key_exists("HBASE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["zookeeper_installed"] =
      array_key_exists("ZOOKEEPER", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["oozie_installed"] =
      array_key_exists("OOZIE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["hcatalog_installed"] =
      array_key_exists("HIVE-METASTORE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);
  $overall["templeton_installed"] =
      array_key_exists("TEMPLETON", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]);

  return $overall;
}

function hdp_mon_helper_get_oozie_url() {
  // oozie info
  $oozie_url =
      "http://".$GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["OOZIE"]["OOZIE_HOST"]
      .":".$GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["OOZIE"]["OOZIE_PORT"]."/oozie";
  return $oozie_url;
}

function hdp_mon_helper_get_ganglia_url() {
  // ganglia info
  $ganglia_url = "http://".$GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_HOST"]
      .":".$GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_PORT"];
  if (substr($GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_ROOT"], 0, 1) != "/") {
    $ganglia_url .= "/";
  }
  $ganglia_url .= $GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_ROOT"];
  return $ganglia_url;
}

function hdp_mon_helper_get_nagios_url() {
  // nagios info
  $nagios_url =
      "http://".$GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["NAGIOSSERVER_HOST"]
      .":".$GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["NAGIOSSERVER_PORT"];
  if (substr($GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["WEB_ROOT"], 0, 1) != "/") {
    $nagios_url .= "/";
  }
  $nagios_url .= $GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["WEB_ROOT"];
  return $nagios_url;
}

/**
 * Helper function - aids unit testing
 */
function hdp_mon_helper_get_cluster_info($hdfs_info, $mr_info, $hbase_info) {
  $result = array();

  $result["overall"] = hdp_mon_helper_get_overall_info();

  // hdfs info
  if (array_key_exists("HDFS", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $result["hdfs"] = array();
    $result["hdfs"]["namenode_addr"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["NAMENODE_ADDR"];
    $result["hdfs"]["secondary_namenode_addr"] =
      $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_ADDR"];
    $result["hdfs"]["namenode_starttime"] = $hdfs_info["start_time"];
    $result["hdfs"]["total_nodes"] = $hdfs_info["total_nodes"];
    $result["hdfs"]["live_nodes"] = $hdfs_info["live_nodes"];
    $result["hdfs"]["dead_nodes"] = $hdfs_info["dead_nodes"];
    $result["hdfs"]["decommissioning_nodes"] =
        $hdfs_info["decommissioning_nodes"];
    $result["hdfs"]["dfs_blocks_underreplicated"] =
        $hdfs_info["dfs_blocks_underreplicated"];
    $result["hdfs"]["safemode"] = $hdfs_info["safemode"];
    $result["hdfs"]["pending_upgrades"] = $hdfs_info["pending_upgrades"];
    $result["hdfs"]["dfs_configured_capacity"] =
        $hdfs_info["dfs_configured_capacity"];
    $result["hdfs"]["dfs_percent_used"] = $hdfs_info["dfs_percent_used"];
    $result["hdfs"]["dfs_percent_remaining"] =
        $hdfs_info["dfs_percent_remaining"];
    $result["hdfs"]["dfs_total_bytes"] = $hdfs_info["dfs_total_bytes"];
    $result["hdfs"]["dfs_used_bytes"] = $hdfs_info["dfs_used_bytes"];
    $result["hdfs"]["nondfs_used_bytes"] = $hdfs_info["nondfs_used_bytes"];
    $result["hdfs"]["dfs_free_bytes"] = $hdfs_info["dfs_free_bytes"];
  }

  // mr info
  if (array_key_exists("MAPREDUCE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $result["mapreduce"] = array();
    $result["mapreduce"]["jobtracker_addr"] =
        $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_ADDR"];
    $result["mapreduce"]["jobtracker_starttime"] = $mr_info["start_time"];
    $result["mapreduce"]["running_jobs"] = $mr_info["running_jobs"];
    $result["mapreduce"]["waiting_jobs"] = $mr_info["waiting_jobs"];
    $result["mapreduce"]["trackers_total"] = $mr_info["trackers_total"];
    $result["mapreduce"]["trackers_live"] = $mr_info["trackers_live"];
    $result["mapreduce"]["trackers_graylisted"] =
        $mr_info["trackers_graylisted"];
    $result["mapreduce"]["trackers_blacklisted"] =
        $mr_info["trackers_blacklisted"];
  }

  // hbase info
  if (array_key_exists("HBASE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $result["hbase"] = array();
    $result["hbase"]["hbasemaster_addr"] =
        $GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_ADDR"];
    $result["hbase"]["total_regionservers"] = $hbase_info["total_regionservers"];
    $result["hbase"]["hbasemaster_starttime"] = $hbase_info["start_time"];
    $result["hbase"]["live_regionservers"] = $hbase_info["live_regionservers"];
    $result["hbase"]["dead_regionservers"] = $hbase_info["dead_regionservers"];
    $result["hbase"]["regions_in_transition_count"] =
        $hbase_info["regions_in_transition_count"];
  }

  // Placeholder until UI handles sections of data
  foreach ($result["hdfs"] as $key=>$val) {
    $result["overall"][$key] = $val;
  }
  foreach ($result["mapreduce"] as $key=>$val) {
    $result["overall"][$key] = $val;
  }
  foreach ($result["hbase"] as $key=>$val) {
    $result["overall"][$key] = $val;
  }

  return $result;
}

/**
 * Should return the cluster summary required on main page
 */
function hdp_mon_jmx_get_cluster_info() {
  // hdfs info
  $hdfs_info = array();
  if (array_key_exists("HDFS", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $info = hdp_mon_jmx_get_hdfs_info(FALSE);
    $hdfs_info = $info["hdfs"];
  }

  // mr info
  $mr_info = array();
  if (array_key_exists("MAPREDUCE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $info = hdp_mon_jmx_get_mapreduce_info(FALSE);
    $mr_info = $info["mapreduce"];
  }

  // hbase info
  $hbase_info = array();
  if (array_key_exists("HBASE", $GLOBALS["HDP_MON_CONFIG"]["SERVICES"])) {
    $info = hdp_mon_jmx_get_hbase_info(FALSE);
    $hbase_info = $info["hbase"];
  }

  $result = hdp_mon_helper_get_cluster_info($hdfs_info, $mr_info, $hbase_info);

  // Placeholder until UI handles sections of data
  // After change return $result as is
  return json_encode($result);
}

?>
