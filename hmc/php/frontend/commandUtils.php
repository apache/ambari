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


function launchCmd ($cmd)
{

  global $logger;
  $logger->log_info("Env variable WCOLL is ".json_encode(getenv("WCOLL")));
  $handle = popen($cmd, "r");
  while (!feof($handle)) {
    $read = fread($handle, 4096);
  }
  pclose($handle);

  // FIXME return some sort of status
  return 0;
}

function readHostsFile($hostsFile) {
  $hostsFd = fopen($hostsFile, "r");
  $hosts = array();
  while (($buffer = fgets($hostsFd, 4096)) !== false) {
    $line = trim($buffer);
    if ($line == "") {
      continue;
    }
    if (substr($line, 0, 1) == "#") {
      continue;
    }
    $splitHosts = preg_split("/[\s,]+/", $line);
    foreach ($splitHosts as $splitHost) {
      $h = trim($splitHost);
      if ($h != "") {
        $matches = array();
        $count = preg_match("/(.*)\[(\d+)-(\d+)\](.*)/", $h, $matches);
        if ($count == 1 && count($matches) == 5) {
          $hostPrefix = $matches[1];
          $hostSuffix = $matches[4];
          for ($i = $matches[2]; $i <= $matches[3]; $i++) {
            $newH = $hostPrefix . $i . $hostSuffix;
            $hosts[] = strtolower($newH);
          }
        } else {
          $hosts[] = strtolower($h);
        }
      }
    }
  }
  return array_unique($hosts);
}

function runPdsh($clusterName, $operationName, $deployUser, $hosts, $cmdLine) {
  global $logger;
  $sshCmd = "ssh -o ConnectTimeOut=3 -o StrictHostKeyChecking=no";
  if (getSshKeyFilePath($clusterName)) {
    $sshCmd = $sshCmd." -i ".getSshKeyFilePath($clusterName)." -l ".$deployUser." $1";
  } else {
    $sshCmd = $sshCmd." -l ".$deployUser." $1";
  }

  $clusterDir = getClusterDir($clusterName);
  $myDir = $clusterDir . $operationName . "/";
  if (is_dir($myDir)) {
    rrmdir($myDir);
  }
  mkdir($myDir);

  $fullSshCmd = $sshCmd . " \"".$cmdLine."\" 1>" . $myDir . "$1.out 2>" . $myDir . "$1.err ";

  $sshCmdFile = $myDir."/ssh.sh";
  $sshCmdFileHdl = fopen($sshCmdFile, 'w');
  fwrite($sshCmdFileHdl, $fullSshCmd ." ; \n");
  fwrite($sshCmdFileHdl, " echo $? > " . $myDir . "$1.done ; \n");
  fclose($sshCmdFileHdl);
  chmod($sshCmdFile, 0555);

  $logger->log_info("Hosts for this operation: ".json_encode($hosts));
  putenv("WCOLL=$hosts");

  $pdshCmd = "pdsh -R exec ".$sshCmdFile." %h ";
  $logger->log_info("Going to execute " . $operationName . " : ".$pdshCmd);
  $retval = launchCmd($pdshCmd);
}

function parseAndUpdateNodeInfo ($clusterName, $operationName, $logger) {

  $clusterDir = getClusterDir($clusterName);
  $commandOutputDir = $clusterDir . $operationName . "/";

  $allHosts = array();

  $finalOpStatus = "SUCCESS";
  $nodeStatus = "SUCCESS";
  $nodeCount = 0;
  $successNodeCount = 0;
  $failedNodeCount = 0;

  if ($dirHandle = opendir($commandOutputDir)) {
    while (false !== ($entry = readdir($dirHandle))) {
      if ($entry == "." || $entry == "..") {
        continue;
      }

      // Only consider .out files
      if(!preg_match("/.out/", $entry)) {
        continue;
      }

      $nodeCount += 1;
      $nodeName = basename($entry, ".out");

      $doneFile = $commandOutputDir . $nodeName . ".done";
      if (file_exists($doneFile)) {
        $nodeStatus = "SUCCESS";
        // Read the contents of the done-file
        $doneFileContents = file_get_contents($doneFile);
        if (trim($doneFileContents) != "0") {
          $failedNodeCount += 1;
          $nodeStatus = "FAILED";
          $finalOpStatus = "FAILED";
          $logger->log_debug( "Contents of done file for $clusterName : $doneFileContents");
        }
      } else {
        $failedNodeCount += 1;
        $nodeStatus = "FAILED";
        $finalOpStatus = "FAILED";
      }

      // Initialize this host's array
      $thisHostArray = array();
      $thisHostArray["hostName"] = $nodeName;
      $thisHostArray["totalMem"] = 0;
      $thisHostArray["cpuCount"] = 0;
      $thisHostArray["osArch"] = "";
      $thisHostArray["disksInfo"] = json_encode(array());
      $thisHostArray["osType"] = "";
      $thisHostArray["os"] = "";
      $thisHostArray["ip"] = $nodeName; // To be unique
      $thisHostArray["badHealthReason"] = "";

      if ($nodeStatus == "FAILED") {
        $thisHostArray["badHealthReason"] =
            rtrim(file_get_contents($commandOutputDir.$nodeName . ".err"));
      } else {
        $successNodeCount += 1;
      }

      $thisHostArray["discoveryStatus"] = $nodeStatus;

      array_push($allHosts, $thisHostArray);
    }

    closedir($dirHandle);
  }

  if ($failedNodeCount == $nodeCount) {
    $finalOpStatus = "TOTALFAILURE";
  }

  $retArr = array(
      "allHosts" => $allHosts,
      "finalOpStatus" => $finalOpStatus
    );

  return $retArr;

}

?>
