/*
function cleanupInstall ()
{
  alert("We will cleanup the cluster now!");
  var cleanupRequestData = {
    "ClusterDeployUser" : globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value'))
  }

  var url = "../php/frontend/nodesAction.php?clusterName=" + InstallationWizard.AddNodes.renderData.clusterName + "&action=uninstall";
  globalYui.io(url, {
    method: 'POST',
    data: cleanupRequestData,
    timeout : 10000,
    on: {
      success: function (x,o) {
        globalYui.log("RAW JSON DATA: " + o.responseText);
        // Process the JSON data returned from the server
        try {
          setupNodesJson = globalYui.JSON.parse(o.responseText);
        } catch (e) {
          alert("JSON Parse failed!");
          return;
        }
        
        globalYui.log("PARSED DATA: " + globalYui.Lang.dump(setupNodesJson));
        if (setupNodesJson.result != 0) {
        // Error!
          alert("Got error!" + setupNodesJson.error);
          return;
        }
        
        setupNodesJson = setupNodesJson.response;
        hideLoadingImg();
        
        globalYui.one("#blackScreenDivId").setStyle("display", "block");
        renderProgress( setupNodesJson, "uninstall" );
        
      },
      
      failure: function (x,o) {
        alert("Async call failed!");
      }
    }
  });
}
*/

function getProgressStateCssClass (opStatus) 
{

  var cssMarkup;
  var error;

  switch (opStatus) {
  case "SUCCESS":
    cssMarkup = 'txnProgressStateDone';
    error = false;
    break;

  case "STARTED":
     cssMarkup = 'txnProgressStateInProgress';
     error = false
     break;

  case "FAILED":
    cssMarkup = 'txnProgressStateError';
    error = true;
    break;

  case "TOTALFAILURE":
    cssMarkup = 'txnProgressStateError';
    error = true;
    break;

  default:
    cssMarkup = 'txnProgressStatePending';
    error = false;
    break;
  }

  var cssMarkupAndError = {
    'cssMarkup' : cssMarkup,
    'error'     : error
  };

  return cssMarkupAndError;
}

function cleanUpTxnProgress ()
{
  globalYui.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
  globalYui.one('#txnProgressStatusMessageDivId').setContent('');
  globalYui.one('#blackScreenDivId').setStyle('display', 'none');
  globalYui.one('#txnProgressStatusDivId').setStyle('display', 'none');
  globalYui.one('#addNodesCoreDivId').setStyle('display', 'none');
  globalYui.one('#txnProgressFieldSetId').setStyle("display", "block");
  //globalYui.one('#txnProgressDynamicRenderDivId').setContent
  //  ( '<img id=txnProgressLoadingImgId class=loadingImg src=../images/loading.gif />' );
}

function generateSingleDiscoverProgressStateMarkup( discoverProgressStateTitle, discoverProgressStateCssClass ) {

  globalYui.log( 'Generating: ' + discoverProgressStateTitle + '-' + discoverProgressStateCssClass );

  var markup = 
    '<li>' + 
      '<div class=' + discoverProgressStateCssClass + '>' +
        discoverProgressStateTitle +
      '</div>' +
    '</li>';

    globalYui.log("XXX" + markup);
  return markup;
}

function renderProgress (discoverProgressInfo) {

  var discoverProgressDataSource = new globalYui.DataSource.IO ({
    source: '../php/frontend/nodesActionProgress.php'
  });

  discoverProgressDataSource.plug(globalYui.Plugin.DataSourceJSONSchema, {
    schema: {
      metaFields: {
        progressStates: 'progressStates',
        currentProgressStateIndex: 'currentProgressStateIndex',
        encounteredError: 'encounteredError',
        stateInfo: 'stateInfo'
      }
    }
  });

  var discoverProgressPollHandle = discoverProgressDataSource.setInterval( 3000, {
    request: '?clusterName=' + discoverProgressInfo.clusterName + '&txnId=' + discoverProgressInfo.txnId + '&action=addNodes',
    callback: {
      success: function (e) {

        var stateInfo = e.response.meta.stateInfo;
        var discoverProgressStates = e.response.meta.progressStates;
        var stateInfoLength = 0;
        var count = 0;
        var lastTxnId = 0;
        var discoverProgressMarkup = '<ul id=txnProgressStatesListId>';

        var overallFail = false;
        var numSubTxns = 0;
        for (txnId in stateInfo.subTxns) {
          numSubTxns++;
        }
        if (numSubTxns == 0) {
          var errorInfoPanelBodyContent = 
            '<img id=errorInfoPanelLoadingImgId class=loadingImg src=../images/loading.gif />';

          // globalYui.log('About to generate markup: ' + discoverProgressMarkup);
          globalYui.one('#txnProgressDynamicRenderDivId').setContent( errorInfoPanelBodyContent );
          globalYui.one("#txnProgressCoreDivId").setStyle("display", "block");
          return;
        }

        for (txnId in stateInfo.subTxns) {
          var currentProgressStateCssClass = getProgressStateCssClass(stateInfo.subTxns[txnId].opStatus);
          var cssMarkup = currentProgressStateCssClass.cssMarkup ;
          // If all states are in pending or just the first one, let's mark the first one as in progress
          if (numSubTxns == 1 && cssMarkup != 'txnProgressStateError') {
            cssMarkup = 'txnProgressStateInProgress';
          }

          discoverProgressMarkup += 
            generateSingleDiscoverProgressStateMarkup(
                stateInfo.subTxns[txnId].description + " " + stateInfo.subTxns[txnId].progress,
//              discoverProgressStates[stateInfoLength], 
              cssMarkup);
          stateInfoLength++;
          count++;
          lastTxnId = txnId;
          overallFail |= currentProgressStateCssClass.error;
        }

        // Render the remaining stages as pending
        if (stateInfoLength < discoverProgressStates.length) {
          for (; count < discoverProgressStates.length; count++ ) {
            var cssClass = 'txnProgressStatePending';
            discoverProgressMarkup += 
              generateSingleDiscoverProgressStateMarkup(
                discoverProgressStates[count],
                cssClass);
          }
        }

        var noNeedForFurtherPolling = false;
        var totalFailure = false;
        var installationStatusDivContent = '';
        var installationStatusDivCssClass = '';

        if (stateInfo.subTxns[lastTxnId].opStatus == "TOTALFAILURE") {
          noNeedForFurtherPolling = true;
          totalFailure = true;
        } else if (stateInfoLength == discoverProgressStates.length) {
          if ((stateInfo.subTxns[lastTxnId].opStatus == "SUCCESS") 
          || (stateInfo.subTxns[lastTxnId].opStatus == "FAILED")) {
            noNeedForFurtherPolling = true;
          } else {
            noNeedForFurtherPolling = false;
          }
        }

        if (overallFail == true) {
          installationStatusDivContent = 
          '<p>' +
            'We found a few rebellious nodes! Take a look at the ' +
            '<a href="javascript:void(null)" id=errorInfoLinkId>' +
            'Error Logs' +
            '</a>' +
            '?' +   
          '</p>';
          installationStatusDivCssClass = 'statusError';
        } else {
          installationStatusDivContent =             
          '<p>' +
            'All done with discovering nodes. ' +
            '<a href="javascript:void(null)" id=successInfoLinkId>' +
              'Great!' +
            '</a>' +
          '</p>';
          installationStatusDivCssClass = 'statusOk';
        }

        if( noNeedForFurtherPolling ) {

          /* We've made all the progress we could have, so stop polling. */
          discoverProgressDataSource.clearInterval( discoverProgressPollHandle );

          globalYui.one('#txnProgressStatusDivId').setStyle('display', 'block');
          globalYui.one('#txnProgressStatusDivId').addClass(installationStatusDivCssClass);
          var installationStatusDiv = globalYui.one('#txnProgressStatusMessageDivId');
          
          installationStatusDiv.setContent(installationStatusDivContent);

          /* If we stopped polling due to error, we need to do more work. */
          if( installationStatusDivCssClass == 'statusError' ) {

            /* Create the panel that'll display our error info. */
            var errorInfoPanel = 
              createInformationalPanel( '#informationalPanelContainerDivId',
                'Logs for the nodes\' initialization process.' );

              globalYui.one("#errorInfoLinkId").on( "click", function(err) {
                var bodyContent = "";
                for (subTxn in e.response.meta.stateInfo.subTxns) {
                  var subTxnInfo = e.response.meta.stateInfo.subTxns[subTxn];
                  var additionalInfoTable = '<table>' +
                                               '<thead><tr><th>Host</th><th>Info</th></tr></thead>';
                  for (hostName in subTxnInfo.state) {
                    additionalInfoTable +=     '<tr><td>' + hostName + '</td><td><pre>' +subTxnInfo.state[hostName] + '</pre></td></tr>'
                  }
                  additionalInfoTable +=    '</table>';
                  bodyContent += '<div class="logEntry">' +
                                   '<div class="logEntryHeader">' + subTxnInfo.description + '</div>' +
                                   '<div class="logEntryBody">' +
                                     '<ul>' + 
                                       '<li>Entry Id : ' + subTxnInfo.subTxnId + '</li>' +
                                       '<li>Final result : ' + subTxnInfo.opStatus + '</li>' +
                                       '<li>Progress at the end : ' + subTxnInfo.progress + '</li>' +
                                       '<li>Additional information : ' + additionalInfoTable + '</li>' +
                                     '</ul>' +
                                   '</div>' +
                                 '</div>';
                }
                errorInfoPanel.set('bodyContent' , bodyContent);
                //errorInfoPanel.set( 'bodyContent', '<pre>' + 
                //  globalYui.JSON.stringify( e.response.meta.stateInfo, null, 4) + '</pre>' );
                errorInfoPanel.show();
                var linkInfo = '<a href="javascript:void(null)"' +
                  'id=errorBackNavLinkId>Back</a>';
                if (totalFailure == false) {
                  linkInfo += ' <a href=' + 
                  '"javascript:void(null)" id=errorFwdNavLinkId>Continue</a>';
                }

                  //REZYYY globalYui.one("#progressErrorInfoNavigateDivId").setContent(linkInfo);
                  globalYui.one("#txnProgressStatusActionsDivId").setContent(linkInfo);

                  // now can add the on-click feature for the links
                  globalYui.one("#errorBackNavLinkId").on( "click", function(e) {
                    cleanUpTxnProgress();
                    errorInfoPanel.destroy();
                    // give cleanup option
                    // cleanupInstall();
                    //REZYYY globalYui.one("#progressErrorInfoNavigateDivId").setContent("");
                    globalYui.one("#txnProgressStatusActionsDivId").setContent("");
                    globalYui.one('#txnProgressCoreDivId').setStyle('display', 'none');
                    globalYui.one("#addNodesCoreDivId").setStyle('display', 'block');
                   });

                  if (totalFailure == false) {
                    globalYui.one("#errorFwdNavLinkId").on( "click", function(e) {
                      cleanUpTxnProgress();
                      errorInfoPanel.destroy();
                      //REZYYY globalYui.one("#progressErrorInfoNavigateDivId").setContent("");
                      globalYui.one("#txnProgressStatusActionsDivId").setContent("");
                      transitionToNextStage( "#txnProgressCoreDivId", discoverProgressInfo, 
                        "#selectServicesCoreDivId", discoverProgressInfo, renderSelectServicesBlock);
                    });
                  }

                //REZYYY globalYui.one("#progressErrorInfoNavigateDivId").setStyle( 'display', 'block' );
            });
          } else {
            globalYui.one("#successInfoLinkId").on( "click", function(e) {
              cleanUpTxnProgress();
              transitionToNextStage( "#txnProgressCoreDivId", discoverProgressInfo, 
                "#selectServicesCoreDivId", discoverProgressInfo, renderSelectServicesBlock);
          });
        }
        }

        discoverProgressMarkup += '</ul>';

//        globalYui.log('About to generate markup: ' + discoverProgressMarkup);
        globalYui.one('#txnProgressDynamicRenderDivId').setContent( discoverProgressMarkup );
          globalYui.one("#txnProgressCoreDivId").setStyle("display", "block");
      },
      failure: function (e) {
        alert('Failed to fetch more progress!');
        /* No point making any more attempts. */
        discoverProgressDataSource.clearInterval( discoverProgressPollHandle );
      }
    }
  });
} 
