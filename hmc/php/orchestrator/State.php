<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * State of both Services (HDFS, MapReduce etc.) and
 * ServiceComponents (NameNode, JobTracker etc.).
 */
class State {
  const UNKNOWN = 0;
  const UNINSTALLING = 1;
  const UNINSTALLED = 2;
  const INSTALLING = 3;
  const INSTALLED = 4;
  const STARTING = 5;
  const STARTED = 6;
  const STOPPING = 7;
  const STOPPED = 8;
  const FAILED = 9;

  public static $STATE =
    array (
      "UNKNOWN",
      "UNINSTALLING",
      "UNINSTALLED",
      "INSTALLING",
      "INSTALLED",
      "STARTING",
      "STARTED",
      "STOPPING",
      "STOPPED",
      "FAILED"
    );

  public static $DESIRED_STATE =
    array (
      self::UNINSTALLING => "uninstalled",
      self::UNINSTALLED => "uninstalled",
      self::INSTALLING => "installed_and_configured",
      self::INSTALLED => "installed_and_configured",
      self::STARTING => "running",
      self::STARTED => "running",
      self::STOPPING => "stopped",
      self::STOPPED => "stopped"
  );

  public static function getStateFromString($stateStr) {
    $found = array_keys(self::$STATE, $stateStr);
    if (is_array($found) && count($found) == 1) {
      return $found[0];
    }
    return FALSE;
  }

  /**
   * Converts INSTALLED to INSTALLING and likewise.
   * No-op for failed or unknown states
   * @param int $state
   */
  public static function convertStateToProgressingState($state) {
    $retState = self::UNKNOWN;
    switch ($state) {
      case State::UNKNOWN:
      case State::UNINSTALLING:
      case State::INSTALLING:
      case State::STARTING:
      case State::STOPPING:
      case State::FAILED:
        $retState = $state;
        break;
      case State::UNINSTALLED:
        $retState = self::UNINSTALLING;
        break;
      case State::INSTALLED:
        $retState = self::INSTALLING;
        break;
      case State::STARTED:
        $retState = self::STARTING;
        break;
      case State::STOPPED:
        $retState = self::STOPPING;
        break;
      default:
        $retState = self::UNKNOWN;
        break;
    }
    return $retState;
  }

}

class TransactionProgress {
  const PENDING = 0;
  const IN_PROGRESS = 1;
  const COMPLETED = 2;
  const FAILED = 3;

  public static $PROGRESS =
  array (
    "PENDING",
    "IN_PROGRESS",
    "COMPLETED",
    "FAILED"
    );

  public static function getProgressFromString($progressStr) {
    $found = array_keys(self::$PROGRESS, $progressStr);
    if (is_array($found) && count($found) == 1) {
      return $found[0];
    }
    return FALSE;
  }

}

function getTransactionProgressFromState($state) {
  $txnState = TransactionProgress::PENDING;
  switch ($state) {
    case State::UNKNOWN:
      $txnState = TransactionProgress::PENDING;
      break;
    case State::UNINSTALLING:
    case State::INSTALLING:
    case State::STARTING:
    case State::STOPPING:
      $txnState = TransactionProgress::IN_PROGRESS;
      break;
    case State::UNINSTALLED:
    case State::INSTALLED:
    case State::STARTED:
    case State::STOPPED:
      $txnState = TransactionProgress::COMPLETED;
      break;
    case State::FAILED:
      $txnState = TransactionProgress::FAILED;
      break;
    default:
      $txnState = TransactionProgress::PENDING;
  }
  return $txnState;
}

function getActionDescription($name, $action, $state) {
  /*
  $stateInfo = "";
  if ($state == "PENDING") {
    $stateInfo = "is $state";
  } else if ($state == "IN_PROGRESS") {
    $stateInfo = "is IN PROGRESS";
  } else if ($state == "COMPLETED" || $state =="FAILED") {
    $stateInfo = "has $state";
  }
  $description = $name . " " . strtolower($action) . " " . strtolower($stateInfo);
  */
  $description = $name.' '.$action;
  return $description;
}
?>
