<?php

// Valid constant names
$rootDir = "/var/run/HMCDir/";
$allClustersDir = $rootDir."clusters/";

$SshKeyFileName = "user-ssh-key";
$HostsFileName = "hosts.txt";

$MapredName = "mapReduce";
$HDFSName = "hdfs";
$HBaseName = "hBase";
$PigName = "pig";
$HCatName = "hCatalog";
$TempletonName = "templeton";
$OozieName = "oozie";
$ZooKeeperName = "zookeeper";
$SqoopName = "sqoop";
$GatewayName = "gateway";
$MiscName = "miscellaneous";
$GangliaCollectorName = "gangliaCollector";
$NagiosName = "nagios";

$JobTrackerName = "jobTracker";
$NameNodeName = "nameNode";
$SecondaryNameNodeName = "secondaryNameNode";
$HBaseMasterName = "hBaseMaster";
$OozieServerName = "oozieServer";
$HCatServerName = "hCatServer";
$MySqlServerName = "mySqlServer";
$TempletonNodeName = "templetonNode";
$NagiosServerName = "nagiosServer";

function getAllClustersDir(){
  global $allClustersDir;
  return $allClustersDir;
}

function getClusterDir($clusterId) {
  if (!isset($clusterId) || $clusterId == "") {
    error_log("**** Got invalid clusterId **** ");
    error_log(print_r(debug_backtrace(), true));
  }
  global $allClustersDir;
  return $allClustersDir.$clusterId."/";
}

function getSshKeyFilePath($clusterId) {
  global $SshKeyFileName;
  $clusterDir = getClusterDir($clusterId);
  return $clusterDir.$SshKeyFileName;
}

function getHostsFilePath($clusterId) {
  global $HostsFileName;
  $clusterDir = getClusterDir($clusterId);
  return $clusterDir.$HostsFileName;
}

// TODO: FIX: Create a separate installer php page.
function getInstallerInputDir($clusterId) {
  $clusterDir = getClusterDir($clusterId);
  return $clusterDir."/installer-input/";
}

// Till we have a db, we use filebased db
function getDBDir() {
  global $rootDir;
  return $rootDir."my-db/";
}

function getClusterDBDir($clusterId) {
  $dbdir = getDBDir().$clusterId."/";
  return $dbdir;
}

/* Moved to createCluster.php. Not needed anymore
function initCluster($clusterId) {
  $clusterDir = getClusterDir($clusterId);
  // TODO: FIXME: All die calls below
  if (!is_dir($clusterDir) && !mkdir($clusterDir, 0700, true)) {
        die('Failed to create directory...');
  }
  $dbDir = getClusterDBDir($clusterId);
  if (!is_dir($dbDir) && !mkdir($dbDir, 0700, true)) {
        die('Failed to create db-directory...');
  }
  $installerInputDir = getInstallerInputDir($clusterId);
  if (!is_dir($installerInputDir) && !mkdir($installerInputDir, 0700, true)) {
        die('Failed to create installer directory...');
  }
}
*/

/*
function storeSelectedServiceNames($servicesArray) {
  $dbDir = getDBDir();
  foreach ($servicesArray as $service) {
    touch($dbDir."SERVICE-".$service); // TODO: FIX: error code
  }
}
*/

function getClusterDeployUser($clusterId) {
  $cluster_info = getPersistedClusterInfo($clusterId);
  return $cluster_info["installInfo"]["deployUser"];
}

function storeServiceMasters($clusterId, $serviceMasters) {
  $cluster_info = getPersistedClusterInfo($clusterId);
  foreach ($serviceMasters as $svc=>$arr) {
    foreach ($arr as $role=>$host) {
      $cluster_info["services"][$svc]["masters"][$role]["hostName"] = $host;
    }
  }
  persistClusterInfo($clusterId, $cluster_info);
}

function isServiceEnabled($clusterId, $serviceName) {
  // $dbDir = getDBDir();
  // return file_exists($dbDir."SERVICE-".$serviceName);
  $info = getPersistedClusterInfo($clusterId);
  if (!$info) { return false; }

  if (isset($info["services"][$serviceName])
      && isset($info["services"][$serviceName]["enabled"])
      && $info["services"][$serviceName]["enabled"] == true) {
    return true;
  }
  return false;
}

function storeOptions($clusterId, $optionsFromUser) {
  $cluster_info = getPersistedClusterInfo($clusterId);
  $services = $cluster_info["services"];

  // Loop through the options and save them
  error_log(json_encode($optionsFromUser), 0);
  foreach ($optionsFromUser as $serviceName => $propertiesList) {
    $serviceProperties = $services[$serviceName]["properties"];
    //error_log($serviceName, 0);
    //error_log(json_encode($propertiesList["properties"]), 0);
    foreach($propertiesList["properties"] as $singlePropKey => $singlePropObj) {
      $serviceProperties[$singlePropKey] = $singlePropObj;
    }
    //$serviceProperties = array_merge($serviceProperties, $propertiesList["properties"]);

    // Persist back
    $services[$serviceName]["properties"] = $serviceProperties;
  }

  // Store the final data back again
  $cluster_info["services"] = $services;
  persistClusterInfo($clusterId, $cluster_info);
}

function storeConfiguredDirectories($clusterId, $mountInfo) {

  $cluster_info = getPersistedClusterInfo($clusterId);
  $services = $cluster_info["services"];

  // Store the list of all mount-points
  $cluster_info["sysInfo"]["mountPoints"] = $mountInfo["mountPoints"];

  // MapReduce properties
  $mapRedServiceProperties = $services[$GLOBALS['MapredName']]["properties"];
  $mapRedServiceProperties["mapReduceLocalDir"] = array( "displayName" => "MapReduce local directories", "value" => $mountInfo["clusterConfig"]["mapReduceLocalDir"]);
  $services[$GLOBALS['MapredName']]["properties"] = $mapRedServiceProperties;

  // HDFS properteis
  $hdfsProperties = $services[$GLOBALS['HDFSName']]["properties"];
  $hdfsProperties["nameNodeDataDir"] = array( "displayName" => "NameNode meta-data directories", "value" =>$mountInfo["clusterConfig"]["nameNodeDataDir"]);
  $hdfsProperties["dataNodeDataDir"] = array( "displayName" => "DataNode data directories", "value" => $mountInfo["clusterConfig"]["dataNodeDataDir"]);
  $services[$GLOBALS['HDFSName']]["properties"] = $hdfsProperties;

  // Oozie properties
  $oozieProperties = $services[$GLOBALS['OozieName']]["properties"];
  $oozieProperties["oozieDbDir"] = array( "displayName" => "Oozie database directory", "value" => $mountInfo["clusterConfig"]["oozieDbDir"]);
  $services[$GLOBALS['OozieName']]["properties"] = $oozieProperties;

  // Zookeeper properties
  $zooKeeperProperties = $services[$GLOBALS['ZooKeeperName']]["properties"];
  $zooKeeperProperties["zooKeeperDir"] = array( "displayName" => "ZooKeeper data directory", "value" => $mountInfo["clusterConfig"]["zooKeeperDataDir"]);
  $services[$GLOBALS['ZooKeeperName']]["properties"] = $zooKeeperProperties;

  // Store the final data
  $cluster_info["services"] = $services;
  persistClusterInfo($clusterId, $cluster_info);
}

function storeMemoryOptionSuggestions($clusterId, $memoryOptionsSuggestions) {

  $cluster_info = getPersistedClusterInfo($clusterId);
  $services = $cluster_info["services"];

  // MapReduce properties
  $mapRedServiceProperties = $services[$GLOBALS['MapredName']]["properties"];
  $mapRedServiceProperties["JobTrackerHeapSize"] = array( "displayName" => "Maximium Heap size for JobTracker", "value" => $memoryOptionsSuggestions["HDPJobTrackerHeapSize"]);
  $mapRedServiceProperties["ChildJavaOptsSize"]= array( "displayName" => "Child Java options for MR tasks", "value" => $memoryOptionsSuggestions["HDPMapRedChildJavaOptsSize"]);
  $mapRedServiceProperties["MapTasksMax"] = array( "displayName" => "Maximum number of maps per node", "value" => $memoryOptionsSuggestions["HDPMapRedMapTasksMax"]);
  $mapRedServiceProperties["ReduceTasksMax"] = array( "displayName" => "Maximum number of reduces per node", "value" => $memoryOptionsSuggestions["HDPMapRedReduceTasksMax"]);
  $services[$GLOBALS['MapredName']]["properties"] = $mapRedServiceProperties;

  // HDFS properteis
  $hdfsProperties = $services[$GLOBALS['HDFSName']]["properties"];
  $hdfsProperties["NameNodeHeapSize"] = array( "displayName" => "Maximum heap size for NameNode", "value" => $memoryOptionsSuggestions["HDPNameNodeHeapSize"]);
  $hdfsProperties["DataNodeHeapSize"] = array( "displayName" => "Maximum heap size for DataNodes", "value" => $memoryOptionsSuggestions["HDPDataNodeHeapSize"]);
  $services[$GLOBALS['HDFSName']]["properties"] = $hdfsProperties;

  // Store the final data
  $cluster_info["services"] = $services;
  error_log("STORE MEM OPTIONS");
  error_log(print_r($cluster_info, true));
  persistClusterInfo($clusterId, $cluster_info);
}

function persistClusterInfo($clusterId, $info) {
  $filename = "ClusterInfo.out";
  $GLOBALS["CLUSTER_INFO"][$clusterId]["info"] = $info;
  persistData($clusterId, $filename, $info);
}


function getPersistedClusterInfo($clusterId) {
  if (isset($GLOBALS["CLUSTER_INFO"][$clusterId]["info"])) {
    return $GLOBALS["CLUSTER_INFO"][$clusterId]["info"];
  }

  $filename = "ClusterInfo.out";
  $data = getPersistedData($clusterId, $filename);
  $GLOBALS["CLUSTER_INFO"][$clusterId]["info"] = $data;
  return $data;
}

/*
 * filename to persist in
 * data mixed array which will be stored as serialized json
 */
function persistData($clusterId, $filename, $data) {
  $filepath = getClusterDBDir($clusterId).$filename;
  $json_data = json_encode($data);
  if (!file_put_contents($filepath, $json_data)) {
    die("Failed to persist data to $filename");
  } 
}

function getPersistedData($clusterId, $filename) {
  $filepath = getClusterDBDir($clusterId).$filename; 
  $data = file_get_contents($filepath);
  if ($data != FALSE && isset($data) && $data != "") {
    return json_decode($data, true);
  }
  error_log("Could not retrieve any data from $filepath");
  return NULL;
}

function setInstallStarted($clusterId) {
  $clusterInfo = getPersistedClusterInfo($clusterId);
  $clusterInfo["install"] = array();
  $clusterInfo["install"]["started"] = true;
  persistClusterInfo($clusterId, $clusterInfo);

  global $CLUSTER_STATUS_INSTALL_IN_PROGRESS;
  setClusterStatus($clusterId, $CLUSTER_STATUS_INSTALL_IN_PROGRESS);
}

function hasInstallStarted($clusterId) {
  $clusterInfo = getPersistedClusterInfo($clusterId);

  if (isset( $clusterInfo["install"]["started"])
      &&  $clusterInfo["install"]["started"] == true) {
    return TRUE;
  }
  return FALSE;
}

$CLUSTER_STATUS_CONFIG_IN_PROGRESS = "CONFIG IN PROGRESS";
$CLUSTER_STATUS_INSTALL_IN_PROGRESS = "INSTALL IN PROGRESS";
$CLUSTER_STATUS_INSTALL_DONE = "INSTALL DONE";
$CLUSTER_STATUS_UNINSTALL_IN_PROGRESS = "UNINSTALL IN PROGRESS";
$CLUSTER_STATUS_INSTALL_SUCCESSFUL = "SUCCESSFUL INSTALL";
$CLUSTER_STATUS_INSTALL_UNSUCCESSFUL = "UNSUCCESSFUL INSTALL";
$CLUSTER_STATUS_DOES_NOT_EXIST = "DOES NOT EXIST";

function setClusterStatus($clusterId, $status) {
  global $CLUSTER_STATUS_CONFIG_IN_PROGRESS;
  global $CLUSTER_STATUS_INSTALL_IN_PROGRESS;
  global $CLUSTER_STATUS_INSTALL_DONE;
  global $CLUSTER_STATUS_UNINSTALL_IN_PROGRESS;
  global $CLUSTER_STATUS_INSTALL_SUCCESSFUL;
  global $CLUSTER_STATUS_INSTALL_UNSUCCESSFUL;
  if ($status != $CLUSTER_STATUS_CONFIG_IN_PROGRESS
      && $status != $CLUSTER_STATUS_INSTALL_IN_PROGRESS
      && $status != $CLUSTER_STATUS_INSTALL_DONE
      && $status != $CLUSTER_STATUS_UNINSTALL_IN_PROGRESS
      && $status != $CLUSTER_STATUS_INSTALL_SUCCESSFUL
      && $status != $CLUSTER_STATUS_INSTALL_UNSUCCESSFUL) {
    die ("Invalid status : $status");
  }
  $clusterInfo = getPersistedClusterInfo($clusterId);
  if (!isset($clusterInfo["clusterStatus"])) {
    $clusterInfo["clusterStatus"] = array();
  }
  $clusterInfo["clusterStatus"]["status"] = $status;
  persistClusterInfo($clusterId, $clusterInfo);
} 

function getClusterStatus($clusterId) {
  global $CLUSTER_STATUS_DOES_NOT_EXIST;
  $clusterInfo = getPersistedClusterInfo($clusterId);
  if (NULL == $clusterInfo || !isset($clusterInfo["clusterStatus"]["status"])) {
    return $CLUSTER_STATUS_DOES_NOT_EXIST;
  }
  return $clusterInfo["clusterStatus"]["status"];
}

function initCluster($clusterId) {
  $clusterDir = getClusterDir($clusterId);
  // TODO: FIXME: All die calls below
  if (!is_dir($clusterDir) && !mkdir($clusterDir, 0700, true)) {
        die('Failed to create directory...');
  }
  $dbDir = getClusterDBDir($clusterId);
  if (!is_dir($dbDir) && !mkdir($dbDir, 0700, true)) {
        die('Failed to create db-directory...');
  }
  $installerInputDir = getInstallerInputDir($clusterId);
  if (!is_dir($installerInputDir) && !mkdir($installerInputDir, 0700, true)) {
        die('Failed to create installer directory...');
  }
}



?>
