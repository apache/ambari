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

include_once 'commandUtils.php';

function handleUninstallTransaction ($clusterName, $deployUser, $rootTxnId,
                                     $mySubTxnId, $parentSubTxnId, $hostsStr,
                                     $operationName, $cmdLine, $dbAccessor,
                                     $logger) {

  $opStatus = "STARTED";
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }

  runPdsh($clusterName, $operationName, $deployUser, $hostsStr, $cmdLine);

  $retArr = parseAndUpdateNodeInfo($clusterName, $operationName, $logger);

  $allHosts = $retArr["allHosts"];
  $finalOpStatus = $retArr["finalOpStatus"];

  // Perisist the data to the db.
  $returnValue = $dbAccessor->addHostsToCluster($clusterName, $allHosts);
  if ($returnValue["result"] != 0 ) {
    $logger->log_error("Got error while adding hosts: ".$returnValue["error"]);
    print json_encode($returnValue);
    return;
  }

  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $finalOpStatus);
  if ($subTransactionReturnValue["result"] != 0 ) {
    $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
    print json_encode($subTransactionReturnValue);
    return;
  }
}

?>
