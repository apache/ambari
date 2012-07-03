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

$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

/* If pattern exists in the output return $ret
If pattern is success pattern then $ret = 0
If pattern is error pattern then $ret = 1
If pattern is empty return the output['retcode']
which is the return code of the remote command
*/
function check_error ($output, $pattern, $ret) {
  $ret1=($ret+1)%2;
  for ($i=0; $i<count($output); $i++) {
    if (preg_match ("/$pattern/", $output[$i])) {
      return $ret;
    }
  }
  return $ret1;
}

function ping($host,$port=8139,$timeout=10, &$errstr, &$errno)
{
  $fsock = fsockopen($host, $port, $errno, $errstr, $timeout);
  if ( ! $fsock )
  {
    return FALSE;
  }
  else
  {
    fclose($fsock);
    return TRUE;
  }
}


/* Sign and verify puppet agent */
function sign_and_verify_agent ($hosts, $logger) {
  $origHosts = $hosts;
  $totalCnt = count($hosts);
  $output = array();
  $hostsState = array();
  foreach ($hosts as $host) {
    $hostsState[$host] = FALSE;
  }

  $logger->log_info("Starting sign/verify puppet agent for " . $totalCnt
      . " nodes, hosts=" . implode(",", $origHosts));

  $signed_hosts = array();
  $logger->log_info("Getting puppet master list to find all signed agents");
  $cmd = "puppet cert --confdir=/etc/puppet/master list --all | grep \"^+ \"";
  exec ($cmd, $signed_hosts, $err);
  for ($k=0; $k<count($signed_hosts); $k++) {
    foreach ($hosts as $i => $host) {
      $host = trim($host);
      if (preg_match ("/$host/", $signed_hosts[$k])) {
        unset($hosts[$i]);
        $hostsState[$host] = TRUE;
      }
    }
  }

  $waitLoops = 10;
  $sleepInterval = 3;
  $waitLoop = 0;
  $logger->log_info("Looping through until all puppet agents are signed");
  for($waitLoop = 0; $waitLoop < $waitLoops; $waitLoop++) {
    $waitSecs = $waitLoop * $sleepInterval;
    if ($waitLoop > 0) {
      $logger->log_info("Waited " . $waitSecs . " seconds for puppet cert sign"
          . ", hostsRemaining=" . count($hosts)
          . ", totalHosts=" . $totalCnt
          . ", totalWaitedTimeSeconds=" . $waitSecs);
    }
    $unsigned_hosts = array();
    $cmd = "puppet cert --confdir=/etc/puppet/master list --all | grep -v \"^+ \"";
    exec ($cmd, $unsigned_hosts, $err);
    foreach ($hosts as $i => $host) {
      $host = trim($host);
      for ($j=0; $j<count($unsigned_hosts); $j++) {
        if (preg_match ("/$host/", $unsigned_hosts[$j])) {
          $logger->log_debug("Signing certificate for ".$host."\n");
          $out_arr = array();
          $cmd = "puppet cert --confdir=/etc/puppet/master sign $host";
          exec ($cmd, $out_arr, $retcode);
          if ($retcode != 0) {
            $logger->log_error("Failed to sign cert for host " . $host);
            $output[$host] =
              array ( "discoveryStatus" => "FAILED",
                      "badHealthReason" => "Puppet cert sign failed: " . implode(";", $out_arr));
            $hostsState[$host] = FALSE;
          } else {
            $logger->log_info("Puppet cert sign succeeded for host " . $host);
            $hostsState[$host] = TRUE;
            if (isset($output[$host])) {
              unset($output[$host]);
            }
          }
          unset($hosts[$i]);
          break;
        }
      }
    }
    if (empty($hosts)) {
      break;
    }
    sleep($sleepInterval);
  }

  if ($waitLoop == 10) {
    $logger->log_error("Timed out waiting for all puppet agents to ping master");
  }

  // re-check if the hosts are now signed
  $logger->log_info("Re-checking to ensure all puppet hosts are signed");
  $signed_hosts = array();
  $cmd = "puppet cert --confdir=/etc/puppet/master list --all | grep \"^+ \"";
  exec ($cmd, $signed_hosts, $err);
  for ($k=0; $k<count($signed_hosts); $k++) {
    foreach ($hostsState as $hostName => $state) {
      $hostName = trim($hostName);
      if (preg_match ("/$hostName/", $signed_hosts[$k])) {
        $logger->log_info("Puppet cert signed for host " . $host);
        $hostsState[$host] = TRUE;
        if (isset($output[$host])) {
          unset($output[$host]);
        }
      }
    }
  }

  $countFailed = 0;
  $countSucceeded = 0;
  foreach ($hostsState as $hostName => $state) {
    if ($state) {
      $countSucceeded++;
    } else {
      $countFailed++;
    }
  }

  $logger->log_info("Puppet cert sign status"
     . ", totalHosts=" . $totalCnt
     . ", succeededHostsCount=" . $countSucceeded
     . ", failedHostsCount=" . $countFailed);

  sleep(5);

  // run multiple attempts for pings to handle intermittent failures
  $pendingNodes = array();
  foreach ($origHosts as $i => $host) {
    $host = trim($host);
    if (array_key_exists ($host , $output)) {
      continue;
    }
    array_push($pendingNodes, $host);
  }

  $retryAttempt = 0;
  do {
    $retryAttempt++;
    if ($retryAttempt > 1) {
      // keep a small sleep between retries
      // no sleep on first loop
      sleep(3);
    }

    $logger->log_debug("Puppet kick --ping retry attempt " . $retryAttempt
        . ", pendingHoststoCheck=" . implode(",", $pendingNodes));

    $failedNodes = array();

    $pHostOutput = array();
    $pHostResponse = array();
    foreach ($pendingNodes as $i => $host) {
      /* Give ping agent check if it is working */
      $logger->log_debug("Pinging puppet agent for host=".$host);
      $errstr = "";
      $errno = "";
      ping($host, 8139, 10, $errstr, $errno);
      $pHostOutput[$host] = $errstr;
      $pHostResponse[$host] = $errno;
    }

    foreach ($pendingNodes as $i => $host) {
      if ($pHostResponse[$host] == 0) {
          $logger->log_info("Ping to puppet agent succeeded for host [" . $host . "]");
          $hostsState[$host] = TRUE;
          if (isset($output[$host])) {
            unset($output[$host]);
          }
      } else {
          $logger->log_error("Failed to ping puppet agent on host [" . $host . "]: " . $pHostOutput[$host]);
          $failedNodes[] = $host;
          $errorCode = $pHostResponse[$host];
          $errorLogs = "Puppet agent ping failed: [" . $pHostOutput[$host] . "]";

          if (!isset($output[$host])) {
            $output[$host] =
                array ( "discoveryStatus" => "FAILED",
                        "badHealthReason" => "Puppet agent ping failed: "
                            . ", error=" . $errorCode
                            . ", outputLogs=" . $errorLogs);
          }
          $hostsState[$host] = FALSE;
      }
    }
    $pendingNodes = $failedNodes;
  } while (!empty($pendingNodes) && $retryAttempt < 3);

  $countFailed = 0;
  $countSucceeded = 0;
  foreach ($hostsState as $hostName => $state) {
    if ($state) {
      $countSucceeded++;
    } else {
      $countFailed++;
    }
  }

  $logger->log_info("Puppet agent ping status"
      . ", totalHosts=" . $totalCnt
      . ", succeededHostsCount=" . $countSucceeded
      . ", failedHostsCount=" . $countFailed);

  $response = array();
  foreach ($hostsState as $host => $state) {
    if (!$state) {
      if (!isset($output[$host])) {
        $logger->log_error("Timed out waiting for puppet agent on host " . $host);
        $response[$host] = array ( "discoveryStatus" => "FAILED",
            "badHealthReason" => "Puppet cert sign timed out");
      } else {
        $response[$host] = $output[$host];
      }
    } else {
      $response[$host] = array ( "discoveryStatus" => "SUCCESS",
          "badHealthReason" => "");
    }
  }

  $logger->log_info("Completed sign/verify puppet agent for "
     . count($response) . " nodes"
     . ", result=" . print_r($response, true));

  return $response;
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

$logger = new HMCLogger("PuppetFinalize:txnId="
    . $rootTxnId . ":subTxnId=" . $mySubTxnId);

$logger->log_info("Starting signing of puppet agents certs for "
    . count($hosts) . " hosts");

$opStatus = "STARTED";
$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

// Create progress files for UI to track
$operationName = "finalizeNodes";
$clusterDir = getClusterDir($clusterName);
$myDir = $clusterDir . $operationName . "/";
if (is_dir($myDir)) {
  rrmdir($myDir);
}
mkdir($myDir);
foreach ($hosts as $host) {
  $fileName = $myDir . "/" . $host . ".out";
  $h = fopen($fileName, "a");
  if ($h !== FALSE) {
    fclose($h);
  }
}

$result = sign_and_verify_agent ($hosts,$logger);
$logger->log_debug("Puppet Cert Sign Result:\n".print_r($result, true));

$nodeFileOut = fopen($readFromFile, "w");
if ($nodeFileOut == FALSE) {
  $subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, "TOTALFAILURE");
  $logger->log_error("Got error while trying to rewrite hosts file");
  return;
}

$updateHosts = array();
$failedHosts = 0;
$successfulHosts = 0;
foreach ($result as $hostName => $hostInfo) {
  $fileName = $myDir . "/" . $hostName . ".done";
  $errFileName = $myDir . "/" . $hostName . ".err";
  if ($hostInfo["discoveryStatus"] == "FAILED") {
    $updateHosts[$hostName] = $hostInfo;
    $errorString = $hostInfo["badHealthReason"];
    $f = fopen($errFileName, "w");
    if ($f !== FALSE) {
      for ($written = 0; $written < strlen($errorString);) {
        $writtenBytes = fwrite($f, substr($errorString, $written));
        if ($writtenBytes === FALSE) {
          $logger->log_error("Failed to write error file for puppet cert sign failure"
              . ", host=" . $hostName
              . ", errFile=" . $errFileName
              . ", error=" . $errorString);
          break;
        }
        $written += $writtenBytes;
      }
      fflush($f);
      fclose($f);
    } else {
      $logger->log_error("Failed to write error file for puppet cert sign failure"
          . ", host=" . $hostName
          . ", errFile=" . $errFileName
          . ", error=" . $errorString);
    }
    system("echo \"1\" > " . $fileName);
    $failedHosts++;
  } else {
    system("echo \"0\" > " . $fileName);
    // write the nodename to the readFromFile file.
    fwrite($nodeFileOut, $hostName."\n");
    $successfulHosts++;
  }
}
fclose($nodeFileOut);

$logger->log_debug("Updating DB for hosts discovery status for puppet agent cert signing");
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
$logger->log_info("Puppet finalize, succeeded for " . $successfulHosts
  . " and failed for " . $failedHosts . " of total " . $totalHosts . " hosts");

$subTransactionReturnValue = $dbAccessor->updateSubTransactionOpStatus($clusterName, $parentSubTxnId, $mySubTxnId, $opStatus);
if ($subTransactionReturnValue["result"] != 0 ) {
  $logger->log_error("Got error while updating subTxn: ".$subTransactionReturnValue["error"]);
  print json_encode($subTransactionReturnValue);
  return;
}

$logger->log_info("Completed signing of certs for puppet agents, opStatus=" . $opStatus);
?>
