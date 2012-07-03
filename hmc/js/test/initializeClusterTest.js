/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

// This is to aid development by jumping to a specified stage on page load
// so that the developer does not have to run through each page to see code
// changes made to the stage (assuming the developer has already run the wizard
// up to that stage and all the necessary data are in the database).
// This is a stop-gap measure until we implement actual state/history management
// so that a browser refresh reloads the current stage, rather than forcing
// the user back to the first stage.
(function() {  
  var clusterName = 'test';
  //var stage ='createCluster';
  //var stage = 'selectServices';
  var stage = 'configureServices';
  Y.one("#createClusterCoreDivId").hide();
  
  var hitCurrentStage = false;
  
  Y.all('#installationWizardProgressBarListId li').each(function(tab) {
    if (tab.get('id') === (stage + 'StageId')) {
      hitCurrentStage = true;
      tab.set('className', 'installationWizardCurrentStage');
    } else if (!hitCurrentStage) {
      tab.set('className', 'installationWizardVisitedStage');
    }
  });

  switch (stage) {
  case 'createCluster':
    globalYui.one("#createClusterCoreDivId").show();
    break;
  case 'selectServices':
    renderSelectServicesBlock({ "clusterName": clusterName, "txnId":1 });
    break;
  case 'assignMasters':
    // TODO
    break;
  case 'selectMountPoints':
    // TODO
    renderConfigureCluster({ "clusterName": clusterName});
    break;
  case 'configureServices':
    renderOptionsPage({ "clusterName": clusterName});
    break;
  default:
    break;
  }
})();
