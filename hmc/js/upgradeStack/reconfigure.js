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


(function() {

  function attachEventHandlers() {
    App.ui.configureServicesUtil.setSubmitButtonSelector('#reconfigureSubmitButton');

    $('#reconfigureSubmitButton').click(function() {

      if ($(this).hasClass('disabled')) {
        return;
      }

      App.ui.showLoadingOverlay();

      var opts = App.ui.configureServicesUtil.generateUserOpts();

      $.ajax({
        type: 'POST',
        url: '/hmc/php/frontend/configureServices.php?clusterName=' + App.props.clusterName,
        data: JSON.stringify(opts),
        timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
        dataType: 'json',
        success: function (data) {
          if (data.result === 0) {
            // server-side validation successful
            // go to Deploy stage
            document.location.href = 'deploy.php';
          } else {
            // server-side validation failed
            App.ui.configureServicesUtil.handleConfigureServiceErrors(data);
            App.ui.hideLoadingOverlay();
          }
        },
        error: function () {
          alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
        }
      });
    });

    // register event handlers for dynamic validation
    // when a key is pressed on a password field, perform password validation
    $('#configureClusterAdvancedDynamicRenderDivId').on(
      'keyup',
      'input[type=password]',
      function () {
        App.ui.configureServicesUtil.checkPasswordCorrectness();
        App.ui.configureServicesUtil.updateServiceErrorCount($(this).attr('name'));
      }
    );
    // when a key is pressed on a text field, just clear the error
    $('#configureClusterAdvancedDynamicRenderDivId').on(
      'keyup',
      'input[type=text],input[type=password]:not(.retypePassword)',
      function () {
        App.ui.configureServicesUtil.clearErrorReason('#' + $(this).attr('id'));
        App.ui.configureServicesUtil.updateServiceErrorCount($(this).attr('name'));
      }
    );
  }

  function renderPage(data) {
    $('#configureClusterAdvancedDynamicRenderDivId').html(App.ui.configureServicesUtil.getOptionsSummaryMarkup(data, false));
    $('#configureServicesTabs a:first').tab('show');
    $('#configureClusterAdvancedCoreDivId').show();
    attachEventHandlers();
    App.ui.hideLoadingOverlay();
  }

  // render the page
  $.ajax({
    type: 'GET',
    url: '/hmc/php/frontend/fetchClusterServices.php?clusterName=' + App.props.clusterName + '&getConfigs=true',
    data: {},
    timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
    success: function (data) {
      renderPage(data.response);
    },
    error: function (data) {
      alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
    }
  });


})();


