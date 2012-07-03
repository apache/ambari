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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once '../util/selectNodes.php';
include_once '../util/clusterState.php';

$logger = new HMCLogger("AssignHosts");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$selectNodes = new SelectNodes();

header("Content-type: application/json");

$clusterName = $_GET['clusterName'];
// TODO: Validate clusterName

$requestData = file_get_contents('php://input');
$componentsToHosts = json_decode($requestData, true);

// needs to persist info that is passed in
$serviceMasters = array();

// Get the info for all services, mainly for isEnabled
$allServicesInfos = $dbAccessor->getAllServicesInfo($clusterName);
if ($allServicesInfos["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServiceInfos["error"]);
  print json_encode($allServicesInfos);
  return;
}

// Get the info of all components. TODO: No getServiceComponentInfo() yet, meh..
$allServicesComponents = $dbAccessor->getAllServiceComponentsList();
if ($allServicesComponents["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServicesComponents["error"]);
  print json_encode($allServicesComponents);
  return;
}

$suggestedNodes = $selectNodes->updateDBWithRoles($clusterName, 
                                                  $dbAccessor,
                                                  $componentsToHosts
                                                );

$logger->log_trace("Debug Trace". json_encode($allServicesComponents["services"]));

/* hack to unlock front end if the updateDBWithRoles
$allHosts = array();
// For each possible service
foreach ($allServicesComponents["services"] as $serviceName => $serviceInfo) {
  // If the service is enabled
  $logger->log_error("service enabled thingie " . $allServicesInfos["services"][$serviceName]["isEnabled"]);
  if ($allServicesInfos["services"][$serviceName]["isEnabled"]) {
    // For each component in the enabled service
    foreach ($serviceInfo["components"] as $componentName => $componentInfo) {
      $logger->log_error("Looking if component exists " . $componentName);
      if (array_key_exists($componentName, $componentsToHosts)) {
        // If the component came from the user's host assignment, save the role in the DB.
        $addHostToComponentResult = $dbAccessor->addHostsToComponent($clusterName, $componentName, array($componentsToHosts[$componentName]), "May the force be with you", "carrot");
        array_push($allHosts, $componentsToHosts[$componentName]);
        if ($addHostToComponentResult["result"] != 0 ) {
          $logger->log_error("Got error while adding host to component".$addHostToComponentResult["error"]);
          print json_encode($addHostToComponentResult);
          return;
        }
      } else {
        $logger->log_error("doesn't exists " . $componentName);
      }
    }
  }
}
$dbAccessor->addHostsToComponent($clusterName, "DATANODE",
  $allHosts,
  "May the force be with you",
  "carrot");
$dbAccessor->addHostsToComponent($clusterName, "TASKTRACKER",
  $allHosts,
  "May the force be with you",
  "carrot");
  hack till here */

// choose the name node as the canary host.
$nameNodeHost = $componentsToHosts["NAMENODE"];
$AllMountPoints = array();
$nameNodeInfoResult = $dbAccessor->getHostInfo($clusterName, $nameNodeHost[0]);
if ($nameNodeInfoResult["result"] != 0 ) {
  $logger->log_error("Got error while getting canary host info ".$nameNodeInfoResult["error"]);
  print json_encode($nameNodeInfoResult);
  return;
}
$logger->log_info("All mount points: ".$nameNodeInfoResult["disksInfo"]);
$AllMountPoints = json_decode($nameNodeInfoResult["disksInfo"], true);

// generate the mount points info required by javascript in the next phase
$propertiesArr = $dbAccessor->getConfigPropertiesMetaInfo();
if ($propertiesArr["result"] != 0) {
  print("Error in config properties meta info");
  return;
}

// TODO: Get the displayNames and key names from DB.
$outjson = array(
            "clusterName" => $clusterName,
            "mountPoints" => $AllMountPoints,
            "servicesInfo" => array (
                                "HDFS" => array(
                                             "dfs_name_dir" => array(
                                                                    "displayName" => "NameNode directories",
                                                                    "maxDirectoriesNeeded" => -1,
                                                                    "suffix" => "hadoop/hdfs/namenode"
                                                                    ),
                                             "dfs_data_dir" => array(
                                                                    "displayName" => "DataNode directories",
                                                                    "maxDirectoriesNeeded" => -1,
                                                                    "suffix" => "hadoop/hdfs/data"
                                                                    ),
                                             "fs_checkpoint_dir" => array(
                                                                    "displayName" => "SecondaryNameNode Checkpoint directory",
                                                                    "maxDirectoriesNeeded" => 1,
                                                                    "suffix" => "hadoop/hdfs/namesecondary"
                                                                    )
                                            ),
                                "MAPREDUCE" => array(
                                             "mapred_local_dir" => array(
                                                                    "displayName" => "MapReduce Data Directories",
                                                                    "maxDirectoriesNeeded" => -1,
                                                                    "suffix" => "hadoop/mapred"
                                                                    ),
                                            ),
                                "OOZIE" => array(
                                             "oozie_data_dir" => array(
                                                                    "displayName" => "Oozie DB Directory",
                                                                    "maxDirectoriesNeeded" => 1,
                                                                    "suffix" => "hadoop/oozie"
                                                                   ),
                                            ),
                                "ZOOKEEPER" => array(
                                             "zk_data_dir" => array(
                                                                    "displayName" => "ZooKeeper Data Directory",
                                                                    "maxDirectoriesNeeded" => 1,
                                                                    "suffix" => "hadoop/zookeeper"
                                                                  ),
                                           ),
                                ),
            );

// Update the state of the cluster.
$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";
$context = array (
  'stage' => "ASSIGN_MASTERS"
);

$retval = updateClusterState($clusterName, $state, $displayName, $context);
if ($retval['result'] != 0) {
  $result = $retval['result'];
  $error = $retval['error'];
}

print(json_encode(array( "result" => $result, "error" => $error, "response" => $outjson)));

?>
