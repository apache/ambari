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

function renderDeployProgress (deployProgressInfo) {

  hideLoadingImg();

  /* At this point, our users are done with the installation wizard
   * and have asked for a deploy, so there's no going back - remove
   * all traces of #installationWizardProgressBarDivId.
   */
  var installationWizardProgressBarDiv = globalYui.one('#installationWizardProgressBarDivId');

  /* But be safe and perform this removal only if #installationWizardProgressBarDivId 
   * actually exists on the page - this .js file is now being used in more 
   * than one place, so this robustness is needed.
   */
  if (installationWizardProgressBarDiv) {
    installationWizardProgressBarDiv.setStyle('display', 'none')
  }

  var hmcRestartMsg = '';
  if (deployProgressInfo.nagiosGangliaCoHosted != null
      && deployProgressInfo.nagiosGangliaCoHosted) {
    hmcRestartMsg = '<span style="color:red"><strong>Note:</strong> You need to restart ' + App.Props.managerServiceName + ' as'
        + ' Nagios/Ganglia are co-hosted on this server.<br>Please restart '
        + App.Props.managerServiceName + ' using \"service ' + App.Props.managerServiceName.toLowerCase() + ' restart\".</span><br>After that is done, ';
  } else {
    hmcRestartMsg = 'Please ';
  }

  hmcRestartMsg += 
      '<a href="javascript:void(null)" id="clustersListLinkId">' +
        'click here to start managing your cluster.' +
      '</a>';

  var deployProgressStatusMessage = {

    success:
      '<p>' +
        'Your cluster is ready! <br/>' + hmcRestartMsg +
      '</p>',
    failure:
      '<p>' +
        'Failed to finish setting up the cluster.<br>Take a look at the ' +
          '<a href="javascript:void(null)" id="showDeployTxnLogsLinkId">deploy logs</a>' +
        ' to find out what might have gone wrong.' +
        '<a href="javascript:void(null)" class="btn btn-large" style="margin-top:10px" id="restartInstallationWizardLinkId">' + 
        'Reinstall Cluster' +
        '</a>' +
      '</p>'
  };

  var deployProgressPostCompletionFixup = {

    success: function( txnProgressWidget ) {

      globalYui.one("#clustersListLinkId").on( "click", function(e) {
        document.location.href = generateHMCUrl();
      });
    },

    failure: function( txnProgressWidget ) {
      globalYui.one("#restartInstallationWizardLinkId").on( "click", function(e) {
        document.location.href = 'installFailed.php';
      });

      /* Create the panel that'll display our error info. */
      var errorInfoPanel = 
        createInformationalPanel( '#informationalPanelContainerDivId', 'Deploy Logs' );

      /* Prime the panel to start off showing our stock loading image. */
      var errorInfoPanelBodyContent = 
        '<img id="errorInfoPanelLoadingImgId" class="loadingImg" src="../images/loading.gif" />';

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

      /* Register a click-handler for #showDeployTxnLogsLinkId to render 
       * the contents inside errorInfoPanel (and make it visible). 
       */
      globalYui.one("#showDeployTxnLogsLinkId").on( "click", function(e) {

        errorInfoPanel.set( 'centered', true );
        errorInfoPanel.set( 'bodyContent', errorInfoPanelBodyContent );
        errorInfoPanel.show();
      });
    }
  };

  var deployProgressWidget = new TxnProgressWidget
    ( deployProgressInfo, 'Deployment Progress', deployProgressStatusMessage, deployProgressPostCompletionFixup );
  
  deployProgressWidget.show();
} 
