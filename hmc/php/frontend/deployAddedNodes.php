<?php

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

/* Now comes in from Config.inc
$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "/tmp/orchestrator-test.log";
$GLOBALS["BACKGROUND_EXECUTOR_PATH"] = "../util/BackgroundExecutor.php";
$GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
$GLOBALS["CLUSTERMAIN_PATH"] = "../orchestrator/ClusterMain.php";
$GLOBALS["puppetManifestDir"] = "/etc/puppet/master/manifests/";
$GLOBALS["puppetKickVersionFile"] = "/tmp/puppet_kick_version.txt";
$GLOBALS["puppetReportsDir"] = "/usr/local/rvm/gems/ruby-1.8.7-p358/gems/puppet-2.7.9/lib/puppet/reports/";
$GLOBALS["puppetKickTimeout"] = 60;
$GLOBALS["puppetMaxParallelKicks"] = 10;
$GLOBALS["DRYRUN"] = FALSE;
*/

/* vinodkv
system("mkdir -p ".$GLOBALS["puppetManifestDir"]);
system("mkdir -p ".$GLOBALS["puppetReportsDir"]);
*/


$logger = new HMCLogger("deployAddedNodes");
$dbHandle = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$dbPath = $GLOBALS["DB_PATH"];
$clusterName = $_GET['clusterName'];
$readFromFile = getHostsFilePath($clusterName);

$requestData = file_get_contents('php://input');
$components = json_decode($requestData, true);

$logger->log_debug("Components are ".json_encode($components));

$startTime = time();
$hmc = new HMC($dbPath, $clusterName);

// Get info about all nodes from hosts file 
// the host file has been pruned by the previous stage of addNodes
$goodHosts = readHostsFile($readFromFile);
$goodHosts = convertToLowerCase($goodHosts);

$logger->log_debug("goodHosts: ".json_encode($goodHosts));

/////// Insert roles for these nodes ////////
foreach ($components as $componentName) {
  $addHostsToComponentResult = $dbHandle->addHostsToComponent($clusterName, $componentName, $goodHosts, "UNKNOWN", "UNKNOWN");
  if ($addHostsToComponentResult["result"] != 0 ) {
    $logger->log_error("Got error adding component $componentName :" .$addHostsToComponentResult["error"]);
    print json_encode($addHostsToComponentResult);
    return false;
  }
}

$result = $hmc->deployNodes($goodHosts);
//print_r($result);

//assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId );

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode($jsonOutput));

/*
do {
  $curTime = time();
  $progress = $hmc->getProgress($txnId);

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  print_r($progress);

  print "======================";
  print(file_get_contents($GLOBALS["puppetManifestDir"]."site.pp"));
  print "======================";

  if (!is_array($progress)
      || $progress["result"] != 0
      || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

print "======================\n";
print "Getting puppet logs\n";
print "======================\n";
$logs = $hmc->getLogs($txnId);
print_r($logs);
*/

?>
