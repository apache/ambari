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

include_once "commandUtils.php";
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

$logger = new HMCLogger("deployAddedNodes");

$dbAccessor = new HMCDBAccessor($dbPath);
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);

/* Perform the actual addition of nodes only if this cluster is in a 
 * deployed state (regardless of whether the deploy was a success or failure). 
 */
if ($clusterState['state'] == 'DEPLOYED') {

  $readFromFile = getHostsFilePath($clusterName);

  $requestData = file_get_contents('php://input');
  $components = json_decode($requestData, true);

  $logger->log_debug("Components are ".json_encode($components));

  $hmc = new HMC($dbPath, $clusterName);

  // Get info about all nodes from hosts file 
  // the host file has been pruned by the previous stage of addNodes
  $goodHosts = readHostsFile($readFromFile);
  $goodHosts = convertToLowerCase($goodHosts);

  $logger->log_debug("goodHosts: ".json_encode($goodHosts));

  /////// Insert roles for these nodes ////////
  foreach ($components as $componentName) {
    $addHostsToComponentResult = $dbAccessor->addHostsToComponent($clusterName, $componentName, $goodHosts, "UNKNOWN", "UNKNOWN");
    if ($addHostsToComponentResult["result"] != 0 ) {
      $logger->log_error("Got error adding component $componentName :" .$addHostsToComponentResult["error"]);
      print json_encode($addHostsToComponentResult);
      return;
    }
  }

  $deployAddedNodesResult = $hmc->deployNodes($goodHosts);

  $txnId = $deployAddedNodesResult["txnId"];

  /* (And when we kick off the node addition is the only time to update the 
   * state of the cluster). 
   */
  $state = "NODE_ADDITION_IN_PROGRESS";
  $displayName = "Node addition in progress";
  $context = array (
    'txnId' => $txnId,
    /* We've come here only if the cluster is in the "DEPLOYED" state, so the 
     * state we stash is the state at the end of the deploy - we'll restore 
     * this state at the end of the service management action.
     */
    'stashedDeployState' => $clusterState
  );

  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $result = $retval['result'];
    $error = $retval['error'];
  }
}
/* In case a node addition is already running, just return the txnId 
 * from the DB instead of kicking off a fresh action - this is so 
 * we can try and preclude multiple additions occurring in parallel.
 */
elseif ($clusterState['state'] == 'NODE_ADDITION_IN_PROGRESS') {

  $txnId = $clusterState['context']['txnId'];
}

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId
  );

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));

?>
