<?php

include 'dbLib.php';
include 'utils.php';
include 'gsInstallerUtils.php';

// TODO: Hardcoded for now
$clusterName = $_GET['clusterName'];
// $clusterName = "MyCluster-12345";

$serviceName = $_GET['Service'];
$action = $_GET['Action'];

$clusterInfo = getPersistedClusterInfo($clusterName);
$servicesInfo = $clusterInfo["services"];

function sendBadRequestResponse($msg) {
 header('Content-type: application/json');
 header("HTTP/1.0 400 Bad Request");
 $result = array();
 $result["error"] = $msg;
 $result["success"] = FALSE;
 print(json_encode($result));
}

function sendErrorResponse($msg) {
 header('Content-type: application/json');
 header("HTTP/1.0 500 Internal Server Error");
 $result = array();
 $result["error"] = $msg;
 $result["success"] = FALSE;
 print(json_encode($result));
}

function returnSuccessResponse() {
  header('Content-type: application/json');
  header("HTTP/1.1 200 OK");
  print(json_encode(array("success" => TRUE)));
}

function takeServiceAction($svc, $actionSvc, $action) {
  global $servicesInfo;
  global $clusterName;
  if (!isset($servicesInfo[$svc]["enabled"])
      || $servicesInfo[$svc]["enabled"] == false) {
     return sendErrorResponse("Service $svc is not installed");
  }

  if (hasInstallStarted($clusterName) && !isGsInstallDone($clusterName)) {
    return sendErrorResponse("Service $svc installation is still in progress");
  }

  $workDir = getClusterDir($clusterName)."gsInstaller/";
  $cmd="/var/www/html/KickAssHDPUI/cpp/runscriptasroot \"cd $workDir; echo y | sh ./gsInstaller-cmdline.sh --action=$action --services=$actionSvc \"";

  $handle = popen($cmd, "r");
  // Do some checks on return status: TODO
  // print $cmd;
  // echo "'$handle'; " . gettype($handle) . "\n";
  // while (!feof($handle)) {
    // $read = fread($handle, 2096);
    // fwrite($loghandle, $read);
  // }
  pclose($handle);

  return returnSuccessResponse();

}

if ($action != "start" && $action != "stop") {
  return sendBadRequestResponse("Invalid action : $action");
}

if ($serviceName == $HDFSName) {
  takeServiceAction($serviceName, "hdfs", $action);
} else if ($serviceName == $MapredName) {
  takeServiceAction($serviceName, "mapreduce", $action);
} else if ($serviceName == $HBaseName) {
  takeServiceAction($serviceName, "hbase", $action);
} else if ($serviceName == $HCatName) {
  takeServiceAction($serviceName, "hcat", $action);
} else if ($serviceName == $TempletonName) {
  takeServiceAction($serviceName, "templeton", $action);
} else if ($serviceName == $OozieName) {
  takeServiceAction($serviceName, "oozie", $action);
} else {
  return sendBadRequestResponse("Invalid service : $serviceName");
}


?>
