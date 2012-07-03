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


class HMCTxnUtils {

  private static $logger = NULL;

  public static function init() {
    if (self::$logger == NULL) {
      self::$logger = new HMCLogger("HMCTxnUtils");
    }
  }

  /**
   * Get current time in microsecs as int
   */
  public static function getCurrentTimeInMicroSecs() {
    $f = microtime(TRUE);
    return intval(round($f , 3)*1000);
  }

  public static function createNewTransaction($dbHandle, $clusterName,
      $statusInfo) {
    $statusStr = json_encode($statusInfo);
    $newTxn =  $dbHandle->createNewTransaction($clusterName, $statusStr, "");
    if ($newTxn === FALSE || $newTxn["result"] != 0) {
      self::$logger->log_error("Failed to create new txn"
          . ", cluster=" . $clusterName
          . ", error=" . $newTxn["error"]);
      return FALSE;
    }
    $txnId = $newTxn["txnId"];
    self::$logger->log_debug("Created a new transaction"
        . ", cluster=" . $clusterName
        . ", txnId=" . $txnId);
    return $txnId;
  }

  /**
   * Execute command in a background process
   * @param resource $dbHandle HMCDBAccessor resource
   * @param string $clusterName
   * @param int $txnId
   * @param string $command
   * @param string $args args as a string
   * @param string $logFile file to log to - optional - default /dev/null
   * @return pid of running process on success
   * @return FALSE on failure
   */
  public static function execBackgroundProcess($dbHandle, $clusterName, $txnId,
      $command, $args, $logFile) {

    if (!isset($GLOBALS["BACKGROUND_EXECUTOR_PATH"])) {
      $GLOBALS["BACKGROUND_EXECUTOR_PATH"] = "/var/lib/HMC/BackgroundExecutor.php";
    }
    if (!isset($GLOBALS["PHP_EXEC_PATH"])) {
      $GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
    }

    $execCommand = $GLOBALS["PHP_EXEC_PATH"] . " " . $GLOBALS["BACKGROUND_EXECUTOR_PATH"]
        . " -t \"" . $txnId . "\""
        . " -c \"" . $command . "\"";

    if (isset($args) && $args != "") {
        $execCommand .= " -a \"" . $args . "\"";
    }

    $logDir = "/tmp";
    if (isset($GLOBALS["HMC_LOG_DIR"])) {
      $logDir = $GLOBALS["HMC_LOG_DIR"];
    }
    if (!isset($logFile) || $logFile == "") {
      $logFile = $logDir."/hmc.txn.".$txnId.".log";
    }
    $execCommand .= " -l \"" . $logFile . "\"";

    self::$logger->log_info("Trying to background a new process"
        . ", cluster=" . $clusterName
        . ", txnId=" . $txnId
        . ", command=" . $command
        . ", args=" . $args
        . ", logFile=" . $logFile
        . ", execCommand=" . $execCommand);

    $errCode = 0;
    $errMsg = "";
    $childPid = -1;

    $handle = popen($execCommand, "r");
    if ($handle === FALSE || !is_resource($handle)) {
      $errMsg = "Could not get valid handle from popen";
      self::$logger->log_error("Error executing background call"
          . ", command=" . $command
          . ", txnId=" . $txnId
          , ", error=" . $errMsg);
      return FALSE;
    }
    else {
      // get child pid
      $output  = "";
      do {
        $data = fread($handle, 256);
        $output .= $data;
        self::$logger->log_info("Output from process"
            . ", command=" . $command
            . ", txnId=" . $txnId
            . ", output=" . $output);
        $match = array();
        $count = preg_match("/Background Child Process PID:(\d+)/",
          $output, $match);
        if ($count > 0) {
          $childPid = intval($match[1]);
          self::$logger->log_info("Found child pid"
              . ", command=" . $command
              . ", txnId=" . $txnId
              . ", output=" . $output
              . ", pid=" . $childPid);
          break;
        }
        if (strpos($output,"Done with parent") !== FALSE) {
          self::$logger->log_error("Background process ended without pid"
              . ", command=" . $command
              . ", txnId=" . $txnId);
          return FALSE;
        }
      } while(!feof($handle));
    }
    pclose($handle);

    if ($childPid > 0) {
      // update child pid into db
      $pidInfo = array();
      $pidInfo["mainParentPid"] = $childPid;
      $result = $dbHandle->updateTransactionPidInfo($clusterName, $txnId,
          json_encode($pidInfo));
      if ($result === FALSE || $result["result"] != 0) {
        self::$logger->log_error("Could not update pid info into DB"
            . ", command=" . $command
            . ", txnId=" . $txnId);
        return FALSE;
      }
      return $childPid;
    }

    self::$logger->log_error("Invalid pid found for background process"
        . ", command=" . $command
        . ", txnId=" . $txnId);
    return FALSE;
  }

  public static function checkTxnProcessStatus($pidInfo) {
    if (!isset($pidInfo["mainParentPid"])) {
      return FALSE;
    }

    $pid = intval($pidInfo["mainParentPid"]);
    $command = "ps -p $pid | grep $pid | grep -v grep | wc -l";
    $output = array();
    $errCode = 0;
    $lastLine = exec($command, $output, $errCode);

    if ($errCode != 0) {
      return FALSE;
    }
    $matchCount = intval($lastLine);
    return ($matchCount > 0);
  }
}

HMCTxnUtils::init();

?>
