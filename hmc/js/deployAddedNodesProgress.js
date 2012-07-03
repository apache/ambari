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
  return '<pre>' +
           globalYui.JSON.stringify( errorInfoJson.logs, null, 4 ) +
         '</pre>';
}
  
function renderDeployAddedNodesProgress(progressInfo) {

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

  var progressStatusMessage = {

    success:
      '<p>' +
        'Successfully added new nodes to your cluster.<br><a href="index.php" id="addMoreNodesSuccessLink" style="margin-top:10px" class="btn btn-large">Continue</a>' + 
      '</p>',
    failure:
      '<p>' +
        'Failed to add new nodes to the cluster.<br>Take a look at the ' +
          '<a href="javascript:void(null)" id="showDeployTxnLogsLinkId">deploy logs</a>' +
        ' to find out what might have gone wrong.' +
        '<a href="index.php" class="btn btn-large" style="margin-top:10px" id="addMoreNodesFailedLink">' + 
        'Continue' +
        '</a>' +
      '</p>'
  };

  var postCompletionFixup = {

    failure: function(txnProgressWidget) {

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
        errorInfoPanel.set('bodyContent', errorInfoPanelBodyContent);
        errorInfoPanel.show();

      });
    }
  };

  var progressWidget = new TxnProgressWidget(progressInfo, 'Add Nodes Progress', progressStatusMessage, postCompletionFixup);
  
  progressWidget.show();
} 
