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
include_once '../util/clusterState.php';
include_once '../db/HMCDBAccessor.php';
include_once "./uninstall/stages.php";

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

$logger = new HMCLogger("uninstallCleanup");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

// this function will launch the stages in the background 
// and poll for completion.
// upon completion, does the nodesActionProgress piece to update
// status of the completed state
function deBootStrap ($clusterName, $deployUser, $txnId, $progressInfo)
{
  global $logger, $dbAccessor, $stagesInfo;

  $txnStatus = !($progressInfo['encounteredError']);

  // do not go ahead with the cleanup. 
  // Update the state of the cluster.
  if (!$txnStatus) {
    $state = "UNINSTALLED";
    $displayName = "Uninstall failed";
    $context = array (
      'status' => $txnStatus,
      'txnId' => $txnId
    );
    LockAcquire(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
    $retval = updateClusterState($clusterName, $state, $displayName, $context);
    LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
    return $retval;
  }

  /////// launch the stages in the background.
  $stagesFile = "./uninstall/stages.php";
  $logger->log_info("ClusterName: $clusterName\n");
  $logger->log_info("Deploy User: $deployUser\n");
  $status = "";         
  $createTxResult = HMCTxnUtils::createNewTransaction($dbAccessor, $clusterName, $status);
  if ($createTxResult == FALSE) {
    $msg = "SETUP: Failed to create new transaction in background: $createTxResult\n";
    $logger->log_error($msg);
    return array("result" => 1, "error" => "$msg");
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
      return array("result" => 1, "error" => "$msg");
  } 

  /////// done launching stages

  /////// now monitor the stages

  $allDone = false;
  while ($allDone == false) {
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
      return (array('result' => 1, 'error' => $orchestratorTxnId['error']));
    }
    // as soon as DB is read, sleep for 2 seconds because we want to 
    // avoid continuous polling of the db
    sleep(2);

    //$logger->log_error(" ==== subTxns info is  " . json_encode($orchestratorTxnId));
    $keys = array_keys($orchestratorTxnId['subTxns']);
    if (count($keys) == 0) {
      $logger->log_debug("No keys found in orchestrator's child transactions");
      continue;
    }
    $firstKey = $keys[0];
    if ($firstKey == '') {
      continue;
    }

    $allSubTxns = $dbHandle->getAllSubTransactionsInfo(
      $clusterName, $firstKey);
    // the all sub txn ids could potentially return an error because
    // there is a potential race condition before the orchestrator's subTxnId has
    // been committed to the db by the backgrounded process.

    if ($allSubTxns['result'] != 0) {
      $logger->log_error("Empty orchestrator txn id " .
        json_encode($allSubTxns));
      return ($jsonOutput);
    }

    $logger->log_debug("allsubtxn count is ".json_encode(count($allSubTxns['subTxns'])));
    $logger->log_debug("stages count is ".json_encode(count($stagesInfo)));
    $logger->log_debug("subtxns ".json_encode($allSubTxns));
    // allDone must be set once the processes launched have completed running
    if ((count($allSubTxns['subTxns']) == count($stagesInfo))) {
      $lastTxn = end($allSubTxns['subTxns']);
      if (($lastTxn['opStatus'] == "SUCCESS")) {
        $allDone = true;
        $result = 0;
        $error = "";
      } else if (($lastTxn['opStatus'] == "FAILED") || 
          ($lastTxn['opStatus'] == "TOTALFAILURE")) {
        $logger->log_debug("");
        $allDone = true;
        $error = json_encode($lastTxn['state']);
        $result = 1;
      }
    }
  }
  /////// done monitoring return back to the uninstall
  
  if ($result != 0) {
    return array('result' => $result, 'error' => $error);
  }
  // need to cleanup db for this cluster
  $dbAccessor->cleanupCluster($clusterName);

  LockAcquire(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
  $clusterStateResponse = $dbAccessor->getClusterState($clusterName);
  if ($clusterStateResponse['result'] != 0) {
    LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX); return $clusterStateResponse;
  }

  $clusterState = json_decode($clusterStateResponse['state'], true);

  // set cluster state to not configured
  $state = "NOT_CONFIGURED";
  $displayName = "Uninstall succeeded";
  $retval = updateClusterState($clusterName, $state, $displayName, $clusterState['context']);
  LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
  return $retval;
}

?>
