// The values of clusterName and freshInstall come from html/addNodesWizard.php
var addNodesRequestData = {
  "clusterName": clusterName,
  "freshInstall": freshInstall
};

InstallationWizard.AddNodes.render(addNodesRequestData);

/* Signify that the containing application is ready for business. */
hideLoadingImg();
