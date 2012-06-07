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
        for (componentName in component) {
          if (component[componentName]['isMaster'] == null && 
              (component[componentName]['isClient'] == null)) {
            continue;
          }
          if (!component[componentName]['isMaster'] && 
                !component[componentName]['isClient']) {
            divContent += '<li class="selectServicesEntry" name=try>';
            divContent +=   '<label class="checkbox" for="install' + serviceName + 'Id">' 
                        +   '<input type="checkbox" disabled="disabled" checked="yes" name="' + serviceName + '" id="install' + serviceName + 'Id" value="' + component[componentName]["componentName"] + '"/>'
                        + component[componentName]['displayName'] + '</label>'
                        +   '<div class="contextualhelp">' + component[componentName]['description'] + '</div>'
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
    var deployAddedNodes = new DeployAddedNodes();
    
    globalYui.io("../php/frontend/deployAddedNodes.php?clusterName="+ InstallationWizard.SelectServicesForNewNodes.renderData.clusterName, {
      method: 'POST',
      data: globalYui.JSON.stringify(deployRequestData),
      timeout : 10000,
      on: {
        start: function(x, o) {
          e.target.set('disabled', false);
          showLoadingImg();
        },
        complete: function(x, o) {
          e.target.set('disabled', false);
          hideLoadingImg();
        },
        success: function (x,o) {
          globalYui.log("RAW DEPLOY DATA: " + o.responseText);

          try {
            deployProgressInfoJson = globalYui.JSON.parse(o.responseText);
          } catch (e) {
            alert("JSON Parse failed!");
            return;
          }

          globalYui.log("PARSED DATA: " + globalYui.Lang.dump(deployProgressInfoJson));

          /* Done with this stage, transition to the next. */
          transitionToNextStage( "#selectServicesCoreDivId", deployRequestData, "#txnProgressCoreDivId", deployProgressInfoJson, deployAddedNodes.renderProgress);

          /* At this point, our users are done with the installation wizard
          * and have asked for a deploy, so there's no going back - remove
          * all traces of #installationWizardProgressBarDivId.
          */
          globalYui.one('#installationWizardProgressBarDivId').setStyle('display', 'none');
        },
      failure: function (x,o) {
        e.target.set('disabled', false);
        alert("Async call failed!");
      }
    }
  });
});
