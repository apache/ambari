<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "../util/HMCTxnUtils.php";

/**
 * Interface between UI and install framework layer.
 */
class HMC {

  private $dbHandle;

  private $dbPath;

  private $logger;

  private $clusterName;

  private $command;

  function __construct($dbPath, $clusterName) {
    $this->dbPath = $dbPath;
    $this->clusterName = $clusterName;
    $this->logger = new HMCLogger("HMC");
    $this->dbHandle = new HMCDBAccessor($dbPath);

    if (!isset($GLOBALS["PHP_EXEC_PATH"])) {
      $GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
    }
    // TODO
    if (!isset($GLOBALS["CLUSTERMAIN_PATH"])) {
      $GLOBALS["CLUSTERMAIN_PATH"] = "ClusterMain.php";
    }

    $this->command = $GLOBALS["PHP_EXEC_PATH"]
        . " " . $GLOBALS["CLUSTERMAIN_PATH"];
  }

  /**
   * Function to deploy and start HDP across the whole cluster
   * Runs in the background
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function deployHDP() {
    $action = "deployHDP";
    $msg = "Deploying cluster";
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a deploy ";
    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * Function to deploy all the required rpms and start all required
   * services on a given node
   * Runs in the background
   * @param array $nodes Hostnames of the nodes to be deployed
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function deployNodes($nodes) {
    $action = "deployNodes";
    $nodeList = implode(",", $nodes);
    $msg = "Deploying on nodes, list=" . $nodeList;
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a deployNode ";
    foreach ($nodes as $node) {
      $args .= " -n " . $node;
    }
    return $this->internalTrigger($action, $msg, $args);
  }


  /**
   * Function to start all the services in order.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function startAllServices() {
    $action = "startAllServices";
    $msg = "Starting all services";
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a startAll ";

    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * Function to stop all the services in order.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function stopAllServices() {
    $action = "stopAllServices";
    $msg = "Stopping all services";
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a stopAll ";

    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * NOT SUPPORTED
   * Function to start given services.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function startServices($services) {
    $action = "startServices";
    $svcList = implode(",", $services);
    $msg = "Starting services, list=".$svcList;
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a start ";
    foreach ($services as $svc) {
      $args .= " -s " . $svc;
    }
    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * NOT SUPPORTED
   * Function to stop given services.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function stopServices($services) {
    $action = "stopServices";
    $svcList = implode(",", $services);
    $msg = "Stopping services, list=".$svcList;
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a stop ";
    foreach ($services as $svc) {
      $args .= " -s " . $svc;
    }
    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * NOT SUPPORTED
   * Function to start a given service and all the services it depends upon to
   * start successfully in the required order.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function startDependentServices($service) {
    // generate a txn id which is returned back to the UI for progress
    // monitoring

    // use popen/pclose to start a background process
    // background script:
    // generate site.pp to start service and dependent services in required order
    // update progress, make logs accessible
    error_log("Start dependent services is not supported");
    exit (1);
  }

  /**
   * NOT SUPPORTED
   * Function to stop a given service and all the services that depend upon it
   * in the required order.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function stopDependentServices($service) {
    // generate a txn id which is returned back to the UI for progress
    // monitoring

    // use popen/pclose to start a background process
    // background script:
    // generate site.pp to stop service and dependent services in required order
    // update progress, make logs accessible
    error_log("Stop dependent services is not supported");
    exit (1);
  }

  /**
   * NOT SUPPORTED
   * Function to reconfigure a cluster by first stopping the whole cluster,
   * re-pushing new configs to all nodes and restarting all services.
   * Runs in the background.
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function reconfigureHDP() {
    // generate a txn id which is returned back to the UI for progress
    // monitoring

    // use popen/pclose to start a background process
    // background script:
    // generate site.pp to stop all services in required order
    // update progress, make logs accessible
    // generate site.pp with new configs
    // update progress, make logs accessible
    // generate site.pp to start all services in required order
    // update progress, make logs accessible
    error_log("Re-configuring is not supported");
    exit (1);

  }

  /**
   * Function to reconfigure services by first stopping the services and the
   * required dependencies, re-pushing new configs to required nodes and
   * restarting all the required services.
   * Runs in the background.
   * @param array services to re-configure
   * @return mixed
   *  txn_id: transaction id to refer to to get progress updates and logs
   *  array ( "txn_id" => $txn_id );
   */
  public function reconfigureServices($services) {
    $action = "reconfigureServices";
    $svcList = implode(",", $services);
    $msg = "Reconfiguring services, list=".$svcList;

    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath
        . " -a reconfigure ";

    foreach ($services as $svc) {
      $args .= " -s " . $svc;
    }

    return $this->internalTrigger($action, $msg, $args);
  }

  /**
   * Get progress update for a given transaction.
   * @param txnId Transaction Id
   * @return mixed
   *   array ( "txn_id" => $txn_id,
   *           "result" => 0,
   *           "processRunning" => bool,
   *           "subTxns" => array (
   *              array (
   *                "subTxnId" =>
   *                "parentSubTxnId" =>
   *                "state" =>
   *                "description" =>
   *              )
   *           )
   *        )
   */
  public function getProgress($txnId) {
    // given the txn id generated for one of the supported actions, provide
    // the current progress update
    // this could also be a trigger to update the current state if needed
    $response = array ( "result" => 0, "error" => "");
    $txnInfo = $this->dbHandle->getTransactionStatusInfo($this->clusterName,
        $txnId);

    if ($txnInfo === FALSE || $txnInfo["result"] != 0) {
      return $txnInfo;
    }

    $pidInfo = json_decode($txnInfo["pidInfo"], true);
    $procRunning = HMCTxnUtils::checkTxnProcessStatus($pidInfo);

    $response["processRunning"] = $procRunning;

    $subTxnInfo = $this->dbHandle->getAllSubTransactionsInfo($this->clusterName,
        $txnId);
    if ($subTxnInfo === FALSE || $subTxnInfo["result"] != 0) {
      return $subTxnInfo;
    }

    $orderedSubTxns = $this->orderSubTxns($subTxnInfo["subTxns"]);

    $response["txnId"] = $txnId;
    $response["subTxns"] = array();
    foreach($orderedSubTxns as $sTxnId => $sTxn) {
      if (isset($sTxn["opStatus"])) {
        $sTxn["opStatus"] = json_decode($sTxn["opStatus"], true);
      } else {
        $sTxn["opStatus"] = array();
      }
      array_push($response["subTxns"], $sTxn);
    }

    return $response;
  }

  public function orderSubTxns($subTxns) {
    $subTxnCount = count($subTxns);

    $parentIndexedSubTxns = array();
    $allSubTxns = array();

    foreach ($subTxns as $subTxn) {
      $allSubTxns[$subTxn["subTxnId"]] = $subTxn;
    }

    foreach ($subTxns as $subTxn) {
      if (!isset($parentIndexedSubTxns[$subTxn["parentSubTxnId"]])) {
        $parentIndexedSubTxns[$subTxn["parentSubTxnId"]] = array();
      }
      $parentIndexedSubTxns[$subTxn["parentSubTxnId"]][$subTxn["subTxnId"]] = $subTxn;
    }

    $rankedSubTxns = array();
    $currentRank = 0;
    $rankedIndexes = array();

    $this->_orderSubTxns($rankedSubTxns, $rankedIndexes,
       $currentRank, $allSubTxns, $parentIndexedSubTxns, $subTxns);


    assert(count($rankedSubTxns) == $subTxnCount);
    return $rankedSubTxns;
  }

  private function _orderSubTxns(&$rankedSubTxns, &$rankedIndexes,
      &$currentRank, &$allSubTxns, $parentIndexedSubTxns, $subTxnsToOrder) {

    $maxIndex = 0;
    $indexedSubTxns = array();

    foreach ($subTxnsToOrder as $subTxn) {
      if ($subTxn["subTxnId"] > $maxIndex) {
        $maxIndex = $subTxn["subTxnId"];
      }
      $indexedSubTxns[$subTxn["subTxnId"]] = $subTxn;
    }

    for ($i = 0; $i <= $maxIndex; ++$i) {
      if (!isset($indexedSubTxns[$i])) {
        continue;
      }

      $subTxn = $indexedSubTxns[$i];
      $parentSubTxnId = $subTxn["parentSubTxnId"];

      if (isset($parentIndexedSubTxns[$i])
          && is_array($parentIndexedSubTxns[$i])
          && count($parentIndexedSubTxns[$i]) > 0) {
        ksort($parentIndexedSubTxns[$i]);
        foreach ($parentIndexedSubTxns[$i] as $index => $pSubTxn) {
          $pSubTxnId = $pSubTxn["subTxnId"];
          $ppSubTxnId = $pSubTxn["parentSubTxnId"];
          if (!isset($rankedIndexes[$pSubTxnId])) {
            $this->_orderSubTxns($rankedSubTxns, $rankedIndexes,
                $currentRank, $allSubTxns, $parentIndexedSubTxns,
                $parentIndexedSubTxns[$ppSubTxnId]);
          }
          if (!isset($rankedIndexes[$pSubTxnId])) {
            $pSubTxn["rank"] = $currentRank;
            $rankedSubTxns[$currentRank] = $pSubTxn;
            $rankedIndexes[$pSubTxn["subTxnId"]] = TRUE;
            ++$currentRank;
          }
         }
      }
      if (!isset($rankedIndexes[$i])) {
        $subTxn["rank"] = $currentRank;
        $rankedSubTxns[$currentRank] = $subTxn;
        $rankedIndexes[$i] = TRUE;
        ++$currentRank;
      }
    }

    return $rankedSubTxns;
  }


  /**
   * Get logs for a given transaction
   * @param string $txnId Transaction Id
   */
  public function getLogs($txnId) {
    // get the logs for a given transaction triggered earlier.
    // not sure if we want to support filtering by log level for now

    $response = array ( "result" => 0, "error" => "");
    $txnInfo = $this->dbHandle->getTransactionStatusInfo($this->clusterName,
       $txnId);

    if ($txnInfo === FALSE || $txnInfo["result"] != 0) {
      return $txnInfo;
    }

    $subTxnInfo = $this->dbHandle->getAllSubTransactionsInfo($this->clusterName,
        $txnId);
    if ($subTxnInfo === FALSE || $subTxnInfo["result"] != 0) {
      return $subTxnInfo;
    }

    $puppetInvoker = new PuppetInvoker($this->dbPath);

    $response["txnId"] = $txnId;
    $response["subTxns"] = array();

    foreach ($subTxnInfo["subTxns"] as $subTxnId => $subTxn) {
      $nodes = array();
      $response["subTxns"][$subTxn["subTxnId"]] =
          array( "nodeReport" => array(),
                 "nodeLogs" => array());

      if (!isset($subTxn["opStatus"])) {
        continue;
      }

      $opStatus = json_decode($subTxn["opStatus"], true);
      if (!isset($opStatus["nodeReport"])) {
          continue;
      }

      $nodeReport = $opStatus["nodeReport"];

      $response["subTxns"][$subTxn["subTxnId"]]["nodeReport"] = $nodeReport;

      $keys = array ( "PUPPET_KICK_FAILED", "PUPPET_OPERATION_FAILED",
          "PUPPET_OPERATION_SUCCEEDED" );
      foreach ($keys as $key) {
        if (isset($nodeReport[$key])
            && is_array($nodeReport[$key])) {
          $nodes = array_merge($nodes, $nodeReport[$key]);
        }
      }

      $transaction = new Transaction($txnId, $subTxn["subTxnId"],
          $subTxn["parentSubTxnId"]);

      $puppetLogs = $puppetInvoker->getReports($nodes, $transaction);
      $this->logger->log_debug("Got puppet reports for transaction=" .$transaction->toString()
          . ", nodes=" . print_r($nodes, true)
          . ", logs=" . print_r($puppetLogs, true));

      $response["subTxns"][$subTxn["subTxnId"]]["nodeLogs"] = $puppetLogs;
    }
    return $response;
  }

  private function internalTrigger($action, $msg, $args) {
    $this->logger->log_info("HMC triggering action=" . $action . ", " . $msg);
    $response = array ( "result" => 0, "error" => "");

    $statusInfo = array ("function" => "HMC::$action",
                         "action" => $msg);
    $txnId = HMCTxnUtils::createNewTransaction($this->dbHandle,
        $this->clusterName, $statusInfo);
    if ($txnId === FALSE) {
      $error = "Failed to create a new transaction, action=" . $action;
      $this->logger->log_error($error);
      return array ("result" => 1, "error" => $error);
    }
    $response["txnId"] = $txnId;
    $args .= " -x " . $txnId;

    $this->logger->log_debug("Triggering background process"
        . ", clusterName=" . $this->clusterName
        . ", txnId=" . $txnId
        . ", command=" . $this->command
        . ", args=" . $args);
    $backgroundPid = HMCTxnUtils::execBackgroundProcess($this->dbHandle,
        $this->clusterName, $txnId, $this->command, $args, "");

    if ($backgroundPid === FALSE) {
      $error = "Failed to trigger background process, action=" . $action;
      $this->logger->log_error($error);
      $response["result"] = 1;
      $response["error"] = $error;
    }
    return $response;
  }

  public function uninstallHDP($wipeoutData = FALSE) {
    $this->logger->log_info("Triggering uninstall"
         . ", clusterName=" . $this->clusterName
         );
    $action = "uninstallHDP";
    $msg = "Uninstalling cluster, wipeout=" . $wipeoutData;
    $args = " -c " . $this->clusterName
        . " -d " . $this->dbPath;
    if (!$wipeoutData) {
      $args .= " -a uninstallAll ";
    } else {
      $args .= " -a wipeout ";
    }
    return $this->internalTrigger($action, $msg, $args);
  }

}

?>
