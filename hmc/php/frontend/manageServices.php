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
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once "../orchestrator/HMC.php";
include_once "../orchestrator/Service.php";
include_once "../orchestrator/ServiceComponent.php";
include_once "../db/Transaction.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";
include_once "configUtils.php";
include_once '../util/suggestProperties.php';
include_once "../util/clusterState.php";

function performServiceManagement( $hmc, $requestObj )
{
  global $logger;
  global $dbAccessor;
  global $clusterName;

  /* What we're here to return. */
  $serviceManagementResult = array();

  $action = $requestObj['action'];

  $serviceNames = array_keys($requestObj['services']);

  switch( $action )
  {
    case 'startAll':
      $serviceManagementResult = $hmc->startAllServices();
      break;

    case 'stopAll':
      $serviceManagementResult = $hmc->stopAllServices();
      break;

    case 'start':
      if( count($serviceNames) > 0 ) {
        $serviceManagementResult = $hmc->startServices( $serviceNames );
      }
      break;

    case 'stop':
      if( count($serviceNames) > 0 ) {
        $serviceManagementResult = $hmc->stopServices( $serviceNames );
      }
      break;

    case 'reconfigure':

      if( count($serviceNames) > 0 ) {

        /* Read additional data from $requestObj and update the DB
         * accordingly before attempting to call $hmc->reconfigureServices().
         */
        // re-using persistConfigs code
        $finalProperties = sanitizeConfigs($requestObj['services'], $logger);
        $serviceManagementResult = validateAndPersistConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties);
        if ($serviceManagementResult['result'] != 0) {
          $logger->log_error("Failed to validate configs from user, error=" . $serviceManagementResult["error"]);
          return $serviceManagementResult;
        }

        /* Also, remember to save a snapshot of the outgoing service
         * config in the 'ConfigHistory' table.
         */
        $changeMsg = "Reconfiguring services, list=" . implode(",", $serviceNames);
        $serviceManagementResult = $dbAccessor->createServiceConfigSnapshot($clusterName, $changeMsg);
        if ($serviceManagementResult['result'] != 0) {
          $logger->log_error("Failed to create config snapshot in DB, error=" . $serviceManagementResult["error"]);
          return $serviceManagementResult;
        }

        $serviceManagementResult = $hmc->reconfigureServices( $serviceNames );
      }
      break;

    default:
      $logger->log_error( "Unrecognized action '" . $action . "' requested" );
      $serviceManagementResult = array ( "result" => -1 , "error" => "Invalid action, action=" . $action);
      break;
  }

  return $serviceManagementResult;
}

$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];

/* For returning in our JSON at the very end. */
$result = 0;
$error = "";

$txnId = -1;

$logger = new HMCLogger("ManageServices");

$dbAccessor = new HMCDBAccessor($dbPath);
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);

/* Perform the actual management only if this cluster is in a deployed state 
 * (regardless of whether the deploy was a success or failure). 
 */
if ($clusterState['state'] == 'DEPLOYED') {

  $hmc = new HMC($dbPath, $clusterName);

  /* Slurp in the POST body. */
  $requestData = file_get_contents('php://input');
  $requestObj = json_decode($requestData, true);

  /* The Main Event. */
  $serviceManagementResult = performServiceManagement($hmc, $requestObj);

  if ($serviceManagementResult["result"] != 0 ) {
    $logger->log_error("Failed to take an action in manage services, error=" . $serviceManagementResult["error"]);
    print json_encode($serviceManagementResult);
    return;
  }

  $txnId = $serviceManagementResult["txnId"];

  /* (And when we kick off the management action is the only time to update the 
   * state of the cluster). 
   */
  $state = "SERVICE_MANAGEMENT_IN_PROGRESS";
  $displayName = "Service management in progress";
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
/* In case a management operation is already running, just return the txnId 
 * from the DB instead of kicking off a fresh managemen action - this is so 
 * we can try and preclude multiple managements occurring in parallel.
 */
elseif ($clusterState['state'] == 'SERVICE_MANAGEMENT_IN_PROGRESS') {

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
