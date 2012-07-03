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


  include_once "../util/Logger.php";
  include_once "PuppetConfigs.php";
  include_once "DBReader.php";
  include_once "genmanifest/generateManifest.php";
  include_once "genmanifest/RoleDependencies.php";

  define("KICKFAILED", "nokick");
  define("KICKSENT", "kicked");
  define("FAILEDNODES", "failed");
  define("SUCCESSFULLNODES", "success");
  define("TIMEDOUTNODES", "timedoutnodes");
  define("PREV_KICK_RUNNING", "prev_kick_running");
  class PuppetInvoker {
    private $parallel;
    private $reportDir;
    private $kickTimeout;
    private $logger;

    function __construct($db) {
      $this->reportDir = $GLOBALS["puppetReportsDir"];
      $this->kickTimeout = $GLOBALS["puppetKickTimeout"];
      $this->parallel = $GLOBALS["puppetMaxParallelKicks"];
      $this->db = $db;
      $this->logger = new HMCLogger("PuppetInvoker");
    }

    private function executeAndGetOutput($cmd) {
      $handle = popen ($cmd . " 2>&1", 'r');
      $output = "";
      if ($handle) {
        while(! feof ($handle)) {
          $read = fgets ($handle);
          $output = $output . $read;
        }
        pclose ($handle);
      }
      return $output;
    }

    private function sendKick($nodes, $txnId, &$failedNodes,
        &$successNodes, &$prevKickRunningNodes) {
      //Some nodes may already be done in the meanwhile
      $doneNodes = array();
      $this->checkForReportFiles($nodes, $txnId, $doneNodes);
      if (count($doneNodes) < count($nodes)) {
        $cmd = "";
        $cmd = $cmd . "puppet kick ";
        foreach ($nodes as $n) {
          $cmd = $cmd . " --host " . $n;
        }
        $p = 10;
        if (count($nodes) < $p) {
          $p = count($nodes);
        }
        $cmd = $cmd . " --parallel " . $p;
        $this->logger->log_debug("Kick command: " . $cmd);
        $output = $this->executeAndGetOutput($cmd);
        $this->logger->log_debug("Kick response begins ===========");
        $this->logger->log_debug($output);
        $this->logger->log_debug("Kick response ends ===========");
      }
      foreach ($nodes as $kNode) {
        if (isset($doneNodes[$kNode])) {
          $this->logger->log_debug($kNode . " previous kick has returned, no need to kick again");
          //Mark as previous kick running, reports will be discovered in waitForResults.
          $prevKickRunningNodes[] = $kNode;
          continue;
        }
        $regExSuccess = "/". $kNode . " .* exit code 0/";
        $regExRunning = "/". $kNode . ".* is already running/";
        if (preg_match($regExSuccess, $output)>0) {
          $successNodes[] = $kNode;
        } else if (preg_match($regExRunning, $output)>0) {
          $this->logger->log_info($kNode . "previous kick still running, will continue to wait");
          $prevKickRunningNodes[] = $kNode;
        } else {
          $this->logger->log_info($kNode . ": Kick failed with $output");
          $failedNodes[] = $kNode;
        }
      }
    }

    private function sendKickWithRetry($nodes, $txnId, &$failedNodes,
        &$successNodes, &$prevKickRunningNodes) {
      $kickFailedNodes = array();
      $this->sendKick($nodes, $txnId, $kickFailedNodes,
          $successNodes, $prevKickRunningNodes);
      $numRetry = 3;
      while ( ($numRetry > 0) && (!empty($kickFailedNodes))) {
          $numRetry = $numRetry - 1;
          $this->logger->log_warn("Retrying kick after 10 seconds on " . print_r($kickFailedNodes, TRUE));
          sleep(10);
          $nodesToKick = $kickFailedNodes;
          $kickFailedNodes = array();
          $this->sendKick($nodesToKick, $txnId, $kickFailedNodes,
              $successNodes, $prevKickRunningNodes);
      }
      foreach ($kickFailedNodes as $fn) {
        $failedNodes[] = $fn;
      }
    }

    private function kickPuppetAsync($nodes, $txnId, &$kickFailedNodes, &$kickSuccessNodes, 
        &$prevKickRunningNodes) {
      $nodeListToKick = array();
      $index = 0;
      foreach($nodes as $n) {
        if ($index < 10) {
          $nodeListToKick[] = $n;
          $index++;
        } else {
          $this->sendKickWithRetry($nodeListToKick, $txnId, $kickFailedNodes,
              $kickSuccessNodes, $prevKickRunningNodes);
          $nodeListToKick = array();
          $nodeListToKick[] = $n;
          $index = 1;
        }
      }
      if ($index > 0) {
          $this->sendKickWithRetry($nodeListToKick, $txnId, $kickFailedNodes,
            $kickSuccessNodes, $prevKickRunningNodes);
      }
    }

    private function getInfoFromDb($clusterName, $nodes, $components) {
       $dbReader = new DBReader($this->db);
       $hostInfo = $dbReader->getHostNames($clusterName);
       $configInfo = $dbReader->getAllConfigs($clusterName);
       $hostRolesStates = $dbReader->getHostRolesStates($clusterName, $nodes, $components);
       $hostAttributes = $dbReader->getAllHostAttributes($clusterName);
       return array($hostInfo, $configInfo, $hostRolesStates, $hostAttributes);
    }

    public function kickPuppet($nodes, $txnObj, $clusterName, $nodesComponents,
        $globalOptions = array()) {
      //Get host config from db
      $txnId = $txnObj->toString();
      $components = array_keys($nodesComponents);
      $infoFromDb = $this->getInfoFromDb($clusterName, $nodes, $components);
      $hostInfo = $infoFromDb[0];
      $configInfo = $infoFromDb[1];
      $hostRolesStates = $infoFromDb[2];
      $hostAttributes = $infoFromDb[3];

      //Treat globalOpts as configs only
      if (!empty($globalOptions)) {
        foreach ($globalOptions as $key => $value) {
          $configInfo[$key] = $value;
        }
      }
       
      $manifestDir = $GLOBALS["puppetMasterModulesDirectory"] . "/catalog/files";
      $response = $this->genKickWait($nodes, $txnId, $clusterName, $hostInfo,
          $configInfo, $hostRolesStates, $hostAttributes, $manifestDir,
          $GLOBALS["puppetKickVersionFile"], $GLOBALS["DRYRUN"]);
      return $response;
    }

    public function kickServiceCheck($serviceCheckNodes, $txnObj, $clusterName) {
      $txnId = $txnObj->toString();

      $hostRolesStates = array();
      $roleDependencies = new RoleDependencies();
      $nodeList = array();
      foreach($serviceCheckNodes as $service => $node) {
        $rs = $roleDependencies->getServiceCheckRole($service);
        if (!isset($rs)) {
          $this->logger->log_error("No service check defined for service "
              . $service);
          continue;
        }
        $nodeList[] = $node;
        if (!isset($hostRolesStates[$node])) {
          $hostRolesStates[$node] = array();
        }
        $hostRolesStates[$node][$rs] = array();
      }
      $nodesToKick = array_unique($nodeList);
      $dbReader = new DBReader($this->db);
      $hostInfo = $dbReader->getHostNames($clusterName);
      $configInfo = $dbReader->getAllConfigs($clusterName);
      $hostAttributes = $dbReader->getAllHostAttributes($clusterName);
      $manifestDir = $GLOBALS["puppetMasterModulesDirectory"] . "/catalog/files";
      $response = $this->genKickWait($nodesToKick, $txnId, $clusterName, $hostInfo,
          $configInfo, $hostRolesStates, $hostAttributes, $manifestDir,
          $GLOBALS["puppetKickVersionFile"], $GLOBALS["DRYRUN"]);
      return $response;
    }

    private function writeVersionFile($versionFile, $txnId) {
      $fh = fopen($versionFile, "w");
      fwrite($fh, $txnId);
      fclose($fh);
    }

    private function createGenKickWaitResponse($kickFailedNodes, $failureResponseNodes, 
        $timedoutNodes, $successfullNodes, $allNodes) {
      $result = 0;
      $error = "";
      if ( (count($allNodes) > 0) && (count($kickFailedNodes) == count($allNodes)) ) {
        $result = -1;
        $error = "All kicks failed";
      }
      $failedNodes = array_merge($failureResponseNodes, $timedoutNodes);
      $response = array (
        "result" => $result,
        "error" => $error,
        KICKFAILED => $kickFailedNodes,
        FAILEDNODES => $failedNodes,
        SUCCESSFULLNODES => $successfullNodes,
        TIMEDOUTNODES => $timedoutNodes
      );
      $stringToLog = print_r($response, TRUE);
      $this->logger->log_info("Response of genKickWait: \n" . $stringToLog);
      return $response;
    }
    /**
     *This is public only for testing, don't use this method directly
     */
    public function genKickWait($nodes, $txnId, $clusterId, $hostInfo,
            $configInfo, $hostRolesStates, $hostAttributes, $manifestDir, $versionFile,
            $dryRun) {
      $kickFailedNodes = array();
      $failureResponseNodes = array();
      $kickedNodes = array();
      $timedoutNodes = array();
      $successfullNodes = array();
     
      if (empty($nodes)) {
        return $this->createGenKickWaitResponse($kickFailedNodes, $failureResponseNodes,
           $timedoutNodes, $successfullNodes, $nodes); 
      }

      //Add manifest loader
      copy($GLOBALS["manifestloaderDir"] . "/site.pp", $GLOBALS["manifestloaderDestinationDir"] . "/site.pp");

      //Generate manifest
      $agentModulesDir = $GLOBALS["puppetAgentModulesDirectory"];
      ManifestGenerator::generateManifest($manifestDir, $hostInfo,
          $configInfo, $hostRolesStates, $hostAttributes, $agentModulesDir);

      //Write version file
      $this->writeVersionFile($versionFile, $txnId);

      if ($dryRun) {
        $successfullNodes = $nodes;
        return $this->createGenKickWaitResponse($kickFailedNodes, $failureResponseNodes,
           $timedoutNodes, $successfullNodes, $nodes);
      }

      //Tar the modules and catalog
      $tarLocation = $GLOBALS["puppetMasterModulesDirectory"] . "/catalog/files" ;
      $removeOldTar = "rm -f " . $tarLocation . "/modules.tgz";
      $this->logger->log_info($removeOldTar);
      exec($removeOldTar);
      $tarCmd = "tar zcf ". $GLOBALS["manifestloaderDir"] . "/modules.tgz" . " " .  $GLOBALS["puppetMasterModulesDirectory"];
      $this->logger->log_info($tarCmd);
      exec($tarCmd);
      $placeNewTarCmd = "mv " . $GLOBALS["manifestloaderDir"] . "/modules.tgz" . " " . $tarLocation;
      $this->logger->log_info($placeNewTarCmd);
      exec($placeNewTarCmd);

      $numRekicks = 1;
      $maxRekicks = 3;
      $nodesToKick = $nodes;
      while ($numRekicks <= $maxRekicks && (count($nodesToKick) > 0)) {
          $newKickedNodes = array();
          $prevKickRunningNodes = array();
          $this->logger->log_info("Kick attempt (" . $numRekicks . "/" . $maxRekicks . ")");
          $result = $this->kickPuppetAsync($nodesToKick, $txnId, $kickFailedNodes, 
              $newKickedNodes, $prevKickRunningNodes);
          $kickedNodes = array_merge($kickedNodes, $newKickedNodes);
          $nodesToWait = array_merge($newKickedNodes, $prevKickRunningNodes);
          $timedoutNodes = array();
          $this->waitForResults($nodesToWait, $txnId, $successfullNodes, 
              $failureResponseNodes, $timedoutNodes);
          if (count($prevKickRunningNodes) == 0) {
            $numRekicks = $numRekicks +1;
          }
          $nodesToKick = $timedoutNodes;
          sleep(1);
      }
      $sitePPFile = $manifestDir . "/site.pp";
      system("mv " . $sitePPFile . " " . $GLOBALS["manifestloaderDir"] . "/site.pp-" . $txnId);
      // Delete version file, it will be generated next time.
      unlink($versionFile);
      $response = $this->createGenKickWaitResponse($kickFailedNodes, $failureResponseNodes,
          $timedoutNodes, $successfullNodes, $nodes); 
      return $response;
    }
    
    private function checkForReportFiles($nodes, $txnId, &$doneNodes) {
      foreach ($nodes as $n) {
        if (isset($doneNodes[$n])) {
          continue;
        }
        $fileName = $this->getReportFilePattern($n, $txnId);
        if (file_exists($fileName)) {
          $doneNodes[$n] = 1;
        }
      }
    }

    private function waitForResults($nodes, $txnId, &$successfullNodes, 
       &$failureResponseNodes, &$timedoutNodes) {
      $doneNodes = array();
      $startTime = time();
      $this->logger->log_info("Waiting for results from "
          . implode(",", $nodes));
      while (true) {
        $this->checkForReportFiles($nodes, $txnId, $doneNodes);
        $this->logger->log_info(count($doneNodes) . " out of " . count($nodes) 
            . " nodes have reported");
        if (count($doneNodes) >= count($nodes)) {
          ##All nodes kicked have reported back
          break;
        }
        $currTime = time();
        if ($currTime - $startTime > $this->kickTimeout) {
          $this->logger->log_warn("Kick timed out, waited "
               . $this->kickTimeout . " seconds");
          break;
        }
        sleep(5);
      }
      ##Get result from each node
      foreach ($nodes as $n) {
        $fileName = $this->getReportFilePattern($n, $txnId);
        if (file_exists($fileName)) {
          $r = file_get_contents($fileName);
          if ((preg_match("/status: changed/", $r) > 0) ||
              (preg_match("/status: unchanged/", $r)) > 0) {
            $successfullNodes[] = $n;
          } else {
            $failureResponseNodes[] = $n;
          }
        } else {
          $timedoutNodes[] = $n;
        }
      }
    }

    /**
     * Returns the summary reports collected from nodes.
     * The function does not wait for any reports, it just
     * returns the available ones.
     * $nodes : Array of nodes to collect reports from.
     * $txnObj : The transaction for which reports are needed.
     * returns array keyed by nodes. For each node, the entry is
     *    also an array with 3 keys:
     *      overall: overall status which can be changed, unchanged
     *             of failed. Both changed and unchanged can be
     *             considered as successful.
     *      finishtime: Time when the kick processing was completed
     *             at the node.
     *      message: This is a little trimmed version of actual reports
     *             sent by the nodes.
     */
    public function getReports($nodes, $txnObj) {
      $txnId = $txnObj->toString();
      $reports = array();
      foreach ($nodes as $n) {
        $filename = $this->getReportFilePattern($n, $txnId);
        if (file_exists($filename)) {
          $r = file_get_contents($filename);
          $reports[$n]["reportfile"] = $filename;
          if (preg_match("/status: changed/", $r) > 0) {
            $reports[$n]["overall"] = "CHANGED";
          } else if (preg_match("/status: unchanged/", $r) > 0) {
            $reports[$n]["overall"] = "UNCHANGED";
          } else {
            $reports[$n]["overall"] = "FAILED";
          }
          $finishtime = array();
          $count = preg_match_all("/time: (.*)/", $r, $finishtime);
          if ($count !== FALSE && $count > 0 ) {
            $reports[$n]["finishtime"] = $finishtime[1][$count-1];
          }
          $messages = array();
          $count = preg_match_all("/message: (.*)/", $r, $messages);
          $reports[$n]["message"] = array();
          if ($count !== FALSE && $count > 0) {
            for ($i = 0; $i < $count; $i++ ) {
              $reports[$n]["message"][] = $messages[1][$i];
            }
          }
        }
      }
      return $reports;
    }

    private function getReportFilePattern($node, $txnId) {
      $reportFile = $this->reportDir . "/" . $txnId . "/" . $node;
      return $reportFile;
    }
  }
?>
