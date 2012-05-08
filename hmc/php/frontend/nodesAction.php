<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

$logger = new HMCLogger("setupNodes");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];
$action = $_GET['action'];
$deployUser = $_POST['ClusterDeployUser'];

header("Content-type: application/json");

$stagesFiles = "";
if ($action == "addNodes") {
  $stagesFile = "./addNodes/stages.php";
} else if ($action == "uninstall") {
  $stagesFile = "./uninstall/stages.php";
} else {
  print (json_encode(array(
    "result" => 1,
    "error" => "Invalid action",
  )
  ));
  return;
}

error_log("ClusterName: ".$clusterName);

$logger->log_debug("ClusterName: $clusterName\n");
$logger->log_debug("Deploy User: $deployUser\n");

// this api just creates a new transaction id for the db
$status = "";
$createTxResult = HMCTxnUtils::createNewTransaction($dbAccessor, $clusterName, $status);
if ($createTxResult == FALSE) {
  $msg = "SETUP: Failed to create new transaction in background: $createTxResult\n";
  $logger->log_error($msg);
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
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
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
}

print (json_encode(array(
  "result" => 0,
  "error" => "",
  "response" => array(
    "clusterName" => $clusterName,
    "txnId" => $rootTxnId,
  )
)
));

?>
