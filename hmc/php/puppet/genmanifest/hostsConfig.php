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


class HostsConfigManifest {
   /**
    * The keys in the data structures are assumed to be already converted
    * to names required by puppet.
    */
   public static function getHostsConfigManifest($dbHostEntries,
      $dbConfigEntries) {
      $manifest = "";
      //Generate Hosts manifest
      foreach($dbHostEntries as $key => $hosts) {
        $hostList = "";
        if (is_array($hosts)) {
          $hostList = "[";
          $first = true;
          foreach ($hosts as $h) {
            if ($first) {
              $first = false;
            } else {
              $hostList = $hostList . ",";
            }
            $hostList = $hostList . "'" . $h . "'";
          }
          $hostList = $hostList . "]";
        } else {
          $hostList = "\"" . $hosts . "\"";
        }
        $manifest = $manifest . "$" . $key . " = " . $hostList . "\n";
      }
      $manifest = $manifest . "\n";
      foreach($dbConfigEntries as $key => $configEntry) {
        $manifest = $manifest .
            HostsConfigManifest::getConfigLine($key, $configEntry);
      }
      return $manifest;
   }

   private static function getPublicFqdnLine($hostName, $publicFqdn) {
      return "\"" . $hostName . "\"" . " => { " . "\"publicfqdn\" => " . "\"" . $publicFqdn . "\" }";
   }

   public static function getHostsAttributesManifest($hostAttributes) {
     $manifest = "";
     if (empty($hostAttributes["hosts"])) {
       return "";
     }
     $first = true;
     foreach ($hostAttributes["hosts"] as $hostDetails) {
       if ( (empty($hostDetails["hostName"])) ||
            (empty($hostDetails["attributes"])) ||
            (empty($hostDetails["attributes"]["publicFQDN"])) ) {
         continue;
       }
       $hostName = $hostDetails["hostName"];
       $publicFqdn = $hostDetails["attributes"]["publicFQDN"];
       if (!$first) {
         $manifest = $manifest . ",\n";
       } else {
         $first = false;
       }
       $manifest = $manifest . "\t" . HostsConfigManifest::getPublicFqdnLine($hostName, $publicFqdn) ;
     }

     if (!empty($manifest)) {
       $manifest = "\$" . "hostAttributes = {\n" . $manifest;
       $manifest = $manifest . "\n}\n";
     }
     return $manifest;
   }

   private static function getConfigLine($key, $value) {
      if (empty($value)) {
        return "";
      }
      $manifest = "$" . $key . " = ";
      if ( ($key == "lzo_enabled") ||
           ($key == "snappy_enabled") || 
           ($key == "security_enabled") ||
           ($key == "wipeoff_data") ) {
        $manifest = $manifest . $value . "\n";
      } else if ($key == "datanode_du_reserved"
                 || $key == "fs_checkpoint_size") {
        // convert GB to bytes and round off to whole number
        $newVal = round($value * 1024 * 1024 * 1024);
        $manifest = $manifest . "\"" .  $newVal . "\"\n";
      } else {
        $manifest = $manifest . "\"" .  $value . "\"\n";
      }
      return $manifest;
   }

   private static $componentToKeytabMap = array (
     "hdp-hadoop::namenode" => array("nn.service.keytab"),
     "hdp-hadoop::snamenode" => array("nn.service.keytab"),
     "hdp-hadoop::datanode" => array("dn.service.keytab"),
     "hdp-hadoop::jobtracker" => array("jt.service.keytab"),
     "hdp-hadoop::tasktracker" => array("tt.service.keytab"),
     "hdp-hbase::master" => array("hm.service.keytab"),
     "hdp-hbase::regionserver" => array("rs.service.keytab"),
     "hdp-hive::server" => array("hive.service.keytab")
   );

   private static $keytabsToPrincipalMap = array (
     "nn.service.keytab" => array("nn", "HTTP", "host"),
     "dn.service.keytab" => array("dn"),
     "jt.service.keytab" => array("jt"),
     "tt.service.keytab" => array("tt"),
     "hm.service.keytab" => array("hm"),
     "rs.service.keytab" => array("rs"),
     "hive.service.keytab" => array("hive")
   );

   private static $headlessUsersKeytab = array (
     "hdfs.headless.keytab" => array("hdfs"),
     "ambari_qa.headless.keytab" => array("ambari_qa")
   );

   private static function getArrayVariableManifest($arr) {
     $manifest = "";
     $manifest = $manifest . "[";
     $first = TRUE;
     foreach ($arr as $k) {
       if (!$first) {
         $manifest = $manifest . ",";
       } else {
         $first = FALSE;
       }
       $manifest = $manifest . "\"" . $k . "\"";
     }
     $manifest = $manifest . "]";
     return $manifest;
   }

   public static function getPrincipalsKeytabs($componentHosts, $realm, $keytabDirAtAdmin) {
     $allPrincipals = array();
     $allKeytabs = array(); 
     $principalsForKeytabs = array();
     foreach ($componentHosts as $comp => $hostList) {
       if (!isset(HostsConfigManifest::$componentToKeytabMap[$comp])) {
         continue;
       }
       $keytabs = HostsConfigManifest::$componentToKeytabMap[$comp];
       foreach ($hostList as $h) {
         foreach ($keytabs as $k) {
           $keytabWithPath = $keytabDirAtAdmin . "/" . $h . "." . $k;
           $allKeytabs[] = $keytabWithPath; 
           $shortNames = HostsConfigManifest::$keytabsToPrincipalMap[$k];
           $principalsForKeytabs[$keytabWithPath] = array();
           foreach ($shortNames as $s) {
             $princ = $s . "/" . $h . "@" . $realm ;
             $principalsForKeytabs[$keytabWithPath][] = $princ;
             $allPrincipals[] = $princ;
           }
         }
       }
     }
     ##Add headless users
     foreach (HostsConfigManifest::$headlessUsersKeytab as $k => $plist) {
       ##TODO add to a directory to download from
       $keytabWithPath = $keytabDirAtAdmin . "/" . $k;
       $allKeytabs[] = $keytabWithPath;
       foreach ($plist as $s) {
         $princ = $s . "@" . $realm ;
         $principalsForKeytabs[$keytabWithPath][] = $princ;
         $allPrincipals[] = $princ;
       } 
     }
     $allPrincipals = array_unique($allPrincipals);
     $allKeytabs = array_unique($allKeytabs);

     $manifest = "";
     //generate manifest for principals
     $manifest = $manifest . "\$principals_to_create = " ;
     $manifest = $manifest . HostsConfigManifest::getArrayVariableManifest($allPrincipals); 
     $manifest = $manifest . "\n";
     //generate manifest for keytabs
     $manifest = $manifest . "\$keytabs_to_create = " ;
     $manifest = $manifest . HostsConfigManifest::getArrayVariableManifest($allKeytabs); 
     $manifest = $manifest . "\n";

     //keytabs to principals
     $manifest = $manifest . "\$principals_in_keytabs = {";
     $first = TRUE;
     foreach ($principalsForKeytabs as $k => $ps) {
       if (!$first) {
         $manifest = $manifest . ", ";
       } else {
         $first = FALSE;
       }
       $manifest = $manifest . "\"" . $k . "\"" . " => " . HostsConfigManifest::getArrayVariableManifest($ps) ;
     }
     $manifest = $manifest . "}\n";
     return $manifest;
   }

}

/***
$compHost = array ("hdp-hadoop::namenode" => array("n1"), "hdp-hadoop::datanode" => array("n2","n3"));
$manifest = HostsConfigManifest::getPrincipalsKeytabs($compHost, "HADOOP.COM", "/t1");

print($manifest);
***/


?>
