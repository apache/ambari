<?php

$requestdata = file_get_contents('php://input');
$obj = json_decode($requestdata, true);

$clusterName = $obj['clusterName'];
$clusterDir = "/var/run/HDPClusters/".$clusterName."/";
$commitNodesScriptInputDir = $clusterDir."/commitNodesScriptInputDir/";
$commitNodesScriptOutputDir = $clusterDir."/commitNodesScriptOutputDir/";

// Hard code host and identity files for now
$obj['ClusterDeployUserIdentityFile']="va_rsa";
$obj['ClusterHostsFile']="hostsFile.txt";

// write the input file to the node-suggestor
$fp = fopen($commitNodesScriptInputDir.'data.txt', 'w');
fwrite($fp, 'HDPClusterDeployUser='.$obj['ClusterDeployUser']."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.$commitNodesScriptInputDir.'/'.$obj['ClusterDeployUserIdentityFile']."\n");
fwrite($fp, 'HDPClusterHostsFile='.$commitNodesScriptInputDir.'/'.$obj['ClusterHostsFile']);
fwrite($fp, 'HDPNameNodeHost='.$obj['nameNode']);
fwrite($fp, 'HDPSecondaryNameNodeHost='.$obj['secondaryNameNode']);
fwrite($fp, 'HDPJobTrackerHost='.$obj['jobTracker']);
fwrite($fp, 'HDPHBaseMasterHost='.$obj['hbaseMaster']);
fwrite($fp, 'HDPHCatalogServerHost='.$obj['hcatServer']);
fwrite($fp, 'HDPGangliaCollectorHost='.$obj['gangliaCollector']);
fwrite($fp, 'HDPNagiosServerHost='.$obj['nagiosServer']);
fwrite($fp, 'HDPOozieServerHost='.$obj['oozieServer']);
fwrite($fp, 'HDPTempletonNodeHost='.$obj['templetonNode']);
fwrite($fp, 'HDPdd='.$obj['templetonNode']);
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
            "allMountPoints"=> $AllMountPoints
           );

header("Content-type: application/json");
print (json_encode($outjson));
//var_dump($AllHosts);
//print "hello";
//var_dump(json_encode($outjson));
?>
