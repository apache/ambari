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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once "../util/util.php";
include_once '../db/HMCDBAccessor.php';

include_once "../util/HMCTxnUtils.php";
include_once 'commandUtils.php';
include_once "../util/YumRepoConfigParser.php";

  $logger = new HMCLogger("BootStrap");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  function bootstrap($clusterName, $user, $rscript, $sshkey, $repo)
  {
    global $logger, $dbAccessoar, $hosts, $readFromFile;

    $master=strtolower(exec('hostname -f'));
    $repoFile = $repo['yumRepoFilePath'];
    $usingLocalRepo = $repo['usingLocalRepo'];
    $gpgKeyFiles = $repo['gpgKeyFiles'];

    exec ("/etc/init.d/iptables stop");
    $logger->log_info("List of hosts to BootStrap ".json_encode($hosts));
    $logger->log_info("Run script for pdsh ".$rscript);
    $scpCmd = "scp -o StrictHostKeyChecking=no ";

    foreach ($hosts as $host) {
      $host = trim($host);

      $filesToCopy = array_merge ( array ($rscript, $repoFile), $gpgKeyFiles);

      /* Copy puppet run script to all nodes */
      // Copy repo file to each node
      // Copy gpg keys to each node
      if (!empty($filesToCopy)) {
        $cmd = "$scpCmd -i $sshkey " . implode(" ", $filesToCopy)
            . " $user@$host:/tmp/ ";
        $logger->log_debug("Running scp command $cmd");
        exec($cmd);
      }
    }

    $remoteRepoFilePath = trim("/tmp/" . basename(trim($repoFile)));
    $remoteGpgKeyPaths = "";
    foreach ($gpgKeyFiles as $kFile) {
      $dFile = trim("/tmp/" . basename(trim($kFile)));
      if ($remoteGpgKeyPaths != "") {
        $remoteGpgKeyPaths .= ",";
      }
      $remoteGpgKeyPaths .= $dFile;
    }

    $rcmd = "/tmp/puppet_agent_install.sh --puppet-master=" . $master
        . " --repo-file=" . $remoteRepoFilePath
        . " --gpg-key-files=" . $remoteGpgKeyPaths;

    if ("true" == strtolower($usingLocalRepo)) {
        $rcmd .= " --using-local-repo ";
    }
    $logger->log_info("Running $rcmd to bootstrap each node");

    runPdsh($clusterName, "bootstrapNodes", $user, $readFromFile, $rcmd);

    $result = parseAndUpdateNodeInfo ($clusterName, "bootstrapNodes", $logger);

    $logger->log_debug("Puppet agent install pdsh result: "
        . print_r($result, true));

    $errNodes = array();
    foreach ($result["allHosts"] as $hostInfo) {
      $errNodes[$hostInfo["hostName"]] =
         array ( "discoveryStatus" => $hostInfo["discoveryStatus"],
                 "badHealthReason" => $hostInfo["badHealthReason"]
                );
    }

    return $errNodes;
  }


$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$mySubTxnId = $argv[4];
$parentSubTxnId = $argv[5];
$readFromFile = $argv[6];

$hosts = readHostsFile($readFromFile);
$hosts = convertToLowerCase($hosts);
$totalHosts = count($hosts);

$opStatus = "STARTED";
$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$sshkey = getSshKeyFilePath($clusterName);
// $rscript = "/var/www/html/KickAssHDPUI/ShellScripts/puppet_agent_install.sh";
$rscript = realpath("../../ShellScripts/puppet_agent_install.sh");

$repository=array();
$configs = $dbAccessor->getServiceConfig($clusterName);
if ($configs["result"] != 0) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error when trying to retrieve configs from DB");
  return;
}

$repoFile = $configs["properties"]["yum_repo_file"];
$usingLocalRepo = $configs["properties"]["using_local_repo"];
$gpgKeyLocations = getEnabledGpgKeyLocations($repoFile);
if ($gpgKeyLocations === FALSE) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error when trying to parse yum repo config");
  return;
}

$tmpDir = "/tmp/hmcDownloads-".time()."/";
$retVal = 0;
$output = array();
exec("mkdir -p ".$tmpDir, $output, $retVal);
if ($retVal != 0) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error when trying to create tmp download dir"
      . ", dir=" . $tmpDir . ", output=" . print_r($output, true));
  return;
}

$gpgKeyFiles = array();

foreach ($gpgKeyLocations as $repoId => $gpgInfo) {
  if (!isset($gpgInfo["gpgkey"])) {
    continue;
  }
  $loc = $gpgInfo["gpgkey"];
  $logger->log_info("Fetching gpg key for $repoId from location $loc");
  $info = parse_url($loc);
  if ($info === FALSE || !isset($info["path"])) {
    $logger->log_error("Skipping invalid url $loc");
    continue;
  }
  $fileName = basename($info["path"]);

  $destFilePath = $tmpDir . "/" . $fileName;

  $fetchCurlCmd = "curl --connect-timeout 30 --fail -s -o "
      . $destFilePath . " " . $loc;

  $logger->log_info("Fetching gpg key for $repoId from location $loc using "
      . $fetchCurlCmd);

  $retVal = 0;
  $output = array();
  exec($fetchCurlCmd, $output, $retVal);

  if ($retVal != 0) {
    $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
    $logger->log_error("Error when trying to download gpg key using "
        . $fetchCurlCmd . ", output=" . print_r($output, true));
    return;
  }
  array_push($gpgKeyFiles, $destFilePath);
}

$repository = array( "yumRepoFilePath" => $repoFile,
                     "usingLocalRepo" => $usingLocalRepo,
                     "gpgKeyFiles" => $gpgKeyFiles);

$logger->log_debug("BootStrapping with puppet");
$boot_result = bootstrap($clusterName, $deployUser, $rscript,
                           $sshkey, $repository);
$logger->log_debug("Boot Result \n".print_r($boot_result, true));

$nodeFileOut = fopen($readFromFile, "w");
if ($nodeFileOut == FALSE) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error while trying to rewrite hosts file");
  return;
}

$updateHosts = array();
$failedHosts = 0;
$successfulHosts = 0;
foreach ($boot_result as $hostName => $hostInfo) {
  if ($hostInfo["discoveryStatus"] == "FAILED") {
    $updateHosts[$hostName] = $hostInfo;
    $failedHosts++;
  } else {
    $successfulHosts++;
    // write the nodename to the readFromFile file.
    fwrite($nodeFileOut, $hostName."\n");
  }
}
fclose($nodeFileOut);

$ret = $dbAccessor->updateHostDiscoveryStatus($clusterName, $updateHosts);
if ($ret["result"] != 0) {
  $logger->log_error("Failed to update DB for hosts status, error="
      . $ret["error"]);
  // TODO - handle failure?
}

$opStatus = "SUCCESS";
if ($totalHosts > 0) {
  if ($successfulHosts == 0) {
    $opStatus = "TOTALFAILURE";
  } else if ($failedHosts > 0) {
    $opStatus = "FAILED";
  }
}

$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$logger->log_info("Completed bootstrapping puppet agents, opStatus=" . $opStatus);
?>
