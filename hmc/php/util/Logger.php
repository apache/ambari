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


if (!isset($GLOBALS["HMCLOGGER_TIMEZONE_SET"])) {
  @date_default_timezone_set("UTC");
  $GLOBALS["HMCLOGGER_TIMEZONE_SET"] = TRUE;
}

/**
 * Simple primitive logger.
 */
class HMCLogger {
  const ALL = 0;
  const TRACE = 1;
  const DEBUG = 2;
  const INFO = 3;
  const WARN = 4;
  const ERROR = 5;
  const FATAL = 6;
  const OFF = 10;

  // component to log along with each message
  private $component;

  // log level
  private $level;

  // log file to log to
  private $logFile;

  // TODO
  const DEFAULT_LOG_FILE = "/tmp/hmc.log";

  /**
   * Constructor
   * @param component Component name to log along with log messages
   */
  public function __construct($component) {
    $this->component = $component;
    $this->level = self::ALL;
    $this->logFile = self::DEFAULT_LOG_FILE;
    if (isset($GLOBALS["HMC_LOG_LEVEL"])) {
      $this->level = $GLOBALS["HMC_LOG_LEVEL"];
    }
    if (isset($GLOBALS["HMC_LOG_FILE"])) {
      $this->logFile = $GLOBALS["HMC_LOG_FILE"];
    }
  }

  /**
   * Set log level. Overrides global log level for this instance.
   */
  public function setLevel($level) {
    $this->level = $level;
  }

  /**
   * Internal helper function to log messages.
   */
  private function do_log($level, $msg) {
    $trace = debug_backtrace();
    $file = "";
    $line = "";
    $func = "";

    if (!empty($trace)) {
      $file_idx = 0;
      $func_idx = 0;
      if (count($trace) >= 2) {
        $file_idx = 1;
        $func_idx = 2;
      }

      $file = isset($trace[$file_idx]["file"]) ? basename($trace[$file_idx]["file"]) : "";
      $line = isset($trace[$file_idx]["line"]) ? $trace[$file_idx]["line"] : "";
      $func = isset($trace[$func_idx]["function"]) ? $trace[$func_idx]["function"] : "";

    }
    $curTime = @date("Y:m:d H:i:s");
    error_log("[$curTime][$level][$this->component][$file:$line][$func]: ".$msg."\n", 3,
        $this->logFile);
  }

  /**
   * Log a message with ERROR level
   */
  public function log_error($msg) {
    if ($this->level <= self::ERROR) {
      $this->do_log("ERROR", $msg);
    }
  }

  /**
   * Log a message with DEBUG level
   */
  public function log_debug($msg) {
    if ($this->level <= self::DEBUG) {
      $this->do_log("DEBUG", $msg);
    }
  }

  /**
   * Log a message with INFO level
   */
  public function log_info($msg) {
    if ($this->level <= self::INFO) {
      $this->do_log("INFO", $msg);
    }
  }

  /**
   * Log a message with TRACE level
   */
  public function log_trace($msg) {
    if ($this->level <= self::TRACE) {
      $this->do_log("TRACE", $msg);
    }
  }

  /**
   * Log a message with WARN level
   */
  public function log_warn($msg) {
    if ($this->level <= self::WARN) {
      $this->do_log("WARN", $msg);
    }
  }
}


?>
