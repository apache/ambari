<?php

include 'dbLib.php';
include 'gsInstallerUtils.php';

$listOfClusters = array();
if ($handle = opendir(getAllClustersDir())) {
  while (($clusterId = readdir($handle)) !== FALSE) {
    if ($clusterId != "." && $clusterId != "..") {
         
      // Return clusterId only if the deploy has started
      // if(hasInstallStarted($clusterId)) {
     //     array_push($listOfClusters, $clusterId);
        $status = getClusterStatus($clusterId);
        global $CLUSTER_STATUS_INSTALL_IN_PROGRESS;
        global $CLUSTER_STATUS_INSTALL_DONE;
        global $CLUSTER_STATUS_INSTALL_SUCCESSFUL;
        global $CLUSTER_STATUS_INSTALL_UNSUCCESSFUL;

        if ($status == $CLUSTER_STATUS_INSTALL_IN_PROGRESS) {
           if (isGsInstallDone($clusterId)) {
             setClusterStatus($clusterId, $CLUSTER_STATUS_INSTALL_DONE);
           }
         }

        $listOfClusters[$clusterId] = getClusterStatus($clusterId);
      //  $listOfClusters[$clusterId] = "started";//getClusterStatus($clusterId);
      //}
    }
  }
  closedir($handle);
}

header("Content-type: application/json");
//print (json_encode($listOfClusters));
print (json_encode($listOfClusters));

?>
