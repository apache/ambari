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


class HMCDBAccessor {

  private $dbHandle;

  private $dbPath;

  private $logger;

  function __construct($db) {
    $this->dbPath = $db;
    $this->logger = new HMCLogger("HMCDBAccessor");
    $dsn = "sqlite:".$db;
    if (!file_exists($db)) {
      $this->logger->log_error("Sqlite db file does not exist, db=$db");
      throw new Exception("Could not find sqlite db file, db=$db");
    }
    $this->dbHandle = new PDO($dsn);
    if ($this->dbHandle === FALSE) {
      $this->logger->log_error("Could not open sqlite db, db=$db");
      throw new Exception("Could not open sqlite db, db=$db");
    }
  }

  private function getLastDBErrorAsString() {
    $error = "";
    $pdoError = $this->dbHandle->errorInfo();
    if (isset($pdoError) && is_array($pdoError)) {
      if (isset($pdoError[0])) {
        $error = "sqlStateErrorCode=".$pdoError[0];
        if (isset($pdoError[1])) {
          $error .= ", driverErrCode=".$pdoError[1];
        }
        if (isset($pdoError[1])) {
          $error .= ", driverErrMsg=".$pdoError[2];
        }
      }
    }
    else {
      $error = "Unknown DB error";
    }
    return $error;
  }

  /**
   * Create a new cluster
   * @param string $clusterName Cluster Name
   * @param string $version Version
   * @param string $state Current state
   * @return mixed
   *   array (
   *           "clusterName" => $clusterName,
   *           "result" => 0,
   *           "error" => "");
   */
  public function createCluster($clusterName, $version, $state) {
    LockAcquire();
    $query = "INSERT INTO Clusters "
        . " ( cluster_name, version, state ) "
        . " VALUES ( "
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote($version) . " , "
        . $this->dbHandle->quote($state) . " )";
    $response = array ( "clusterName" => $clusterName,
        "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  /**
   * Update cluster state for a given clusterName
   * @param string $clusterName Cluster Name
   * @param string $state New state of cluster
   * @return mixed
   *   array ( "state" => $clusterState,
   *           "result" => 0,
   *           "error" => "");
   */

  public function getClusterState ($clusterName) {
    LockAcquire();
    $query = "SELECT state FROM Clusters WHERE cluster_name = "
        . $this->dbHandle->quote($clusterName);
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["state"] = $result[0]["state"];
      LockRelease(); return $response;
    }

    LockRelease(); return $response;
  }

  /**
   * Update cluster state for a given clusterName
   * @param string $clusterName Cluster Name
   * @param string $state New state of cluster
   * @return mixed
   *   array ( "clusterName" => $clusterName,
   *           "clusterState" => $clusterState,
   *           "oldClusterState" => $oldClusterState,
   *           "result" => 0,
   *           "error" => "");
   */
  public function setClusterState($clusterName, $state) {
    LockAcquire();
    $response = array ( "clusterName" => $clusterName,
        "result" => 0, "error" => "");
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $query = "SELECT state FROM Clusters WHERE cluster_name = "
        . $this->dbHandle->quote($clusterName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["oldClusterState"] = $result[0]["state"];
      $query = "UPDATE Clusters SET state = " . $this->dbHandle->quote($state)
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName);
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $ret = $this->dbHandle->commit();
      if (!$ret) {
        $error = $this->getLastDBErrorAsString();
        $response["result"] = 1;
        $response["error"] = "Failed to commit DB transaction, error=".$error;
        LockRelease(); return $response;
      }
      $response["clusterState"] = $state;
      LockRelease();
      return $response;
    }
    $this->dbHandle->rollBack();
    $response["result"] = 1;
    $response["error"] = "Could not find cluster name".$clusterName." in DB";
    LockRelease(); return $response;
  }

  /**
   * Get info on all clusters
   * @return mixed
   *   array (
   *           "result" => 0,
   *           "error" => "",
   *           "clusters" => array (
   *              "$clusterName" => array (
   *                  "clusterName" => $clusterName,
   *                  "version" => $version,
   *                  "state" => $state
   *                ),
   *                ...
   *           )
   *         );
   */
  public function getAllClusters() {
    LockAcquire();
    $query = "SELECT cluster_name, version, state FROM Clusters";
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusters"] = array();
    foreach ($result as $entry) {
      $cluster = array();
      $cluster["clusterName"] = $entry["cluster_name"];
      $cluster["state"] = $entry["state"];
      $cluster["version"] = $entry["version"];
      $response["clusters"][$cluster["clusterName"]] = $cluster;
    }
    LockRelease(); return $response;
  }

/////////////////////////////////////////// Services related APIs ////////////////////////////////////////////

  /**
   * Get the list of all possible services supported by the hadoop statck
   * @return mixed
   *   array (
   *           "result" => 0,
   *           "error" => "",
   *           "services" => array (
   *              "$serviceName" => array (
   *                  "serviceName" => $serviceName,
   *                  "displayName" => $displayName,
   *                  "description" => $desc,
   *                  "attributes"  => array( "isMaster" => boolean, "isClient" => boolean )
   *                ),
   *                ...
   *           )
   *         );
   */
  public function getAllServicesList() {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT service_name, description, display_name, attributes FROM Services";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["services"] = array();
    foreach ($result as $entry) {
      $svcInfo = array();
      $svcInfo["serviceName"] = $entry["service_name"];
      $svcInfo["displayName"] = $entry["display_name"];
      $svcInfo["description"] = $entry["description"];
      $svcInfo["attributes"] = json_decode($entry["attributes"], true);
      $response["services"][$svcInfo["serviceName"]] = $svcInfo;
    }
    LockRelease(); return $response;
  }

  /**
   * Get all components for all services that are supported.
   * @return
   *   array (
   *           "result" => 0,
   *           "error" => "",
   *           "services" => array (
   *              "$serviceName" => array (
   *                  "serviceName" => $serviceName,
   *                  "components" => array (
   *                     "$componentName" => array (
   *                        "componentName" =>
   *                        "isMaster" =>
   *                        "isClient" =>
   *                        "displayName" =>
   *                        "description" =>
   *                        )
   *                    )
   *                )
   *           )
   *       )
   */
  public function getAllServiceComponentsList() {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT service_name, component_name, display_name"
        . ", attributes, description FROM ServiceComponents";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["services"] = array();
    foreach ($result as $entry) {
      $serviceName = $entry["service_name"];
      if (!isset($response["services"]["$serviceName"])) {
        $response["services"][$serviceName] = array();
        $response["services"][$serviceName]["serviceName"] = $serviceName;
        $response["services"][$serviceName]["components"] = array();
      }
      $comp = array();
      $comp["serviceName"] = $serviceName;
      $comp["componentName"] = $entry["component_name"];
      $comp["displayName"] = $entry["display_name"];
      $comp["attributes"] = json_decode($entry["attributes"], true);
      /* $comp["isMaster"] and $comp["isClient"] are populated only for
       * backward compatibility (with when is_master and is_client were
       * fields in this table) and should be removed once all the code
       * that uses this method is fixed to look at
       * $comp["attributes"]["isMaster"] directly.
       */
      $comp["isMaster"] = FALSE;
      if (isset($comp["attributes"]["isMaster"])) {
        $comp["isMaster"] = $comp["attributes"]["isMaster"];
      }
      $comp["isClient"] = FALSE;
      if (isset($comp["attributes"]["isClient"])) {
        $comp["isClient"] = $comp["attributes"]["isClient"];
      }
      $response["services"][$serviceName]["components"]
          [$comp["componentName"]] = $comp;
    }
    LockRelease(); return $response;
  }

  /**
   * Get the list of all services that are available for the given clusterName
   * @return mixed
   *   array (
   *           "result" => 0,
   *           "error" => "",
   *           "clusterName" => $clusterName,
   *           "services" => array (
   *              "$serviceName" => array (
   *                  "serviceName" => $serviceName,
   *                  "state" => $state,
   *                  "desiredState" => $desiredState,
   *                  "isEnabled" => $isEnabled
   *                ),
   *                ...
   *           )
   *         );
   */
  public function getAllServicesInfo($clusterName) {
    LockAcquire();
    $query = "SELECT ServiceInfo.service_name as service_name"
        . ", ServiceInfo.state as state"
        . ", ServiceInfo.desired_state as desired_state"
        . ", ServiceInfo.is_enabled as is_enabled FROM ServiceInfo "
        . " LEFT JOIN Services ON ServiceInfo.service_name = Services.service_name "
        . " WHERE ServiceInfo.cluster_name = " . $this->dbHandle->quote($clusterName)
        . " ORDER BY Services.id ";
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["services"] = array();
    foreach ($result as $entry) {
      $svcInfo = array();
      $svcInfo["serviceName"] = $entry["service_name"];
      $svcInfo["state"] = $entry["state"];
      $svcInfo["desiredState"] = $entry["desired_state"];
      $svcInfo["isEnabled"] = $entry["is_enabled"];
      $response["services"][$svcInfo["serviceName"]] = $svcInfo;
    }
    LockRelease(); return $response;
  }

  /**
   * Get information related to a service
   * @param string $clusterName
   * @param string $serviceName
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "serviceName" => $serviceName,
   *       "isEnabled" => $isEnabled,
   *       "state" => $state,
   *       "desiredState" =>  $desiredState
   *    )
   */
  public function getServiceInfo($clusterName, $serviceName) {
    LockAcquire();
    $error = "";
    $query = "SELECT is_enabled, state, desired_state FROM ServiceInfo "
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND service_name = " . $this->dbHandle->quote($serviceName);
    $response = array ( "result" => 0, "error" => "");
    $response["clusterName"] = $clusterName;
    $response["serviceName"] = $serviceName;
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["isEnabled"] = $result[0]["is_enabled"];
      $response["state"] = $result[0]["state"];
      $response["desiredState"] = $result[0]["desired_state"];
      LockRelease(); return $response;
    }
    $response["result"] = 1;
    $response["error"] = "Could not find service in DB";
    LockRelease(); return $response;
  }

  /**
   * Set Service Desired State
   * @param clusterName Cluster Name
   * @param serviceName Service Name
   * @param desiredState New Desired State
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "desiredState" => $desiredState,
   *       "oldDesiredState" => $oldDesiredState,
   *       "clusterName" =>
   *       "serviceName" =>
   *    )
   */
  public function setServiceDesiredState($clusterName, $serviceName, $desiredState) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "",
        "clusterName" => $clusterName, "serviceName" => $serviceName);
    $error = "";
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $query = "SELECT desired_state FROM ServiceInfo"
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND service_name = " . $this->dbHandle->quote($serviceName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["oldDesiredState"] = $result[0]["desired_state"];
      $query = "UPDATE ServiceInfo SET desired_state = "
          . $this->dbHandle->quote($desiredState)
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
          . " AND service_name = " . $this->dbHandle->quote($serviceName);
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $ret = $this->dbHandle->commit();
      if (!$ret) {
        $error = $this->getLastDBErrorAsString();
        $response["result"] = 1;
        $response["error"] = "Failed to commit DB transaction, error=".$error;
        LockRelease(); return $response;
      }
      $response["desiredState"] = $desiredState;
      LockRelease(); return $response;
    }
    $this->dbHandle->rollBack();
    $response["result"] = 1;
    $response["error"] = "Could not find service in DB";
    LockRelease(); return $response;
  }

  /**
   * Set Service State
   * @param clusterName Cluster Name
   * @param serviceName Service Name
   * @param desiredState New Desired State
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "state" => $state,
   *       "oldState" => $oldState,
   *       "clusterName" =>
   *       "serviceName" =>
   *    )
   */
  public function setServiceState($clusterName, $serviceName, $state) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "",
        "clusterName" => $clusterName, "serviceName" => $serviceName);
    $error = "";
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $query = "SELECT state FROM ServiceInfo"
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND service_name = " . $this->dbHandle->quote($serviceName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["oldState"] = $result[0]["state"];
      $query = "UPDATE ServiceInfo SET state = "
          . $this->dbHandle->quote($state)
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
          . " AND service_name = " . $this->dbHandle->quote($serviceName);
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $ret = $this->dbHandle->commit();
      if (!$ret) {
        $error = $this->getLastDBErrorAsString();
        $response["result"] = 1;
        $response["error"] = "Failed to commit DB transaction, error=".$error;
        LockRelease(); return $response;
      }
      $response["state"] = $state;
      LockRelease(); return $response;
    }
    $this->dbHandle->rollBack();
    $response["result"] = 1;
    $response["error"] = "Could not find service in DB";
    LockRelease(); return $response;
  }

  /**
   * Add list of services for cluster
   * @param string $clusterName
   * @param mixed $services
   *    array (
   *       array (
   *          "serviceName" => ,
   *          "state" =>
   *          "desiredState" =>
   *          "isEnabled" =>
   *        )
   *     )
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function addServicesToCluster($clusterName, $services) {
    $response = array ( "result" => 0, "error" => "",
        "clusterName" => $clusterName);
    $error = "";

    $supportedServices = $this->getAllServicesList();
    if ($supportedServices === FALSE
        || $supportedServices["result"] != 0) {
      return $supportedServices;
    }

    LockAcquire();
    // sanitize input
    $sanitizedSvcs = array();
    foreach ($services as $service) {
      if (!isset($service["serviceName"])
          || $service["serviceName"] == "") {
        $response["error"] = "Invalid arguments";
        $response["result"] = 1;
        LockRelease(); return $response;
      }
      if (!isset($supportedServices["services"][$service["serviceName"]])) {
        $response["error"] = "Invalid service - not supported, svc="
            . $service["serviceName"];
        $response["result"] = 1;
        LockRelease(); return $response;
      }
      if (!isset($service["state"])) {
        $service["state"] = "";
      }
      if (!isset($service["desiredState"])) {
        $service["desiredState"] = "";
      }
      if (!isset($service["isEnabled"])) {
        $service["isEnabled"] = 0;
      }
      else {
        if ($service["isEnabled"]) {
          $service["isEnabled"] = 1;
        }
        else {
          $service["isEnabled"] = 0;
        }
      }
      $sanitizedSvcs[$service["serviceName"]] = $service;
    }

    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($sanitizedSvcs as $service) {
      $query = "INSERT INTO ServiceInfo "
        . " ( cluster_name, service_name, state, desired_state, is_enabled ) "
        . " VALUES "
        . " ("
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote($service["serviceName"]) . " , "
        . $this->dbHandle->quote($service["state"]) . " , "
        . $this->dbHandle->quote($service["desiredState"]) . " , "
        . $service["isEnabled"]
        . " )";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
        . ", query=".$query
        . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

/////////////////////////////////////////// End of services related APIs ////////////////////////////////////////////

/////////////////////////////////////////// Hosts related APIs ////////////////////////////////////////////

  /**
   * Add new hosts to Hosts table for a given cluster.
   * Replaces existing hosts if needed
   * @param string $clusterName ClusterName
   * @param mixed $hostsInfo Mixed array containing host info
   *   array (
   *     array (
   *           "hostName" =>
   *           "ip" =>
   *           "totalMem" =>
   *           "cpuCount" =>
   *           "osArch" =>
   *           "osType" =>
   *           "os" =>
   *           "disksInfo" =>
   *           "discoveryStatus" =>
   *           "badHealthReason" =>
   *           "attributes" => array ( ... )
   *         ),
   *      )
   * @return mixed
   *   array ( "clusterName" => $clusterName,
   *           "hosts" => array ($hostName1, $hostName2 ...),
   *           "result" => 0,
   *           "error" => ""
   *        );
   */
  public function addHostsToCluster($clusterName, $hostsInfo) {
    LockAcquire();
    $response = array( "result" => 0, "error" => "");

    if (empty($hostsInfo)) {
      LockRelease(); return $response;
    }

    $hosts = array();
    if (empty($hostsInfo)) {
      return $response;
    }

    foreach ($hostsInfo as $hostInfo) {
      if (!isset($hostInfo["hostName"])
          || $hostInfo["hostName"] == ""
          || !isset($hostInfo["discoveryStatus"])) {
        $response["result"] = 1;
        $response["error"] = "Invalid arguments";
        LockRelease(); return $response;
      }
      // sanitize input
      if (!isset($hostInfo["totalMem"])) {
        $hostInfo["totalMem"] = 0;
      }
      if (!isset($hostInfo["cpuCount"])) {
        $hostInfo["cpuCount"] = 0;
      }
      if (!isset($hostInfo["osArch"])) {
        $hostInfo["os_arch"] = "";
      }
      if (!isset($hostInfo["osType"])) {
        $hostInfo["osType"] = "";
      }
      if (!isset($hostInfo["os"])) {
        $hostInfo["os"] = "";
      }
      if (!isset($hostInfo["disksInfo"])) {
        $hostInfo["disksInfo"] = "";
      }
      if (!isset($hostInfo["badHealthReason"])) {
        $hostInfo["badHealthReason"] = "";
      }
      if (!isset($hostInfo["attributes"])) {
        $hostInfo["attributes"] = array();
      }
      $hostInfo["attributes"] = json_encode($hostInfo["attributes"]);
      array_push($hosts, $hostInfo);
    }

    $response["clusterName"] = $clusterName;

    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($hosts as $hostInfo) {
      $this->logger->log_debug(json_encode(($hostInfo)));
      $query = " INSERT OR REPLACE INTO Hosts "
        . " ( cluster_name, host_name, ip, total_mem, cpu_count, "
        . " os_arch, os_type, os, disks_info, discovery_status, "
        . " bad_health_reason, attributes ) "
        . " VALUES "
        . " ("
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote($hostInfo["hostName"]) . " , "
        . $this->dbHandle->quote($hostInfo["ip"]) . " , "
        . $this->dbHandle->quote($hostInfo["totalMem"]) . " , "
        . $this->dbHandle->quote($hostInfo["cpuCount"]) . " , "
        . $this->dbHandle->quote($hostInfo["osArch"]) . " , "
        . $this->dbHandle->quote($hostInfo["osType"]) . " , "
        . $this->dbHandle->quote($hostInfo["os"]) . " , "
        . $this->dbHandle->quote($hostInfo["disksInfo"]) . " , "
        . $this->dbHandle->quote($hostInfo["discoveryStatus"]) . " , "
        . $this->dbHandle->quote($hostInfo["badHealthReason"]) . " , "
        . $this->dbHandle->quote($hostInfo["attributes"])
        . " )  ";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $error = "";
    $query = "SELECT host_name FROM Hosts WHERE "
        . " ( cluster_name = " . $this->dbHandle->quote($clusterName) . " ) AND ( ";
    $index = 0;
    foreach ($hostsInfo as $hostInfo) {
      if ($index != 0) {
        $query .= " OR ";
      }
      $index++;
      $query .= " host_name = " . $this->dbHandle->quote($hostInfo["hostName"]);
    }
    $query .= " ) ";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["hosts"] = array();
    if (isset($result) && is_array($result) && count($result) > 0) {
      foreach ($result as $entry) {
        $host["hostName"] = $entry["host_name"];
        array_push($response["hosts"], $entry["host_name"]);
      }
      LockRelease(); return $response;
    }
    $response["result"] = 1;
    $response["error"] = "Invalid result set returned from DB";
    LockRelease(); return $response;
  }

  /**
   * Add new hosts to Hosts table for a given cluster.
   * Replaces existing hosts if needed
   * @param string $clusterName ClusterName
   * @param mixed $hostsInfo Mixed array containing host info
   *   array (
   *     "$hostName" => array (
   *           "discoveryStatus" =>
   *           "badHealthReason" =>
   *         ),
   *      )
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  public function updateHostDiscoveryStatus($clusterName, $hostsInfo) {
    LockAcquire();
    $response = array( "result" => 0, "error" => "");

    $hosts = array();
    foreach ($hostsInfo as $hostName => $hostInfo) {
      if (!isset($hostInfo["discoveryStatus"])) {
        $response["result"] = 1;
        $response["error"] = "Invalid arguments";
        LockRelease(); return $response;
      }
      if (!isset($hostInfo["badHealthReason"])) {
        $hostInfo["badHealthReason"] = "";
      }
      $hosts[$hostName] = $hostInfo;
    }
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $rowsChanged = 0;
    foreach ($hosts as $hostName => $hostInfo) {
      $this->logger->log_debug(json_encode(($hostInfo)));
      $query = " UPDATE Hosts SET "
          . " discovery_status = "
          . $this->dbHandle->quote($hostInfo["discoveryStatus"])
          . ", bad_health_reason = "
          . $this->dbHandle->quote($hostInfo["badHealthReason"])
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
          . " AND host_name = " . $this->dbHandle->quote($hostName);
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $rowsChanged += $ret;
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }


  /**
   * Get information on all hosts
   * @param string $clusterName
   * @param mixed $filter
   *   - only supports equal operand
   *   - only supports discoveryStatus filter
   *   - array (
   *      "=" => array ( "discoveryStatus" => "SUCCESS" )
   *      )
   *   - format
   *     "$operand" => array ( "$column1" => "$value1", ... )
   * @param mixed $order order by particular column
   *   - only supports hostName, ip, totalMem, cpuCount, osArch, osType
   *    array (
   *      "sortColumn" => "totalMem",
   *      "sortOrder" => "ASC/DESC"
   *      )
   *   - optionally, an array of [ "sortColumn" => $sortColumn, "sortOrder" => $sortOrder]
   *     can be used
   *
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "hosts" => array (
   *           array (
   *               "hostName" =>
   *               "ip" =>
   *               "totalMem" =>
   *               "cpuCount" =>
   *               "osArch" =>
   *               "osType" =>
   *               "os" =>
   *               "disksInfo" =>
   *               "discoveryStatus" =>
   *               "badHealthReason" =>
   *               "attributes" => array ( ... )
   *           ),
   *           ...
   *        )
   *     )
   */
  public function getAllHostsInfo($clusterName, $filter = array(),
      $order = array()) {
    LockAcquire();
    $error = "";
    $query = "SELECT "
        . " cluster_name, host_name, ip, total_mem, "
        . " cpu_count, os_arch, os_type, os, disks_info, "
        . " discovery_status, bad_health_reason, attributes "
        . " FROM Hosts WHERE host_name != ''";

    if ($clusterName != "") {
      $query .= " AND cluster_name = "
          . $this->dbHandle->quote($clusterName);
    }

    if (is_array($filter) && !empty($filter)) {
      foreach ($filter as $operand => $cols) {
        if ($operand == "=" || $operand == "!=") {
          foreach ($cols as $columnName => $value) {
            if ($columnName == "discoveryStatus") {
              $query .= " AND discovery_status " . $operand . " "
                  . $this->dbHandle->quote($value);
            }
          }
        }
      }
    }
    $using_sort = FALSE;
    if (isset($order) && is_array($order)) {
      if (sizeof($order) > 0) {
        $query .= " ORDER BY ";
        $using_sort = TRUE;
      }
      while (sizeof($order) > 0) {
        // is it an array of arrays, for an array of sortColumn and sortOrder?        
        if (isset($order[0]) && is_array($order[0])) {
          $this->logger->log_debug('Order is array of arrays');
          $orderItem = array_shift($order);
        } else {
          $orderItem["sortColumn"] = array_shift($order);
          $orderItem["sortOrder"] = array_shift($order);
        }
        if (isset($orderItem["sortColumn"])) {
          if ($orderItem["sortColumn"] == "hostName") {
            $query .= "host_name";
          }
          else if ($orderItem["sortColumn"] == "ip") {
            $query .= "ip";
          }
          else if ($orderItem["sortColumn"] == "totalMem") {
            $query .= "total_mem";
          }
          else if ($orderItem["sortColumn"] == "cpuCount") {
            $query .= "cpu_count";
          }
          else if ($orderItem["sortColumn"] == "osArch") {
            $query .= "os_arch";
          }
          else if ($orderItem["sortColumn"] == "osType") {
            $query .= "os_type";
          }
          if (isset($orderItem["sortOrder"])) {
            $query .= " ".$orderItem["sortOrder"];
            if (sizeof($order) > 0) {
              $query .= ',';
            }
          }
        }
      }
    }

    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["hosts"] = array();
    $response["clusterName"] = $clusterName;
    foreach ($result as $entry) {
      $host = array();
      $host["hostName"] = $entry["host_name"];
      $host["ip"] = $entry["ip"];
      $host["totalMem"] = $entry["total_mem"];
      $host["cpuCount"] = $entry["cpu_count"];
      $host["osArch"] = $entry["os_arch"];
      $host["osType"] = $entry["os_type"];
      $host["os"] = $entry["os"];
      $host["disksInfo"] = $entry["disks_info"];
      $host["discoveryStatus"] = $entry["discovery_status"];
      $host["badHealthReason"] = $entry["bad_health_reason"];
      $host["attributes"] = json_decode($entry["attributes"], true);
      $host["clusterName"] = $entry["cluster_name"];
      array_push($response["hosts"], $host);
    }
    LockRelease(); return $response;
  }

  /**
  * Get information on a specified host
  * @param string $clusterName
  * @param string $hostName
  * @return mixed
  *   array (
  *       "result" => 0,
  *       "error" => "",
  *       "clusterName" => $clusterName,
  *       "hostName" =>
  *       "ip" =>
  *       "totalMem" =>
  *       "cpuCount" =>
  *       "osArch" =>
  *       "osType" =>
  *       "os" =>
  *       "disksInfo" =>
  *       "discoveryStatus" =>
  *       "badHealthReason" =>
  *       "attributes" => array ( ... )
  *     )
  */
  public function getHostInfo($clusterName, $hostName) {
    LockAcquire();
    $error = "";
    $query = "SELECT "
        . " host_name, ip, total_mem, "
        . " cpu_count, os_arch, os_type, os, disks_info, "
        . " discovery_status, bad_health_reason, attributes "
        . " FROM Hosts WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND host_name = " . $this->dbHandle->quote($hostName);
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if ($clusterName != "") {
      $response["clusterName"] = $clusterName;
    }
    if (isset($result) && is_array($result) && count($result) == 1) {
      $entry = $result[0];
      $response["clusterName"] = $entry["cluster_name"];
      $response["hostName"] = $entry["host_name"];
      $response["ip"] = $entry["ip"];
      $response["totalMem"] = $entry["total_mem"];
      $response["cpuCount"] = $entry["cpu_count"];
      $response["osArch"] = $entry["os_arch"];
      $response["osType"] = $entry["os_type"];
      $response["os"] = $entry["os"];
      $response["disksInfo"] = $entry["disks_info"];
      $response["discoveryStatus"] = $entry["discovery_status"];
      $response["badHealthReason"] = $entry["bad_health_reason"];
      $response["attributes"] = json_decode($entry["attributes"], true);
      LockRelease(); return $response;
    }
    $response["result"] = 1;
    $response["error"] = "Could not find host in DB";
    LockRelease(); return $response;
  }

/////////////////////////////////////////// End of hosts related APIs ////////////////////////////////////////////

/////////////////////////////////////////// Configuration related APIs ////////////////////////////////////////////

  /**
   * Get meta info about all config properties
   * @return mixed
   *  array (
   *        "result" => 0,
   *        "error" => "",
   *        "configs" => array(
   *                   "key" => array (
   *                       "key" => $key,
   *                       "displayName" => $displayName,
   *                       "serviceName" => $serviceName
   *                       "description" => $description,
   *                       "displayType" => $displayType,
   *                       "displayAttributes" => array ( ... )
   *                       "value" => $default_value
   *                      )
   *                    .....
   *     );
  */
  public function getConfigPropertiesMetaInfo() {
    LockAcquire();
    $error = "";
    $query = "SELECT "
        . " key, display_name, service_name, description, default_value"
        . ", display_type, display_attributes "
        . " FROM ConfigProperties";
    $response = array ( "result" => 0, "error" => "", "configs" => array());
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    foreach ($result as $entry) {
      $prop = array();
      $prop["key"] = $entry["key"];
      $prop["displayName"] = $entry["display_name"];
      $prop["serviceName"] = $entry["service_name"];
      $prop["description"] = $entry["description"];
      $prop["displayType"] = $entry["display_type"];
      $prop["displayAttributes"] = json_decode($entry["display_attributes"], true);
      $prop["value"] = $entry["default_value"];
      $response["configs"][$prop["key"]] = $prop;
    }
    LockRelease(); return $response;
  }

  /**
   * Get all configuration properties for a given cluster
   * @param string $clusterName
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "properties" => array (
   *           "key1" => "val1",
   *           "key2" => "val2",
   *           ...
   *         )
   *      )
   */
  public function getServiceConfig($clusterName) {
    LockAcquire();
    $error = "";
    $query = "SELECT key, value FROM ServiceConfig "
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName);
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["properties"] = array();
    $response["clusterName"] = $clusterName;
    foreach ($result as $entry) {
      $response["properties"][$entry["key"]] = $entry["value"];
    }
    LockRelease(); return $response;
  }

  /**
   * Update config properties for a given cluster
   * Updates prop key if it exists or inserts a new entry if not found.
   * @param string $clusterName
   * @param mixed $config
   * 	array of properties as key-val pairs
   *    $config = array ( "prop_key1" => "prop_val1", ... );
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  public function updateServiceConfigs($clusterName, $config) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    if (count($config) == 0) {
      // no-op
      LockRelease(); return $response;
    }
    $error = "";
    $rowsChanged = 0;
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($config as $key=>$val) {
      $query = " INSERT OR REPLACE INTO ServiceConfig "
        . " ( cluster_name, key, value ) "
        . " VALUES "
        . " ( " . $this->dbHandle->quote($clusterName) . ", "
        . $this->dbHandle->quote($key) . ", "
        . $this->dbHandle->quote($val) . " ) ";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $ret = $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $rowsChanged += $ret;
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=". $error;
      LockRelease(); return $response;
    }
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
  * Update config properties for a given cluster
  * Updates prop key if it exists or inserts a new entry if not found.
  * @param string $clusterName
  * @param mixed $config
  * 	array of properties as key-val pairs
  *    $config =
  *      array (
  *        "component" => array (
  *           "hostname" =>
  *              array ( "prop_key1" => "prop_val1", ... )
  *           )
  *        )
  * @return mixed
  *   array (
  *       "result" => 0,
  *       "error" => "",
  *       "sql" => array ( "rowsChanged" => 1 )
  *    )
  */
  public function updateHostRoleConfigs($clusterName, $config) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    if (count($config) == 0) {
      // no-op
      LockRelease(); return $response;
    }
    $error = "";
    $rowsChanged = 0;
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($config as $compName => $hostsInfo) {
      foreach ($hostsInfo as $hostName => $configProps) {
        foreach ($configProps as $key => $val) {
          $query = " INSERT OR REPLACE INTO HostRoleConfig "
              . " ( cluster_name, component_name, host_name"
              . ", key, value ) "
              . " VALUES "
              . " ( " . $this->dbHandle->quote($clusterName) . ", "
              . $this->dbHandle->quote($compName) . ", "
              . $this->dbHandle->quote($hostName) . ", "
              . $this->dbHandle->quote($key) . ", "
              . $this->dbHandle->quote($val) . " ) ";
          $this->logger->log_trace("Running query: $query");
          $ret = $this->dbHandle->exec($query);
          if (FALSE === $ret) {
            $error = $this->getLastDBErrorAsString();
            $ret = $this->dbHandle->rollBack();
            $this->logger->log_error("Error when executing query"
                . ", query=".$query
                . ", error=".$error);
            $response["result"] = 1;
            $response["error"] = $error;
            LockRelease(); return $response;
          }
          $rowsChanged += $ret;
        }
      }
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=". $error;
      LockRelease(); return $response;
    }
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Get host role configs
   * @param string $clusterName
   * @param string $componentName ( optional )
   */
  public function getHostRoleConfigs($clusterName, $componentName = "") {
    LockAcquire();
    $error = "";
    $query = "SELECT component_name, host_name, key, value FROM HostRoleConfig "
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName);
    if ($componentName != "") {
      $query .= " AND component_name = "
          . $this->dbHandle->quote($componentName);
    }

    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["properties"] = array();
    foreach ($result as $entry) {
      $compName = $entry["component_name"];
      $hostName = $entry["host_name"];
      $key = $entry["key"];
      $val = $entry["value"];
      if (!isset($response["properties"][$compName])) {
        $response["properties"][$compName] = array();
      }
      if (!isset($response["properties"][$compName][$hostName])) {
        $response["properties"][$compName][$hostName] = array();
      }
      $response["properties"][$compName][$hostName][$key] = $val;
    }
    LockRelease(); return $response;


  }

/////////////////////////////////////////// Configuration related APIs ////////////////////////////////////////////

/////////////////////////////////////////// Component related APIs ////////////////////////////////////////////

  /**
   * Get all components for a given service.
   * @param string $serviceName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "serviceName" => $serviceName,
   *       "components" => array (
   *           "$componentName" => array (
   *               "componentName" =>
   *               "displayName" =>
   *               "isMaster" =>
   *               "isClient" =>
   *               "description" =>
   *             )
   *         )
   *      )
   */
  public function getAllServiceComponents($serviceName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT component_name, display_name, attributes, description"
        . " FROM ServiceComponents WHERE service_name = "
        . $this->dbHandle->quote($serviceName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["serviceName"] = $serviceName;
    $response["components"] = array();
    foreach ($result as $entry) {
      $comp = array();
      $comp["componentName"] = $entry["component_name"];
      $comp["displayName"] = $entry["display_name"];
      $comp["description"] = $entry["description"];
      $attributes = json_decode($entry["attributes"], true);
      $comp["isMaster"] = FALSE;
      $comp["isClient"] = FALSE;
      if (isset($attributes["isMaster"])) {
        $comp["isMaster"] = $attributes["isMaster"];
      }
      if (isset($attributes["isClient"])) {
        $comp["isClient"] = $attributes["isClient"];
      }
      $response["components"][$comp["componentName"]] = $comp;
    }
    LockRelease(); return $response;
  }

 /**
   * Add host-role mappings for given hostnames to the specified component
   * @param string $clusterName
   * @param string $componentName
   * @param array $hosts Array of hostnames
   * @param string $state
   * @param string $desiredState
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function addHostsToComponent($clusterName, $componentName,
      $hosts, $state, $desiredState) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");

    if ($hosts == NULL || empty($hosts)) {
      LockRelease(); return $response;
    } else {
      $noOp = TRUE;
      foreach ($hosts as $host) {
        if (isset($host)) {
          $h = trim($host);
          if ($h != "") {
            $noOp = FALSE;
            break;
          }
        }
      }
      if ($noOp) {
        $this->logger->log_warn("Invalid hosts array passed in to function"
            . ", hosts=" . implode(",", $hosts));
        LockRelease(); return $response;
      }
    }

    $error = "";
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($hosts as $host) {
      $query = " INSERT OR REPLACE INTO HostRoles "
          . " ( cluster_name, component_name"
          . ", host_name, state, desired_state ) "
          . " VALUES "
          . " ( " . $this->dbHandle->quote($clusterName) . ", "
          . $this->dbHandle->quote($componentName) . ", "
          . $this->dbHandle->quote($host) . ", "
          . $this->dbHandle->quote($state) . ", "
          . $this->dbHandle->quote($desiredState) . " ) ";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $ret = $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
            . ", query=".$query
            . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=". $error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  /**
  * Get all hosts mapped to any component
  * @param string $clusterName
  * @return mixed
  *    array (
  *       "result" => 0,
  *       "error" => "",
  *       "clusterName" => $clusterName,
  *       "components" => array (
  *          "$componentName" = array (
  *              "componentName" => $componentName,
  *              "hosts" => array (
  *                  "$hostName" => array (
  *                      "hostName" =>
  *                      "state" =>
  *                      "desiredState" =>
  *                     )
  *                  )
  *              )
  *          )
  *     )
  */
  public function getAllHostsByComponent($clusterName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $error = "";
    $query = "SELECT role_id, component_name, host_name, state, desired_state"
        . " FROM HostRoles"
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["components"] = array();
    foreach ($result as $entry) {
      $componentName = $entry["component_name"];
      if (!isset($response["components"][$componentName])) {
        $response["components"][$componentName] = array();
        $response["components"][$componentName]["componentName"] =
            $componentName;
        $response["components"][$componentName]["hosts"] = array();
      }
      $hostInfo = array();
      $hostInfo["hostName"] = $entry["host_name"];
      $hostInfo["roleId"] = $entry["role_id"];
      $hostInfo["state"] = $entry["state"];
      $hostInfo["desiredState"] = $entry["desired_state"];
      $response["components"][$componentName]["hosts"]
          [$hostInfo["hostName"]] = $hostInfo;
    }
    LockRelease(); return $response;
  }

  /**
   * Get all hosts mapped to all the  components
   * @param string $clusterName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "hosts" => array (
   *           "hostName" =>
   *          "components" = array (
   *              "componentName", .....
   *              )
   *          )
   *     )
   */
  public function getAllHostsToComponentMap($clusterName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $error = "";
    $query = "SELECT component_name, host_name, state, desired_state "
      . " FROM HostRoles"
      . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["hosts"] = array();
    foreach ($result as $entry) {
      $hostName = $entry["host_name"];
      if (!isset($response["hosts"][$hostName])) {
        $response["hosts"][$hostName] = array();
        $initComponents = array();
        array_push($initComponents, $entry["component_name"]);
        $response["hosts"][$hostName]["components"] =
          $initComponents;
      } else {
        $components = $response["hosts"][$hostName]["components"];
        array_push($components, $entry["component_name"]);
        $response["hosts"][$hostName]["components"] = $components;
      }
    }
    LockRelease(); return $response;
  }

  /**
   * Get hosts mapped to a given component
   * @param string $clusterName
   * @param string $componentName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "componentName" => $componentName,
   *       "hosts" => array (
   *           "$hostName" => array (
   *               "hostName" =>
   *               "state" =>
   *               "desiredState" =>
   *             )
   *         )
   *     )
   */
  public function getHostsForComponent($clusterName, $componentName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $error = "";
    $query = "SELECT host_name, state, desired_state FROM HostRoles"
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND component_name = " . $this->dbHandle->quote($componentName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["hosts"] = array();
    $response["clusterName"] = $clusterName;
    $response["componentName"] = $componentName;
    foreach ($result as $entry) {
      $hostInfo = array();
      $hostInfo["hostName"] = $entry["host_name"];
      $hostInfo["state"] = $entry["state"];
      $hostInfo["desiredState"] = $entry["desired_state"];
      $response["hosts"][$hostInfo["hostName"]] = $hostInfo;
    }
    LockRelease(); return $response;
  }

  /**
   * Set the state for specified hosts mapped to given component/role
   * @param string $clusterName
   * @param string $componentName
   * @param array $hosts
   * @param string $state
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  public function setHostsStateForComponent($clusterName, $componentName,
      $hosts, $state) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    if (count($hosts) == 0) {
      LockRelease(); return $response;
    }
    $query = " UPDATE HostRoles SET state = " . $this->dbHandle->quote($state)
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND component_name = " . $this->dbHandle->quote($componentName)
        . " AND ( ";
    $index = 0;
    foreach ($hosts as $host) {
      if ($index != 0) {
        $query .= " OR ";
      }
      $index++;
      $query .= " host_name = " . $this->dbHandle->quote($host);
    }
    $query .= " ) ";
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = $ret;
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Set the state for specified hosts mapped to given component/role
   * @param string $clusterName
   * @param string $componentName
   * @param array $hosts
   * @param string $desiredState
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  public function setHostsDesiredStateForComponent($clusterName, $componentName,
      $hosts, $desiredState) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    if (count($hosts) == 0) {
      LockRelease(); return $response;
    }
    $query = " UPDATE HostRoles SET desired_state = " . $this->dbHandle->quote($desiredState)
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND component_name = " . $this->dbHandle->quote($componentName)
        . " AND ( ";
    $index = 0;
    foreach ($hosts as $host) {
      if ($index != 0) {
        $query .= " OR ";
      }
      $index++;
      $query .= " host_name = " . $this->dbHandle->quote($host);
    }
    $query .= " ) ";
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = $ret;
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Get services that the given service depends upon
   * @param string $serviceName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "serviceName" =>
   *       "serviceDependencies" = array ( "svc1", "svc2", ... )
   *     )
   */
  public function getServiceDependencies($serviceName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT to_service_name FROM ServiceDependencies WHERE "
        . " from_service_name = " . $this->dbHandle->quote($serviceName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["serviceName"] = $serviceName;
    $response["serviceDependencies"] = array();
    foreach ($result as $entry) {
      array_push($response["serviceDependencies"], $entry["to_service_name"]);
    }
    LockRelease(); return $response;
  }

  /**
   * Get services which depend on the given service.
   * @param string $serviceName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "serviceName" =>
   *       "serviceDependents" = array ( "svc1", "svc2", ... )
   *     )
   */
  public function getServiceDependents($serviceName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT from_service_name FROM ServiceDependencies WHERE "
    . " to_service_name = " . $this->dbHandle->quote($serviceName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["serviceName"] = $serviceName;
    $response["serviceDependents"] = array();
    foreach ($result as $entry) {
      array_push($response["serviceDependents"], $entry["from_service_name"]);
    }
    LockRelease(); return $response;
  }

  /**
   * Get components that the given component depends upon
   * @param string $componentName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "componentName" =>
   *       "componentDependencies" = array ( "comp1", "comp2", ... )
   *    )
   */
  public function getServiceComponentDependencies($componentName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT to_component_name "
        . " FROM ServiceComponentDependencies WHERE "
        . " from_component_name = "
        . $this->dbHandle->quote($componentName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["componentName"] = $componentName;
    $response["componentDependencies"] = array();
    foreach ($result as $entry) {
      array_push($response["componentDependencies"],
          $entry["to_component_name"]);
    }
    LockRelease(); return $response;
  }

  /**
   * Get components that the given component depends upon
   * @param string $componentName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "serviceName" =>
   *       "componentName" =>
   *       "componentDependents" = array ( "comp1", "comp2", ... )
   *     )
   */
  public function getServiceComponentDependents($componentName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT from_component_name "
        . " FROM ServiceComponentDependencies WHERE "
        . " to_component_name = "
        . $this->dbHandle->quote($componentName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["componentName"] = $componentName;
    $response["componentDependents"] = array();
    foreach ($result as $entry) {
      array_push($response["componentDependents"],
          $entry["from_component_name"]);
    }
    LockRelease(); return $response;
  }

  /**
   * Add Service Components to a given cluster
   * @param string $clusterName
   * @param mixed $components
   *    array (
   *      "$serviceName" => array (
   *         "$componentName" =>
   *            "state" =>
   *            "desiredState" =>
   *          )
   *       )
   *    )
   */
  public function addServiceComponentsToCluster($clusterName, $components) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "",
        "clusterName" => $clusterName);
    $error = "";
    $supportedComps = $this->getAllServiceComponentsList();
    if ($supportedComps === FALSE
        || $supportedComps["result"] != 0) {
      LockRelease(); return $supportedComps;
    }

    // sanitize input
    $sanitizedComps = array();
    foreach ($components as $svc=>$comps) {
      if (!isset($comps)
          || !is_array($comps)
          || count($comps) == 0) {
        $this->logger->log_warn("Invalid args to addServiceComponentsToCluster"
            . ", svc=" . $svc
            . ", comps either null or empty");
        continue;
      }

      if (!isset($supportedComps["services"][$svc])
          || !is_array($supportedComps["services"][$svc])
          || !isset($supportedComps["services"][$svc]["components"])
          || !is_array($supportedComps["services"][$svc]["components"])) {
        $response["error"] = "Invalid service - not supported, svc="
            . $svc;
        $response["result"] = 1;
        LockRelease(); return $response;
      }
      foreach ($comps as $compName=>$comp) {
        if (!isset($supportedComps["services"][$svc]["components"][$compName])
            || !is_array($supportedComps["services"]
                        [$svc]["components"][$compName])) {
          $response["error"] = "Invalid component - not supported, svc="
              . $svc . ", component=" . $compName;
          $response["result"] = 1;
          LockRelease(); return $response;
        }
        $saneComp = array(); // yes - it really is sane as in not insane
        $saneComp["serviceName"] = $svc;
        $saneComp["componentName"] = $compName;
        $saneComp["state"] = "";
        $saneComp["desiredState"] = "";
        if (isset($comp["state"])) {
          $saneComp["state"] = $comp["state"];
        }
        if (isset($comp["desiredState"])) {
          $saneComp["desiredState"] = $comp["desiredState"];
        }
        array_push($sanitizedComps, $saneComp);
      }
    }

    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    foreach ($sanitizedComps as $comp) {
      $query = "INSERT INTO ServiceComponentInfo "
        . " ( cluster_name, service_name, component_name"
        . ", state, desired_state ) "
        . " VALUES "
        . " ("
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote($comp["serviceName"]) . " , "
        . $this->dbHandle->quote($comp["componentName"]) . " , "
        . $this->dbHandle->quote($comp["state"]) . " , "
        . $this->dbHandle->quote($comp["desiredState"])
        . " )";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
        . ", query=".$query
        . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  /**
   * Get all components for all services within the cluster
   * @param string $clusterName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "services" => array (
   *           "$serviceName" => array (
   *              "serviceName" => $serviceName,
   *              "components" => array (
   *                  "$componentName" => array (
   *                     "componentName" =>
   *                     "state" =>
   *                     "desiredState" =>
   *                   )
   *                )
   *             )
   *         )
   */
  public function getAllServiceComponentsInfo($clusterName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT service_name, component_name, state, desired_state "
       . " FROM ServiceComponentInfo WHERE "
       . " cluster_name = " . $this->dbHandle->quote($clusterName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["services"] = array();
    foreach ($result as $entry) {
      $serviceName = $entry["service_name"];
      if (!isset($response["services"]["$serviceName"])) {
        $response["services"][$serviceName] = array();
        $response["services"][$serviceName]["serviceName"] = $serviceName;
        $response["services"][$serviceName]["components"] = array();
      }
      $comp = array();
      $comp["serviceName"] = $serviceName;
      $comp["componentName"] = $entry["component_name"];
      $comp["state"] = $entry["state"];
      $comp["desiredState"] = $entry["desired_state"];
      $response["services"][$serviceName]["components"]
          [$comp["componentName"]] = $comp;
    }
    LockRelease(); return $response;
  }

  /*
  public function getServiceComponentInfo($clusterName, $componentName) {
    LockAcquire();
    // TODO
    // use getAllServiceComponentsInfo for now
  }
  */

  private function genericSetServiceComponentState($clusterName, $componentName,
      $state, $updateAllMappedHosts, $updateDesired) {
    LockAcquire();
    $columnName = "state";
    if ($updateDesired) {
      $columnName = "desired_state";
    }
    $response = array ( "result" => 0, "error" => "");
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $query = "UPDATE ServiceComponentInfo SET " . $columnName . " = "
        . $this->dbHandle->quote($state)
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND component_name = " . $this->dbHandle->quote($componentName);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    if ($updateAllMappedHosts) {
      $query = "UPDATE HostRoles SET " . $columnName . " = "
          . $this->dbHandle->quote($state)
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
          . " AND component_name = " . $this->dbHandle->quote($componentName);
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
        . ", query=".$query
            . ", error=".$error);
            $response["result"] = 1;
            $response["error"] = $error;
        LockRelease(); return $response;
      }
    }

    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  /**
   * Update the state for a specified component. Based on update hosts flag,
   * set the state for all the hosts mapped to that component too.
   * @param string $clusterName
   * @param string $componentName
   * @param string $state
   * @param bool $updateAllMappedHosts
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *    )
   */
  public function setServiceComponentState($clusterName, $componentName,
      $state, $updateAllMappedHosts) {
    return $this->genericSetServiceComponentState($clusterName, $componentName,
      $state, $updateAllMappedHosts, FALSE);
  }

  /**
   * Update the desired state for a specified component. Based on update hosts
   * flag, set the desired state for all the hosts mapped to that component too.
   * @param string $clusterName
   * @param string $componentName
   * @param string $desiredState
   * @param bool $updateAllMappedHosts
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *    )
   */
  public function setServiceComponentDesiredState($clusterName, $componentName,
      $desiredState, $updateAllMappedHosts) {
    return $this->genericSetServiceComponentState($clusterName, $componentName,
      $desiredState, $updateAllMappedHosts, TRUE);
  }

  // TODO add apis for config history

  // TODO add apis for txn status table

  /**
   * Create a new transaction
   * @param string $statusInfo Status blob to attach to txn in DB
   * @param string $pidInfo Pid Info to attach to txn
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "txnId" => $txn_id
   *    )
   */
  public function createNewTransaction($clusterName, $statusInfo, $pidInfo) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $query = "INSERT INTO TransactionStatus ( cluster_name, create_time"
        . ", status_info, pid_info ) VALUES ( "
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote(time()) . " , "
        . $this->dbHandle->quote($statusInfo) . " , "
        . $this->dbHandle->quote($pidInfo) . " ) ";
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    $lastInsertId = intval($this->dbHandle->lastInsertId());
    if ($lastInsertId == 0) {
      $this->dbHandle->rollBack();
      $error = "Obtained invalid last insert id from DB";
      $this->logger->log_error($error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $response["txnId"] = $lastInsertId;
    LockRelease(); return $response;
  }

  /**
   * Get transaction Info
   * @param string $clusterName
   * @param int $txnId
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" => $clusterName,
   *       "txnId" => $txn_id,
   *       "statusInfo" => $statusInfo,
   *       "pidInfo" => $pidInfo,
   *       "createTime" => $createTime
   *    )
   */
  public function getTransactionStatusInfo($clusterName, $txnId) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT create_time, status_info, pid_info "
        . " FROM TransactionStatus WHERE "
        . " cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND txn_id = " . $this->dbHandle->quote($txnId);
    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["txnId"] = $txnId;
    if (isset($result) && is_array($result) && count($result) == 1) {
      $entry = $result[0];
      $response["createTime"] = $entry["create_time"];
      $response["statusInfo"] = $entry["status_info"];
      $response["pidInfo"] = $entry["pid_info"];
      LockRelease(); return $response;
    }
    $response["result"] = 1;
    $response["error"] = "Could not find txn in DB";
    LockRelease(); return $response;
  }

  /**
   * Update pid info for a given transaction
   * @param string $clusterName
   * @param string $txnId
   * @param string $pidInfo
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function updateTransactionPidInfo($clusterName, $txnId, $pidInfo) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "UPDATE TransactionStatus SET pid_info = "
        . $this->dbHandle->quote($pidInfo)
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND txn_id = " . $this->dbHandle->quote($txnId);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = intval($ret);
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Update status info for a given transaction
   * @param string $clusterName
   * @param string $txnId
   * @param string $statusInfo
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function updateTransactionStatusInfo($clusterName, $txnId, $statusInfo) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "UPDATE TransactionStatus SET status_info = "
        . $this->dbHandle->quote($statusInfo)
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND txn_id = " . $this->dbHandle->quote($txnId);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = intval($ret);
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Add a new sub-txn
   * @param string $clusterName
   * @param int $txnId
   * @param int $subTxnId
   * @param int $parentSubTxnId
   * @param string $state
   * @param string $description
   * @param string $progress
   * @param string $subTxnType
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function insertOrUpdateSubTransaction($clusterName, $txnId, $subTxnId,
      $parentSubTxnId, $state, $description, $progress, $subTxnType) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");

    $query = "SELECT cluster_name, txn_id, sub_txn_id, op_status "
        . " FROM SubTransactionStatus "
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND txn_id = " . $this->dbHandle->quote($txnId)
        . " AND sub_txn_id = " . $this->dbHandle->quote($subTxnId);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    $opStatus = json_encode(array());
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (is_array($result) && count($result) == 1) {
      $opStatus = $result[0]["op_status"];
    }

    $query = "INSERT OR REPLACE INTO SubTransactionStatus "
        . " ( cluster_name, txn_id, sub_txn_id, parent_sub_txn_id"
        . ", state, description, progress, sub_txn_type, op_status ) VALUES ( "
        . $this->dbHandle->quote($clusterName) . " , "
        . $this->dbHandle->quote($txnId) . " , "
        . $this->dbHandle->quote($subTxnId) . " , "
        . $this->dbHandle->quote($parentSubTxnId) . " , "
        . $this->dbHandle->quote($state) . " , "
        . $this->dbHandle->quote($description) . " , "
        . $this->dbHandle->quote($progress) . " , "
        . $this->dbHandle->quote($subTxnType) . " , "
        . $this->dbHandle->quote($opStatus) . " ) ";
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = intval($ret);
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);

    LockRelease(); return $response;
  }

  /**
   * Get all sub transactions
   * Enter description here ...
   * @param string $clusterName
   * @param int $txnId
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" =>
   *       "txnId" =>
   *       "subTxns" => array (
   *          $subTxnId => array (
   *             "subTxnId" =>
   *             "parentSubTxnId" =>
   *             "state" =>
   *             "description" =>
   *             "progress" =>
   *             "subTxnType" =>
   *             "opStatus" =>
   *            )
   *         )
   *     )
   */
  public function getAllSubTransactionsInfo($clusterName, $txnId) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT sub_txn_id, parent_sub_txn_id, state, description"
        . ", progress, sub_txn_type, op_status FROM SubTransactionStatus "
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND txn_id = " . $this->dbHandle->quote($txnId);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["txnId"] = $txnId;
    $response["subTxns"] = array();
    foreach ($result as $entry) {
      $subTxn = array();
      $subTxn["subTxnId"] = $entry["sub_txn_id"];
      $subTxn["parentSubTxnId"] = $entry["parent_sub_txn_id"];
      $subTxn["state"] = $entry["state"];
      $subTxn["description"] = $entry["description"];
      $subTxn["progress"] = $entry["progress"];
      $subTxn["subTxnType"] = $entry["sub_txn_type"];
      $subTxn["opStatus"] = $entry["op_status"];
      $response["subTxns"][$entry["sub_txn_id"]] = $subTxn;
    }
    LockRelease(); return $response;
  }

  /**
   * Update op status for sub txn
   * @param string $clusterName
   * @param int $txnId
   * @param int $subTxnId
   * @param string $progress
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function updateSubTransactionProgress($clusterName, $txnId,
      $subTxnId, $progress) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "UPDATE SubTransactionStatus SET progress = "
       . $this->dbHandle->quote($progress)
       . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
       . " AND txn_id = " . $this->dbHandle->quote($txnId)
       . " AND sub_txn_id = " . $this->dbHandle->quote($subTxnId);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = intval($ret);
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Update op status for sub txn
   * @param string $clusterName
   * @param int $txnId
   * @param int $subTxnId
   * @param string $opStatus
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function updateSubTransactionOpStatus($clusterName, $txnId,
      $subTxnId, $opStatus) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "UPDATE SubTransactionStatus SET op_status = "
       . $this->dbHandle->quote($opStatus)
       . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
       . " AND txn_id = " . $this->dbHandle->quote($txnId)
       . " AND sub_txn_id = " . $this->dbHandle->quote($subTxnId);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $rowsChanged = intval($ret);
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  /**
   * Get roles which host belongs to
   * @param string $clusterName
   * @param string $hostName
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => "",
   *       "clusterName" =>
   *       "hosts" => array (
   *         "$hostName" => array (
   *           "services" => array (
   *              "$serviceName" => array (
   *                 "components" => array (
   *                     "$compName1" => array (
   *                         "state" =>
   *                         "desiredState" =>
   *                     ),
   *                    "$compName2" =>
   *                    ...
   *                  )
   *               )
   *            )
   *         )
   *       )
   *    )
   */
  public function getRolesForHosts($clusterName, $hostNames) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    if (count($hostNames) == 0) {
      LockRelease(); return $response;
    }

    $query = "SELECT HostRoles.host_name as host_name"
        . ", HostRoles.component_name as component_name"
        . ", ServiceComponentInfo.service_name as service_name"
        . ", ServiceComponentInfo.state as component_state"
        . ", ServiceComponentInfo.desired_state as component_desired_state"
        . " FROM HostRoles LEFT JOIN ServiceComponentInfo "
        . " ON HostRoles.cluster_name = ServiceComponentInfo.cluster_name "
        . " AND HostRoles.component_name = ServiceComponentInfo.component_name "
        . " WHERE HostRoles.cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND  ( ";
    $index = 0;
    foreach ($hostNames as $hostName) {
      if ($index != 0) {
        $query .= " OR ";
      }
      ++$index;
      $query .= " HostRoles.host_name = " . $this->dbHandle->quote($hostName);
    }
    $query .= " ) ";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["clusterName"] = $clusterName;
    $response["hosts"] = array();
    foreach ($result as $entry) {
      if (!isset($response["hosts"][$entry["host_name"]])) {
        $response["hosts"][$entry["host_name"]] = array("services" => array());
      }

      if (!isset($entry["service_name"])
          || $entry["service_name"] == "") {
        $this->logger->log_warn("Invalid entry in host roles table, "
            . print_r($entry, true));
        continue;
      }
      if (!isset($response["hosts"][$entry["host_name"]]["services"][$entry["service_name"]])) {
        $response["hosts"][$entry["host_name"]]["services"][$entry["service_name"]] =
            array( "components" => array());
      }
      $compInfo = array();
      $compInfo["componentName"] = $entry["component_name"];
      $compInfo["state"] = $entry["component_state"];
      $compInfo["desiredState"] = $entry["component_desired_state"];
      $response["hosts"][$entry["host_name"]]["services"][$entry["service_name"]]
          ["components"][$entry["component_name"]] = $compInfo;
    }
    LockRelease(); return $response;
  }

  /**
   * Dump out the contents of the ServiceDependencies table
   * @return mixed
   *     array (
   *        "result" =>
   *        "error" =>
   *        "serviceDependencies" => array (
   *            "svcName" => array (
   *               "svcDep1",
   *               ...
   *             ),
   *             ...
   *          )
   *      )
   */
  public function getAllServiceDependencies() {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");
    $query = "SELECT from_service_name, to_service_name "
        . " FROM ServiceDependencies";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);

    $svcDeps = array();
    foreach ($result as $entry) {
      if (!isset($svcDeps[$entry["from_service_name"]])) {
        $svcDeps[$entry["from_service_name"]] = array();
      }
      array_push($svcDeps[$entry["from_service_name"]],
          $entry["to_service_name"]);
    }
    $response["serviceDependencies"] = $svcDeps;
    LockRelease(); return $response;
  }

  /**
   * Get recursive dependencies for a given service.
   * @param mixed $serviceDependencies Full Service Dependencies list as
   *    obtained from getAllServiceDependencies()
   * @param string $serviceName Service name to get the dependencies for.
   * @return array of service dependencies.
   */
  public function getRecursiveServiceDependency($serviceDependencies,
      $serviceName) {
    if (!isset($serviceDependencies[$serviceName])) {
      return array();
    }
    $svcDeps = array();

    $bfsQ = array ( $serviceName );
    $seen = array();
    while (count($bfsQ) > 0) {
      $svc = array_shift($bfsQ);
      if (isset($seen[$svc])) {
        continue;
      }
      if ($svc != $serviceName) {
        array_push($svcDeps, $svc);
      }
      $seen[$svc] = TRUE;
      if (!isset($serviceDependencies[$svc])) {
        continue;
      }
      $bfsQ = array_merge($bfsQ, $serviceDependencies[$svc]);
    }
    return $svcDeps;
  }

  /**
  * Get recursive dependents for a given service.
  * @param mixed $serviceDependencies Full Service Dependencies list as
  *    obtained from getAllServiceDependencies()
  * @param string $serviceName Service name to get the dependents for.
  * @return array of service dependents.
  */
  public function getRecursiveServiceDependents($serviceDependencies,
    $serviceName) {
    $serviceDependents = array();
    foreach ($serviceDependencies as $fromSvc => $toSvcs) {
      foreach ($toSvcs as $toSvc) {
        if (!isset($serviceDependents[$toSvc])) {
          $serviceDependents[$toSvc] = array();
        }
        array_push($serviceDependents[$toSvc], $fromSvc);
      }
    }

    if (!isset($serviceDependents[$serviceName])) {
      return array();
    }
    $svcDeps = array();

    $bfsQ = array ( $serviceName );
    $seen = array();
    while (count($bfsQ) > 0) {
      $svc = array_shift($bfsQ);
      if (isset($seen[$svc])) {
        continue;
      }
      if ($svc != $serviceName) {
        array_push($svcDeps, $svc);
      }
      $seen[$svc] = TRUE;
      if (!isset($serviceDependents[$svc])) {
        continue;
      }
      $bfsQ = array_merge($bfsQ, $serviceDependents[$svc]);
    }
    return $svcDeps;
  }

  /**
   * Set host state
   * @param string $clusterName
   * @param mixed $hostsToUpdate
   *   array (
   *      "$svc" => array (
   *         "$compName" => array (
   *            "host1", "host2" ...
   *            )
   *         )
   *      )
   * @param string $state
   * @param boolean $updateDesired
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  private function setGenericHostsState($clusterName, $hostsToUpdate,
      $state, $updateDesired) {
    LockAcquire();
    $columnName = "state";
    if ($updateDesired) {
      $columnName = "desired_state";
    }
    $response = array ( "result" => 0, "error" => "");
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $rowsChanged = 0;
    foreach ($hostsToUpdate as $svcName => $compList) {
      foreach ($compList as $compName => $hostList) {
        if (count($hostList) == 0) {
          continue;
        }
        $query = "UPDATE HostRoles SET " . $columnName . " = "
            . $this->dbHandle->quote($state) . " WHERE "
            . " cluster_name = " . $this->dbHandle->quote($clusterName)
            . " AND component_name = " . $this->dbHandle->quote($compName)
            . " AND ( ";
        $index = 0;
        foreach ($hostList as $host) {
          if ($index != 0) {
            $query .= " OR ";
          }
          ++$index;
          $query .= " host_name = " . $this->dbHandle->quote($host);
        }
        $query .= " ) ";
        $this->logger->log_trace("Running query: $query");
        $ret = $this->dbHandle->exec($query);
        if (FALSE === $ret) {
          $error = $this->getLastDBErrorAsString();
          $this->dbHandle->rollBack();
          $this->logger->log_error("Error when executing query"
              . ", query=".$query
              . ", error=".$error);
          $response["result"] = 1;
          $response["error"] = $error;
          LockRelease(); return $response;
        }
        $rowsChanged += $ret;
      }
    }

    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }


  /**
  * Set host state
  * @param string $clusterName
  * @param mixed $hostsToUpdate
  *   array (
  *      "$svc" => array (
  *         "$compName" => array (
  *            "host1", "host2" ...
  *            )
  *         )
  *      )
  * @param string $state
  * @return mixed
  *   array (
  *       "result" => 0,
  *       "error" => "",
  *       "sql" => array ( "rowsChanged" => 1 )
  *    )
  */
  public function setHostsState($clusterName, $hostsToUpdate, $state) {
    return $this->setGenericHostsState($clusterName, $hostsToUpdate, $state,
        FALSE);
  }

  /**
  * Set host state
  * @param string $clusterName
  * @param mixed $hostsToUpdate
  *   array (
  *      "$svc" => array (
  *         "$compName" => array (
  *            "host1", "host2" ...
  *            )
  *         )
  *      )
  * @param string $state Desired State
  * @return mixed
  *   array (
  *       "result" => 0,
  *       "error" => "",
  *       "sql" => array ( "rowsChanged" => 1 )
  *    )
  */
  public function setHostsDesiredState($clusterName, $hostsToUpdate, $state) {
    return $this->setGenericHostsState($clusterName, $hostsToUpdate, $state,
        TRUE);
  }

  /**
   * Set host role state to match components to which it belongs to
   * @param mixed $hosts
   *   array (
   *      "$svc" => array (
   *         "$compName" => array (
   *            "host1", "host2" ...
   *            )
   *         )
   *      )
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "sql" => array ( "rowsChanged" => 1 )
   *    )
   */
  private function genericMatchHostStateToComponent($clusterName, $hosts,
      $updateDesired) {
    $response = array ("result" => 0, "error" => "");
    if (count($hosts) == 0) {
      return $response;
    }
    $columnName = "state";
    if ($updateDesired) {
      $columnName = "desired_state";
    }

    $compList = array();
    foreach ($hosts as $svc => $compInfo) {
      foreach ($compInfo as $compName => $hostList)
      $compList[$compName] = $hostList;
    }

    if (count($compList) == 0) {
      return $response;
    }

    LockAcquire();
    $query = "SELECT component_name, ". $columnName . " FROM "
        . " ServiceComponentInfo WHERE "
        . " cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND ( " ;

    $index = 0;
    foreach ($compList as $compName => $foo) {
      if ($index != 0) {
        $query .= " OR ";
      }
      ++$index;
      $query .= " component_name = " . $this->dbHandle->quote($compName);
    }
    $query .= " ) ";
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);

    $compState = array();
    foreach ($result as $entry) {
      $compState[$entry["component_name"]] = $entry[$columnName];
    }

    $rowsChanged = 0;
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    foreach ($compList as $compName => $hostList) {
      if (count($hostList) == 0) {
        continue;
      }
      if (!isset($compState[$compName])) {
        continue;
      }

      $query = " UPDATE HostRoles SET " . $columnName . " = "
          . $this->dbHandle->quote($compState[$compName])
          . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
          . " AND component_name = " . $this->dbHandle->quote($compName)
          . " AND ( ";
      $index = 0;
      foreach ($hostList as $host) {
        if ($index != 0) {
          $query .= " OR ";
        }
        ++$index;
        $query .= " host_name = " . $this->dbHandle->quote($host);
      }
      $query .= " ) ";
      $this->logger->log_trace("Running query: $query");
      $ret = $this->dbHandle->exec($query);
      if (FALSE === $ret) {
        $error = $this->getLastDBErrorAsString();
        $this->dbHandle->rollBack();
        $this->logger->log_error("Error when executing query"
        . ", query=".$query
        . ", error=".$error);
        $response["result"] = 1;
        $response["error"] = $error;
        LockRelease(); return $response;
      }
      $rowsChanged += $ret;
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $response["sql"] = array ( "rowsChanged" => $rowsChanged);
    LockRelease(); return $response;
  }

  public function matchHostStateToComponent($clusterName, $hosts) {
    return $this->genericMatchHostStateToComponent($clusterName, $hosts, FALSE);
  }

  public function matchHostDesiredStateToComponent($clusterName, $hosts) {
    return $this->genericMatchHostStateToComponent($clusterName, $hosts, TRUE);
  }

  /**
   * Use the current set of configs in ServiceConfig to create a snapshot
   * in the ConfigHistory table
   * @param string $clusterName
   * @param string $changeLogMsg
   * @return mixed
   *   array (
   *       "result" => 0,
   *       "error" => "",
   *       "configSnapshotId" => $snapshotId
   *    )
   */
  public function createServiceConfigSnapshot($clusterName, $changeLogMsg) {
    $response = array ("result" => 0, "error" => "");
    $result = $this->getServiceConfig($clusterName);
    if ($result["result"] != 0) {
      return $result;
    }

    $configs = $result["properties"];

    $configBlob = json_encode(
         array ( "clusterName" => $clusterName,
                 "properties" => $configs ) );

    $curTime = time();

    LockAcquire();
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $query = "INSERT INTO ConfigHistory ( cluster_name, config, change_log, "
        . " update_time ) VALUES ( " . $this->dbHandle->quote($clusterName)
        . ", " . $this->dbHandle->quote($configBlob)
        . ", " . $this->dbHandle->quote($changeLogMsg)
        . ", " . $this->dbHandle->quote($curTime) . " ) ";
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    $lastInsertId = intval($this->dbHandle->lastInsertId());
    if ($lastInsertId == 0) {
      $this->dbHandle->rollBack();
      $error = "Obtained invalid last insert id from DB";
      $this->logger->log_error($error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }

    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=".$error;
      LockRelease(); return $response;
    }

    $response["configSnapshotId"] = $lastInsertId;
    LockRelease(); return $response;
  }


  public function getConfigSnapshot($clusterName, $snapshotId) {
    LockAcquire();
    $response = array ("result" => 0, "error" => "");

    $query = "SELECT config, change_log, update_time FROM ConfigHistory"
        . " WHERE cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND version = " . $this->dbHandle->quote($snapshotId);

    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);

    $response["clusterName"] = $clusterName;
    $response["configSnapshotId"] = $snapshotId;
    if (isset($result) && is_array($result) && count($result) == 1) {
      $response["changeLog"] = $result[0]["change_log"];
      $response["config"] = json_decode($result[0]["config"], true);
      $response["updateTime"] = $result[0]["update_time"];
      LockRelease(); return $response;
    }
    $response["result"] = 1;
    $response["error"] = "Could not find config snapshot in DB";
    LockRelease(); return $response;
  }

  private function deleteClusterTable ($clusterName, $table) {
    LockAcquire();
    $response = array ("result" => 0, "error" => "");
    $query = "DELETE from " . $table . " WHERE cluster_name = "
           . $this->dbHandle->quote($clusterName);
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt == FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  private function deleteAllInTable ($table) {
    LockAcquire();
    $response = array ("result" => 0, "error" => "");
    $query = "DELETE from " . $table;
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt == FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

  public function cleanupServices($clusterName) {
    $this->deleteClusterTable($clusterName, "ServiceConfig");
    $this->deleteClusterTable($clusterName, "ServiceInfo");
  }

  public function cleanupHosts ($clusterName) {
    $this->deleteClusterTable($clusterName, "Hosts");
    $this->deleteClusterTable($clusterName, "HostRoles");
    $this->cleanupServices($clusterName);
  }

  public function cleanupCluster ($clusterName) {
//    $this->deleteAllInTable("Clusters");
    $this->deleteClusterTable($clusterName, "ServiceComponentInfo");
    $this->cleanupHosts($clusterName);
  }

public function cleanAllHostRoles($clusterName) {
  LockAcquire();

  $response = array ("result" => 0, "error" => "");
  $query = "DELETE from HostRoles WHERE cluster_name = "
           . $this->dbHandle->quote($clusterName);
  $this->logger->log_trace("Running query: $query");
  $pdoStmt = $this->dbHandle->query($query);
  if ($pdoStmt == FALSE) {
    $error = $this->getLastDBErrorAsString();
    $this->logger->log_error("Error when executing query"
      . ", query=".$query
      . ", error=".$error);
    $response["result"] = 1;
    $response["error"] = $error;
    LockRelease(); return $response;
  }
  LockRelease(); return $response;

}

  public function wipeOutClusters () {
    $this->deleteAllInTable("Clusters");
    $this->deleteAllInTable("ServiceComponentInfo");
    $this->deleteAllInTable("Hosts");
    $this->deleteAllInTable("HostRoles");
    $this->deleteAllInTable("ServiceConfig");
    $this->deleteAllInTable("ServiceInfo");
  }


  /**
   * Returns all nodes that were successfully discovered but have no
   * components/roles assigned to them within the given cluster
   * Enter description here ...
   * @param unknown_type $clusterName
   */
  public function getAllUnassignedHosts($clusterName) {
    LockAcquire();
    $error = "";
    $query = "SELECT "
        . " Hosts.host_name, Hosts.ip, Hosts.total_mem, "
        . " Hosts.cpu_count, Hosts.os_arch, Hosts.os_type, Hosts.os, Hosts.disks_info, "
        . " Hosts.discovery_status, Hosts.bad_health_reason, Hosts.attributes "
        . " FROM Hosts LEFT JOIN HostRoles ON "
        . " Hosts.cluster_name = HostRoles.cluster_name "
        . " AND Hosts.host_name = HostRoles.host_name "
        . " WHERE Hosts.cluster_name = "
        . $this->dbHandle->quote($clusterName)
        . " AND Hosts.discovery_status = 'SUCCESS'"
        . " AND HostRoles.component_name ISNULL";

    $response = array ( "result" => 0, "error" => "");
    $this->logger->log_trace("Running query: $query");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    $response["hosts"] = array();
    $response["clusterName"] = $clusterName;
    foreach ($result as $entry) {
      $host = array();
      $host["hostName"] = $entry["host_name"];
      $host["ip"] = $entry["ip"];
      $host["totalMem"] = $entry["total_mem"];
      $host["cpuCount"] = $entry["cpu_count"];
      $host["osArch"] = $entry["os_arch"];
      $host["osType"] = $entry["os_type"];
      $host["os"] = $entry["os"];
      $host["disksInfo"] = $entry["disks_info"];
      $host["discoveryStatus"] = $entry["discovery_status"];
      $host["badHealthReason"] = $entry["bad_health_reason"];
      $host["attributes"] = json_decode($entry["attributes"], true);
      array_push($response["hosts"][$host["hostName"]], $host);
    }
    LockRelease(); return $response;
  }

 /**
   * Remove host-role mappings for given hostnames from the specified component
   * @param string $clusterName
   * @param string $componentName
   * @param array $hosts Array of hostnames
   * @return mixed
   *    array (
   *       "result" => 0,
   *       "error" => ""
   *     )
   */
  public function removeAllHostsFromComponent($clusterName, $componentName) {
    LockAcquire();
    $response = array ( "result" => 0, "error" => "");

    $error = "";
    $ret = $this->dbHandle->beginTransaction();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $response["result"] = 1;
      $response["error"] = "Failed to start DB transaction, error=".$error;
      LockRelease(); return $response;
    }
    $query = "DELETE FROM HostRoles WHERE "
        . " cluster_name = " . $this->dbHandle->quote($clusterName)
        . " AND component_name = " . $this->dbHandle->quote($componentName);
    $this->logger->log_trace("Running query: $query");
    $ret = $this->dbHandle->exec($query);
    if (FALSE === $ret) {
      $error = $this->getLastDBErrorAsString();
      $ret = $this->dbHandle->rollBack();
      $this->logger->log_error("Error when executing query"
          . ", query=".$query
          . ", error=".$error);
      $response["result"] = 1;
      $response["error"] = $error;
      LockRelease(); return $response;
    }
    $ret = $this->dbHandle->commit();
    if (!$ret) {
      $error = $this->getLastDBErrorAsString();
      $this->logger->log_error("Failed to commit DB transaction, error=".$error);
      $response["result"] = 1;
      $response["error"] = "Failed to commit DB transaction, error=". $error;
      LockRelease(); return $response;
    }
    LockRelease(); return $response;
  }

}
?>
