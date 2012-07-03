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
include_once '../util/util.php';

$logger = new HMCLogger("obtainNodesInfo");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$lineSeparatorPattern = "HDP-------HDP";

function getCommandLine() {
  global $lineSeparatorPattern;
  $echoCmd = "echo ".$lineSeparatorPattern."$? ;";
  $freeCmd = "free -m | sed \\\"1 d\\\" | awk '{ print \\\$2 }' | sed -n 1p ;";
  $cpuCmd = "grep -c processor /proc/cpuinfo ;";
  $archCmd = "uname -m ;";
  $mntPointCmd = "df -lkh | sed \\\"1 d\\\" | grep -vw \\\"/boot\\\" | grep -vw \\\"/dev\/shm\\\" | grep -vw \\\"/home\\\" | grep -vw \/ | awk '{ print \\\$(NF)}' ; ";
  $osTypeCmd = "if [ -f /usr/bin/lsb_release ] ; then lsb_release -sd | tr '[:upper:]' '[:lower:]' | tr '\\\"' ' ' | awk '{ for(i=1; i<=NF; i++) { print \\\$i; print '\\n'; if ( \\\$i ~ /[0-9]+/ ) { break; } } }' ; "
      . "else cat \\`ls /etc/*release | grep \\\"redhat\|SuSE\\\"\\` | head -1 | awk '{ for(i=1; i<=NF; i++) { print \\\$i; print '\\n'; if ( \\\$i ~ /[0-9]+/ ) { break; } } }' | tr '[:upper:]' '[:lower:]' ; "
      . "fi ; ";
  $osInfoCmd = "if [ -f /usr/bin/lsb_release ] ; then lsb_release -sd | tr '\\\"' ' '; "
      . " else cat \\`ls /etc/*release | grep \\\"redhat\|SuSE\\\"\\` | head -1 ; "
      . " fi; uname -a ; ";
  $ipCmd = "hostname -i ;";
  $publicDnsCmd = "curl --connect-timeout 3 -f -s http://169.254.169.254/latest/meta-data/public-hostname && echo '' || hostname ;";
  $privateDnsCmd = "curl --connect-timeout 3 -f -s http://169.254.169.254/latest/meta-data/local-hostname && echo '' || hostname ;";

  $cmdLine = $freeCmd.$echoCmd.$cpuCmd.$echoCmd.$archCmd.$echoCmd.$mntPointCmd.$echoCmd.$osTypeCmd.$echoCmd.$osInfoCmd.$echoCmd.$ipCmd.$echoCmd.$publicDnsCmd.$echoCmd.$privateDnsCmd.$echoCmd;

  // uncomment following line for demo purposes.
  // $cmdLine = $cmdLine . 'sleep $[ $RANDOM % 5 ]; ';
  return $cmdLine;
}

$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$mySubTxnId = $argv[4];
$parentSubTxnId = $argv[5];
$readFromFile = $argv[6];

$opStatus = "STARTED";
$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$stageName = "obtainNodesInfo";
$cmdLine = getCommandLine();
$hosts = explode(",", $hostsStr);
runPdsh($clusterName, $stageName, $deployUser, $readFromFile, $cmdLine);

$finalOpStatus = "SUCCESS";
$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $finalOpStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

?>
