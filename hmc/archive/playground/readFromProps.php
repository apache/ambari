<?php

$inputBaseDir = "/var/www/html/KickAssHDPUI/php/playground/input";
$outputBaseDir = "/var/www/html/KickAssHDPUI/php/playground/output";

// Read from the input
/*
$uploadDir = '/var/www/uploads/';
$identifyFile = $uploadDir . basename($_FILES['ClusterDeployUserIdentityFile']['name']);
$hostsFile = $uploadDir . basename($_FILES['ClusterHostsFile']['name']);
echo '<pre>';
if (move_uploaded_file($_FILES['ClusterDeployUserIdentityFile']['tmp_name'], $identifyFile)) {
    echo "File is valid, and was successfully uploaded.\n";
} else {
    echo "Possible file upload attack!\n";
}

echo 'Here is some more debugging info:';
print_r($_FILES);

print "</pre>";
*/

$json = '{
         "ClusterDeployUser": "hortonva",
         "ClusterDeployUserIdentityFile": "va_rsa",
         "ClusterHostsFile": "hostsFile.txt",
         "InstallHBase": "false",
         "InstallPig": "false",
         "InstallHCatalog": "false",
         "InstallTempleton": "false",
         "InstallOozie": "false",
         "InstallSqoop": "false",
         "InstallLzo": "false"
         }';
/*
$json = '{"foo-bar": 12345,
        "one-more":"4567"
        }';
*/
$obj = json_decode($json, true);
//var_dump($obj);
//print $obj['ClusterDeployUser']; // 12345
//print $obj->{'one-more'}

// write the input file to the node-suggestor
$fp = fopen('data.txt', 'w');
fwrite($fp, 'HDPClusterDeployUser='.$obj['ClusterDeployUser']."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.$inputBaseDir.'/'.$obj['ClusterDeployUserIdentityFile']."\n");
fwrite($fp, 'HDPClusterHostsFile='.$inputBaseDir.'/'.$obj['ClusterHostsFile']);
fclose($fp);

$cmd = "/var/www/html/KickAssHDPUI/ShellScripts/suggestNodes.sh data.txt ".$outputBaseDir;
//$cmd = "/bin/ls -lR /etc";
// invoke the node-suggstor
$handle = popen($cmd, "r");
// Do some checks on return status: TODO
print $cmd;
echo "'$handle'; " . gettype($handle) . "\n";
while (!feof($handle)) {
  $read = fread($handle, 2096);
  //echo $read;
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

function ReadAllHostNamesFromFile ($fileName)
{
  $handle = fopen($fileName, "r");
  $retValue = array();
  if ($handle) {
    while (($buffer = fgets($handle, 4096)) !== false) {
      $fullLine = explode(' ', $buffer);
      array_push($retValue, $fullLine[0]);
    }
     
    if (!feof($handle)) {
      echo "Error: unexpected fgets() fail\n";
    }
  }
  return $retValue;
}

// Read the output from the node-suggestor

$NameNodeSuggestOut = ReadHostNameFromFile($outputBaseDir."/NameNodeSuggest.out");
$NameNodeMountPointsSuggestOut = ReadHostNameFromFile($outputBaseDir."/NameNodeMountPointsSuggest.out");
$SecondaryNameNodeSuggestOut = ReadHostNameFromFile($outputBaseDir."/SecondaryNameNodeSuggest.out");
$HBaseMasterSuggestOut = ReadHostNameFromFile($outputBaseDir."/HBaseMasterSuggest.out");
$JobTrackerSuggestOut = ReadHostNameFromFile($outputBaseDir."/JobTrackerSuggest.out");
$GatewaySuggestOut = ReadHostNameFromFile($outputBaseDir."/GatewaySuggest.out");
$HCatalogServerSuggestOut = ReadHostNameFromFile($outputBaseDir."/HCatalogServerSuggest.out");
$GangliaCollectorSuggestOut = ReadHostNameFromFile($outputBaseDir."/GangliaCollectorSuggest.out");
$NagiosServerSuggestOut = ReadHostNameFromFile($outputBaseDir."/NagiosServerSuggest.out");
$DashboardSuggestOut = ReadHostNameFromFile($outputBaseDir."/DashboardSuggest.out");
$OozieServerSuggestOut = ReadHostNameFromFile($outputBaseDir."/OozieServerSuggest.out");
$TempletonNodeSuggestOut = ReadHostNameFromFile($outputBaseDir."/TempletonNodeSuggest.out");
$AllHosts = ReadAllHostNamesFromFile($outputBaseDir."/HostDiscovery.out");

$outjson = '{
            "namenode": '.$NameNodeSuggestOut.',
            "secondarynamenode": '.$SecondaryNameNodeSuggestOut.',
            "jobtracker": '.$JobTrackerSuggestOut.',
            "gangliacollector": '.$GangliaCollectorSuggestOut.',
            "nagiosserver": '.$NagiosServerSuggestOut.',
            "all": '.$AllHosts.'
           }';

//var_dump($AllHosts);
//print "hello";
var_dump(json_encode($outjson));
?>
