<?php

include 'dbLib.php';
include 'utils.php';
include 'gsInstallerUtils.php';

// TODO: Hardcoded for now
$clusterName = $_GET['clusterName'];
//$clusterName = "MyCluster-12345";

$getAll = FALSE;
$lastN = 50;
if (isset($_GET["lines"])) {
  if ($_GET["lines"] == "all" ) {
    $getAll = TRUE;
  }
  else if (is_numeric($_GET["lines"])) {
    $lastN = $_GET["lines"];
  }
}

$output = "";
if ($getAll == TRUE) {
  $output = getGsInstallLogs($clusterName);
}
else {
  $output = getLastNLinesFromGsInstallLogs($clusterName, $lastN);
}

$gsInstallDone = isGsInstallDone($clusterName);

header("Content-type: application/json");

$response = array();

$installstatus = getGsInstallExitStatus($clusterName);

// NULL for error
// 0 for success
// 1 for error
// -1 for installer running
// -2 for not installed
if ($installstatus === NULL) {
   $response["installExitStatus"] = "UNKNOWN";
} else if ($installstatus === 0) {
   $response["installExitStatus"] = "SUCCESS";
} else if ($installstatus === 1) {
   $response["installExitStatus"] = "FAILURE";
} else if ($installstatus === -1) {
   $response["installExitStatus"] = "INSTALLING";
} else if ($installstatus === -2) {
   $response["installExitStatus"] = "NOTINSTALLED";
} else {
   $response["installExitStatus"] = "UNKNOWN";
}

if ($output === FALSE) {
   $response["error"] = TRUE;
   $response["logs"] = "";
}
else {
   $response["error"] = FALSE;
   $response["logs"] = $output;
}

$response["clusterName"] = $clusterName;
$response["gsInstallDone"] = $gsInstallDone;

print (json_encode($response));

/*
header("Content-type: text/plain");
print $output;
*/


?>
