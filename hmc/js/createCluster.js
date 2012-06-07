function handleCreateClusterError (errorResponse) {

  globalYui.one("#clusterNameId").addClass('formInputError');
  setFormStatus(errorResponse.error, true);
  globalYui.one("#clusterNameId").focus();
}

globalYui.one('#createClusterSubmitButtonId').on('click',function (e) {

      var createClusterData = {
        "clusterName" : globalYui.Lang.trim(globalYui.one("#clusterNameId").get('value')),
      };

      globalYui.log("Cluster Name: "+globalYui.Lang.dump(createClusterData));

      /* Always clear the slate with each submit. */
      clearFormStatus();
      globalYui.one("#clusterNameId").removeClass('formInputError');

      submitDataAndProgressToNextScreen(
        '../php/frontend/createCluster.php', createClusterData, e.target, 
        '#createClusterCoreDivId', '#addNodesCoreDivId', InstallationWizard.AddNodes.render,
        handleCreateClusterError );
});

/* Signify that the containing application is ready for business. */
hideLoadingImg();

/* At the end of the installation wizard, we hide 
 * #installationWizardProgressBarDivId, so make sure we explicitly show
 * it at the beginning, to ensure we work correctly when user flow 
 * (potentially) cycles back here.
 */
globalYui.one('#installationWizardProgressBarDivId').setStyle('display', 'block');
