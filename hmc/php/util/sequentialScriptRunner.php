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


// all scripts run with base directory as frontend
include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once "../util/HMCTxnUtils.php";
include_once "commandUtils.php";
include_once '../util/util.php';

// this script will write to TransactionStatus table the following:
// 1. txn-id -> describes state sshable, discoverable, bootstrapable
// 2. status_info blob -> specific errors for the state

// initial setup
$logger = new HMCLogger("sequentialScriptExecutor");
$dbHandle = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function updateProgressForStage($clusterName, $rootTxnId, $orchestratorTxnId,
    $mySubTxnId, $operationName, $numTotalNodes) {
  global $logger, $dbHandle, $stagesInfo;

  $clusterDir = getClusterDir($clusterName);
  $commandOutputDir = $clusterDir . $operationName . "/";

  $numNodesFailed = 0;
  $numNodesSucceeded = 0;
  $additionalInfo = array();

  $foundDir = FALSE;
  if (is_dir($commandOutputDir) && $dirHandle = opendir($commandOutputDir)) {
    if (!$foundDir) {
      $logger->log_debug($commandOutputDir . " exists finally ");
    }
    $foundDir = TRUE;
    while (false !== ($entry = readdir($dirHandle))) {
      if ($entry == "." || $entry == "..") {
        continue;
      }
      // Only consider .out files
      if(!preg_match("/.out/", $entry)) {
        continue;
      }
      $nodeName = basename($entry, ".out");

      $doneFile = $commandOutputDir . $nodeName . ".done";
      if (file_exists($doneFile)) {
        // Read the contents of the done-file
        $doneFileContents = file_get_contents($doneFile);
        if (trim($doneFileContents) == "0") {
          $numNodesSucceeded++;
          $additionalInfo[$nodeName] = "Successful with no errors.";
        } else {
          $numNodesFailed++;
          // Let's read the errors
          $errorFile = $commandOutputDir . $nodeName . ".err";
          $errorFileContents = file_get_contents($errorFile);
          $additionalInfo[$nodeName] = "Failed. Reason: $errorFileContents";
        }
      }
    }
  } else {
    $logger->log_debug( $commandOutputDir . " still doesn't exist");
  }

  $logger->log_debug(" Current operation is $operationName and commandOutput dir: $commandOutputDir");
  $logger->log_debug(" total nodes: $numTotalNodes, succeeded: $numNodesSucceeded, failed: $numNodesFailed");
  $progressState = generateNodeActionProgress($numTotalNodes, $numNodesSucceeded, $numNodesFailed, "succeeded");
  $logger->log_debug(" Current progressState is ".$progressState);

  $state = json_encode($additionalInfo); $description = $stagesInfo[$operationName]["description"]; $progress = $progressState; $subTxnType = "";
  $subTransactionReturnValue = $dbHandle->insertOrUpdateSubTransaction($clusterName, $orchestratorTxnId, $mySubTxnId, $orchestratorTxnId, $state, $description, $progress, $subTxnType );
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while creating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }
}

$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$readFromFile = $argv[4];
$stagesFile = $argv[5];

include_once $stagesFile;

$logger->log_debug("Sequential runner params: $clusterName, $deployUser, $rootTxnId, $readFromFile, $stagesFile");

// Create a sub-txn for the orchestrator
$status = "";
$createTxResult = HMCTxnUtils::createNewTransaction($dbHandle, $clusterName, $status);
if ($createTxResult == FALSE) {
  $logger->log_error("Failed to create new transaction " . " in background: $createTxResult\n");
  return;
}
$orchestratorTxnId = $createTxResult;
$logger->log_debug("Txn Id: $orchestratorTxnId\n");

$state = ""; $description = "sequentialScriptExecutor"; $progress = ""; $subTxnType = "";
$subTransactionReturnValue = $dbHandle->insertOrUpdateSubTransaction($clusterName, $rootTxnId, $orchestratorTxnId, $rootTxnId, $state, $description, $progress, $subTxnType );
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while creating subTxn for sequentialScriptExecutor: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$logger->log_debug("sequentialScriptExecutor txnId: $orchestratorTxnId\n");

$startTime = time(0);

$cmd = "";
$currentStage = "";
$statusInfo = "";

$count = 0;

$thisHostName = trim(strtolower(exec('hostname -f')));

// Add the host which runs the server to the list
function addThisHostToList($hosts, $logger, $thisHostName, $readFromFile) {
  $result = array();
  foreach($hosts as $host) {
    array_push($result, $host);
    if ($thisHostName == $host) {
     $logger->log_debug("Management host $thisHostName exists".
      " in the hosts file");
     return $hosts;
    }
  }

  $outFd = fopen($readFromFile, "a");
  if ($outFd == FALSE) {
    $logger->log_error("Failed to add the hmc host to the nodes file");
    return $result;
  }

  fwrite($outFd, "\n".$thisHostName);
  fclose($outFd);

  array_push($result, $thisHostName);
  return $result;
}

$hosts = readHostsFile($readFromFile);
$hosts = convertToLowerCase($hosts);

$logger->log_debug("The hosts after converting to lower case ".print_r($hosts, true));
// Only add this host to list if the db does not have that host already.
$checkThisHostInDB = $dbHandle->getHostInfo($clusterName, $thisHostName);
$logger->log_debug("Host Info in DB ".print_r($checkThisHostInDB, true));
if ($checkThisHostInDB["result"] != 0) {
  $logger->log_info($thisHostName . "not found in DB so adding it to the list of hosts");
  $hosts = addThisHostToList($hosts, $logger, $thisHostName, $readFromFile);
}
$hosts = convertToLowerCase($hosts);

$logger->log_debug("The hosts that are being sshed to are ".print_r($hosts, true));
$allHosts = $hosts; // Keep a copy in case

foreach ($stagesInfo as $stage => $stageInfo) {

  // create a new subtransaction for each stage
  $mySubTxnId = 100 + $count;
  $state = ""; $description = $stagesInfo[$stage]["description"]; $progress = ": starting"; $subTxnType = "";
  $subTransactionReturnValue = $dbHandle->insertOrUpdateSubTransaction($clusterName, $orchestratorTxnId, $mySubTxnId, $orchestratorTxnId, $state, $description, $progress, $subTxnType );
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while creating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }

  $currentStage = $stage;

  // SubTxn is created. Set start op_status
  $updateSubTransactionStatusResult = $dbHandle->updateSubTransactionOpStatus($clusterName, $orchestratorTxnId, $mySubTxnId, $opStatus);
  if ($updateSubTransactionStatusResult["result"] != 0 ) {
    $logger->log_error("Got error while updating subTxn: ".$updateSubTransactionStatusResult["error"]);
    print json_encode($updateSubTransactionStatusResult);
    return;
  }
  // If the host list is empty, say because of failures in previous stage, no point carrying it on..
  $hostCount = count($hosts);
  if ($hostCount == 0) {
    $logger->log_info("Skipping stage " . $stage . " as no valid hosts available");
    continue; // so that all stages can get marked as failures
  }

  $cmd = $GLOBALS["PHP_EXEC_PATH"]." ".$stagesInfo[$stage]["scriptName"];
  $args = "$clusterName $deployUser $rootTxnId $mySubTxnId $orchestratorTxnId " . $readFromFile;
  $count++;
  $execBackgroundResult = HMCTxnUtils::execBackgroundProcess($dbHandle, $clusterName, $rootTxnId, $cmd, $args, "" );
  if ($execBackgroundResult == FALSE) {
    $logger->log_error("Failed to execute $currentStage".
      " in background: $execBackgroundResult\n");
    return;
  }

  $allSubTransactionsInfoResult = $dbHandle->getAllSubTransactionsInfo($clusterName, $orchestratorTxnId);
  if ($allSubTransactionsInfoResult["result"] != 0 ) {
    $logger->log_error("Got error while getting subTxnInfo: ".$allSubTransactionsInfoResult["error"]);
    print json_encode($allSubTransactionsInfoResult);
    return;
  }

  /*
  if (count($allSubTransactionsInfoResult["subTxns"]) != $count) {
    $logger->log_info("Still waiting ");
  }
  */

  //$mySubTxnId = '"'.$mySubTxnId.'"';
//  $logger->log_error(" sequentialScriptExecutors sub txns  " . json_encode($allSubTransactionsInfoResult));
  $successStatus = "SUCCESS";
  $errorStatus = "FAILED";
  $totalFailedStatus = "TOTALFAILURE";
  $currentStatus = $allSubTransactionsInfoResult["subTxns"][$mySubTxnId]["opStatus"];
  while ( $currentStatus != $successStatus && $currentStatus != $errorStatus
    && $currentStatus != $totalFailedStatus) {
        sleep(1);

        $allSubTransactionsInfoResult = $dbHandle->getAllSubTransactionsInfo($clusterName, $orchestratorTxnId);
        if ($allSubTransactionsInfoResult["result"] != 0 ) {
          $logger->log_error("Got error while getting subTxnInfo: ".$allSubTransactionsInfoResult["error"]);
          print json_encode($allSubTransactionsInfoResult);
          return;
        }
        $currentStatus = $allSubTransactionsInfoResult["subTxns"][$mySubTxnId]["opStatus"];

        //$logger->log_debug(" sequentialScriptExecutors sub txns  " . json_encode($allSubTransactionsInfoResult));
        $logger->log_debug("Host count is $hostCount");
        if ($currentStatus != $successStatus && $currentStatus != $errorStatus
            && $currentStatus != $totalFailedStatus) {
          updateProgressForStage($clusterName, $rootTxnId,
            $orchestratorTxnId, $mySubTxnId, $stage, $hostCount);
        }

        //$logger->log_debug("Status we are seeing: " . $currentStatus . " txnId: " . $orchestratorTxnId . " subTxnId " . $mySubTxnId);
  }

  // Just in case, the command finished too fast and the while loop is skipped.
  updateProgressForStage($clusterName, $rootTxnId, $orchestratorTxnId, $mySubTxnId, $stage, $hostCount);
  $logger->log_debug("Came out of the launch for stage " . $currentStage . "\n");
  unset($subTxn);

  if ($currentStatus == $totalFailedStatus) {
    $logger->log_error("Encountered total failure in transaction $mySubTxnId" .
     " while running cmd: $cmd with args: $args");
    return;
  }

  ////////// Construct the host list needed for next stage ////////////
  $nextStageHosts = array();
  foreach ($hosts as $host) {
     ////// Get info about this node from db ////
    $hostInfo = $dbHandle->getHostInfo($clusterName,$host);
    if ($hostInfo["result"] != 0 ) {
      $logger->log_error("Got error while getting hostInfo for $host :" .$hostInfo["error"]);
      continue;
    }

    if ($hostInfo["discoveryStatus"] == "SUCCESS") {
      $nextStageHosts[] = $host;
    }
  }

  // need to provide this list of hosts to the next phase through the hosts.txt
  $fp = fopen($readFromFile, "w");
  foreach ($nextStageHosts as $nextStageHost) {
    fwrite($fp, $nextStageHost."\n");
  }
  fclose($fp);
  // Change the host list to weed-out bad nodes.
  $hosts = $nextStageHosts;
  ////////// End of constructructing the host list needed for next stage ////////////

}

?>
