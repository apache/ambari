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
include_once './configUtils.php';
include_once '../util/suggestProperties.php';
include_once "../util/clusterState.php";

$logger = new HMCLogger("Options");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

// Read from the input
$requestdata = file_get_contents('php://input');
$requestObj = json_decode($requestdata, true);

$clusterName = $_GET['clusterName'];
// TODO: Validate clusterName

$validateOnly = isset($_GET['validateOnly']);

$finalProperties = sanitizeConfigs($requestObj, $logger);

if ($validateOnly) {
  $result = validateConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties);
  if ($result['result'] != 0) {
    $logger->log_error("Failed to validate configs from user (validate only), error=" . $result["error"]);
    print json_encode($result);
    return;
  }
} else {
  $result = validateAndPersistConfigsFromUser($dbAccessor, $logger, $clusterName, $finalProperties);
  if ($result['result'] != 0) {
    $logger->log_error("Failed to validate configs from user (validate and persist), error=" . $result["error"]);
    print json_encode($result);
    return;
  }
}
$jsonOutput = array();
$jsonOutput['clusterName'] = $clusterName;

// Update the state of the cluster.
$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";
$context = array (
  'stage' => "CONFIGURE_SERVICES"
);

$retval = updateClusterState($clusterName, $state, $displayName, $context);
if ($retval['result'] != 0) {
  $result = $retval['result'];
  $error = $retval['error'];
}

print (json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));

?>
