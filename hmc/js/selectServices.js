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

InstallationWizard.SelectServices = {

  renderData: {},

  render:
    function (selectServicesInfo) {

      InstallationWizard.SelectServices.renderData = selectServicesInfo;

    }
};

var data;
var allBoxesSelected = true;

function renderServiceList(responseJson) {
  data = responseJson;
  var divContent = '';
  var coreContent = '';
  var optionalContent = '';
  var nonSelectableContent = '';
  for (serviceName in data['services'])  {
    data['services'][serviceName]['refCount'] = 0;
    if (data['services'][serviceName]['reverseDependencies'] == null) {
      data['services'][serviceName]['reverseDependencies'] = new Array();
    }
    for (var i = 0; i < data['services'][serviceName]['dependencies'].length; i++) {
      svcDep = data['services'][serviceName]['dependencies'][i];
      if (data['services'][svcDep]['reverseDependencies'] == null) {
        data['services'][svcDep]['reverseDependencies'] = new Array();
      }
      var found = false;
      for (var j = 0; j < data['services'][svcDep]['reverseDependencies'].length; j++) {
        if (data['services'][svcDep]['reverseDependencies'][j] == serviceName) {
          found = true;
          break;
        }
      }
      if (!found) {
        data['services'][svcDep]['reverseDependencies'].push(serviceName);
      }
    }

    // globalYui.log("Handling service : " + serviceName);
    var content = '';
    content = generateSelectServiceCheckbox(data['services'][serviceName]);

    if (data['services'][serviceName].attributes.mustInstall) {
      coreContent += content;
    } else {
      if (data['services'][serviceName].attributes.editable) {
        optionalContent += content;
      }
      else {
        nonSelectableContent += content;
      }
    } 

  } 

  //            divContent += coreContent + optionalContent + nonSelectableContent;
   coreContent = '<div>' +
                  '<label class="checkbox" for="selectAllCheckBoxId"><span id="labelForSelectAllId">Select all</span>' +
                  '<input type="checkbox" name="selectAll" id="selectAllCheckBoxId"/></label>' +
                '</div>' +
                coreContent;
  globalYui.one("#selectCoreServicesDynamicRenderDivId").setContent(coreContent);     
  globalYui.one("#selectOptionalServicesDynamicRenderDivId").setContent(optionalContent);
  globalYui.one("#selectNonSelectableServicesDynamicRenderDivId").setContent(nonSelectableContent);
  globalYui.one('#selectServicesCoreDivId').setStyle("display", "block");

  // For now, we want all services to be enabled by default
  selectDelectAll(true);
  displayStatusOnSelectDeselectAll(true, false);
  globalYui.one("#selectAllCheckBoxId").set('checked', true);
}

function generateSelectServiceCheckbox(serviceInfo) {

  var dContent = '<div class="formElement" name="' + serviceInfo.serviceName + '" id="'
      + 'selectServicesEntry' + serviceInfo.serviceName + 'DivId"';

  if (!serviceInfo.attributes.editable
      && !serviceInfo.attributes.mustInstall) {
    dContent += ' style="display:none" ';
  }
  dContent += '><label class="checkbox" for="install' + serviceInfo.serviceName + 'Id"'
      + '>' + serviceInfo.displayName
      + '<input type="checkbox" name="' + serviceInfo.serviceName + '"'
      + ' id="installService' + serviceInfo.serviceName + 'Id" value="install'
      + serviceInfo.serviceName + 'Value"';

  if (serviceInfo.attributes != null) {
     if (serviceInfo.attributes.noDisplay) {
        return '';
     }
     if (!serviceInfo.attributes.editable) {
       dContent += ' disabled="disabled"';
     }
     if (serviceInfo.attributes.mustInstall) {
       dContent += ' checked="yes"';
     }
  }
 
  dContent += '/> - <span class="description">' + serviceInfo['description'] + '</span></label>' +
          //'<div class="description">' + serviceInfo['description'] + '</div>' +
        '</div>';

  // globalYui.log("Handling service entry: " + dContent);
  return dContent;
}

function getButtonId(serviceName) {
  return 'installService' + serviceName + 'Id';
}

function setRefCounts(serviceName) {

  var buttonId = getButtonId(serviceName);

  // Set the refCount for 'this' service
  if (!globalYui.one('#' + buttonId).get('checked')) {
     data['services'][serviceName]['refCount'] = 0;
  } else if (data['services'][serviceName]['refCount'] == 0) {
     data['services'][serviceName]['refCount'] = 1;
  }

  // Set the refCounts for 'this' service's dependencies
  var selectYes = true;
  if (!globalYui.one('#' + buttonId).get('checked')) {
     selectYes = false;
  }
  for (var i = 0; i < data['services'][serviceName]['dependencies'].length; i++) {
     var serviceDep = data['services'][serviceName]['dependencies'][i];
     if (selectYes) {
        data['services'][serviceDep]['refCount']++;
     } else {
        data['services'][serviceDep]['refCount']--;
        if (data['services'][serviceDep]['refCount'] < 0) {
           data['services'][serviceDep]['refCount'] = 0;
        }
     }
  }
}

function displayStatusOnSuccess(serviceName) {

  var buttonId = getButtonId(serviceName);

  var selectYes = true;
  if (!globalYui.one('#' + buttonId).get('checked')) {
     selectYes = false;
  }

  var statusString = "Selected " + data['services'][serviceName].displayName + " for installation. ";
  if (!globalYui.one('#' + buttonId).get('checked')) {
     statusString = "Deselected " + data['services'][serviceName].displayName + " and all its dependencies.";
  }
  // Generate the status string for dependencies
  var dependencies = "";
  for (var i = 0; i < data['services'][serviceName]['dependencies'].length; i++) {
     var serviceDep = data['services'][serviceName]['dependencies'][i];
     if (selectYes) {
        if (!data['services'][serviceDep].attributes.mustInstall) {
          dependencies += data['services'][serviceDep].displayName + " ";
        }
     }
  }
  if(selectYes) {
    if(dependencies != "") {
      statusString += "Also added  " + dependencies + " as dependencies.";
    }
  }
  setFormStatus(statusString, false);
}

function updateRendering() {

  var currentAllBoxesSelected = true;

  for (svcName in data['services']) {

     if (data['services'][svcName].attributes.noDisplay) {
       continue;
     }

     // globalYui.log('Svc ref count : ' + svcName + ' : ' + data['services'][svcName]['refCount']);

     var itemId = getButtonId(svcName);
     if (data['services'][svcName].attributes.mustInstall ||
         data['services'][svcName]['refCount'] > 0) {
        globalYui.one('#' + itemId).set('checked' ,'yes');
        if (!data['services'][svcName].attributes.editable) {
           var divId = 'selectServicesEntry' + svcName + 'DivId';
           globalYui.one('#' + divId).setStyle('display', '');
        }
     } else {
        currentAllBoxesSelected = false;
        globalYui.one('#' + itemId).set('checked' ,'');
        if (!data['services'][svcName].attributes.editable) {
           var divId = 'selectServicesEntry' + svcName + 'DivId';
           globalYui.one('#' + divId).setStyle('display', 'none');
        }
     }
  }

  if (allBoxesSelected != currentAllBoxesSelected) {
    allBoxesSelected = currentAllBoxesSelected;
    // Update the selectAll button
    globalYui.one("#selectAllCheckBoxId").set('checked', allBoxesSelected);
    displayStatusOnSelectDeselectAll(allBoxesSelected, false);
  }
}

function displayStatusOnSelectDeselectAll(selectAll, setFormStatusAlso) {
  var labelNode = globalYui.one("#labelForSelectAllId");
  if (selectAll) {
    labelNode.setContent("Select all");
  } else {
    labelNode.setContent("Select all");
  }
  if (setFormStatusAlso) {
    if (selectAll) {
      setFormStatus("Selected all services", false);
  } else {
    setFormStatus("Deselected all optional services", false);
  }
  }
}

function selectDelectAll(selectAll) {
  var node = globalYui.one("#selectAllCheckBoxId");
  var labelNode = globalYui.one("#labelForSelectAllId");
  for (svcName in data['services']) {
    if (!data['services'][svcName].attributes.noDisplay && !data['services'][svcName].attributes.mustInstall && data['services'][svcName].attributes.editable) {
      var itemId = getButtonId(svcName);
      if ( selectAll != globalYui.one('#' + itemId).get('checked')) {
        globalYui.one('#' + itemId).set('checked' , selectAll);
        setRefCounts(svcName);
      }
    }
  }
  // All done, update our rendering
  updateRendering();
}

globalYui.one('#selectServicesCoreDivId').delegate('click', function (e) {

    // Select-all checkbox
    if (this.get('id') == 'selectAllCheckBoxId') {
      var node = globalYui.one("#selectAllCheckBoxId");
      var selectAll = node.get('checked');
      selectDelectAll(selectAll);
      displayStatusOnSelectDeselectAll(selectAll, true);
      return;
    }
    //// End of select-all checkbox

    // globalYui.log(globalYui.Lang.dump(this));
    var serviceName = this.getAttribute('name');
    var buttonId = getButtonId(serviceName);

    // Deselecting an already selected service
    if (!globalYui.one('#' + buttonId).get('checked')) {
      var invalidDep = false;
      var invalidDepReason = "";
      for (var i = 0; i < data['services'][serviceName]['reverseDependencies'].length; i++) {
        var nm = data['services'][serviceName]['reverseDependencies'][i];
        if (data['services'][nm]['refCount'] > 0) {
          invalidDep = true;
          invalidDepReason = "Cannot deselect: " + data['services'][serviceName].displayName + " is needed by " + data['services'][nm].displayName;
          break;
        }
      }
      if (invalidDep) {
        setFormStatus(invalidDepReason, true);
        globalYui.one('#' + buttonId).set('checked', 'yes');
        return;
      }

      // Some things are deselected, so update the selectAll button
      globalYui.one("#selectAllCheckBoxId").set('checked', false);
    }

    setRefCounts(serviceName);

    // Display status as to what we have done now.
    displayStatusOnSuccess(serviceName);

    // All done, update our rendering
    updateRendering();

//}, 'li.selectServicesEntry');
}, 'input[type=checkbox]');

globalYui.one('#selectServicesSubmitButtonId').on('click',function (e) {
    var selectServicesRequestData = {
        "services" : [ ] } ;
    for (svcName in data['services']) {
       /* if (data['services'][svcName].attributes.noDisplay) {
         continue;
       }*/
       var svcObj = { "serviceName" : svcName,
                      "isEnabled": (data['services'][svcName].attributes.mustInstall || data['services'][svcName]['refCount'] > 0) };
       selectServicesRequestData.services.push(svcObj);
    }

    // alert(globalYui.Lang.dump(selectServicesRequestData));

    var url = "../php/frontend/selectServices.php?clusterName=" + InstallationWizard.SelectServices.renderData.clusterName;
    var requestData = selectServicesRequestData;
    var submitButton = globalYui.one('#selectServicesSubmitButtonId');
    var thisScreenId = "#selectServicesCoreDivId";
    var nextScreenId  = "#assignHostsCoreDivId";
    var nextScreenRenderFunction = new AssignMasters().render;

    submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);

});

function renderSelectServicesBlock(infoInitializeCluster) {

  InstallationWizard.SelectServices.renderData = infoInitializeCluster;

  //////// Get the list of services and relevant information for rendering them.
  var clusterName = InstallationWizard.SelectServices.renderData.clusterName;
  var inputUrl = "../php/frontend/fetchClusterServices.php?clusterName=" + clusterName ;
  executeStage(inputUrl, renderServiceList);
}
