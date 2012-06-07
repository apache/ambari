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

class Cluster {

  public $name;
  public $db;
  public $puppet;
  private $logger;
  private $state;
  private $currentAction;

  public function __construct($clusterName, $db, $puppet) {
    $this->name = $clusterName;
    $this->db = $db;
    $this->puppet = $puppet;
    $this->logger = new HMCLogger("Cluster:".$clusterName);
    $this->state = "";
    $this->currentAction = "";
  }

  public function resetSubTxnId() {
    $GLOBALS["SUB_TXN_ID"] = 1;
  }

  /**
   * Function to install, configure and start HDP across the whole cluster
   * @param transaction transactionId for the operation
   */
  public function deployHDP($transaction) {
    $this->currentAction = "Cluster install";
    $result = $this->_deployHDP($transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    return $this->_deployHDP($transaction, FALSE);
  }

  private function _deployHDP($transaction, $dryRun) {
    $services = $this->db->getClusterServices();
    if ($services == FALSE) {
      $this->logger->log_error("Failed to get cluster services.");
      return array("result" => -1, "error" => "Failed to get cluster services from DB.");
    }
    $n = count($services);
    $this->logger->log_info("Deploying HDP with $n services.... DryRun=$dryRun");
    $result = $this->_installAllServices($services,
        $transaction->createSubTransaction(), $dryRun);
    if ($result['result'] !== 0) {
      $this->logger->log_error("Failed to install services.");
      return $result;
    }

    $result = $this->_startAllServices($services,
        $transaction->createSubTransaction(), $dryRun);

    $this->resetSubTxnId();
    $this->db->reset();

    return $result;
  }

  private function _uninstallAllServices($services, $transaction, $dryRun,
      $wipeoutData = FALSE) {
    $n = count($services);
    $this->logger->log_info("Uninstalling HDP with $n services.... DryRun=$dryRun");
    $this->setState(State::UNINSTALLING, $transaction, $dryRun);

    $svcTxnMap = array();
    foreach($services as $service) {
      $svcTxnMap[$service->name] = $transaction->createSubTransaction();
    }
    // Mark all as uninstalled before the actual kick so
    // that desired state kicks in
    foreach($services as $service) {
      $result = $service->uninstall($svcTxnMap[$service->name], $dryRun);
      if ($result['result'] != 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, TRUE);
        return $result;
      }
    }

    // Real Run
    if (!$dryRun) {
      $result = $this->db->getAllNodes();
      if ($result['result'] !== 0) {
        $this->logger->log_error("UnInstalling HDP failed with: " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, TRUE);
        }
        return $result;
      }

      $wipeoutFlag = "false";
      if ($wipeoutData) {
        $wipeoutFlag = "true";
      }

      $nodes = $result["nodes"];

      $this->logger->log_debug("Kicking puppet for uninstalling "
         . "cluster=" . $this->name
         . ", txn=" . $transaction->toString());
      $startTime = time();
      $result = $this->puppet->kickPuppet($nodes, $transaction, $this->name,
          $result["componentMapping"], array ("wipeoff_data" => $wipeoutFlag));
      $this->logger->log_debug("Puppet kick response for uninstalling "
          . "cluster=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));
      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
         "stats" =>
            array (
              "NODE_COUNT" => count($nodes),
              "TIME_TAKEN_SECS" => $timeTaken),
         "nodeReport" =>
            array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
              "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
              "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
              "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for uninstall HDP");
      $this->db->persistTransactionOpStatus($transaction,
          json_encode($opStatus));
      if ($result['result'] != 0) {
        $this->logger->log_error("UnInstalling HDP failed with: " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, TRUE);
        }
        return $result;
      }

      if (count($nodes) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, TRUE);
        }
        return array ( "result" => -3,
                  "error" => "Puppet kick failed on all nodes");
      }

    }
    $this->setState(STATE::UNINSTALLED, $transaction, $dryRun);
    $this->logger->log_info("Uninstalling HDP with $n services complete. DRYRUN=$dryRun");

   return array("result" => 0, "error" => "");
  }

  private function _uninstallAll($transaction, $dryRun, $wipeoutData = FALSE) {
    $services = $this->db->getClusterServices();
    if ($services == FALSE) {
      $this->logger->log_error("Failed to get cluster services.");
      return array("result" => -1, "error" => "Failed to get cluster services from DB.");
    }

    // reverse order of services
    // should cause NAGIOS to stop first
    $services = array_reverse($services);

    $this->logger->log_info("Uninstalling HDP.... DryRun=$dryRun");
    $result = $this->_stopAllServices($services,
        $transaction->createSubTransaction(), $dryRun);
    if ($result['result']  !== 0) {
      $this->logger->log_error("Failed to stop services.");
      return $result;
    }
    $result = $this->_uninstallAllServices($services,
        $transaction->createSubTransaction(), $dryRun, $wipeoutData);
    if ($result['result']  !== 0) {
      $this->logger->log_error("Failed to uninstall services.");
      return $result;
    }
    return $result;
  }

  /**
   * Function to stop & uninstall HDP across the whole cluster
   * @param transaction transactionId for the operation
   */
  public function uninstallHDP($transaction, $wipeoutData = FALSE) {
    $this->currentAction = "Cluster uninstall";
    $this->logger->log_info("Uninstalling HDP, wipeoutDataFlag="
        . $wipeoutData);

    $this->_uninstallAll($transaction->createSubTransaction(), TRUE,
        $wipeoutData);

    $this->resetSubTxnId();
    $this->db->reset();

    return $this->_uninstallAll($transaction->createSubTransaction(), FALSE,
        $wipeoutData);
  }

  /**
   * Function to deploy all the required rpms and start all required
   * services on a given node
   * @param transaction transactionId for the operation
   * @param array $nodes Hostnames of the nodes to be deployed
   */
  public function deployNodes($transaction, $nodes) {
    // get components which this node maps to
    // set state to install for required components
    // kick puppet to install on single node
    // set state for host to same state as the component state ( started/stopped )
    // kick puppet for this single node
    $this->logger->log_info("Deploying nodes " . implode(",", $nodes));

    $hostRoles = $this->db->getNodeRolesAndState($nodes);
    if ($hostRoles['result'] !== 0) {
      $this->logger->log_error("Error obtaining host role state info from DB"
          . ", error=" . $hostRoles["error"]);
      return $hostRoles;
    }

    $hostCompMapping = array();
    // array ( "svcName" => "compName" => ( "state" => , "hosts" => array (nodes) );
    foreach ($hostRoles["hosts"] as $hostName => $hostInfo) {
      if (!isset($hostInfo["services"])) {
        continue;
      }
      foreach ($hostInfo["services"] as $svcName => $svcInfo) {
        if (!isset($svcInfo["components"])) {
          continue;
        }
        foreach ($svcInfo["components"] as $compName => $compInfo) {
          if (!isset($hostCompMapping[$svcName])) {
            $hostCompMapping[$svcName] = array();
          }
          if (!isset($hostCompMapping[$svcName][$compName])) {
            $hostCompMapping[$svcName][$compName] = array();
          }
          if (!isset($compInfo["state"])) {
            $compInfo["state"] = "UNKNOWN";
          }
          $state = State::getStateFromString($compInfo["state"]);
          if (!isset($hostCompMapping[$svcName][$compName]["state"])) {
            $hostCompMapping[$svcName][$compName]["state"] = $state;
          }
          if (!isset($hostCompMapping[$svcName][$compName]["hosts"])) {
            $hostCompMapping[$svcName][$compName]["hosts"] = array();
          }
          array_push($hostCompMapping[$svcName][$compName]["hosts"],
              $hostName);
        }
      }
    }

    $result = $this->_installNodes($transaction->createSubTransaction(),
        $hostCompMapping, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $result = $this->_startNodes($transaction->createSubTransaction(),
        $hostCompMapping, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $result = $this->_restartDashboardAndNagios($transaction->createSubTransaction(),
        TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }

    $this->resetSubTxnId();
    $this->db->reset();

    $result = $this->_installNodes($transaction->createSubTransaction(),
        $hostCompMapping, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $result = $this->_startNodes($transaction->createSubTransaction(),
        $hostCompMapping, FALSE);
    if ($result['result'] !== 0) {
      return $result;
    }

    $result = $this->_restartDashboardAndNagios($transaction->createSubTransaction(),
        FALSE);

    return $result;
  }

  private function _restartDashboardAndNagios($transaction, $dryRun,
      $restartNagiosOnly = FALSE) {
    $this->currentAction = "Reconfigure Nagios/Ganglia";
    $this->logger->log_info("Restarting dashboard and nagios, dryRun=" . $dryRun);

    // check if nagios installed and needs to be restarted
    $nagiosComp = $this->db->getNagiosServerComponent();
    $restartNagios = FALSE;
    if ($nagiosComp !== FALSE) {
      if ($nagiosComp->state == STATE::STARTED) {
        $restartNagios = TRUE;
      }
    }

    // check if dashboard installed and needs to be restarted
    $restartDashboard = FALSE;
    if (!$restartNagiosOnly) {
      $dashboardComp = $this->db->getDashboardServerComponent();
      if ($dashboardComp !== FALSE) {
        if ($dashboardComp->state == STATE::STARTED) {
          $restartDashboard = TRUE;
        }
      }
    }

    $this->setState(State::STOPPING, $transaction, $dryRun);

    if ($restartNagios) {
      $this->logger->log_info("Stopping Nagios Server");
      $result = $nagiosComp->stop($transaction->createSubTransaction(), $dryRun);
      if ($result["result"] != 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $nagiosService = $this->db->getService("NAGIOS");
        if ($nagiosService !== FALSE) {
          $nagiosService->setState(STATE::FAILED,
              $transaction->createSubTransaction(), $dryRun, FALSE);
        }
        $this->logger->log_error("Failed to stop nagios server, error"
            . $result["error"]);
        return $result;
      }
    }

    if ($restartDashboard) {
      $this->logger->log_info("Stopping Dashboard");
      $result = $dashboardComp->stop($transaction->createSubTransaction(), $dryRun);
      if ($result["result"] != 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->logger->log_error("Failed to stop dashboard server, error"
            . $result["error"]);
        return $result;
      }
    }

    $this->setState(State::STARTING, $transaction, $dryRun);

    if ($restartDashboard) {
      $this->logger->log_info("Starting Dashboard");
      $result = $dashboardComp->start($transaction->createSubTransaction(), $dryRun);
      if ($result["result"] != 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $dashboardService = $this->db->getService("DASHBOARD");
        if ($dashboardService !== FALSE) {
          $dashboardService->setState(STATE::FAILED,
              $transaction->createSubTransaction(), $dryRun, FALSE);
        }
        $this->logger->log_error("Failed to start dashboard server, error"
            . $result["error"]);
        return $result;
      }
    }

    if ($restartNagios) {
      $this->logger->log_info("Starting Nagios Server");
      $result = $nagiosComp->start($transaction->createSubTransaction(), $dryRun);
      if ($result["result"] != 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->logger->log_error("Failed to start nagios server, error"
            . $result["error"]);
        return $result;
      }
    }

    $this->setState(State::STARTED, $transaction, $dryRun);
    return array ( "result" => 0, "error" => "");
  }

  private function _installNodes($transaction, $hostCompMapping, $dryRun) {
    $this->logger->log_info("Installing on nodes dryRun=" . $dryRun);
    $this->currentAction = "Install nodes";
    $hostsToInstall = array();
    $allHosts = array();
    $compMapping = array();
    foreach ($hostCompMapping as $svcName => $svcInfo) {
      $hostsToInstall[$svcName] = array();
      foreach ($svcInfo as $compName => $compInfo) {
        $hostsToInstall[$svcName][$compName] = array();
        if ($compInfo["state"] == State::INSTALLED
            || $compInfo["state"] == State::STARTED
            || $compInfo["state"] == State::STOPPED
            || $compInfo["state"] == State::FAILED) {
          $hostsToInstall[$svcName][$compName] = $compInfo["hosts"];
          $allHosts = array_merge($allHosts, $compInfo["hosts"]);
          $compMapping[$compName] = $compInfo["hosts"];
        }
      }
    }

    if (count($allHosts) == 0) {
      return array("result" => 0, "error" => "");
    }

    $allHosts = array_unique($allHosts);

    // set state in DB to installing for the required hosts
    if ($dryRun) {
      $this->setState(State::INSTALLING, $transaction, $dryRun);
    } else {
      $this->db->setHostsState($hostsToInstall, State::INSTALLING);
      $this->setState(State::INSTALLING, $transaction, $dryRun);

      // send kick to hosts
      $this->logger->log_debug("Kicking puppet for installing nodes on "
         . " cluster=" . $this->name
         . ", txn=" . $transaction->toString());
      $startTime = time();
      $result = $this->puppet->kickPuppet($allHosts, $transaction, $this->name,
          $compMapping);
      $this->logger->log_debug("Puppet kick response for installing nodes on"
          . " cluster=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
          "stats" =>
             array (
                    "NODE_COUNT" => count($allHosts),
                    "TIME_TAKEN_SECS" => $timeTaken),
          "nodeReport" =>
             array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                  "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                  "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
                  "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for deploying nodes");
      $this->db->persistTransactionOpStatus($transaction,
          json_encode($opStatus));

      if ($result['result'] != 0) {
        $this->logger->log_error("Installing nodes failed with:  " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->db->setHostsState($allHosts, State::FAILED);
        return $result;
      }

      if (count($allHosts) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->db->setHostsState($hostsToInstall, State::FAILED);
        return array ( "result" => -3,
                        "error" => "Puppet kick failed on all nodes");
      }

      $this->db->setHostsState($hostsToInstall, State::INSTALLED);
      $this->setState(State::INSTALLED, $transaction, $dryRun);
    }
    return array("result" => 0, "error" => "");
  }

  private function _startNodes($transaction, $hostCompMapping, $dryRun) {
    $this->logger->log_info("Starting nodes dryRun=" . $dryRun);

    $this->currentAction = "Start nodes";
    $hostsToStart = array();
    $kickHosts = array();
    $noOpHosts = array();
    $compMapping = array();
    foreach ($hostCompMapping as $svcName => $svcInfo) {
      $hostsToStart[$svcName] = array();
      foreach ($svcInfo as $compName => $compInfo) {
        $hostsToStart[$svcName][$compName] = array();
        if ($compInfo["state"] == State::STARTED) {
          $hostsToStart[$svcName][$compName] = $compInfo["hosts"];
          $kickHosts = array_merge($kickHosts, $compInfo["hosts"]);
          $compMapping[$compName] = $compInfo["hosts"];
        } else {
          $noOpHosts[$svcName][$compName] = $compInfo["hosts"];
        }
      }
    }

    // set state for remaining hosts to stopped or installed as needed?
    if (!$dryRun) {
      $result = $this->db->matchHostStateToComponent($noOpHosts);
      if ($result['result'] != 0) {
        $this->logger->log_error("Could not update the state for hosts in no-op list"
           . ", error=" . $result["error"]);
        return $result;
      }
    }

    if (count($kickHosts) == 0) {
      return array("result" => 0, "error" => "");
    }

    $kickHosts = array_unique($kickHosts);

    // set state in DB to starting for the required hosts
    if ($dryRun) {
      $this->setState(State::STARTING, $transaction, $dryRun);
    } else {
      $this->db->setHostsState($hostsToStart, State::STARTING);
      $this->setState(State::STARTING, $transaction, $dryRun);

      // send kick to  hosts
      $this->logger->log_debug("Kicking puppet for starting nodes on"
          . " cluster=" . $this->name
          . ", txn=" . $transaction->toString());
      $startTime = time();
      $result = $this->puppet->kickPuppet($kickHosts, $transaction,
          $this->name, $compMapping);
      $this->logger->log_debug("Puppet kick response for starting nodes on"
          . " cluster=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
          "stats" =>
              array (
                    "NODE_COUNT" => count($kickHosts),
                    "TIME_TAKEN_SECS" => $timeTaken),
         "nodeReport" =>
            array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                  "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                  "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
                  "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for deploying nodes");
      $this->db->persistTransactionOpStatus($transaction,
          json_encode($opStatus));

      if ($result['result'] != 0) {
        $this->logger->log_error("Starting nodes failed with:  " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->db->setHostsState($hostsToStart, State::FAILED);
        return $result;
      }

      if (count($kickHosts) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun);
        $this->db->setHostsState($hostsToStart, State::FAILED);
        return array ( "result" => -3,
                       "error" => "Puppet kick failed on all nodes");
      }

      // set state in DB to started
      $this->db->setHostsState($hostsToStart, State::STARTED);
      $this->setState(State::STARTED, $transaction, $dryRun);

    }
    return array("result" => 0, "error" => "");
  }

  /**
   * Function to install all the services in order.
   * @param transaction transactionId for the operation
   */
  function installAllServices($transaction) {
    $services = $this->db->getClusterServices();
    $result = $this->_installAllServices($services, $transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }

    $this->resetSubTxnId();
    $this->db->reset();

    return $this->_installAllServices($services, $transaction, FALSE);
  }

  private function _installAllServices($services, $transaction, $dryRun) {
    $n = count($services);
    $this->logger->log_info("Installing HDP with $n services... DryRun=$dryRun");
    $this->setState(State::INSTALLING, $transaction, $dryRun);

    $svcTxnMap = array();
    foreach ($services as $service) {
      $svcTxnMap[$service->name] = $transaction->createSubTransaction();
    }

    // Mark all as installed before the actual kick so
    // that desired state kicks in
    foreach ($services as $service) {
      $result = $this->installService($svcTxnMap[$service->name], $service, $dryRun);
      if ($result['result'] !== 0) {
        $this->setState(State::FAILED, $transaction, $dryRun);
        $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, FALSE);
        return $result;
      }
    }

    // Special case...
    if (!$dryRun) {

      $result = $this->db->getAllNodes();
      if ($result['result'] !== 0) {
        $this->logger->log_error("Installing HDP failed with:  " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach ($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, FALSE);
        }
        return $result;
      }

      $nodes = $result["nodes"];
      $this->logger->log_debug("Kicking puppet for installing"
          . " cluster=" . $this->name
          . ", txn=" . $transaction->toString());
      $startTime = time();
      $result = $this->puppet->kickPuppet($nodes, $transaction, $this->name,
          $result["componentMapping"]);
      $this->logger->log_debug("Puppet kick response for installing"
          . " cluster=" . $this->name
          . ", txn=" . $transaction->toString()
          . ", response=" . print_r($result, true));

      // handle puppet response
      $timeTaken = time() - $startTime;
      $opStatus = array(
         "stats" =>
             array (
                    "NODE_COUNT" => count($nodes),
                    "TIME_TAKEN_SECS" => $timeTaken),
         "nodeReport" =>
             array ( "PUPPET_KICK_FAILED" => $result[KICKFAILED],
                    "PUPPET_OPERATION_FAILED" => $result[FAILEDNODES],
                    "PUPPET_OPERATION_TIMEDOUT" => $result[TIMEDOUTNODES],
                    "PUPPET_OPERATION_SUCCEEDED" => $result[SUCCESSFULLNODES]));

      $this->logger->log_info("Persisting puppet report for install HDP");
      $this->db->persistTransactionOpStatus($transaction,
         json_encode($opStatus));

      if ($result['result'] != 0) {
        $this->logger->log_error("Installing HDP failed with:  " . $result['error']);
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach ($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, FALSE);
        }
        return $result;
      }

      if (count($nodes) > 0
          && count($result[SUCCESSFULLNODES]) == 0) {
        $this->logger->log_error("Puppet kick failed, no successful nodes");
        $this->setState(State::FAILED, $transaction, $dryRun);
        foreach ($services as $service) {
          $service->setState(State::FAILED, $svcTxnMap[$service->name], $dryRun, FALSE);
        }
        return array ( "result" => -3,
                       "error" => "Puppet kick failed on all nodes");
      }

    }

    // TODO - Update DB with transaction
    $this->setState(State::INSTALLED, $transaction, $dryRun);
    $this->logger->log_info("Installing HDP with $n services complete. DRYRUN=$dryRun");
    return array("result" => 0, "error" => "");
  }

  /**
   * Function to start all the services in order.
   * @param transaction transactionId for the operation
   */
  public function startAllServices($transaction) {
    $services = $this->db->getClusterServices();
    $n = count($services);
    $this->logger->log_info("Starting $n services");
    $result = $this->_startAllServices($services, $transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    $services = $this->db->getClusterServices();
    return $this->_startAllServices($services, $transaction, FALSE);
  }

  public function startServices($transaction, $serviceNames) {
    $services = $this->db->getServices($serviceNames);
    $result = $this->_startAllServices($services, $transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    $services = $this->db->getServices($serviceNames);
    return $this->_startAllServices($services, $transaction, FALSE);
  }

  private function _startAllServices($services, $transaction, $dryRun) {
    $n = count($services);
    $this->logger->log_info("Starting HDP with $n services...");
    foreach ($services as $service) {
      $result = $this->startService($transaction->createSubTransaction(), $service, $dryRun);
      if ($result['result'] !== 0) {
        return $result;
      }
    }

    // TODO - Update DB with transaction
    $this->logger->log_info("Starting HDP with $n services complete. ");
    return array("result" => 0, "error" => "");
  }

  /**
   * Function to stop all the services in order.
   * @param transaction transactionId for the operation
   */
  public function stopAllServices($transaction) {
    $services = $this->db->getClusterServices();
    $n = count($services);
    $this->logger->log_info("Stopping $n services");
    $result = $this->_stopAllServices($services, $transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    $services = $this->db->getClusterServices();
    return $this->_stopAllServices($services, $transaction, FALSE);
  }

  public function stopServices($transaction, $serviceNames) {
    $services = $this->db->getServices($serviceNames);
    $result = $this->_stopAllServices($services, $transaction, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    $services = $this->db->getServices($serviceNames);
    return $this->_stopAllServices($services, $transaction, FALSE);
  }

  private function _stopAllServices($services, $transaction, $dryRun) {
    $n = count($services);
    $this->logger->log_info("Stopping HDP with $n services... DryRun=$dryRun");
    foreach ($services as $service) {
      $result = $this->stopService($transaction->createSubTransaction(),
          $service, $dryRun);
      if ($result['result'] !== 0) {
        return $result;
      }
    }

    // TODO - Update DB with transaction
    $this->logger->log_info("Stopping HDP with $n services complete. DryRun=$dryRun");
    return array("result" => 0, "error" => "");
  }

  /**
   * Function to install a given service.
   * @param transaction transactionId for the operation
   * @param service service to be installed
   * @param dryRun dry-run?
   */
  private function installService($transaction, $service, $dryRun) {
    $this->logger->log_info("Installing service $service->name ...");
    $result = $service->install($transaction, $dryRun);
    $this->logger->log_info("Installing service $service->name complete. ");
    return $result;
  }

  /**
   * Function to start a given service.
   * Should ensure required dependent services are up and running.
   * This could be done in the puppet layer.
   * @param transaction transactionId for the operation
   * @param service service to be started
   * @param dryRun dry-run?
   */
  private function startService($transaction, $service, $dryRun) {
    $this->logger->log_info("Starting service $service->name ...");
    $result = $service->start($transaction, $dryRun);
    $this->logger->log_info("Starting service $service->name complete. "
        . "Result=" . $result["result"]);
    return $result;
  }

  /**
   * Function to stop a given service.
   * Should ensure required dependent services are not running.
   * This could be done in the puppet layer.
   * @param transaction transactionId for the operation
   * @param service service to be stopped
   * @param dryRun dry-run?
   */
  private function stopService($transaction, $service, $dryRun) {
    $this->logger->log_info("Stopping service $service->name ...");
    $result = $service->stop($transaction, $dryRun);
    $this->logger->log_info("Stopping service $service->name complete. "
        . "Result=" . $result["result"]);
    return $result;
  }

  /**
   * Function to reconfigure a set of services by first stopping the services and
   * the required dependencies, re-pushing new configs to required nodes and
   * restarting all the required services.
   * @param transaction transactionId for the operation
   * @param serviceNames services to be reconfigured
   */
  public function reconfigureServices($transaction, $serviceNames) {
    $result = $this->_reconfigureServices($transaction, $serviceNames, TRUE);
    if ($result['result'] !== 0) {
      return $result;
    }
    $this->resetSubTxnId();
    $this->db->reset();
    return $this->_reconfigureServices($transaction, $serviceNames, FALSE);
  }

  private function getServices($serviceNames) {
    $services = array();
    foreach ($serviceNames as $serviceName) {
      $service = $this->db->getService($serviceName);
      if ($service === FALSE) {
        return array("result" => -1, "error" => "Failed to get Service for $serviceName");
      }
      array_push($services, $service);
    }
    return array("result" => 0, "error" => "", "services" => $services);
  }

  private function getUniqueServices($services) {
    $uniques = array();

    foreach ($services as $service) {
      $uniques[$service->name] = $service;
    }

    return array_values($uniques);
  }

  private function _reconfigureServices($transaction, $serviceNames, $dryRun) {
    $serviceList = implode($serviceNames, ",");
    $this->currentAction = "Reconfigure";

    $this->logger->log_debug("reconfigureServices for ($serviceList) DRYRUN=$dryRun");
    $result = $this->getServices($serviceNames);
    if ($result["result"] != 0) {
      $this->logger->log_error("Failed to get Service objects.");
      return $result;
    }

    $services = $result["services"];
    $servicesToSetToStoppedState = array();

    $svcsToStart = array();
    foreach ($services as $svcObj) {
      if ($svcObj->state == STATE::STARTED || $svcObj->state == STATE::STARTING) {
        array_push($svcsToStart, $svcObj->name);
      } else if ($svcObj->state == STATE::STOPPED) {
        array_push($servicesToSetToStoppedState, $svcObj->name);
      }
    }

    // get all dependents recursively for all the services that will be
    // reconfigured
    $dependents = array();
    foreach ($serviceNames as $serviceName) {
      $svcDeps = $this->db->getRecursiveServiceDependents($serviceName);
      if ($svcDeps === FALSE) {
        continue;
      }
      $dependents = array_merge($dependents, $svcDeps);
    }
    $dependents = array_unique($dependents);
    foreach ($dependents as $serviceName) {
      $svc = $this->db->getService($serviceName);
      if ($svc !== FALSE) {
        array_push($services, $svc);
        if ($svc->state == STATE::STARTED || $svc->state == STATE::STARTING) {
          array_push($svcsToStart, $serviceName);
        } else if ($svc->state == STATE::STOPPED) {
          array_push($servicesToSetToStoppedState, $serviceName);
        }
      }
    }

    $services = $this->getUniqueServices($services);

    // HACK!!!!! restart nagios everytime a service is reconfigured as
    // nagios runs checks on almost all services
    $nagiosComp = $this->db->getNagiosServerComponent();
    $restartNagios = FALSE;
    if ($nagiosComp !== FALSE) {
      if ($nagiosComp->state == STATE::STARTED) {
        $restartNagios = TRUE;
      }
    }

    // Stop the services
    $this->logger->log_debug("reconfigureServices: Stopping services ($serviceList) dryRun=$dryRun");
    foreach ($services as $service) {
      $result = $this->stopService($transaction->createSubTransaction(), $service, $dryRun);
      if ($result['result'] !== 0) {
        $this->logger->log_error("Failed to stop service $service->name with " . $result["error"]);
        return $result;
      }
    }

    $this->logger->log_debug("reconfigureServices: Installing services ($serviceList) dryRun=$dryRun");
    $result = $this->_installAllServices($services, $transaction->createSubTransaction(), $dryRun);
    if ($result['result'] !== 0) {
      $this->logger->log_error("Failed to install services with " . $result["error"]);
      return $result;
    }

    $serviceToStartList = implode(",", $svcsToStart);

    // Start the services
    $this->logger->log_debug("reconfigureServices: Starting services ($serviceToStartList) dryRun=$dryRun");

    // Start all services and dependents which were in a started state initially
    // that would have been stopped as a result of reconfiguration
    foreach ($svcsToStart as $serviceName) {
      $service = $this->db->getService($serviceName);
      $result = $this->startService($transaction->createSubTransaction(), $service, $dryRun);
      if ($result['result'] !== 0) {
        $this->logger->log_error("Failed to start service $service->name with " . $result["error"]);
        return $result;
      }
    }

    // Set the originally stopped services back to stopped state
    foreach ($servicesToSetToStoppedState as $serviceName) {
      $service = $this->db->getService($serviceName);
      $this->logger->log_info("Setting state to STOPPED for service "
          . $service->name);
      $service->setState(STATE::STOPPED,
          $transaction->createSubTransaction(), $dryRun, FALSE);
    }

    if ($restartNagios) {
      $this->logger->log_info("Restarting Nagios Server after reconfiguration");
      $result = $this->_restartDashboardAndNagios(
          $transaction->getNextSubTransaction(), $dryRun, TRUE);
      if ($result["result"] != 0) {
        $this->logger->log_error("Failed to restart nagios server, error"
            . $result["error"]);
        return $result;
      }
    }

    return array("result" => 0, "error" => "");
  }

  /**
   * Run smoke tests on a given services.
   * @param transaction transaction
   * @param serviceNames service names
   */
  public function smokeServices($transaction, $serviceNames) {
    $serviceList = implode($serviceNames, ",");
    $this->logger->log_debug("smokeServices: Smoking services ($serviceList)");
    $result = $this->getServices($serviceNames);
    if ($result["result"] != 0) {
      $this->logger->log_error("Failed to get Service objects.");
      return $result;
    }

    $services = $result["services"];

    // Smoke the services
    foreach ($services as $service) {
      $this->logger->log_debug("About to smoke $service->name");
      $result = $service->smoke($transaction->createSubTransaction(), FALSE);
      if ($result['result'] != 0) {
        $this->logger->log_error("Failed to smoke service $service->name with " . $result["error"]);
        return $result;
      } else {
        $this->logger->log_debug("Succesfully smoked service $service->name");
      }
    }

    $this->resetSubTxnId();
    $this->db->reset();

    return array("result" => 0, "error" =>"");
  }

  /**
   *
   */
  private function setState($state, $transaction, $dryRun) {
    $txnProgress = getTransactionProgressFromState($state);
//  $desc = "CLUSTER"."-".$this->currentAction."-". TransactionProgress::$PROGRESS[$txnProgress];
    $desc = getActionDescription("", $this->currentAction, TransactionProgress::$PROGRESS[$txnProgress]);
    if ($dryRun) {
//      $desc = "CLUSTER"."-".$this->currentAction."-PENDING";
      $desc = getActionDescription("", $this->currentAction, "PENDING");
    }
    $result =
      $this->db->persistTransaction($transaction, State::$STATE[$state],
          $desc, TransactionProgress::$PROGRESS[$txnProgress],
          "CLUSTER", $dryRun);
    if ($result['result'] !== 0) {
      $this->state == State::FAILED;
      $this->logger->log_error($this->name." - ".State::$STATE[$state]);
      return $result;
    }

    $this->state = $state;
    $this->logger->log_info("$this->name - " . State::$STATE[$state]);
    return array("result" => 0, "error" => "");
  }

}

?>
