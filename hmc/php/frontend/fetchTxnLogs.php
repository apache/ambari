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

include_once "../orchestrator/HMC.php";
include_once "../orchestrator/Service.php";
include_once "../orchestrator/ServiceComponent.php";
include_once "../db/Transaction.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";

$dbPath = $GLOBALS["DB_PATH"];

$clusterName = $_GET['clusterName'];
$txnId = $_GET['txnId'];

function fetchTxnLogs( $txnId )
{
  global $dbPath;
  global $clusterName;

  $hmc = new HMC($dbPath, $clusterName);

  $logs = $hmc->getLogs($txnId);
  $progress = $hmc->getProgress($txnId);



//REZXXX  $logs = '';
//REZXXX
//REZXXX  /* Generate long logs. */
//REZXXX  for($i = 0; $i < 100; $i++) {
//REZXXX    $logs .= "1111 <br/>";
//REZXXX  }

  return array ('logs' => $logs['subTxns'],
                'progress' => $progress['subTxns']);
}

$result = fetchTxnLogs($txnId);

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId,
    'logs' => $result['logs'],
    'progress' => $result['progress']);

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode($jsonOutput));

?>
