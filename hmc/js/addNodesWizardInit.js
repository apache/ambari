// The values of clusterName and freshInstallation come from html/addNodesWizard.php
var addNodesRequestData = {
  "clusterName": clusterName,
  "freshInstall": freshInstallation
};

InstallationWizard.AddNodes.render(addNodesRequestData);
