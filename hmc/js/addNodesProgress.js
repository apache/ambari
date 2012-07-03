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


function cleanUpTxnProgress () {
  globalYui.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
  globalYui.one('#txnProgressStatusMessageDivId').setContent('');
  globalYui.one('#blackScreenDivId').setStyle('display', 'none');
  globalYui.one('#txnProgressStatusDivId').setStyle('display', 'none');
  globalYui.one('#addNodesCoreDivId').setStyle('display', 'none');
  globalYui.one('#txnProgressCoreDivId').setStyle('display', 'none');
  //globalYui.one('#txnProgressContentDivId').setContent
  //  ( '<img id=txnProgressLoadingImgId class=loadingImg src=../images/loading.gif />' );
}

function generateSingleDiscoverProgressStateMarkup(discoverProgressStateTitle, progressState) {
  var stateClass;
  var barClass;
    
  switch (progressState) {
  case "SUCCESS":
    stateClass = 'txnProgressStateDone';
    barClass = 'progress progress-success';
    break;

  case "STARTING":
  case "STARTED":
  case "IN_PROGRESS":
     stateClass = 'txnProgressStateInProgress';
     //barClass = 'progress progress-striped active';
     barClass = 'progress';
     break;

  case "FAILED":
  case "TOTALFAILURE":
    stateClass = 'txnProgressStateError';
    barClass = 'progress progress-danger';
    break;

  default: // PENDING
    stateClass = 'txnProgressStatePending';
    barClass = 'progress';
    break;
  }
  
  var barMarkup = '<div class="' + barClass + '"><div class="bar"></div></div>';
  if (stateClass == 'txnProgressStateInProgress') {
    barMarkup = '<div id="activeProgressBarContainer">' + barMarkup + '</div>';
  }
  
  var markup = '<li><label class="' + stateClass + '">' + discoverProgressStateTitle + '</label>' + barMarkup + '</li>';
  //globalYui.log('progress state=' + progressState + ', markup=' + markup);
  return markup;
}

function renderProgress (discoverProgressInfo) {

  var txnProgressShown = false;
  var pollingStopped = false;
  
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
  
  function clearActiveProgressBar() {
    var bar = globalYui.one('#activeProgressBar');
    if (bar != null) {
      bar.remove();
    }
    globalYui.on('windowresize', function(e) {
      setActiveProgressBarInPlace();
    });
  }
  
  function setActiveProgressBarInPlace() {
    // Puts an active progress bar where the placeholder with the DIV ID of "activeProgressBarSpot" is located.
    // Creates an instance of the active progress bar if one does not already exist
    // so that we can keep reusing it and moving it in place, rather than dynamically rendering it
    // on every successful callback to avoid flickering/disconnect due to animation.
    
    var bar = globalYui.one('#activeProgressBar');
    var barContainer = globalYui.one('#activeProgressBarContainer');    
    
    if (barContainer != null) {
      if (bar == null) {
        globalYui.one("body").append('<div id="activeProgressBar" class="wrapped progress progress-striped active" style="position:absolute;top:-50px;left:0;z-index:99;"><div style="width:100%" class="bar"></div></div>');
        bar = globalYui.one('#activeProgressBar');
      }      
      bar.setStyle('display', 'block');
      if (bar.getX() != barContainer.getX() || bar.getY() != barContainer.getY()) {      
        bar.setXY(barContainer.getXY());
      }
    } else if (bar != null) {
      bar.setStyle('display', 'none');
    }    
  }
  
  function runPollTask() {
    discoverProgressDataSource.sendRequest({
      request: '?clusterName=' + discoverProgressInfo.clusterName + '&txnId=' + discoverProgressInfo.txnId + '&action=addNodes',
      callback: {
        success: function (e) {
  
          if (pollingStopped) {
            return;
          }
  
          globalYui.one("#txnProgressHeader").setContent('Node Discovery and Preparation');
  
          var stateInfo = e.response.meta.stateInfo;
          var discoverProgressStates = e.response.meta.progressStates;
          var stateInfoLength = 0;
          var count = 0;
          var lastTxnId = 0;
          var discoverProgressMarkup = '<ul id="steps" class="wrapped">';
  
          var overallFail = false;
          var numSubTxns = 0;
          for (txnId in stateInfo.subTxns) {
            numSubTxns++;
          }

          if (numSubTxns == 0) {
            var errorInfoPanelBodyContent = 
              '<img id="errorInfoPanelLoadingImgId" class="loadingImg" src="../images/loading.gif" />';
  
            globalYui.one('#txnProgressContentDivId').setContent( errorInfoPanelBodyContent );
            globalYui.one("#txnProgressCoreDivId").setStyle("display", "block");
            poll();
            return;
          }
  
          for (var txnId in stateInfo.subTxns) {
            var currentProgressState = stateInfo.subTxns[txnId].opStatus;
            // If all states are in pending or just the first one, let's mark the first one as in progress
            if (numSubTxns == 1 && currentProgressState != 'FAILED' && currentProgressState != 'TOTALFAILURE') {
              currentProgressState = "IN_PROGRESS";
            }
            
            discoverProgressMarkup += 
              generateSingleDiscoverProgressStateMarkup(
                stateInfo.subTxns[txnId].description + stateInfo.subTxns[txnId].progress,
                currentProgressState);
            stateInfoLength++;
            count++;
            lastTxnId = txnId;
            overallFail |= (currentProgressState == 'FAILED' || currentProgressState == 'TOTALFAILURE');
          }
  
          // Render the remaining stages as pending
          if (stateInfoLength < discoverProgressStates.length) {
            for (; count < discoverProgressStates.length; count++ ) {     
              discoverProgressMarkup += 
                generateSingleDiscoverProgressStateMarkup(
                  discoverProgressStates[count],
                  'PENDING');
            }
          }
  
          var noNeedForFurtherPolling = false;
          var totalFailure = false;
          var installationStatusDivContent = '';
  
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
  
          if( noNeedForFurtherPolling ) {
  
            /* We've made all the progress we could have, so stop polling. */
            pollingStopped = true;
              
            if (!overallFail) {
              statusContent =             
                '<p>' +
                  'Finished node discovery and preparation. ' +
                  '<a class="btn btn-large" href="javascript:void(null)" id="successInfoLinkId">' +
                    'Proceed to Select Services' +
                  '</a>' +
                '</p>';
                globalYui.one('#txnProgressStatusDivId').removeClass('statusError');
                globalYui.one('#txnProgressStatusDivId').addClass('statusOk');
                globalYui.one('#txnProgressStatusMessageDivId').setContent(statusContent);    
                globalYui.one("#successInfoLinkId").on( "click", function(e) {
                  cleanUpTxnProgress();
                  transitionToNextStage( "#txnProgressCoreDivId", discoverProgressInfo, 
                    "#selectServicesCoreDivId", discoverProgressInfo, renderSelectServicesBlock);
              });
            } else {
              statusContent = 
                '<p>' +
                  'An error was encountered with some of the nodes.<br>' +
                  'Take a look at the <a href="javascript:void(null)" id=errorInfoLinkId>' +
                  'error logs</a> to see what might have happened.<br>'; 
      
                statusContent += '<a class="btn btn-large" style="margin:10px 0" href="javascript:void(null)"' +
                  'id="errorBackNavLinkId">Back to Add Nodes</a>';
                if (totalFailure == false) {
                  statusContent += ' <a class="btn btn-large" href=' + 
                    '"javascript:void(null)" id="errorFwdNavLinkId" style="margin:10px 0 10px 20px">Ignore and Continue</a>';
                }
                statusContent += '</p>';
                globalYui.one('#txnProgressStatusDivId').removeClass('statusOk');
                globalYui.one('#txnProgressStatusDivId').addClass('statusError');
                globalYui.one('#txnProgressStatusMessageDivId').setContent(statusContent);
                // now can add the on-click feature for the links
                globalYui.one("#errorBackNavLinkId").on( "click", function(e) {
                  cleanUpTxnProgress();
                  errorInfoPanel.destroy();
                  // give cleanup option
                  // cleanupInstall();
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
                /* If we stopped polling due to error, we need to do more work. */
                
                /* Create the panel that'll display our error info. */
                var errorInfoPanel = 
                  createInformationalPanel( '#informationalPanelContainerDivId',
                    'Node Discovery and Preparation Logs' );
    
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
                    //REZYYY globalYui.one("#progressErrorInfoNavigateDivId").setStyle( 'display', 'block' );
                });
            } // end if error
            
            globalYui.one('#txnProgressStatusDivId').setStyle('display', 'block');            
  
          } // end no need for further polling
  
          discoverProgressMarkup += '</ul>';
          globalYui.log('About to generate markup: ' + discoverProgressMarkup);
          globalYui.one('#txnProgressContentDivId').setContent(discoverProgressMarkup);
          
          if (!txnProgressShown) {
            globalYui.one('#txnProgressCoreDivId').setStyle('display','block');
            txnProgressShown = true;
          }
          setActiveProgressBarInPlace();
          if (!pollingStopped) {
            poll();
          }
        },
        failure: function (e) {
          alert('Failed to fetch more progress!');
          /* No point making any more attempts. */
          pollingStopped = true;
        }
      }
    });
  }

  function poll() {
    window.setTimeout(runPollTask, 3000);
  }
  
  clearActiveProgressBar();

  runPollTask();
} 
