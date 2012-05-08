<?php

include 'dbLib.php';
include 'utils.php';

$requestdata = file_get_contents('php://input');
$obj = json_decode($requestdata, true);

/*
$obj = array(
         "clusterName"=> "MyCluster-12345",
         "ClusterDeployUser"=> "hortonva",
         "ClusterDeployUserIdentityFile"=> "va_rsa",
         "ClusterHostsFile"=> "../hostsFile.txt",
         "nameNode"=> "hrt12n15",
         "secondaryNameNode"=> "hrt12n16",
         "jobTracker"=> "hrt12n17",
         "hbaseMaster"=> "hrt12n18",
         "hcatServer"=> "hrt12n19",
         "gangliaCollector"=> "hrt12n15",
         "nagiosServer"=> "hrt12n16",
         "oozieServer"=> "hrt12n17",
         "templetonNode"=> "hrt12n18",
         "mountPoints" => array ( "/grid/0", "/grid/1" );
         );
*/

$clusterName = $_GET['clusterName'];
$clusterDir = getClusterDir($clusterName);
$installerInputDir = getInstallerInputDir($clusterName);

// persist mount points 
storeConfiguredDirectories($clusterName, $obj);

$json_Output = array(
  "clusterName" => $clusterName,
  "services" => array (
    "$MapredName" => array (
      "enabled" => isServiceEnabled($clusterName, $MapredName),
      "properties" => array (
      "jobTrackerHeapSize" => array(
        "type" => "text",
        "displayName" => "JobTracker heap size",
        "value" => "",
      ),
      "childJavaOptions" => array(
        "type" => "text",
        "displayName" => "Child Java options for MR tasks",
        "value" => "",
      ),
      "maxMapsPerNode" => array(
        "type" => "text",
        "displayName" => "Maximum number of maps per node",
        "value" => "",
      ),
      "maxRedsPerNode" => array(
        "type" =>"text",
        "displayName" => "Maximum number of reducers per node",
        "value" => "",
      ),
      ),
    ),
    "$HDFSName" => array (
      "enabled" => isServiceEnabled($clusterName, $HDFSName),
      "properties" => array (
      "nameNodeHeapSize" => array(
        "type" => "text",
        "displayName" => "Maximum heap size for NameNode",
        "value" => "",
      ),
      "dataNodeHeapSize" => array(
        "type" => "text",
        "displayName" => "Maximum heap size for DataNodes",
        "value" => "",
      ),
      ),
    ),
    "$PigName" => array (
      "enabled" => isServiceEnabled($clusterName, $PigName),
      "properties" => array (
      ),
    ),
    "$HBaseName" => array (
      "enabled" => isServiceEnabled($clusterName, $HBaseName),
      "properties" => array (
      ),
    ),
    "$OozieName" => array (
      "enabled" => isServiceEnabled($clusterName, $OozieName),
      "properties" => array (
      ),
    ),
    "$HCatName" => array (
      "enabled" => isServiceEnabled($clusterName, $HCatName),
      "properties" => array (
        "mysqlHost" => array (
          "type" => "text",
          "displayName" => "MySql host",
          "value" => "",
        ),

        "mysqlDB" => array (
          "type" => "text",
          "displayName" => "MySql DBRoot",
          "value" => "",
        ),
        "mysqlUser" => array (
          "type" => "text",
          "displayName" => "MySql DBUser",
          "value" => "",
        ),
        "mysqlPasswd" => array (
          "type" => "password",
          "displayName" => "MySql Passwd",
          "value" => "",
        ),
      ),
    ),
    "$TempletonName" => array (
      "enabled" => isServiceEnabled($clusterName, $TempletonName),
      "properties" => array (
      ),
    ),
    "$SqoopName" => array (
      "enabled" => isServiceEnabled($clusterName, $SqoopName),
      "properties" => array (
      ),
    ),
    "$GatewayName" => array (
      "enabled" => isServiceEnabled($clusterName, $GatewayName),
      "properties" => array (
      ),
    ),
    "$ZooKeeperName" => array (
      "enabled" => isServiceEnabled($clusterName, $ZooKeeperName),
      "properties" => array (
      ),
    ),
    "$MiscName" => array (
      "enabled" => true,
      "properties" => array (
        "enableLZO" => array (
          "type" => "checkbox",
          "displayName" => "Enable LZO",
          "value" => false,
        ),
      ),
    ),
    "$GangliaCollectorName" => array (
      "enabled" => isServiceEnabled($clusterName, $GangliaCollectorName),
      "properties" => array (
      ),
    ),
    "$NagiosName" => array (
      "enabled" => isServiceEnabled($clusterName, $NagiosName),
      "properties" => array (
      ),
    ),
  ),
);
print(json_encode($json_Output));

?>
