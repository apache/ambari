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
include_once './localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once '../util/clusterState.php';

header("Content-type: application/json");

$logger = new HMCLogger("CreateCluster");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$multipleClustersSupported = false;
$allClustersResult = $dbAccessor->getAllClusters();
$listOfClusters = array();
if ($allClustersResult["result"] != 0) {
  print(json_encode($allClustersResult));
  return;
}

// Read from the input
$requestdata = file_get_contents('php://input');
$requestObj = json_decode($requestdata, true);

$clusterName = trim($requestObj["clusterName"]);

if ($clusterName == "") {
  print json_encode(array("result" => 1, "error" => "Cluster name cannot be empty"));
  return;
}
if (preg_match("/[\s]+/", $clusterName) > 0) {
  print json_encode(array("result" => 1, "error" => "Cluster name cannot contain whitespaces"));
  return;
}

// do not allow ? ! * + - | " [ ] / ( ) { } ! @ # $ % ^ & * ( ) ' ` ~ , .
if (preg_match('/(\?|\+|\-|\||\"|\[|\]|\/|\{|\}|\!|\@|\#|\$|\%|\^|\&|\*|\(|\)|\'|\`|\~|\,|\.)/', $clusterName) > 0) {
  print json_encode(array("result" => 1, "error" => "Cluster name cannot contain special characters"));
  return;
}
// Validate clusterName: TODO; FIXME

/*
if (!array_key_exists($clusterName, $allClustersResult["clusters"])) {
  if (!$multipleClustersSupported && count($allClustersResult["clusters"]) != 0 ) {
    print (json_encode(array( "result" => 1, "error" => "Multiple clusters are not supported and you already have a cluster installed" )));
    return;
  }
}
*/

// create the lockfile in the clusterDir
$fileHdl = fopen($GLOBALS["HMC_CLUSTER_PATH"]."/lockfile", "w");
if ($fileHdl == false) {
  print json_encode(array( "result" => 1, "error" => 'Failed to create lock file...'));
  return;
}
fclose($fileHdl);

// if user re-enters this page, we need to delete and restart the cluster conf
$dbAccessor->wipeOutClusters($clusterName);
$logger->log_info("Completed deletion of cluster: ".$clusterName);

$hdpVersion="1.0"; // TODO: hardcoded
$state="Configuration in progress";

$response = $dbAccessor->createCluster($clusterName, $hdpVersion, $state);

// Return errors from response
if ($response["result"] != 0) {
  $logger->log_debug(print_r($response, true));
  print json_encode($response);
  return;
}

$clusterName = $response["clusterName"];


// Populate the ServiceComponentInfo table
$allServiceComps = array();
$allServicesArray = $dbAccessor->getAllServicesList();
if ($allServicesArray["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServicesArray["error"]);
  print json_encode($allServicesArray);
  return;
}

$allServicesInfo = $dbAccessor->getAllServicesInfo($clusterName);
if ($allServicesInfo["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServicesInfo["error"]);
  print json_encode($allServicesInfo);
  return;
}

foreach($allServicesArray["services"] as $service) {
  $thisService = array();
  $serviceName = $service["serviceName"];
  $componentsStaticTableDBResult = $dbAccessor->getAllServiceComponents($serviceName);
  if ($componentsStaticTableDBResult["result"] != 0 ) {
    $logger->log_error("Got error while getting all service components:".$componentsStaticTableDBResult["error"]);
    print json_encode($componentsStaticTableDBResult);
    return;
  }

  foreach($componentsStaticTableDBResult["components"] as $componentName => $component) {
    $componentArray  = array();
    $componentArray["state"] = "UNKNOWN";
    $componentArray["desiredState"] = "UNKNOWN";
    $thisService[$componentName] = $componentArray;
  }

  $allServiceComps[$serviceName] = $thisService;
}

$result = $dbAccessor->addServiceComponentsToCluster($clusterName, $allServiceComps);
if ($result["result"] != 0 ) {
  $logger->log_error("Got error while adding all service components:".$result["error"]);
  print json_encode($result);
  return;
}
// end of populating the ServiceComponentInfo table

$propertiesArr = $dbAccessor->getConfigPropertiesMetaInfo();
if ($propertiesArr["result"] != 0) {
  print json_encode(array( "result" => 1, "error" => "Error in config properties meta info"));
  return;
}

$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";
$context = array (
  'stage' => "CREATE_CLUSTER"
);

// update state of the cluster to be configuration in progress
$retval = updateClusterState($clusterName, $state, $displayName, $context);
if ($retval['result'] != 0) {
  $result = $retval['result'];
  $error = $retval['error'];
}

$output = array(
                 "result" => $result,
                 "error" => $error,
                 "response" => array(
                                 "clusterName" => $response["clusterName"]
                        )
              );

print (json_encode($output));
?>
