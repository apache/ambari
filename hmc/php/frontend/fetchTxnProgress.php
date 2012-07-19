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
include_once "../conf/Config.inc";
include_once "../orchestrator/HMC.php";
include_once '../util/clusterState.php';
include_once "uninstallCleanup.php";
include_once "deployPostProcess.php";
include_once "uninstallCleanup.php";
include_once "restoreDeployedStatePostProcess.php";

$dbPath = $GLOBALS["DB_PATH"];

$clusterName = $_GET['clusterName'];
$txnId = $_GET['txnId'];
$deployUser = isset($_GET['deployUser']) ? $_GET['deployUser'] : 'root';

$logger = new HMCLogger("TxnProgress");

$map = array(
  "HMC::deployHDP" => array (
      "deployPostProcess"
  ),
  "HMC::deployNodes" => array (
      "restoreDeployedStatePostProcess"
  ),
  "HMC::uninstallHDP" => array (
      "deBootStrap"
  ),
  "HMC::startAllServices" => array (
      "restoreDeployedStatePostProcess"
  ),
  "HMC::stopAllServices" => array (
      "restoreDeployedStatePostProcess"
  ),
  "HMC::startServices" => array (
      "restoreDeployedStatePostProcess"
  ),
  "HMC::stopServices" => array (
      "restoreDeployedStatePostProcess"
  ),
  "HMC::reconfigureServices" => array (
      "restoreDeployedStatePostProcess"
  )
);

$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function fetchTxnProgress( $txnId )
{
  global $dbPath;
  global $clusterName;

  $hmc = new HMC($dbPath, $clusterName);

  $progress = $hmc->getProgress($txnId);

  return $progress;
}

function sortProgressStatesByRank( $first, $second )
{
  if( $first['rank'] == $second['rank'] )
  {
    return 0;
  }

  return ($first['rank'] < $second['rank']) ? -1 : 1;
}

$progress = fetchTxnProgress($txnId);

// TODO XXX Check for $progress['result'] and $progress['error'] here, before proceeding.

/* Tack on some additional state to make life on the frontend easier. */
$progress['encounteredError'] = false;

/* Marker to keep track of whether at least one subTxn has been kicked off. */
$atLeastOneSubTxnInProgress = false;

/* Sort the subTxns array inside $progress by rank, and then remove all notion
 * of rank from the sorted array we're going to return.
 */
usort( $progress['subTxns'], 'sortProgressStatesByRank' );

foreach( $progress['subTxns'] as &$progressSubTxn )
{
  unset( $progressSubTxn['rank'] );

  /* Any one subTxn failing means we want the frontend to bail. */
  if( $progressSubTxn['progress'] == 'FAILED' )
  {
    $progress['encounteredError'] = true;
  }
  /* We need to make sure at least one subTxn is not pending before
   * sending a progress report back to the frontend - if not, the
   * progress states aren't yet finalized and will change across
   * invocations to this webservice, so we prefer to wait before
   * showing anything.
   */
  if( $progressSubTxn['progress'] != 'PENDING' )
  {
    $atLeastOneSubTxnInProgress = true;
  }
}

$lastTxnIndex = -1;
if ($atLeastOneSubTxnInProgress) {
  $lastTxnIndex = count($progress['subTxns']) - 1;
}

/* If at least one subTxn isn't in progress, signal to the frontend that
 * there's nothing worthy for it to process yet.
 */
if (!$atLeastOneSubTxnInProgress) {
  $progress['subTxns'] = null;
}

LockAcquire(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);

$doPostProcess = TRUE;
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);
$logger->log_debug("Got cluster state: ".json_encode($clusterStateResponse));
if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX); return;
}

// if state is not set, should not proceed to post process.
// setting post process to false allows returning appropriate txn data
// without post processing.
$clusterState = null;
if (!isset($clusterStateResponse['state'])) {
  $doPostProcess = FALSE;
  // create an empty cluster state
  $clusterState = array();
} else {
  $clusterState = json_decode($clusterStateResponse['state'], true);
  $logger->log_debug("Current cluster state, " . print_r($clusterState, true));
}

/* check for matching txn id. if present check if it is same
 * if not return with all data for txn id requested for and 
 * do nothing for post process
 * setting post process to false allows returning appropriate txn data
 * without post processing.
 */ 

if (array_key_exists('context', $clusterState)) {  
  $clusterContext = $clusterState['context'];
  if (!array_key_exists('txnId', $clusterContext) || !isset($clusterContext['txnId']) || ($clusterContext["txnId"] != $txnId)) {
    $logger->log_debug("TxnId does not exist ".
      array_key_exists("txnId", $clusterContext) .
      " or not set " .!isset($clusterContext["txnId"]) ." or does not match " .
      ($clusterContext["txnId"] != $txnId));
    $doPostProcess = FALSE;
  }
} 

if ($progress['processRunning'] == FALSE) {
  $logger->log_trace("Checking cluster state for post process state");
  $context = $clusterState['context'];
  if (isset($context['isInPostProcess'])) {
    $doPostProcess = FALSE;
    $logger->log_trace("Post process already done before in another call");
    if ($context['isInPostProcess'] == TRUE) {
      $logger->log_trace("Post process still in progress in another call");      
      $progress['processRunning'] = TRUE;
    } else {
      $logger->log_trace("Post process completed in another call");
      $progress['processRunning'] = FALSE;
      if (!isset($context['postProcessSuccessful'])
          || $context['postProcessSuccessful'] == FALSE) {
        $progress['encounteredError'] = TRUE;
        if ($lastTxnIndex >= 0) {
          $progress['subTxns'][$lastTxnIndex]["progress"] = "FAILED";
        }
      }
    }
  }
}


if ((($progress['processRunning'] == FALSE) || ($progress['encounteredError'] == TRUE))
    && $doPostProcess) {
  // get the transaction status info from db
  $retval = $dbAccessor->getTransactionStatusInfo($clusterName, $txnId);
  if ($retval["result"] != 0) {
    $progress['encounteredError'] = TRUE;
    if ($lastTxnIndex >= 0) {
      $progress['subTxns'][$lastTxnIndex]["progress"] = "FAILED";
    }
  } else {
    if (isset($retval['statusInfo'])) {
      $statusInfo = json_decode($retval['statusInfo'], true);
      $logger->log_debug("Status info function ".$statusInfo['function']);
      $logger->log_debug("Running post process functions");
      // run the next script from the map
      // supports multiple post process functions
      foreach ($map[$statusInfo['function']] as $postProcessFunc) {
        $logger->log_debug("Post process function is ".$postProcessFunc);
        // setting cluster state to denote in post process
        $clusterState['context']['isInPostProcess'] = TRUE;
        $logger->log_trace("Starting post process function");      
        updateClusterState($clusterName, $clusterState['state'],
          $clusterState['displayName'], $clusterState['context']);
        LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
        $retval = $postProcessFunc($clusterName, $deployUser, $txnId, $progress);
        LockAcquire(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);
        // setting cluster state to denote post process completed
        $logger->log_trace("Finished post process function");

        $clusterStateResponse = $dbAccessor->getClusterState($clusterName);
        if ($clusterStateResponse['result'] != 0) {
          print json_encode($clusterStateResponse);
          LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX); return;
        }

        $clusterState = json_decode($clusterStateResponse['state'], true);
        $clusterState['context']['isInPostProcess'] = FALSE;
        $clusterState['context']['postProcessSuccessful'] = ($retval["result"] == 0);


        updateClusterState($clusterName, $clusterState['state'],
          $clusterState['displayName'], $clusterState['context']);
        $clusterStateResponse = $dbAccessor->getClusterState($clusterName);
        $logger->log_trace("STATE AFTER UPDATE: ".json_encode($clusterStateResponse));
        if ($retval["result"] != 0) {
          $progress['encounteredError'] = TRUE;
          if ($lastTxnIndex >= 0) {
            $progress['subTxns'][$lastTxnIndex]["progress"] = "FAILED";
          }
          // if the post process failed stop from calling 
          // further post process functions.
          break;
        }
      }
    }
  }
}

LockRelease(HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX);

/* Clean up some more remnants that we don't need on the frontend. */
unset( $progress['result'] );
unset( $progress['error'] );

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId,
    'progress' => $progress );

if ($deployUser != null) {
  $jsonOutput['deployUser'] = $deployUser;
}

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode($jsonOutput));

?>
