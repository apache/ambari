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

include_once "Cluster.php";
include_once "Service.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";
include_once "../util/HMCTxnUtils.php";

$defaultLogger = new HMCLogger("ClusterMain");

function usage() {
  $usageStr = <<<USAGESTR
Usage:
  -c/--cluster-name       Cluster Name
  -x/--txn-id             Transaction ID
  -d/--db-path            DB Path
  -a/--action             Action - deploy, deployNode, startAll, stopAll,
                          uninstallAll, reconfigure, wipeout
  -s/--service            Service - names of services
  -n/--node               Node - hostname of node to take action on
  -r/--dry-run            Dry-run only
  -h/--help               Print usage message
USAGESTR;
  print "\n".$usageStr."\n";
}


/*
$ACTIONS = array (
  "deploy",
  "deployNode",
  "startAll",
  "start",
  "stopAll",
  "stop",
  "installAll",
  "install",
  "uninstallAll",
  "uninstall",
  "reconfigureAll",
  "reconfigure"
);
*/

$ACTIONS = array (
  "deploy",
  "deployNode",
  "startAll",
  "stopAll",
  "uninstallAll",
  "start",
  "stop",
  "reconfigure",
  "wipeout"
);

function validateAndCreateDBHandle($dbPath) {
  global $logger;
  if ($dbPath == "") {
    $logger->log_error("Invalid DB Path provided");
    exit (1);
  }
  if (!file_exists($dbPath)) {
    $logger->log_error("DB Path provided does not exist, dbpath=" . $dbPath);
    exit (1);
  }
  $dbHandle = new HMCDBAccessor($dbPath);
  if (isset($dbHandle)) {
    return $dbHandle;
  }
  $logger->log_error("Could not initialize handle to DB, dbPath=" . $dbPath);
  exit (1);
}

function validateCluster($dbHandle, $clusterName) {
  global $logger;
  if ($clusterName == "") {
    $logger->log_error("Invalid cluster name provided");
    exit (1);
  }
  $clusters = $dbHandle->getAllClusters();
  if ($clusters === FALSE || $clusters["result"] != 0
      || !isset($clusters["clusters"])
      || !is_array($clusters["clusters"])
      || !isset($clusters["clusters"][$clusterName])
      || !is_array($clusters["clusters"][$clusterName])) {
    $logger->log_error("Could not retrieve cluster info from DB"
        . ", clusterName=" . $clusterName
        . ", error=" . $clusters["error"]);
    exit (1);
  }
}

function validateService($dbHandle, $serviceName) {
  global $logger;
  $services = $dbHandle->getAllServicesList();
  if ($services === FALSE || $services["result"] != 0
      || !isset($services["services"])
      || !is_array($services["services"])
      || !isset($services["services"][$serviceName])
      || !is_array($services["services"][$serviceName])) {
    $logger->log_error("Could not retrieve service info from DB"
        . ", serviceName=" . $serviceName
        . ", error=" . $services["error"]);
    exit (1);
  }
}

function validateNode($dbHandle, $clusterName, $nodeName) {
  global $logger;
  $hostInfo = $dbHandle->getHostInfo($clusterName, $nodeName);
  if ($hostInfo === FALSE || $hostInfo["result"] != 0) {
    $logger->log_error("Could not find node info in DB"
        . ", clusterName=" . $clusterName
        . ", hostName=" . $nodeName
        . ", error=" . $hostInfo["error"]);
    exit (1);
  }
}

function validateAction($dbHandle, $clusterName, $action, $serviceNames,
    $nodeNames) {
  global $ACTIONS;
  global $logger;
  if (array_search($action, $ACTIONS) === FALSE) {
    $logger->log_error("Invalid action provided, action=" . $action);
    exit (1);
  }

  if ($action == "start" || $action == "stop" || $action == "uninstall"
      || $action == "reconfigure") {
    if (count($serviceNames) == 0) {
      $logger->log_error("No services listed for given action, action="
          . $action);
      exit (1);
    }
    foreach ($serviceNames as $serviceName) {
      validateService($dbHandle, $serviceName);
    }
  }

  if ($action == "deployNode") {
    foreach ($nodeNames as $nodeName) {
      validateNode($dbHandle, $clusterName, $nodeName);
    }
  }

}

function validateTxn($dbHandle, $clusterName, $txnId) {
  global $logger;
  $txnInfo = $dbHandle->getTransactionStatusInfo($clusterName, $txnId);
  if ($txnInfo === FALSE || $txnInfo["result"] != 0) {
    $logger->log_error("Could not find txn info in DB"
        . ", clusterName=" . $clusterName
        . ", txnId=" . $txnId
        . ", error=" . $txnInfo["error"]);
    exit (1);
  }
}

function createServiceObj($dbHandle, $clusterName, $serviceName,
    $odb, $puppetInvoker) {
  global $logger;
  $services = $dbHandle->getAllServicesInfo($clusterName);
  if ($services === FALSE || $services["result"] != 0) {
    $logger->log_error("Failed to get service list from DB"
        . ", error=" . $services["error"]);
    exit (1);
  }
  foreach ($services["services"] as $svc) {
    if ($svc["serviceName"] != $serviceName) {
      continue;
    }
    $svcObj = new Service($svc["serviceName"], $svc["state"],
       $odb, $puppetInvoker, $svc["serviceName"]);
    return $svcObj;
  }
  $logger->log_error("Could not find service in DB"
      . ", serviceName=" . $serviceName);
  exit (1);
}

$params = array(
   "c:" => "cluster-name:",
   "x:" => "txn-id",
   "d:" => "db-path:",
   "a:" => "action",
   "s:" => "service:",
   "n:" => "node:",
   "r"  => "dry-run",
   "h" => "help"
);

$argsStr = implode(" ", $argv);
$options = @getopt(implode("", array_keys($params)), array_values ($params));

$clusterName = "";
$dbPath = "";
$action = "";
$serviceNames = array();
$nodeNames = array();
$dryRun = FALSE;
$txnId = "";

foreach ($options as $opt => $val) {
  if ($opt == "c" || $opt == "cluster-name") {
    $clusterName = $val;
  }
  else if ($opt == "x" || $opt == "txn-id") {
    $txnId = $val;
  }
  else if ($opt == "d" || $opt == "db-path") {
    $dbPath = $val;
  }
  else if ($opt == "a" || $opt == "action") {
    $action = $val;
  }
  else if ($opt == "s" || $opt == "service") {
    if (is_array($val)) {
      $serviceNames = $val;
    }
    else {
      array_push($serviceNames, $val);
    }
  }
  else if ($opt == "n" || $opt == "node") {
    if (is_array($val)) {
      $nodeNames = $val;
    }
    else {
      array_push($nodeNames, $val);
    }
  }
  else if ($opt == "r" || $opt == "dry-run") {
    $dryRun = TRUE;
  }
  else if ($opt == "h" || $opt == "help") {
    usage();
    exit (0);
  }
  else {
    $defaultLogger->log_error("Invalid command line option provided, opt=$opt");
    $defaultLogger->log_error("ClusterMain execution failed, args=".$argsStr);
    exit (1);
  }
}

if ($clusterName == "") {
  $defaultLogger->log_error("ClusterMain execution failed"
  . ", no cluster name defined, args=".$argsStr);
  exit (1);
}
if ($action == "") {
  $defaultLogger->log_error("ClusterMain execution failed"
      . ", no action defined, args=".$argsStr);
  exit (1);
}
if ($txnId == "") {
  $defaultLogger->log_error("ClusterMain execution failed"
  . ", no txnId defined, args=".$argsStr);
  exit (1);
}
if ($dbPath == "") {
  $defaultLogger->log_error("ClusterMain execution failed"
  . ", no db path defined, args=".$argsStr);
  exit (1);
}

$logger = new HMCLogger("ClusterMain:TxnId=".$txnId);

$dbHandle = validateAndCreateDBHandle($dbPath);
validateCluster($dbHandle, $clusterName);
validateTxn($dbHandle, $clusterName, $txnId);
validateAction($dbHandle, $clusterName, $action, $serviceNames, $nodeNames);

$puppetInvoker = new PuppetInvoker($dbPath);
$odb =  new OrchestratorDB($dbPath, $clusterName, $puppetInvoker);
$cluster = new Cluster($clusterName, $odb, $puppetInvoker);
$transaction = new Transaction($txnId, 0, 0);

// reset sub txn id
// treat 0 as no-parent
$GLOBALS["SUB_TXN_ID"] = 1;

if ($dryRun) {
  print "DEBUG: DRYRUN: ARGS: ".implode(" ",$argv)."\n";
  print "DEBUG: DRYRUN: DB PATH: $dbPath\n";
  print "DEBUG: DRYRUN: CLUSTER NAME: $clusterName\n";
  print "DEBUG: DRYRUN: TXN : ".$transaction->toString()."\n";
  print "DEBUG: DRYRUN: ACTION: $action\n";
  exit(0);
}

$logger->log_info("Taking action=$action on cluster=$clusterName, txn="
    . $transaction->toString());

$result = array( "error" => "", "result" => 0);
if ($action == "deploy") {
  $result = $cluster->deployHDP($transaction);
} else if ($action == "deployNode") {
  $result = $cluster->deployNodes($transaction, $nodeNames);
} else if ($action == "startAll") {
  $result = $cluster->startAllServices($transaction);
} else if ($action == "stopAll") {
  $result = $cluster->stopAllServices($transaction);
} else if ($action == "uninstallAll") {
  $wipeoutData = FALSE;
  $result = $cluster->uninstallHDP($transaction, $wipeoutData);
} else if ($action == "wipeout") {
  $wipeoutData = TRUE;
  $result = $cluster->uninstallHDP($transaction, $wipeoutData);
} else if ($action == "start") {
  $result = $cluster->startServices($transaction, $serviceNames);
} else if ($action == "stop") {
  $result = $cluster->stopServices($transaction, $serviceNames);
} else if ($action == "reconfigure") {
  $result = $cluster->reconfigureServices($transaction, $serviceNames);
} else {
  $result = array ( "result" => -1,
      "error" => "Unsupported action, action=$action");
}

$logger->log_info("Completed action=$action on cluster=$clusterName, txn="
    . $transaction->toString()
    . ", result=" . $result["result"]
    . ", error=" . $result["error"]);

exit ($result["result"]);
?>
