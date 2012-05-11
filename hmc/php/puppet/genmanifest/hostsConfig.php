<?php

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
           ($key == "snappy_enabled") ) {
        $manifest = $manifest . $value . "\n";
      } else {
        $manifest = $manifest . "\"" .  $value . "\"\n";
      }
      return $manifest;
   }
}

?>
