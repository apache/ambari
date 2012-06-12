<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once '../util/suggestProperties.php';
include_once '../util/clusterState.php';

$logger = new HMCLogger("ConfigureCluster");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

header("Content-type: application/json");

$clusterName = $_GET['clusterName'];
// TODO: Validate clusterName

$requestData = file_get_contents('php://input');
$requestObj = json_decode($requestData, true);

// persist the user entered mount points *******
foreach ($requestObj["clusterConfig"] as $serviceName=>$objects) {
  $dbAccessor->updateServiceConfigs($clusterName, $objects);
}
// finished persisting the mount point info *******

// logic for next stage *******
// trigger script to do advanced config recommendations
$suggestProperties = new SuggestProperties();
$ret = $suggestProperties->suggestProperties($clusterName, $dbAccessor, TRUE);
if ($ret["result"] != 0) {
  $logger->log_error("Failed to run advanced properties suggestion, error=".$ret["error"]);
  print (json_encode($ret));
  return;
}
// logic for next stage ends here *******

// give back json data for the advanced cluster properties *******
// Return a valid response
$jsonOutput = array();
$jsonOutput["clusterName"] = $clusterName;

// Update the state of the cluster.
$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";
$context = array (
  'stage' => "CONFIGURE_CLUSTER"
);

$retval = updateClusterState($clusterName, $state, $displayName, $context);
if ($retval['result'] != 0) {
  $result = $retval['result'];
  $error = $retval['error'];
}

print(json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));

?>
