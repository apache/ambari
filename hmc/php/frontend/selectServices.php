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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once "../util/clusterState.php";
include_once '../util/selectNodes.php';
// include_once 'install_puppet.php';

$logger = new HMCLogger("InitializeCluster");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$selectNodes = new SelectNodes();

header("Content-type: application/json");

$clusterName = $_GET['clusterName'];
// Validate clusterId: TODO; FIXME

/////// only persist the yum repo stuff
$serviceConfigResult = $dbAccessor->getServiceConfig($clusterName);
if ($serviceConfigResult["result"] != 0) {
  $logger->log_error("Failed to get service config ".json_encode($serviceConfigResult));
  print (json_encode($serviceConfigResult));
  return;
}

// We need to clean up prior instances for this cluster name
$dbAccessor->cleanupServices($clusterName);

$yumInfoArray = array("using_local_repo" => $serviceConfigResult['properties']['using_local_repo'], 
                      "yum_repo_file" => $serviceConfigResult['properties']['yum_repo_file']
                     );
$dbResponse = $dbAccessor->updateServiceConfigs($clusterName, $yumInfoArray);
if ($dbResponse["result"] != 0) {
    $logger->log_error("Got error while persisting configs: ".$dbResponse["error"]);
    print (json_encode($dbResponse));
    return;
}

////// done persisting 

// Read from the input
$requestdata = file_get_contents('php://input');
$requestObj = json_decode($requestdata, true);

// Persist the services enabled stuff
$servicesCheckList = $requestObj["services"];

// create local copy for easy lookup
$tempLocalServicesCopy = array();
foreach($servicesCheckList as $serviceInfo) {
  $tempLocalServicesCopy[$serviceInfo["serviceName"]] = array("isEnabled" => $serviceInfo["isEnabled"]);
}

$logger->log_debug("Temp local copy for services \n".json_encode($tempLocalServicesCopy));

$svcDepsListArr = $dbAccessor->getAllServiceDependencies();
if ($svcDepsListArr["result"] != 0) {
  $logger->log_error("Got error while getting service dependencies: ".$svcDepsListArr["error"]);
  print json_encode($svcDepsListArr);
  return;
}
$svcDepsList = $svcDepsListArr["serviceDependencies"];

// generate full svc list based on dep tree
$localServicesCopy = array();
foreach ($tempLocalServicesCopy as $svcName => $svcInfo) {
  if (!$svcInfo["isEnabled"]
      && !isset($localServicesCopy[$svcName])) {
    $localServicesCopy[$svcName] = $svcInfo;
    continue;
  }
  $localServicesCopy[$svcName] = array("isEnabled" => TRUE);
  $svcDeps = $dbAccessor->getRecursiveServiceDependency(
      $svcDepsList, $svcName);

  if (is_array($svcDeps)) {
    foreach ($svcDeps as $svcDep) {
      $localServicesCopy[$svcDep] = array("isEnabled" => TRUE);
    }
  }
}

$servicesCheckList = array();
foreach ($localServicesCopy as $svcName => $svcInfo) {
  $svcInfo["serviceName"] = $svcName;
  array_push($servicesCheckList, $svcInfo);
}

$logger->log_debug("svc list".json_encode($servicesCheckList)."========");

$dbResponse = $dbAccessor->addServicesToCluster($clusterName, $servicesCheckList);
if ($dbResponse["result"] != 0) {
  $logger->log_error("Got error while adding service: ".$dbResponse["error"]);
  print json_encode($dbResponse);
  return;
}

$jsonOutput["clusterName"] = $clusterName;

// Populate all hosts for the UI
$jsonOutput["allHosts"] = array();
/*
 * FIXME need to get all hosts from select Nodes script
 */
$allHostsInfo = $dbAccessor->getAllHostsInfo($clusterName,
  array("=" => array ( "discoveryStatus" => "SUCCESS")),
  array(
    array("sortColumn" => "totalMem", "sortOrder" => "DESC"),
    array("sortColumn" => "cpuCount", "sortOrder" => "DESC"),
    array("sortColumn" => "hostName", "sortOrder" => "ASC")
  )
);
if ($allHostsInfo["result"] != 0 ) {
  $logger->log_error("Got error while getting hostsInfo ".$allHostsInfo["error"]);
  print json_encode($allHostsInfo);
  return;
}

$logger->log_debug("HOSTS_INFO: ".json_encode($allHostsInfo["hosts"]));
foreach($allHostsInfo["hosts"] as $hostInfo) {
  $logger->log_debug("HOST: ".json_encode($hostInfo));
  array_push($jsonOutput["allHosts"], $hostInfo);
}

// bootstrap($jsonOutput["allHosts"], getSshKeyFilePath($clusterName));

$suggestedNodes = $selectNodes->selectNodes($clusterName,
                                            $dbAccessor);

//////////////////// Populate service masters for the UI ///////////////////////////
$jsonOutput["services"] = array();
$allServicesArray = $dbAccessor->getAllServicesList();
if ($allServicesArray["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServicesArray["error"]);
  print json_encode($allServicesArray);
  return;
}

$allServicesInfo = $dbAccessor->getAllServicesInfo($clusterName);
if ($allServicesInfo["result"] != 0 ) {
  $logger->log_error("Got error while getting all services list ".$allServicesInfo["error"]);
  print json_encode($allServicesInfo);
  return;
}

foreach($allServicesArray["services"] as $service) {
  $thisService = array();
  $serviceName = $service["serviceName"];
  $thisService["enabled"] = $allServicesInfo["services"][$serviceName]["isEnabled"];
  $thisService["masters"] = array();
  $allServiceComponents = $dbAccessor->getAllServiceComponents($serviceName);
  if ($allServiceComponents["result"] != 0 ) {
    $logger->log_error("Got error while getting all service compoenents:".$allServiceComponents["error"]);
    print json_encode($allServiceComponents);
    return;
  }

  foreach($allServiceComponents["components"] as $componentName => $component) {
    if($component["isMaster"] == 0) {
      continue;
    }
    $thisMaster = array();
    $hosts = $suggestedNodes["mastersToHosts"][$componentName]["hostNames"];
    $thisMaster["name"] = $component["componentName"];
    $thisMaster["displayName"] = $component["displayName"];
    $thisMaster["description"] = $component["description"];
    $thisMaster["hostNames"] =
          $hosts;
    array_push($thisService["masters"], $thisMaster);
  }
  array_push($jsonOutput["services"], $thisService);
}
//////////////////// End of populate service masters for the UI ///////////////////////////

$jsonOutput['managerHostName'] = strtolower(exec('hostname -f'));

// Update the state of the cluster.
$result = 0;
$error = "";

$state = "CONFIGURATION_IN_PROGRESS";
$displayName = "Configuration in progress";
$context = array (
  'stage' => "SELECT_SERVICES"
);

$retval = updateClusterState($clusterName, $state, $displayName, $context);
if ($retval['result'] != 0) {
  $result = $retval['result'];
  $error = $retval['error'];
}

print (json_encode(array("result" => $result, "error" => $error, "response" => $jsonOutput)));
?>
