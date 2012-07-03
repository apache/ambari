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

function getWipeOutConfirmation () {
  var selections = globalYui.all("#confirmWipeOutDivId input[type=checkbox]");

  var wipeOutChecked = false;
  selections.each( function(selection) {
    wipeOutChecked = selection.get('checked');
  });

  if (wipeOutChecked) {
    return "wipeOut";
  } else {
    return "uninstall";
  }

}

globalYui.one('#addNodesSubmitButtonId').on('click',function (e) {

  var focusId = '';
  var message = '';
  var errCount = 0;

  var userId = globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value'));
  if (userId == '') {
    errCount++;
    focusId = '#clusterDeployUserId';
    message += 'SSH Username cannot be empty';
    globalYui.one("#clusterDeployUserId").addClass('formInputError');
  } else {
    globalYui.one("#clusterDeployUserId").removeClass('formInputError');
  }

  var fileName = globalYui.one("#clusterDeployUserIdentityFileId").get('value');
  if (fileName == '') {
    errCount++;
    if (focusId == '') {
      focusId = '#clusterDeployUserIdentityFileId';
    }
    if (message != '') {
      message += ',';
    } 
    message += 'SSH Private Key File not specified';
    globalYui.one("#clusterDeployUserIdentityFileId").addClass('formInputError');
  } else {
    globalYui.one("#clusterDeployUserIdentityFileId").removeClass('formInputError');
  }

  if (nodesAction != "uninstall") {
    fileName = globalYui.one("#clusterHostsFileId").get('value');
    if (fileName == '') {
      errCount++;
      if (focusId == '') {
        focusId = '#clusterHostsFileId';
      }
      if (message != '') {
        message += ',';
      } 
      message += 'Hosts File not specified';
      globalYui.one("#clusterHostsFileId").addClass('formInputError');
    } else {
      globalYui.one("#clusterHostsFileId").removeClass('formInputError');
    }
  }

  if (errCount != 0) {
    globalYui.one(focusId).focus();
    setFormStatus(message, true);
    return;
  }

  clearFormStatus();
  
  var doWipeout = globalYui.one('#confirmWipeOutCheckId').get('checked');
  var warningMessage = doWipeout ? "All your data, in addition to services, will be deleted from all your cluster nodes.  Are you sure you want to proceed?" : "All your services will be deleted from all your cluster nodes.  Your data will not be deleted.  Are you sure you want to proceed?";
  
  var confirmPanel = createInformationalPanel('#informationalPanelContainerDivId','Uninstall Cluster');
  confirmPanel.set('centered', true);
  confirmPanel.set('bodyContent', warningMessage);
  confirmPanel.addButton({
    value: 'Cancel',
    action: function (e) {
      e.preventDefault();
      destroyInformationalPanel(confirmPanel);
    },
    classNames: '',
    section: 'footer'
  });
  confirmPanel.addButton({
    value: 'Proceed with Uninstall',
    action: function (e) {
      e.preventDefault();
      destroyInformationalPanel(confirmPanel);
      showLoadingImg();

      globalYui.log("About to upload files.");
      e.target.set('disabled', true);

      var addNodesFilesForm = globalYui.one("#addNodesFilesFormId");

      addNodesFilesForm.set('action', '../php/frontend/addNodes.php?clusterName=' + 
        clusterName);

      /* Set the target of the first form's upload to be a hidden iframe 
       * on the page so as not to redirect to the PHP page we're POSTing 
       * to.
       *
       * See http://www.openjs.com/articles/ajax/ajax_file_upload/ for 
       * more on this.
       */
      addNodesFilesForm.set('target', 'fileUploadTarget');

      /* And then programmatically submit the first of the 2 forms. */ 
      addNodesFilesForm.submit();
      globalYui.log("Files submitted to server.");

      e.target.set('disabled', false);
    },
    classNames: 'okButton',
    section: 'footer'
  });

  confirmPanel.show();
  
});

var setupNodesJson = "";

globalYui.one("#fileUploadTargetId").on('load', function (e) {

    e.target.set('disabled', true);

    var action = getWipeOutConfirmation();

    var uninstallRequestData = { };

    var userId = globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value'));
    var url = "../php/frontend/uninstall.php?clusterName=" + clusterName + "&action=" + action + "&clusterDeployUser=" + userId;
    var requestData = uninstallRequestData;
    var submitButton = e.target;
    var thisScreenId = "#addNodesCoreDivId";
    var nextScreenId = "#txnProgressCoreDivId";
    var nextScreenRenderFunction = renderUninstallProgress;
    submitDataAndProgressToNextScreen(url, requestData, submitButton, thisScreenId, nextScreenId, nextScreenRenderFunction);
});

/* Main() */
hideLoadingImg();
