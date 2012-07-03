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

function renderManageServicesProgress( manageServicesProgressInfo ) {

  var manageServicesProgressStatusMessage = {

    success:
      '<p>' +
        'Successfully completed the operation. ' + 
          '<a href="javascript:void(null)" id=closeManageServicesProgressWidgetLinkId>' + 
            'Continue' +
          '</a>' +
      '</p>',

    failure: 
      '<p>' + 
        'Failed to complete the operation.  Please ' +
          '<a href="javascript:void(null)" id=showManageServicesTxnLogsLinkId>take a look at Operation Logs</a>' +
        ' to see what might have gone wrong.' +
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

        var manageServicesUriPath = '/hmc/html/manageServices.php';
        var manageServicesUriPathRegEx = new RegExp(manageServicesUriPath);

        /* If we're already on manageServicesUriPath, just close the txnProgressWidget. */
        if( window.location.pathname.match(manageServicesUriPathRegEx) ) {
          txnProgressWidget.hide();
        }
        /* If not, redirect to manageServicesUriPath. */
        else {
          document.location.href = generateHMCUrl
            ( manageServicesUriPath + '?clusterName=' + txnProgressWidget.txnProgressContext.clusterName );
        }
      });

      /* Resume polling for information about the cluster's services. */
      if( typeof fetchClusterServicesPoller != 'undefined' ) {
        fetchClusterServicesPoller.start();
      }
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

        errorInfoPanel.set( 'centered', true );
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

      if( typeof fetchClusterServicesPoller != 'undefined' ) {
        /* Resume polling for information about the cluster's services. */
        fetchClusterServicesPoller.start();
      }
    }
  };

  var manageServicesProgressWidget = new TxnProgressWidget
    ( manageServicesProgressInfo, 'Manage Services', manageServicesProgressStatusMessage, manageServicesProgressPostCompletionFixup );

  manageServicesProgressWidget.show();
}
