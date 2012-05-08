var tmpY1;

function generateSingleDiscoverProgressStateMarkup( discoverProgressStateTitle, discoverProgressStateCssClass ) {

  tmpY1.log( 'Generating: ' + discoverProgressStateTitle + '-' + discoverProgressStateCssClass );

  var markup = 
    '<li>' + 
      '<div class=' + discoverProgressStateCssClass + '>' +
        discoverProgressStateTitle +
      '</div>' +
    '</li>';

    tmpY1.log("XXX" + markup);
  return markup;
}

var errorInformationPanel;

function renderProgress (Y, discoverProgressInfo) {

  tmpY1 = Y;

  var discoverProgressDataSource = new Y.DataSource.IO ({
    source: '../php/frontend/fetchDiscoverProgressInfo.php'
  });

  discoverProgressDataSource.plug(Y.Plugin.DataSourceJSONSchema, {
    schema: {
      metaFields: {
        progressStates: 'progressStates',
        currentProgressStateIndex: 'currentProgressStateIndex',
        encounteredError: 'encounteredError',
        stateInfo: 'stateInfo',
        nextStageInfo: 'nextStageInfo'
      }
    }
  });

  var discoverProgressPollHandle = discoverProgressDataSource.setInterval( 3000, {
    request: '?clusterName=' + discoverProgressInfo.clusterName,
    callback: {
      success: function (e) {

        var discoverProgressStates = e.response.meta.progressStates;

//        Y.log(Y.Lang.dump(discoverProgressStates));
//        Y.log(e.response.meta.currentProgressStateIndex);

        var discoverProgressMarkup = '<ul id=displayProgressStatesListId>';

        var progressStateIndex = 0;

        /* Generate markup for all the "done" states. */
        for( ; progressStateIndex < e.response.meta.currentProgressStateIndex; ++progressStateIndex ) {

//          Y.log( 'Done loop - ' + progressStateIndex );
          discoverProgressMarkup += generateSingleDiscoverProgressStateMarkup
            ( discoverProgressStates[ progressStateIndex ], 'displayProgressStateDone' );
//            Y.log("Currently, markup is:" + discoverProgressMarkup );
        }

        if( progressStateIndex < discoverProgressStates.length ) {

//          Y.log( 'In progress - ' + progressStateIndex );

          /* Decide upon what CSS class to assign to the currently-in-progress
           * state - if an error was marked as having been encountered, assign
           * the fitting .discoverProgressStateError, else just annoint it with
           * .discoverProgressStateInProgress 
           */
          var currentProgressStateCssClass = 'displayProgressStateInProgress';

          if( e.response.meta.encounteredError ) {

            currentProgressStateCssClass = 'displayProgressStateError';
          }

          /* Then generate markup for the "in-progress" state. */
          discoverProgressMarkup += generateSingleDiscoverProgressStateMarkup
            ( discoverProgressStates[ progressStateIndex ], currentProgressStateCssClass );

          ++progressStateIndex;

          /* Finally, generate markup for all the "pending" states. */
          for( ; progressStateIndex < discoverProgressStates.length; ++progressStateIndex ) {

//            Y.log( 'Pending loop - ' + progressStateIndex );
            discoverProgressMarkup += generateSingleDiscoverProgressStateMarkup
              ( discoverProgressStates[ progressStateIndex ], 'displayProgressStatePending' );
          }
        }

        var noNeedForFurtherPolling = false;
        var installationStatusDivContent = '';
        var installationStatusDivCssClass = '';

        /* We can break this polling cycle in one of 2 ways: 
         * 
         * 1) If all the states have been progressed through.
         */
        if( e.response.meta.currentProgressStateIndex == (discoverProgressStates.length) ) {

          noNeedForFurtherPolling = true;

          if ( e.response.meta.encounteredError == true) {
            installationStatusDivContent = 
              '<p>' +
              'We found a few wild ones! Take a look at the' + 
              '<a href="javascript:void(null)" id="errorInfoLinkId">Error Logs</a>' +
              '?' +  
              '</p>';

            installationStatusDivCssClass = 'statusError';

          } else {
            installationStatusDivContent = 
            '<p>' +
              'All done with discovering nodes! ' + 
              '<a href="javascript:void(null)" id=successInfoLinkId>' + 
                'Great!' +
              '</a>' +
            '</p>';
          installationStatusDivCssClass = 'statusOk';
          }
        }

        if( noNeedForFurtherPolling ) {

          /* We've made all the progress we could have, so stop polling. */
          discoverProgressDataSource.clearInterval( discoverProgressPollHandle );

          var installationStatusDiv = Y.one('#displayProgressStatusDivId');
          
          installationStatusDiv.set('className', installationStatusDivCssClass);
          installationStatusDiv.setContent(installationStatusDivContent);
          installationStatusDiv.setStyle('display', 'block');

          /* If we stopped polling due to error, we need to do more work. */
          if( installationStatusDivCssClass == 'statusError' ) {

            /* Create the panel that'll display our error info. */
            errorInformationPanel = 
              createInformationalPanel( Y, '#informationalPanelContainerDivId', 'Deploy Logs' );

            /* Augment errorInformationPanel with the relevant buttons. */
            var backToInstallationWizardButton = {
              value: 'I will tame the wild \'uns!',
              action: function (e) {
              e.preventDefault();
              Y.one('#displayProgressStatusDivId').setStyle('display', 'none');
              Y.one('#displayProgressCoreDivId').setStyle('display', 'none');
              Y.one('#installationWizardProgressBarDivId').setStyle('display', 
                                                                    'block');
              Y.one("#initializeClusterCoreDivId").setStyle('display', 'block');
              alert("HIDING!");
              errorInformationPanel.hide();
              },
              section: 'footer'
            };

            var goToServicesSelectPageButton = {
              value: 'I dont mind the wild ones!',
              action: function (e) {
              e.preventDefault();
              Y.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
              Y.one('#displayProgressStatusDivId').setStyle('display', 'none');
              transitionToNextStage( "#displayProgressCoreDivId", discoverProgressInfo, "#selectServicesCoreDivId", discoverProgressInfo, renderSelectServicesBlock);
                alert("HIDING!");
                errorInformationPanel.hide();
                errorInformationPanel = null;
              },
              section: 'footer'
            };

            errorInformationPanel.addButton( backToInstallationWizardButton );
            errorInformationPanel.addButton( goToServicesSelectPageButton );

            Y.one("#errorInfoLinkId").on( "click", function(err) {
            
                Y.log("ERROR LINK RENDER: " + Y.Lang.dump(e.response.meta.stateInfo));
                errorInformationPanel.set( 'bodyContent', Y.Lang.dump( e.response.meta.stateInfo ) );
                alert("SHOWING! " + Y.Lang.dump(errorInformationPanel));
                errorInformationPanel.show();
            });
          } else {
            Y.one("#successInfoLinkId").on( "click", function(e) {
              Y.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
              Y.one('#displayProgressStatusDivId').setStyle('display', 'none');
              transitionToNextStage( "#displayProgressCoreDivId", discoverProgressInfo, "#selectServicesCoreDivId", discoverProgressInfo, renderSelectServicesBlock);
          });
        }
        }

        discoverProgressMarkup += '</ul>';

//        Y.log('About to generate markup: ' + discoverProgressMarkup);
        Y.one('#displayProgressDynamicRenderDivId').setContent( discoverProgressMarkup );
      },
      failure: function (e) {
        alert('Failed to fetch more progress!');
        /* No point making any more attempts. */
        discoverProgressDataSource.clearInterval( discoverProgressPollHandle );
      }
    }
  });
} 
