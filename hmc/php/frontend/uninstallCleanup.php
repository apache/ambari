<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
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
function deBootStrap ($clusterName, $deployUser, $stageInfo)
{
  global $logger, $dbAccessor;

  /////// launch the stages in the background.
  $stagesFile = "./uninstall/stages.php";
  $logger->log_debug("ClusterName: $clusterName\n");
  $logger->log_debug("Deploy User: $deployUser\n");
  $status = "";         
  $createTxResult = HMCTxnUtils::createNewTransaction($dbAccessor, $clusterName, $status);
  if ($createTxResult == FALSE) {
    $msg = "SETUP: Failed to create new transaction in background: $createTxResult\n";
    $logger->log_error($msg);
    return array("result" => 1, "error" => "$msg");
  }

  $rootTxnId = $createTxResult;
  $logger->log_error("Txn Id ===== $rootTxnId\n");
  $logger->log_error("Clustername ===== $clusterName");
    
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
    sleep(2);
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
      return ($jsonOutput);
    }

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

    $jsonOutput['currentProgressStateIndex'] = count($allSubTxns['subTxns']) - 1;
    if ($jsonOutput['currentProgressStateIndex'] < 0) {
      $jsonOutput['currentProgressStateIndex'] = 0;
    }
    $jsonOutput['stateInfo'] = $allSubTxns;
    // Decode log information store as part of state.
    foreach ($jsonOutput['stateInfo']['subTxns'] as $subTxnId => $subTxnInfo) {
      $jsonOutput['stateInfo']['subTxns'][$subTxnId]['state'] = json_decode($jsonOutput['stateInfo']['subTxns'][$subTxnId]['state']);
    }

    $allDone = true;
  }
  /////// done monitoring return back to the uninstall
  
  // need to cleanup db for this cluster
  $dbAccessor->deleteCluster($clusterName);
  
  return array('result' => 0);
}

?>
