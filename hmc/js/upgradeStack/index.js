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

  App.ui.showLoadingOverlay();

  function renderPage(data) {
    $('#versionInfo').html(
      'You are upgrading your Hadoop stack (HDP) from Version ' + data.versionInfo.currentStackVersion +
      ' to ' + data.versionInfo.latestStackVersion + '.'
    );
    App.ui.hideLoadingOverlay();
  }

  // render the page
  $.ajax({
    type: 'GET',
    url: '/hmc/php/frontend/fetchClusterServices.php?clusterName=' + App.props.clusterName,
    data: {},
    timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
    success: function (data) {
      renderPage(data.response);
    },
    error: function (data) {
      alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
    },
  });

})();