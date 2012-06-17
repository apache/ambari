// This is to aid development by jumping to a specified stage on page load
// so that the developer does not have to run through each page to see code
// changes made to the stage (assuming the developer has already run the wizard
// up to that stage and all the necessary data are in the database).
// This is a stop-gap measure until we implement actual state/history management
// so that a browser refresh reloads the current stage, rather than forcing
// the user back to the first stage.
(function() {  
  var clusterName = 'test';
  //var stage ='createCluster';
  //var stage = 'selectServices';
  var stage = 'configureServices';
  Y.one("#createClusterCoreDivId").hide();
  
  var hitCurrentStage = false;
  
  Y.all('#installationWizardProgressBarListId li').each(function(tab) {
    if (tab.get('id') === (stage + 'StageId')) {
      hitCurrentStage = true;
      tab.set('className', 'installationWizardCurrentStage');
    } else if (!hitCurrentStage) {
      tab.set('className', 'installationWizardVisitedStage');
    }
  });

  switch (stage) {
  case 'createCluster':
    globalYui.one("#createClusterCoreDivId").show();
    break;
  case 'selectServices':
    renderSelectServicesBlock({ "clusterName": clusterName, "txnId":1 });
    break;
  case 'assignMasters':
    // TODO
    break;
  case 'selectMountPoints':
    // TODO
    renderConfigureCluster({ "clusterName": clusterName});
    break;
  case 'configureServices':
    renderOptionsPage({ "clusterName": clusterName});
    break;
  default:
    break;
  }
})();
