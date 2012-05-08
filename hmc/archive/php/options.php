<?php
include 'dbLib.php';
include 'utils.php';

// Read from the input
$requestdata = file_get_contents('php://input');
$obj = json_decode($requestdata, true);

$clusterName = $_GET['clusterName'];

// Store the final options that the user has submitted
error_log(json_encode($obj), 0);
storeOptions($clusterName, $obj);
$clusterDeployUser = getClusterDeployUser($clusterName);
$clusterDir = getClusterDir($clusterName);

// write the input file to the node-suggestor
$scriptInputDataFile = $clusterDir."memorySuggestor-data.txt";
$fp = fopen($scriptInputDataFile, 'w');
fwrite($fp, 'HDPClusterDeployUser='.$clusterDeployUser."\n");
fwrite($fp, 'HDPClusterDeployUserIdentityFile='.getSshKeyFilePath($clusterName)."\n");
fclose($fp);

$installerInputDir = getInstallerInputDir($clusterName);
$outputFile = $clusterDir."memorySuggestor-output.txt";
$cmd = "/var/www/html/KickAssHDPUI/archive/ShellScripts/suggestMemSize.sh ".$installerInputDir." ".$scriptInputDataFile." ".$outputFile." 0";
$handle = popen($cmd, "r");
// Do some checks on return status: TODO
//print $cmd;
//echo "'$handle'; " . gettype($handle) . "\n";
while (!feof($handle)) {
  $read = fread($handle, 2096);
  // echo $read;
}
pclose($handle);

$keys = ReadNthWordsOfAllLinesFromFile($outputFile, 1);
$values = ReadNthWordsOfAllLinesFromFile($outputFile, 2);

$memOptionSuggestions = array();
for ($i = 0; $i < sizeof($keys); $i++) {
  if (isset($values[$i])) {
    $memOptionSuggestions[$keys[$i]] = $values[$i];
  }
}
// Persist it now itself. TODO: Move this to next megaConfigure page.
storeMemoryOptionSuggestions($clusterName, $memOptionSuggestions);

//print (json_encode($memOptionSuggestions));

// Give the whole db
header("Content-type: application/json");
print (json_encode(getPersistedClusterInfo($clusterName)));
?>
