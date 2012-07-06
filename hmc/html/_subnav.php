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
?>
<?php
switch ($requestPage) {
case 'html':
case 'index.php':
  $activeTab = 'SUMMARY';
  break;
case 'manageServices.php':
  $activeTab = 'SERVICES';
  break;
case 'addNodesWizard.php':
  $activeTab = 'ADD_NODES';
  break;
case 'uninstallWizard.php':
  $activeTab = 'UNINSTALL';
  break;
default:
  $activeTab = '';
}
?>
<div id="subnav">
  <ul class="nav nav-tabs">
    <li class="<?php echo ($activeTab == 'SUMMARY') ? 'active' : '' ?>"><a id="subNavSummaryLink" href="index.php">Cluster Summary</a></li>
    <li class="<?php echo ($activeTab == 'SERVICES') ? 'active' : '' ?>"><a id="subNavServicesLink" href="manageServices.php?clusterName=<?php echo $clusterName ?>">Manage Services</a></li>
    <li class="<?php echo ($activeTab == 'ADD_NODES') ? 'active' : '' ?>"><a id="subAddNodesLink" href="addNodesWizard.php?clusterName=<?php echo $clusterName ?>">Add Nodes</a></li>
    <li class="<?php echo ($activeTab == 'UNINSTALL') ? 'active' : '' ?>"><a id="subNavUninstallLink" href="uninstallWizard.php?clusterName=<?php echo $clusterName ?>">Uninstall</a></li>
  </ul>
</div>
