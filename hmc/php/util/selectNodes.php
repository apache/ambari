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

class SelectNodes {
  private $logger;

  /**
   * Needs a default constructor else warnings will appear.
   */
  function __construct() {
    $this->logger = new HMCLogger("SelectNodes");
  }

  /** Helper function for creating an array for hostName and totalMem
   */
  function createHostMap($hostInfo) {
    $result = array("hostNames" => array($hostInfo["hostName"]));
    return $result;
  }
  /** Return only the enabled services.
   */
  function filterEnabledServices($services) {
    $enabledServices = array();
    foreach($services as $serviceName=>$serviceInfo) {
      if ($serviceInfo["isEnabled"] == 1) {
        $enabledServices[$serviceName] = $serviceInfo;
      }
    }
    return $enabledServices;
  }

  /**
   * Helper function to add HDFS NameNode
   */
  function addNameNode($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("HDFS", $serviceInfo)) {

      $result["mastersToHosts"]["NAMENODE"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add SNameNode
   */
  function addSNameNode($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("HDFS", $serviceInfo)) {
      $result["mastersToHosts"]["SNAMENODE"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add JobTracker.
   */
  function addJobTracker($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("MAPREDUCE", $serviceInfo)) {
      $result["mastersToHosts"]["JOBTRACKER"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add HBase Master.
   */
  function addHBaseMaster($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("HBASE", $serviceInfo)) {
      $result["mastersToHosts"]["HBASE_MASTER"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add Oozie server.
   */
  function addOozieServer($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("OOZIE", $serviceInfo)) {
      $result["mastersToHosts"]["OOZIE_SERVER"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add Hive Server.
   */
  function addHiveServer($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("HIVE", $serviceInfo)) {
      $result["mastersToHosts"]["HIVE_SERVER"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add Templeton.
   */
  function addTempletonServer($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("TEMPLETON", $serviceInfo)) {
      $result["mastersToHosts"]["TEMPLETON_SERVER"] = $this->createHostMap($hostInfo);
    }
    return $result;
  }

  /**
   * Helper function to add ZooKeeper master server.
   */
  function addZooKeeperServer($serviceInfo, $result, $hostInfo) {
    if (array_key_exists("ZOOKEEPER", $serviceInfo)) { 
      if (array_key_exists("ZOOKEEPER_SERVER", $result["mastersToHosts"])) {
        array_push($result["mastersToHosts"]["ZOOKEEPER_SERVER"]["hostNames"],
            $hostInfo["hostName"]);
      } else {
        $result["mastersToHosts"]["ZOOKEEPER_SERVER"] = $this->createHostMap($hostInfo);
      }
    }
    return $result;
  }

  /**
   * Helper function to add Ganglia master server.
   */
  function addGangliaServer($result, $hostInfo) {
    $result["mastersToHosts"]["GANGLIA_MONITOR_SERVER"] = $this->createHostMap($hostInfo);
    return $result;
  }

  /**
   * Helper function to add Nagios server.
   */
  function addNagiosServer($result, $hostInfo) {
    $result["mastersToHosts"]["NAGIOS_SERVER"] = $this->createHostMap($hostInfo);
    return $result;
  }

  /**
   * Adds all the slaves to the hostlist given whats enabled
   */
  function addSlaves($db, $hostlist, $clusterName, $services, $gangliaMaster) {
    $db->addHostsToComponent($clusterName, "TASKTRACKER", $hostlist, "ASSIGNED", "");
    $db->addHostsToComponent($clusterName, "DATANODE", $hostlist, "ASSIGNED", "");
    if (array_key_exists("HBASE", $services)) {
      $db->addHostsToComponent($clusterName, "HBASE_REGIONSERVER", $hostlist, "ASSIGNED", "");
    }
    if (array_key_exists("GANGLIA", $services)) {
      if (sizeof($hostlist) > 0) {
        $db->addHostsToComponent($clusterName, "GANGLIA_MONITOR", $hostlist, "ASSIGNED", "");
      }
    }
  }

  /**
   * Return a list of hosts excluding the ones in
   * $excludeHosts
   */
  function getExcludeHosts($allHosts, $excludeHosts) {
    $result = array();
    $found = FALSE;
    foreach ($allHosts as $host) {
      foreach($excludeHosts as $exclude) {
        if ($host == $exclude) {
          $found = TRUE;
          break;
        }
        $found = FALSE;
      }
      if (!$found) {
        array_push($result, $host);
      }
    }
    return $result;
  }

  /**
   * Return a list of slaves given what machines
   * masters are running on given the set of all host
   * and services enabled.
   */
  function getSlaveList($allHosts, $masterToHost, $services) {
    $result = array();
    $numNodes = sizeof($allHosts);
    if ($numNodes == 1) {
      array_push($result, $allHosts[0]);
      return $result;
    }
    if ($numNodes <= 5) {
      /* all slaves except for the namenode */
      $excludeList = $this->getExcludeHosts($allHosts, $masterToHost["NAMENODE"]);
      return $excludeList;
    }
    if ($numNodes > 5) {
      /* all slaves except for the namenode/JT/Hbase master */
      $excludeHosts = array();
      array_push($excludeHosts, $masterToHost["NAMENODE"][0]);
      array_push($excludeHosts, $masterToHost["JOBTRACKER"][0]);
      if (array_key_exists("HBASE", $services)) {
        array_push($excludeHosts, $masterToHost["HBASE_MASTER"][0]);
      }
      $excludeList = $this->getExcludeHosts($allHosts, $excludeHosts);
      return $excludeList;
    }
  }

  /**
   * convert hostInfo to a flat list
   */
  function convertHostInfoToList($hostsInfo) {
    $result = array();
    foreach($hostsInfo as $host) {
      array_push($result, $host["hostName"]);
    }
    return $result;
  }

  /**
   * Function to update the DB with roles on what user has selected.
   * This also selects the DataNodes/TaskTrackers/RegionServers based
   * on what services were picked.
   * @return Error or not
   *     array("result" => $result, "error" => $error)
   * @param clusterName name of the cluster
   * @param db the database being used
   * @param masterToHost the master to Host mapping that the user selected
   *          array("componentName" => hostName")
   */
  public function updateDBWithRoles($clusterName, $db, $masterToHost) {
    $return = array();
    $return["result"] = 0;
    $return["error"] = "";
    $this->logger->log_info("Updating with Info from User: \n".$clusterName."\n "
        .print_r($masterToHost, true));
    $allHostsDBInfo = $db->getAllHostsInfo($clusterName,
        array("=" => array ( "discoveryStatus" => "SUCCESS")), array());
    if ($allHostsDBInfo["result"] != 0) {
      $this->logger->log_error("Issue getting all hosts info ".$allHostsDBInfo["error"]);
      $return["result"] = $allHostsDBInfo["result"];
      $return["error"] = $allHostsDBInfo["error"];
      return $return;
    }
    $allHosts_t = $allHostsDBInfo["hosts"];
    /* get all enabled services */
    $servicesDBInfo = $db->getAllServicesInfo($clusterName);
    if ($servicesDBInfo["result"] != 0) {
      $this->logger->log_error("Issue getting all services enabled ".$allHostsDBInfo["error"]);
      $return["result"] = $servicesDBInfo["result"];
      $return["error"] = $servicesDBInfo["error"];
      return $return;
    }
    $services_tmp = $servicesDBInfo["services"];
    $services = $this->filterEnabledServices($services_tmp);
    $clusterInfo = $db->getClusterState($clusterName);
    if ($clusterInfo["result"] != 0) {
      $this->logger->log_error("Error getting cluster state ". $clusterInfo["error"]);
      $return["result"] = $clusterInfo["result"];
      $return["error"] = $clusterInfo["error"];
      return $return;
    }
    $clusterState = json_decode($clusterInfo[state],true);
    if($clusterState[state] == "CONFIGURATION_IN_PROGRESS") {
      $hostRolesResult = $db->cleanAllHostRoles($clusterName);
      if ($hostRolesResult["result"] != 0) {
        $this->logger->log_error("Issue cleaning all host roles ".$hostRolesResult["error"]);
        $return["result"] = $hostRolesResult["result"];
        $return["error"] = $hostRolesResult["error"];
        return $return;
      }
    }

    $allHosts = $this->convertHostInfoToList($allHosts_t);
    foreach($masterToHost as $componentName=>$hostNames) {
      $this->logger->log_info("For cluster  $clusterName setting $componentName to host \n". print_r($hostNames, true));
      $db->addHostsToComponent($clusterName, $componentName, $hostNames, "ASSIGNED", "");
      if ($componentName == "GANGLIA_MONITOR_SERVER") {
        $gangliaMaster = $hostNames[0];
      }
      if ($componentName == "ZOOKEEPER_SERVER") { 
        $sizeHosts = sizeof($hostNames);
        if ($sizeHosts == 1) {
          $hostConfig = array ( "ZOOKEEPER_SERVER" => array( $hostNames[0] => array ( "myid" => 1 )));
          $db->updateHostRoleConfigs($clusterName, $hostConfig);
        } else {
          $hostConfig = array( "ZOOKEEPER_SERVER" => array() );
          for ($i=0; $i < 3; $i++) {
            $hostConfig["ZOOKEEPER_SERVER"][$hostNames[$i]] = array ( "myid" => $i+1 );
          }
          $db->updateHostRoleConfigs($clusterName, $hostConfig);
        }
      }
    }
    /** make sure ganglia is added to all the masters **/
    $masterHosts = array();
    foreach($masterToHost as $componentName=>$hostNames) {
      foreach($hostNames as $hostName) {
        array_push($masterHosts, $hostName);
        $this->logger->log_debug("Adding host $hostName for GANGLIA_MONITOR");
      }
    }
    $db->addHostsToComponent($clusterName, "GANGLIA_MONITOR", $masterHosts, "ASSIGNED", "");

    // add DASHBOARD component
    $dashhostName = strtolower(exec('hostname -f'));
    $db->addHostsToComponent($clusterName, "DASHBOARD" , array($dashhostName), "ASSIGNED", "");

    $slaveList = $this->getSlaveList($allHosts, $masterToHost, $services);
    $this->logger->log_info("Slave List \n".print_r($slaveList, true));
    $this->addSlaves($db, $slaveList, $clusterName, $services, $gangliaMaster);
    /* pick a node for gateway */
    $gateway = $slaveList[0];
    //    print_r($services);
    foreach ($services as $key=>$s) {
      $serviceName = $s["serviceName"];
      if ($serviceName != "GANGLIA" && $serviceName != "NAGIOS" && $serviceName != "MISCELLANEOUS" && $serviceName != "DASHBOARD") {
        $db->addHostsToComponent($clusterName, $serviceName."_CLIENT", array($gateway), "ASSIGNED", "");
      }
    }
    return;
  }

  function excludeHMCHost($allHostsInfo) {
    $result = array();
    $hmcHostName = trim(strtolower(exec('hostname -f')));
    foreach($allHostsInfo as $hostInfo) {
      if ($hostInfo["hostName"] != $hmcHostName) {
        array_push($result, $hostInfo);
      }
    }
    $this->logger->log_debug("List for picking Nagios/Ganglia masters from \n".print_r($result, true));
    return $result;
  }

  /**
   * Function to select a list of nodes assuming
   * it gets all the info from the db
   * @param clustername the name of the cluster we are deploying/managing
   * @param db database from where to read, usually pass in new HMCDBAccessor("mydb.data");
   * @return mixed
   *  array (
   *       "result" => 0,
   *       "error" => "",
   *       "mastersToHosts" => array(
   *        "masterName" => array(array(
   *                     "hostname", "totalMem")) -- this in case we have multiple hosts to suggest.
   *       );
   */

  public function selectNodes($clustername, $db) {
    $return = array();
    $order = array(
        array("sortColumn" => "totalMem", "sortOrder" => "DESC"),
        array("sortColumn" => "cpuCount", "sortOrder" => "DESC"),
        array("sortColumn" => "hostName", "sortOrder" => "ASC"),
        );
    $allHostsDBInfo = $db->getAllHostsInfo($clustername,
        array("=" => array ( "discoveryStatus" => "SUCCESS")) , $order);
    if ($allHostsDBInfo["result"] != 0) {
      $this->logger->log_error("Issue getting all hosts info ".$allHostsDBInfo["error"]);
      $return["result"] = $allHostsDBInfo["result"];
      $return ["error"] = $allHostsDBInfo["error"];
      return $return;
    }
    $allHostsInfo = $allHostsDBInfo["hosts"];
    $numNodes = sizeof($allHostsInfo);
    $this->logger->log_info("Size of Cluster ".$numNodes);
    $servicesDBInfo = $db->getAllServicesInfo($clustername);
    if ($servicesDBInfo["result"] != 0) {
      $this->logger->log_error("Issue getting all services enabled ".$allHostsDBInfo["error"]);
      $return["result"] = $servicesDBInfo["result"];
      $return["error"] = $servicesDBInfo["error"];
      return $return;
    }
    $services_tmp = $servicesDBInfo["services"];
    $services = $this->filterEnabledServices($services_tmp);
    $numServices = sizeof($services);
    $result["result"] = 0;
    $this->logger->log_debug(print_r($allHostsDBInfo, true));
    $this->logger->log_debug(print_r($services,true));
    // logic to avoid installing Nagios/Ganglia Master on the HMC node...
    // commented out for now
    //$thisHostName = trim(strtolower(exec('hostname -f')));
    //$monitorIndex = ($thisHostName != $allHostsInfo[0]['hostName']) ? 0 : 1;
    $monitorIndex = 0;
    $allHostsInfoExHMC = $this->excludeHMCHost($allHostsInfo);
    $this->logger->log_debug('num nodes='.$numNodes);  
    if ( $numNodes == 1 ) {
      $result = $this->addNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addSNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addJobTracker($services, $result, $allHostsInfo[0]);
      $result = $this->addHBaseMaster($services, $result, $allHostsInfo[0]);
      $result = $this->addOozieServer($services, $result, $allHostsInfo[0]);
      $result = $this->addHiveServer($services, $result, $allHostsInfo[0]);
      $result = $this->addTempletonServer($services, $result, $allHostsInfo[0]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[0]);
      $result = $this->addGangliaServer($result, $allHostsInfo[0]);
      $result = $this->addNagiosServer($result, $allHostsInfo[0]);
      return $result;
    }
    if ( $numNodes < 3) {
      $result = $this->addNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addSNameNode($services, $result, $allHostsInfo[1]);
      $result = $this->addJobTracker($services, $result, $allHostsInfo[1]);
      $result = $this->addHBaseMaster($services, $result, $allHostsInfo[0]);
      $result = $this->addOozieServer($services, $result, $allHostsInfo[1]);
      $result = $this->addHiveServer($services, $result, $allHostsInfo[1]);
      $result = $this->addTempletonServer($services, $result, $allHostsInfo[1]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[0]);
      $result = $this->addGangliaServer($result, $allHostsInfoExHMC[$monitorIndex]);
      $result = $this->addNagiosServer($result, $allHostsInfoExHMC[$monitorIndex]); 
      return $result;
    }
    if ( $numNodes <= 5) {
      $result = $this->addNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addSNameNode($services, $result, $allHostsInfo[1]);
      $result = $this->addJobTracker($services, $result, $allHostsInfo[1]);
      $result = $this->addHBaseMaster($services, $result, $allHostsInfo[0]);
      $result = $this->addOozieServer($services, $result, $allHostsInfo[1]);
      $result = $this->addHiveServer($services, $result, $allHostsInfo[1]);
      $result = $this->addTempletonServer($services, $result, $allHostsInfo[1]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[0]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[1]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[2]);
      $result = $this->addGangliaServer($result, $allHostsInfoExHMC[$monitorIndex]);
      $result = $this->addNagiosServer($result, $allHostsInfoExHMC[$monitorIndex]); 
      return $result;
    }

    if ( $numNodes <= 30) {
      $result = $this->addNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addSNameNode($services, $result, $allHostsInfo[1]);
      $result = $this->addJobTracker($services, $result, $allHostsInfo[1]);
      $result = $this->addHBaseMaster($services, $result, $allHostsInfo[2]);
      $result = $this->addOozieServer($services, $result, $allHostsInfo[2]);
      $result = $this->addHiveServer($services, $result, $allHostsInfo[2]);
      $result = $this->addTempletonServer($services, $result, $allHostsInfo[2]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[0]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[1]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[2]);
      $result = $this->addGangliaServer($result, $allHostsInfoExHMC[$monitorIndex]);
      $result = $this->addNagiosServer($result, $allHostsInfoExHMC[$monitorIndex]);
      return $result;
    }
    if ( $numNodes > 30) {
      $result = $this->addNameNode($services, $result, $allHostsInfo[0]);
      $result = $this->addSNameNode($services, $result, $allHostsInfo[1]);
      $result = $this->addJobTracker($services, $result, $allHostsInfo[2]);
      $result = $this->addHBaseMaster($services, $result, $allHostsInfo[3]);
      $result = $this->addOozieServer($services, $result, $allHostsInfo[3]);
      $result = $this->addHiveServer($services, $result, $allHostsInfo[4]);
      $result = $this->addTempletonServer($services, $result, $allHostsInfo[4]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[0]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[1]);
      $result = $this->addZooKeeperServer($services, $result, $allHostsInfo[2]);
      $result = $this->addGangliaServer($result, $allHostsInfoExHMC[$monitorIndex]);
      $result = $this->addNagiosServer($result, $allHostsInfoExHMC[$monitorIndex]);
      return $result;
    }
  }
}
?>
