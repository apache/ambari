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


function generateNodeActionProgress ($numTotalNodes, $numNodesSucceeded, $numNodesFailed, $finalGoodState) {

  $numNodesInProgress = $numTotalNodes - $numNodesFailed - $numNodesSucceeded;

  if ($numTotalNodes == ($numNodesSucceeded + $numNodesFailed)) {
    // This means all nodes are done.
    if ($numTotalNodes == $numNodesSucceeded) {
      // Meaning all nodes have succeeded
      $progressString = ": All $numTotalNodes nodes $finalGoodState";
    } else if ($numNodesSucceeded != 0) {
      // Some succeeded, some failed
      $progressString = ": Of $numTotalNodes nodes, $numNodesSucceeded $finalGoodState and $numNodesFailed failed";
    } else {
      $progressString = ": All $numTotalNodes nodes failed!";
      // All nodes have failed
    }
  } else {
    // This means some nodes are in progress
    if ($numNodesFailed == 0) {
      // Some nodes in progress, no failed nodes
      if ($numNodesSucceeded != 0) {
        // Some in progress, some succeeded, no failed
        $progressString = ": $numNodesInProgress / $numTotalNodes in progress, $numNodesSucceeded $finalGoodState";
      } else {
        // All nodes are in progress
        $progressString = ": $numNodesInProgress / $numTotalNodes in progress";
      }
    } else {
      // Some in progress, some failed
      if ($numNodesSucceeded != 0) {
        // Some in progress, some succeeded and some failed
        $progressString = ": $numNodesInProgress / $numTotalNodes in progress; $numNodesSucceeded $finalGoodState and $numNodesFailed failed";
      } else {
        // Some in progress, some failed, none succeeded
        $progressString = ": $numNodesInProgress / $numTotalNodes in progress; $numNodesFailed failed";
      }
    }
  }

  return $progressString;
}

function convertToLowerCase($hosts) {
  $result = array();
  foreach($hosts as $host) {
    $lhost = strtolower($host);
    array_push($result, $lhost);
  }
  return $result;
}

function removeCarriageReturn($hostsFileDestination) {
  exec("sed -i -e 's/\r//g' ". $hostsFileDestination, $output, $retVal);
  if($retVal != 0) {
    $logger->log_error("shell command exec() removing '\r' from ". $hostsFileDestination . " file failed");
  }
}

?>
