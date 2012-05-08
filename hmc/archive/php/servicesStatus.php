<?php

include 'dbLib.php';
include 'utils.php';
include 'gsInstallerUtils.php';

// TODO: Hardcoded for now
$clusterName = $_GET['clusterName'];
//$clusterName = "MyCluster-12345";
// Validate clusterName: TODO; FIXME
//$clusterDir = getClusterDir($clusterName);
$clusterInfo = getPersistedClusterInfo($clusterName);
$servicesInfo = $clusterInfo["services"];


$SERVICE_CHECKS_INSTALLDIR="/var/lib/gsInstaller/service_checks/";
$NN_SCRIPT="check_namenode_up.sh";
$HBM_SCRIPT="check_hbasemaster_up.sh";
$HCAT_SCRIPT="check_hcatserver_up.sh";
$JT_SCRIPT="check_jobttracker_up.sh";
$OOZIE_SCRIPT="check_oozieserver_up.sh";
$TEMPLETON_SCRIPT="check_templeton_up.sh";
$ZK_SCRIPT="check_zookeeper_cluster_up.sh";


function convertStatusNumToString($num) {
  $ret = "UNKNOWN ERROR";
  switch ($num) {
    case 0:
      $ret = "RUNNING";
      break;
    case 1:
      $ret = "RUNNING IN DEGRADED MODE";
      break;
    case 2:
      $ret = "NOT RUNNING";
      break;
    case 3:
      $ret = "UNKNOWN ERROR";
      break;
    case 4:
      $ret = "NOT INSTALLED";
      break;
    case 5:
      $ret = "INSTALLATION IN PROGRESS";
      break;
  }
  return $ret;
}

// 0 ok - running
// 1 warn
// 2 crit
// 3 unknown
// 4 uninstalled
// 5 install in progress
function getSvcStatus($svc, $role, $script) {
  global $servicesInfo;
  global $clusterName;
  if (!isset($servicesInfo[$svc]["enabled"]) 
      || $servicesInfo[$svc]["enabled"] == false) {
    return 4;
  }
  
  if (hasInstallStarted($clusterName) && !isGsInstallDone($clusterName)) {
    return 5;
  }

  $host = $servicesInfo[$svc]["masters"][$role]["hostName"];

  global $SERVICE_CHECKS_INSTALLDIR;
  $script_path=$SERVICE_CHECKS_INSTALLDIR.$script;
  if (!file_exists($script_path)) {
    error_log("Could not find check script $script_path for $svc : $role");
    return 3;
  }
  $cmd=$script_path." --host $host";
  $retval = 3;
  //system($cmd, $retval);

  $handle = popen($cmd, "r");
  // TODO error check
  $output = "";
  while (!feof($handle)) {
    $read = fread($handle, 2096);
    $output .= $read;
  }
  pclose($handle);
 
  error_log("Ouput for $svc check: $output");

  if ($output == "") {
     $retval = 3;
  } else if (strpos($output, "OK:") === 0) {
     $retval = 0;
  } else if (strpos($output, "WARNING:") === 0) {
     $retval = 1;
  } else if (strpos($output, "CRITICAL:") === 0) {
     $retval = 2;
  }

  return $retval;
}

function getHdfsStatus() {
  global $NN_SCRIPT;
  return getSvcStatus("hdfs", "nameNode", $NN_SCRIPT);
}

function getMapRedStatus() {
  global $JT_SCRIPT;
  return getSvcStatus("mapReduce", "jobTracker", $JT_SCRIPT);
}


function getHBaseStatus() {
  global $HBM_SCRIPT;
  return getSvcStatus("hBase", "hBaseMaster", $HBM_SCRIPT);
}

function getHCatStatus() {
  global $HCAT_SCRIPT;
  return getSvcStatus("hCatalog", "hCatServer", $HCAT_SCRIPT);
}


/*
function getZKStatus() {
  global $ZK_SCRIPT;
  return getSvcStatus("zooKeeper", 
}
*/

function getOozieStatus() {
  global $OOZIE_SCRIPT;
  return getSvcStatus("oozie", "oozieServer", $OOZIE_SCRIPT);
}

function getTempletonStatus() {
  global $TEMPLETON_SCRIPT;
  return getSvcStatus("templeton", "templetonNode", $TEMPLETON_SCRIPT);
}

$serviceStatus = array(
    $servicesInfo[$HDFSName]["displayName"] => array(
                           "description" => "The Hadoop distributed file-system",
                           "statusNum" => getHdfsStatus(),
                        ),
    $servicesInfo[$MapredName]["displayName"] => array(
                           "description" => "The Hadoop MapReduce processing platform",
                           "statusNum" => getMapRedStatus(),
                        ),
    $servicesInfo[$HBaseName]["displayName"] => array(
                           "description" => "The Hadoop Database",
                           "statusNum" => getHBaseStatus(),
                        ),
    $servicesInfo[$HCatName]["displayName"] => array(
                           "description" => "The Hadoop Catalog",
                           "statusNum" => getHcatStatus(),
                        ),
    $servicesInfo[$TempletonName]["displayName"] => array(
                           "description" => "Hadoop web-services",
                           "statusNum" => getTempletonStatus(),
                        ),
    $servicesInfo[$OozieName]["displayName"] => array(
                           "description" => "Hadoop job-workflow system",
                           "statusNum" => getOozieStatus(),
                        ),
  );

foreach ($serviceStatus as $key=>$status) {
  $serviceStatus[$key]["status"] = convertStatusNumToString($serviceStatus[$key]["statusNum"]);
}

$response = array();
$response["serviceStatus"] = $serviceStatus;

header("Content-type: application/json");
print(json_encode($response));

?>
