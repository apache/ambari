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

function renderDeploySummary(deployInfo) {

  var deploySummary = App.ui.configureServicesUtil.getDeploySummaryMarkup(deployInfo);

  Y.log("Final HTML: " + Y.Lang.dump(deploySummary));

  Y.one("#deployDynamicRenderDivId").setContent( deploySummary );
  App.ui.hideLoadingOverlay();
  Y.one("#deployCoreDivId").show();
}

Y.one('#deploySubmitButtonId').on('click',function (e) {

    e.target.set('disabled', true);

    var deployRequestData = {};

    var url = "../php/frontend/deploy.php?clusterName=" + App.props.clusterName;
    var requestData = deployRequestData;
    var submitButton = e.target;
    var thisScreenId = "#deployCoreDivId";
    var nextScreenId = "#txnProgressCoreDivId";
    var nextScreenRenderFunction = renderDeployProgress;
    App.transition.submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
});

function renderDeploy (deployInfo) {
  App.props.clusterName = deployInfo.clusterName
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + App.props.clusterName + "&getConfigs=true&getComponents=true";
  App.transition.executeStage(inputUrl, renderDeploySummary);
}
