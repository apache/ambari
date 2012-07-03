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

include_once 'commandUtils.php';
include_once '../util/clusterState.php';
include_once "../util/HMCTxnUtils.php";

$logger = new HMCLogger("setupNodes");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];
$action = $_GET['action'];
$deployUser = $_POST['ClusterDeployUser'];

$propertiesArr = $dbAccessor->getConfigPropertiesMetaInfo();
if ($propertiesArr["result"] != 0) {
  $logger->log_error("Could not get config meta info from DB, error="
      . $propertiesArr["error"]);
  print json_encode(array( "result" => 1, "error" => "Error in config properties meta info"));
  return;
}

// Use meta info defaults
// Override with current svc configs
// Override with POST params

$useLocalYumRepo = $propertiesArr["configs"]["using_local_repo"]["value"];
$repoFilePath = $propertiesArr["configs"]["yum_repo_file"]["value"];

$currentConfigs = $dbAccessor->getServiceConfig($clusterName);
if ($currentConfigs["result"] != 0) {
  $logger->log_error("Could not get configs from DB, error="
      . $currentConfigs["error"]);
  print json_encode(array( "result" => 1, "error" => "Could not get configs from DB"));
  return;
}

if (isset($currentConfigs["properties"]["using_local_repo"]) && 
    $currentConfigs["properties"]["using_local_repo"] != "") {
  $useLocalYumRepo = $currentConfigs["properties"]["using_local_repo"];
}

if (isset($currentConfigs["properties"]["yum_repo_file"])
    && $currentConfigs["properties"]["yum_repo_file"] != "") {
  $repoFilePath = $currentConfigs["properties"]["yum_repo_file"];
}

if (isset($_POST['useLocalYumRepo']) && (trim($_POST['useLocalYumRepo']) != "")) {
  $useLocalYumRepo = trim($_POST['useLocalYumRepo']);
}

if (isset($_POST['yumRepoFilePath'])
    && trim($_POST['yumRepoFilePath']) != "") {
  $repoFilePath = trim($_POST['yumRepoFilePath']);
}

header("Content-type: application/json");

if (!file_exists($repoFilePath)) {
  $logger->log_warn("Invalid repo file provided, file does not exist"
      . ", repoFile=" . $repoFilePath);
  print (json_encode(array(
      "result" => 1,
      "error" => "Invalid repo file path specified"
  )
  ));
  return;
}

// TODO - error checks for download urls
/*
if (parse_url($hdpArtifactsDownloadUrl) === FALSE
    || parse_url($gplArtifactsDownloadUrl) === FALSE) {
  $logger->log_warn("Invalid download urls provided, could not parse"
      . ", hdpArtifactsDownloadUrl=" . $hdpArtifactsDownloadUrl
      . ", gplArtifactsDownloadUrl=" . $gplArtifactsDownloadUrl);

  print (json_encode(array(
        "result" => 1,
        "error" => "Invalid download urls specified")));
  return;
}
*/

$configs =  array ( "using_local_repo" => $useLocalYumRepo,
                    "yum_repo_file" => $repoFilePath);
$dbResponse = $dbAccessor->updateServiceConfigs($clusterName, $configs);
if ($dbResponse["result"] != 0) {
  $logger->log_error("Got error while persisting configs: ".$dbResponse["error"]);
  return $dbResponse;
}

$stagesFiles = "";
if ($action == "addNodes") {
  $stagesFile = "./addNodes/stages.php";
} else if ($action == "uninstall") {
  $stagesFile = "./uninstall/stages.php";
} else {
  print (json_encode(array(
    "result" => 1,
    "error" => "Invalid action",
  )
  ));
  return;
}

error_log("ClusterName: ".$clusterName);

$logger->log_info("ClusterName: $clusterName\n");
$logger->log_info("Deploy User: $deployUser\n");

// this api just creates a new transaction id for the db
$status = "";
$createTxResult = HMCTxnUtils::createNewTransaction($dbAccessor, $clusterName, $status);
if ($createTxResult == FALSE) {
  $msg = "SETUP: Failed to create new transaction in background: $createTxResult\n";
  $logger->log_error($msg);
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
}

$rootTxnId = $createTxResult;
$logger->log_info("Root Txn Id = $rootTxnId\n");

$cmd = $GLOBALS["PHP_EXEC_PATH"] . " " . "../util/sequentialScriptRunner.php";
// $cmd = $GLOBALS["PHP_EXEC_PATH"] . " " . "./addNodes/addNodesOrchestrator.php";

$hostsFile = getHostsFilePath($clusterName);

$args = "$clusterName $deployUser $rootTxnId $hostsFile $stagesFile";

$execBackgroundResult = HMCTxnUtils::execBackgroundProcess($dbAccessor, $clusterName, $rootTxnId, $cmd, $args, "");
if ($execBackgroundResult == FALSE) {
  $msg = "Failed to execute addNodesOrchestrator in background: $execBackgroundResult\n";
  $logger->log_error($msg);
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
}

$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";

$context = array (
  'stage' => "ADD_NODES",
  'txnId' => $rootTxnId
);

/* Only updateClusterState() if we're in the middle of configuring the cluster 
 * the first time through. 
 */
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);

if ($clusterState['state'] == 'CONFIGURATION_IN_PROGRESS') {
  // update state of the cluster to be configuration in progress
  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $result = $retval['result'];
    $error = $retval['error'];
  }
}

print (json_encode(array(
  "result" => $result,
  "error" => $error,
  "response" => array(
    "clusterName" => $clusterName,
    "txnId" => $rootTxnId,
  )
)
));

?>
