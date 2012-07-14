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

var kerbConfirmDataPanel;
var panelOkButton = {
  value: 'DEPLOY',
  action: function (e) {
    e.preventDefault();
    App.ui.destroyInfoPanel(kerbConfirmDataPanel);
    deployServices();
  },
  classNames: 'okButton',
  section: 'footer'
};

function renderDeploySummary(deployInfo) {
  App.props.securityType = deployInfo.services.KERBEROS.properties.kerberos_install_type.value;
  var deploySummary = App.ui.configureServicesUtil.getDeploySummaryMarkup(deployInfo);
  Y.log("Final HTML: " + Y.Lang.dump(deploySummary));
  Y.one("#deployDynamicRenderDivId").setContent( deploySummary );
  App.ui.hideLoadingOverlay();
  Y.one("#deployCoreDivId").show();
  kerbConfirmDataPanel = App.ui.createInfoPanel("A note on kerberos settings:");
  var panelbody = '<div>'
    + '<h1>'
    + 'Keytab Path '
    + deployInfo.services.KERBEROS.properties.keytab_path.value
    + '</h1>';
  kerbConfirmDataPanel.set('bodyContent',panelbody);
  kerbConfirmDataPanel.addButton( panelOkButton );
}

function deployServices (){
  var deployRequestData = {};
  var url = "../php/frontend/deploy.php?clusterName=" + App.props.clusterName;
  var requestData = deployRequestData;
  var submitButton = Y.one('#deploySubmitButtonId');
  var thisScreenId = "#deployCoreDivId";
  var nextScreenId = "#txnProgressCoreDivId";
  var nextScreenRenderFunction = renderDeployProgress;
  App.transition.submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
}

function showKerbPanel(postShowFn, confirmationDataPanel) {
  confirmationDataPanel.set('y', 200);
  confirmationDataPanel.set('x', (globalYui.one('body').get('region').width - confirmationDataPanel.get('width')) / 2);
  confirmationDataPanel.show();
  if (postShowFn != null) {
    postShowFn.call();
  }
}

Y.one('#deploySubmitButtonId').on('click',function (e) {
  e.target.set('disabled', true);
  if (App.props.securityType === "USER_SET_KERBEROS") {
    showKerbPanel(function() {}, kerbConfirmDataPanel);
    e.target.set('disabled', false);
  } else {
    deployServices();
  }
});

function renderDeploy(deployInfo) {
  App.props.clusterName = deployInfo.clusterName;
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + App.props.clusterName + "&getConfigs=true&getComponents=true";
  App.transition.executeStage(inputUrl, renderDeploySummary);
}
