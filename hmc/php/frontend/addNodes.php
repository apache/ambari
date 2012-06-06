<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "../util/clusterState.php";
include_once '../util/util.php';

include_once './commandUtils.php';
/*
sleep(3);
 */

$logger = new HMCLogger("UploadFiles");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];

/* Figure out whether it's a fresh install or an AddNodesWizard-ish flow. */
$clusterStateResponse = $dbAccessor->getClusterState($clusterName);

if ($clusterStateResponse['result'] != 0) {
  print json_encode($clusterStateResponse);
  return;
}

$clusterState = json_decode($clusterStateResponse['state'], true);
$freshInstall = ($clusterState['state'] == 'CONFIGURATION_IN_PROGRESS') ? true : false;

$clusterDir = getClusterDir($clusterName);

$logString = "Cluster Name: $clusterName Cleanup required? $freshInstall and type: ".gettype($freshInstall);
$logger->log_debug($logString);
// Validate clusterName: TODO; FIXME

// We need to clean up prior instances for this
// cluster name if this is a fresh install
if ($freshInstall) {
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

header("Content-type: application/json");

// Validate that there are no nodes that are already in use in case of addNodesWizard
$logger->log_debug("$freshInstall");
if (!$freshInstall) {

  // Get the list of current nodes
    $allHostsInfoResult = $dbAccessor->getAllHostsInfo($clusterName, array());
    if ($allHostsInfoResult["result"] != 0 ) {
      $logger->log_error("Got error while getting hostInfo for $host :" .$allHostsInfoResult["error"]);
      print json_encode($allHostsInfoResult);
      return;
    }

    // See if they are duplicates
    $newHosts = readHostsFile($hostsFileDestination);
    $duplicateHosts = array();
    $logger->log_debug("checking for dups");
    foreach ($allHostsInfoResult["hosts"] as $hostInfo) {
      if (in_array($hostInfo["hostName"], $newHosts)) {
        $duplicateHosts[] = $hostInfo["hostName"];
      }
    }
    $numDupHosts = count($duplicateHosts);
    $numNewHosts = count($newHosts);
    if ($numDupHosts != 0) {
      if ($numNewHosts == $numDupHosts) {
        print (json_encode(array("result" => 2, "error" => "All the hosts in the given file are already being used in cluster '$clusterName'")));
      } else {
        print (json_encode(array("result" => 3, "error" => "Some hosts in the given file are already being used in cluster '$clusterName'", "hosts" => implode(",", $duplicateHosts))));

        // Just re-edit the hosts' file in case users says go ahead
        $nodeFileOut = fopen($hostsFileDestination, "w");
        foreach ($newHosts as $newHost) {
          if (in_array($newHost, $duplicateHosts)) {
            continue;
          }
          fwrite($nodeFileOut, $newHost."\n");
        }
        fclose($nodeFileOut);
      }
      return;
    }
} else {
  // Update the state of the cluster.
  $state = "CONFIGURATION_IN_PROGRESS";
  $displayName = "Configuration in progress";
  $context = array (
      'stage' => "ADD_NODES"
      );

  $retval = updateClusterState($clusterName, $state, $displayName, $context);
}

print (json_encode(array("result" => 0)));

/*
$outjson = array(
                  "errorCode"=> $retval['result'],
 //                 "clusterName" => $clusterName,
 //                 "finalDestination" => $identityFileDestination,
 //                 "fileToBeMoved" => $_FILES['clusterDeployUserIdentityFile']['tmp_name'],
                );
                */

// TODO: FIXME: Check file lengths.

?>
