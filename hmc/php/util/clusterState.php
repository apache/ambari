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


define ("HMC_CLUSTER_STATE_LOCK_FILE_SUFFIX", "clusterstate");
// initial setup
function updateClusterState ($clusterName, $state, $displayName, $context) {
  $logger = new HMCLogger("ClusterState");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  $stateObj = array (
                  'state' => $state,
                  'displayName' => $displayName,
                  'timeStamp' => time(),
                  'context' => $context
                );

  $stateStr = json_encode($stateObj);
  $retval = $dbAccessor->setClusterState($clusterName, $stateStr);

  $logger->log_info("Update Cluster State with ".$stateStr);

  return $retval;
}

function needWipeOut ($clusterName) {
  $logger = new HMCLogger("ClusterState");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);
  $clusterStatus = $dbAccessor->getClusterStatus($clusterName);
  return $clusterStatus;
}

?>
