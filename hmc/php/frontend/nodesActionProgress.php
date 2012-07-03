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

$logger = new HMCLogger("Add nodes poller");

$clusterName = $_GET['clusterName'];
$action = $_GET['action'];
$rootTxnId = $_GET['txnId'];

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

include_once $stagesFile;

header("Content-type: application/json");

$logger->log_info("Cluster Name: $clusterName Root Txn ID: $rootTxnId");

$progressStates = array();
foreach ($stagesInfo as $stage => $stageInfo) {
  array_push($progressStates, $stageInfo["description"]);
}

$jsonOutput = array (
  'progressStates'=> $progressStates,
  'currentProgressStateIndex' => 0, // by default is state Sshable
  'encounteredError' => false,
  'stateInfo' => array(),
  );

// use the txn id for finding the status to pass back to the user
// the orchestrator txn id could potentially return an error because
// there is a potential race condition before the orchestrator txnId has
// been committed to the db by the backgrounded process.
$dbHandle = new HMCDBAccessor($GLOBALS["DB_PATH"]);
$orchestratorTxnId = $dbHandle->getAllSubTransactionsInfo(
                      $clusterName, $rootTxnId);
if ($orchestratorTxnId['result'] != 0) { // encountered error
  $logger->log_error("Empty orchestrator txn id " .
      json_encode($orchestratorTxnId));
  // Check if this should return error FIXME
  // $jsonOutput['encounteredError'] = true;
  print json_encode($jsonOutput);
  return;
}

//$logger->log_error(" ==== subTxns info is  " . json_encode($orchestratorTxnId));
$keys = array_keys($orchestratorTxnId['subTxns']);
if (count($keys) == 0) {
  print json_encode($jsonOutput);
  $logger->log_debug("No keys found in orchestrator's child transactions");
  return;
}
$firstKey = $keys[0];
if ($firstKey == '') {
  print json_encode($jsonOutput);
  return;
}

$allSubTxns = $dbHandle->getAllSubTransactionsInfo(
                $clusterName, $firstKey);
// the all sub txn ids could potentially return an error because
// there is a potential race condition before the orchestrator's subTxnId has
// been committed to the db by the backgrounded process.

if ($allSubTxns['result'] != 0) {
  $logger->log_error("Empty orchestrator txn id " .
      json_encode($allSubTxns));
  print json_encode($jsonOutput);
  exit(0);
}

$jsonOutput['currentProgressStateIndex'] = count($allSubTxns['subTxns']) - 1;
if ($jsonOutput['currentProgressStateIndex'] < 0) {
  $jsonOutput['currentProgressStateIndex'] = 0;
}
$jsonOutput['stateInfo'] = $allSubTxns;
// Decode log information store as part of state.
foreach ($jsonOutput['stateInfo']['subTxns'] as $subTxnId => $subTxnInfo) {
  $jsonOutput['stateInfo']['subTxns'][$subTxnId]['state'] = json_decode($jsonOutput['stateInfo']['subTxns'][$subTxnId]['state'], true);
}

print json_encode($jsonOutput);

?>
