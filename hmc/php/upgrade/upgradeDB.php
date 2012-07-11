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


class UpgradeDB {

  private $dbHandle;

  private $dbPath;

  private $logger;

  function __construct($db) {
    $this->dbPath = $db;
    $dsn = "sqlite:".$db;
    if (!file_exists($db)) {
      print("Sqlite db file does not exist, db=$db");
      throw new Exception("Could not find sqlite db file, db=$db");
    }
    $this->dbHandle = new PDO($dsn);
    if ($this->dbHandle === FALSE) {
      $this->logger->log_error("Could not open sqlite db, db=$db");
      throw new Exception("Could not open sqlite db, db=$db");
    }
  }
  
  /**
    * Get the version of Ambari that needs to be upgraded
    * @return mixed
    * array ("version" => $ambariVersion,
    *        "result" => 0,
    *        "error" => "");
    */
  public function getAmbariVersion () {
    $query = "select name from sqlite_master where type = \"table\" and name LIKE \"AmbariConfig\"";
    $response = array( "result" => 0, "error" => "");
    $pdoStmt = $this->dbHandle->query($query);
    if ($pdoStmt === FALSE) {
      $error = "Failed to run sql query $query";
      print($error);
      $response["result"] = 1;
      $response["error"] = $error;
      return $response;
    }
    $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
    if (isset($result) && is_array($result) && count($result) == 0) {
      //there is no table named AmbariConfig - 1.0
      $response["version"] = "1.0.0";
    }
    else {
      $query = "SELECT version FROM AmbariConfig WHERE key_name = \"Ambari\" AND
        tag_name = \"Current\"";
      $pdoStmt = $this->dbHandle->query($query);
      if ($pdoStmt === FALSE) {
        $error = "Error when executing query $query";
        $response["result"] = 1;
        $response["error"] = $error;
        return $response;
      }
      $result = $pdoStmt->fetchAll(PDO::FETCH_BOTH);
      if (isset($result) && is_array($result) && count($result) == 1) {
        $response["version"] = $result[0]["version"];
      }
    }
    return $response;
  }
}
$upgradeDir = $argv[2];
$hmcHandle = new UpgradeDB($argv[1]);
$response = $hmcHandle->getAmbariVersion();
if ($response["version"] == "1.0.0") {
  $db = new PDO("sqlite:".$argv[1]);
  if ($handle = opendir($upgradeDir)) {
    while (false !== ($entry = readdir($handle))) {
      if ($entry != "." && $entry != "..") { 
        $absolutePath = $upgradeDir."/".$entry;
        $sql = file_get_contents($absolutePath);
        $db->exec($sql) or die(print_r($db->errorInfo(), true));
      }
    }
    closedir($handle);
  }
} else {
  // no need for upgrade
  //print ("No Need for upgrade");
}
?>
