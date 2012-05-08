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
         );
*/


$clusterName = $_GET['clusterName'];
$clusterDir = getClusterDir($clusterName);

/*
$MapredName = "mapReduce";
$HDFSName = "hdfs";
$HBaseName = "hBase";
$PigName = "pig";
$HcatName = "hCatalog";
$TempletonName = "templeton";
$OozieName = "oozie";
$SqoopName = "sqoop";
$GatewayName = "gateway";


*/

/////////////// Persist final master-server names ///////////////////////////////
$serviceMasters = array();

$serviceMasters[$HDFSName] = array(
          $NameNodeName => $obj["nameNodeHost"],
          $SecondaryNameNodeName => $obj["secondaryNameNodeHost"]
  );
$serviceMasters[$MapredName] = array(
          $JobTrackerName => $obj["jobTrackerHost"],
          );
if(isServiceEnabled($clusterName, $HBaseName)) {
  $serviceMasters[$HBaseName] = array ( $HBaseMasterName => $obj["hBaseMasterHost"] );
}
if(isServiceEnabled($clusterName, $HCatName)) {
  $serviceMasters[$HCatName] = array ( $HCatServerName => $obj["hCatServerHost"] );
}
if(isServiceEnabled($clusterName, $OozieName)) {
  $serviceMasters[$OozieName] = array ( $OozieServerName => $obj["oozieServerHost"] );
}
if(isServiceEnabled($clusterName, $TempletonName)) {
  $serviceMasters[$TempletonName] = array ( $TempletonNodeName => $obj["templetonNodeHost"] );
}

storeServiceMasters($clusterName, $serviceMasters);
/////////////// End of persisting final master-server names /////////////////////

$inputDirForGetMountPointsScript = $clusterDir."/inputDirForGetMountPointsScript/";
$cmd="rm -rf $inputDirForGetMountPointsScript/*; mkdir -p $inputDirForGetMountPointsScript";
error_log("Running cmd : $cmd");
system("rm -rf $inputDirForGetMountPointsScript/*; mkdir -p $inputDirForGetMountPointsScript"); // TODO: Error codes
$outputDirForGetMountPointsScript = getInstallerInputDir($clusterName);

// Hard code host and identity files for now
//$obj['ClusterDeployUserIdentityFile']="va_rsa";
//$obj['ClusterHostsFile']="hostsFile.txt";

// write the input file to the mountPointsScript
$fp = fopen($inputDirForGetMountPointsScript."data.txt", 'w');
//TODO: FIXME hardcoded. Need to persist and recover.
//fwrite($fp, 'HDPClusterDeployUser='.$obj['ClusterDeployUser']."\n");
fwrite($fp, 'HDPClusterDeployUser='.getClusterDeployUser($clusterName)."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.getSshKeyFilePath($clusterName)."\n");
fwrite($fp, 'HDPClusterHostsFile='.getHostsFilePath($clusterName)."\n");
fwrite($fp, 'HDPNameNodeHost='.$obj['nameNodeHost']."\n");
fwrite($fp, 'HDPSecondaryNameNodeHost='.$obj['secondaryNameNodeHost']."\n");
fwrite($fp, 'HDPJobTrackerHost='.$obj['jobTrackerHost']."\n");
fwrite($fp, 'HDPGangliaCollectorHost='.$obj['gangliaCollectorHost']."\n");
fwrite($fp, 'HDPNagiosServerHost='.$obj['nagiosServerHost']."\n");
if(isServiceEnabled($clusterName, $HBaseName)) {
  fwrite($fp, 'HDPHBaseMasterHost='.$obj['hBaseMasterHost']."\n");
}
if(isServiceEnabled($clusterName, $HCatName)) {
  fwrite($fp, 'HDPHCatalogServerHost='.$obj['hCatServerHost']."\n");
}
if(isServiceEnabled($clusterName, $OozieName)) {
  fwrite($fp, 'HDPOozieServerHost='.$obj['oozieServerHost']."\n");
}
if(isServiceEnabled($clusterName, $TempletonName)) {
  fwrite($fp, 'HDPTempletonNodeHost='.$obj['templetonNodeHost']."\n");
}
fclose($fp);

$cmd = "/var/www/html/KickAssHDPUI/archive/ShellScripts/generateNodeFiles.sh ".$inputDirForGetMountPointsScript."data.txt ".$outputDirForGetMountPointsScript;
error_log("Running cmd: $cmd");
$handle = popen($cmd, "r");
// Do some checks on return status: TODO
//print $cmd;
//echo "'$handle'; " . gettype($handle) . "\n";
while (!feof($handle)) {
  $read = fread($handle, 2096);
}
pclose($handle);

$AllMountPoints = ReadNthWordsOfAllLinesFromFile($outputDirForGetMountPointsScript."/NameNodeMountPointsSuggest.out", 1);

/*
function generateDirs($allMountPts, $suffix) {
  $result = "";
  $first = true;
  foreach ($allMountPts as $mountPt) {
    if($first) {
      $first = false;
    } else {
      $result = ",".$result;
    }
    $result = $result.$mountPt."/".$suffix;
  }
  return $result;
}

function generateSingleDir($allMountPts, $suffix) {
  $result="";
  foreach ($allMountPts as $mountPt) {
    $result = $result.$mountPt."/".$suffix;
    return $result;
  }
}
*/

$outjson = array(
            "clusterName" => $clusterName,
            "mountPoints" => $AllMountPoints,
            "servicesInfo" => array (
                                array( "keyName" => "nameNodeDataDir", "displayName" => "NameNode Data Directories", "maxDirectoriesNeeded" => -1, "suffix" => "hadoop/hdfs/namenode"),
                                array( "keyName" => "dataNodeDataDir", "displayName" => "DataNode Data Directories", "maxDirectoriesNeeded" => -1, "suffix" => "hadoop/hdfs/data"),
                                array( "keyName" => "mapReduceLocalDir", "displayName" => "MapReduce Data Directories", "maxDirectoriesNeeded" => -1, "suffix" => "hadoop/mapred"),
                                array( "keyName" => "oozieDbDir", "displayName" => "Oozie DB Directory", "maxDirectoriesNeeded" => 1, "suffix" => "hadoop/oozie"),
                                array( "keyName" => "zooKeeperDataDir", "displayName" => "ZooKeeper Data Directory", "maxDirectoriesNeeded" => 1 , "suffix" => "hadoop/zookeeper"),
                                )
            );
/*            "nameNodeDataDir"=> generateDirs($AllMountPoints, "hadoop/hdfs/namenode"),
            "dataNodeDataDir"=> generateDirs($AllMountPoints, "hadoop/hdfs/data"),
            "mapreduceDataDir"=> generateDirs($AllMountPoints, "hadoop/mapred"),
            "oozieDbDir"=> generateSingleDir($AllMountPoints, "hadoop/oozie"),
            "zookeeperDataDir"=> generateSingleDir($AllMountPoints, "hadoop/zookeeper"),
           );
            */

header("Content-type: application/json");
print (json_encode($outjson));
//var_dump($AllHosts);
//print "hello";
//var_dump(json_encode($outjson));
?>
