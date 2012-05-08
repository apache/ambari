<?php

include "hostsConfig.php";
include "nodeManifest.php";
include "RoleDependencies.php";

 class ManifestGenerator {
   public static function generateManifest($dir, $hostInfo,
       $configInfo, $hostRoles, $hostAttributes, $modulesDir) {
     $sitePPFile = $dir . "/site.pp";
     //Delete file if exists
     if (file_exists($sitePPFile)) {
       unlink($sitePPFile);
     }
     $fh = fopen($dir . "/site.pp", "w");

     $importString = ManifestGenerator::getAllImports($modulesDir);
     fwrite($fh, $importString);

     $hostsConfig = HostsConfigManifest::getHostsConfigManifest($hostInfo,
         $configInfo);
     fwrite($fh, $hostsConfig . "\n");

     //Write internal to external mapping if exists
     $hostsAttributeManifest = HostsConfigManifest::getHostsAttributesManifest(
         $hostAttributes);
     fwrite($fh, $hostsAttributeManifest . "\n");

     $roleDepObj = new RoleDependencies();
     foreach ($hostRoles as $n => $rolesStates) {
       $nm = new NodeManifest($n);
       $roleList = array_keys($rolesStates);
       $rolesStatesOrig = $rolesStates;
       foreach($rolesStatesOrig as $role => $states) {
            //Add host level package dependencies
         $serviceState = SERVICE_STATE_NOT_APPLICABLE;
         if (isset($states[SERVICE_STATE_KEY])) {
            $serviceState = $states[SERVICE_STATE_KEY];
         }
         $hostLevelDependencies = $roleDepObj->getHostLevelDependencies($role,
                 $serviceState);
         foreach ($hostLevelDependencies as $depRole => $rstates) {
           $roleList[] = $depRole;
           foreach ($rstates as $k => $v) {
             //Assuming no conflicting states
             $rolesStates[$depRole][$k] = $v;
           }
         }
       }
       $roleList = array_unique($roleList);

       //Determine stages for each role
       $roleStages = $roleDepObj->getRolesStages($roleList);
       asort($roleStages, SORT_NUMERIC);
       foreach ($roleStages as $r => $s) {
         $nm->setRoleStage($r, $s);
         if (isset($rolesStates[$r])) {
           foreach($rolesStates[$r] as $stateName => $stateVal) {
             $nm->setRoleState($r, $stateName, $stateVal);
           }
         }
       }
       $nodeManifestString = $nm->generateNodeManifest();
       fwrite($fh, $nodeManifestString . "\n");
     }
     fclose($fh);
   }

   private static function getAllImports($modulesDir) {
     $importString = "";
     $importString = $importString . "import \"" . $modulesDir . "/hdp/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hadoop/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hbase/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-zookeeper/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-oozie/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-pig/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-sqoop/manifests/*.pp" ."\"\n";
     #$importString = $importString . "import \"" . $modulesDir . "/hdp-hcat/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-templeton/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hive/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hcat/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-mysql/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/mysql/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-monitor-webserver/manifests/*.pp" ."\"\n";
     return $importString;
   }
 }

?>
