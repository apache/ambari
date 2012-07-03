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
include_once '../util/clusterState.php';

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

function deployPostProcess($clusterName, $user, $txnId, $progress)
{

  $logger = new HMCLogger("DeployPostProcess");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  $result = 0;
  $error = "";

  $txnStatus = !($progress['encounteredError']);
  $state = "DEPLOYED";
  if ($txnStatus) {
    $displayName = "Deployed successfully";
  } else {
    $displayName = "Deploy failed";
  }

  $context = array (
    'status' => $txnStatus,
    'txnId' => $txnId
  );

  // update state of the cluster 
  $retval = updateClusterState($clusterName, $state, $displayName, $context);
  if ($retval['result'] != 0) {
    $logger->log_error("Update cluster state failed");
    $result = $retval['result'];
    $error = $retval['error'];
  }

  return (array("result" => $result, "error" => $error));
}

?>
