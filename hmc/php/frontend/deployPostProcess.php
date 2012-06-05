<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once '../util/clusterState.php';

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

function deployPostProcess($clusterName, $user, $txnId, $progress)
{

  $logger = new HMCLogger("DeployPostProcess");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  $result = 0;
  $error = "";

  $txnStatus = $dbAccessor->getTransactionStatusInfo($clusterName, $txnId);
  if ($txnStatus['result'] != 0) {
    $logger->log_error("Deploy post process get txn info failed");
    $result = $txnStatus['result'];
    $error = $txnStatus['error'];
    return (array("result" => $result, "error" => $error));
  }

  $txnStatus = !($progress['encounteredError']);
  $state = "DEPLOYED";
  if ($txnStatus) {
    $displayName = "Deployed successfully";
  } else {
    $displayName = "Deploy failed";
  }

  $context = array (
    'status' => $txnStatus,
    'txnId' => $txnId
  );

  // update state of the cluster 
  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $logger->log_error("Update cluster state failed");
    $result = $retval['result'];
    $error = $retval['error'];
  }

  return (array("result" => $result, "error" => $error));
}

?>
