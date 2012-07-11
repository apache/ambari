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

// Do not cache pages.  Otherwise handling browser back/forward
// can cause problems due to stale content.
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Expires: Sat, 26 Jul 1997 05:00:00 GMT");

$BASE_DIR = dirname(__FILE__);
require_once $BASE_DIR.'/../php/conf/MessageResources-en.inc';
require_once $BASE_DIR.'/../php/util/Logger.php';
require_once $BASE_DIR.'/../php/conf/Config.inc';
require_once $BASE_DIR.'/../php/util/lock.php';
require_once $BASE_DIR.'/../php/db/HMCDBAccessor.php';
require_once $BASE_DIR.'/../php/util/clusterState.php';

/*
 * Due to the way include files are scoped,
 * we return the string to be eval'd, rather than
 * executing them.
 */
function redirectToPage($requestPage, $targetPage)
{
  $bypassRouter = $GLOBALS['BYPASS_ROUTER'] || (isset($_GET['bypassRouter']) && $_GET['bypassRouter']);
  if (!$bypassRouter) {
    // if the page is index.php or the root app directory, don't redirect... simply include
    if ($requestPage != 'index.php' && $requestPage != 'html') {
      return "header('Location: /hmc/html/$targetPage'); exit;";
    } else {
      return "require('$targetPage'); exit;";
    }
  } else {
    // do not redirect/forward.  fall through
    return "";
  }
}

$logger = new HMCLogger("Router");
$db = new HMCDBAccessor($GLOBALS["DB_PATH"]);
$appDir = "/hmc/html/";

$res = $db->getAllClusters();
$clusters = $res['clusters'];
$requestPage = str_replace($appDir, '', $_SERVER['SCRIPT_NAME']);
$logger->log_trace('requestPage=' . $requestPage);

if (sizeof($clusters) == 0) {
  if ($requestPage != 'welcome.php' && $requestPage != 'initializeCluster.php') {
    eval(redirectToPage($requestPage, 'welcome.php'));
  }
  $clusterState = 'NOT_CONFIGURED';
} else {
  foreach ($clusters as $cluster) {
    $clusterName = $cluster['clusterName'];
    $state = json_decode($cluster['state'], true);
    $logger->log_trace('cluster state=' . print_r($state, 1));
    switch ($state['state']) {
      case 'NOT_CONFIGURED':
        if ($requestPage != 'welcome.php' &&
          $requestPage != 'initializeCluster.php'
        ) {
          eval(redirectToPage($requestPage, 'welcome.php'));
        }
        $clusterState = 'NOT_CONFIGURED';
        break;
      case 'DEPLOYED':
        if ($state['context']['status']) {
          if ($requestPage == 'initializeCluster.php') {
            eval(redirectToPage($requestPage, 'index.php'));
          }
          $clusterState = 'OPERATIONAL';
        } else {
          if ($requestPage != 'installFailed.php' && $requestPage != 'uninstallWizard.php') {
            eval(redirectToPage($requestPage, 'installFailed.php'));
          }
          $clusterState = 'DEPLOY_FAILED';
        }
        break;
      case 'CONFIGURATION_IN_PROGRESS':
        if ($requestPage != 'welcome.php' && $requestPage != 'initializeCluster.php') {
          eval(redirectToPage($requestPage, 'welcome.php'));
        }
        $clusterState = 'CONFIGURATION_IN_PROGRESS';
        break;
      case 'DEPLOYMENT_IN_PROGRESS':
        if ($requestPage != 'showDeployProgress.php') {
          eval(redirectToPage($requestPage, 'showDeployProgress.php'));
        }
        $clusterState = 'DEPLOYMENT_IN_PROGRESS';
        break;
      case 'NODE_ADDITION_IN_PROGRESS':
        if ($requestPage != 'showDeployAddedNodesProgress.php') {
          eval(redirectToPage($requestPage, 'showDeployAddedNodesProgress.php'));
        }
        $clusterState = 'NODE_ADDITION_IN_PROGRESS';
        break;
      case 'SERVICE_MANAGEMENT_IN_PROGRESS':
        if ($requestPage != 'showManageServicesProgress.php') {
          eval(redirectToPage($requestPage, 'showManageServicesProgress.php'));
        }
        $clusterState = 'SERVICE_MANAGEMENT_IN_PROGRESS';
        break;
      case 'UNINSTALLATION_IN_PROGRESS':
        if ($requestPage != 'showUninstallProgress.php') {
          eval(redirectToPage($requestPage, 'showUninstallProgress.php'));
        }
        $clusterState = 'UNINSTALLATION_IN_PROGRESS';
        break;
      case 'UNINSTALLED':
        if ($requestPage != 'uninstallFailed.php') {
          eval(redirectToPage($requestPage, 'uninstallFailed.php'));
        }
        $clusterState = 'UNINSTALL_FAILED';
        break;
      case 'UPGRADE_STACK_UNINSTALL_IN_PROGRESS':
        if ($requestPage != 'upgradeStack/showUpgradeProgress.php') {
          eval(redirectToPage($requestPage, 'upgradeStack/showUpgradeProgress.php'));
        }
        $clusterState = 'UPGRADE_STACK_UNINSTALL_IN_PROGRESS';
        break;
      case 'UPGRADE_STACK_UNINSTALL_FAILED':
        if ($requestPage != 'upgradeStack/showUpgradeProgress.php') {
          eval(redirectToPage($requestPage, 'upgradeStack/showUpgradeProgress.php'));
        }
        $clusterState = 'UPGRADE_STACK_UNINSTALL_FAILED';
        break;
      case 'UPGRADE_STACK_UNINSTALLED':
        if ($requestPage != 'upgradeStack/showUpgradeProgress.php') {
          eval(redirectToPage($requestPage, 'upgradeStack/showUpgradeProgress.php'));
        }
        $clusterState = 'UPGRADE_STACK_UNINSTALLED';
        break;
      case 'UPGRADE_STACK_DEPLOY_IN_PROGRESS':
        if ($requestPage != 'upgradeStack/showUpgradeProgress.php') {
          eval(redirectToPage($requestPage, 'upgradeStack/showUpgradeProgress.php'));
        }
        $clusterState = 'UPGRADE_STACK_DEPLOY_IN_PROGRESS';
        break;
      case 'UPGRADE_STACK_DEPLOY_FAILED':
        if ($requestPage != 'upgradeStack/showUpgradeProgress.php') {
          eval(redirectToPage($requestPage, 'upgradeStack/showUpgradeProgress.php'));
        }
        $clusterState = 'UPGRADE_STACK_DEPLOY_FAILED';
        break;
    }
  }
}
?>
