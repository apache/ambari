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
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

$dbPath = $GLOBALS["DB_PATH"];

header("Content-type: application/json");

$logger = new HMCLogger("FetchGangliaClusterHosts");
$dbAccessor = new HMCDBAccessor($dbPath);

$clusterName = $_GET['clusterName'];

$gangliaClusterNameToServiceComponentNamesMapping = 
  array( 
    'HDPHBaseMaster' => array('HBASE_MASTER'),
    'HDPJobTracker' => array('JOBTRACKER'), 
    'HDPNameNode' => array('NAMENODE','SNAMENODE'),
    'HDPSlaves' => array('HBASE_REGIONSERVER','TASKTRACKER','DATANODE') 
  );

/* The meat of what we're here to return. */
$gangliaClusters = array();

/* The Main Event. */

$allHostsInfoByComponent = $dbAccessor->getAllHostsInfoByComponent($clusterName);

if( $allHostsInfoByComponent['result'] == 0 ) {

  /* Use $gangliaClusterNameToServiceComponentNamesMapping as our guiding light. */
  foreach( $gangliaClusterNameToServiceComponentNamesMapping as $gangliaClusterName => $serviceComponentNames ) {

    /* Reinitialize this "working copy" for each $gangliaClusterName. */
    $currentGangliaClusterHostsInfo = array();

    /* Each Ganglia Cluster Name maps to a list of Service Component Names, so 
     * gather information for the hosts in ALL the relevant Service Components.
     */
    foreach( $serviceComponentNames as $serviceComponentName ) {
      if( isset($allHostsInfoByComponent['components'][$serviceComponentName]) ) {
        foreach( $allHostsInfoByComponent['components'][$serviceComponentName]['hosts'] as $hostName => $hostInfo ) {
          $currentGangliaClusterHostsInfo[] = $hostInfo;
        }
      }
    }

    /* De-dupe all the host info entries before storing a final list for $gangliaClusterName. 
     *
     * When we enforce a minimum version of PHP > 5.2.9, replace the encode+decode trick below with:
     * 
     *    $gangliaClusters[$gangliaClusterName] = array_unique($currentGangliaClusterHostsInfo, SORT_REGULAR);
     */
    $gangliaClusters[$gangliaClusterName] = 
      array_map('json_decode', array_unique(array_map('json_encode', $currentGangliaClusterHostsInfo)));
  }

} else {
  $gotError = "Error while getting all hosts info by component: " . $allHostsInfoByComponent['error'] ;
  $logger->log_error($gotError);
  print (json_encode( array("result" => 1, "error" => $gotError)));
  return;
}

$result = 0;
$error = "";
/* Create the output data... */
$jsonOutput = array(
  'result' => $result,
  'error' => $error,
  'response' => array(
    'clusterName' => $clusterName,
    'gangliaClusters' => $gangliaClusters)
);

/* ...and spit it out. */

print (json_encode($jsonOutput));

?>
