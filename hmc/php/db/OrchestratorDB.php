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

include_once "../orchestrator/State.php";
include_once "../orchestrator/Service.php";
include_once "../orchestrator/ServiceComponent.php";
include_once "../orchestrator/Cluster.php";

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "Transaction.php";

class OrchestratorDB {

  // Cluster name
  public $clusterName;

  private $db;

  private $logger;

  private $puppet;

  private $exploreMode;

  private $servicesCache;

  private $serviceComponentsCache;

  private $serviceMetaInfo;

  private $serviceComponentMetaInfo;

  /**
   * Initialize ODB
   * @param string $dbPath
   * @param string $clusterName
   * @param object $puppet PuppetInvoker
   */
  function __construct($dbPath, $clusterName, $puppet) {
    $this->clusterName = $clusterName;
    $this->db = new HMCDBAccessor($dbPath);
    $this->puppet = $puppet;
    $this->exploreMode = false;
    $this->servicesCache = array();
    $this->serviceComponentsCache = array();
    $this->serviceMetaInfo = NULL;
    $this->serviceComponentMetaInfo = NULL;
    $this->logger = new HMCLogger("OrchestratorDB");
  }

  function getServiceDisplayName($serviceName) {
    if (!isset($serviceMetaInfo)
        || $serviceMetaInfo == NULL) {
      $result = $this->db->getAllServicesList();
      if ($result["result"] != 0 || !isset($result["services"])) {
        $this->logger->log_error("Failed to retrieve service meta info from DB"
           . ", error=" . $result["error"]);
        return $serviceName;
      }
      $serviceMetaInfo = $result["services"];
    }
    if (isset($serviceMetaInfo[$serviceName]["displayName"])
        && $serviceMetaInfo[$serviceName]["displayName"] != "") {
      return $serviceMetaInfo[$serviceName]["displayName"];
    }
    return $serviceName;
  }

  function getServiceComponentDisplayName($serviceName, $componentName) {
    if (!isset($serviceComponentMetaInfo)
        || $serviceComponentMetaInfo == NULL) {
      $result = $this->db->getAllServiceComponentsList();
      if ($result["result"] != 0 || !isset($result["services"])) {
        $this->logger->log_error("Failed to retrieve service component meta info from DB"
            . ", error=" . $result["error"]);
        return $componentName;
      }
      $serviceComponentMetaInfo = $result["services"];
    }
    if (isset($serviceComponentMetaInfo[$serviceName]["components"][$componentName]["displayName"])
        && $serviceComponentMetaInfo[$serviceName]["components"][$componentName]["displayName"] != "") {
      return $serviceComponentMetaInfo[$serviceName]["components"][$componentName]["displayName"];
    }
    return $componentName;
  }

  /**
   * Get all the services in the cluster.
   * @return array of Service objects
   * @return FALSE on error
   */
  public function getClusterServices() {
    $services = $this->db->getAllServicesInfo($this->clusterName);
    if ($services === FALSE || $services["result"] != 0) {
      $this->logger->log_error("Failed to get service list from DB");
      return FALSE;
    }
    $svcObjs = array();
    foreach ($services["services"] as $svc) {
      if (!$svc["isEnabled"]) {
        continue;
      }
      $state = STATE::getStateFromString($svc["state"]);
      if ($state === FALSE) {
        $this->logger->log_error("Found service with invalid state"
            . ", service=" . $svc["serviceName"]
            . ", state=" . $svc["state"]);
        $state = STATE::UNKNOWN; // unknown
      }

      $svcObj =
        $this->getServiceObj($svc["serviceName"], $state, $this, $this->puppet);
      array_push($svcObjs, $svcObj);
    }
    return $svcObjs;
  }

  /**
   * Get the Service object corresponding to the serviceName.
   * @param serviceName service name
   * @return Service
   * @return FALSE on error
   */
  public function getService($serviceName) {
    $serviceInfo = $this->db->getServiceInfo($this->clusterName, $serviceName);
    if ($serviceInfo["result"] != 0) {
      $this->logger->log_error("Failed to get serviceInfo for $serviceName with "
          . $serviceInfo["error"]);
      return FALSE;
    }

    if ($serviceInfo["isEnabled"] != TRUE) {
      $this->logger->log_error("Could not find ServiceInfo for legal $serviceName");
      return FALSE;
    }

    $state = STATE::getStateFromString($serviceInfo["state"]);
    if ($state === FALSE) {
      $this->logger->log_error("Found service with invalid state"
          . ", service=" . $serviceName
          . ", state=" . $serviceInfo["state"]);
      $state = STATE::UNKNOWN; // unknown
    }
    return $this->getServiceObj($serviceName, $state, $this, $this->puppet);
  }

  public function getServices($serviceNames) {
    $svcObjs = array();
    foreach ($serviceNames as $serviceName) {
      $svcObj = $this->getService($serviceName);
      if ($svcObj === FALSE) {
        return FALSE;
      }
      $svcObjs[$serviceName] = $svcObj;
    }
    return $svcObjs;
  }

  /**
   * Get service dependencies for the given service.
   * @param serviceName service name
   * @return array of Service objects
   */
  public function getServiceDependencies($serviceName) {
    $svcDeps = $this->db->getServiceDependencies($serviceName);
    if ($svcDeps === FALSE || $svcDeps["result"] != 0) {
      $this->logger->log_error("Failed to get service deps from DB");
      return FALSE;
    }
    $services = $this->db->getAllServicesInfo($this->clusterName);
    if ($services === FALSE || $services["result"] != 0) {
      $this->logger->log_error("Failed to get service list from DB");
      return FALSE;
    }

    $svcObjs = array();
    foreach ($svcDeps["serviceDependencies"] as $svcDep) {
      if (!isset($services["services"][$svcDep])) {
        $this->logger->log_error("Found a service dependency that does not "
            . " exist in DB");
        return FALSE;
      }
      $svc = $services["services"][$svcDep];
      $state = STATE::getStateFromString($svc["state"]);
      if ($state === FALSE) {
        $this->logger->log_error("Found service with invalid state"
            . ", service=" . $svc["serviceName"]
            . ", state=" . $svc["state"]);
        $state = STATE::UNKNOWN; // unknown
      }

      $svcObj =
        $this->getServiceObj($svc["serviceName"], $state, $this, $this->puppet);
      array_push($svcObjs, $svcObj);
    }
    return $svcObjs;
  }

  /**
   * Get services which depend on the given service.
   * @param serviceName service name
   * @return array of Service objects
   */
  public function getServiceDependents($serviceName) {
    $svcDeps = $this->db->getServiceDependents($serviceName);
    if ($svcDeps === FALSE || $svcDeps["result"] != 0) {
      $this->logger->log_error("Failed to get service deps from DB");
      return FALSE;
    }
    $services = $this->db->getAllServicesInfo($this->clusterName);
    if ($services === FALSE || $services["result"] != 0) {
      $this->logger->log_error("Failed to get service list from DB");
      return FALSE;
    }

    $svcObjs = array();
    foreach ($svcDeps["serviceDependents"] as $svcDep) {
      if (!isset($services["services"][$svcDep])) {
        $this->logger->log_debug("Found a service dependent that does not "
            . " exist in DB");
        continue;
      }
      $svc = $services["services"][$svcDep];
      if (!$svc["isEnabled"]) {
        continue;
      }
      $state = STATE::getStateFromString($svc["state"]);
      if ($state === FALSE) {
        $this->logger->log_warn("Found service with invalid state"
            . ", service=" . $svc["serviceName"]
            . ", state=" . $svc["state"]);
        $state = STATE::UNKNOWN; // unknown
      }

      $svcObj =
        $this->getServiceObj($svc["serviceName"], $state, $this, $this->puppet);
      array_push($svcObjs, $svcObj);
    }
    return $svcObjs;
  }

  /**
   * Get components of a service.
   * @param serviceName service name
   * @return array of ServiceComponent objects
   */
  public function getServiceComponents($serviceName) {

    $result = $this->db->getAllServiceComponents($serviceName);
    if ($result === FALSE || $result["result"] != 0
        || !isset($result["components"])
        || !is_array($result["components"])) {
      $this->logger->log_error("Failed to get component list from DB");
      return FALSE;
    }

    $fullCompList = $result["components"];

    $result = $this->db->getAllServiceComponentsInfo($this->clusterName);
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get component list from DB");
      return FALSE;
    }

    $compObjs = array();
    if (!isset($result["services"][$serviceName])
        || !is_array($result["services"][$serviceName])
        || !is_array($result["services"][$serviceName]["components"])) {
      return array();
    }

    $comps = $result["services"][$serviceName]["components"];
    foreach ($comps as $comp) {
      $state = STATE::getStateFromString($comp["state"]);
      if ($state === FALSE) {
        $this->logger->log_warn("Found component with invalid state"
            . ", component=" . $comp["componentName"]
            . ", state=" . $comp["state"]);
        $state = STATE::UNKNOWN;
      }
      $isClient = FALSE;
      if (isset($fullCompList[$comp["componentName"]])) {
        if (isset($fullCompList[$comp["componentName"]]["isClient"])) {
          $isClient = $fullCompList[$comp["componentName"]]["isClient"];
        }
      } else {
        $this->logger->log_warn("Found component which doesn't exist in meta list"
            . ", component=" . $comp["componentName"]);
      }

      $compObj =
        $this->getServiceComponentObj($comp["componentName"], $serviceName,
          $state, $this, $this->puppet, $isClient);
      array_push($compObjs, $compObj);
    }
    return $compObjs;
  }

  public function  getNagiosServerComponent() {
    $svc =  $this->getService("NAGIOS");
    if ($svc === FALSE) {
      return $svc;
    }
    $compObjs = $this->getServiceComponents("NAGIOS");
    foreach ($compObjs as $compObj) {
      if ($compObj->name == "NAGIOS_SERVER") {
        return $compObj;
      }
    }
    return FALSE;
  }

  public function  getDashboardServerComponent() {
    $svc =  $this->getService("DASHBOARD");
    if ($svc === FALSE) {
      return $svc;
    }
    $compObjs = $this->getServiceComponents("DASHBOARD");
    foreach ($compObjs as $compObj) {
      if ($compObj->name == "DASHBOARD") {
        return $compObj;
      }
    }
    return FALSE;
  }

  /**
   * Get component dependencies for a given component of a given
   * service.
   * @param serviceName service name
   * @param componentName component name
   * @return array of ServiceComponent objects
   */
  public function getComponentDependencies($serviceName, $componentName) {
    $result = $this->db->getAllServiceComponentsList();
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get component list from DB");
       return FALSE;
     }

     $fullCompList = array();
     if (isset($result["services"])) {
       foreach ($result["services"] as $svc => $svcInfo) {
        if (isset($svcInfo["components"])) {
          foreach ($svcInfo["components"] as $comp => $compInfo) {
            $fullCompList[$comp] = $compInfo["isClient"];
          }
        }
      }
    }

    $result = $this->db->getAllServiceComponentsInfo($this->clusterName);
    if ($result === FALSE || $result["result"] != 0
        || !isset($result["services"][$serviceName])
        || !is_array($result["services"][$serviceName])
        || !is_array($result["services"][$serviceName]["components"])) {
      $this->logger->log_error("Failed to get component list from DB");
      return FALSE;
    }

    $compDeps = $this->db->getServiceComponentDependencies($componentName);
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get component deps list from DB");
      return FALSE;
    }

    $compObjs = array();
    $comps = $result["services"][$serviceName]["components"];

    foreach ($comps as $comp) {
      if (FALSE === array_search($comp["componentName"], $compDeps["componentDependencies"])) {
        $this->logger->log_debug("Skipping component as not in dep list, comp="
            . $comp["componentName"]);
        continue;
      }
      $state = STATE::getStateFromString($comp["state"]);
      if ($state === FALSE) {
        $this->logger->log_error("Found component with invalid state"
            . ", component=" . $comp["componentName"]
            . ", state=" . $comp["state"]);
        $state = STATE::UNKNOWN;
      }
      $isClient = FALSE;
      if (isset($fullCompList[$comp["componentName"]])) {
        $isClient = $fullCompList[$comp["componentName"]];
      } else {
        $this->logger->log_warn("Found component which doesn't exist in meta list"
            . ", component=" . $comp["componentName"]);
      }

      $compObj =
        $this->getServiceComponentObj($comp["componentName"], $serviceName,
          $state, $this, $this->puppet, $isClient);
      array_push($compObjs, $compObj);
    }
    return $compObjs;
  }

  /**
  * Get component dependents for a given component of a given
  * service.
  * @param serviceName service name
  * @param componentName component name
  * @return array of ServiceComponent objects
  */
  public function getComponentDependents($serviceName, $componentName) {
    $result = $this->db->getAllServiceComponentsList();
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get component list from DB");
      return FALSE;
    }

    $fullCompList = array();
    if (isset($result["services"])) {
      foreach ($result["services"] as $svc => $svcInfo) {
        if (isset($svcInfo["components"])) {
          foreach ($svcInfo["components"] as $comp => $compInfo) {
            $fullCompList[$comp] = $compInfo["isClient"];
          }
        }
      }
    }

    $result = $this->db->getAllServiceComponentsInfo($this->clusterName);
    if ($result === FALSE || $result["result"] != 0
    || !isset($result["services"][$serviceName])
    || !is_array($result["services"][$serviceName])
    || !is_array($result["services"][$serviceName]["components"])) {
      $this->logger->log_error("Failed to get component list from DB");
      return FALSE;
    }

    $compDeps = $this->db->getServiceComponentDependents($componentName);
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get component deps list from DB");
      return FALSE;
    }

    $compObjs = array();
    $comps = $result["services"][$serviceName]["components"];

    foreach ($comps as $comp) {
      if (FALSE === array_search($comp["componentName"], $compDeps["componentDependents"])) {
        $this->logger->log_debug("Skipping component as not in dep list, comp="
        . $comp["componentName"]);
        continue;
      }
      $state = STATE::getStateFromString($comp["state"]);
      if ($state === FALSE) {
        $this->logger->log_error("Found component with invalid state"
        . ", component=" . $comp["componentName"]
        . ", state=" . $comp["state"]);
        $state = STATE::UNKNOWN;
      }
      $isClient = FALSE;
      if (isset($fullCompList[$comp["componentName"]])) {
        $isClient = $fullCompList[$comp["componentName"]];
      } else {
        $this->logger->log_warn("Found component which doesn't exist in meta list"
        . ", component=" . $comp["componentName"]);
      }

      $compObj =
          $this->getServiceComponentObj($comp["componentName"], $serviceName,
            $state, $this, $this->puppet, $isClient);
      array_push($compObjs, $compObj);
    }
    return $compObjs;
  }


  /**
   * Get all nodes in the cluster.
   * @return mixed
   *   array( "result" => 0, "error" => msg, "nodes" => array())
   */
  public function getAllNodes() {
    $result = $this->db->getAllHostsInfo($this->clusterName,
        array("=" => array ( "discoveryStatus" => "SUCCESS")), array());
    if ($result === FALSE || $result["result"] != 0
        || !isset($result["hosts"]) || !is_array($result["hosts"])) {
      $this->logger->log_error("Failed to get host list from DB");
      return array ("result" => 1,
                    "error" => "Failed to get host list from DB");
    }

    $nodes = array();
    foreach ($result["hosts"] as $host) {
      array_push($nodes, $host["hostName"]);
    }

    $result = $this->db->getAllHostsByComponent($this->clusterName);
    if ($result === FALSE || $result["result"] != 0) {
      $this->logger->log_error("Failed to get host component mapping from DB");
      return array ("result" => 1,
                    "error" => "Failed to get host component mapping from DB");
    }

    $compMapping = array ();
    if (isset($result["components"])
        && is_array($result["components"])) {
      foreach ($result["components"] as $compName => $hostsList) {
        if (isset($hostsList["hosts"])
            && !empty($hostsList["hosts"])) {
          $compMapping[$compName] = array_keys($hostsList["hosts"]);
        }
      }
    }

    return array ("result" => 0, "error" => "", "nodes" => $nodes,
        "componentMapping" => $compMapping);
  }

  /**
   * Get nodes for the given service-component.
   * @param serviceComponent service component
   * @return mixed
   *   array( "result" => 0, "error" => msg, "nodes" => array())
   */
  public function getComponentNodes($serviceComponent) {
    $result = $this->db->getHostsForComponent($this->clusterName,
        $serviceComponent->name);
    if ($result === FALSE || $result["result"] != 0
    || !isset($result["hosts"]) || !is_array($result["hosts"])) {
      $this->logger->log_error("Failed to get host list from DB");
      return array ("result" => 1,
                    "error" => "Failed to get host list from DB");
    }

    $nodes = array_keys($result["hosts"]);
    return array ("result" => 0, "error" => "", "nodes" => $nodes);
  }

  /**
   * Set service state.
   * @param service service whose state needs to be set
   * @param state service state
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function setServiceState($service, $state) {
    $this->logger->log_info($service->name . " - ". State::$STATE[$state]);

    if (isset(State::$DESIRED_STATE[$state])) {
      $result = $this->db->setServiceDesiredState($this->clusterName,
          $service->name, State::$DESIRED_STATE[$state]);
    }
    $result = $this->db->setServiceState($this->clusterName,
        $service->name, State::$STATE[$state]);

    return $result;
  }

  /**
   * Set service component state.
   * @param serviceComponent service-component whose state needs to be set
   * @param state service-component state
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function setServiceComponentState($serviceName, $componentName, $state) {
    $this->logger->log_info("Update ServiceComponentState " . $serviceName . " - "
        . $componentName . " - " . State::$STATE[$state]);

    if (isset(State::$DESIRED_STATE[$state])) {
       $result = $this->db->setServiceComponentDesiredState($this->clusterName,
         $componentName, State::$DESIRED_STATE[$state], TRUE);
    }

    $result = $this->db->setServiceComponentState($this->clusterName,
        $componentName, State::$STATE[$state], TRUE);
    return $result;
  }

  /**
   * Persist a single transaction.
   * @param transaction transaction to be persisted
   * @param state state of the transaction
   * @param description description of the transaction
   * @param dryRun is this a dry-run?
   * @param txnType Type identifier of txn
   * @return mixed
   *   array( "result" => 0, "error" => msg)
   */
  public function persistTransaction($transaction, $state, $description,
      $progress, $txnType, $dryRun) {
    if ($transaction == NULL) {
      return array ( "result" => 0, "error" => "" );
    }
    if ($dryRun == TRUE) {
      $state = "PENDING";
      $progress = "PENDING";
    }

    $this->logger->log_info("persist: " . $transaction->toString()
        . ":" . $state . ":" . $description . ":" . $progress);

    $result = $this->db->insertOrUpdateSubTransaction($this->clusterName,
        $transaction->txId, $transaction->subTxId, $transaction->parentSubTxId,
        $state, $description, $progress, $txnType);
    return $result;
  }

  public function persistTransactionOpStatus($transaction, $opStatus) {
    if ($transaction == NULL) {
      return array ( "result" => 0, "error" => "" );
    }
    $result = $this->db->updateSubTransactionOpStatus($this->clusterName,
        $transaction->txId, $transaction->subTxId, $opStatus);
    return $result;
  }

  private function getServiceObj($serviceName, $serviceState, $db, $puppet) {
    $service = NULL;
    if (array_key_exists($serviceName, $this->servicesCache)) {
      $service = $this->servicesCache[$serviceName];
      $this->logger->log_debug("Got cached service for $serviceName");
    } else {
      $displayName = $this->getServiceDisplayName($serviceName);
      $service = new Service($this->clusterName, $serviceName, $serviceState,
          $db, $puppet, $displayName);
      $this->servicesCache[$serviceName] = $service;
      $this->logger->log_debug("Did not get cached service for $serviceName");
    }
    return $service;
  }

  private function getServiceComponentObj($componentName, $serviceName, $componentState,
      $db, $puppet, $isClient) {
    $serviceComponent = NULL;
    if (array_key_exists($componentName, $this->serviceComponentsCache)) {
      $serviceComponent = $this->serviceComponentsCache[$componentName];
      $this->logger->log_debug("Got cached serviceComponent for $componentName");
    } else {
      $displayName = $this->getServiceComponentDisplayName($serviceName,
          $componentName);
      $serviceComponent =
        new ServiceComponent($this->clusterName, $componentName, $serviceName,
          $componentState, $db, $puppet, $isClient, $displayName);
      $this->logger->log_debug("Did not get cached serviceComponent for $componentName");
      $this->serviceComponentsCache[$componentName] = $serviceComponent;
    }
    return $serviceComponent;
  }

  public function reset() {
    $this->servicesCache = array();
    $this->serviceComponentsCache = array();
    $this->logger->log_debug("Reset caches.");
  }

  public function getServiceClientNode($serviceName) {
    $this->logger->log_debug("getServiceClientNode called");
    $componentName = $serviceName."_CLIENT";
    $clients = $this->db->getHostsForComponent($this->clusterName,
        $componentName);

    if ($clients === FALSE || $clients["result"] != 0) {
      return $clients;
    }

    $nodes = array_keys($clients["hosts"]);
    return array("result" => 0, "error" => "", "nodes" => $nodes);
  }

  public function getNodeServices($node) {
    $roles = $this->db->getRolesForHosts($this->clusterName, array($node));
    if ($roles === FALSE || $roles["result"] != 0) {
      return $roles;
    }
    $serviceNames = array();
    if (isset($roles["hosts"][$node]["services"])) {
      $serviceNames = array_keys($roles["hosts"][$node]["services"]);
    }

    $services = array();
    foreach ($serviceNames as $serviceName) {
      $service = $this->getService($serviceName);
      if (!$service) {
        $this->logger->log_warn("Failed to get service object for $serviceName");
        return array('result' => -1, 'error' => "Failed to get service object for $serviceName");
      }
      array_push($services, $service);
    }

    return array('result' => 0, 'error' => "", 'services' => $services);
  }

  public function getNodeRolesAndState($nodes) {
    $roles = $this->db->getRolesForHosts($this->clusterName, $nodes);
    return $roles;
  }

  public function setHostsState($hostsToUpdate, $state) {
    $this->logger->log_debug("Update HostRoleState - " . State::$STATE[$state]
        . print_r($hostsToUpdate, true));

    if (isset(State::$DESIRED_STATE[$state])) {
      $result = $this->db->setHostsDesiredState($this->clusterName,
          $hostsToUpdate, State::$DESIRED_STATE[$state]);
    }

    $result = $this->db->setHostsState($this->clusterName,
        $hostsToUpdate, State::$STATE[$state]);
    return $result;
  }

  public function matchHostStateToComponent($hosts) {
    $this->db->matchHostDesiredStateToComponent($this->clusterName, $hosts);
    $result = $this->db->matchHostStateToComponent($this->clusterName, $hosts);
    return $result;
  }

  public function getRecursiveServiceDependents($serviceName) {
    $deps = $this->db->getAllServiceDependencies();
    if ($deps["result"] != 0) {
      return FALSE;
    }
    return $this->db->getRecursiveServiceDependents($deps["serviceDependencies"],
        $serviceName);
  }

};

?>
