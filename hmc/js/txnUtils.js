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

App.ui.TxnProgressWidget = function (config) {

  var CONTAINER_DOM_ID = 'txnProgressCoreDivId';
  var CLOSE_LINK_DOM_ID = 'txnProgressWidgetCloseLink';
  var SHOW_LOGS_LINK_DOM_ID = 'txnProgressWidgetShowLogsLink';

  var periodicDataPoller = null;
  var isLogFetched = false;
  var errorInfoPanel = null;
  var targetUrl = null;

  config.successMessage = config.successMessage ||
    '<p>' +
      'Successfully completed the operation.' +
      '<a href="javascript:void(null)" id="' + CLOSE_LINK_DOM_ID + '" style="margin-left:10px" class="btn btn-large">' +
      'Continue' +
      '</a>' +
      '</p>';

  config.failureMessage = config.failureMessage ||
    '<p>' +
      'Failed to complete the operation.  Please ' +
      '<a href="javascript:void(null)" id="' + SHOW_LOGS_LINK_DOM_ID + '">take a look at Operation Logs</a>' +
      ' to see what might have gone wrong.  ' +
      '<a href="javascript:void(null)" id="' + CLOSE_LINK_DOM_ID + '">' +
      'Close' +
      '</a>' +
      '</p>';

  config.onSuccess = config.onSuccess || function (widget) {
  };

  config.onFailure = config.onFailure || function (widget) {
  };

  config.onClose = config.onClose || function (widget) {
  };

  this.setTargetUrl = function (url) {
    targetUrl = url;
  };

  var txnProgressStateShouldBeSkipped = function (txnProgressState) {

    var skipIt = false;

    if ((txnProgressState.subTxnType != 'CLUSTER') &&
      (txnProgressState.subTxnType != 'SERVICE') &&
      (txnProgressState.subTxnType != 'SERVICE-SMOKETEST')) {

      skipIt = true;
    }

    return skipIt;
  };

  var getTxnElementMarkup = function (txnProgressStateTitle, txnProgressState) {

    var stateClass;
    var barClass;
    var status;

    switch (txnProgressState) {
      case 'COMPLETED':
        stateClass = 'txnProgressStateDone';
        barClass = 'progress progress-success';
        status = 'Completed';
        break;

      case 'IN_PROGRESS':
        stateClass = 'txnProgressStateInProgress';
        //barClass = 'progress progress-striped active';
        barClass = 'progress';
        status = 'In Progress';
        break;

      case 'FAILED':
        stateClass = 'txnProgressStateError';
        barClass = 'progress progress-danger';
        status = 'Failed';
        break;

      default:
        stateClass = 'txnProgressStatePending';
        barClass = 'progress';
        status = 'Pending';
        break;
    }

    var barMarkup = '<div class="' + barClass + '"><div class="bar"></div></div>';

    if (stateClass == 'txnProgressStateInProgress') {
      barMarkup = '<div id="activeProgressBarContainer">' + barMarkup + '</div>';
    }

    var markup = '<li class="clearfix"><label class="' + stateClass + '">' + txnProgressStateTitle + '</label>' + barMarkup + '<div class="status ' + stateClass + '">' + status + '</div>' + '</li>';

    return markup;
  };

  var clearActiveProgressBar = function () {
    var bar = Y.one('#activeProgressBar');
    if (bar != null) {
      bar.remove();
    }
    Y.on('windowresize', function (e) {
      setActiveProgressBarInPlace();
    });
  };

  var setActiveProgressBarInPlace = function () {
    var bar = Y.one('#activeProgressBar');
    var barContainer = Y.one('#activeProgressBarContainer');
    var marginTop = 3;

    // Puts an active progress bar where the placeholder with the DIV ID of "activeProgressBarSpot" is located.
    // Creates an instance of the active progress bar if one does not already exist
    // so that we can keep reusing it and moving it in place, rather than dynamically rendering it
    // on every successful callback to avoid flickering/disconnect due to animation.
    if (barContainer != null) {
      if (bar == null) {
        Y.one("body").append('<div id="activeProgressBar" class="progress progress-striped active" style="position:absolute;top:-50px;left:0;z-index:99;"><div style="width:100%" class="bar"></div></div>');
        bar = Y.one('#activeProgressBar');
      }
      bar.show();
      if (bar.getX() != barContainer.getX() || bar.getY() != barContainer.getY() + marginTop) {
        bar.setXY([ barContainer.getX(), barContainer.getY() + marginTop ]);
      }
    } else if (bar != null) {
      bar.hide();
    }
  };

  var requestStr = '?clusterName=' + config.context.clusterName + '&txnId=' + config.context.txnId;
  if ("deployUser" in config.context) {
    requestStr += '&deployUser=' + config.context.deployUser;
  }

  var pdpDataSourceContext = {
    source: '/hmc/php/frontend/fetchTxnProgress.php',
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
    success: function (e, pdp) {

      var txnProgressMarkup =
        '<img id=txnProgressLoadingImgId class=loadingImg src=/hmc/images/loading.gif />';

      var noNeedForFurtherPolling = false;
      var txnProgressStatusDivContent = '';
      var txnProgressStatusDivCssClass = '';

      var txnProgress = e.response.meta.progress;

      /* Guard against race conditions where txnProgress is null because the
       * txn hasn't had time to be kicked off yet.
       */
      if (txnProgress) {

        /* The first time we get back meaningful progress data, pause the
         * automatic polling to avoid race conditions where response N+1
         * is made (and returns with fresh data) while request N hasn't
         * yet been fully processed.
         *
         * We'll unpause at the end, after we've performed the rendering
         * of the updated states.
         */
        pdp.pause();

        var txnProgressStates = txnProgress.subTxns || [];
        Y.log(Y.Lang.dump(txnProgressStates));

        txnProgressMarkup = '<ul>';

        var progressStateIndex = 0;

        /* Generate markup for all the "done" states. */
        for (; progressStateIndex < txnProgressStates.length; ++progressStateIndex) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if (txnProgressStateShouldBeSkipped(presentTxnProgressState)) {
            continue;
          }

          /* The first sign of a state that isn't done, and we're outta here. */
          if (presentTxnProgressState.progress != 'COMPLETED') {
            break;
          }

          Y.log('Done loop - ' + progressStateIndex);

          txnProgressMarkup += getTxnElementMarkup
            (presentTxnProgressState.description, 'COMPLETED');

          Y.log("Currently, markup is:" + txnProgressMarkup);
        }

        /* Next, generate markup for the first "in-progress" state. */
        for (; progressStateIndex < txnProgressStates.length; ++progressStateIndex) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if (txnProgressStateShouldBeSkipped(presentTxnProgressState)) {
            continue;
          }

          /* The first state that shouldn't be skipped is marked as being
           * "in-progress", even if presentTxnProgressState.progress is
           * not explicitly set to "IN_PROGRESS".
           *
           * This is to take care of race conditions where the poll to the
           * backend is made at a time when the previous state has
           * "COMPLETED" but the next state hasn't been started yet (which
           * means it's "PENDING") - if we were explicitly looking for
           * "IN_PROGRESS", there'd be nothing to show in this loop and it
           * would run to the end of txnProgressStates hunting for that
           * elusive "IN_PROGRESS", thus not even showing any of the
           * "PENDING" states, causing a momentary jitter in the rendering
           * (see AMBARI-344 for an example).
           */
          Y.log('In-progress/failed - ' + progressStateIndex);

          /* Decide upon what CSS class to assign to the currently-in-progress
           * state - if an error was marked as having been encountered, assign
           * the fitting .txnProgressStateError, else just annoint it with
           * .txnProgressStateInProgress
           */
          var currentProgressState = 'IN_PROGRESS';

          /* The 2 possible indications of error are:
           *
           * a) presentTxnProgressState.progress is 'IN_PROGRESS' but
           *    txnProgress.encounteredError is true.
           * b) presentTxnProgressState.progress is 'FAILED'.
           */
          if ((txnProgress.encounteredError) ||
            (presentTxnProgressState.progress == 'FAILED')) {

            currentProgressState = 'FAILED';
          }

          /* And generate markup for this "in-progress" state. */
          txnProgressMarkup += getTxnElementMarkup
            (presentTxnProgressState.description, currentProgressState);

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

        /* Finally, generate markup for all the "pending" states. */
        for (; progressStateIndex < txnProgressStates.length; ++progressStateIndex) {

          var presentTxnProgressState = txnProgressStates[ progressStateIndex ];

          /* Step over any progress states that don't deserve to be shown. */
          if (txnProgressStateShouldBeSkipped(presentTxnProgressState)) {
            continue;
          }

          Y.log('Pending loop - ' + progressStateIndex);

          txnProgressMarkup += getTxnElementMarkup
            (presentTxnProgressState.description, 'PENDING');

        }

        txnProgressMarkup += '</ul>';

        /* Make sure we have some progress data to show - if not,
         * we'll just show a loading image until this is non-null.
         *
         * The additional check for txnProgress.processRunning is to account
         * for cases where there are no subTxns (because it's all a no-op at
         * the backend) - the loading image should only be shown as long as
         * the backend is still working; after that, we should break out of
         * the loading image loop and let the user know that there was
         * nothing to be done.
         */
        if (txnProgress.subTxns == null) {
          if (txnProgress.processRunning == 0) {
            txnProgressMarkup =
              '<br/>' +
                '<div class="txnNoOpMsg">' +
                'There are no tasks for this transaction.' +
                '</div>' +
                '<br/>';
          }
          else {
            txnProgressMarkup =
              '<img id="txnProgressLoadingImgId" class="loadingImg" src="/hmc/images/loading.gif" />';
          }
        }

        /* We can break this polling cycle in one of 2 ways:
         *
         * 1) If we are explicitly told by the backend that we're done.
         */
        if (txnProgress.processRunning == 0) {

          noNeedForFurtherPolling = true;
          /* Be optimistic and assume that no errors were encountered (we'll
           * get more in touch with reality further below).
           */
          txnProgressStatusDivContent = config.successMessage;
          txnProgressStatusDivCssClass = 'statusOk';
        }

        /* 2) If we encounter an error.
         *
         * Note how this is placed after the previous check, so as to serve
         * as an override in case the backend explicitly told us that we're
         * done, but an error was encountered in that very last progress report.
         */
        if (txnProgress.encounteredError) {

          noNeedForFurtherPolling = true;
          txnProgressStatusDivContent = config.failureMessage;
          txnProgressStatusDivCssClass = 'statusError';
        }
      }

      /* Render txnProgressMarkup before making any decisions about the
       * future state of pdp.
       */
      Y.log('About to generate markup: ' + txnProgressMarkup);
      Y.one('#txnProgressContentDivId').setContent(txnProgressMarkup);
      setActiveProgressBarInPlace();

      /* And before checking out, decide whether we're done with this txn
       * or whether any more polling is required.
       */
      if (noNeedForFurtherPolling) {

        /* We've made all the progress we could have, so stop polling. */
        pdp.stop();

        var txnProgressStatusDiv = Y.one('#txnProgressStatusDivId');

        txnProgressStatusDiv.addClass(txnProgressStatusDivCssClass);
        txnProgressStatusDiv.one('#txnProgressStatusMessageDivId').setContent(txnProgressStatusDivContent);
        txnProgressStatusDiv.show();

        /* Run the post-completion callback. */
        if (txnProgressStatusDivCssClass == 'statusOk') {
          if (config.onSuccess) {
            config.onSuccess(this);
          }
        }
        else if (txnProgressStatusDivCssClass == 'statusError') {
          if (config.onFailure) {
            config.onFailure(this);
          }
        }
      }
      else {
        /* There's still more progress to be made, so unpause. */
        pdp.unPause();
      }

    }.bind(this),

    failure: function (e, pdp) {
      alert('Failed to fetch more progress!');
    }.bind(this)
  };

  this.show = function () {
    // start with a clean slate to clear any mess left by previous invocations
    var statusDiv = Y.one('#txnProgressStatusDivId');
    statusDiv.one('#txnProgressStatusMessageDivId').setContent('');
    statusDiv.one('#txnProgressStatusActionsDivId').setContent('');
    statusDiv.removeClass('statusOk');
    statusDiv.removeClass('statusError');
    statusDiv.hide();
    Y.one('#txnProgressContentDivId').setContent
      ('<ul class="wrapped"><li><img id="txnProgressLoadingImgId" class="loadingImg" src="/hmc/images/loading.gif" /></li></ul>');

    // clear active progress bar if one already exists
    clearActiveProgressBar();

    App.ui.hideLoadingOverlay();

    Y.one("#txnProgressHeader").setContent(config.title);
    Y.one('#blackScreenDivId').show();
    Y.one('#txnProgressCoreDivId').show();

    periodicDataPoller.start();
  };

  this.hide = function () {
    periodicDataPoller.stop();

    Y.one('#txnProgressCoreDivId').hide();
    Y.one('#blackScreenDivId').hide();
  };

  // initialize this object

  periodicDataPoller = new App.io.PeriodicDataPoller(pdpDataSourceContext, pdpResponseHandler);

  var onClick = function (e) {
    switch (e.target.get('id')) {
      case CLOSE_LINK_DOM_ID:
        if (config.onClose) {
          config.onClose();
          if (targetUrl != null) {
            document.location.href = targetUrl;
          }
          this.hide();
        }
        break;
      case SHOW_LOGS_LINK_DOM_ID:

        if (!isLogFetched) {
          errorInfoPanel = App.ui.createInfoPanel('Operation Logs');

          var bodyContent =
            '<img id="errorInfoPanelLoadingImgId" class="loadingImg" src="/hmc/images/loading.gif" />';

          errorInfoPanel.set('bodyContent', bodyContent);
          errorInfoPanel.show();

          Y.io('/hmc/php/frontend/fetchTxnLogs.php?clusterName=' +
            config.context.clusterName + '&txnId=' + config.context.txnId, {

            timeout: App.io.FETCH_LOG_TIMEOUT_MS,
            on: {
              success: function (x, o) {

                var errorInfoJson = null;
                try {
                  errorInfoJson = Y.JSON.parse(o.responseText);
                } catch (e) {
                  alert("JSON Parse failed!");
                  return;
                }

                errorInfoPanelBodyContent =
                  '<pre>' +
                    Y.JSON.stringify(errorInfoJson.logs, null, 4) +
                    '</pre>';

                errorInfoPanel.set('bodyContent', errorInfoPanelBodyContent);

                isLogFetched = true;
              },
              failure: function (x, o) {
                alert("Async call failed!");
              }
            }
          });
        } else {
          errorInfoPanel.show();
        }
        break;
    }
  }.bind(this);

  Y.one('#' + CONTAINER_DOM_ID).delegate('click', onClick, 'a');

};
