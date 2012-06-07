<?php

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

?>
