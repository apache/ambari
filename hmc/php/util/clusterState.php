<?php

// initial setup
function updateClusterState ($clusterName, $state, $displayName, $context) {
  $logger = new HMCLogger("ClusterState");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  $stateObj = array (
                  'state' => $state,
                  'displayName' => $displayName,
                  'timeStamp' => time(),
                  'context' => $context
                );

  $stateStr = json_encode($stateObj);
  $retval = $dbAccessor->setClusterState($clusterName, $stateStr);

  $logger->log_debug("Update Cluster State with ".$stateStr);

  return $retval;
}

function needWipeOut ($clusterName) {
  $logger = new HMCLogger("ClusterState");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);
  $clusterStatus = $dbAccessor->getClusterStatus($clusterName);
  return $clusterStatus;
}

?>
