<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once '../util/clusterState.php';

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

function manageServicesPostProcess($clusterName, $user, $txnId, $progress)
{
  $logger = new HMCLogger("ManageServicesPostProcess");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  $result = 0;
  $error = "";

  /* Safe fallbacks, in case the call to getClusterState() below fails. */
  $state = "DEPLOYED";
  $displayName = "Deployed successfully";
  $context = array (
    'status' => TRUE
  );

  $clusterStateResponse = $dbAccessor->getClusterState($clusterName);

  if ($clusterStateResponse['result'] != 0) {
    $logger->log_error("Failed to fetch cluster state (for restoration of stashed state)");

    $result = $clusterStateResponse["result"];
    $error = $clusterStateResponse["error"];
  }
  else {
    $clusterState = json_decode($clusterStateResponse['state'], true);

    $stashedDeployState = $clusterState["context"]["stashedDeployState"]; 
    /* Restore the cluster's state to that stashed at the time of beginning the
     * service management. 
     */
    $state = $stashedDeployState["state"];
    $displayName = $stashedDeployState["displayName"];
    $context = $stashedDeployState["context"];
  }

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
