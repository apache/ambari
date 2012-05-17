// The values of clusterName and freshInstallation come from html/addNodesWizard.php
var addNodesRequestData = {
  "clusterName": clusterName,
  "freshInstall": freshInstallation
};

InstallationWizard.AddNodes.render(addNodesRequestData);

/* Signify that the containing application is ready for business. */
hideLoadingImg();
