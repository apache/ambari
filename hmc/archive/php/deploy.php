<?php
include 'dbLib.php';
include 'utils.php';
include 'gsInstallerUtils.php';

$clusterName = $_GET['clusterName'];
// $clusterName = "MyCluster-12345";
// Validate clusterName: TODO; FIXME
//$clusterDir = getClusterDir($clusterName);
$clusterInfo = getPersistedClusterInfo($clusterName);

/*
function generateDirs($allMountPts, $suffix) {
  $result = "";
  $first = true;
  foreach ($allMountPts as $mountPt) {
    if($first) {
      $first = false;
    } else {
      $result = $result.",";
    }
    $result = $result.$mountPt."/".$suffix;
  }
  return $result;
}
*/

//////////////////////////////// Write out the gsInstaller.properties file
// Read the template into memory
$templatedData = file_get_contents('/var/www/html/KickAssHDPUI/archive/ShellScripts/templates/gsInstaller.properties.in');
// Templated variables
$allMountPoints = $clusterInfo["sysInfo"]["mountPoints"];
$services = $clusterInfo["services"];
$mySqlBase = $services[$HCatName]["properties"];
// TODO: Declare the dirs suffixes as constants
$replacements = array(
                     #"@HDPDeployUser@" => $clusterInfo["installInfo"]["deployUser"],
                     "@HDPDeployUser@" => "root",
                     "@HDPSshKey@" => getClusterDir($clusterName)."/"."user-ssh-key", // TODO: Make the name constant
                     "@HDPEnableLzo@" => ( $services[$MiscName]["properties"]["enableLZO"]["value"] ? "yes" : "no" ),
                     "@HDPInstallPig@" => ( $services[$PigName]["enabled"] ? "yes" : "no" ),
                     "@HDPInstallHBase@" => ( $services[$HBaseName]["enabled"] ? "yes" : "no" ),
                     "@HDPInstallHCatalog@" => ( $services[$HCatName]["enabled"] ? "yes" : "no" ), 
                     "@HDPInstallTempleton@" => ( $services[$TempletonName]["enabled"] ? "yes" : "no" ),
                     "@HDPInstallSqoop@" => ( $services[$SqoopName]["enabled"] ? "yes" : "no" ),
                     "@HDPInstallOozie@" => ( $services[$OozieName]["enabled"] ? "yes" : "no" ), 
                     "@HDPDataNodeDataDir@" => $services[$HDFSName]["properties"]["dataNodeDataDir"]["value"],
                     "@HDPNameNodeDataDir@" => $services[$HDFSName]["properties"]["nameNodeDataDir"]["value"],
                     "@HDPMapReduceLocalDir@" => $services[$MapredName]["properties"]["mapReduceLocalDir"]["value"],
                     "@HDPZookeeperDataDir@" => $services[$ZooKeeperName]["properties"]["zooKeeperDataDir"]["value"],
                     "@HDPOozieDbDir@" => $services[$OozieName]["properties"]["oozieDbDir"]["value"],
                     "@HDPHCatalogMySqlDbHost@" => ( isset($mySqlBase["mysqlHost"]["value"]) ? $mySqlBase["mysqlHost"]["value"] : "" ),
                     "@HDPHCatalogMySqlDbName@" => ( isset($mySqlBase["mysqlDB"]["value"]) ? $mySqlBase["mysqlDB"]["value"] : "" ),
                     "@HDPHCatalogMySqlDbUser@" => ( isset($mySqlBase["mysqlUser"]["value"]) ? $mySqlBase["mysqlUser"]["value"] : "" ),
                     "@HDPHCatalogMySqlDbPassword@" => ( isset($mySqlBase["mysqlPasswd"]["value"]) ? $mySqlBase["mysqlPasswd"]["value"] : "" ),
                     "@HDPGSInstallLogFile@" => getGsInstallerLogPath($clusterName),
                     "@HDPGSInstallStatusDumpFile@" => getGsInstallerStatusDumpPath($clusterName)
                     );
// Replace all the tags with correspond values
$finalGsInstallerPropsContents = str_replace(array_keys($replacements), array_values($replacements), $templatedData);
// Write out the completed file into gsInstaller-input-dir
$finalGsInstallerPropsFile = getInstallerInputDir($clusterName)."gsInstaller.properties";
file_put_contents($finalGsInstallerPropsFile, $finalGsInstallerPropsContents);
//////////////////////////////// End of writing out the gsInstaller.properties file

//////////////////////////////// Write out the gsCluster.properties file

/* Dead code 
// Templated variables
// All props to be replace eventually. TODO:
#$varsToBeReplaced = array("@HDPHadoopHeapSize@", "@HDPNameNodeHeapSize@", "@HDPFSInMemorySize@", "@HDPNameNodeOptNewSize@", "@HDPDataNodeDuReserved@", "@HDPDataNodeHeapSize@", "@HDPJobTrackerOptNewSize@", "@HDPJobTrackerOptMaxNewSize@", "@HDPJobTrackerHeapSize@", "@HDPMapRedMapTasksMax@", "@HDPMapRedReduceTasksMax@", "@HDPMapRedClusterMapMemoryMB@", "@HDPMapRedClusterReduceMemoryMB@", "@HDPMapRedClusterMaxMapMemoryMB@", "@HDPMapRedClusterMaxReduceMemoryMB@", "@HDPMapRedJobMapMemoryMB@", "@HDPMapRedJobReduceMemoryMB@", "@HDPMapRedChildJavaOptsSize@", "@HDPIoSortMB@", "@HDPIoSortSpillPercent@", "@HDPMapReduceUserLogRetainHours@", "@HDPMaxTasksPerJob@", "@HDPDFSDataNodeFailedVolumeTolerated@", "@HDPHBaseMasterHeapSize@", "@HDPHBaseRegionServerHeapSize@");
#$varsToBeReplaced = array( "@HDPNameNodeHeapSize@", "@HDPJobTrackerHeapSize@", "@HDPHBaseMasterHeapSize@", "@HDPDataNodeHeapSize@", "@HDPHadoopHeapSize@", "@HDPMapRedChildJavaOptsSize@" );
$valuesToReplaceWith = array($memoryOptionsSuggestions["HDPNameNodeHeapSize"], $memoryOptionsSuggestions["HDPJobTrackerHeapSize"], $memoryOptionsSuggestions["HDPHBaseMasterHeapSize"], $memoryOptionsSuggestions["HDPDataNodeHeapSize"], $memoryOptionsSuggestions["HDPHadoopHeapSize"], $memoryOptionsSuggestions["HDPMapRedChildJavaOptsSize"]);
// Replace all the tags with correspond values
$finalGsClusterPropsContents = str_replace($varsToBeReplaced, $valuesToReplaceWith, $templatedData);
End dead code */

// Read the template into memory
$templatedData = file_get_contents('/var/www/html/KickAssHDPUI/ShellScripts/templates/gsCluster.properties.in');
// get mem suggestions 
$memoryOptionsSuggestions = $clusterInfo["sysInfo"]["memoryOptionsSuggestions"];

$gsClusterProps = array();
$gsClusterProps["@HDPNameNodeHeapSize@"] = $memoryOptionsSuggestions["HDPNameNodeHeapSize"];
$gsClusterProps["@HDPJobTrackerHeapSize@"] = $memoryOptionsSuggestions["HDPJobTrackerHeapSize"];
$gsClusterProps["@HDPHBaseMasterHeapSize@"] = $memoryOptionsSuggestions["HDPHBaseMasterHeapSize"];
$gsClusterProps["@HDPDataNodeHeapSize@"] = $memoryOptionsSuggestions["HDPDataNodeHeapSize"];
$gsClusterProps["@HDPHadoopHeapSize@"] = $memoryOptionsSuggestions["HDPHadoopHeapSize"];
$gsClusterProps["@HDPMapRedChildJavaOptsSize@"] = $memoryOptionsSuggestions["HDPMapRedChildJavaOptsSize"];
$gsClusterProps["@HDPHBaseRegionServerHeapSize@"] = "1024m";
$gsClusterProps["@HDPFSInMemorySize@"] = "256";
$gsClusterProps["@HDPNameNodeOptNewSize@"] = "640m";
$gsClusterProps["@HDPDataNodeDuReserved@"] = "1073741824";
$gsClusterProps["@HDPJobTrackerOptNewSize@"] = "200m";
$gsClusterProps["@HDPJobTrackerOptMaxNewSize@"] = "200m";
$gsClusterProps["@HDPMapRedMapTasksMax@"] = "4";
$gsClusterProps["@HDPMapRedReduceTasksMax@"] = "2";
$gsClusterProps["@HDPMapRedClusterMapMemoryMB@"] = "-1";
$gsClusterProps["@HDPMapRedClusterReduceMemoryMB@"] = "-1";
$gsClusterProps["@HDPMapRedClusterMaxMapMemoryMB@"] = "-1";
$gsClusterProps["@HDPMapRedClusterMaxReduceMemoryMB@"] = "-1";
$gsClusterProps["@HDPMapRedJobMapMemoryMB@"] = "-1";
$gsClusterProps["@HDPMapRedJobReduceMemoryMB@"] = "-1";
$gsClusterProps["@HDPIoSortMB@"] = "200m";
$gsClusterProps["@HDPIoSortSpillPercent@"] = "0.9";
$gsClusterProps["@HDPMapReduceUserLogRetainHours@"] = "24";
$gsClusterProps["@HDPMaxTasksPerJob@"] = "-1";
$gsClusterProps["@HDPDFSDataNodeFailedVolumeTolerated@"] = "0";
 
$finalGsClusterPropsContents = str_replace(array_keys($gsClusterProps),
    array_values($gsClusterProps), $templatedData);

// Write out the completed file into gsInstaller-input-dir
$finalGsClusterPropsFile = getInstallerInputDir($clusterName)."gsCluster.properties";
file_put_contents($finalGsClusterPropsFile, $finalGsClusterPropsContents);
//////////////////////////////// End of writing out the gsCluster.properties file

// Copy monInstaller as is for now until we add templating support
$copyDir = getInstallerInputDir($clusterName);
system("cp -f /var/www/html/KickAssHDPUI/ShellScripts/templates/monInstaller.properties.in $copyDir/monInstaller.properties");

header("Content-type: text/plain");
print "";
setupGsInstaller($clusterName);
copyHostRoleFilesToRunDir($clusterName);
setInstallStarted($clusterName);
triggerGsInstaller($clusterName);


?>
