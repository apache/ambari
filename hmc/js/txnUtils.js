function TxnProgressWidget( txnProgressContext, txnProgressStatusMessage, txnProgressPostCompletionFixup ) {

  /**************************** Private methods ********************************/
  var txnProgressStateShouldBeSkipped = function( txnProgressState ) {
    
    var skipIt = false;

    /* Step over any deploy progress states that aren't in the CLUSTER, SERVICE 
     * or SERVICE-SMOKETEST contexts. 
     */
    if( (txnProgressState.subTxnType != 'CLUSTER') && 
        (txnProgressState.subTxnType != 'SERVICE') &&
        (txnProgressState.subTxnType != 'SERVICE-SMOKETEST') ) {

      skipIt = true;
    }

    return skipIt;
  }

  var generateSingleTxnProgressStateMarkup = function( txnProgressStateTitle, txnProgressStateCssClass ) {

    globalYui.log( 'Generating: ' + txnProgressStateTitle + '-' + txnProgressStateCssClass );

    var markup = 
      '<li>' + 
        '<div class=' + txnProgressStateCssClass + '>' +
          /* TODO XXX Format this nicer by camel-casing and putting the status in parentheses. */
          txnProgressStateTitle +
        '</div>' +
      '</li>';

    globalYui.log("XXX" + markup);
    return markup;
  }

  /**************************** Public data members ********************************/
  globalYui.log( 'Got txnId:' + txnProgressContext.txnId );

  this.txnProgressContext = txnProgressContext;
  this.txnProgressStatusMessage = txnProgressStatusMessage;
  this.txnProgressPostCompletionFixup = txnProgressPostCompletionFixup;
  var requestStr = '?clusterName=' + this.txnProgressContext.clusterName + '&txnId=' + this.txnProgressContext.txnId;

  if ("deployUser" in this.txnProgressContext) {
    requestStr += '&deployUser=' + this.txnProgressContext.deployUser;
  }

  var pdpDataSourceContext = {
    source: '../php/frontend/fetchTxnProgress.php',
    schema: {
      metaFields: {
        progress: 'progress'
      }
    },

    request: requestStr, 
    pollInterval: 3000,
    maxFailedAttempts: 5
  };

  var pdpResponseHandler = {
    success: function (e,pdp) {
      /* What we're here to render. */
      var txnProgressMarkup = '';

      var txnProgress = e.response.meta.progress;

      /* Make sure we have some progress data to start to show - if not, 
       * we'll just show a loading image until this is non-null.
       */
      if( txnProgress != null ) {

        var txnProgressStates = txnProgress.subTxns;
        globalYui.log(globalYui.Lang.dump(txnProgressStates));

        txnProgressMarkup = '<ul id=txnProgressStatesListId>';

        var progressStateIndex = 0;

        /* Generate markup for all the "done" states. */
        for( ; progressStateIndex < txnProgressStates.length; ++progressStateIndex ) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if( txnProgressStateShouldBeSkipped( presentTxnProgressState ) ) {
            continue;
          }

          /* The first sign of a state that isn't done, and we're outta here. */
          if( presentTxnProgressState.progress != 'COMPLETED' ) {
            break;
          }

          globalYui.log( 'Done loop - ' + progressStateIndex );

          txnProgressMarkup += generateSingleTxnProgressStateMarkup
            ( presentTxnProgressState.description, 'txnProgressStateDone' );

            globalYui.log("Currently, markup is:" + txnProgressMarkup );
        }

        /* Next, generate markup for the first "in-progress" state. */
        for( ; progressStateIndex < txnProgressStates.length; ++progressStateIndex ) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if( txnProgressStateShouldBeSkipped( presentTxnProgressState ) ) {
            continue;
          }

          /* We only care to process "in-progress" states.
           *
           * However, when a state fails, its progress property is set to
           * 'FAILED', so check for that as well. Without this, a failed 
           * state leads to only the previous 'COMPLETED' states being 
           * rendered (with the 'FAILED' and 'PENDING' states being skipped
           * over because there's no 'IN_PROGRESS' state) whilst 
           * #txnProgressStatusDivId shows an overall error (as it rightly 
           * should) - in short, confusion all around.
           */
          if( (presentTxnProgressState.progress == 'IN_PROGRESS') ||
              (presentTxnProgressState.progress == 'FAILED') ) {

            globalYui.log( 'In-progress/failed - ' + progressStateIndex );

            /* Decide upon what CSS class to assign to the currently-in-progress
             * state - if an error was marked as having been encountered, assign
             * the fitting .txnProgressStateError, else just annoint it with
             * .txnProgressStateInProgress 
             */
            var currentProgressStateCssClass = 'txnProgressStateInProgress';

            /* The 2 possible indications of error are:
             * 
             * a) presentTxnProgressState.progress is 'IN_PROGRESS' but 
             *    txnProgress.encounteredError is true.
             * b) presentTxnProgressState.progress is 'FAILED'.
             */
            if( (txnProgress.encounteredError) || 
                (presentTxnProgressState.progress == 'FAILED') ) {

              currentProgressStateCssClass = 'txnProgressStateError';
            }

            /* And generate markup for this "in-progress" state. */
            txnProgressMarkup += generateSingleTxnProgressStateMarkup
              ( presentTxnProgressState.description, currentProgressStateCssClass );

            /* It's important to manually increment progressStateIndex here, 
             * to set it up correctly for the upcoming loop.
             */
            ++progressStateIndex;

            /* Remember, we only care for the FIRST "in-progress" state.
             *
             * Any following "in-progress" states will all be marked as 
             * "pending", so as to avoid the display from becoming 
             * disorienting (with multiple states "in-progress").
             */
            break;
          }
        }

        /* Finally, generate markup for all the "pending" states. */
        for( ; progressStateIndex < txnProgressStates.length; ++progressStateIndex ) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if( txnProgressStateShouldBeSkipped( presentTxnProgressState ) ) {
            continue;
          }

          globalYui.log( 'Pending loop - ' + progressStateIndex );
          txnProgressMarkup += generateSingleTxnProgressStateMarkup
            ( presentTxnProgressState.description, 'txnProgressStatePending' );
        }

        var noNeedForFurtherPolling = false;
        var txnProgressStatusDivContent = '';
        var txnProgressStatusDivCssClass = '';

        /* We can break this polling cycle in one of 2 ways: 
         * 
         * 1) If we are explicitly told by the backend that we're done.
         */
        if( txnProgress.processRunning == 0 ) {

          noNeedForFurtherPolling = true;
          /* Be optimistic and assume that no errors were encountered (we'll
           * get more in touch with reality further below).
           */
          txnProgressStatusDivContent = this.txnProgressStatusMessage.success;
          txnProgressStatusDivCssClass = 'statusOk';
        }

        /* 2) If we encounter an error.
         *
         * Note how this is placed after the previous check, so as to serve
         * as an override in case the backend explicitly told us that we're
         * done, but an error was encountered in that very last progress report.
         */
        if( txnProgress.encounteredError ) {

          noNeedForFurtherPolling = true;
          txnProgressStatusDivContent = this.txnProgressStatusMessage.failure;
          txnProgressStatusDivCssClass = 'statusError';
        }

        if( noNeedForFurtherPolling ) {

          /* We've made all the progress we could have, so stop polling. */
          pdp.stop();

          var txnProgressStatusDiv = globalYui.one('#txnProgressStatusDivId');
          
          txnProgressStatusDiv.addClass(txnProgressStatusDivCssClass);
          txnProgressStatusDiv.one('#txnProgressStatusMessageDivId').setContent(txnProgressStatusDivContent);
          txnProgressStatusDiv.setStyle('display', 'block');

          /* Run the post-completion fixups. */
          if( txnProgressStatusDivCssClass == 'statusOk' ) {
            if( this.txnProgressPostCompletionFixup.success ) {
              this.txnProgressPostCompletionFixup.success(this);
            }
          }
          else if( txnProgressStatusDivCssClass == 'statusError' ) {
            if( this.txnProgressPostCompletionFixup.failure ) {
              this.txnProgressPostCompletionFixup.failure(this);
            }
          }
        }

        txnProgressMarkup += '</ul>';
      }
      else {
        txnProgressMarkup = '<img id=txnProgressLoadingImgId class=loadingImg src=../images/loading.gif />';
      }

      globalYui.log('About to generate markup: ' + txnProgressMarkup);
      globalYui.one('#txnProgressDynamicRenderDivId').setContent( txnProgressMarkup );

    }.bind(this),

    failure: function (e, pdp) {
      alert('Failed to fetch more progress!');
    }.bind(this)
  };

  this.periodicDataPoller = new PeriodicDataPoller( pdpDataSourceContext, pdpResponseHandler );
}

TxnProgressWidget.prototype.show = function() {
  
  /* Start with a clean slate for #txnProgressStatusDivId, regardless of 
   * the mess previous uses might have left it in.
   */
  var txnProgressStatusDiv = globalYui.one('#txnProgressStatusDivId');
  txnProgressStatusDiv.one('#txnProgressStatusMessageDivId').setContent('');
  txnProgressStatusDiv.one('#txnProgressStatusActionsDivId').setContent('');
  /* Remove the CSS statusOk/statusError classes from txnProgressStatusDiv 
   * as well - sure would be nice to remove all classes that match a 
   * pattern, but oh well. 
   */
  txnProgressStatusDiv.removeClass('statusOk');
  txnProgressStatusDiv.removeClass('statusError');

  /* Similarly, set a clean slate for #txnProgressDynamicRenderDivId as well. */
  globalYui.one('#txnProgressDynamicRenderDivId').setContent
    ( '<img id=txnProgressLoadingImgId class=loadingImg src=../images/loading.gif />' );

  globalYui.one('#blackScreenDivId').setStyle('display', 'block');
  globalYui.one('#txnProgressCoreDivId').setStyle('display','block');  

  this.periodicDataPoller.start();
}

TxnProgressWidget.prototype.hide = function() {

  this.periodicDataPoller.stop();

  globalYui.one('#txnProgressStatusDivId').setStyle('display', 'none'); 
  globalYui.one('#txnProgressCoreDivId').setStyle('display','none');
  globalYui.one('#blackScreenDivId').setStyle('display', 'none');
}
