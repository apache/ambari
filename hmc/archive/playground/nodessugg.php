<?php

// write the input file to the node-suggestor
$fp = fopen('data.txt', 'w');
fwrite($fp, 'HDPClusterDeployUser=root');
fwrite($fp, 'HDPClusterDeployUserIdentityFile=/var/sshkey');
fwrite($fp, 'HDPClusterHostsFile=/var/hosts');

$cmd = "../ShellScripts/suggestNodes.sh data.txt ./";
// invoke the node-suggstor
$handle = popen($cmd, "r");

fclose($fp);

function ReadFile ($fileName)
{
  $fp = fopen($filename, "r");
  $retval = fread($fp, filesize($filename));
  fclose($fp);
  return $retval;
}

// Read the output from the node-suggestor

$filename = "NameNodeSuggest.out";
$NameNodeSuggestOut = ReadFile($filename);

$filename = "NameNodeMountPointsSuggest.out";
$NameNodeMountPointsSuggestOut = ReadFile($filename);

$filename = "SecondaryNameNodeSuggest.out";
$SecondaryNameNodeSuggestOut = ReadFile($filename);

$filename = "HBaseMasterSuggest.out";
$HBaseMasterSuggestOut = ReadFile($filename);

$filename = "JobTrackerSuggest.out";
$JobTrackerSuggestOut = ReadFile($filename);

$filename = "GatewaySuggest.out";
$GatewaySuggestOut = ReadFile($filename);

$filename = "HCatalogServerSuggest.out";
$HCatalogServerSuggestOut = ReadFile($filename);

$filename = "GangliaCollectorSuggest.out";
$GangliaCollectorSuggestOut = ReadFile($filename);

$filename = "NagiosServerSuggest.out";
$NagiosServerSuggestOut = ReadFile($filename);

$filename = "DashboardSuggest.out";
$DashboardSuggestOut = ReadFile($filename);

$filename = "OozieServerSuggest.out";
$OozieServerSuggestOut = ReadFile($filename);

$filename = "TempletonNodeSuggest.out";
$TempletonNodeSuggestOut = ReadFile($filename);
?>
