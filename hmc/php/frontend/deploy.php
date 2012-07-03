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
include_once "../orchestrator/HMC.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";
include_once "../util/clusterState.php";

$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];

$hmc = new HMC($dbPath, $clusterName);

/* For returning in our JSON at the very end. */
$result = 0;
$error = "";

$txnId = -1;

$dbAccessor = new HMCDBAccessor($dbPath);
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);

/* Run an actual deploy only if this cluster has passed the stage of 
 * configuring its services (which means the next thing to do is to
 * kick off the deploy). 
 */
if (($clusterState['state'] == 'CONFIGURATION_IN_PROGRESS') && 
    ($clusterState['context']['stage'] == 'CONFIGURE_SERVICES')) {

  $deployResult = $hmc->deployHDP();
  if ($deployResult["result"] != 0) {
    print json_encode($deployResult);
    return;
  }

  if (!isset($deployResult["txnId"])) {
    print json_encode ( array("result" => 1, "error" => "Could not obtain txn info for triggered command"));
    return;
  }

  $txnId = $deployResult["txnId"];

  /* (And when we kick off the deploy is the only time to update the state of 
   * the cluster).
   */
  $state = "DEPLOYMENT_IN_PROGRESS";
  $displayName = "Deployment in progress";
  $context = array (
    'txnId' => $txnId
  );

  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $result = $retval['result'];
    $error = $retval['error'];
  }
}
/* In case the deploy is already running or has ended, just return the txnId 
 * from the DB instead of kicking off a fresh deploy - this is so we can use
 * this entrypoint to show the cluster's deploy progress at any time in the
 * future, not just during a live deploy. 
 */
elseif (($clusterState['state'] == 'DEPLOYMENT_IN_PROGRESS') ||
        ($clusterState['state'] == 'DEPLOYED')) {

  $txnId = $clusterState['context']['txnId'];
}

$thisHostName = trim(strtolower(exec('hostname -f')));

$nagiosGangliaCoHosted = FALSE;

// check if nagios hosted on same server
if (!$nagiosGangliaCoHosted) {
  // check if component mapped to this host
  $hostMap = $dbAccessor->getHostsForComponent($clusterName, "NAGIOS_SERVER");
  if (isset($hostMap["hosts"][$thisHostName])) {
    $nagiosGangliaCoHosted = TRUE;
  }
}

// if still nothing then check if ganglia server installed on same server
if (!$nagiosGangliaCoHosted) {
  // check if component mapped to this host
  $hostMap = $dbAccessor->getHostsForComponent($clusterName,
      "GANGLIA_MONITOR_SERVER");
  if (isset($hostMap["hosts"][$thisHostName])) {
    $nagiosGangliaCoHosted = TRUE;
  }
}

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId,
    'nagiosGangliaCoHosted' => $nagiosGangliaCoHosted);

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));

?>
