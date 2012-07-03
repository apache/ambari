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

InstallationWizard.AddNodes = {

  renderData:
    {},

  render:
    function (addNodesRenderData) {

      /* Always update the object's renderData first. */
      InstallationWizard.AddNodes.renderData = addNodesRenderData;

      /* Since this screen is completely statically rendered, nothing else
       * needs to be done here.
       */
      if (globalYui.one("#yumMirrorSupportFormButtonId")) {
        if (globalYui.one("#yumMirrorSupportFormButtonId").get('checked')) {
          globalYui.one('#yumMirrorSupportFormFieldsId').setStyle('display', 'block');
        } else {
          globalYui.one('#yumMirrorSupportFormFieldsId').setStyle('display', 'none');
          globalYui.one("#yumRepoFilePathId").set('value', '');
        }
      }
      globalYui.one("#addNodesCoreDivId").setStyle('display', 'block');

      hideLoadingImg();
    }
};

if (globalYui.one("#yumMirrorSupportFormButtonId")) {
  globalYui.one("#yumMirrorSupportFormButtonId").on('click', function(e) {
    if (globalYui.one("#yumMirrorSupportFormButtonId").get('checked')) {
      globalYui.one('#yumMirrorSupportFormFieldsId').setStyle('display', 'block');
    } else {
      globalYui.one('#yumMirrorSupportFormFieldsId').setStyle('display', 'none');
    }
  });
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
      message += '. ';
    }
    message += 'SSH Private Key File not specified';
    globalYui.one("#clusterDeployUserIdentityFileId").addClass('formInputError');
  } else {
    globalYui.one("#clusterDeployUserIdentityFileId").removeClass('formInputError');
  }

  fileName = globalYui.one("#clusterHostsFileId").get('value');
  if (fileName == '') {
    errCount++;
    if (focusId == '') {
      focusId = '#clusterHostsFileId';
    }
    if (message != '') {
      message += '. ';
    }
    message += 'Hosts File not specified';
    globalYui.one("#clusterHostsFileId").addClass('formInputError');
  } else {
    globalYui.one("#clusterHostsFileId").removeClass('formInputError');
  }

  if (globalYui.one("#yumMirrorSupportFormButtonId")) {
    if (globalYui.one("#yumMirrorSupportFormButtonId").get('checked')) {
      // local yum mirror support
      var repoFile = globalYui.Lang.trim(globalYui.one("#yumRepoFilePathId").get('value'));
      if (repoFile == '') {
        errCount++;
        if (focusId == '') {
          focusId = '#yumRepoFilePathId';
        }
        if (message != '') {
          message += '. ';
        }
        message += 'Yum Repo file not specified';
        globalYui.one("#yumRepoFilePathId").addClass('formInputError');
      }
    }
  }

  if (errCount != 0) {
    globalYui.one(focusId).focus();
    setFormStatus(message, true);
    return;
  }

  clearFormStatus();

  showLoadingImg();

  globalYui.log("About to upload files.");
  e.target.set('disabled', true);

  var addNodesFilesForm = globalYui.one("#addNodesFilesFormId");

  addNodesFilesForm.set('action', '../php/frontend/addNodes.php?clusterName=' +
    InstallationWizard.AddNodes.renderData.clusterName);

  /* Set the target of the first form's upload to be a hidden iframe
   * on the page so as not to redirect to the PHP page we're POSTing
   * to.
   *
   * See http://www.openjs.com/articles/ajax/ajax_file_upload/ for
   * more on this.
   */
  addNodesFilesForm.set('target', 'fileUploadTarget');

  /* And then programmatically submit the first of the 2 forms. */
  doPostUpload = true;
  addNodesFilesForm.submit();
  globalYui.log("Files submitted to server.");

  e.target.set('disabled', false);
});

var setupNodesJson = "";
var doPostUpload = false; // this flag is to prevent the #fileUploadTargetId iframe onload event from being invoked on browser back action

globalYui.one("#fileUploadTargetId").on('load', function (e) {

    if (!doPostUpload) {
  	  return;
    }
    globalYui.log("File upload finished");

    if (freshInstall == false) {
      // Do checks only in case of addNodesWizard
      var myIFrame = globalYui.one("#fileUploadTargetId"); 
      var myIFrameContent = myIFrame.get('contentWindow.document.body');
      var content = myIFrameContent.one('pre:first-child');
      var responseText = content.get('text');

      var responseJson = globalYui.JSON.parse(responseText);

      if (responseJson.result != 0) {
        // This means we hit an error
      if (responseJson.result == 3) {
        info =
          '<p>' +
            responseJson.error + '. ' +
            '<a href="javascript:void(null)" id=errorHostInfoLinkId>' +
            'Show me the duplicates</a>' +
          '</p>';

          setFormStatus(info, true);
          var infoPanel = createInformationalPanel("#informationalPanelContainerDivId", "Duplicate nodes");
          infoPanel.set('centered', true);
          var infoPanelContent = '';
          for (cluster in responseJson.hosts) {
            infoPanelContent += 'Cluster: <b>' + cluster + '</b><ul>';
            for (host in responseJson.hosts[cluster]) {
              infoPanelContent += '<li>' + responseJson.hosts[cluster][host] + '</li>';
            }

            infoPanelContent += '</ul><br/>';
          }
          infoPanel.set('bodyContent', infoPanelContent);
          infoPanel.addButton({
            value: 'Close',
            action: function(e) {
              e.preventDefault();
              destroyInformationalPanel(infoPanel);
            },

            className: '',
            section: 'footer'
          });

          globalYui.one('#errorHostInfoLinkId').on("click", function(e) {
            infoPanel.show();
          });
          hideLoadingImg();
          return;

      } else {
        alert('Got and error ' + responseJson.error);
        hideLoadingImg();
        return;
      }
      }
    }

    doPostUpload = false;
    
    var repoFile = '';
    var localYumRepo = '';

    if (globalYui.one("#yumMirrorSupportFormButtonId")) {
      if (globalYui.one("#yumMirrorSupportFormButtonId").get('checked')) {
        localYumRepo = 'true';
        // local yum mirror support
        repoFile = globalYui.Lang.trim(globalYui.one("#yumRepoFilePathId").get('value'));
      }
    }

    var addNodesRequestData = {
      "ClusterDeployUser" : globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value')),
      "useLocalYumRepo" : localYumRepo,
      "yumRepoFilePath": repoFile
    }

    // Trigger the execution of setting up nodes
    var url = "../php/frontend/nodesAction.php?clusterName=" + InstallationWizard.AddNodes.renderData.clusterName + "&action=addNodes";
    globalYui.io(url, {
      method: 'POST',
      data: addNodesRequestData,
      timeout : 10000,
      on: {
        success: function (x,o) {
          	
          globalYui.log("RAW JSON DATA: " + o.responseText);
          // Process the JSON data returned from the server
          try {
            setupNodesJson = globalYui.JSON.parse(o.responseText);
          } catch (e) {
            alert("JSON Parse failed!");
            return;
          }
          globalYui.log("PARSED DATA: " + globalYui.Lang.dump(setupNodesJson));
          if (setupNodesJson.result != 0) {
            // Error!
            alert("Got error! " + setupNodesJson.error);
            hideLoadingImg();
            return;
          }
          setupNodesJson = setupNodesJson.response;


          hideLoadingImg();

          globalYui.one("#blackScreenDivId").setStyle("display", "block");

          renderProgress( setupNodesJson );

        },
        failure: function (x,o) {
          alert("Async call failed!");
        }
      }
    });

});
