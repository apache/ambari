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
      globalYui.one("#addNodesCoreDivId").setStyle('display', 'block');
    }
};

globalYui.one('#addNodesSubmitButtonId').on('click',function (e) {

  var focusId = '';
  var message = '';
  var errCount = 0;

  var userId = globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value'));
  if (userId == '') {
    errCount++;
    focusId = '#clusterDeployUserId';
    message += 'Cluster Deploy User cannot be empty';
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
    message += 'User Identity file not specified';
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
      message += ',';
    } 
    message += 'Hosts file not specified';
    globalYui.one("#clusterHostsFileId").addClass('formInputError');
  } else {
    globalYui.one("#clusterHostsFileId").removeClass('formInputError');
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
    InstallationWizard.AddNodes.renderData.clusterName + "&freshInstall=" + InstallationWizard.AddNodes.renderData.freshInstall);

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
});

var setupNodesJson = "";

globalYui.one("#fileUploadTargetId").on('load', function (e) {

    globalYui.log("File upload finished");

    var addNodesRequestData = {
      "ClusterDeployUser" : globalYui.Lang.trim(globalYui.one("#clusterDeployUserId").get('value'))
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
            alert("Got error!" + setupNodesJson.error); 
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
