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

function renderDeploySummary (deployInfo) {
  var deploySummary = "";
  
  var unit, unitClass, unitLabel;

  for (var serviceName in deployInfo.services) {
    var serviceHasToBeRendered = false;
    var masterSummary = "";
    var propertySummary = "";

    if (deployInfo.services.hasOwnProperty( serviceName )) {

      var perServiceInfo = deployInfo.services[serviceName];

      var configElementName = serviceName;
      var configElementIdName = configElementName + 'Id';

      if (perServiceInfo.isEnabled == false) {
          continue;
      }

      // Render all master components
      for (var componentIndex in perServiceInfo.components) {
        if (!perServiceInfo.components[componentIndex].isMaster) {
          continue;
        }
        var component = perServiceInfo.components[componentIndex];
        serviceHasToBeRendered = true;
        masterSummary += '<div class="formElement">' +
                           '<label for=' + component.componentName + 'Id>' + component.displayName + '&nbsp; : &nbsp;</label>' +
                           '<input type=text name=' + component.componentName + 'Name id=' + component.componentName + 'Id readonly=readonly value=\"' + component.hostNames.join(',') + '\">' +
                         '</div>';
      }

      for (var mPropertiesKey in perServiceInfo.properties) {
        if (perServiceInfo.properties[mPropertiesKey].type == "NODISPLAY") {
          continue;
        }
        serviceHasToBeRendered = true;

        readOnlyAttr = 'readonly=readonly';
        valueAttr = 'value=\"' + perServiceInfo.properties[mPropertiesKey].value + '\"';
        type = convertDisplayType(perServiceInfo.properties[mPropertiesKey].type);
        if (type == "checkbox") {
          readOnlyAttr = 'disabled="disabled"';
          var checkVal = perServiceInfo.properties[mPropertiesKey].value;
          if (checkVal == 'true') {
            valueAttr = 'checked=yes';
          } else {
            valueAttr = '';
          }
        }
        
        unit = perServiceInfo.properties[mPropertiesKey].unit;
        unitClass = (unit != null) ? 'unit' : '';
        unitLabel = (unit != null && unit != 'int') ? unit : '';        
        
        propertySummary += '<div class="formElement">' +
                             '<label for=' + mPropertiesKey  + 'Id>' + perServiceInfo.properties[mPropertiesKey].displayName + '</label>' +
                             '<input class="' + unitClass + '" type=' + type + ' name=' + mPropertiesKey + 'Name id=' + mPropertiesKey + 'Id ' + readOnlyAttr + ' ' + valueAttr + '>' +
                             '<label class="unit">' + unitLabel + '</label>' +
                           '</div>';
      }
    }

    if (serviceHasToBeRendered) {
      deploySummary += '<fieldset>' + '<legend>' + perServiceInfo.displayName + '</legend>';
      deploySummary += masterSummary;
      deploySummary += propertySummary;
      deploySummary += '</fieldset><br/>';
    }
  }

  globalYui.log("Final HTML: " + globalYui.Lang.dump(deploySummary));

  globalYui.one("#deployDynamicRenderDivId").setContent( deploySummary );
  hideLoadingImg();
  globalYui.one("#deployCoreDivId").setStyle("display", "block");
}

var globalDeployInfo = null;

globalYui.one('#deploySubmitButtonId').on('click',function (e) {

    e.target.set('disabled', true);

    var deployRequestData = {};

    var url = "../php/frontend/deploy.php?clusterName="+globalDeployInfo.clusterName;
    var requestData = deployRequestData;
    var submitButton = e.target;
    var thisScreenId = "#deployCoreDivId";
    var nextScreenId = "#txnProgressCoreDivId";
    var nextScreenRenderFunction = renderDeployProgress;
    submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
});

function renderDeploy (deployInfo) {
  globalDeployInfo = deployInfo;
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + deployInfo.clusterName + "&getConfigs=true&getComponents=true";
  executeStage(inputUrl, renderDeploySummary);
}
