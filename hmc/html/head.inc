<?php
require_once '../php/conf/MessageResources-en.inc';
require_once '../php/util/Logger.php';
require_once '../php//conf/Config.inc';
require_once "../php/util/lock.php";
require_once '../php/db/HMCDBAccessor.php';
require_once "../php/util/clusterState.php";

/* 
 * due to the way include files are scoped,
 * we return the string to be eval'd, rather than
 * executing them.
 */
function redirectToPage($requestPage, $targetPage) {
  // if the page is index.php or the root app directory, don't redirect... simply include
  if ($requestPage != 'index.php' && $requestPage != 'html') {
    return "header('Location: /hmc/html/$targetPage');";
  } else {
    return "require('$targetPage');";
  }
}

$logger = new HMCLogger("Interceptor");
$db = new HMCDBAccessor($GLOBALS["DB_PATH"]);
$appDir = "/hmc/html";

$res = $db->getAllClusters();
$clusters = $res['clusters'];
$requestPage = basename(preg_replace('/\?.*/', '', $_SERVER['REQUEST_URI']));
$logger->log_trace('requestPage='.$requestPage);

if (sizeof($clusters) == 0) {
  if ($requestPage != 'welcome.php' && $requestPage != 'initializeCluster.php') {
    eval(redirectToPage($requestPage, 'welcome.php'));
    exit;
  }
  $clusterState = 'NOT_CONFIGURED';
} else {
  foreach ($clusters as $cluster) {
    $state = json_decode($cluster['state'], true);    
    $logger->log_trace('cluster state='.print_r($state,1));
    switch ($state['state']) {
      case 'DEPLOYED':
        if ($state['context']['status']) {
          $clusterState = 'OPERATIONAL';
        } else {
          if ($requestPage != 'installFailed.php' && $requestPage != 'uninstallWizard.php') {
            eval(redirectToPage($requestPage, 'installFailed.php'));
            exit; 
          }
          $clusterState = 'DEPLOY_FAILED';            
        }
        break;
      case 'CONFIGURATION_IN_PROGRESS':
        if ($requestPage != 'reinstall.php' && $requestPage != 'initializeCluster.php') {
          $targetPage = 'reinstall.php';
          eval(redirectToPage($requestPage, 'reinstall.php'));
          exit;
        }
        $clusterState = 'CONFIGURATION_IN_PROGRESS';
        break;      
      case 'DEPLOYMENT_IN_PROGRESS':
        if ($requestPage != 'showDeployProgress.php') {
          $targetPage = 'showDeployProgress.php?clusterName=' . $cluster['clusterName'];
          $_GET['clusterName'] = $cluster['clusterName'];
          eval(redirectToPage($requestPage, 'showDeployProgress.php'));
          exit;
        }          
        $clusterState = 'DEPLOYMENT_IN_PROGRESS';
        break;
    } 
  }  
}
?>