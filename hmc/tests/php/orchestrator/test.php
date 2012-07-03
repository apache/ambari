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


include_once "Service.php";
include_once "Cluster.php";
include_once "../util/Logger.php";
include_once "../db/OrchestratorDB.php";
include_once "../db/Transaction.php";

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";
$GLOBALS["SUB_TXN_ID"] = 0;

$clusterName = "test-red";

class TestDB extends OrchestratorDB {

  private $hdfs;
  private $namenode;
  private $secondary_namenode;
  private $datanode;

  private $mapreduce;
  private $jobtracker;
  private $tasktracker;

  private $zookeeper;
  private $zookeeper_node;

  private $hbase;
  private $hbase_master;
  private $hbase_regionserver;

  function __construct($puppet) {
    parent::__construct("./test.db", "DataCluster", $puppet);

    $this->namenode = new ServiceComponent($clusterName, "namenode", "hdfs", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->secondary_namenode = new ServiceComponent($clusterName, "secondary_namenode", "hdfs", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->datanode = new ServiceComponent($clusterName, "datanode", "hdfs", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->hdfs = new Service($clusterName, "hdfs", State::UNINSTALLED, $this, $puppet);

    $this->jobtracker= new ServiceComponent($clusterName, "jobtracker", "mapreduce", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->tasktracker = new ServiceComponent($clusterName, "tasktracker", "mapreduce", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->mapreduce = new Service($clusterName, "mapreduce", State::UNINSTALLED, $this, $puppet);

    $this->zookeeper_node = new ServiceComponent($clusterName, "zookeeper_node", "zookeeper", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->zookeeper = new Service($clusterName, "zookeeper", State::UNINSTALLED, $this, $puppet);

    $this->hbase_master = new ServiceComponent($clusterName, "hbase_master", "hbase", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->hbase_regionserver = new ServiceComponent($clusterName, "hbase_regionserver", "hbase", State::UNINSTALLED, $this, $puppet, FALSE);
    $this->hbase = new Service($clusterName, "hbase", State::UNINSTALLED, $this, $puppet);
  }

  public function getClusterServices() {
    return array($this->hdfs, $this->zookeeper, $this->mapreduce, $this->hbase);
  }

  public function getServiceDependencies($serviceName) {
    switch ($serviceName) {
      case "hdfs":
      case "zookeeper":
        return array();

      case "mapreduce":
        return array($this->hdfs);

      case "hbase":
        return array($this->hdfs, $this->zookeeper);

      default:
        return "Unknown service $serviceName";
    }
  }

  public function getServiceDependents($serviceName) {
    switch ($serviceName) {
      case "hdfs":
        return array($this->mapreduce, $this->hbase);

      case "zookeeper":
        return array($this->hbase);

      case "mapreduce":
      case "hbase":
        return array();

      default:
       return "Unknown service $serviceName";
    }
  }

  public function getServiceComponents($serviceName) {
    switch ($serviceName) {
      case "hdfs":
        return array($this->namenode, $this->secondary_namenode, $this->datanode);

      case "zookeeper":
        return array($this->zookeeper_node);

      case "mapreduce":
        return array($this->jobtracker, $this->tasktracker);

      case "hbase":
        return array($this->hbase_master, $this->hbase_regionserver);

      default:
       return "Unknown service $serviceName";
    }
  }

  public function getComponentDependencies($serviceName, $componentName) {
    switch ($componentName) {
      case "namenode":
        return array();

      case "secondary_namenode":
        return array($this->namenode);

      case "datanode":
        return array($this->secondary_namenode);

      case "zookeeper_node":
        return array();

      case "jobtracker":
        return array();

      case "tasktracker":
        return array($this->jobtracker);

      case "hbase_master":
        return array();

      case "hbase_regionserver":
        return array($this->hbase_master);

      default:
       return "Unknown component $componentName";
    }
  }

  public function reset($state) {
    $this->hdfs->state = $state;
    $this->namenode->state = $state;
    $this->secondary_namenode->state = $state;
    $this->datanode->state = $state;

    $this->mapreduce->state = $state;
    $this->jobtracker->state = $state;
    $this->tasktracker->state = $state;

    $this->zookeeper->state = $state;
    $this->zookeeper_node->state = $state;

    $this->hbase->state = $state;
    $this->hbase_master->state = $state;
    $this->hbase_regionserver->state = $state;
  }

}

class MockPuppetInvoker extends PuppetInvoker {
  function __construct() {
    parent::__construct("test", 100);
  }

  public function install($txid) {
    print("MockPuppetInvoker::install\n");
    return array("result" => 0, "error" => 0);
  }

  public function kickPuppet($nodes, $transaction, $clusterName) {
    $trace = debug_backtrace();
    print("MockPuppetInvoker::kickPuppet from " . $trace[1]["function"] . " with " . $transaction->toString() . "\n");
    return array("result" => 0, "error" => 0, KICKSENT => array(), KICKFAILED => array(), FAILEDNODES => array(), SUCCESSFULLNODES => array());
  }

  public function kickServiceCheck($nodes, $transaction, $clusterName) {
    $trace = debug_backtrace();
    print("MockPuppetInvoker::kickServiceCheck from " . $trace[1]["function"] . " with " . $transaction->toString() . "\n");
    return array("result" => 0, "error" => 0, KICKSENT => array(), KICKFAILED => array(), FAILEDNODES => array(), SUCCESSFULLNODES => array());
  }
}

/*
print_r($hdfs->install());
print_r($hdfs->start());

print_r($mapreduce->install());
print_r($mapreduce->start());

print_r($hbase->install());
print_r($hbase->start());

print_r($hdfs->stop());
print_r($zookeeper->stop());
*/

$puppet = new MockPuppetInvoker();
$db = new OrchestratorDB("./test.db", "DataCluster", $puppet);
$cluster = new Cluster($clusterName, $db, $puppet);
$t0 = new Transaction(0, 0, 0);
print_r($cluster->deployHDP($t0));
$cluster->resetSubTxnId();
$t1 = new Transaction(1, 0, 0);
print_r($cluster->reconfigureServices($t1, array("MAPREDUCE")));
$t2 = new Transaction(2, 0, 0);
print_r($cluster->smokeServices($t2, array("HDFS", "MAPREDUCE")));
$cluster->resetSubTxnId();
$t20 = new Transaction(20, 0, 0);
print_r($cluster->stopAllServices($t20));
$cluster->resetSubTxnId();
?>
