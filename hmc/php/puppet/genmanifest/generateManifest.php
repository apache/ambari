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
           //The state could be conflicting, pick the appropriate state
           //e.g. a dependency shows up with two different service states
           ManifestGenerator::resolveDepRoleStates($depRole, $rstates, $rolesStates);
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
       ManifestGenerator::optimizePackageInstall($n, $nm, $rolesStates, $configInfo, $roleStages);
       $nodeManifestString = $nm->generateNodeManifest();
       fwrite($fh, $nodeManifestString . "\n");
     }
     fclose($fh);
   }

   private static function priorityState($u, $v) {
     $order = array (
       SERVICE_STATE_NOT_APPLICABLE => 0,
       SERVICE_STATE_UNINSTALLED => 1,
       SERVICE_STATE_STOPPED => 2,
       SERVICE_STATE_NO_OP => 3,
       SERVICE_STATE_INSTALLED_AND_CONFIGURED => 4,
       SERVICE_STATE_RUNNING => 5
     );

     if (!isset($v)) {
       return $u;
     } else if (!isset($u)) {
       return $v;
     } else if ($order[$u] > $order[$v]) {
       return $u;
     } else {
       return $v;
     }
   }

   private static function resolveDepRoleStates($depRole, $rstates, &$rolesStates) {
     foreach ($rstates as $k => $v) {
       if ($k == SERVICE_STATE_KEY) {
         $alreadySetSvcState = NULL;
         if (isset($rolesStates[$depRole][$k])) {
           $alreadySetSvcState = $rolesStates[$depRole][$k]; 
         }
         $rolesStates[$depRole][$k] = 
             ManifestGenerator::priorityState($v, $alreadySetSvcState);
       } else {
         //Assume no conflicts for this key
         $rolesStates[$depRole][$k] = $v;
       }
     }
   }

   private static function optimizePackageInstall($node, &$manifestObj, $rolesStatesDs, 
       $configInfo, $roleStages) {
     //Figure out the state
     $serviceState = SERVICE_STATE_INSTALLED_AND_CONFIGURED;
     foreach ($rolesStatesDs as $r => $stateList) {
       if ((isset($stateList[SERVICE_STATE_KEY])) &&
           ($stateList[SERVICE_STATE_KEY] != SERVICE_STATE_NO_OP )) {
         if ($stateList[SERVICE_STATE_KEY] != SERVICE_STATE_INSTALLED_AND_CONFIGURED) {
           $serviceState = NULL;
           break;
         }
       }
     } 
     if (!isset($serviceState)) {
       $serviceState = SERVICE_STATE_UNINSTALLED;
       //See if it is uninstalled
       foreach ($rolesStatesDs as $r => $stateList) {
         if ( (isset($stateList[SERVICE_STATE_KEY])) && 
           ($stateList[SERVICE_STATE_KEY] != SERVICE_STATE_NO_OP )) {
           if ($stateList[SERVICE_STATE_KEY] != SERVICE_STATE_UNINSTALLED) {
             $serviceState = NULL;
             break;
           }
         }
       } 
     }
     if (!isset($serviceState)) {
       //No optimization needed
       return;
     }
     //get list of packages
     $stages = array();
     foreach($roleStages as $roleName => $val) {
        $stages[$val] = $roleName;
     }
     ksort($stages, SORT_NUMERIC);
     $packageList = array();
     foreach($stages as $theStage => $r) {
       if (!isset($rolesStatesDs[$r])) {
         continue;
       }
       //Add in the order of the stages
       $stateList = $rolesStatesDs[$r];
       if ($stateList[SERVICE_STATE_KEY] != $serviceState) { 
         continue;
       }
       if (isset(self::$rolesToPackageMap[$r])) {
         $p = self::$rolesToPackageMap[$r];
         if (!in_array($p, $packageList)) {
           $packageList[] = $p;
         }
       }
     }

     if (empty($packageList)) {
       //No packages don't bother
       return;       
     }

     //lzo and snappy 
     $packageList[] = self::$rolesToPackageMap["snappy"];
     if ($configInfo["lzo_enabled"] == "true") {
       $packageList[] = self::$rolesToPackageMap["lzo"];
     }

     $firstP = true;
     $pList = "\"";
     foreach ($packageList as $p) {
       if ($firstP) {
         $firstP = false;
       } else {
         $pList = $pList . " ";
       }
       $pList = $pList . $p;
     }
     $pList = $pList . "\"";
     $manifestObj->setRoleState("hdp", SERVICE_STATE_KEY, $serviceState);
     $manifestObj->setRoleState("hdp", "pre_installed_pkgs", $pList);
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
     $importString = $importString . "import \"" . $modulesDir . "/hdp-templeton/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hive/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-hcat/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-mysql/manifests/*.pp" ."\"\n";
     $importString = $importString . "import \"" . $modulesDir . "/hdp-monitor-webserver/manifests/*.pp" ."\"\n";
     return $importString;
   }

   private static $rolesToPackageMap = array (
     "hdp-hadoop::namenode" => "hadoop hadoop-libhdfs.x86_64 hadoop-native.x86_64 hadoop-pipes.x86_64 hadoop-sbin.x86_64 hadoop-lzo",
     "hdp-hadoop::snamenode" => "hadoop hadoop-libhdfs.x86_64 hadoop-native.x86_64 hadoop-pipes.x86_64 hadoop-sbin.x86_64 hadoop-lzo",
     "hdp-hadoop::jobtracker" => "hadoop hadoop-libhdfs.x86_64 hadoop-native.x86_64 hadoop-pipes.x86_64 hadoop-sbin.x86_64 hadoop-lzo",
     "hdp-hadoop::client" => "hadoop hadoop-libhdfs.i386 hadoop-native.i386 hadoop-pipes.i386 hadoop-sbin.i386 hadoop-lzo",
     "hdp-hadoop::datanode" => "hadoop hadoop-libhdfs.i386 hadoop-native.i386 hadoop-pipes.i386 hadoop-sbin.i386 hadoop-lzo",
     "hdp-hadoop::tasktracker" => "hadoop hadoop-libhdfs.i386 hadoop-native.i386 hadoop-pipes.i386 hadoop-sbin.i386 hadoop-lzo",
     "hdp-zookeeper" => "zookeeper",
     "hdp-zookeeper::client" => "zookeeper",
     "hdp-hbase::master" => "hbase",
     "hdp-hbase::regionserver" => "hbase",
     "hdp-hbase::client" => "hbase",
     "hdp-pig" => "pig.noarch",
     "hdp-sqoop" => "sqoop mysql-connector-java-5.0.8-1",
     "hdp-hive::server" => "hive mysql-connector-java-5.0.8-1",
     "hdp-hive::client" => "hive",
     "hdp-hcat" => "hcatalog",
     "hdp-oozie::server" => "oozie.noarch extjs-2.2-1",
     "hdp-oozie::client" => "oozie-client.noarch",
     "hdp-mysql::server" => "mysql-server",
     "hdp-templeton::server" => "templeton templeton-tar-pig-0.0.1-1 templeton-tar-hive-0.0.1-1",
     "hdp-templeton::client" => "templeton",
     "lzo" => "lzo lzo.i386 lzo-devel lzo-devel.i386",
     "snappy" => "snappy snappy-devel",
     "hdp-ganglia::server" => "ganglia-gmetad-3.2.0 ganglia-gmond-3.2.0 gweb hdp_mon_ganglia_addons",
     "hdp-ganglia::monitor" => "ganglia-gmond-3.2.0 gweb hdp_mon_ganglia_addons",
     "hdp-nagios::server" => "hdp_mon_nagios_addons nagios-3.2.3 nagios-plugins-1.4.9 fping net-snmp-utils",
     "hdp-dashboard" => "hdp_mon_dashboard",
   );
 }

?>
