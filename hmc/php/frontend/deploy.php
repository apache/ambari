<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "../orchestrator/HMC.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";

$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];

$startTime = time();
$hmc = new HMC($dbPath, $clusterName);

$result = $hmc->deployHDP();
if ($result["result"] != 0) {
  print json_encode($result);
  return;
}

if (!isset($result["txnId"])) {
  print json_encode ( array("result" => 1, "error" => "Could not obtain txn info for triggered command"));
  return;
}

$txnId = $result["txnId"];

/* Create the output data... */
$jsonOutput = array(
    'startTime' => $startTime,
    'clusterName' => $clusterName,
    'txnId' => $txnId );

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode(array("result" => 0, "error" => 0, "response" => $jsonOutput)));

?>
