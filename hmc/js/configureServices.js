var globalOptionsInfo = null;
           
Y.one('#configureClusterAdvancedSubmitButtonId').on('click',function (e) {
  
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
  Y.one('#configureClusterAdvancedSubmitButtonId').simulate('click');  
  hideLoadingImg();
}

function renderOptionsPage (optionsInfo) {
  globalOptionsInfo = optionsInfo;
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + optionsInfo.clusterName + "&getConfigs=true";
  executeStage(inputUrl, renderConfigureServicesInternal);
}
