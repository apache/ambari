<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once 'commandUtils.php';

function handleUninstallTransaction ($clusterName, $deployUser, $rootTxnId,
                                     $mySubTxnId, $parentSubTxnId, $hostsStr,
                                     $operationName, $cmdLine, $dbAccessor,
                                     $logger) {

  $opStatus = "STARTED";
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }

  $hosts = explode(",", $hostsStr);
  runPdsh($clusterName, $operationName, $deployUser, $hosts, $cmdLine);

  $retArr = parseAndUpdateNodeInfo($clusterName, $operationName, $logger);

  $allHosts = $retArr["allHosts"];
  $finalOpStatus = $retArr["finalOpStatus"];

  // Perisist the data to the db.
  $returnValue = $dbAccessor->addHostsToCluster($clusterName, $allHosts);
  if ($returnValue["result"] != 0 ) {
    $logger->log_error("Got error while adding hosts: ".$returnValue["error"]);
    print json_encode($returnValue);
    return;
  }

  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $finalOpStatus);
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }
}

?>
