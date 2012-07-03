<?php
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/

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

/* If bypassRouter parameter is 1, don't do any routing */

$bypassRouter = (isset($_GET['bypassRouter']) && $_GET['bypassRouter']);

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
    $clusterName = $cluster['clusterName'];
    $state = json_decode($cluster['state'], true);    
    $logger->log_trace('cluster state='.print_r($state,1));
    switch ($state['state']) {
      case 'NOT_CONFIGURED':
        if ($requestPage != 'welcome.php' && 
            $requestPage != 'initializeCluster.php') {
          eval(redirectToPage($requestPage, 'welcome.php'));
          exit;
        }
        $clusterState = 'NOT_CONFIGURED';
        break;
      case 'DEPLOYED':
        if ($state['context']['status']) {
          if (!$bypassRouter && $requestPage == 'initializeCluster.php') {
            eval(redirectToPage($requestPage, 'index.php'));
            exit;
          }
          $clusterState = 'OPERATIONAL';
        } else {
          if (!$bypassRouter && $requestPage != 'installFailed.php' && $requestPage != 'uninstallWizard.php') {
            eval(redirectToPage($requestPage, 'installFailed.php'));
            exit; 
          }
          $clusterState = 'DEPLOY_FAILED';            
        }
        break;
      case 'CONFIGURATION_IN_PROGRESS':
        if (!$bypassRouter && $requestPage != 'welcome.php' && $requestPage != 'initializeCluster.php') {
          eval(redirectToPage($requestPage, 'welcome.php'));
          exit;
        }
        $clusterState = 'CONFIGURATION_IN_PROGRESS';
        break;      
      case 'DEPLOYMENT_IN_PROGRESS':
        if (!$bypassRouter && $requestPage != 'showDeployProgress.php') {
          eval(redirectToPage($requestPage, 'showDeployProgress.php'));
          exit;
        }     
        $clusterState = 'DEPLOYMENT_IN_PROGRESS';
        break;
      case 'NODE_ADDITION_IN_PROGRESS':
        if (!$bypassRouter && $requestPage != 'showDeployAddedNodesProgress.php') {
          eval(redirectToPage($requestPage, 'showDeployAddedNodesProgress.php'));
          exit;
        }     
        $clusterState = 'NODE_ADDITION_IN_PROGRESS';
        break;
      case 'SERVICE_MANAGEMENT_IN_PROGRESS':
        if (!$bypassRouter && $requestPage != 'showManageServicesProgress.php') {
          eval(redirectToPage($requestPage, 'showManageServicesProgress.php'));
          exit;
        }     
        $clusterState = 'SERVICE_MANAGEMENT_IN_PROGRESS';
        break;        
      case 'UNINSTALLATION_IN_PROGRESS':
        if (!$bypassRouter && $requestPage != 'showUninstallProgress.php') {
          eval(redirectToPage($requestPage, 'showUninstallProgress.php'));
          exit;
        }     
        $clusterState = 'UNINSTALLATION_IN_PROGRESS';
        break;
      case 'UNINSTALLED':
        if (!$bypassRouter && $requestPage != 'uninstallFailed.php') {
          eval(redirectToPage($requestPage, 'uninstallFailed.php'));
          exit;
        }
        $clusterState = 'UNINSTALL_FAILED';
        break;        
    } 
  }  
}
?>
