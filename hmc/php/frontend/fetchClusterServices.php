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

$dbPath = $GLOBALS["DB_PATH"];

header("Content-type: application/json");

$logger = new HMCLogger("FetchClusterServices");
$dbAccessor = new HMCDBAccessor($dbPath);

$clusterName = $_GET['clusterName'];
if (isset($_GET['getComponents'])) {
  $getComponents = $_GET['getComponents'];
} else {
  $getComponents = "false";
}

if (isset($_GET['getConfigs'])) {
  $getConfigs = $_GET['getConfigs'];
} else {
  $getConfigs = "false";
}

if (isset($_GET['serviceName'])) {
  $forService = $_GET['serviceName'];
} else {
  $forService = "all";
}

function getAllComponentsForService ($serviceName)
{
  global $logger, $dbAccessor, $clusterName;

  $logger->log_debug("Get all the service components for service $serviceName");
  $returnComponentsArray = array();
  // populate all components for the services
  // includes the isMaster information
  $componentResult = $dbAccessor->getAllServiceComponents($serviceName);
  if ($componentResult["result"] == 0) {
    foreach($componentResult["components"] as $componentName => $component) {
      $hostsForComponentDBResult = $dbAccessor->getHostsForComponent($clusterName, $componentName);
      if ($hostsForComponentDBResult["result"] != 0 ) {
        $logger->log_error("Got error while getting hosts for component ".$hostsForComponentDBResult["error"]);
        print json_encode($hostsForComponentDBResult);
        return;
      }
      $allHosts = array_keys($hostsForComponentDBResult["hosts"]);
      $component["hostNames"] = $allHosts;
      array_push($returnComponentsArray, $component);
    }
  } else {
    $gotError = "Error while getting component info: " . $componentResult["error"];
    $logger->log_error($gotError);
    print (json_encode( array("result" => 1, "error" => $gotError)));
  }

  return $returnComponentsArray;
}

/* The Main Event. */

$services = array();

$allServicesInfo = $dbAccessor->getAllServicesList();

if( $allServicesInfo['result'] == 0 ) {
  /* This needs to be retrieved once at the beginning for repeatedly passing
   * in to getRecursiveServiceDependency() below.
   */
  $allServiceDependencies = $dbAccessor->getAllServiceDependencies();

  if( $allServiceDependencies['result'] == 0 ) {
    $clusterServicesInfo = $dbAccessor->getAllServicesInfo($clusterName);

    if( $clusterServicesInfo['result'] == 0 ) {
      /* Loop through the static catalog of services. */
      foreach( $allServicesInfo['services'] as $serviceName => $serviceInfo ) {
        /* Initialize each entry in $services with that from the static
         * services catalog.
         */
        if (($forService !== "all") && ($serviceName !== $forService)) {
          continue;
        }
        $services[$serviceName] = $serviceInfo;

        /* Next, augment the entry for $serviceName with the dependency list. */
        $services[$serviceName]['dependencies'] =
          $dbAccessor->getRecursiveServiceDependency
          ( $allServiceDependencies['serviceDependencies'], $serviceName );

        /* Next, augment the entry for $serviceName with the dependents list. */
        $services[$serviceName]['dependents'] =
          $dbAccessor->getRecursiveServiceDependents
          ( $allServiceDependencies['serviceDependencies'], $serviceName );

        /* Finally, if $serviceName has an entry in $clusterServicesInfo (which
         * means $serviceName has been selected for $clusterName), tack on some
         * additional install-specific data.
         */
        if( isset($clusterServicesInfo['services'][$serviceName]) ) {
          $services[$serviceName]['state'] = $clusterServicesInfo['services'][$serviceName]['state'];
          $services[$serviceName]['desiredState'] = $clusterServicesInfo['services'][$serviceName]['desiredState'];
          $services[$serviceName]['isEnabled'] = $clusterServicesInfo['services'][$serviceName]['isEnabled'];
          if ($getComponents == "true") {
            $services[$serviceName]["components"] = getAllComponentsForService($serviceName);
          }
        }
      }
    } else {
      $gotError = "Error while getting cluster services info: " . $clusterServicesInfo['error'] ;
      $logger->log_error($gotError);
      print (json_encode( array("result" => 1, "error" => $gotError)));
      return;
    }
  } else {
    $gotError = "Error while getting all service dependencies: " . $allServiceDependencies['error'] ;
    $logger->log_error($gotError);
    print (json_encode( array("result" => 1, "error" => $gotError)));
    return;
  }
} else {
  $gotError = "Error while getting all services info: " . $allServicesInfo['error'] ;
  $logger->log_error($gotError);
  print (json_encode( array("result" => 1, "error" => $gotError)));
  return;
}

if ($getConfigs == "true") {
// populating the properties for the services.
  $staticConfigProps = $dbAccessor->getConfigPropertiesMetaInfo();
  if ($staticConfigProps["result"] != 0) {
    $logger->log_error("Failed to get config properties");
    return;
  } else { // success case we found the meta info
    $dynamicConfigTableResult = $dbAccessor->getServiceConfig($clusterName);
    if ($dynamicConfigTableResult["result"] != 0) {
      print ("Error in getting configured properties for services"
        .$dynamicConfigTableResult["error"]);
      return;
    } else { // success case we found the configured properties.
      $dynamicConfigTable = $dynamicConfigTableResult["properties"];
      foreach ($staticConfigProps["configs"] as $key => $propInfo) {
        $serviceName = $propInfo["serviceName"];
        // if service specific config needs to be passed back,
        // bypass all other services.
        if (($forService !== "all") && ($forService !== $serviceName)) {
          continue;
        }
        /*
        $logger->log_debug("Service static config $serviceName keys ".
        json_encode($key));
         */
        if (!array_key_exists("properties", $services[$serviceName])) {
          $services[$serviceName]["properties"] = array();
        }

        if (isset($dynamicConfigTable[$key])) {
          $value = $dynamicConfigTable[$key];
          // $logger->log_debug("$value from service config for $key");
        } else {
          $value = $propInfo["value"];
        }

        $services[$serviceName]["properties"][$key] = array(
            "displayName" => $propInfo["displayName"],
            "description" => $propInfo["description"],
            "type" => $propInfo["displayType"],
            "unit" => (isset($propInfo["displayAttributes"]["unit"]) ? $propInfo["displayAttributes"]["unit"] : null),
            "value" => $value,
            "displayAttributes" => $propInfo["displayAttributes"]
          );
      }
    }
  }
}

$result = 0;
$error = "";
/* Create the output data... */
$jsonOutput = array(
    'result' => $result,
    'error' => $error,
    'response' => array(
      'clusterName' => $clusterName,
      'managerHostName' => strtolower(exec('hostname -f')),
      'services' => $services )
  );

/* ...and spit it out. */

print (json_encode($jsonOutput));

?>
