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


include_once '../../../php/util/Logger.php';
include_once '../../../php/conf/Config.inc';
include_once "../../../php/util/lock.php";
include_once '../../../php/db/HMCDBAccessor.php';
include_once "../../../php/db/OrchestratorDB.php";
include_once "../../../php/orchestrator/Service.php";
include_once "../../../php/orchestrator/ServiceComponent.php";

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::TRACE;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";

system("rm -rf ./test.db");
system("rm -rf ./hmc.log");

assert_options(ASSERT_BAIL, 1);

system("sqlite3 ./test.db < ../../../db/schema.dump");

$dbPath = "./test.db";
$db = new HMCDBAccessor($dbPath);

print "Test createCluster\n";
$clusterName = "TestCluster";
$version = "1.0.0";
$state = "CFG_IN_PROGRESS";
$result = $db->createCluster($clusterName, $version, $state);
assert(is_array($result) && isset($result["clusterName"]));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test Duplicate Cluster Creation Fail\n";
$result = $db->createCluster($clusterName, $version, $state);
assert(is_array($result));
assert($result["result"] != 0);
assert($result["error"] != "");

// create 2nd cluster
$clusterName2 = "TestCluster2";
$version = "1.0.0";
$state = "INSTALL_IN_PROGRESS";
$result = $db->createCluster($clusterName2, $version, $state);
assert(is_array($result) && isset($result["clusterName"]));
assert($result["clusterName"] == $clusterName2);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test setClusterState\n";
$result = $db->setClusterState($clusterName, "INSTALLED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getAllClusters\n";
$result = $db->getAllClusters();
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["clusters"]) && count($result["clusters"]) == 2);
assert(isset($result["clusters"][$clusterName])
       && $result["clusters"][$clusterName]["clusterName"] == $clusterName
       && $result["clusters"][$clusterName]["version"] == $version
       && $result["clusters"][$clusterName]["state"] == "INSTALLED");
assert(isset($result["clusters"][$clusterName2])
       && $result["clusters"][$clusterName2]["clusterName"] == $clusterName2
       && $result["clusters"][$clusterName2]["version"] == $version
       && $result["clusters"][$clusterName2]["state"] == "INSTALL_IN_PROGRESS");


print "Test addHostsToCluster\n";
$hosts = array (
   array (
        "hostName" => "localhost1",
        "ip" => "127.0.0.1",
        "totalMem" => 64,
        "cpuCount" => 4,
        "osArch" => "i386",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error",
        "attributes" => array ( "foo" => "bar" )
     ),
   array (
        "hostName" => "localhost2",
        "ip" => "127.0.0.2",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "error",
        "badHealthReason" => "invalid ssh key"
     )
);
$result = $db->addHostsToCluster($clusterName, $hosts);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 2);
assert(in_array("localhost1", $result["hosts"]));
assert(in_array("localhost2", $result["hosts"]));

$hosts2 = array (
   array (
        "hostName" => "localhost3",
        "ip" => "127.0.0.3",
        "totalMem" => 640,
        "cpuCount" => 4,
        "osArch" => "i386",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error"
     )
);
$result = $db->addHostsToCluster($clusterName2, $hosts2);
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["clusterName"] == $clusterName2);
assert(count($result["hosts"]) == 1);
assert(in_array("localhost3", $result["hosts"]));

print "Test updateHostDiscoveryStatus\n";
$updateHostInfo = array (
  "localhost3" => array ( "discoveryStatus" => "error",
                          "badHealthReason" => "foo bar"));

$result = $db->updateHostDiscoveryStatus($clusterName2, $updateHostInfo);
assert($result["result"] == 0);
assert($result["error"] == "");
$hostName = "localhost3";
$result = $db->getHostInfo($clusterName2, $hostName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["discoveryStatus"] == "error");
assert($result["badHealthReason"] == "foo bar");

print "Test getHostInfo\n";
$result = $db->getHostInfo($clusterName, "localhost_does_not_exist");
assert($result["result"] != 0);
assert($result["error"] != "");

$hostName = "localhost1";
$result = $db->getHostInfo($clusterName, $hostName);
assert($result["result"] == 0);
assert($result["error"] == "");
unset($result["result"]);
unset($result["error"]);
unset($result["clusterName"]);
foreach ($result as $key=>$val) {
  assert(array_key_exists($key, $result));
  assert($val == $result[$key]);
}
assert(is_array($result["attributes"])
       && $result["attributes"]["foo"] == "bar");

print "Test getAllHostsInfo\n";
$result = $db->getAllHostsInfo($clusterName, "", "");
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result) && is_array($result["hosts"])
       && count($result["hosts"]) == 2);
foreach ($result["hosts"] as $tmpHost) {
  assert(is_array($tmpHost["attributes"]));
}

print "Test getAllHostsInfo with filter\n";
$result = $db->getAllHostsInfo($clusterName, array ( "=" => array ( "discoveryStatus" => "error")), "");
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result) && is_array($result["hosts"])
       && count($result["hosts"]) == 1);
assert($result["hosts"][0]["ip"] == "127.0.0.2");

$result = $db->getAllHostsInfo($clusterName, array ( "!=" => array ( "discoveryStatus" => "errorfoo")), "");
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result) && is_array($result["hosts"])
       && count($result["hosts"]) == 2);

$result = $db->getAllHostsInfo($clusterName, "", array("sortColumn" => "totalMem", "sortOrder" => "DESC"));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result) && is_array($result["hosts"])
       && count($result["hosts"]) == 2);
assert($result["hosts"][0]["hostName"] == "localhost1"
       && $result["hosts"][1]["hostName"] == "localhost2");
foreach ($hosts[0] as $key=>$val) {
  assert(array_key_exists($key, $result["hosts"][0]));
  assert($val == $result["hosts"][0][$key]);
}

$result = $db->getAllHostsInfo($clusterName, "", array("sortColumn" => "totalMem", "sortOrder" => "ASC"));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result) && is_array($result["hosts"])
       && count($result["hosts"]) == 2);
assert($result["hosts"][0]["hostName"] == "localhost2"
       && $result["hosts"][1]["hostName"] == "localhost1");


print "Test updateServiceConfigs\n";
$serviceName = "HDFS";
$config = array ( "key1" => "val1", "key2" => "val2");
$result = $db->updateServiceConfigs($clusterName, $config);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(isset($result["sql"]["rowsChanged"])
    && $result["sql"]["rowsChanged"] == 2);
$result = $db->updateServiceConfigs($clusterName, $config);
assert($result["result"] == 0);
assert($result["error"] == "");

// TODO
/*
print "Test getServiceConfig\n";
$result = $db->getServiceConfig($clusterName, $serviceName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["clusterName"] == $clusterName);
assert($result["serviceName"] == $serviceName);
assert(is_array($result["properties"])
       && count($result["properties"]) == 2);
assert($result["properties"]["key1"] == "val1"
       && $result["properties"]["key2"] == "val2");

print "Test updateServiceConfigs\n";
$config = array ( "key1" => "val1", "key2" => "val21", "key3" => "val3");
$result = $db->updateServiceConfigs($clusterName, $serviceName, $config);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(isset($result["sql"]["rowsChanged"])
    && $result["sql"]["rowsChanged"] == 3);

$result = $db->getServiceConfig($clusterName, $serviceName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["clusterName"] == $clusterName);
assert($result["serviceName"] == $serviceName);
assert(is_array($result["properties"])
    && count($result["properties"]) == 3);
assert($result["properties"]["key1"] == "val1"
    && $result["properties"]["key2"] == "val21"
    && $result["properties"]["key3"] == "val3");

print "Test updateServiceConfigs Service2\n";
$serviceName2 = "MAPREDUCE";
$config = array ( "key4" => "val4", "key5" => "val5", "key6" => "val6");
$result = $db->updateServiceConfigs($clusterName, $serviceName2, $config);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(isset($result["sql"]["rowsChanged"])
    && $result["sql"]["rowsChanged"] == 3);

print "Test getAllConfigs\n";
$result = $db->getAllConfigs($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["clusterName"] == $clusterName);
assert(is_array($result["services"]) && count($result["services"]) == 2);

assert(is_array($result["services"][$serviceName])
       && is_array($result["services"][$serviceName]["properties"])
       && count($result["services"][$serviceName]["properties"]) == 3);

assert($result["services"][$serviceName]["properties"]["key1"] == "val1"
       && $result["services"][$serviceName]["properties"]["key2"] == "val21"
       && $result["services"][$serviceName]["properties"]["key3"] == "val3");

assert(is_array($result["services"][$serviceName2])
       && is_array($result["services"][$serviceName2]["properties"])
       && count($result["services"][$serviceName2]["properties"]) == 3);

assert($result["services"][$serviceName2]["properties"]["key4"] == "val4"
       && $result["services"][$serviceName2]["properties"]["key5"] == "val5"
       && $result["services"][$serviceName2]["properties"]["key6"] == "val6");
*/

print "Test getAllServicesList\n";
$result = $db->getAllServicesList();
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["services"]) && count($result["services"]) > 2);
assert(is_array($result["services"]["HDFS"]));
assert($result["services"]["HDFS"]["serviceName"] == "HDFS");
assert($result["services"]["HDFS"]["displayName"] == "HDFS");
assert($result["services"]["HDFS"]["description"] != "");

print "Test addServicesToCluster\n";
$services = array(
   array (
     "serviceName" => "HDFS",
     "state" => "INSTALLING",
     "desiredState" => "INSTALLED",
     "isEnabled" => TRUE
   ),
   array (
     "serviceName" => "MAPREDUCE",
     "isEnabled" => TRUE
   ),
   array (
     "serviceName" => "HBASE",
     "isEnabled" => FALSE
   ),
   array (
     "serviceName" => "PIG"
   )

);

print "Test addServicesToCluster\n";
$result = $db->addServicesToCluster($clusterName, $services);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test invalid addServicesToCluster\n";
array_push($services, array("serviceName" => "foo"));
$result = $db->addServicesToCluster($clusterName, $services);
assert(is_array($result));
assert($result["result"] != 0);
assert($result["error"] != "");

print "Test getAllServicesInfo\n";
$result = $db->getAllServicesInfo($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["services"]));
assert(is_array($result["services"]["HDFS"])
    && $result["services"]["HDFS"]["serviceName"] == "HDFS"
    && $result["services"]["HDFS"]["state"] == "INSTALLING"
    && $result["services"]["HDFS"]["desiredState"] == "INSTALLED"
    && $result["services"]["HDFS"]["isEnabled"] == 1);
assert(is_array($result["services"]["MAPREDUCE"])
    && $result["services"]["MAPREDUCE"]["serviceName"] == "MAPREDUCE"
    && $result["services"]["MAPREDUCE"]["state"] == ""
    && $result["services"]["MAPREDUCE"]["desiredState"] == ""
    && $result["services"]["MAPREDUCE"]["isEnabled"] == 1);
assert(is_array($result["services"]["HBASE"])
    && $result["services"]["HBASE"]["serviceName"] == "HBASE"
    && $result["services"]["HBASE"]["state"] == ""
    && $result["services"]["HBASE"]["desiredState"] == ""
    && $result["services"]["HBASE"]["isEnabled"] == 0);
assert(is_array($result["services"]["PIG"])
    && $result["services"]["PIG"]["serviceName"] == "PIG"
    && $result["services"]["PIG"]["state"] == ""
    && $result["services"]["PIG"]["desiredState"] == ""
    && $result["services"]["PIG"]["isEnabled"] == 0);

print "Test getAllServiceComponents\n";
$result = $db->getAllServiceComponents("HDFS");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["components"]));
assert($result["serviceName"] == "HDFS");
assert(is_array($result["components"]["NAMENODE"])
  && $result["components"]["NAMENODE"]["componentName"] == "NAMENODE"
  && $result["components"]["NAMENODE"]["isMaster"] == true
  && $result["components"]["NAMENODE"]["isClient"] == false
  && $result["components"]["NAMENODE"]["displayName"] == "NameNode"
  && $result["components"]["NAMENODE"]["description"] != "");
assert(is_array($result["components"]["SNAMENODE"])
  && $result["components"]["SNAMENODE"]["componentName"] == "SNAMENODE"
  && $result["components"]["SNAMENODE"]["isMaster"] == true
  && $result["components"]["SNAMENODE"]["isClient"] == false
  && $result["components"]["SNAMENODE"]["displayName"] == "Secondary NameNode"
  && $result["components"]["SNAMENODE"]["description"] != "");
assert(is_array($result["components"]["DATANODE"])
  && $result["components"]["DATANODE"]["componentName"] == "DATANODE"
  && $result["components"]["DATANODE"]["isMaster"] == false
  && $result["components"]["DATANODE"]["isClient"] == false
  && $result["components"]["DATANODE"]["displayName"] == "Datanode"
  && $result["components"]["DATANODE"]["description"] != "");
assert(is_array($result["components"]["HDFS_CLIENT"])
  && $result["components"]["HDFS_CLIENT"]["componentName"] == "HDFS_CLIENT"
  && $result["components"]["HDFS_CLIENT"]["isMaster"] == false
  && $result["components"]["HDFS_CLIENT"]["isClient"] == true
  && $result["components"]["HDFS_CLIENT"]["displayName"] == "HDFS Client"
  && $result["components"]["HDFS_CLIENT"]["description"] != "");


print "Test getServiceInfo\n";
/*
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
$serviceName = "HDFS";
$result = $db->getServiceInfo($clusterName, $serviceName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["serviceName"] == "HDFS"
    && $result["state"] == "INSTALLING"
    && $result["desiredState"] == "INSTALLED"
    && $result["isEnabled"] == 1);

print "Test setServiceState\n";
$result = $db->setServiceState($clusterName, $serviceName, "STARTING");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["serviceName"] == "HDFS"
  && $result["state"] == "STARTING"
  && $result["oldState"] == "INSTALLING");

print "Test setServiceDesiredState\n";
$result = $db->setServiceDesiredState($clusterName, $serviceName, "STARTED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["serviceName"] == "HDFS"
    && $result["desiredState"] == "STARTED"
    && $result["oldDesiredState"] == "INSTALLED");

print "Test addHostsToComponent\n";
$serviceName = "HDFS";
$componentName = "DATANODE";
$hosts = array ( "localhost1", "localhost2");
$result = $db->addHostsToComponent($clusterName,
    $componentName, $hosts, "INSTALLING", "INSTALLED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$cltComp = "HDFS_CLIENT";
$hosts = array ( "localhost1", "localhost2");
$result = $db->addHostsToComponent($clusterName,
   $cltComp, $hosts, "INSTALLING", "INSTALLED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->addHostsToComponent($clusterName2,
    $componentName, array ( "localhost3") , "INSTALLING", "INSTALLED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getAllHostsByComponent\n";
$result = $db->getAllHostsByComponent($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["components"]));
assert(is_array($result["components"][$componentName]));
assert($result["components"][$componentName]["componentName"] == $componentName);
assert(is_array($result["components"][$componentName]["hosts"])
       && count($result["components"][$componentName]["hosts"]) == 2);
assert(is_array($result["components"][$componentName]["hosts"]["localhost1"])
       && is_array($result["components"][$componentName]["hosts"]["localhost2"]));

print "Test getHostsForComponent\n";
$result = $db->getHostsForComponent($clusterName, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 2);
assert(is_array($result["hosts"]["localhost1"])
       && is_array($result["hosts"]["localhost2"]));

print "Test getAllServiceComponentsList\n";
$result = $db->getAllServiceComponentsList();
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["services"])
       && count($result["services"]) >= 2
       && is_array($result["services"]["HDFS"])
       && is_array($result["services"]["MAPREDUCE"]));

foreach ($result["services"] as $serviceNm => $service) {
  assert(isset($service["serviceName"])
         && $service["serviceName"] == $serviceNm
         && is_array($service["components"])
         && count($service["components"]) >= 1);
  foreach ($service["components"] as $compName => $comp) {
    assert(isset($comp["serviceName"])
        && $comp["serviceName"] == $serviceNm
        && $comp["componentName"] == $compName
        && isset($comp["isMaster"])
        && isset($comp["isClient"])
        && isset($comp["displayName"]));
  }
  if ($serviceNm == "ZOOKEEPER") {
    assert(count($service["components"]) == 2);
    assert(isset($service["components"]["ZOOKEEPER_SERVER"])
           && is_array($service["components"]["ZOOKEEPER_SERVER"])
           && $service["components"]["ZOOKEEPER_SERVER"]["isMaster"] == true
           && $service["components"]["ZOOKEEPER_SERVER"]["isClient"] == false
           && $service["components"]["ZOOKEEPER_SERVER"]["displayName"] == "ZooKeeper Server");
    assert(isset($service["components"]["ZOOKEEPER_CLIENT"])
           && is_array($service["components"]["ZOOKEEPER_CLIENT"])
           && $service["components"]["ZOOKEEPER_CLIENT"]["isMaster"] == false
           && $service["components"]["ZOOKEEPER_CLIENT"]["isClient"] == true
           && $service["components"]["ZOOKEEPER_CLIENT"]["displayName"] == "ZooKeeper Client");
  }
}

print "Test setHostsStateForComponent\n";
$componentName = "DATANODE";
$hostArray = array ( "localhost1", "localhost2", "localhost3" );
$result = $db->setHostsStateForComponent($clusterName, $componentName,
   $hostArray, "UNINSTALLING");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->getHostsForComponent($clusterName, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 2);
assert(is_array($result["hosts"]["localhost1"])
  && is_array($result["hosts"]["localhost2"]));
assert($result["hosts"]["localhost1"]["state"] == "UNINSTALLING");
assert($result["hosts"]["localhost2"]["state"] == "UNINSTALLING");

$result = $db->getHostsForComponent($clusterName2, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 1);
assert(is_array($result["hosts"]["localhost3"])
       && $result["hosts"]["localhost3"]["state"] != "UNINSTALLING");

print "Test setHostsDesiredStateForComponent\n";
$componentName = "DATANODE";
$hostArray = array ( "localhost1" );
$result = $db->setHostsStateForComponent($clusterName, $componentName,
    $hostArray, "UNINSTALLED");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->getHostsForComponent($clusterName, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 2);
assert(is_array($result["hosts"]["localhost1"])
    && is_array($result["hosts"]["localhost2"]));
assert($result["hosts"]["localhost1"]["state"] == "UNINSTALLED");
assert($result["hosts"]["localhost2"]["state"] != "UNINSTALLED");

print "Test getServiceDependencies\n";
$result = $db->getServiceDependencies("HBASE");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["serviceName"] == "HBASE");
assert(is_array($result["serviceDependencies"])
       && array_search("HDFS", $result["serviceDependencies"]) !== FALSE
       && array_search("ZOOKEEPER", $result["serviceDependencies"]) !== FALSE);

print "Test getServiceDependents\n";
$result = $db->getServiceDependents("HDFS");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["serviceName"] == "HDFS");
assert(is_array($result["serviceDependents"])
       && array_search("HBASE", $result["serviceDependents"]) !== FALSE
       && array_search("MAPREDUCE", $result["serviceDependents"]) !== FALSE);

print "Test getServiceComponentDependencies\n";
$result = $db->getServiceComponentDependencies("DATANODE");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["componentName"] == "DATANODE");
assert(is_array($result["componentDependencies"])
    && array_search("NAMENODE", $result["componentDependencies"]) !== FALSE);

print "Test getServiceComponentDependents\n";
$result = $db->getServiceComponentDependents("NAMENODE");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["componentName"] == "NAMENODE");
assert(is_array($result["componentDependents"])
    && array_search("DATANODE", $result["componentDependents"]) !== FALSE
    && array_search("SNAMENODE", $result["componentDependents"]) !== FALSE);

print "Test addServiceComponentsToCluster\n";
$comps = array (
   "HDFS" => array (
       "DATANODE" => array ( "state" => "STARTING",
                             "desiredState" => "STARTED"
       ),
       "HDFS_CLIENT" => array ( "state" => "INSTALLING",
                             "desiredState" => "INSTALLED"),
   ),
   "MAPREDUCE" => array (
       "JOBTRACKER" => array ( "state" => "INSTALLED",
                         "desiredState" => "INSTALLED"
       ),
       "TASKTRACKER" => array ( "state" => "INSTALLED",
                         "desiredState" => "INSTALLED"
       )
    )
);
$result = $db->addServiceComponentsToCluster($clusterName, $comps);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getAllServiceComponentsInfo\n";
$result = $db->getAllServiceComponentsInfo($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["services"]));
foreach ($comps as $svc => $svccomps) {
  assert (isset($result["services"][$svc])
          && is_array($result["services"][$svc])
          && is_array($result["services"][$svc]["components"]));
  foreach ($svccomps as $compName => $comp) {
    assert (is_array($result["services"][$svc]["components"][$compName])
            && $result["services"][$svc]["components"][$compName]["state"] ==
                $comp["state"]
            && $result["services"][$svc]["components"]
                [$compName]["desiredState"] == $comp["desiredState"]);
  }
}

print "Test setServiceComponentState\n";
$componentName = "DATANODE";
$result = $db->setServiceComponentState($clusterName, $componentName,
      "INVALID_STATE", TRUE);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$result = $db->getAllServiceComponentsInfo($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$svc = "HDFS";
$compName = "DATANODE";
assert($result["services"][$svc]["components"][$compName]["state"] == "INVALID_STATE");

// verify host state updated too
$result = $db->getHostsForComponent($clusterName, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 2);
assert(is_array($result["hosts"]["localhost1"])
   && is_array($result["hosts"]["localhost2"]));
assert($result["hosts"]["localhost1"]["state"] == "INVALID_STATE");
assert($result["hosts"]["localhost2"]["state"] == "INVALID_STATE");

print "Test setServiceComponentDesiredState\n";
$result = $db->setServiceComponentDesiredState($clusterName, $componentName,
      "INVALID STATE", FALSE);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$result = $db->getAllServiceComponentsInfo($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$svc = "HDFS";
$compName = "DATANODE";
assert($result["services"][$svc]["components"][$compName]["state"] == "INVALID_STATE");
assert($result["services"][$svc]["components"][$compName]["desiredState"] == "INVALID STATE");

// verify host desired state not update
$result = $db->getHostsForComponent($clusterName, $componentName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]) && count($result["hosts"]) == 2);
assert(is_array($result["hosts"]["localhost1"])
    && is_array($result["hosts"]["localhost2"]));
assert($result["hosts"]["localhost1"]["desiredState"] != "INVALID STATE");
assert($result["hosts"]["localhost2"]["desiredState"] != "INVALID STATE");

print "Test createNewTransaction\n";

$result = $db->createNewTransaction($clusterName, "Status", "Pid");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["txnId"] == 1);

print "Test getTransactionStatusInfo\n";
$curTime = time();
$txnId = $result["txnId"];
$result = $db->getTransactionStatusInfo($clusterName, $txnId);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["txnId"] == $txnId);
assert(($result["createTime"]+600) > $curTime);
assert($result["pidInfo"] == "Pid");
assert($result["statusInfo"] == "Status");

print "Test updateTransactionStatusInfo\n";
$result = $db->updateTransactionStatusInfo($clusterName, $txnId, "New Status");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["sql"]["rowsChanged"] != 0);

$result = $db->getTransactionStatusInfo($clusterName, $txnId);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["txnId"] == $txnId);
assert($result["pidInfo"] == "Pid");
assert($result["statusInfo"] == "New Status");

print "Test updateTransactionPidInfo\n";
$result = $db->updateTransactionPidInfo($clusterName, $txnId, "New Pid");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["sql"]["rowsChanged"] != 0);

$result = $db->getTransactionStatusInfo($clusterName, $txnId);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["txnId"] == $txnId);
assert($result["pidInfo"] == "New Pid");
assert($result["statusInfo"] == "New Status");

print "Test insertOrUpdateSubTransaction - Insert only\n";
$subTxnId = 1;
$parentSubTxnId = 0;
$state = "foo1";
$description = "desc1";
$progress = "bar1";
$subTxnType = "type1";
$result = $db->insertOrUpdateSubTransaction($clusterName, $txnId, $subTxnId,
    $parentSubTxnId, $state, $description, $progress, $subTxnType);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$subTxnId = 2;
$parentSubTxnId = 0;
$state = "foo2";
$description = "desc2";
$result = $db->insertOrUpdateSubTransaction($clusterName, $txnId, $subTxnId,
$parentSubTxnId, $state, $description, $progress, $subTxnType);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getAllSubTransactionsInfo\n";
$result = $db->getAllSubTransactionsInfo($clusterName, $txnId);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["subTxns"])
       && count($result["subTxns"]) == 2);

$found = 0;
foreach ($result["subTxns"] as $subTxnId => $subTxn) {
  if ($subTxn["subTxnId"] == 1
      && $subTxn["parentSubTxnId"] == 0
      && $subTxn["state"] == "foo1"
      && $subTxn["description"] == "desc1"
      && $subTxn["progress"] == $progress
      && $subTxn["subTxnType"] == $subTxnType
      && $subTxn["opStatus"] == "[]") {
    ++$found;
  }
  else if ($subTxn["subTxnId"] == 2
      && $subTxn["parentSubTxnId"] == 0
      && $subTxn["state"] == "foo2"
      && $subTxn["description"] == "desc2"
      && $subTxn["progress"] == $progress
      && $subTxn["subTxnType"] == $subTxnType
      && $subTxn["opStatus"] == "[]") {
      ++$found;
  }
}
assert ($found == 2);

print "Test updateSubTransactionOpStatus\n";

$result = $db->updateSubTransactionOpStatus($clusterName, 1, 2, "Op Status");

print "Test insertOrUpdateSubTransaction - Update\n";
$subTxnId = 2;
$parentSubTxnId = 1;
$state = "foo2_1";
$description = "desc2_1";
$result = $db->insertOrUpdateSubTransaction($clusterName, $txnId, $subTxnId,
    $parentSubTxnId, $state, $description, $progress, $subTxnType);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->getAllSubTransactionsInfo($clusterName, $txnId);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["subTxns"])
&& count($result["subTxns"]) == 2);

$found = 0;
foreach ($result["subTxns"] as $subTxnId => $subTxn) {
  if ($subTxn["subTxnId"] == 1
      && $subTxn["parentSubTxnId"] == 0
      && $subTxn["state"] == "foo1"
      && $subTxn["description"] == "desc1"
      && $subTxn["opStatus"] == "[]") {
    ++$found;
  }
  else if ($subTxn["subTxnId"] == 2
      && $subTxn["parentSubTxnId"] == 1
      && $subTxn["state"] == "foo2_1"
      && $subTxn["description"] == "desc2_1"
      && $subTxn["opStatus"] == "Op Status") {
    ++$found;
  }
}
assert ($found == 2);

print "Test OrchestratorDB\n";
$puppet = "";
$odb = new OrchestratorDB($dbPath, $clusterName, $puppet);

print "Test getClusterServices\n";
$result = $odb->getClusterServices();
assert(count($result) == 2);
$found = 0;
foreach ($result as $svc) {
  if ($svc->name == "HDFS" || $svc->name == "MAPREDUCE") {
    ++$found;
  }
}
assert($found == 2);

print "Test getServiceDependencies\n";
$result = $odb->getServiceDependencies("MAPREDUCE");
assert(count($result) == 1);
assert($result[0]->name == "HDFS");

print "Test getServiceDependents\n";
$result = $odb->getServiceDependents("HDFS");
assert(count($result) == 1);
assert($result[0]->name == "MAPREDUCE");

print "Test getServiceComponents\n";
$result = $odb->getServiceComponents("HDFS");
$found = 0;
foreach ($result as $comp) {
  if ($comp->name == "NAMENODE"
      || $comp->name == "SNAMENODE"
      || $comp->name == "HDFS_CLIENT"
      || $comp->name == "DATANODE") {
    ++$found;
  }
}
assert(count($result) == 2);
assert($found == 2);

print "Test getComponentDependencies\n";
$result = $odb->getComponentDependencies("MAPREDUCE", "TASKTRACKER");
assert(count($result) == 1);
assert($result[0]->name == "JOBTRACKER");

print "Test getAllNodes\n";
$result = $odb->getAllNodes();
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["componentMapping"]));
assert(is_array($result["nodes"])
       && count($result["nodes"]) == 1);
assert($result["nodes"][0] == "localhost1");

print "Test getComponentNodes\n";
$svcCompObj = new ServiceComponent($clusterName, "DATANODE", "HDFS", "", $odb, $puppet, FALSE, "Datanode");
$result = $odb->getComponentNodes($svcCompObj);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["nodes"])
    && count($result["nodes"]) == 2);
assert($result["nodes"][0] == "localhost1"
    || $result["nodes"][0] == "localhost2");
assert($result["nodes"][1] == "localhost1"
    || $result["nodes"][1] == "localhost2");

print "Test setServiceState\n";
$svcObj = new Service($clusterName, "HDFS", 0, $odb, $puppet, "HDFS");
$result = $odb->setServiceState($svcObj, 1);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test setServiceComponentState\n";
$result = $odb->setServiceComponentState($svcObj->name, $svcCompObj->name, 2);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test persistTransaction\n";
$txnId = 1;
$subTxnId = 1;
$parentSubTxnId = 0;
$transaction = new Transaction($txnId, $subTxnId, $parentSubTxnId);
$result = $odb->persistTransaction($transaction, "UNINSTALLED", "NO STATE",
    "PENDING", "TYPE1", FALSE);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getServiceClientNode\n";
$result = $odb->getServiceClientNode("HDFS");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["nodes"])
       && count($result["nodes"]) == 2);

print "Test getRolesByHost\n";
$result = $db->getRolesForHosts($clusterName, array("localhost1", "localhost2"));
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["hosts"]["localhost1"]["services"]));
assert(is_array($result["hosts"]["localhost1"]["services"]["HDFS"])
      && is_array($result["hosts"]["localhost1"]["services"]["HDFS"]["components"])
      && count($result["hosts"]["localhost1"]["services"]["HDFS"]["components"]) == 2);
foreach ($result["hosts"]["localhost1"]["services"]["HDFS"]["components"] as $cmpNm => $cmpCheck) {
  assert($cmpCheck["componentName"] == $cmpNm);
  assert(isset($cmpCheck["state"]));
  assert(isset($cmpCheck["desiredState"]));
}
assert($result["hosts"]["localhost1"]["services"]["HDFS"]["components"]["HDFS_CLIENT"]["state"] == "INSTALLING");
assert($result["hosts"]["localhost1"]["services"]["HDFS"]["components"]["HDFS_CLIENT"]["desiredState"] == "INSTALLED");

print "Test getNodeServices\n";
$result = $odb->getNodeServices("localhost1");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["services"])
       && count($result["services"]) == 1);
assert($result["services"][0]->name == "HDFS");

print "Test getAllServiceDependencies\n";
$result = $db->getAllServiceDependencies();
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["serviceDependencies"])
       && count($result["serviceDependencies"]) > 5);

print "Test getRecursiveServiceDependency\n";
$result = $db->getRecursiveServiceDependency($result["serviceDependencies"],
    "HIVE");
assert(is_array($result)
    && count($result) == 2
    && in_array("HDFS", $result)
    && in_array("MAPREDUCE", $result));

print "Test setHostsState\n";
$hostsToUpdate = array (
   "HDFS" => array (
      "DATANODE" => array ( "localhost1"),
      "HDFS_CLIENT" => array ( "localhost1", "localhost2")
   )
);
$result = $db->setHostsState($clusterName, $hostsToUpdate, "FOO");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["sql"]["rowsChanged"] == 3);
$result = $db->getAllHostsByComponent($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["components"]["DATANODE"]["hosts"]["localhost1"]["state"]
  == "FOO");
assert($result["components"]["DATANODE"]["hosts"]["localhost2"]["state"]
  != "FOO");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost1"]["state"]
   == "FOO");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost2"]["state"]
   == "FOO");

print "Test setHostsDesiredState\n";
$result = $db->setHostsDesiredState($clusterName, $hostsToUpdate, "BAR");
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["sql"]["rowsChanged"] == 3);

$result = $db->getAllHostsByComponent($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["components"]["DATANODE"]["hosts"]["localhost1"]["desiredState"]
    == "BAR");
assert($result["components"]["DATANODE"]["hosts"]["localhost2"]["desiredState"]
    != "BAR");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost1"]["desiredState"]
    == "BAR");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost2"]["desiredState"]
    == "BAR");

$hostsToUpdate = array (
   "HDFS" => array (
      "DATANODE" => array ( "localhost1"),
      "HDFS_CLIENT" => array ( "localhost1")
   )
);
$result = $db->matchHostStateToComponent($clusterName, $hostsToUpdate);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->matchHostDesiredStateToComponent($clusterName, $hostsToUpdate);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->getAllHostsByComponent($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["components"]["DATANODE"]["hosts"]["localhost1"]["state"]
  != "FOO");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost1"]["state"]
  != "FOO");
assert($result["components"]["DATANODE"]["hosts"]["localhost1"]["desiredState"]
  != "BAR");
assert($result["components"]["HDFS_CLIENT"]["hosts"]["localhost1"]["desiredState"]
  != "BAR");


$hostsToUpdate = array (
   "HDFS" => array (
      "DATANODE" => array ( "localhost1", "localhost2"),
      "HDFS_CLIENT" => array ( "localhost1", "localhost2")
   )
);
$result = $db->matchHostStateToComponent($clusterName, $hostsToUpdate);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$result = $db->matchHostDesiredStateToComponent($clusterName, $hostsToUpdate);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getNodeRolesAndState\n";
$result = $odb->getNodeRolesAndState($hosts);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(isset($result["hosts"]["localhost1"]["services"]["HDFS"]["components"]["DATANODE"]["state"]));
assert(isset($result["hosts"]["localhost1"]["services"]["HDFS"]["components"]["HDFS_CLIENT"]["state"]));
assert(isset($result["hosts"]["localhost2"]["services"]["HDFS"]["components"]["DATANODE"]["state"]));
assert(isset($result["hosts"]["localhost2"]["services"]["HDFS"]["components"]["HDFS_CLIENT"]["state"]));

print "Test createServiceConfigSnapshot\n";
$result = $db->createServiceConfigSnapshot($clusterName, "Test snapshot");
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["configSnapshotId"] > 0);

print "Test getConfigSnapshot\n";
$configSnapshotId = $result["configSnapshotId"];
$result = $db->getConfigSnapshot($clusterName, $configSnapshotId);
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["configSnapshotId"] == $configSnapshotId);
assert($result["changeLog"] == "Test snapshot");
assert(is_array($result["config"]["properties"])
       && count($result["config"]["properties"]) > 0);
assert($result["updateTime"] >= time());

print "Test updateHostRoleConfigs\n";
$hostConfigs = array ( "ZOOKEEPER_SERVER" => array ( "localhost1" => array ( "zookeeper_quorum_id" => 1, "test_id" => 1),
                                              "localhost2" => array ( "zookeeper_quorum_id" => 1) ),
                       "ZOOKEEPER_CLIENT" => array ( "localhost1" => array ( "zookeeper_quorum_id" => 1, "test_id" => 1),
                                              "localhost2" => array ( "zookeeper_quorum_id" => 1) ));
$result = $db->updateHostRoleConfigs($clusterName, $hostConfigs);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(isset($result["sql"]["rowsChanged"])
    && $result["sql"]["rowsChanged"] == 6);

$result = $db->updateHostRoleConfigs($clusterName, $hostConfigs);
assert($result["result"] == 0);
assert($result["error"] == "");

$hostConfigs = array ( "ZOOKEEPER_SERVER" => array ( "localhost1" => array ( "zookeeper_quorum_id" => 1, "test_id" => 1),
                                              "localhost2" => array ( "zookeeper_quorum_id" => 2) ),
                       "ZOOKEEPER_CLIENT" => array ( "localhost1" => array ( "zookeeper_quorum_id" => 1, "test_id" => 1),
                                              "localhost2" => array ( "zookeeper_quorum_id" => 2) ));
$result = $db->updateHostRoleConfigs($clusterName, $hostConfigs);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Test getHostRoleConfigs\n";
$result = $db->getHostRoleConfigs($clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["properties"] == $hostConfigs);

$result = $db->getHostRoleConfigs($clusterName, "ZOOKEEPER_SERVER");
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["properties"] != $hostConfigs);
assert($result["properties"]["ZOOKEEPER_SERVER"] == $hostConfigs["ZOOKEEPER_SERVER"]);

print "Test getConfigPropertiesMetaInfo\n";
$result = $db->getConfigPropertiesMetaInfo();
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["configs"]));
foreach ($result["configs"] as $key => $info) {
  assert($info["key"] == $key);
  assert(isset($info["displayName"]));
  assert(isset($info["serviceName"]));
  assert(isset($info["description"]));
  assert(isset($info["displayType"]));
  assert(isset($info["value"]));
  assert(is_array($info["displayAttributes"]));
}

print "Test getAllUnassignedHosts\n";

$result = $db->getAllHostsInfo("", "", "");
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 3);

$hostsToAdd = array (
array (
        "hostName" => "localhost4",
        "ip" => "127.0.0.4",
        "totalMem" => 64,
        "cpuCount" => 4,
        "osArch" => "i386",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error",
        "attributes" => array ( "foo" => "bar" )
),
array (
        "hostName" => "localhost5",
        "ip" => "127.0.0.5",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "error",
        "badHealthReason" => "invalid ssh key"
)
);
$result = $db->addHostsToCluster($clusterName, $hostsToAdd);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");

$result = $db->getAllUnassignedHosts($clusterName);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 1);
assert($result["hosts"][0]["hostName"] == $hostsToAdd[0]["hostName"]);


?>
