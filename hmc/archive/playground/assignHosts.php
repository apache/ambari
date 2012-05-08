<?php

include 'dbLib.php';

$requestdata = file_get_contents('php://input');
// $obj = json_decode($requestdata, true);
$obj = array(
         "clusterName"=> "MyCluster-12345",
         "ClusterDeployUser"=> "hortonva",
         "ClusterDeployUserIdentityFile"=> "va_rsa",
         "ClusterHostsFile"=> "../hostsFile.txt",
         "InstallHBase"=> "false",
         "InstallPig"=> "false",
         "InstallHCatalog"=> "false",
         "InstallTempleton"=> "false",
         "InstallOozie"=> "false",
         "InstallSqoop"=> "false",
         "InstallLzo"=> "false",
         );


$clusterName = $obj['clusterName'];
$clusterDir = getClusterDir($clusterName);
$commitNodesScriptInputDir = $clusterDir."/commitNodesScriptInputDir/";
$commitNodesScriptOutputDir = $clusterDir."/commitNodesScriptOutputDir/";

// Hard code host and identity files for now
$obj['ClusterDeployUserIdentityFile']="va_rsa";
$obj['ClusterHostsFile']="hostsFile.txt";

// write the input file to the node-suggestor
$fp = fopen($commitNodesScriptInputDir."data.txt", 'w');
fwrite($fp, 'HDPClusterDeployUser='.$obj['ClusterDeployUser']."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.$commitNodesScriptInputDir.'/'.$obj['ClusterDeployUserIdentityFile']."\n");
fwrite($fp, 'HDPClusterHostsFile='.$commitNodesScriptInputDir.'/'.$obj['ClusterHostsFile']."\n");
fwrite($fp, 'HDPNameNodeHost='.$obj['nameNode']."\n");
fwrite($fp, 'HDPSecondaryNameNodeHost='.$obj['secondaryNameNode']."\n");
fwrite($fp, 'HDPJobTrackerHost='.$obj['jobTracker']."\n");
fwrite($fp, 'HDPHBaseMasterHost='.$obj['hbaseMaster']."\n");
fwrite($fp, 'HDPHCatalogServerHost='.$obj['hcatServer']."\n");
fwrite($fp, 'HDPGangliaCollectorHost='.$obj['gangliaCollector']."\n");
fwrite($fp, 'HDPNagiosServerHost='.$obj['nagiosServer']."\n");
fwrite($fp, 'HDPOozieServerHost='.$obj['oozieServer']."\n");
fwrite($fp, 'HDPTempletonNodeHost='.$obj['templetonNode']."\n");
fclose($fp);

$cmd = "/var/www/html/KickAssHDPUI/ShellScripts/generateNodeFiles.sh ".$commitNodesScriptInputDir."data.txt ".$commitNodesScriptOutputDir;
$handle = popen($cmd, "r");
// Do some checks on return status: TODO
//print $cmd;
//echo "'$handle'; " . gettype($handle) . "\n";
while (!feof($handle)) {
  $read = fread($handle, 2096);
  error_log($read, 3, "/tmp/errors.txt");
}
pclose($handle);

function ReadAllFirstWordOfLinesFromFile ($fileName)
{
  $handle = fopen($fileName, "r");
  $retValue = array();
  if ($handle) {
    while (($buffer = fgets($handle, 4096)) !== false) {
      $fullLine = explode(' ', $buffer);
      array_push($retValue, $fullLine[0]);
    }
     
    if (!feof($handle)) {
      echo "Error: unexpected fgets() fail\n"; // TODO: Fix
    }
  }
  return $retValue;
}

$AllMountPoints = ReadAllFirstWordOfLinesFromFile($commitNodesScriptOutputDir."/NameNodeMountPointsSuggest.out");

$outjson = array(
            "allMountPoints"=> $AllMountPoints,
           );

header("Content-type: application/json");
print (json_encode($outjson));
//var_dump($AllHosts);
//print "hello";
//var_dump(json_encode($outjson));
?>
