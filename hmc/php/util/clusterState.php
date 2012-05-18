<?php

include_once '../php/util/Logger.php';
include_once '../php/conf/Config.inc';
include_once '../php/frontend/localDirs.php';
include_once "../php/util/lock.php";
include_once '../php/db/HMCDBAccessor.php';

// initial setup
$logger = new HMCLogger("sequentialScriptExecutor");
$dbHandle = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function needWipeOut ($clusterName) {
  global $logger, $dbHandle;
  $clusterStatus = $dbHandle->getClusterStatus($clusterName);
  return $clusterStatus;
}

?>
