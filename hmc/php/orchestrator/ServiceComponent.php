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

include_once "State.php";

/**
 * ServiceComponent represents one of the constituent components of
 * a Service e.g. NameNode is a component of HDFS.
 */
class ServiceComponent {

  // Name of the component
  public $name;

  // Display Name of the component
  public $displayName;

  // Name of the service to which the component belongs to
  public $serviceName;

  // State of the component
  public $state;

  // Component dependencies
  public $dependencies;

  // Component dependents
  public $dependents;

  // Database
  public $db;

  // Puppet
  public $puppet;

  // logger
  private $logger;

  // clusterName
  private $clusterName;

  // is this a client component
  public $isClient;

  // current action
  private $currentAction;

  function __construct($clusterName, $componentName, $serviceName, $componentState,
      $db, $puppet, $isClient, $displayName) {
    $this->clusterName = $clusterName;
    $this->name = $componentName;
    $this->displayName = $displayName;
    $this->serviceName = $serviceName;
    $this->state = $componentState;
    $this->db = $db;
    $this->puppet = $puppet;
    $this->logger = new HMCLogger("ServiceComponent:".$componentName);
    $this->logger->log_debug("ServiceComponent: $componentName, $serviceName, $componentState, $isClient");
    $this->isClient = $isClient;
    $this->currentAction = "";
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
      $result = $this->db->persistTransaction($transaction, State::$STATE[$state],
            $desc, TransactionProgress::$PROGRESS[$txnProgress],
            "SERVICECOMPONENT", $dryRun);
      if ($result['result'] !== 0) {
        $this->state == State::FAILED;
        $this->logger->log_error("$this->name - ".State::$STATE[$state]);
        $this->db->setServiceState($this, $state);
        return $result;
      }
    }

    if (!$dryRun) {
      $result = $this->db->setServiceComponentState($this->serviceName, $this->name, $state);
      if ($result['result'] !== 0) {
        $this->state == State::FAILED;
        $this->logger->log_error("$this->name - ".State::$STATE[$state]);
        $this->db->setServiceState($this, $state);
        return $result;
      }
    }

    $this->state = $state;
    $this->logger->log_info("$this->name - ".State::$STATE[$state] . " dryRun=$dryRun");
    return array("result" => 0, "error" => "");
  }

  /**
   * UnInstall the component.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function uninstall($transaction, $dryRun) {
    $this->currentAction = "uninstall";

    // Check if it's already UNINSTALLED
    if ($this->state === State::UNINSTALLED) {
      $this->logger->log_info("ServiceComponent $this->name is already UNINSTALLED!");
      return array("result" => 0, "error" => "");
    }

    // Note that we are about to UNINSTALL
    $result = $this->setState(State::UNINSTALLING, $transaction, $dryRun, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }

    return $this->setState(State::UNINSTALLED, $transaction, $dryRun, FALSE);
  }


  /**
   * Install the component.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function install($transaction, $dryRun) {
    $this->currentAction = "install";

    // Check if it's already INSTALLED
    if ($this->state === State::INSTALLED) {
      $this->logger->log_info("ServiceComponent $this->name is already INSTALLED!");
      return array("result" => 0, "error" => "");
    }

    // Ensure each dependent component is INSTALLED
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
        return array("result" => $depResult, "error" => "Failed to start $dep->name with $depResult (\'$depErrMsg\')");
      }
    }

    // Note that we are about to INSTALL
    $result = $this->setState(State::INSTALLING, $transaction, $dryRun, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }

    return $this->setState(State::INSTALLED, $transaction, $dryRun, FALSE);
  }

  /**
   * Start the component.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function start($transaction, $dryRun) {
    $this->currentAction = "start";

    if ($this->isClient) {
      // no-op for clients
      return array( "result" => 0, "error" => "");
    }

    // Check if it's already STARTED
    if ($this->state === State::STARTED) {
      $this->logger->log_info("ServiceComponent $this->name is already STARTED!");
      return array("result" => 0, "error" => "");
    }

    // Ensure state is INSTALLED or STOPPED or FAILED
    if ($this->state !== State::INSTALLED
        && $this->state !== State::STARTING
        && $this->state !== State::STOPPING
        && $this->state !== State::STOPPED
        && $this->state !== State::FAILED) {
      $this->logger->log_error("ServiceComponent $this->name is not INSTALLED or STOPPED or FAILED!");
      return array("result" => -1, "error" => "ServiceComponent $this->name is not INSTALLED or STOPPED or FAILED!");
    }

    // Ensure each dependent component is STARTED
    $result = $this->getDependencies($transaction);
    if ($result["result"] !== 0) {
      return $result;
    }
    foreach ($this->dependencies as $dep) {
      $s = $dep->start($transaction->createSubTransaction(), $dryRun);
      $depResult = $s['result'];
      $depErrMsg = $s['error'];
      if ($depResult !== 0) {
        return array("result" => $depResult, "error" => "Failed to start $dep->name with $depResult (\'$depErrMsg\')");
      }
    }

    // Note that we are about to START
    $result = $this->setState(State::STARTING, $transaction, $dryRun, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }

    // Start self
    //$this->logger->log_error("TODO: Call out for Puppet::start on $this->name (generate site.pp & kick)");
    $nodes = $this->getNodes();
    if ($nodes['result'] !== 0) {
      $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
      return $nodes;
    }

    if (!$dryRun) {
      $this->logger->log_info("Kicking puppet for starting component on "
         . " cluster=" . $this->clusterName
         . ", servicecomponent=" . $this->name
         . ", txn=" . $transaction->toString());

      $startTime = time();
      $result = $this->puppet->kickPuppet($nodes['nodes'], $transaction,
          $this->clusterName, array ( $this->name => $nodes['nodes'] ));
      $this->logger->log_info("Puppet kick response for starting component on "
          . " cluster=" . $this->clusterName
          . ", servicecomponent=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
          "stats" =>
              array (
                     "NODE_COUNT" => count($nodes['nodes']),
                     "TIME_TAKEN_SECS" => $timeTaken),
          "nodeReport" =>
              array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                      "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                      "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
                   "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for starting "
          . $this->name);
      $this->db->persistTransactionOpStatus($transaction,
        json_encode($opStatus));

      if ($result['result'] !== 0) {
        $this->logger->log_error("Puppet kick failed, result="
            . $result['result']);
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        return $result;
      }

      if (count($nodes['nodes']) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        return array ( "result" => -3,
            "error" => "Puppet kick failed on all nodes");
      }
    }

    // Done!
    return $this->setState(State::STARTED, $transaction, $dryRun, TRUE);
  }

  /**
   * Get nodes on which this component is installed.
   * @return mixed
   *   array("result" => 0, "error" => "", "nodes" => array())
   */
  public function getNodes() {
    return $this->db->getComponentNodes($this);
  }

  /**
   * Stop the component.
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function stop($transaction, $dryRun) {
    $this->currentAction = "stop";

    if ($this->isClient) {
      // no-op for clients
      return array( "result" => 0, "error" => "");
    }

    // Check if it's already STOPPED
    if ($this->state === State::STOPPED) {
      $this->logger->log_info("ServiceComponent $this->name is already STOPPED!");
      return array("result" => 0, "error" => "");
    }

    // Only stop if state is STARTED/STARTING/STOPPING/FAILED
    if ($this->state !== State::STARTED
        && $this->state !== State::STARTING
        && $this->state !== State::STOPPING
        && $this->state !== State::FAILED) {
      $this->logger->log_error("ServiceComponent $this->name is not STARTED/FAILED!"
          . "Current state = " . State::$STATE[$this->state]
          . " - STOP is a no-op");
      return array("result" => 0, "error" => "");
    }

    // Ensure each dependent component is STOPPED
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

    // Note we are about to STOP
    $result = $this->setState(State::STOPPING, $transaction, $dryRun, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }

    // Stop self
    $nodes = $this->getNodes();
    if ($nodes['result'] !== 0) {
      $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
      return $nodes;
    }

    if (!$dryRun) {
      $this->logger->log_info("Kicking puppet for stopping component on"
          . " cluster=" . $this->clusterName
          . ", servicecomponent=" . $this->name
          . ", txn=" . $transaction->toString());

      $startTime = time();
      $result = $this->puppet->kickPuppet($nodes['nodes'], $transaction,
          $this->clusterName, array ( $this->name => $nodes['nodes'] ));
      $this->logger->log_info("Puppet kick response for stopping component on"
          . " cluster=" . $this->clusterName
          . ", servicecomponent=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
          "stats" =>
              array (
                     "NODE_COUNT" => count($nodes['nodes']),
                     "TIME_TAKEN_SECS" => $timeTaken),
          "nodeReport" =>
              array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                      "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                      "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
                      "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for stopping "
          . $this->name);
      $this->db->persistTransactionOpStatus($transaction,
          json_encode($opStatus));

      if ($result['result'] !== 0) {
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        return $result;
      }

      if (count($nodes['nodes']) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun, TRUE);
        return array ( "result" => -3,
            "error" => "Puppet kick failed on all nodes");
      }

    }

    // Done!
    return $this->setState(State::STOPPED, $transaction, $dryRun, TRUE);
  }

  private function getDependencies($transaction) {
    if (!isset($this->dependencies)) {
      $this->dependencies = $this->db->getComponentDependencies($this->serviceName, $this->name);
    }
    return $this->checkDBReturn($transaction, $this->dependencies);
  }

  private function getDependents($transaction) {
    if (!isset($this->dependents)) {
      $this->dependents = $this->db->getComponentDependents($this->serviceName, $this->name);
    }
    return $this->checkDBReturn($transaction, $this->dependents);
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
