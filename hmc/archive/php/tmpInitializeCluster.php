<?php
include './dbLib.php';
include './utils.php';

// Read from the input
$requestdata = file_get_contents('php://input');
//error_log($requestdata, 3, "/tmp/rez.log");
$requestObj = json_decode($requestdata, true);

$clusterName = $_GET['clusterName'];
// Validate clusterName: TODO; FIXME
$clusterDir = getClusterDir($clusterName);
/*
if(!is_dir($clusterDir)) {
  print "Cluster ".$clusterName." doesn't exist";
  exit 1;
}
*/

$suggestNodesScriptInputDir = $clusterDir."/suggestNodesScriptInputDir/";
$suggestNodesScriptOutputDir = $clusterDir."/suggestNodesScriptOutputDir/";
mkdir($suggestNodesScriptInputDir); // TODO: Check error conditions
mkdir($suggestNodesScriptOutputDir);// TODO: Check error conditions

// Hard code host and identity files for now
//$requestObj['ClusterDeployUserIdentityFile']="va_rsa";
//$requestObj['ClusterHostsFile']="hostsFile.txt";

// write the input file to the node-suggestor
$fp = fopen($suggestNodesScriptInputDir."/data.txt", 'w');
fwrite($fp, 'HDPClusterDeployUser='.$requestObj['ClusterDeployUser']."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.getSshKeyFilePath($clusterName)."\n");
fwrite($fp, 'HDPClusterHostsFile='.getHostsFilePath($clusterName)."\n");
fclose($fp);

// invoke the node-suggstor
$cmd = "/var/www/html/KickAssHDPUI/ShellScripts/suggestNodes.sh ".$suggestNodesScriptInputDir."/data.txt ".$suggestNodesScriptOutputDir;
$handle = popen($cmd, "r");
// Do some checks on return status: TODO
while (!feof($handle)) {
  $read = fread($handle, 2096);
  #error_log($read, 3, "/tmp/errors.txt");
}
pclose($handle);

function ReadHostNameFromFile ($fileName)
{
  $fp = fopen($fileName, "r");
  $retval = fread($fp, filesize($fileName));
  $retval = explode(' ', $retval);
  fclose($fp);
  return $retval[0];
}

// Read the output from the node-suggestor

$GatewaySuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/GatewaySuggest.out");
$NameNodeSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/NameNodeSuggest.out");
$JobTrackerSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/JobTrackerSuggest.out");
$SecondaryNameNodeSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/SecondaryNameNodeSuggest.out");
$HBaseMasterSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/HBaseMasterSuggest.out");
$HCatalogServerSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/HCatalogServerSuggest.out");
$OozieServerSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/OozieServerSuggest.out");
$TempletonNodeSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/TempletonNodeSuggest.out");
$GangliaCollectorSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/GangliaCollectorSuggest.out");
$NagiosServerSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/NagiosServerSuggest.out");
/// TODO: FIXME: Why is monitoring dashboard different from management console
$DashboardSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/DashboardSuggest.out");
$AllHosts = ReadNthWordsOfAllLinesFromFile($suggestNodesScriptOutputDir."/HostDiscovery.out", 1);

/// TODO: Move to next phase
$NameNodeMountPointsSuggestOut = ReadHostNameFromFile($suggestNodesScriptOutputDir."/NameNodeMountPointsSuggest.out");

$jsonOutput = array(
            "clusterName" => $clusterName,
            "services" => array(
             "$MapredName" =>   array (
                 "name" => $MapredName,
                 "enabled" => true,
                 "displayName" => "Mapreduce",
                 "masters" => array(
                                     $JobTrackerName => array (
                                       "name" => $JobTrackerName,
                                       "displayName" => "JobTracker",
                                       "hostName" => $JobTrackerSuggestOut,
                                       ),
                                  ),
                 "properties" => array(
                                       "EnableLzo" => false,
                                      ),
                ),
             "$HDFSName" =>  array (
                 "name" => $HDFSName,
                 "enabled" => true,
                 "displayName" => "HDFS",
                 "masters" => array(
                                     $NameNodeName => array (
                                       "name" => $NameNodeName,
                                       "displayName" => "NameNode",
                                       "hostName" => $NameNodeSuggestOut,
                                       ),
                                     $SecondaryNameNodeName => array (
                                       "name" => $SecondaryNameNodeName,
                                       "displayName" => "Secondary NameNode",
                                       "hostName" => $SecondaryNameNodeSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
             "$PigName" => array (
                 "name" => $PigName,
                 "enabled" => $requestObj["InstallPig"],
                 "displayName" => "Pig",
                 "masters" => array(),
                 "properties" => array(),
                ),
             "$HBaseName" => array (
                 "name" => $HBaseName,
                 "enabled" => $requestObj["InstallHBase"],
                 "displayName" => "HBase",
                 "masters" => array(
                                     $HBaseMasterName => array (
                                       "name" => $HBaseMasterName,
                                       "displayName" => "HBase Master",
                                       "hostName" => $HBaseMasterSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
              "$OozieName" => array (
                 "name" => $OozieName,
                 "enabled" => $requestObj["InstallOozie"],
                 "displayName" => "Oozie",
                 "masters" => array(
                                     $OozieServerName => array (
                                       "name" => $OozieServerName,
                                       "displayName" => "Oozie Server",
                                       "hostName" => $OozieServerSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
              "$HCatName" => array (
                 "name" => $HCatName,
                 "enabled" => $requestObj["InstallHCatalog"],
                 "displayName" => "HCatalog",
                 "masters" => array(
                                     $HCatServerName => array (
                                       "name" => $HCatServerName,
                                       "displayName" => "HCatalog Server",
                                       "hostName" => $HCatalogServerSuggestOut,
                                       ),

                                     $MySqlServerName => array (
                                       "name" => $MySqlServerName,
                                       "displayName" => "MySql Server",
                                       "hostname" => "",
                                       "dbname" => "",
                                       "dbuser" => "",
                                       "dbpasswd" => "",
                                       ),
                                  ),
                 "properties" => array(),
                ),
              "$TempletonName" => array (
                 "name" => $TempletonName,
                 "enabled" => $requestObj["InstallTempleton"],
                 "displayName" => "Templeton",
                 "masters" => array(
                                     $TempletonNodeName => array (
                                       "name" => $TempletonNodeName,
                                       "displayName" => "Templeton Node",
                                       "hostName" => $TempletonNodeSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
            "$SqoopName" =>  array (
                 "name" => $SqoopName,
                 "enabled" => $requestObj["InstallSqoop"],
                 "displayName" => "Sqoop",
                 "masters" => array(),
                 "properties" => array(),
                ),
             "$GatewayName" => array (
                 "name" => $GatewayName,
                 "enabled" => true,
                 "displayName" => "Gateway",
                 "masters" => array(),
                 "properties" => array(),
                ),
             "$ZooKeeperName" => array (
                 "name" => $ZooKeeperName,
                 "enabled" => $requestObj["InstallHBase"], // TODO: FIXME dependencies
                 "displayName" => "ZooKeeper",
                 "masters" => array(),
                 "properties" => array(),
                ),
             "$GangliaCollectorName" => array (
                 "name" => $GangliaCollectorName,
                 "enabled" => true,
                 "displayName" => "Ganglia Collector",
                 "masters" => array(
                                     $GangliaCollectorName => array (
                                       "name" => $GangliaCollectorName,
                                       "displayName" => "Ganglia Collector",
                                       "hostName" => $GangliaCollectorSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
             "$NagiosName" => array (
                 "name" => $NagiosName,
                 "enabled" => true,
                 "displayName" => "Nagios",
                 "masters" => array(
                                     $NagiosServerName => array (
                                       "name" => $NagiosServerName,
                                       "displayName" => "Nagios Server",
                                       "hostName" => $NagiosServerSuggestOut,
                                       ),
                                  ),
                 "properties" => array(),
                ),
            ),
            "allHosts"=> $AllHosts,
            "hosts" => array(),
            "sysInfo" => array(
               "mountPoints" => array(),
              ),
            "installInfo" => array (
               "deployUser" => $requestObj['ClusterDeployUser']
              ),
           );

/*
/////////////////// Persist the services list to the DB
$services = array();
if($requestObj["InstallHBase"] == "true") {
  array_push($services, $HBaseName);
}
if($requestObj["InstallPig"] == "true") {
  array_push($services, $PigName);
}
if($requestObj["InstallHCatalog"] == "true") {
  array_push($services, $HCatName);
}
if($requestObj["InstallOozie"] == "true") {
  array_push($services, $OozieName);
}
if($requestObj["InstallTempleton"] == "true") {
  array_push($services, $TempletonName);
}
if($requestObj["InstallSqoop"] == "true") {
  array_push($services, $SqoopName);
}
storeSelectedServiceNames($services);
/////////////////// End of persisting the services list
*/

/// Store cluster selection info
persistClusterInfo($clusterName, $jsonOutput);

header("Content-type: application/json");

// re-work svc array for UI
$svc_array = $jsonOutput["services"];
$jsonOutput["services"] = array();

foreach ($svc_array as $key=>$arr) {
  $svc_arr = $arr;
  $svc_arr["masters"] = array();
  foreach ($arr["masters"] as $role=>$role_arr) {
    array_push($svc_arr["masters"], $role_arr);
  }
  array_push($jsonOutput["services"], $svc_arr);
}

print (json_encode($jsonOutput));
?>
