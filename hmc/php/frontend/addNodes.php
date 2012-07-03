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
$logger->log_info($logString);
// Validate clusterName: TODO; FIXME

// We need to clean up prior instances for this
// cluster name if this is a fresh install
if ($freshInstall) {
  $dbAccessor->cleanupHosts($clusterName);
}

// Read from the input
$deployUser = $_POST['ClusterDeployUser'];

/////// Remove the cluster directory before copying the files
$clusterDir = getClusterDir($clusterName);
rrmdir($clusterDir);
if (!is_dir($clusterDir) && !mkdir($clusterDir, 0700, true)) {
  print json_encode(array( "result" => 1, "error" => 'Failed to create directory...'));
  return;
}
////// end of directory removal

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

removeCarriageReturn($hostsFileDestination);

header("Content-type: application/json");

// Validate that there are no nodes that are already in use in case of addNodesWizard
$logger->log_info("Doing a fresh install: $freshInstall");
if (!$freshInstall) {

  // Get the list of current nodes
  $allHostsInfoResult = $dbAccessor->getAllHostsInfo("", array());
  if ($allHostsInfoResult["result"] != 0 ) {
    $logger->log_error("Got error while getting hostInfo for $host :" .$allHostsInfoResult["error"]);
    print json_encode($allHostsInfoResult);
    return;
  }

  $unassignedHostResult = $dbAccessor->getAllUnassignedHosts($clusterName);
  if ($unassignedHostResult["result"] != 0) {
    print json_encode($unassignedHostResult);
    return;
  }

  $unassignedHostList = $unassignedHostResult["hosts"];

  // See if they are duplicates
  $newHosts = readHostsFile($hostsFileDestination);
  $duplicateHosts = array();
  $logger->log_debug("Checking for Duplicate Hosts.");
  foreach ($allHostsInfoResult["hosts"] as $hostInfo) {
    $logger->log_debug("Checking for Duplicate Hosts. hostname = ".
      $hostInfo["hostName"]. " key does not exist? " . 
      array_key_exists($hostInfo["hostName"], $unassignedHostList). 
      " incoming cluster name: $clusterName, host is part of cluster "
      .$hostInfo["clusterName"]);
    if (in_array($hostInfo["hostName"], $newHosts) &&
      (!(array_key_exists($hostInfo["hostName"], $unassignedHostList)) 
      || ($hostInfo["clusterName"] != $clusterName))) {
        if (!array_key_exists($hostInfo["clusterName"], $duplicateHosts)) {
          $duplicateHosts[$hostInfo["clusterName"]] = array();
        }
        array_push($duplicateHosts[$hostInfo["clusterName"]], 
                   $hostInfo["hostName"]);
      }
  }
  $numDupHosts = count($duplicateHosts);
  $numNewHosts = count($newHosts);
  if ($numDupHosts != 0) {
    $logger->log_warn("Trying to add duplicate nodes to the cluster, dups="
        . print_r($duplicateHosts, true));
    print (json_encode(array("result" => 3, "error" => "Some hosts in the given file are already being used in cluster", "hosts" => $duplicateHosts)));
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
