/* Declarations of global data. */
var fetchClusterServicesPoller;
var clusterServices;

function performServiceManagement( action, serviceName, confirmationDataPanel ) {

  /* First, (temporarily) stop any further fetches. */
  fetchClusterServicesPoller.stop();

  var manageServicesRequestData = {
    action: action, 
    services: {}
  };

  if( action == "reconfigure" ) {
    manageServicesRequestData.services = generateUserOpts();
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
          destroyInformationalPanel(confirmationDataPanel);

          var manageServicesProgressStatusMessage = {

            success:
              '<p>' +
                'Yabba Dabba Doo! Manage services ' + 
                  '<a href="javascript:void(null)" id=closeManageServicesProgressWidgetLinkId>' + 
                    'even harder' +
                  '</a>' + 
                '?' +
              '</p>',

            failure: 
              '<p>' + 
                'Scooby Doo, where are you? Perhaps in the ' +
                  '<a href="javascript:void(null)" id=showManageServicesTxnLogsLinkId>Operation Logs</a>' +
                '?' +
              '</p>'
          };

          var manageServicesProgressPostCompletionFixup = {

            success: function( txnProgressWidget ) {

              /* Register a click-handler for the just-rendered 
               * #closeManageServicesProgressWidgetLinkId.
               *
               * Don't worry about this being a double-registration - although
               * it looks that way, it's not, because (an identical, but that's 
               * irrelevant, really) manageServicesProgressStatusMessage.success 
               * is re-rendered afresh each time through, and thus this 
               * click-handler must also be re-registered each time 'round.
               */
              globalYui.one("#closeManageServicesProgressWidgetLinkId").on( "click", function(e) {

                txnProgressWidget.hide();
              });

              /* Resume polling for information about the cluster's services. */
              fetchClusterServicesPoller.start();
            },

            failure: function( txnProgressWidget ) {

              /* <-------------------- REZXXX BEGIN -----------------------> */

              /* Create the panel that'll display our error info. */
              var errorInfoPanel = 
                createInformationalPanel( '#informationalPanelContainerDivId', 'Operation Logs' );

              /* Prime the panel to start off showing our stock loading image. */
              var errorInfoPanelBodyContent = 
                '<img id=errorInfoPanelLoadingImgId class=loadingImg src=../images/loading.gif />';

              /* Make the call to our backend to fetch the report for this txnId. */
              globalYui.io('../php/frontend/fetchTxnLogs.php?clusterName=' + 
                txnProgressWidget.txnProgressContext.clusterName + '&txnId=' + txnProgressWidget.txnProgressContext.txnId, {
                  
                timeout: 10000,
                on: {
                  success: function (x,o) {

                    globalYui.log("RAW JSON DATA: " + o.responseText);

                    var errorInfoJson = null;

                    // Process the JSON data returned from the server
                    try {
                      errorInfoJson = globalYui.JSON.parse(o.responseText);
                    }
                    catch (e) {
                      alert("JSON Parse failed!");
                      return;
                    }

                    /* TODO XXX Remove some of the noise from this to allow
                     * for better corelation - for now, just dump a 
                     * pretty-printed version of the returned JSON.
                     */
                    errorInfoPanelBodyContent = 
                      '<pre>' + 
                        globalYui.JSON.stringify( errorInfoJson.logs, null, 4 ) +
                      '</pre>';

                    /* Update the contents of errorInfoPanel (which was, till
                     * now, showing the loading image). 
                     */
                    errorInfoPanel.set( 'bodyContent', errorInfoPanelBodyContent );
                  },
                  failure: function (x,o) {
                    alert("Async call failed!");
                  }
                }
              });

              var firstTimeShowingErrorInfoPanel = true;

              /* Register a click-handler for #showManageServicesTxnLogsLinkId 
               * to render the contents inside errorInfoPanel (and make it visible). 
               */
              globalYui.one("#showManageServicesTxnLogsLinkId").on( "click", function(e) {

                errorInfoPanel.set( 'bodyContent', errorInfoPanelBodyContent );
                errorInfoPanel.show();

                if( firstTimeShowingErrorInfoPanel ) {

                  globalYui.one('#txnProgressStatusActionsDivId').setContent(  
                    '<a href="javascript:void(null)" id=closeManageServicesProgressWidgetLinkId>' + 
                      'Close' +
                    '</a>' );

                  globalYui.one("#closeManageServicesProgressWidgetLinkId").on( "click", function(e) {

                    txnProgressWidget.hide();
                  });

                  firstTimeShowingErrorInfoPanel = false;
                }
              });

              /* <--------------------- REZXXX END ------------------------> */

              /* Resume polling for information about the cluster's services. */
              /* TODO XXX Move this into the click handler for closing the widget after the first show of the panel... */
              fetchClusterServicesPoller.start();
            }
          };

          var manageServicesProgressWidget = new TxnProgressWidget
            ( manageServicesResponseJson, manageServicesProgressStatusMessage, manageServicesProgressPostCompletionFixup );

          /* And now that confirmationDataPanel is hidden, show manageServicesProgressWidget. */
          manageServicesProgressWidget.show();
        }
        else {
          /* No need to hide confirmationDataPanel here - there are errors 
           * that need to be handled. 
           */
          handleConfigureServiceErrors( manageServicesResponseJson );
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

  var affectedServices;

  var confirmationDataPanelTitle = '';
  var confirmationDataPanelBodyContent = '';
  var confirmationDataPanelWidth = 800;
  var confirmationDataPanelHeight = 400;

  var confirmationDataPanel;

  var confirmationDataPanelNoButton = {
    value: 'Cancel',
    action: function (e) {
      e.preventDefault();
      destroyInformationalPanel(confirmationDataPanel);
    },
    section: 'footer'
  };

  var confirmationDataPanelYesPanel = {
    value: 'OK',
    action: function (e) {
      e.preventDefault();
      performServiceManagement( action, serviceName, confirmationDataPanel );
    },
    classNames: 'yo',
    section: 'footer'
  };

  if( action == 'reconfigure' ) {
    confirmationDataPanelTitle = 'Reconfigure ' + serviceName + ' (will stop and start given service and services that depend on it)';
    confirmationDataPanelBodyContent = 
      "<img id=errorInfoPanelLoadingImgId class=loadingImg src=../images/loading.gif />";

    executeStage( '../php/frontend/fetchClusterServices.php?clusterName=' + clusterName + 
      '&getConfigs=true&serviceName=' + serviceName, function (serviceConfigurationData) {

      var serviceConfigurationMarkup = constructDOM( serviceConfigurationData );

      if( globalYui.Lang.trim( serviceConfigurationMarkup).length == 0 ) {
        serviceConfigurationMarkup = '<p>Move along folks, nothing to see here...</p>';
      }
      else {
        /* Augment confirmationDataPanel with the relevant buttons only if there 
         * is something of value to show. 
         */
        confirmationDataPanel.addButton( confirmationDataPanelNoButton );
        confirmationDataPanel.addButton( confirmationDataPanelYesPanel );
      }

      /* XXX Note that this must be kept in-sync with the corresponding markup
       * on the InstallationWizard page.
       */
      confirmationDataPanelBodyContent = 
        '<div id=formStatusDivId class=formStatusBar style="visibility:hidden">'+
          'Placeholder' +
        '</div>' +
        '<br/>' +
        '<div id=configureClusterAdvancedCoreDivId>' + 
          '<form id=configureClusterAdvancedFormId>' +
            '<fieldset id=configureClusterAdvancedFieldSetId>' +
              '<div id=configureClusterAdvancedDynamicRenderDivId>' +
                serviceConfigurationMarkup +
              '</div>' +
            '</fieldset>' +
          '</form>' +
        '</div>';

      confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent );
    });

    confirmationDataPanelWidth = 1000;
    confirmationDataPanelHeight = 500;
  }
  else if( action == 'start' ) {
    var serviceDisplayName = clusterServices[serviceName].displayName;
    confirmationDataPanelTitle = 'Starting ' + serviceDisplayName;
    confirmationDataPanelBodyContent = "We are now going to start " + serviceDisplayName + "..<br/><br/>";
    affectedServices = clusterServices[serviceName].dependencies;
  }
  else if( action == 'stop' ) {
    var serviceDisplayName = clusterServices[serviceName].displayName;
    confirmationDataPanelTitle = 'Stopping ' + serviceDisplayName;
    confirmationDataPanelBodyContent = "We are now going to stop " + serviceDisplayName + "..<br/><br/>";
    affectedServices = clusterServices[serviceName].dependents;
  }
  else if( action == 'startAll' ) {
    confirmationDataPanelTitle = 'Start All Services';
    confirmationDataPanelBodyContent = "We are now going to start all services in the cluster";
  }
  else if( action == 'stopAll' ) {
    confirmationDataPanelTitle = 'Stop All Services';
    confirmationDataPanelBodyContent = "We are now going to stop all the services in the cluster";
  }

  if(action =='start' || action == 'stop') {
    var dependencyMarkup = "";
    for (affectedSrvc in affectedServices) {
      if (clusterServices[affectedServices[affectedSrvc]].attributes.runnable) {
        dependencyMarkup += '<tr><td>' + clusterServices[affectedServices[affectedSrvc]].displayName + '</td><td>' + titleCase(clusterServices[affectedServices[affectedSrvc]].state) + '</td></tr>';
      }
    }
    if (dependencyMarkup != '') {
      // Add this service at the top of the list
      dependencyMarkup = '<table><thead><th>Service name</th><th>Current state</th></thead><tr><td>' + serviceDisplayName + '</td><td>' + titleCase(clusterServices[serviceName].state) + '</td></tr>' + dependencyMarkup + '</table>';
      confirmationDataPanelBodyContent += 'Including this service and all its recursive dependencies, the following is the list of services that we will ' + action + ':' +
        '<br/>' +
        '<div id="manageServicesDisplayDepsOnAction">' +
        dependencyMarkup +
        '</div>';
    }
  }

  confirmationDataPanelBodyContent = '<div id="confirmationDataPanelBodyContent">' + confirmationDataPanelBodyContent + '</div>';

  /* Create the panel that'll display our confirmation/data dialog. */
  confirmationDataPanel = 
    createInformationalPanel( '#informationalPanelContainerDivId', confirmationDataPanelTitle );

  confirmationDataPanel.set( 'height', confirmationDataPanelHeight );
  confirmationDataPanel.set( 'width', confirmationDataPanelWidth );
  confirmationDataPanel.set( 'bodyContent', confirmationDataPanelBodyContent );

  /* Augment confirmationDataPanel with the relevant buttons unconditionally in 
   * the case of all actions other than "reconfigure" - for "reconfigure", we have 
   * special logic above. 
   */
  if( action != 'reconfigure' ) {
    confirmationDataPanel.addButton( confirmationDataPanelNoButton );
    confirmationDataPanel.addButton( confirmationDataPanelYesPanel );
  }

  confirmationDataPanel.show();
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
        '<div>' + 
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
              
      generatedServiceManagementEntryMarkup += 
            '<a href="javascript:void(null)" name="start" title="Start" ' +
               'class="btn serviceManagementEntryAction serviceManagementEntryActionStart"><i class="icon-play"></i></a>' +
            '<a href="javascript:void(null)" name="stop" title="Stop" ' +
               'class="btn serviceManagementEntryAction serviceManagementEntryActionStop"><i class="icon-stop"></i></a>';
    }

    generatedServiceManagementEntryMarkup += 
            '<a href="javascript:void(null)" name="reconfigure" title="Reconfigure" ' +
               'class="btn serviceManagementEntryAction serviceManagementEntryActionReconfigure"><i class="icon-cog"></i></a>' +
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
    serviceManagementMarkup += '<div class="serviceManagementGroup"> Client-only software: <br/>';
    for (var serviceName in clusterServices) {
      var serviceInfo = clusterServices[serviceName];
      if (clusterServices.hasOwnProperty(serviceName) && !serviceInfo.attributes.runnable) {
        serviceManagementMarkup += generateServiceManagementEntryMarkup( serviceName, serviceInfo );
      }
    }
    serviceManagementMarkup += '</div>';

    // Real services with server side components
    serviceManagementMarkup += '<div class="serviceManagementGroup"> Long running services: <br/><ul>';
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
