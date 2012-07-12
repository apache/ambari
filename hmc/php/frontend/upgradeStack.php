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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "../util/clusterState.php";
include_once '../util/util.php';

include_once './commandUtils.php';
/*
sleep(3);
 */

$logger = new HMCLogger("UpgradeStack");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];

// Remove the cluster directory before copying the files
$clusterDir = getClusterDir($clusterName);
rrmdir($clusterDir);
if (!is_dir($clusterDir) && !mkdir($clusterDir, 0700, true)) {
  print json_encode(array( "result" => 1, "error" => 'Failed to create directory...'));
  return;
}
// end of directory removal

$keyFileDestination = getSshKeyFilePath($clusterName);

if (move_uploaded_file($_FILES['sshPrivateKeyFile']['tmp_name'], $keyFileDestination)) {
    //echo "File is valid, and was successfully uploaded.\n";

  // Set the permissions
  chmod($keyFileDestination, 0400);
  // TODO: Error conditions
} else {
  $logger->log_error("SSH key file copy to destination ".$keyFileDestination." failed");
}

$resp = updateClusterState($clusterName, 'UPGRADE_STACK_STARTED', 'Upgrade Stack - Started', array());
if ($resp['result'] != 0) {
  print json_encode($resp);
  return;
}
$clusterState = json_decode($resp['state'], true);
$dbAccessor->

header("Content-type: application/json");

print (json_encode(array("result" => 0)));

?>
