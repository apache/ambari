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

  $('#upgradeButton').click(function () {

    var message = '';
    var errorCount = 0;

    var fileName = $('#sshPrivateKeyFile').val();
    if (fileName == '') {
      message += 'SSH Private Key File not specified';
      $('#sshPrivateKeyFile').addClass('formInputError');
      errorCount++;
    } else {
      $('#sshPrivateKeyFile').removeClass('formInputError');
    }

    if (errorCount > 0) {
      App.ui.setFormStatus(message, true);
      return;
    }

    App.ui.clearFormStatus();

    var warningMessage = "The current version of your Hadoop stack will be uninstalled first and the new version will be installed after.  This will not delete your data.<br>Are you sure you want to proceed with upgrade?";

    var confirmPanel = App.ui.createInfoPanel('Upgrade Hadoop Stack');
    confirmPanel.set('bodyContent', warningMessage);
    confirmPanel.addButton({
      value: 'Cancel',
      action: function (e) {
        e.preventDefault();
        App.ui.destroyInfoPanel(confirmPanel);
      },
      section: 'footer'
    });
    confirmPanel.addButton({
      value: 'Proceed with Upgrade',
      action: function (e) {
        e.preventDefault();
        App.ui.destroyInfoPanel(confirmPanel);

        App.ui.showLoadingOverlay();

        var form = $('#upgradeStackForm');

        // upgradeStack.php handles ssh key upload and also triggers
        // the upgrade process
        form.attr('action', '/hmc/php/frontend/upgradeStack.php?clusterName=' +
          App.props.clusterName);

        form.attr('target', 'fileUploadTarget');
        form.submit();
      },
      classNames: 'okButton',
      section: 'footer'
    });

    confirmPanel.show();

  });

  // Event handler for when the ssh key file upload is done)
  $('#fileUploadTarget').load(function () {
    document.location.href = 'showUpgradeProgress.php';
  });

  function renderPage(data) {
    $('#versionInfo').html(
      'You will be upgrading your Hadoop stack from HDP ' + data.versionInfo.currentStackVersion +
      ' to HDP ' + data.versionInfo.latestStackVersion + '.'
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
    }
  });

  App.ui.showLoadingOverlay();

})();