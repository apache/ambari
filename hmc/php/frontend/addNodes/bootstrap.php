<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once "../util/util.php";
include_once '../db/HMCDBAccessor.php';

include_once "../util/HMCTxnUtils.php";
include_once 'commandUtils.php';

  $logger = new HMCLogger("BootStrap");
  $dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

  function bootstrap($clusterName, $user, $rscript, $sshkey, $repo)
  {
    global $logger, $dbAccessoar, $hosts, $readFromFile;

    $master=strtolower(exec('hostname -f'));
    $repo_name = $repo['name'];
    $repo_desc = $repo['desc'];
    $repo_url = $repo['url'];
    $repo_gpgkey = $repo['gpgkeyurl'];
    exec ("/etc/init.d/iptables stop");
    $logger->log_debug("List of hosts to BootStrap ".json_encode($hosts));
    $logger->log_debug("Run script for pdsh ".$rscript);
    $scpCmd = "scp -o StrictHostKeyChecking=no ";
    foreach ($hosts as $host) {
      $host = trim($host);
      $cmd = "$scpCmd -i $sshkey $rscript $user@$host:/tmp";
      exec ("$scpCmd -i $sshkey $rscript $user@$host:/tmp");
    }

    /* Copy puppet run script to all nodes */
    $logger->log_debug("/tmp/puppet_agent_install.sh $master $repo_name $repo_desc $repo_url $repo_gpgkey");

    $rcmd = "/tmp/puppet_agent_install.sh $master $repo_name $repo_desc $repo_url $repo_gpgkey";
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
$repository['name']="hmc_puppet";
$repository['desc']="puppetlabs";
$repository['url']="http://yum.puppetlabs.com/el/5/products/x86_64/";
$repository['gpgkeyurl']="http://yum.puppetlabs.com/RPM-GPG-KEY-puppetlabs";
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
