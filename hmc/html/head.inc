<?php
require_once '../php/conf/MessageResources-en.inc';
require_once '../php/util/Logger.php';
require_once '../php//conf/Config.inc';
require_once "../php/util/lock.php";
require_once '../php/db/HMCDBAccessor.php';
require_once "../php/util/clusterState.php";

$logger = new HMCLogger("Interceptor");
$db = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$res = $db->getAllClusters();
$clusters = $res['clusters'];
$baseUrl = basename(preg_replace('/\?.*/', '', $_SERVER['REQUEST_URI']));
$logger->log_trace('basename='.$baseUrl);
$appDir = "/hmc/html";

$logger->log_trace('basename='.$baseUrl);
if (sizeof($clusters) == 0) {
  if ($baseUrl != 'welcome.php' && $baseUrl != 'initializeCluster.php') {
    header("Location: $appDir/welcome.php");
    return;
  }
} else {
  foreach ($clusters as $cluster) {
    $state = json_decode($cluster['state'], true);    
    $logger->log_trace('cluster state='.print_r($state,1));
    switch ($state['state']) {
      case 'DEPLOYED':
        if ($state['context']['status']) {
          return;
        } else {
          if ($baseUrl != "reinstall.php" && $baseUrl != "uninstallWizard.php") {
            header("Location: $appDir/reinstall.php");
            return;
          }
        }
        break;
      case 'CONFIGURATION_IN_PROGRESS':
        if ($baseUrl != 'initializeCluster.php') {
          header("Location: $appDir/initializeCluster.php");
        }
        break;      
      case 'DEPLOYMENT_IN_PROGRESS':
        if ($baseUrl != 'showDeployProgress.php') {
          header("Location: $appDir/showDeployProgress.php?clusterName=" . $cluster['clusterName']);          
        }
        break;
    } 
  }  
}
?>