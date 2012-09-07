<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


function validateDirList($dirListStr) {
  $dirList = explode(",", trim($dirListStr));
  if (count($dirList) == 0) {
    return array ( "error" => "No directories specified");
  }
  $validDirCount = 0;
  foreach ($dirList as $d) {
    $dir = trim($d);
    if ($dir == "") {
      continue;
    }
    $check = validatePath($dir);
    if ($check["error"] != "") {
      return $check;
    }
    $validDirCount++;
  }
  if ($validDirCount == 0) {
    return array ( "error" => "No directories specified");
  }
  return array ( "error" => "");
}

function validatePath($pathStr) {
  $path = trim($pathStr);
  if ($path == "") {
    return array ( "error" => "No path specified");
  }
  if (substr($path, 0, 1) != "/") {
    return array ( "error" => "Directory path not absolute");
  }
  return array ( "error" => "");
}

function basicNumericCheck($val, $negativeAllowed = TRUE) {
  if (!is_numeric($val)) {
    return array ( "error" => "Invalid value specified, should be numeric");
  }
  if (!$negativeAllowed) {
    if ($val < 0) {
      return array ( "error" => "Invalid value specified, "
          . "negative values not allowed");
    }
  }
  return array ( "error" => "");
}

/**
 * @param mixed $configs
 *    array (
 *           "prop_key1" => "val1",
 *           ...
 *        )
 */
function validateConfigs($svcConfigs) {
  $REQUIRED_FIELD_MESSAGE = 'This is required.  Please specify.';
  $errors = array();
//  foreach ($configs as $svc => $svcConfigs) {
    foreach ($svcConfigs as $key => $val) {
      $val = trim($val);
      if ($key == "dfs_name_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "dfs_data_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "fs_checkpoint_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_local_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "oozie_data_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "zk_data_dir") {
        $check = validateDirList($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hive_mysql_host") {
        // TODO ??
      } else if ($key == "hive_database_name") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        } else if (preg_match("/^\w+$/", $val) == 0) {
          $errors[$key] = array ( "error" => "Database name should only contain alphanumeric characters");
        }
      } else if ($key == "hive_metastore_user_name") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        } else if (preg_match("/^\w+$/", $val) == 0) {
          $errors[$key] = array ( "error" => "Database user name should only contain alphanumeric characters");
        }
      } else if ($key == "hive_metastore_user_passwd") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "java32_home") {
        if ($val != "") {
          $check = validatePath($val);
          if ($check["error"] != "") {
            $errors[$key] = $check;
          }
        }
      } else if ($key == "java64_home") {
        if ($val != "") {
          $check = validatePath($val);
          if ($check["error"] != "") {
            $errors[$key] = $check;
          }
        }
      } else if ($key == "jdk_location") {
        if ($val != "") {
          //if (filter_var($val, FILTER_VALIDATE_URL) === FALSE) {
          //  $errors[$key] = array ( "error" => "Invalid url specified");
          //}
        }
      } else if ($key == "hdfs_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "mapred_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "dfs_support_append") {
        // TODO
      } else if ($key == "dfs_webhdfs_enabled") {
        // TODO
      } else if ($key == "hadoop_logdirprefix") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hadoop_piddirprefix") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "scheduler_name") {
        // TODO check for valid scheduler names?
      } else if ($key == "hbase_log_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hbase_pid_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hbase_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "zk_log_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "zk_pid_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "zk_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "hcat_logdirprefix") {
      } else if ($key == "hcat_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "templeton_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "templeton_pid_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "templeton_log_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "oozie_log_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "oozie_pid_dir") {
        $check = validatePath($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "oozie_user") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "nagios_web_login") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "nagios_web_password") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        }
      } else if ($key == "nagios_contact") {
        if ($val == "") {
          $errors[$key] = array ( "error" => $REQUIRED_FIELD_MESSAGE);
        } else if (0 == preg_match("/^(\w+((-\w+)|(\.\w+)|(_\w+))*)\@(\w+((\.|-)\w+)*\.\w+$)/",$val)) {
          $errors[$key] = array ( "error" => "Not a valid email address");
        }
      } else if ($key == "hadoop_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "namenode_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "namenode_opt_newsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "datanode_du_reserved") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "dtnode_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "jtnode_opt_newsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "jtnode_opt_maxnewsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "jtnode_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_map_tasks_max") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_red_tasks_max") {
              $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_cluster_map_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_cluster_red_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_cluster_max_map_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_cluster_max_red_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_job_map_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_job_red_mem_mb") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "mapred_child_java_opts_sz") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "io_sort_mb") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "io_sort_spill_percent") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        } else if ($val > 1) {
          $errors[$key] = array ( "error" => "value cannot be greater than 1");
        }
      } else if ($key == "mapreduce_userlog_retainhours") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "maxtasks_per_job") {
        $check = basicNumericCheck($val);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "dfs_datanode_failed_volume_tolerated") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "tickTime") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "initLimit") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "syncLimit") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "clientPort") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hbase_master_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "hbase_regionserver_heapsize") {
        $check = basicNumericCheck($val, FALSE);
        if ($check["error"] != "") {
          $errors[$key] = $check;
        }
      } else if ($key == "lzo_enabled") {
        if ($val != "true" && $val != "false") {
          $errors[$key] = array ( "error" => "Invalid value. Only true/false allowed");
        }
      } else if ($key == "snappy_enabled") {
        if ($val != "true" && $val != "false") {
          $errors[$key] = array ( "error" => "Invalid value. Only true/false allowed");
        }
      }

      /*
      hive_mysql_host|
      hive_database_name|
      hive_metastore_user_name|
      hive_metastore_user_passwd|
      java32_home|
      java64_home|
      jdk_location|
      dfs_support_append|true
      dfs_webhdfs_enabled|false
      scheduler_name|org.apache.hadoop.mapred.CapacityTaskScheduler
      nagios_web_login|nagiosadmin
      nagios_web_password|admin
      hadoop_heapsize|1024
      namenode_heapsize|1024
      namenode_opt_newsize|200
      datanode_du_reserved|1073741824
      dtnode_heapsize|1024
      jtnode_opt_newsize|200
      jtnode_opt_maxnewsize|200
      jtnode_heapsize|1024
      mapred_map_tasks_max|4
      mapred_red_tasks_max|2
      mapred_cluster_map_mem_mb|-1
      mapred_cluster_red_mem_mb|-1
      mapred_cluster_max_map_mem_mb|-1
      mapred_cluster_max_red_mem_mb|-1
      mapred_job_map_mem_mb|-1
      mapred_job_red_mem_mb|-1
      mapred_child_java_opts_sz|768
      io_sort_mb|200
      io_sort_spill_percent|0.9
      mapreduce_userlog_retainhours|24
      maxtasks_per_job|-1
      dfs_datanode_failed_volume_tolerated|0
      tickTime|2000
      initLimit|10
      syncLimit|5
      clientPort|2181
      hbase_master_heapsize|1024
      hbase_regionserver_heapsize|1024
      */

    }
//  }
  $result = 0;
  if (!empty($errors)) {
    $result = -1;
  }
  return array ( "result" => $result, "properties" => $errors );
}

////////////Helper function definitions
function handleHiveMysql($clusterName, $dbAccessor, $logHandle) {
  $services = $dbAccessor->getAllServicesInfo($clusterName);
  $configs = $dbAccessor->getServiceConfig($clusterName);
  $hostForMysql = $dbAccessor->getHostsForComponent($clusterName, "HIVE_MYSQL");
  if ( ($services["services"]["HIVE"]["isEnabled"] == 1) &&
       ( isset($configs["properties"])
         && ( !isset($configs["properties"]["hive_mysql_host"])
              || empty($configs["properties"]["hive_mysql_host"])) ) &&
       (empty($hostForMysql["hosts"])) ) {
    $logHandle->log_debug("Hive is enabled but mysql server is not set, set it up on hive server itself");
    $hostComponents = $dbAccessor->getHostsForComponent($clusterName, "HIVE_SERVER");
    $hiveServerHosts = array_keys($hostComponents["hosts"]);
    $newConfig = array ( "hive_mysql_host" => "localhost" );  
    $dbAccessor->updateServiceConfigs($clusterName, $newConfig);
    $dbAccessor->addHostsToComponent($clusterName, "HIVE_MYSQL",
          $hiveServerHosts, "ASSIGNED", "");
  } else {
    if (isset($configs["properties"])
        && isset($configs["properties"]["hive_mysql_host"])
        && $configs["properties"]["hive_mysql_host"] != "localhost") {
      $dbAccessor->removeAllHostsFromComponent($clusterName, "HIVE_MYSQL");
    }
  }
}

/*
 * Trim each of the entries in a comma-separated list
 */
function trimDirList($dirListStr) {
  $dirList = explode(",", $dirListStr);
  if (count($dirList) == 0) {
    return "";
  }
  $sanitizedDirs = array();
  foreach ($dirList as $dir) {
    $d = trim($dir);
    if ($d != "") {
      array_push($sanitizedDirs, $d);
    }
  }
  return implode(",", $sanitizedDirs);
}

/**
 * Basic trimming, conversion into required array format to update DB
 * @param mixed $requestObjFromUser
 */
function sanitizeConfigs($requestObjFromUser, $logger) {
  $finalProperties = array();
  foreach ($requestObjFromUser as $serviceName=>$objects) {
    $allProps = $objects["properties"];
    foreach ($allProps as $key => $valueObj) {
      $val = trim($valueObj["value"]);
      $finalProperties[$key] = $val;
      if ($key == "dfs_name_dir"
          || $key == "dfs_data_dir"
          || $key == "mapred_local_dir"
          || $key == "oozie_data_dir"
          || $key == "zk_data_dir"
          || $key == "fs_checkpoint_dir" ) {
        $finalProperties[$key] = trimDirList($val);
      } else if ($key == "lzo_enabled"
                 || $key == "snappy_enabled") {
        $finalProperties[$key] = strtolower($val);
      }
    }
  }
  return $finalProperties;
}

/**
 * @param $dbAccessor HMCDBAccessor
 * @param $logger HMCLogger
 * @param $clusterName
 * @param $finalProperties
 * @return array|mixed
 */
function validateConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties) {

  // Validate/verify configs
  $cfgResult = validateConfigs($finalProperties);
  $suggestProperties = new SuggestProperties();
  $cfgSuggestResult = $suggestProperties->verifyProperties($clusterName, $dbAccessor, $finalProperties);
  if ($cfgResult["result"] != 0 || $cfgSuggestResult["result"] != 0) {
    $mergedErrors = array( "result" => 1, "error" => "Some configuration parameters need your attention before you can proceed.",
      "properties" => array());

    if (isset($cfgResult["properties"])) {
      $mergedErrors["properties"] = $cfgResult["properties"];
    }

    if (isset($cfgSuggestResult["cfgErrors"])) {
      foreach ($cfgSuggestResult["cfgErrors"] as $propKey => $errInfo) {
        $mergedErrors["properties"][$propKey] = $errInfo;
      }
    }
    /* TODO - need to handle values with warnings - do we want to tell users they are using above recommended settings? */

    $logger->log_error("Got error when validating configs");
    return $mergedErrors;
  }
  return $cfgSuggestResult;
}

/**
 * @param $dbAccessor HMCDBAccessor
 * @param $logger HMCLogger
 * @param $clusterName
 * @param $finalProperties
 * @return array|mixed
 */
function validateAndPersistConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties) {

  // sanitize and persist the user entered configs *******

  $result = validateConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties);
  if ($result["result"] != 0) {
    return $result;
  }

  $dbResponse = $dbAccessor->updateServiceConfigs($clusterName, $finalProperties);
  if ($dbResponse["result"] != 0) {
    $logger->log_error("Got error while persisting configs: ".$dbResponse["error"]);
    return $dbResponse;
  }

  //Additional services that need to be enabled based on configuration.
  //Hack to handle mysql for hive
  handleHiveMysql($clusterName, $dbAccessor, $logger);

  // finished persisting the configs *******
  return array("result" => 0, "error" => 0);
}

?>
