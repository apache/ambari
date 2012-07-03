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

include_once "../util/util.php";
include_once "../util/HMCTxnUtils.php";
include_once 'commandUtils.php';

$logger = new HMCLogger("findSshableNodes");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function getCommandLine() {
  $cmdLine = "hostname ;";
  // uncomment following line for demo purposes.
  // $cmdLine = $cmdLine . 'sleep $[ $RANDOM % 5 ]; ';
  return $cmdLine;
}

$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$mySubTxnId = $argv[4];
$parentSubTxnId = $argv[5];
$readFromFile = $argv[6];

$opStatus = "STARTED";
$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$stageName = "findSshableNodes";
$cmdLine = getCommandLine();
// $hosts = explode(",", $hostsStr);
runPdsh($clusterName, $stageName, $deployUser, $readFromFile, $cmdLine);

////////////// now read the per-host output files to get ssh-able information about each node ////////////////
$clusterDir = getClusterDir($clusterName);
$commandOutputDir = $clusterDir . $stageName . "/";

$allHosts = array();

$finalOpStatus = "SUCCESS";

$numTotalNodes = 0;
$numNodesSucceeded = 0;
$numNodesFailed = 0;
if ($dirHandle = opendir($commandOutputDir)) {
  while (false !== ($entry = readdir($dirHandle))) {
    if ($entry == "." || $entry == "..") {
      continue;
    }

    $nodeStatus = "SUCCESS";
    // Only consider .out files
    if(!preg_match("/.out/", $entry)) {
      continue;
    }
    $nodeName = basename($entry, ".out");
    $nodeStatus = "SUCCESS";

    $doneFile = $commandOutputDir . $nodeName . ".done";
    if (file_exists($doneFile)) {
      // Read the contents of the done-file
      $doneFileContents = file_get_contents($doneFile);
      if (trim($doneFileContents) != "0") {
        $numNodesFailed += 1;
        $nodeStatus = "FAILED";
        $finalOpStatus = "FAILED";
        $logger->log_debug( "Contents of done file for $clusterName : $doneFileContents");
      }
    } else {
      $numNodesFailed += 1;
      $nodeStatus = "FAILED";
      $finalOpStatus = "FAILED";
    }

    // Initialize this host's array
    $thisHostArray = array();
    $thisHostArray["hostName"] = strtolower($nodeName);
    $thisHostArray["totalMem"] = 0;
    $thisHostArray["cpuCount"] = 0;
    $thisHostArray["osArch"] = "";
    $thisHostArray["disksInfo"] = json_encode(array());
    $thisHostArray["osType"] = "";
    $thisHostArray["os"] = "";
    $thisHostArray["ip"] = $nodeName; // To be unique

    if ($nodeStatus != "FAILED") {
      $sshContents = file_get_contents($commandOutputDir.$entry);
      if ($sshContents == "") {
        $numNodesFailed += 1;
        $finalOpStatus = "FAILED";
        $nodeStatus = "FAILED";
      }
    }

    // since node status can be updated in the above block as well.
    if ($nodeStatus != "FAILED") {
      $numNodesSucceeded += 1;
    } else {
      $thisHostArray["badHealthReason"] =
          rtrim(file_get_contents($commandOutputDir.$nodeName . ".err"));
    }

    $thisHostArray["discoveryStatus"] = $nodeStatus;

    array_push($allHosts, $thisHostArray);
  }

  closedir($dirHandle);
}

// Perisist the data to the db.
$logger->log_info("Going to persist information sshAble nodes");
$returnValue = $dbAccessor->addHostsToCluster($clusterName, $allHosts);
if ($returnValue["result"] != 0 ) {
  $logger->log_error("Got error while adding hosts: ".$returnValue["error"]);
  print json_encode($returnValue);
  return;
}

if ($numNodesSucceeded == 0) {
  $finalOpStatus = "TOTALFAILURE";
}

$nodeFileOut = fopen($readFromFile, "w");
if ($nodeFileOut == FALSE) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error while trying to rewrite hosts file");
  return;
}

// foreach successfully discovered host write the host list to the readFromFile
foreach ($allHosts as $hostInfo) {
  if ($hostInfo["discoveryStatus"] == "FAILED") {
    continue;
  }

  // write the nodename to the readFromFile file.
  fwrite($nodeFileOut, $hostInfo["hostName"]."\n");
}

fclose($nodeFileOut);

$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $finalOpStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

?>
