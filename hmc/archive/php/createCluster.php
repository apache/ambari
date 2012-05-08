<?php
include 'dbLib.php';

/*
function initCluster($clusterName) {
  $clusterDir = getClusterDir($clusterName);
  // TODO: FIXME: All die calls below
  if (!is_dir($clusterDir) && !mkdir($clusterDir, 0700, true)) {
        die('Failed to create directory...');
  }
  $dbDir = getClusterDBDir($clusterName);
  if (!is_dir($dbDir) && !mkdir($dbDir, 0700, true)) {
        die('Failed to create db-directory...');
  }
  $installerInputDir = getInstallerInputDir($clusterName);
  if (!is_dir($installerInputDir) && !mkdir($installerInputDir, 0700, true)) {
        die('Failed to create installer directory...');
  }
}
*/

// Read from the input
$requestdata = file_get_contents('php://input');
//error_log($requestdata, 3, "/tmp/rez.log");
$requestObj = json_decode($requestdata, true);

$clusterName = $requestObj["clusterName"];
// Validate clusterName: TODO; FIXME
initCluster($clusterName);

$output = array( "clusterName" => $clusterName );

header("Content-type: application/json");
print (json_encode($output));
?>
