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
