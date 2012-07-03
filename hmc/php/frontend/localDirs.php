<?php
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
