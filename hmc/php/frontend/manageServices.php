<?php

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

$logger = new HMCLogger("ManageServices");
$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];
$dbAccessor = new HMCDBAccessor($dbPath);

header("Content-type: application/json");

function performServiceManagement( $hmc, $requestObj )
{
  global $logger;
  global $dbAccessor;
  global $clusterName;

  /* What we're here to return. */
  $result = array();

  $action = $requestObj['action'];

  $serviceNames = array_keys($requestObj['services']);

  switch( $action )
  {
    case 'startAll':
      $result = $hmc->startAllServices();
      break;

    case 'stopAll':
      $result = $hmc->stopAllServices();
      break;

    case 'start':
      if( count($serviceNames) > 0 ) {
        $result = $hmc->startServices( $serviceNames );
      }
      break;

    case 'stop':
      if( count($serviceNames) > 0 ) {
        $result = $hmc->stopServices( $serviceNames );
      }
      break;

    case 'reconfigure':

      if( count($serviceNames) > 0 ) {

        /* Read additional data from $requestObj and update the DB
         * accordingly before attempting to call $hmc->reconfigureServices().
         *
         */

        /*
        $configsToUpdate = array ();
        foreach ($requestObj['services'] as $svcName => $svcInfo) {
          if (!isset($svcInfo["properties"])
              || !is_array($svcInfo["properties"])) {
            continue;
          }
          // TODO: Stupid translation, FIXME says tshooter to hitman
          $finalProperties = array();
          foreach ($svcInfo["properties"] as $key => $valueObj) {
            $finalProperties[$key] = $valueObj["value"];
          }
          $configsToUpdate = array_merge($configsToUpdate, $finalProperties);
        }
        $result = $dbAccessor->updateServiceConfigs($clusterName, $configsToUpdate);
        if ($result['result'] != 0) {
          $logger->log_error("Failed to update the configs in DB, error=" . $result["error"]);
          return $result;
        }
        */

        // re-using persistConfigs code
        $result = validateAndPersistConfigsFromUser($dbAccessor, $logger, $clusterName, $requestObj['services']);
        if ($result['result'] != 0) {
          $logger->log_error("Failed to validate configs from user, error=" . $result["error"]);
          return $result;
        }

        /*
         * Also, remember to save a snapshot of the outgoing service
         * config in the 'ConfigHistory' table (thanks, @tshooter).
         */
        $changeMsg = "Reconfiguring services, list=" . implode(",", $serviceNames);
        $result = $dbAccessor->createServiceConfigSnapshot($clusterName, $changeMsg);
        if ($result['result'] != 0) {
          $logger->log_error("Failed to create config snapshot in DB, error=" . $result["error"]);
          return $result;
        }

        $result = $hmc->reconfigureServices( $serviceNames );
      }
      break;

    default:
      $logger->log_error( "Unrecognized action '" . $action . "' requested" );
      $result = array ( "result" => -1 , "error" => "Invalid action, action=" . $action);
      break;
  }

  return $result;
}

$hmc = new HMC($dbPath, $clusterName);

/* Slurp in the POST body. */
$requestData = file_get_contents('php://input');
$requestObj = json_decode($requestData, true);

/* The Main Event. */
$result = performServiceManagement($hmc, $requestObj);

/* Augment $result with 'clusterName'. */
$result["clusterName"] = $clusterName;

if ($result["result"] != 0 ) {
  $logger->log_error("Failed to take an action in manage services, error=" . $result["error"]);
}

print (json_encode($result));

?>
