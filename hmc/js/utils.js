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

/*
 * Allows 'this' to be bound statically.
 * Primarily used when creating objects whose methods will be used as
 * callbacks in unknown contexts.
 */
Function.prototype.bind = function (scope) {
  var _function = this;

  return function () {
    return _function.apply(scope, arguments);
  };
};

// Define the App namespace
var App = App || {
  props: {},
  io: {},
  ui: {},
  util: {}
};

// Define App.props package
App.props = {
  managerServiceName: 'Ambari',
  clusterName: '',
  homeUrl: '/hmc/html/'
};

// On some pages the clusterName global variable is set before
// this file is loaded.  Remember it in the App namespace.
if (typeof clusterName !== 'undefined') {
  App.props.clusterName = clusterName;
}

// Define App.ui package
(function() {

  var _infoPanel = null;

  function createInfoPanel(headerContent) {

    if (_infoPanel) {
      _infoPanel.hide();
      _infoPanel.destroy();
    }

    var containerDiv = Y.one('#informationalPanelContainerDivId');
    containerDiv.setHTML('<div id="informationalPanelInnerContainerDivId"></div>');
    //Y.one('#informationalPanelContainerDivId').append();

    _infoPanel = new Y.Panel({
      srcNode: '#informationalPanelInnerContainerDivId',
      headerContent: headerContent,
      width: 800,
      height: 400,
      render: true,
      modal: true,
      zIndex: 100,
      centered: true,
      visible: false
    });
    return _infoPanel;
  }

  // for now, there can be only one instance of info panel,
  // so the input parameter is ignored
  function destroyInfoPanel(infoPanel) {
    if (_infoPanel) {
      _infoPanel.hide();
      _infoPanel.destroy();
      _infoPanel = null;
    }
  }

  function showLoadingOverlay() {
    Y.one("#loadingDivId").show();
  }

  function hideLoadingOverlay() {
    Y.one("#loadingDivId").hide();
  }

  function setFormStatus(statusString, isError, noFade) {
    var formStatusDivCssClass;
    if (isError) {
      formStatusDivCssClass = 'statusError';
    } else {
      formStatusDivCssClass = 'statusOk';
    }
    var formStatusDiv = Y.all("#formStatusDivId");
    formStatusDiv.setStyle("visibility", "visible");
    formStatusDiv.show();
    formStatusDiv.set('className', '');
    formStatusDiv.addClass("formStatusBar");
    formStatusDiv.addClass(formStatusDivCssClass);
    formStatusDiv.setContent(statusString);
  }

  function clearFormStatus() {
    var formStatusDiv = Y.all("#formStatusDivId");
    formStatusDiv.setStyle("visibility", "hidden");
    formStatusDiv.hide();
    formStatusDiv.set('className', '');
    formStatusDiv.addClass("formStatusBar");
  }

  // exports
  App.ui = {
    createInfoPanel: createInfoPanel,
    destroyInfoPanel: destroyInfoPanel,
    showLoadingOverlay: showLoadingOverlay,
    hideLoadingOverlay: hideLoadingOverlay,
    setFormStatus: setFormStatus,
    clearFormStatus: clearFormStatus
  };
})();

// Define App.io package
(function() {

  function PeriodicDataPoller(dataSourceContext, responseHandler) {

    this.dataSourceContext = dataSourceContext;

    /* Smoothe out the optional bits of this.dataSourceContext. */
    if (!this.dataSourceContext.pollInterval) {
      /* How often we poll. */
      this.dataSourceContext.pollInterval = DEFAULT_POLLING_INTERVAL_MS;
    }
    if (!this.dataSourceContext.maxFailedAttempts) {
      /* How many failed attempts before we stop polling. */
      this.dataSourceContext.maxFailedAttempts = 25;
    }

    this.responseHandler = responseHandler;

    /* Of course, we're not paused when we start off. */
    this.paused = false;

    this.dataSource = new Y.DataSource.IO({
      source: this.dataSourceContext.source
    });

    this.dataSource.plug(Y.Plugin.DataSourceJSONSchema, {
      schema: this.dataSourceContext.schema
    });

    this.dataSourcePollFailureCount = 0;

    /* Set when start() is invoked. */
    this.dataSourcePollHandle = null;

    this.dataSourcePollRequestContext = {
      request: this.dataSourceContext.request,
      callback: {
        success: function (e) {
          /* Avoid race conditions in JS by not processing incoming responses
           * from the backend if the PDP is paused (which is our signal that
           * a previous response is still in the middle of being processed).
           */
          if (!(this.isPaused())) {
            /* Reset our failure count every time we succeed. */
            this.dataSourcePollFailureCount = 0;

            /* Invoke user-pluggable code. */
            if (this.responseHandler.success) {
              this.responseHandler.success(e, this);
            }
          }
        }.bind(this),

        failure: function (e) {
          if (!(this.isPaused())) {
            ++this.dataSourcePollFailureCount;

            if (this.dataSourcePollFailureCount > this.dataSourceContext.maxFailedAttempts) {

              /* Invoke user-pluggable code. */
              if (this.responseHandler.failure) {
                this.responseHandler.failure(e, this);
              }

              /* No point making any more attempts. */
              this.stop();
            }
          }
        }.bind(this)
      }
    };
  }

  /* Start polling. */
  PeriodicDataPoller.prototype.start = function () {

    this.dataSourcePollHandle = this.dataSource.setInterval
      (this.dataSourceContext.pollInterval, this.dataSourcePollRequestContext);
  };

  /* Stop polling. */
  PeriodicDataPoller.prototype.stop = function () {

    /* Always unPause() during stop(), so the next start() won't be neutered. */
    this.unPause();
    this.dataSource.clearInterval(this.dataSourcePollHandle);
  };

  /* When the PDP is paused, the polling continues on its regular fixed
   * interval, but this.responseHandler is not invoked, thus avoiding
   * a race condition (at least) in JS.
   *
   * TODO XXX Improve upon this to not even make calls to the backend
   * while not losing our periodicity.
   */
  PeriodicDataPoller.prototype.pause = function () {

    this.paused = true;
  };

  PeriodicDataPoller.prototype.unPause = function () {

    this.paused = false;
  };

  PeriodicDataPoller.prototype.isPaused = function () {

    return this.paused;
  };

  /* Perform a one-time poll.
   *
   * Meant to be used when the polling is not at a set frequency (as with the
   * start()/stop() pair), and is instead meant to be under explicit
   * control of the application.
   */
  PeriodicDataPoller.prototype.pollOnce = function () {

    Y.io(this.dataSourceContext.source + this.dataSourcePollRequestContext.request, {
      on: this.dataSourcePollRequestContext.callback
    });
  };

  // exports
  App.io = {
    PeriodicDataPoller: PeriodicDataPoller,
    DEFAULT_AJAX_TIMEOUT_MS: 10000,
    FETCH_LOG_TIMEOUT_MS: 20000,
    DEFAULT_POLLING_INTERVAL_MS: 5000,
    DEFAULT_AJAX_ERROR_MESSAGE: 'Failed to retrieve information from the server'
  };

})();

// define App.util package
(function() {

  function titleCase(word) {
    return word.substr(0, 1).toUpperCase() + word.substr(1).toLowerCase();
  }

  // exports
  App.util = {
    titleCase: titleCase
  };

})();

// Define App.transition package
(function() {

  function transitionToNextStage(currentStageDivSelector, currentStageData, newStageDivSelector, newStageData, newStageRenderFunction) {

    App.ui.clearFormStatus();

    Y.one(currentStageDivSelector).hide();

    /* Render the next stage. */
    newStageRenderFunction(newStageData);

    Y.log("In transitionToNextStage: " + currentStageDivSelector + "->" + newStageDivSelector);

    /* There can be only one 'current' stage at a time. */
    var currentStage = Y.one('.installationWizardCurrentStage');

    if (currentStage) {
      var nextStage = null;

      /* Check to make sure we haven't reached the last stage. */
      if (nextStage = currentStage.next('.installationWizardUnvisitedStage')) {

        /* Mark this up-until-now 'current' stage as 'visited'. */
        currentStage.replaceClass('installationWizardCurrentStage', 'installationWizardVisitedStage');

        /* Mark the stage after that as the new 'current' stage. */
        nextStage.replaceClass('installationWizardUnvisitedStage', 'installationWizardCurrentStage');
      }
    }
  }

  function swapStageVisibilities(currentStageDivSelector, newStageDivSelector) {

    Y.log("In swapStageVisibilities: " + currentStageDivSelector + "->" + newStageDivSelector);
    /* Hide the current stage. */
    Y.one(currentStageDivSelector).hide();

    /* Show the new stage. */
    Y.one(newStageDivSelector).show();
  }

  function executeStage(inputUrl, renderStageFunction) {
    Y.io(inputUrl, {
      method: 'GET',
      timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
      on: {
        success: function (x, o) {
          var responseJson;

          Y.log("RAW JSON DATA: " + o.responseText);
          // Process the JSON data returned from the server
          try {
            responseJson = Y.JSON.parse(o.responseText);
          }
          catch (e) {
            App.ui.hideLoadingOverlay();
            alert("JSON Parse failed!");
            return;
          }

          Y.log("PARSED DATA: " + Y.Lang.dump(responseJson));

          if (responseJson.result != 0) {
            App.ui.hideLoadingOverlay();
            // Error!
            alert("Got error during getting data: " + responseJson.error);
            return;
          }
          responseJson = responseJson.response;
          renderStageFunction(responseJson);
          App.ui.hideLoadingOverlay();
          return;
        },
        failure: function (x, o) {
          alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
          return;
        }
      }
    });
  }

  function submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction, errorHandlerFunction) {
    App.ui.showLoadingOverlay();

    Y.io(url, {

      method: 'POST',
      data: Y.JSON.stringify(requestData),
      timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
      on: {
        start: function (x, o) {
          submitButton.set('disabled', true);
          Y.log("In start function");
          // App.ui.showLoadingOverlay();
        },
        complete: function (x, o) {
          submitButton.set('disabled', false);
          Y.log("In stop function");
          // App.ui.hideLoadingOverlay();
        },
        success: function (x, o) {
          var responseJson;

          submitButton.set('disabled', false);
          Y.log("RAW JSON DATA: " + o.responseText);

          // Process the JSON data returned from the server
          try {
            responseJson = Y.JSON.parse(o.responseText);
          }
          catch (e) {
            submitButton.set('disabled', false);
            App.ui.hideLoadingOverlay();
            alert("JSON Parse failed!");
            return;
          }

          Y.log("PARSED DATA: " + Y.Lang.dump(responseJson));

          if (responseJson.result != 0) {
            submitButton.set('disabled', false);
            // Error!
            Y.log("Got error during submit data!" + responseJson.error);
            if (errorHandlerFunction) {
              Y.log("Invoking error handler function");
              errorHandlerFunction(responseJson);
            } else {
              alert("Got error during submit data!" + responseJson.error);
            }
            App.ui.hideLoadingOverlay();
            return;
          }
          responseJson = responseJson.response;

          /* Done with this stage, transition to the next. */
          transitionToNextStage(thisScreenId, requestData, nextScreenId, responseJson, nextScreenRenderFunction);
        },
        failure: function (x, o) {
          submitButton.set('disabled', false);
          alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
        }
      }
    });
  }

  // exports
  App.transition = {
    transitionToNextStage: transitionToNextStage,
    swapStageVisibilities: swapStageVisibilities,
    executeStage: executeStage,
    submitDataAndProgressToNextScreen: submitDataAndProgressToNextScreen
  };
})();

