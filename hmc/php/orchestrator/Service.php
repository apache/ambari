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

include_once "../db/OrchestratorDB.php";
include_once "../db/Transaction.php";
include_once "../puppet/PuppetInvoker.php";
include_once "State.php";
include_once "ServiceComponent.php";

/**
 * Service represents one of the main services deployed such as:
 * HDFS, MapReduce, ZooKeeper, HBase, HCatalog, Oozie
 */
class Service {

  // Name of the service
  public $name;

  // Display Name of the service
  public $displayName;

  // Service state
  public $state;

  // Cluster Name
  public $clusterName;

  // Service dependencies
  public $dependencies;

  // Service dependents
  public $dependents;

  // Service components
  public $components;

  // Database
  public $db;

  // Puppet
  public $puppet;

  // logger
  private $logger;

  // current action being done
  private $currentAction;

  // whether all components are clients only or not
  private $isClientOnly;

  // Set state whether smoke tests are required to be run for client-only services
  // For client-components set smoke tests to be run only when install is done
  private $runClientSmokeTest;

  function __construct($clusterName, $serviceName, $serviceState,
      $odb, $puppet, $displayName) {
    $this->name = $serviceName;
    $this->displayName = $displayName;
    $this->state = $serviceState;
    $this->clusterName = $clusterName;
    $this->db = $odb;
    $this->puppet = $puppet;
    $this->logger = new HMCLogger("Service: $serviceName ($clusterName)");
    $this->logger->log_debug("Service: $serviceName, $serviceState, $clusterName");
    $this->currentAction = "";
    $this->isClientOnly = NULL;
    $this->runClientSmokeTest = FALSE;
  }

  /**
   * Persist state into DB
   * @param State $state
   * @param Transaction $transaction
   * @param bool $dryRun
   * @param bool $persistTxn - FALSE in case of INSTALL only
   */
  function setState($state, $transaction, $dryRun, $persistTxn) {
    if ($persistTxn) {
      $txnProgress = getTransactionProgressFromState($state);
      $desc = getActionDescription($this->displayName, $this->currentAction,
          TransactionProgress::$PROGRESS[$txnProgress]);
      if ($dryRun) {
        $desc = getActionDescription($this->displayName, $this->currentAction,
            "PENDING");
      }
      $result =
        $this->db->persistTransaction($transaction, State::$STATE[$state],
            $desc, TransactionProgress::$PROGRESS[$txnProgress],
            "SERVICE", $dryRun);
      if ($result['result'] !== 0) {
        $this->state == State::FAILED;
        $this->logger->log_error($this->name." - ".State::$STATE[$state]);
        $this->logger->log_error("Failed to persist transaction: " . $transaction->toString());
        $this->db->setServiceState($this, $state);
        return $result;
      }
    }

    if (!$dryRun) {
      $result = $this->db->setServiceState($this, $state);
      if ($result['result'] !== 0) {
        $this->state == State::FAILED;
        $this->logger->log_error("Failed to persist state for Service "
            . "$this->name - ".State::$STATE[$state] . " dryRun=$dryRun");
        $this->db->setServiceState($this, $state);
        return $result;
      }
    }

    $this->state = $state;
    $this->logger->log_info("$this->name - " . State::$STATE[$state]
        . " dryRun=$dryRun");
    return array("result" => 0, "error" => "");
  }

  /**
   * Uninstall the service.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function uninstall($transaction, $dryRun) {
    $this->currentAction = "uninstall";

    // Check if it's already INSTALLED or STARTED
    if ($this->state === State::UNINSTALLED) {
      $this->logger->log_debug("Service $this->name is already UNINSTALLED!");
      return array("result" => 0, "error" => "");
    }

    // Ensure state is UNINSTALLED or FAILED
    if ($this->state !== State::STOPPED &&
        $this->state !== State::FAILED &&
        $this->state !== State::UNKNOWN &&
        $this->state != State::INSTALLED) {
      $this->logger->log_error("Service $this->name is not UNKNOWN or INSTALLED or FAILED or STOPPED! state=" . State::$STATE[$this->state]);
      return array("result" => -1, "error" => "Service $this->name is not INSTALLED or FAILED or STOPPED! state=" . State::$STATE[$this->state]);
    }

    // Note that we are about to UNINSTALL
    $result = $this->setState(State::UNINSTALLING, $transaction, $dryRun, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }

    // Mark each component as UNINSTALLED
    $result = $this->getComponents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }

    foreach ($this->components as $component) {
      $subTxn = $transaction->createSubTransaction();
      $s = $component->uninstall($subTxn, $dryRun);
    }

    // Done!
    return $this->setState(State::UNINSTALLED, $transaction, $dryRun, FALSE);
  }


  /**
   * Install & configure the service.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function install($transaction, $dryRun) {
    $this->currentAction = "install";

    // set flag to ensure smoke tests are run for client-only services
    // as we just installed or re-configured something
    $this->runClientSmokeTest = TRUE;

    // Check if it's already INSTALLED or STARTED
    if ($this->state === State::INSTALLED ||
        $this->state === State::STARTED) {
      $this->logger->log_debug("Service $this->name is already INSTALLED!");
      return array("result" => 0, "error" => "");
    }

    // Ensure state is UNINSTALLED or FAILED
    if ($this->state !== State::UNINSTALLED &&
        $this->state !== State::FAILED &&
        $this->state !== State::UNKNOWN &&
        $this->state !== State::STOPPED) {
      $this->logger->log_error("Service $this->name is not UNKNOWN or UNINSTALLED or FAILED or STOPPED! state=" . State::$STATE[$this->state]);
      return array("result" => -1, "error" => "Service $this->name is not UNINSTALLED or FAILED or STOPPED! state=" . State::$STATE[$this->state]);
    }

    // Ensure each dependent service is INSTALLED
    $result = $this->getDependencies($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }
    foreach ($this->dependencies as $dep) {
      $subTxn = $transaction->createSubTransaction();
      $s = $dep->install($subTxn, $dryRun);
      $depResult = $s['result'];
      $depErrMsg = $s['error'];
      if ($depResult !== 0) {
        return array("result" => $depResult, "error" => "Failed to install $dep->name with $depResult (\'$depErrMsg\')");
      }
    }

    // Note that we are about to INSTALL
    $result = $this->setState(State::INSTALLING, $transaction, $dryRun, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }

    // Install self
    // TODO: Special case, don't use Puppet here!

    // Mark each component as INSTALLED
    $result = $this->getComponents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }
    foreach ($this->components as $component) {
      $subTxn = $transaction->createSubTransaction();
      $s = $component->install($subTxn, $dryRun);
    }

    // Done!
    return $this->setState(State::INSTALLED, $transaction, $dryRun, FALSE);
  }

  /**
   * Requires components to be set before calling this api.
   * @return boolean whether service has only client components
   */
  private function checkIsClientOnly() {
    if (isset($this->isClientOnly)
        && $this->isClientOnly != NULL) {
      return $this->isClientOnly;
    }
    $isClientOnly = TRUE;
    foreach ($this->components as $component) {
      if (!$component->isClient) {
        $isClientOnly = FALSE;
        break;
      }
    }
    $this->isClientOnly = $isClientOnly;
    return $isClientOnly;
  }

  /**
   * Start the service.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function start($transaction, $dryRun) {
    $this->currentAction = "start";

    $result = $this->getComponents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }

    // Ensure each dependent service is STARTED
    $result = $this->getDependencies($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }
    foreach ($this->dependencies as $dep) {
      $s = $dep->start($transaction->createSubTransaction(), $dryRun, TRUE);
      $depResult = $s['result'];
      $depErrMsg = $s['error'];
      if ($depResult !== 0) {
        return array("result" => $depResult,
                 "error" => "Failed to start $dep->name with $depResult (\'$depErrMsg\')");
      }
    }

    $this->checkIsClientOnly();

    $this->logger->log_debug("Service - " . $this->name . " - isClientOnly="
        . $this->isClientOnly
        . ", dryRun=" . $dryRun);

    $persistTxn = TRUE;
    $actualDryRun = $dryRun;
    if ($this->isClientOnly) {
      // this is to ensure that we do not persist the start sub-txn into the DB
      // also start-stop state does not make sense for a client-only service
      // we retain notion of START state in memory to ensure that we do not
      // kick the smoke test twice
      $persistTxn = FALSE;
      $dryRun = TRUE;
    }

    // Check if it's already STARTED
    if ($this->state === State::STARTED) {
      $this->logger->log_debug("Service $this->name is already STARTED!");
      return array("result" => 0, "error" => "");
    }

    // Ensure state is INSTALLED or STOPPED or FAILED
    if ($this->state !== State::INSTALLED
        && $this->state !== State::STARTING
        && $this->state !== State::STOPPING
        && $this->state !== State::STOPPED
        && $this->state !== State::FAILED) {
      $this->logger->log_error("Service $this->name is not INSTALLED or STOPPED or FAILED!");
      return array("result" => -1,
          "error" => "Service $this->name is not INSTALLED or STOPPED or FAILED!");
    }

    // Note that we are about to START
    $result = $this->setState(State::STARTING, $transaction, $dryRun, $persistTxn);
    if ($result['result'] !== 0) {
      $this->setState(State::FAILED, $transaction, $dryRun, $persistTxn);
      return $result;
    }

    if (!$this->isClientOnly) {
      // Start each component
      foreach ($this->components as $component) {
        $s = $component->start($transaction->createSubTransaction(), $dryRun);
        $cmpResult = $s['result'];
        $cmpErrMsg = $s['error'];
        if ($cmpResult !== 0) {
          $this->setState(State::FAILED, $transaction, $dryRun, $persistTxn);
          return array("result" => $cmpResult, "error" => "Failed to start $component->name with $cmpResult (\'$cmpErrMsg\')");
        }
      }
    }

    // Done!
    $result = $this->setState(State::STARTED, $transaction, $dryRun, $persistTxn);
    if ($result["result"] != 0) {
      $this->setState(State::FAILED, $transaction, $dryRun, $persistTxn);
      $this->logger->log_error("Failed to set state to STARTED with " . $result["error"]);
      return $result;
    }

    return $this->smoke($transaction->getNextSubTransaction(), $actualDryRun);
  }

  private function setSmokeProgress($transaction, $dryRun, $txnProgress) {
    $this->logger->log_debug("Setting smoke test progress for service="
        . $this->name . ", dryrun=" . $dryRun
        . ", progress=" . TransactionProgress::$PROGRESS[$txnProgress]);

    if ($dryRun) {
      $txnProgress = TransactionProgress::PENDING;
    }
    $desc = getActionDescription($this->displayName, "test",
        TransactionProgress::$PROGRESS[$txnProgress]);

    $result =
        $this->db->persistTransaction($transaction,
            TransactionProgress::$PROGRESS[$txnProgress],
            $desc, TransactionProgress::$PROGRESS[$txnProgress],
            "SERVICE-SMOKETEST", $dryRun);

    /*
    TODO error check later
    if ($result['result'] !== 0) {
      $this->state == State::FAILED;
      $this->logger->log_error($this->name." - ".State::$STATE[$state]);
      $this->db->setServiceState($this, $state);
      return $result;
    }
    */
    return array("result" => 0, "error" => "");
  }

  public function smoke($transaction, $dryRun) {
    $this->currentAction = "test";

    $this->checkIsClientOnly();
    if ($this->isClientOnly
        && !$this->runClientSmokeTest) {
      $this->logger->log_info("Skipping client-only service smoke tests"
          . " as nothing installed in this cycle");
      return array("result" => 0, "error" => "");
    }

    $result = $this->db->getServiceClientNode($this->name);
    if ($result == FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to access db to get service-client node for $this->name");
      $this->setSmokeProgress($transaction, $dryRun,
          TransactionProgress::FAILED);
      return;
    }

    if (!is_array($result["nodes"]) || count($result["nodes"]) == 0 ) {
      $this->logger->log_warn("Cannot find service-client node for $this->name");
      // treating this as a no-op
      // TODO - should it be a failure instead?
      return array("result" => 0, "error" => "");
    }

    $clientNode = $result["nodes"][0];

    // set smoke starting state
    $this->setSmokeProgress($transaction, $dryRun,
        TransactionProgress::IN_PROGRESS);

    $result = $this->getComponents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }


    if (!$this->isClientOnly) {
      // Check if it's already STARTED
      // only in case service has non-client components
      if ($this->state !== State::STARTED) {
        $this->logger->log_debug("Service $this->name is not STARTED, cannot run smoke tests!");
        $this->setSmokeProgress($transaction, $dryRun, TransactionProgress::FAILED);
        return array("result" => -2, "error" =>
            "Service $this->name is not STARTED, cannot run smoke tests!");
      }
    }

    if (!$dryRun) {
      $this->logger->log_debug("Kicking puppet for smoketesting service on "
         . " cluster=" . $this->clusterName
         . ", service=" . $this->name
         . ", txn=" . $transaction->toString());

      $startTime = time();
      $result =
        $this->puppet->kickServiceCheck( array($this->name => $clientNode),
            $transaction, $this->clusterName);

      $this->logger->log_debug("Puppet kick response for smoketesting service on "
          . " cluster=" . $this->clusterName
          . ", service=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
          "stats" =>
               array (
                      "NODE_COUNT" => 1,
                      "TIME_TAKEN_SECS" => $timeTaken),
          "nodeReport" =>
              array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                  "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                  "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for smoke testing "
          . $this->name);
      $this->db->persistTransactionOpStatus($transaction,
          json_encode($opStatus));

      if ($result["result"] != 0
          || count($result[SUCCESSFULLNODES]) != 1) {
        $this->logger->log_error("Service smoke check failed with "
            . print_r($result, true));
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        $this->setSmokeProgress($transaction, $dryRun,
            TransactionProgress::FAILED);
        return array("result" => -2, "error" =>
          "Service $this->name is not STARTED, smoke tests failed!");
      }
    }

    $this->setSmokeProgress($transaction, $dryRun,
        TransactionProgress::COMPLETED);
    return array("result" => 0, "error" => "");
  }

  /**
   * Stop the service.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function stop($transaction, $dryRun) {
    $this->currentAction = "stop";

    $result = $this->getComponents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }

    // Ensure each dependent service is STOPPED
    $result = $this->getDependents($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }
    foreach ($this->dependents as $dep) {
      $s = $dep->stop($transaction->createSubTransaction(), $dryRun);
      $depResult = $s['result'];
      $depErrMsg = $s['error'];
      if ($depResult !== 0) {
        return array("result" => $depResult, "error" => "Failed to stop $dep->name with $depResult (\'$depErrMsg\')");
      }
    }

    $this->checkIsClientOnly();

    if (!$this->isClientOnly) {
      // Check if it's already STOPPED
      if ($this->state === State::STOPPED) {
        $this->logger->log_info("Service $this->name is already STOPPED!");
        return array("result" => 0, "error" => "");
      }

      // Only stop if state is STARTED/STARTING/STOPPING/FAILED
      if ($this->state !== State::STARTED
          && $this->state !== State::STARTING
          && $this->state !== State::STOPPING
          && $this->state !== State::FAILED) {
        $this->logger->log_info("Service " . $this->name . " is not STARTED/STOPPING/FAILED!"
            . "Current state = " . State::$STATE[$this->state]
            . " - STOP is a no-op");
        return array("result" => 0, "error" => "");
      }

      // Note we are about to STOP
      $result = $this->setState(State::STOPPING, $transaction, $dryRun, TRUE);
      if ($result['result'] !== 0) {
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        return $result;
      }

      // Stop each component
      foreach ($this->components as $component) {
        $s = $component->stop($transaction->createSubTransaction(), $dryRun);
        $cmpResult = $s['result'];
        $cmpErrMsg = $s['error'];
        if ($cmpResult !== 0) {
          $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
          return array("result" => $cmpResult, "error" => "Failed to stop $component->name with $cmpResult (\'$cmpErrMsg\')");
        }
      }

      // Done!
      return $this->setState(State::STOPPED, $transaction, $dryRun, TRUE);
    }

    return array("result" => 0, "error" => "");
  }

  private function getDependencies($transaction) {
    if (!isset($this->dependencies)) {
      $this->dependencies = $this->db->getServiceDependencies($this->name);
    }
    return $this->checkDBReturn($transaction, $this->dependencies);
  }

  private function getDependents($transaction) {
    if (!isset($this->dependents)) {
      $this->dependents = $this->db->getServiceDependents($this->name);
    }
    return $this->checkDBReturn($transaction, $this->dependents);
  }

  private function getComponents($transaction) {
    if (!isset($this->components)) {
      $this->components = $this->db->getServiceComponents($this->name);
    }
    return $this->checkDBReturn($transaction, $this->components);
  }

  private function checkDBReturn($transaction, $dbResult) {
    if ($dbResult === FALSE) {
      $trace = debug_backtrace();
      $this->logger->log_error("DB Error: " . $trace[1]["function"]);
      $this->setState(State::FAILED, $transaction, FALSE, TRUE);
      return array("result" => $dbResult, "error" => "Failed to update db for $this->name with $dbResult");
    }
    return array("result" => 0, "error" => "");
  }
}

?>
