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

var globalOptionsInfo = null;

function validate() {
  var opts = configureServicesUtil.generateUserOpts();

  $.ajax({
    type: 'POST',
    url: '../php/frontend/configureServices.php?clusterName=' + globalOptionsInfo.clusterName + "&validateOnly=1",
    data: JSON.stringify(opts),
    dataType: 'json',
    timeout: 10000,
    success: function (data) {
      if (data.result != 0) {
        configureServicesUtil.handleConfigureServiceErrors(data);
      }
    },
    failure: function (data) {
      alert('Async failed');
    }
  });
}

Y.one('#configureClusterAdvancedSubmitButtonId').on('click',function (e) {
  
  if (this.hasClass('disabled')) {
    return;
  }
  
  var opts = configureServicesUtil.generateUserOpts();

  e.target.set('disabled', true);
  var url = "../php/frontend/configureServices.php?clusterName="+globalOptionsInfo.clusterName;
  var requestData = opts;
  var submitButton = e.target;
  var thisScreenId = "#configureClusterAdvancedCoreDivId";
  var nextScreenId = "#deployCoreDivId";
  var nextScreenRenderFunction = renderDeploy;
  var errorFunction = configureServicesUtil.handleConfigureServiceErrors;
  submitDataAndProgressToNextScreen(url, requestData, submitButton,
      thisScreenId, nextScreenId, nextScreenRenderFunction, errorFunction);
});

// register event handlers for dynamic validation
// when a key is pressed on a password field, perform password validation
Y.one("#configureClusterAdvancedDynamicRenderDivId").delegate(
  {
    'keyup' : function (e) {
      configureServicesUtil.checkPasswordCorrectness();
      configureServicesUtil.updateServiceErrorCount(e.target.get('name'));
    }
  },
  "input[type=password]"
);
// when a key is pressed on a text field, just clear the error
Y.one("#configureClusterAdvancedDynamicRenderDivId").delegate(
  {
    'keyup' : function (e) {
      configureServicesUtil.clearErrorReason('#' + e.target.get('id'));
      configureServicesUtil.updateServiceErrorCount(e.target.get('name'));
    }
  },
  "input[type=text],input[type=password]:not(.retypePassword)"
);

function renderConfigureServicesInternal (optionsInfo) {
  Y.one("#configureClusterAdvancedDynamicRenderDivId").setContent(configureServicesUtil.getOptionsSummaryMarkup(optionsInfo, false));
  $('#configureServicesTabs a:first').tab('show');
  Y.one("#configureClusterAdvancedCoreDivId").show();
  validate();
  hideLoadingImg();
}

function renderOptionsPage (optionsInfo) {
  globalOptionsInfo = optionsInfo;
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + optionsInfo.clusterName + "&getConfigs=true";
  executeStage(inputUrl, renderConfigureServicesInternal);
}
