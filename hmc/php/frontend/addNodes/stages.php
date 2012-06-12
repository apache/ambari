<?php

$stagesInfo = array(
    "findSshableNodes" => array (
      "description" => "Finding reachable nodes",
      "scriptName" => "./addNodes/findSshableNodes.php",
      ),
    "obtainNodesInfo" => array(
      "description" => "Obtaining information about reachable nodes",
      "scriptName" => "./addNodes/obtainNodesInfo.php",
      ),
    "verifyAndUpdateNodesInfo" => array(
      "description" => "Verifying and updating node information",
      "scriptName" => "./addNodes/verifyAndUpdateNodesInfo.php",
    ),
    "bootstrapNodes" => array(
      "description" => "Preparing discovered nodes",
      "scriptName" => "./addNodes/bootstrap.php",
      ),
    "finalizeNodes" => array(
      "description" => "Finalizing bootstrapped nodes",
      "scriptName" => "./addNodes/finalizeNodes.php",
      )
    );
?>
