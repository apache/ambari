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
include_once "../util/util.php";
include_once '../db/HMCDBAccessor.php';
include_once "../orchestrator/HMC.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";
include_once "../util/clusterState.php";

$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];

/* For returning in our JSON at the very end. */
$result = 0;
$error = "";

$txnId = -1;
$deployUser = "";

$logger = new HMCLogger("Uninstall");

$dbAccessor = new HMCDBAccessor($dbPath);
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);

/* Run an actual uninstall only if this cluster is in a deployed state 
 * (regardless of whether the deploy was a success or failure). 
 */
if ($clusterState['state'] == 'DEPLOYED') {

  $logger->log_info("Uninstall invoked");

  $action = $_GET['action'];
  $deployUser = $_GET['clusterDeployUser'];

  $wipeout = FALSE;
  if ($action == "wipeOut") {
    $wipeout = TRUE;
  } else {
    $wipeout = FALSE;
  }

  ////// need to generate the hosts.txt file with all the good nodes in the cluster
  $allHostsInfo = $dbAccessor->getAllHostsInfo($clusterName, 
    array("=" => array ( "discoveryStatus" => "SUCCESS")));
  if ($allHostsInfo["result"] != 0 ) {
    $logger->log_error("Got error while getting hostsInfo ".$allHostsInfo["error"]);
    print json_encode($allHostsInfo);
    return;
  }

  $hostFileName = getHostsFilePath($clusterName);

  $hostFileHdl = fopen($hostFileName, "w");

  foreach ($allHostsInfo["hosts"] as $hostInfo) {
    fwrite($hostFileHdl, $hostInfo["hostName"]."\n");
  }

  fclose($hostFileHdl);
  ////// end of generating new file

  $logger->log_info("Uninstall got wipeout value $wipeout");

  // call the wipeout script and return the transaction id
  $hmc = new HMC($dbPath, $clusterName);

  $uninstallResult = $hmc->uninstallHDP($wipeout);
  if ($uninstallResult["result"] != 0) {
    print json_encode($uninstallResult);
    return;
  }

  if (!isset($uninstallResult["txnId"])) {
    print json_encode ( array("result" => 1, "error" => "Could not obtain txn info for triggered command"));

    return;
  }

  $txnId = $uninstallResult["txnId"];

  /* (And when we kick off the uninstall is the only time to update the state 
   * of the cluster). 
   */
  $state = "UNINSTALLATION_IN_PROGRESS";
  $displayName = "Uninstallation in progress";
  $context = array (
    'txnId' => $txnId,
    'deployUser' => $deployUser
  );

  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $result = $retval['result'];
    $error = $retval['error'];
  }
}
/* In case the uninstall is already running or has ended, just return the txnId 
 * and deployUser from the DB instead of kicking off a fresh uninstall - this 
 * is so we can use this entrypoint to show the cluster's uninstall progress at 
 * any time in the future, not just during a live uninstall. 
 */
elseif ($clusterState['state'] == 'UNINSTALLATION_IN_PROGRESS') {

  $txnId = $clusterState['context']['txnId'];
  $deployUser = $clusterState['context']['deployUser'];
}

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId,
    'deployUser' => $deployUser
  );

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));

?>
