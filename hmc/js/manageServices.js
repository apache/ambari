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

/* Declarations of global data. */
var fetchClusterServicesPoller;
var clusterServices;

// Storing globally for the sake of multiple screens in reconfigure
var localReconfigureServiceData = {};
var remoteReconfigureServiceData = {};
var confirmationDataPanelBodyContent = '';

var confirmationDataPanel;

var panelNoButton = {
  value: 'Cancel',
  action: function (e) {
    e.preventDefault();
    hideAndDestroyPanel();
  },
  section: 'footer'
};

var panelYesButton;

// Only one service can be reconfigured at a time.
var reconfigLevelOneYesButton;
var reconfigLevelTwoNoButton;

function showPanel() {
  showPanel(function() {});
}

function showPanel(postShowFn) {
  confirmationDataPanel.set('y', 200);
  confirmationDataPanel.set('x', (globalYui.one('body').get('region').width - confirmationDataPanel.get('width'))/2);
  confirmationDataPanel.show();
  if (postShowFn != null) {
    postShowFn.call();
  }
}

function hidePanel(postHideFn) {
  if (postHideFn != null) {
    postHideFn.call();
  }
}

function hideAndDestroyPanel() {
  hidePanel(function() {
    confirmationDataPanel.hide();
    destroyInformationalPanel(confirmationDataPanel);
  });
}

function getTitleForReconfiguration(serviceName) {
  return 'Make Configuration Changes for ' + serviceName;
}

function setupReconfigureFirstScreen(serviceName) {
  var panelTitle = getTitleForReconfiguration(serviceName);
  confirmationDataPanel.set( 'headerContent', panelTitle);
  confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent);
  // Remove buttons from previous stage
  confirmationDataPanel.removeButton(0);
  confirmationDataPanel.removeButton(0);
  confirmationDataPanel.addButton( panelNoButton );
  confirmationDataPanel.addButton( reconfigLevelOneYesButton );
}

function setupReconfigureSecondScreen(serviceName) {
  var affectedServices = clusterServices[serviceName].dependencies;
  var dependents = clusterServices[serviceName].dependents;
  for (dep in dependents) {
    affectedServices.push(dependents[dep]);
  }
  var panelContent = 'Affected services:' + getAffectedDependenciesMarkup(affectedServices, serviceName, 'reconfigure');
  var panelTitle = 'Review changes to ' + serviceName + '\'s configuration';
  confirmationDataPanel.set( 'headerContent', panelTitle);
  confirmationDataPanel.set( 'bodyContent', panelContent);
  // Remove buttons from previous stage
  confirmationDataPanel.removeButton(0);
  confirmationDataPanel.removeButton(0);
  confirmationDataPanel.addButton( reconfigLevelTwoNoButton );
  confirmationDataPanel.addButton( panelYesButton );
}

// Clean up the affected-services list to only include appropriate installed long-running services
function getAffectedDependenciesMarkup(affectedServices, serviceName, action) {

  var affectedDependenciesMarkup = '';

  var serviceDisplayName = clusterServices[serviceName].displayName;

  var deps = affectedServices;
  affectedServices = [];
  for (dep in deps) {
    var svc = deps[dep];
    if (clusterServices.hasOwnProperty(svc) && (clusterServices[svc].isEnabled == 1) && clusterServices[svc].attributes.runnable ) {
      affectedServices.push(svc);
    }
  }

  var dependencyMarkup = "";
  for (affectedSrvc in affectedServices) {
    if (clusterServices[affectedServices[affectedSrvc]].attributes.runnable) {
      dependencyMarkup += '<tr><td>' + clusterServices[affectedServices[affectedSrvc]].displayName + '</td><td>' + titleCase(clusterServices[affectedServices[affectedSrvc]].state) + '</td></tr>';
    }
  }
  if (dependencyMarkup != '') {
    // Add this service at the top of the list
    dependencyMarkup = '<table><thead><th>Service name</th><th>Current state</th></thead><tr><td>' + serviceDisplayName + '</td><td>' + titleCase(clusterServices[serviceName].state) + '</td></tr>' + dependencyMarkup + '</table>';
    affectedDependenciesMarkup += 'Including this service and all its recursive dependencies, the following is the list of services that will be affected by ' + action + ' of ' + serviceName + ' :' +
      '<br/>' +
      '<div id="manageServicesDisplayDepsOnAction">' +
      dependencyMarkup +
      '</div>';
  }
  return affectedDependenciesMarkup;
}

function setupStartServiceScreen(serviceName) {
  setupStartStopServiceScreen('start', serviceName);
}

function setupStopServiceScreen(serviceName) {
  setupStartStopServiceScreen('stop', serviceName);
}

function setupStartStopServiceScreen(action, serviceName) {

  var serviceDisplayName = clusterServices[serviceName].displayName;
  var affectedServices;
  var confirmationDataPanelTitle;

  if ( action == 'start') {
    confirmationDataPanelTitle = 'Starting ' + serviceDisplayName;
    confirmationDataPanelBodyContent = "We are now going to start " + serviceDisplayName + "...<br/><br/>";
    affectedServices = clusterServices[serviceName].dependencies;
  } else if (action == 'stop') {
    confirmationDataPanelTitle = 'Stopping ' + serviceDisplayName;
    confirmationDataPanelBodyContent = "We are now going to stop " + serviceDisplayName + "...<br/><br/>";
    affectedServices = clusterServices[serviceName].dependents;
  }

  confirmationDataPanelBodyContent += getAffectedDependenciesMarkup(affectedServices, serviceName, action);
  confirmationDataPanelBodyContent = '<div id="confirmationDataPanelBodyContent">' + confirmationDataPanelBodyContent + '</div>';

  confirmationDataPanel.set( 'headerContent', confirmationDataPanelTitle);
  confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent);
  confirmationDataPanel.set( 'height', 400);
  confirmationDataPanel.set( 'width', 800);

  confirmationDataPanel.addButton( panelNoButton);
  confirmationDataPanel.addButton( panelYesButton );

  showPanel();
}

function setupStartAllServicesScreen() {
  setupStartStopAllServicesScreen('startAll');
}

function setupStopAllServicesScreen() {
  setupStartStopAllServicesScreen('stopAll');
}

function setupStartStopAllServicesScreen(action) {
  var confirmationDataPanelTitle;
  var confirmationDataPanelBodyContent;

  if ( action == 'startAll' ) {
    confirmationDataPanelTitle = 'Start All Services';
    confirmationDataPanelBodyContent = "We are now going to start all services in the cluster";
  } else if ( action == 'stopAll' ) {
    confirmationDataPanelTitle = 'Stop All Services';
    confirmationDataPanelBodyContent = "We are now going to stop all the services in the cluster";
  }

  confirmationDataPanel.set( 'headerContent', confirmationDataPanelTitle);
  confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent);
  confirmationDataPanel.set( 'height', 400);
  confirmationDataPanel.set( 'width', 800);

  confirmationDataPanel.addButton( panelNoButton);
  confirmationDataPanel.addButton( panelYesButton );
  showPanel();
}

function setupReconfigureScreens(serviceName) {
  // TODO: Needed for others too?
  /* First, (temporarily) stop any further fetches. */
  fetchClusterServicesPoller.stop();

  reconfigLevelOneYesButton = {
    value: 'Apply Changes',
    action: function (e) {
      e.preventDefault();

      localReconfigureServiceData = configureServicesUtil.generateUserOpts();
      var remoteProps = remoteReconfigureServiceData.services[serviceName].properties;
      var localProps = localReconfigureServiceData[serviceName].properties;
      var allEqual = true;
      for (key in localProps) {
        var remoteValue = remoteProps[key].value;
        var localValue = localProps[key]["value"];
        if ( localValue != remoteValue) {
          allEqual = false;
        }
      }
      if (allEqual) {
        alert("You haven't made any changes");
        return;
      }
      hidePanel(function() {

        // Store the requestData and the html
        confirmationDataPanelBodyContent = confirmationDataPanel.get( 'bodyContent' );
        setupReconfigureSecondScreen(serviceName);
        showPanel();
      });
    },
    classNames: 'okButton',
    section: 'footer'
  };

  reconfigLevelTwoNoButton = {
    value: 'Go back and re-edit',
    action: function (e) {
      e.preventDefault();

      hidePanel(function() {
        setupReconfigureFirstScreen(serviceName);
        showPanel();
      });
    },
    section: 'footer'
  };

  // Render first with a loading image and then get config items
  confirmationDataPanelBodyContent = 
    "<img id=errorInfoPanelLoadingImgId class=loadingImg src=../images/loading.gif />";
  var confirmationDataPanelTitle = getTitleForReconfiguration(serviceName);
  confirmationDataPanel.set( 'height', 500);
  confirmationDataPanel.set( 'width', 1000);
  confirmationDataPanel.set( 'headerContent', confirmationDataPanelTitle);
  confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent );
  showPanel();

  executeStage( '../php/frontend/fetchClusterServices.php?clusterName=' + clusterName + 
    '&getConfigs=true&serviceName=' + serviceName, function (serviceConfigurationData) {

    // Store the remote data
    remoteReconfigureServiceData = serviceConfigurationData;

    var serviceConfigurationMarkup = configureServicesUtil.getOptionsSummaryMarkup(serviceConfigurationData, true);

    if( globalYui.Lang.trim( serviceConfigurationMarkup).length == 0 ) {
      serviceConfigurationMarkup = '<p>There is nothing to reconfigure for this service.</p>';
    }
    else {
      /* Augment confirmationDataPanel with the relevant buttons only if there 
       * is something of value to show. 
       */
      confirmationDataPanel.addButton( panelNoButton );
      confirmationDataPanel.addButton( reconfigLevelOneYesButton );
    }

    /* XXX Note that this must be kept in-sync with the corresponding markup
     * on the InstallationWizard page.
     */
    confirmationDataPanelBodyContent = 
      '<div id=formStatusDivId class=formStatusBar style="display:none">'+
        'Placeholder' +
      '</div>' +
      '<div id=configureClusterAdvancedCoreDivId>' + 
        '<form id=configureClusterAdvancedFormId>' +
          '<fieldset id=configureClusterAdvancedFieldSetId>' +
            '<div id=configureClusterAdvancedDynamicRenderDivId>' +
              serviceConfigurationMarkup +
            '</div>' +
          '</fieldset>' +
        '</form>' +
      '</div>';

    confirmationDataPanelBodyContent = '<div id="confirmationDataPanelBodyContent">' + confirmationDataPanelBodyContent + '</div>';

    confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent );
  });
}

function performServiceManagement( action, serviceName, confirmationDataPanel ) {

  /* First, (temporarily) stop any further fetches. */
  fetchClusterServicesPoller.stop();

  var manageServicesRequestData = {
    action: action, 
    services: {}
  };

  if( action == "reconfigure" ) {
    manageServicesRequestData.services = localReconfigureServiceData;
  }
  else {
    /* Need to explicitly set a key named for serviceName this way because it's
     * a variable - in the future, the value will be a filled-out array (for
     * now, we only support managing a single service at a time). 
     */
    manageServicesRequestData.services[serviceName] = {};
  }

  globalYui.io( "../php/frontend/manageServices.php?clusterName=" + clusterName, {
    method: 'POST',
    data: globalYui.JSON.stringify(manageServicesRequestData),
    timeout: 10000,
    on: {
      success: function(x, o) {

        globalYui.log("RAW JSON DATA: " + o.responseText);

        var manageServicesResponseJson;

        try {
          manageServicesResponseJson = globalYui.JSON.parse(o.responseText);
        }
        catch (e) {
          alert("JSON Parse failed!");
          return;
        }

        globalYui.log(globalYui.Lang.dump(manageServicesResponseJson));

        /* Check that manageServicesResponseJson actually indicates success. */
        if( manageServicesResponseJson.result == 0 ) {

          /* Only on success should we destroy confirmationDataPanel - on 
           * failure, we depend on the fact that there'll be errors shown 
           * inside the panel that the user will want/need to interact with.
           */
          hideAndDestroyPanel();

          renderManageServicesProgress( manageServicesResponseJson.response );
        }
        else {
          /* No need to hide confirmationDataPanel here - there are errors 
           * that need to be handled. 
           */
          if (action == 'reconfigure') {

            hidePanel(function() {
              setupReconfigureFirstScreen(serviceName);
              showPanel( function() {
                configureServicesUtil.handleConfigureServiceErrors( manageServicesResponseJson );
            });
          });
          } else {
            // Can't do anything for others
            alert('Got error during ' + action + ' : ' + globalYui.Lang.dump(manageServicesResponseJson));
          }
        }
      },
      failure: function(x, o) {
        alert("Async call failed!");
      }
    }
  });
}

function getServiceConfigurationMarkup( serviceConfigurationData ) {

  return serviceConfigurationMarkup;
}

function serviceManagementActionClickHandler( action, serviceName ) {

  // Reinit the global content
  confirmationDataPanelBodyContent = '';

  var confirmationDataPanelTitle = ''; // Set title later

  /* Create the panel that'll display our confirmation/data dialog. */
  confirmationDataPanel = 
      createInformationalPanel( '#informationalPanelContainerDivId', confirmationDataPanelTitle );

  panelYesButton = {
    value: 'OK',
    action: function (e) {
      e.preventDefault();
      performServiceManagement( action, serviceName, confirmationDataPanel );
    },
    classNames: 'okButton',
    section: 'footer'
  };

  if ( action == 'start') {
    setupStartServiceScreen(serviceName);
  } else if ( action == 'stop') {
    setupStopServiceScreen(serviceName);
  } else if( action == 'startAll' ) {
    setupStartAllServicesScreen();
  } else if( action == 'stopAll' ) {
    setupStopAllServicesScreen();
  } else if( action == 'reconfigure' ) {
    setupReconfigureScreens(serviceName);
  }
}

function deduceServiceManagementEntryCssClass( serviceInfo ) {

  var serviceManagementEntryCssClass = '';

  var serviceState = serviceInfo.state;

  if( serviceState.match(/^stop/i) || serviceState.match(/^fail/i) ) {
    serviceManagementEntryCssClass = "serviceManagementEntryStopped";
  }
  else if( serviceState.match(/^start/i) ) {
    serviceManagementEntryCssClass = "serviceManagementEntryStarted";
  }
  else if( serviceState.match(/^install/i) ) {
    serviceManagementEntryCssClass = "serviceManagementEntryInstalled";
  }
  else if( serviceState.match(/^uninstall/i) ) {
    serviceManagementEntryCssClass = "serviceManagementEntryUninstalled";
  }

//  globalYui.log( "Picking CSS class for" + serviceInfo.serviceName + ": " + serviceManagementEntryCssClass );

  return serviceManagementEntryCssClass;
}

function generateServiceManagementEntryMarkup( serviceName, serviceInfo ) {
  
  var generatedServiceManagementEntryMarkup = '';

  var serviceAttributes = serviceInfo.attributes;

  /* Only generate a Service Management entry for services that are:
   *
   * a) enabled
   * b) runnable 
   * c) meant to be displayed
   */
  if( (serviceInfo.isEnabled == true) && !serviceAttributes.noDisplay ) {

    var serviceManagementEntryCssClass = deduceServiceManagementEntryCssClass( serviceInfo );

    generatedServiceManagementEntryMarkup += 

      '<li class="serviceManagementEntry '+ serviceManagementEntryCssClass + '">' + 
        '<div id="serviceManagementFor' + serviceName + '">' + 
          '<span class="serviceManagementEntryNameContainer">' +
            '<a href="javascript:void(null)" name="' + serviceName + '" class="serviceManagementEntryName">' + 
              serviceInfo.displayName +
            '</a>' +
          '</span>' +
          '<div class="serviceManagementEntryStateContainer">' +
            titleCase(serviceInfo.state) +
          '</div>' +
          '<div class="serviceManagementEntryActionsContainer">';

    if( serviceAttributes.runnable ) {

      var serviceManagementEntryAnchorName = '';
      var serviceManagementEntryAnchorTitle = '';
      var serviceManagementEntryAnchorCssClasses = 'serviceManagementEntryAction btn '; 
      var serviceManagementEntryIconCssClass = '';

      /* Already-started/stopped services shouldn't allow a start/stop operation on them. */
      if( serviceInfo.state == 'STOPPED' || serviceInfo.state == 'FAILED') {
        serviceManagementEntryAnchorName = 'start';
        serviceManagementEntryAnchorTitle = 'Start';
        serviceManagementEntryAnchorCssClasses += 'serviceManagementEntryActionStart';
        serviceManagementEntryIconCssClass = 'iconic-play';
      } 
      else if ( serviceInfo.state == 'STARTED' ) {
        serviceManagementEntryAnchorName = 'stop';
        serviceManagementEntryAnchorTitle = 'Stop';
        serviceManagementEntryAnchorCssClasses += 'serviceManagementEntryActionStop';
        serviceManagementEntryIconCssClass = 'iconic-stop';
      }
      else if ( serviceInfo.state == 'STOPPING') {
        serviceManagementEntryAnchorName = 'start';
        serviceManagementEntryAnchorTitle = 'Start';
        serviceManagementEntryAnchorCssClasses += 'serviceManagementEntryActionStart disabled';
        serviceManagementEntryIconCssClass = 'iconic-start disabled';
      }
      else if ( serviceInfo.state == 'STARTING') {
        serviceManagementEntryAnchorName = 'stop';
        serviceManagementEntryAnchorTitle = 'Stop';
        serviceManagementEntryAnchorCssClasses += 'serviceManagementEntryActionStop disabled';
        serviceManagementEntryIconCssClass = 'iconic-stop disabled';
      }
      
      generatedServiceManagementEntryMarkup += 
        '<a href="javascript:void(null)" name="' + serviceManagementEntryAnchorName + '" ' +
        'title="' + serviceManagementEntryAnchorTitle + '" ' +
        'class="' + serviceManagementEntryAnchorCssClasses + '"><i class="' + serviceManagementEntryIconCssClass + '"></i></a> ';
    }

    var notReconfigurable = [ 'PIG', 'SQOOP', 'OOZIE', 'TEMPLETON', 'GANGLIA', 'HIVE' ];
    var reconfigureClass;
    if (globalYui.Array.indexOf(notReconfigurable, serviceName) >= 0) {
      reconfigureClass = 'serviceManagementEntryActionReconfigure disabled';
    } else {
      reconfigureClass = 'serviceManagementEntryActionReconfigure';
    }
  
    generatedServiceManagementEntryMarkup += '<a href="javascript:void(null)" name="reconfigure" title="Reconfigure" ' +
              'class="btn serviceManagementEntryAction ' + reconfigureClass + '"><i class="iconic-cog"></i></a>' +
            '</div>' +
          '</div>' +
        '</li>';
  }

  return generatedServiceManagementEntryMarkup;
}

// Do Not Remove --> We'll uncomment this section when the Service names link to something meaningful.
//
// /* Register click handlers for the service links themselves. */
// globalYui.one('#serviceManagementListId').delegate('click', function (e) {
//     alert(this.getAttribute('name'));
// }, 'li.serviceManagementEntry span.serviceManagementEntryNameContainer a.serviceManagementEntryName' );

/* Register click handlers for the global-action buttons. */
globalYui.one('#serviceManagementGlobalActionsDivId').delegate('click', function (e) {
    var action = this.getAttribute('name');

    serviceManagementActionClickHandler( action );
}, 'button' );

/* Register click handlers for the action links for each service. */
globalYui.one('#serviceManagementListId').delegate('click', function (e) {
    var action = this.getAttribute('name');
    var serviceName = this.ancestor('li.serviceManagementEntry').
                    one('span.serviceManagementEntryNameContainer a.serviceManagementEntryName').getAttribute('name');

    serviceManagementActionClickHandler( action, serviceName );
}, 'li.serviceManagementEntry div.serviceManagementEntryActionsContainer a.serviceManagementEntryAction' );

/* Main() */

/* The clusterName variable is set in the Javascript scaffolding spit out by manageServices.php */
var fetchClusterServicesPollerContext = {
  source: '../php/frontend/fetchClusterServices.php',
  schema: {
    metaFields: {
      services: 'response.services'
    }
  },
  request: '?clusterName=' + clusterName,
  /* TODO XXX Change this from 5 seconds to 1 minute. */
  pollInterval: 5000,
  maxFailedAttempts: 5
};

var fetchClusterServicesPollerResponseHandler = {
  success: function (e, pdp) {
    /* Clear the screen of the loading image (in case it's currently showing). */
    hideLoadingImg();

    /* The data from our backend. */
    clusterServices = e.response.meta.services;

    /* What we're here to render. */
    var serviceManagementMarkup = '';

    // Separate block for client-only software
    /*
    var clientOnlySoftwareMarkup = '';
    for (var serviceName in clusterServices) {
      var serviceInfo = clusterServices[serviceName];
      if (clusterServices.hasOwnProperty(serviceName) && !serviceInfo.attributes.runnable) {
        clientOnlySoftwareMarkup += generateServiceManagementEntryMarkup( serviceName, serviceInfo );
      }
    }
    if (clientOnlySoftwareMarkup != '') {
      serviceManagementMarkup += '<div class="serviceManagementGroup"><h2>Client-only software</h2><ul>';
      serviceManagementMarkup += clientOnlySoftwareMarkup;
      serviceManagementMarkup += '</div>';
    }
    */

    // Real services with server side components
    serviceManagementMarkup += '<div class="serviceManagementGroup" style="margin-top:30px"><ul>';
    for (var serviceName in clusterServices) {
      var serviceInfo = clusterServices[serviceName];
      if (clusterServices.hasOwnProperty(serviceName) && serviceInfo.attributes.runnable) {
        serviceManagementMarkup += generateServiceManagementEntryMarkup( serviceName, serviceInfo );
      }
    }
    serviceManagementMarkup += '</ul></div>';

    /* Link the newly-generated serviceManagementMarkup into the DOM. */
    globalYui.one("#serviceManagementDynamicRenderDivId").setContent( serviceManagementMarkup );

    /* If serviceManagementMarkup is non-empty, unveil the contents of 
     * #serviceManagementGlobalActionsDivId (which contains the StartAll 
     * and StopAll buttons) as well.
     */
    if( globalYui.Lang.trim( serviceManagementMarkup ).length > 0 ) {
      globalYui.one("#serviceManagementGlobalActionsDivId").setStyle( 'display', 'block' );
    }
  },

  failure: function (e, pdp) {
    /* Clear the screen of the loading image (in case it's currently showing). */
    hideLoadingImg();

    alert('Failed to fetch cluster services!');
  }
};

fetchClusterServicesPoller = new PeriodicDataPoller
  ( fetchClusterServicesPollerContext, fetchClusterServicesPollerResponseHandler );

/* Kick the polling loop off. */
fetchClusterServicesPoller.start();
