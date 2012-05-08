<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

/*
sleep(3);
 */

$logger = new HMCLogger("UploadFiles");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];
$cleanup = $_GET['freshInstall'];
$clusterDir = getClusterDir($clusterName);

$logString = "Cluster Name: $clusterName Cleanup required? $cleanup and type: ".gettype($cleanup);
$logger->log_debug($logString);
// Validate clusterName: TODO; FIXME

// We need to clean up prior instances for this
// cluster name if this is a fresh install
if ($cleanup == 'true') {
  $dbAccessor->cleanupHosts($clusterName);
}

// Read from the input
$deployUser = $_POST['ClusterDeployUser'];

$identityFileDestination = getSshKeyFilePath($clusterName);
$hostsFileDestination = getHostsFilePath($clusterName);

// TODO: FIXME: Change echos to return error codes.
//echo '<pre>';
if (move_uploaded_file($_FILES['clusterDeployUserIdentityFile']['tmp_name'], $identityFileDestination)) {
    //echo "File is valid, and was successfully uploaded.\n";

  // Set the permissions
  chmod($identityFileDestination, 0400);// TODO: Error conditions
} else {
  $logger->log_error("Identity file copy to loc ".$identityFileDestination." failed");
    //echo "Possible file upload attack!\n";
}

if (move_uploaded_file($_FILES['clusterHostsFile']['tmp_name'], $hostsFileDestination)) {
    //echo "File is valid, and was successfully uploaded.\n";
  } else {
  $logger->log_error("Hosts file copy to loc ".$hostsFileDestination." failed");
    //echo "Possible file upload attack!\n";
}

$outjson = array(
                  "errorCode"=> 0,
 //                 "clusterName" => $clusterName,
 //                 "finalDestination" => $identityFileDestination,
 //                 "fileToBeMoved" => $_FILES['clusterDeployUserIdentityFile']['tmp_name'],
                );

// Post the response so that the front-end assumes this is back-grounded
/*
$outfile = $clusterDir."/json.out";
$outHandle = fopen($outfile, "w");
fwrite($outHandle, json_encode($outjson));
fclose($outHandle);
$outStr = json_encode($outjson);
$len = strlen($outStr);
header("Content-Length: $len");
header("Content-type: application/json");
print (($outStr));
ob_flush();
flush();
*/

/*
// invoke the node-discoverer
$logger->log_debug("About to discover node properties");
$allHosts = discoverNodes($clusterName, $deployUser);
$logger->log_debug("All Hosts Info \n".print_r($allHosts, true));
$hostList = array();
foreach($allHosts as $hostInfo) {
  $logger->log_debug("HOST: ".json_encode($hostInfo));
  array_push($hostList, $hostInfo["hostName"]);
}
*/

/* Bootstrap happens here - installs puppet.
$repository=array();
$repository['name']="hmc_puppet";
$repository['desc']="puppetlabs";
$repository['url']="http://yum.puppetlabs.com/el/5/products/x86_64/";
$repository['gpgkeyurl']="http://yum.puppetlabs.com/RPM-GPG-KEY-puppetlabs";
$logger->log_debug("BootStrapping with puppet");
$boot_result = bootstrap($hostsFileDestination, $GLOBALS["puppet_install"], getSshKeyFilePath($clusterName), $repository);
$logger->log_debug("Boot Result \n".print_r($boot_result, true));
 */

/*
// Perisist the data to the db.
$logger->log_debug("Going to persist discovered node properties");
$returnValue = $dbAccessor->addHostsToCluster($clusterName, $allHosts);
if ($returnValue["result"] != 0 ) {
  $logger->log_error("Got error while adding hosts: ".$returnValue["error"]);
  print json_encode($returnValue);
  return;
}
*/

// TODO: FIXME: Check file lengths.

?>
