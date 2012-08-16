<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


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
         foreach ($p as $apack) {
           if (!in_array($p, $packageList)) {
             $packageList[] = $apack;
           }
         }
       }
     }

     if (empty($packageList)) {
       //No packages don't bother
       return;
     }

     //lzo and snappy
     $snappyPackages = self::$rolesToPackageMap["snappy"];
     foreach ($snappyPackages as $spack) {
       if (!in_array($spack, $packageList)) {
         $packageList[] = $spack;
       }
     }
     if ($configInfo["lzo_enabled"] == "true") {
       $p = self::$rolesToPackageMap["lzo"];
       foreach ($p as $apack) {
         if (!in_array($p, $packageList)) {
           $packageList[] = $apack;
         }
       }
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
       "hdp-hadoop::namenode" => array ("hadoop", "hadoop-libhdfs.x86_64", "hadoop-native.x86_64", "hadoop-pipes.x86_64",
            "hadoop-sbin.x86_64", "hadoop-lzo"),
       "hdp-hadoop::snamenode" => array ("hadoop", "hadoop-libhdfs.x86_64", "hadoop-native.x86_64", "hadoop-pipes.x86_64",
            "hadoop-sbin.x86_64", "hadoop-lzo"),
       "hdp-hadoop::jobtracker" => array ("hadoop", "hadoop-libhdfs.x86_64", "hadoop-native.x86_64", "hadoop-pipes.x86_64",
            "hadoop-sbin.x86_64", "hadoop-lzo"),
       "hdp-hadoop::client" => array ("hadoop", "hadoop-libhdfs", "hadoop-native",
            "hadoop-pipes", "hadoop-sbin", "hadoop-lzo"),
       "hdp-hadoop::client" => array ("hadoop", "hadoop-libhdfs", "hadoop-native",
            "hadoop-pipes", "hadoop-sbin", "hadoop-lzo"),
       "hdp-hadoop::datanode" => array ("hadoop", "hadoop-libhdfs", "hadoop-native",
            "hadoop-pipes", "hadoop-sbin", "hadoop-lzo"),
       "hdp-hadoop::tasktracker" => array ("hadoop", "hadoop-libhdfs", "hadoop-native",
            "hadoop-pipes", "hadoop-sbin", "hadoop-lzo"),
       "hdp-zookeeper" => array ("zookeeper"),
       "hdp-zookeeper::client" => array ("zookeeper"),
       "hdp-hbase::master" => array ("hbase"),
       "hdp-hbase::regionserver" => array("hbase"),
       "hdp-hbase::client" => array("hbase"),
       "hdp-pig" => array("pig.noarch"),
       "hdp-sqoop" => array("sqoop", "mysql-connector-java"),
       "hdp-hive::server" => array("hive", "mysql-connector-java"),
       "hdp-hive::client" => array("hive"),
       "hdp-hcat" => array("hcatalog"),
       "hdp-oozie::server" => array("oozie.noarch", "extjs-2.2-1"),
       "hdp-oozie::client" => array("oozie-client.noarch"),
       "hdp-mysql::server" => array("mysql-server"),
       "hdp-templeton::server" => array("templeton", "templeton-tar-pig-0.0.1.14-1", "templeton-tar-hive-0.0.1.14-1"),
       "hdp-templeton::client" => array("templeton"),
       "lzo" => array("lzo", "lzo", "lzo-devel", "lzo-devel"),
       "snappy" => array("snappy", "snappy-devel"),
       "hdp-ganglia::server" => array("ganglia-gmetad-3.2.0", "ganglia-gmond-3.2.0", "gweb", "hdp_mon_ganglia_addons"),
       "hdp-ganglia::monitor" => array("ganglia-gmond-3.2.0", "gweb", "hdp_mon_ganglia_addons"),
       "hdp-nagios::server" => array("hdp_mon_nagios_addons", "nagios-3.2.3", "nagios-plugins-1.4.9", "fping", "net-snmp-utils"),
       "hdp-dashboard" => array("hdp_mon_dashboard"),
   );
 }

?>
