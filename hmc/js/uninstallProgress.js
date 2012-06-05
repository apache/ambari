function generateLogsContent(errorInfoJson) {
/*
  content = '<div id=\"ProgressLogsContainer\">'
  for (i=0; i < errorInfoJson['progress'].length; i++) {
    var subTxnId = errorInfoJson['progress'][i]['subTxnId'];
    var state = errorInfoJson['progress'][i]['state'];
    var desc = errorInfoJson['progress'][i]['description'];

    var stateClass = 'ProgressLogsSubTxnState' + state;

    var subTxnDiv = '<div id=\"'
      + 'ProgressLogsSubTxnContainer' + subTxnId + 'Id\"'
      + ' class=\"ProgressLogsSubTxnContainer ' + stateClass + ' \" name=\"' + desc + '\">';

    for (hostName in errorInfoJson['logs'][subTxnId]['nodeLogs']) {
      var nodeReport = errorInfoJson['logs'][subTxnId]['nodeLogs'][hostName];
      var hostState = nodeReport['overall'];
      var hostStateClass = 'ProgressLogsSubTxnNodeState' + hostState;
      var reportContainer = '<div id=\"ProgressLogsSubTxnNodeContainer' + subTxnId + hostName + 'Id\"'
        + ' class=\"ProgressLogsSubTxnNodeContainer ' + hostStateClass + '\"' + ' name=\"' + hostName + '\">'
        + '<div class=\"ProgressLogsSubTxnNodeContainerLogs\">'
        + globalYui.JSON.stringify(nodeReport.message)
        + '</div>'
        + '</div>' + '<br/>'
      subTxnDiv += reportContainer;
    }
    subTxnDiv += '</div>' + '<br/>';
    content += subTxnDiv;
  }
  content += '</div>';

  return content;
*/
  return '<pre>' +
           globalYui.JSON.stringify( errorInfoJson.logs, null, 4 ) +
         '</pre>';

}

function generateClustersListUrl( clusterName ) {

  var url = '';

  var currentUrl = window.location.href;
  globalYui.log('Current URL: ' + currentUrl);
  var currentPathPos = currentUrl.indexOf(window.location.pathname);
  globalYui.log('Current Path Pos: ' + currentPathPos);

  if( -1 != currentPathPos ) {

    /*
    url = currentUrl.substr(0, currentPathPos) + 
      '/hmc/html/manageServices.php?clusterName=' + clusterName;
  globalYui.log('Services Page URL: ' + url);
      */
    // ClusterName is no longer needed
    url = currentUrl.substr(0, currentPathPos) + '/hmc/html/index.php';
  }

  return url;
}

function renderUninstallProgress (uninstallProgressInfo) {

  hideLoadingImg();

  var hmcRestartMsg = '';
  if (uninstallProgressInfo.nagiosGangliaCoHosted != null
      && uninstallProgressInfo.nagiosGangliaCoHosted) {
    hmcRestartMsg = '<strong>Note:</strong> We detected that you need to restart HMC as'
        + ' Nagios/Ganglia are co-hosted on this server. <br/>Please restart'
        + ' HMC using \"service hmc restart\" and then head ';
  } else {
    hmcRestartMsg = 'May we be so bold as to suggest heading ';
  }

  hmcRestartMsg += 'on over to the ' +
      '<a href="javascript:void(null)" id=clustersListLinkId>' +
        'Cluster information' +
      '</a>' +
      ' page?';

  var uninstallProgressStatusMessage = {

    success:
      '<p style=\"text-align:center\">' +
        'All done with the uninstall! <br/>' + hmcRestartMsg +
      '</p>',
    failure:
      '<p>' +
        'We made a boo-boo! Take a look at the ' +
          '<a href="javascript:void(null)" id=showUninstallTxnLogsLinkId>Uninstall Logs</a>' +
        '?' +
      '</p>'
  };

  var uninstallProgressPostCompletionFixup = {

    success: function( txnProgressWidget ) {

      globalYui.one("#clustersListLinkId").on( "click", function(e) {
        window.open( generateClustersListUrl(txnProgressWidget.txnProgressContext.clusterName) );
      });
    },

    failure: function( txnProgressWidget ) {

      /* Create the panel that'll display our error info. */
      var errorInfoPanel = 
        createInformationalPanel( '#informationalPanelContainerDivId', 'Uninstall Logs' );

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
              errorInfoPanelBodyContent = generateLogsContent(errorInfoJson);

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

      /* Register a click-handler for #showUninstallTxnLogsLinkId to render 
       * the contents inside errorInfoPanel (and make it visible). 
       */
      globalYui.one("#showUninstallTxnLogsLinkId").on( "click", function(e) {

        errorInfoPanel.set( 'bodyContent', errorInfoPanelBodyContent );
        errorInfoPanel.show();

        if( firstTimeShowingErrorInfoPanel ) {

          globalYui.one('#txnProgressStatusActionsDivId').setContent(  
            '<a href="javascript:void(null)" id=clustersListLinkId>' + 
              'Restart the installation wizard' + 
            '</a>' );

          globalYui.one("#clustersListLinkId").on( "click", function(e) {
            window.open( generateClustersListUrl(txnProgressWidget.txnProgressContext.clusterName) );
          });

          firstTimeShowingErrorInfoPanel = false;
        }
      });
    }
  };

  var uninstallProgressWidget = new TxnProgressWidget
    ( uninstallProgressInfo, uninstallProgressStatusMessage, uninstallProgressPostCompletionFixup );

  uninstallProgressWidget.show();
} 
