<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

$logger = new HMCLogger("ListClusters");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$listOfClusters = array();

$allClustersResult = $dbAccessor->getAllClusters();

header("Content-type: application/json");

$response = array( "result" => 0, "error" => "" );

// Errors
if ($allClustersResult["result"] != 0) {
  print(json_encode($allClustersResult));
  exit(1);
}

foreach($allClustersResult["clusters"] as $clusterId => $value) {
  $logger->log_debug("Got cluster ".$clusterId.json_encode($value));
  $listOfClusters[$value["clusterName"]] = $value["state"];
}

$response["response"] = $listOfClusters;
print (json_encode($response));

?>
