<?php
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


$logger = new HMCLogger("Uninstall");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$logger->log_debug("Uninstall invoked");
$clusterName = $_GET['clusterName'];
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
  array("=" => array ( "discoveryStatus" => "SUCCESS"));
if ($allHostsInfo["result"] != 0 ) {
  $logger->log_error("Got error while getting hostsInfo ".$allHostsInfo["error"]);
  print json_encode($allHostsInfo);
  return;
}

$hostFileName = getHostsFilePath($clusterName);

$hostFileHdl = fopen($hostFileName, "w");

foreach ($allHostsInfo["hosts"] as $hostInfo) {
  fwrite($hostFileHdl, "$hostInfo["hostName"]\n");
}

fclose($hostFileHdl);
$logger->log_debug("HOST FILE IS NOW POPULATED GO AHEAD!!");
sleep(5);
////// end of generating new file

$logger->log_debug("Uninstall got wipeout value $wipeout");

$dbPath = $GLOBALS["DB_PATH"];

// call the wipeout script and return the transaction id
$hmc = new HMC($dbPath, $clusterName);

$startTime = time();
$result = $hmc->uninstallHDP($wipeout);
if ($result["result"] != 0) {
  print json_encode($result);
  return;
}

if (!isset($result["txnId"])) {
  print json_encode ( array("result" => 1, "error" => "Could not obtain txn info for triggered command"));

  return;
}

$txnId = $result["txnId"];

$jsonOutput = array(
    'startTime' => $startTime,
    'clusterName' => $clusterName,
    'deployUser' => $deployUser,
    'txnId' => $txnId,
  );

header("Content-type: application/json");
print (json_encode(array("result" => 0, "error" => 0, "response" => $jsonOutput)));

?>
