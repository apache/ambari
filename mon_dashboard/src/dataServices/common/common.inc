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

// Expected to be included by endpoint script
// include_once("global_configs.inc");

/**
 * Get whether debug mode enabled
 * @return boolean True if debug mode enabled
 */
function hdp_mon_get_debug_mode() {
  if (isset($GLOBALS["HDP_MON_DEBUG_MODE"])) {
    return $GLOBALS["HDP_MON_DEBUG_MODE"];
  }

  // default false
  $GLOBALS["HDP_MON_DEBUG_MODE"] = FALSE;
  return FALSE;
}

/**
 * Helper logging function to log the file,
 * line and function where the message was to
 * be logged from
 * Enter description here ...
 * @param string $msg Message to be logged
 */
function hdp_mon_error_log($msg) {
  $trace = debug_backtrace();
  $file = "";
  $line = "";
  $func = "";
  if (!empty($trace)) {
    $idx = 0;
    if (count($trace) > 1) {
      $idx = 1;
    }    
    $file = isset($trace[$idx]["file"]) ? basename($trace[$idx]["file"]) : "";
    $line = isset($trace[$idx]["line"]) ? $trace[$idx]["line"] : "";
    $func = isset($trace[$idx]["function"]) ? $trace[$idx]["function"] : "";    
  }
  error_log("[$file:$line][$func]: ".$msg);
}
