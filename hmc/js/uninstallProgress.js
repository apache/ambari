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

function renderUninstallProgress (uninstallProgressInfo) {

  hideLoadingImg();


  var uninstallProgressStatusMessage = {

    success:
      '<p>' +
        'Uninstalled the cluster successfully.' +
        '<a href="javascript:void(null)" style="margin-left:20px" class="btn btn-large" id="clustersListLinkId">' +
          'Continue' +
        '</a>' +
      '</p>',
    failure:
      '<p>' +
        'There was a problem with uninstall.<br />Take a look at ' +
          '<a href="javascript:void(null)" id="showUninstallTxnLogsLinkId">Uninstall Logs</a>' +
        ' to see what might have happened.<br>' +
        '<a href="javascript:void(null)" class="btn btn-large" style="margin-top:10px" id="clustersListLinkId">' + 
            'Close' + 
        '</a>' +
      '</p>'
  };

  var uninstallProgressPostCompletionFixup = {

    success: function( txnProgressWidget ) {

      globalYui.one("#clustersListLinkId").on( "click", function(e) {
        document.location.href = generateHMCUrl();
      });
    },

    failure: function( txnProgressWidget ) {

      globalYui.one("#clustersListLinkId").on( "click", function(e) {
        document.location.href = generateHMCUrl();
      });
      
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


      globalYui.one("#showUninstallTxnLogsLinkId").on( "click", function(e) {
        errorInfoPanel.set( 'centered', true);
        errorInfoPanel.set( 'bodyContent', errorInfoPanelBodyContent );
        errorInfoPanel.show();
      });
    }
  };

  var uninstallProgressWidget = new TxnProgressWidget
    ( uninstallProgressInfo, 'Uninstall Cluster', uninstallProgressStatusMessage, uninstallProgressPostCompletionFixup );

  uninstallProgressWidget.show();
} 
