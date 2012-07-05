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

/* Allows 'this' to be bound statically.
 *
 * Primarily used when creating objects whose methods will be used as
 * callbacks in unknown contexts.
 */
Function.prototype.bind = function (scope) {
  var _function = this;

  return function () {
    return _function.apply(scope, arguments);
  };
};

var globalSingletonInformationalPanel;

function createInformationalPanel(containerNodeId, headerContentString) {

  /* XXX This should check that globalSingletonInformationalPanel is within
   * containerNodeId, and only then perform this cleanup, but this whole
   * panel-related section needs to be rewritten anyway - for now, we only
   * support the one globalSingletonInformationalPanel, and passing in
   * anything other than #informationalPanelContainerDivId as containerNodeId
   * is not guaranteed to work.
   */
  if (globalSingletonInformationalPanel) {
    destroyInformationalPanel(globalSingletonInformationalPanel);
  }

  Y.one(containerNodeId).append('<div id="informationalPanelInnerContainerDivId"></div>');
  
  var newPanel = new Y.Panel({
    srcNode: '#informationalPanelInnerContainerDivId',
    headerContent: headerContentString,
    width: 800,
    height: 400,
    render: true,
    modal: true,
    zIndex: 100,
    centered: true,
    visible: false
  });

  globalSingletonInformationalPanel = newPanel;

  return newPanel;
}

function createInfoPanel(headerContent) {
  return createInformationalPanel('#informationalPanelContainerDivId', headerContent);
}

function destroyInformationalPanel(theInformationalPanelInstance) {

  if (theInformationalPanelInstance) {

    theInformationalPanelInstance.hide();
    theInformationalPanelInstance.destroy();

    if (theInformationalPanelInstance === globalSingletonInformationalPanel) {
      globalSingletonInformationalPanel = null;
    }
  }
}

function showLoadingImg() {
  Y.one("#loadingDivId").show();
}

function hideLoadingImg() {
  Y.one("#loadingDivId").hide();
}

function swapStageVisibilities(currentStageDivSelector, newStageDivSelector) {

  Y.log("In swapStageVisibilities: " + currentStageDivSelector + "->" + newStageDivSelector);
  /* Hide the current stage. */
  Y.one(currentStageDivSelector).hide();

  /* Show the new stage. */
  Y.one(newStageDivSelector).show();
}

/* TODO XXX Consider bundling the last 3 parameters into their own NewStage object.
 * TODO XXX Do the same for the first 2 parameters and a CurrentStage object.
 */
function transitionToNextStage(currentStageDivSelector, currentStageData, newStageDivSelector, newStageData, newStageRenderFunction) {

  clearFormStatus();

  Y.one(currentStageDivSelector).hide();

  /* Render the next stage. */
  newStageRenderFunction(newStageData);

  Y.log("In transitionToNextStage: " + currentStageDivSelector + "->" + newStageDivSelector);

  //// tshooter: No longer doing this given dynamic rendering on stages. Only hide current stage.
  /* And make it visibly replace the currently showing one. */
  ///// tshooter: commented: swapStageVisibilities(currentStageDivSelector, newStageDivSelector);

  /* And now, handle the updates to addNodesWizardStages... */

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

function clearFormStatus() {
  var formStatusDiv = Y.all("#formStatusDivId");
  // formStatusDiv.setContent("");
  formStatusDiv.setStyle("visibility", "hidden");
  formStatusDiv.hide();
  formStatusDiv.set('className', '');
  formStatusDiv.addClass("formStatusBar");
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
  if (!isError && !noFade) {
    //setTimeout(fadeFormStatus, 1000);
  }
}

function fadeFormStatus() {
  var formStatusDiv = Y.one("#formStatusDivId");
  formStatusDiv.addClass("formStatusBarZeroOpacity");
}

function convertDisplayType(displayType) {
  switch (displayType) {
    case "NODISPLAY":
      return "NODISPLAY";

    case "TEXT":
      return "text";

    case "SECRET":
      return "password";

    case "ONOFF":
      return "checkbox";

    default:
      return "text";
  }
}

function executeStage(inputUrl, renderStageFunction) {
  Y.io(inputUrl, {
    method: 'GET',
    timeout: 10000,
    on: {
      success: function (x, o) {
        Y.log("RAW JSON DATA: " + o.responseText);
        // Process the JSON data returned from the server
        try {
          responseJson = Y.JSON.parse(o.responseText);
        }
        catch (e) {
          hideLoadingImg();
          alert("JSON Parse failed!");
          return;
        }

        Y.log("PARSED DATA: " + Y.Lang.dump(responseJson));

        if (responseJson.result != 0) {
          hideLoadingImg();
          // Error!
          alert("Got error during getting data: " + responseJson.error);
          return;
        }
        responseJson = responseJson.response;
        renderStageFunction(responseJson);
        hideLoadingImg();
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
  showLoadingImg();
  Y.io(url, {

    method: 'POST',
    data: Y.JSON.stringify(requestData),
    timeout: 10000,
    on: {
      start: function (x, o) {
        submitButton.set('disabled', true);
        Y.log("In start function");
        // showLoadingImg();
      },
      complete: function (x, o) {
        submitButton.set('disabled', false);
        Y.log("In stop function");
        // hideLoadingImg();
      },
      success: function (x, o) {
        submitButton.set('disabled', false);
        Y.log("RAW JSON DATA: " + o.responseText);

        // Process the JSON data returned from the server
        try {
          responseJson = Y.JSON.parse(o.responseText);
        }
        catch (e) {
          submitButton.set('disabled', false);
          hideLoadingImg();
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
          hideLoadingImg();
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

function PeriodicDataPoller(dataSourceContext, responseHandler) {

  this.dataSourceContext = dataSourceContext;

  /* Smoothe out the optional bits of this.dataSourceContext. */
  if (!this.dataSourceContext.pollInterval) {
    /* How often we poll. */
    this.dataSourceContext.pollInterval = 5000;
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

function titleCase(word) {
  return word.substr(0, 1).toUpperCase() + word.substr(1).toLowerCase();
}

// Create namespace and export functionality.
// We'll remove globally defined functions and properties eventually
// For now we need to keep the non-namespaced global functions and properties
// so that our existing code continues to work without refactoring.
var App = App || {
  props: {
    managerServiceName: 'Ambari',
    clusterName: '',
    homeUrl: '/hmc/html/'
  },
  io: {
    PeriodicDataPoller: PeriodicDataPoller,
    DEFAULT_AJAX_TIMEOUT_MS: 10000,
    FETCH_LOG_TIMEOUT_MS: 20000,
    DEFAULT_AJAX_ERROR_MESSAGE: 'Failed to retrieve information from the server'
  },
  ui: {
    createInfoPanel: createInfoPanel,
    hideLoadingOverlay: hideLoadingImg
  }
};

App.Props = App.props;
