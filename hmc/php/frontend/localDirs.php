<?php

// Valid constant names
$rootDir = $GLOBALS["HMC_CLUSTER_PATH"];
$allClustersDir = $rootDir."clusters/";

$SshKeyFileName = "user-ssh-key";
$HostsFileName = "hosts.txt";

function getAllClustersDir(){
  global $allClustersDir;
  return $allClustersDir;
}

function getClusterDir($clusterId) {
  if (!isset($clusterId) || $clusterId == "") {
    error_log("**** Got invalid clusterId **** ");
    error_log(print_r(debug_backtrace(), true));
  }
  global $allClustersDir;
  return $allClustersDir.$clusterId."/";
}

function getSshKeyFilePath($clusterId) {
  global $SshKeyFileName;
  $clusterDir = getClusterDir($clusterId);
  return $clusterDir.$SshKeyFileName;
}

function getHostsFilePath($clusterId) {
  global $HostsFileName;
  $clusterDir = getClusterDir($clusterId);
  return $clusterDir.$HostsFileName;
}

function rrmdir($dir) {
  foreach(glob($dir . '/*') as $file) {
    if(is_dir($file))
      rrmdir($file);
    else
      unlink($file);
  }
  rmdir($dir);
}

?>
