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

class SuggestProperties {
  private $logger;

  /**
   * Needs a default constructor else warnings will appear.
   */
  function __construct() {
    $this->logger = new HMCLogger("SuggestProperties");
  }


  /**
   * Allocate Heap Size for a component given what all processes are running
   * on the host.
   */
  function allocateHeapSizeForDaemon($componentName, $hostRoles, $hostInfoMap,
      $allHostsToComponents, $is32bit) {
    // TODO fix
    // code should handle 32-bit checks - cannot assign over 4 GB for a role
    // which uses 32-bit procs
    // if os is 32-bit we have even more restrictions
    // for now assuming 64-bit os
    $this->logger->log_info("Calculating Heap Size For ".$componentName);
    $host = $this->getHostForComponent($hostRoles, $componentName);
    $this->logger->log_info("Model HostName for ".$componentName." ".$host);
    $hostMem = $hostInfoMap[$host]["totalMem"]*0.9;
    $numProcs = sizeof($allHostsToComponents["hosts"][$host]["components"]);
    $normalizedMem = (int) (ceil ($hostMem/$numProcs));
    if ($is32bit) {
      $normalizedMem = min($normalizedMem, (pow(2,32)/(1024*1024))-1);
    }
    $normalizedMem = ((int)($normalizedMem/8))*8;
    $this->logger->log_info("Component=" . $componentName . " Host="
        . $host." Mem=".$hostMem." numComponents=".$numProcs.
        " perComponentMem=".$normalizedMem);

    if ($normalizedMem < 256) {
      $this->logger->log_info("Normalizing memory to 256 as min required");
      $normalizedMem = 256;
    }

    return $normalizedMem;
  }

  function allocateHeapSizeWithMax($componentName, $hostRoles, $hostInfoMap,
      $allHostsToComponents, $is32bit, $max) {
    $heapSizeT = $this->allocateHeapSizeForDaemon($componentName, $hostRoles, $hostInfoMap,
        $allHostsToComponents, $is32bit);
    if ($heapSizeT > $max) {
      $heapSizeT = $max;
    }

    $this->logger->log_info("Calculating Maxed Heap Size For ".$componentName ." $heapSizeT with max $max" );
    return $heapSizeT; 
  }

  function getMaxHeapSizeForDaemon($componentName, $hostRoles, $hostInfoMap,
      $allHostsToComponents, $is32bit) {
    $this->logger->log_info("Calculating Max Heap Size For ".$componentName);
    $host = $this->getHostForComponent($hostRoles, $componentName);
    $this->logger->log_info("Model HostName for ".$componentName." ".$host);
    $hostMem = $hostInfoMap[$host]["totalMem"];
    $normalizedMem = $hostMem;
    if ($is32bit) {
      $normalizedMem = min($normalizedMem, (pow(2,32)/(1024*1024))-1);
    }
    $this->logger->log_info("Component=" . $componentName . " Host="
        . $host." HostMem=".$hostMem
        ." MaxNormalizedMem=".$normalizedMem);
    return $normalizedMem;
  }


  /**
   * get the host info in a list, convert it to a map so that easy
   * to lookup
   */
  function createHostToInfoMap($hostInfo) {
    $hosts = $hostInfo["hosts"];
    $result = array();
    foreach($hosts as $host) {
      $result[$host["hostName"]] = $host;
    }
    return $result;
  }

  /**
   * Return a single host that maps to a master service.
   */
  function getHostForComponent($hostRoles, $role) {
    $listHosts = $hostRoles["components"][$role]["hosts"];
    foreach ($listHosts as $hostName=>$hostInfo) {
      $retHost = $hostName;
      break;
    }
    return $retHost;
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
   * Function to suggest Properties to the user.
   * It will read the db to get the sevices that are configured
   * return back the configs with suggestions based on the services that are
   * configured.
   * NOTE: It will only return recommended configs - does not return other
   * props or defaults from DB
   * @param clustername the name of the cluster we are deploying/managing
   * @param db database from where to read, usually pass in new HMCDBAccessor("mydb.data");
   * @param updateDB bool whether to update db with suggested settings
   * @return mixed
   *  array (
   *       "result" => 0,
   *       "error" => "",
   *       "configs" => array(
   *        "key" => "val",
   *        ...
   *        )
   *       );
   */

  public function suggestProperties($clusterName, $db,
      $updateDB) {
    $result = array();
    $result["result"] = 0;
    $result["error"] = "";

    $servicesDBInfo = $db->getAllServicesInfo($clusterName);
    if ($servicesDBInfo["result"] != 0) {
      $result["result"] = $servicesDBInfo["result"];
      $result["error"] = $servicesDBInfo["error"];
      return $result;
    }
    $services_tmp = $servicesDBInfo["services"];
    $services = $this->filterEnabledServices($services_tmp);
    $this->logger->log_debug("Services Enabled \n".print_r($services, true));
    $hostRoles = $db->getAllHostsByComponent($clusterName);
    if ($hostRoles["result"] != 0) {
      $result["result"] = $hostRoles["result"];
      $result["error"] = $hostRoles["error"];
      return $result;
    }
    $order = array("sortColumn" => "cpuCount",
        "sortOrder" => "ASC");

    $allHosts = $db->getAllHostsInfo($clusterName,
        array("=" => array ( "discoveryStatus" => "SUCCESS")), $order);
    if ($allHosts["result"] != 0) {
      $result["result"] = $allHosts["result"];
      $result["error"] = $allHosts["error"];
      return $result;
    }
    // convert host list to a map so thats easy to lookup
    $hostInfoMap = $this->createHostToInfoMap($allHosts);
    $allHostsToComponents = $db->getAllHostsToComponentMap($clusterName);
    if ($allHostsToComponents["result"] != 0) {
      $result["result"] = $allHostsToComponents["result"];
      $result["error"] = $allHostsToComponents["error"];
      return $result;
    }


    // filter host roles for client-only components
    $ignoredComponents = array();

    $allComponents = $db->getAllServiceComponentsList();
    if ($allComponents["result"] == 0) {
      if (isset($allComponents["services"])
          && is_array($allComponents["services"])) {
        foreach ($allComponents["services"] as $svcName => $svcInfo) {
          if (isset($svcInfo["components"])
              && is_array($svcInfo["components"])) {
            foreach ($svcInfo["components"] as $compName => $compInfo) {
              if (isset($compInfo["isClient"]) && $compInfo["isClient"]) {
                $ignoredComponents[$compName] = TRUE;
              } else if ($compName == "GANGLIA_MONITOR") {
                $ignoredComponents[$compName] = TRUE;
              }
            }
          }
        }
      }
    }

    foreach ($allHostsToComponents["hosts"] as $hostName => $compList) {
      $newComps = array();
      foreach ($compList["components"] as $compName) {
        if (!isset($ignoredComponents[$compName])) {
          array_push($newComps, $compName);
        }
      }
      $allHostsToComponents["hosts"][$hostName]["components"] = $newComps;
    }

    $result["configs"] = array();

    // set the num map/reduce tasks
    // assuming that there is atleast one host
    if (count($allHosts["hosts"]) == 1) {
      // for single node install use 2 maps and 2 reduce slots
      $this->logger->log_info("Single node install: Using Num Maps 2, Num Reduces 2");
      $result["configs"]["mapred_map_tasks_max"] = 2;
      $result["configs"]["mapred_red_tasks_max"] = 2;
    } else {
      $minCpuHost = $allHosts["hosts"][0];
      $this->logger->log_info("Host Info with Min Cpu \n".print_r($minCpuHost, true));
      $minCpus = $minCpuHost["cpuCount"];
      $numMap = (int) (ceil ($minCpus/3 * 2 * 2)); // 2/3'rd of cpucount and multiply it by 2.
      if ($numMap <= 0) {
        $numMap = 1;
      }
      $numRed = ($minCpus * 2) - $numMap;
      if ($numRed <= 0) {
        $numRed = 1;
      }
      $this->logger->log_info("Num Maps ".$numMap ." Num Reduces ".$numRed);
      $result["configs"]["mapred_map_tasks_max"] = $numMap;
      $result["configs"]["mapred_red_tasks_max"] = $numRed;
    }

    /* suggest memory for all the needed master daemons */
    /* assume MR and HDFS are always selected */
    $nnHeap = $this->allocateHeapSizeForDaemon("NAMENODE", $hostRoles,
        $hostInfoMap, $allHostsToComponents, FALSE);
    $result["configs"]["namenode_heapsize"] = $nnHeap;

    /* suggest the jt heap size */
    $jtHeap = $this->allocateHeapSizeForDaemon("JOBTRACKER", $hostRoles,
        $hostInfoMap, $allHostsToComponents, FALSE);
    $result["configs"]["jtnode_heapsize"] = $jtHeap;

    /* check if HBase is installed and then pick */
    if (array_key_exists("HBASE", $services)) {
      $hbaseHeap = $this->allocateHeapSizeForDaemon("HBASE_MASTER", $hostRoles,
          $hostInfoMap, $allHostsToComponents, FALSE);
      $result["configs"]["hbase_master_heapsize"] = $hbaseHeap;
    }
    $heapSize = $this->allocateHeapSizeWithMax("DATANODE", $hostRoles,
        $hostInfoMap, $allHostsToComponents, TRUE, 2048);
    // cap the datanode heap size and hadoop heap size
    $result["configs"]["dtnode_heapsize"] = $heapSize;
    $result["configs"]["hadoop_heapsize"] = $heapSize;

    // TODO fix - this should be based on heap size divided by max task
    // limit on the host
    $heapSize = $this->allocateHeapSizeForDaemon("TASKTRACKER", $hostRoles,
        $hostInfoMap, $allHostsToComponents, TRUE);
    $heapSizeWithMax = $this->allocateHeapSizeWithMax("TASKTRACKER", $hostRoles,
        $hostInfoMap, $allHostsToComponents, TRUE, 2048);
    $this->logger->log_info("Maxed Heap Size for MR Child opts ".$heapSizeWithMax);
    $result["configs"]["mapred_child_java_opts_sz"] = $heapSizeWithMax;

    if (array_key_exists("HBASE", $services)) {
      $heapSize = $this->allocateHeapSizeForDaemon("HBASE_REGIONSERVER", $hostRoles,
          $hostInfoMap, $allHostsToComponents, FALSE);
      $result["configs"]["hbase_regionserver_heapsize"] = $heapSize;
    }

    /** TODO change this to be from the UI later **/
    $hostname = strtolower(exec('hostname -f'));
    $result["configs"]["jdk_location"] = "http://".$hostname."/downloads";

    if ($updateDB) {
      $this->logger->log_info("Updating suggested configs into DB");
      $ret = $db->updateServiceConfigs($clusterName, $result["configs"]);
      if ($ret["result"] != 0) {
        $this->logger->log_error("Error updating suggested configs into DB"
            . ", result=" . $ret["result"]
            . ", error=" . $ret["error"]);
        $result["result"] = $ret["result"];
        $result["error"] = $ret["error"];
      }
    }

    $this->logger->log_info("Calculated Config Parameters \n".print_r($result, true));
    return $result;
  }

  /**
   * Verify properties set in DB
   * @param string $clusterName
   * @param object $db - HMCDBAccessor
   * @param mixed $configs
   *    array ( "prop_key1" => "prop_val1", ... )
   * @return mixed
   *     array (
   *        "result" => 0,
   *        "error" => "",
   *        "cfgErrors" => array (
   *           "propKey" => array (
   *               "value" => "current val in DB",
   *               "recommendedValue" => $recoVal,
   *               "error" => "reason why this is an error"
   *             ),
   *             ...
   *           ),
   *        "cfgWarnings" => array (
   *           "propKey" => array (
   *               "value" => "current val in DB",
   *               "recommendedValue" => $recoVal,
   *               "error" => "reason why this is an warning"
   *             ),
   *             ...
   *           )
   *       )
   */
  public function verifyProperties($clusterName, $db, $configs) {
    $result = array();
    $result["result"] = 0;
    $result["error"] = "";

    $servicesDBInfo = $db->getAllServicesInfo($clusterName);
    if ($servicesDBInfo["result"] != 0) {
      $result["result"] = $servicesDBInfo["result"];
      $result["error"] = $servicesDBInfo["error"];
      return $result;
    }

    $services_tmp = $servicesDBInfo["services"];
    $services = $this->filterEnabledServices($services_tmp);
    $this->logger->log_debug("Services Enabled \n".print_r($services, true));
    $hostRoles = $db->getAllHostsByComponent($clusterName);
    if ($hostRoles["result"] != 0) {
      $result["result"] = $hostRoles["result"];
      $result["error"] = $hostRoles["error"];
      return $result;
    }
    $order = array("sortColumn" => "cpuCount",
            "sortOrder" => "ASC");

    $allHosts = $db->getAllHostsInfo($clusterName,
        array("=" => array ( "discoveryStatus" => "SUCCESS")), $order);
    if ($allHosts["result"] != 0) {
      $result["result"] = $allHosts["result"];
      $result["error"] = $allHosts["error"];
      return $result;
    }

    // convert host list to a map so thats easy to lookup
    $hostInfoMap = $this->createHostToInfoMap($allHosts);
    $allHostsToComponents = $db->getAllHostsToComponentMap($clusterName);
    if ($allHostsToComponents["result"] != 0) {
      $result["result"] = $allHostsToComponents["result"];
      $result["error"] = $allHostsToComponents["error"];
      return $result;
    }

    $recommendedInfo = $this->suggestProperties($clusterName, $db, FALSE);
    if ($recommendedInfo["result"] != 0) {
      $result["result"] = $recommendedInfo["result"];
      $result["error"] = $recommendedInfo["error"];
      return $result;
    }

    $recommendedConfigs = $recommendedInfo["configs"];

    // errors => array ( key => array ( value, recommended_value, reason ))
    $cfgErrors = array();
    $cfgWarnings = array();

    // verify map and reduce tasks max settings
    if (isset($configs["mapred_map_tasks_max"])
        && isset($configs["mapred_red_tasks_max"])) {
      if ($configs["mapred_map_tasks_max"] == 0
          || $configs["mapred_red_tasks_max"] == 0) {
        $reason = "Value cannot be 0";
        if ($configs["mapred_map_tasks_max"] == 0) {
          $cfgErrors["mapred_map_tasks_max"] = array ( "value" => 0,
              "recommendedValue" => $recommendedConfigs["mapred_map_tasks_max"],
              "error" => $reason);
        }
        if ($configs["mapred_red_tasks_max"] == 0) {
          $cfgErrors["mapred_red_tasks_max"] = array ( "value" => 0,
              "recommendedValue" => $recommendedConfigs["mapred_red_tasks_max"],
              "error" => $reason);
        }
      }
      if ($configs["mapred_map_tasks_max"] >
          $recommendedConfigs["mapred_map_tasks_max"]) {
        $cfgWarnings["mapred_map_tasks_max"] = array (
            "value" => $configs["mapred_map_tasks_max"],
            "recommendedValue" => $recommendedConfigs["mapred_map_tasks_max"],
            "error" => "Value greater than recommended");
      }

      if ($configs["mapred_red_tasks_max"] >
          $recommendedConfigs["mapred_red_tasks_max"]) {
        $cfgWarnings["mapred_red_tasks_max"] = array (
            "value" => $configs["mapred_red_tasks_max"],
            "recommendedValue" => $recommendedConfigs["mapred_red_tasks_max"],
            "error" => "Value greater than recommended");
      }
    }

    $memProps = array (
        "namenode_heapsize" => array ( "role" => "NAMENODE", "32bit" => FALSE),
        "jtnode_heapsize" => array ( "role" => "JOBTRACKER", "32bit" => FALSE),
        "dtnode_heapsize" => array ( "role" => "DATANODE", "32bit" => TRUE),
        "hadoop_heapsize" => array ( "role" => "DATANODE", "32bit" => TRUE),
        "mapred_child_java_opts_sz" => array ( "role" => "TASKTRACKER", "32bit" => TRUE)
      );

    if (array_key_exists("HBASE", $services)) {
      $memProps["hbase_master_heapsize"] =
          array ( "role" => "HBASE_MASTER", "32bit" => FALSE);
      $memProps["hbase_regionserver_heapsize"] =
          array ( "role" => "HBASE_REGIONSERVER", "32bit" => FALSE);
    }

    foreach ($memProps as $prop => $propInfo) {
      if (!isset($configs[$prop])) {
        continue;
      }

      if ($configs[$prop] < 256) {
        $reason = "Value less than min 256M";
        $cfgErrors[$prop] = array (
                      "value" => $configs[$prop],
                      "recommendedValue" => $recommendedConfigs[$prop],
                      "error" => $reason
        );
        continue;
      }
      if ($configs[$prop] >
        $recommendedConfigs[$prop]) {
        $maxHeap = $this->getMaxHeapSizeForDaemon($propInfo["role"], $hostRoles,
            $hostInfoMap, $allHostsToComponents, $propInfo["32bit"]);
        if ($configs[$prop] > $maxHeap) {
          $reason = "Value greater than mem limit allowed";
          $cfgErrors[$prop] = array (
              "value" => $configs[$prop],
              "recommendedValue" => $recommendedConfigs[$prop],
              "error" => $reason
            );
        } else {
          $reason = "Value greater than recommended mem limit";
          $cfgWarnings[$prop] = array (
              "value" => $configs[$prop],
              "recommendedValue" => $recommendedConfigs[$prop],
              "error" => $reason
            );
        }
      }
    }

    $result = count($cfgErrors);
    $error = "";
    if ($result != 0 ) {
      $error = "Invalid Configs";
    }

    return array ("result" => $result, "error" => $error,
                  "cfgErrors" => $cfgErrors,
                  "cfgWarnings" => $cfgWarnings);
  }

}
?>
