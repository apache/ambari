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
include_once './uninstall/uninstallUtil.php';

$logger = new HMCLogger("CleanupProcesses");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function getCommandLine() {
  $cmdLine = " service hmc-agent stop; exit 0";
  return $cmdLine;
}

$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$mySubTxnId = $argv[4];
$parentSubTxnId = $argv[5];
$hostsStr = $argv[6];
// stage name should match corr. stage in stages.php
$stageName = "CleanupProcesses";

$cmdLine = getCommandLine();

handleUninstallTransaction($clusterName, $deployUser, $rootTxnId,
  $mySubTxnId, $parentSubTxnId, $hostsStr,
  $stageName, $cmdLine, $dbAccessor, $logger);

?>
