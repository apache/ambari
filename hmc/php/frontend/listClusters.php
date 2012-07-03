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
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

$logger = new HMCLogger("ListClusters");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$listOfClusters = array();

$allClustersResult = $dbAccessor->getAllClusters();

header("Content-type: application/json");

$response = array( "result" => 0, "error" => "" );

// Errors
if ($allClustersResult["result"] != 0) {
  print(json_encode($allClustersResult));
  exit(1);
}

foreach($allClustersResult["clusters"] as $clusterId => $value) {
  $logger->log_debug("Got cluster ".$clusterId.json_encode($value));
  $listOfClusters[$value["clusterName"]] = $value["state"];
}

$response["response"] = $listOfClusters;
print (json_encode($response));

?>
