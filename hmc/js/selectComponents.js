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

InstallationWizard.SelectServicesForNewNodes = {
  renderData: {},
  render: function (deployAddedNodesData) {
    InstallationWizard.SelectServicesForNewNodes.renderData = deployAddedNodesData;
    getServiceComponentListAndRender(clusterName);
  }
};

function getServiceComponentListAndRender(clusterName) {
  globalYui.io("../php/frontend/fetchClusterServices.php?clusterName=" + clusterName + "&getComponents=true", {
  method: 'GET',
  timeout: 10000,
  on: {
    success: function (x,o) {
    globalYui.log("RAW JSON DATA: " + o.responseText);
    // Process the JSON data returned from the server
    try {
      responseJson = globalYui.JSON.parse(o.responseText);
    } 
    catch (e) {
      alert("JSON Parse failed!");
      return;
    }

    globalYui.log("PARSED DATA: " + globalYui.Lang.dump(responseJson));

    if (responseJson.result != 0) {
      // Error!
      alert("Got error during fetching services !" + responseJson.error); 
      return;
    }
    data = responseJson.response;

    var divContent = '<ul>';
    // for each service that is enabled, find all the slave components 
    // and display them. 
    // At this phase, we do not worry about dependencies because these are
    // just slaves and each slave is independent of the other
    for (serviceName in data['services'])  {
      if (data['services'][serviceName]['isEnabled'] != "0" && data['services'][serviceName]['attributes']['noDisplay'] == false) {
        component = data['services'][serviceName]['components'];
        for (componentIndex in component) {
          if (component[componentIndex]['isMaster'] == null && 
              (component[componentIndex]['isClient'] == null)) {
            continue;
          }
          if (!component[componentIndex]['isMaster'] && 
              !component[componentIndex]['isClient'] &&
              component[componentIndex].componentName != 'HIVE_MYSQL') {
            divContent += '<li class="selectServicesEntry" name=try>';
            divContent +=   '<label class="checkbox" for="install' + serviceName + 'Id">' 
                        +   '<input type="checkbox" disabled="disabled" checked="yes" name="' + serviceName + '" id="install' + serviceName + 'Id" value="' + component[componentIndex].componentName + '"/>'
                        + component[componentIndex].displayName + '</label>'
                        +   '<div class="contextualhelp">' + component[componentIndex].description + '</div>'
                        + '</li>';
          } else {
            continue;
          }
        }
      } else {
        continue;
      }
    }

    divContent += '</ul>';

    globalYui.one("#selectComponentsDynamicRenderDivId").setContent(divContent);
    globalYui.one('#selectServicesCoreDivId').setStyle("display", "block");
    },
   
    failure: function (x,o) {
      alert("Async call failed!");
    }
  }
  });

}

function getSelectedComponents () {
  var desiredComponents = [];
  
  var selections = 
    globalYui.all("#selectComponentsDynamicRenderDivId input[type=checkbox]");
    selections.each(function(selection) {
      if (selection.get('checked') == true) {
        desiredComponents.push(selection.get('value'));
      }
    });

    return desiredComponents;
}

function renderSelectServicesBlock( selectServicesInfo ) {
  InstallationWizard.SelectServicesForNewNodes.render(selectServicesInfo);
}

globalYui.one('#deployAddedNodesSubmitButtonId').on('click',function (e) {

    e.target.set('disabled', true);

    var deployRequestData = getSelectedComponents();

    var url = "../php/frontend/deployAddedNodes.php?clusterName=" + InstallationWizard.SelectServicesForNewNodes.renderData.clusterName;
    var requestData = deployRequestData;
    var submitButton = e.target;
    var thisScreenId = "#selectServicesCoreDivId";
    var nextScreenId = "#txnProgressCoreDivId";
    var nextScreenRenderFunction = renderDeployAddedNodesProgress;
    submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
});
