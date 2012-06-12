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

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once "selectNodes.php";

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";

function testselectNodes($clustername) {
  $selectNodes = new SelectNodes();
  $ret = $selectNodes->selectNodes("DataCluster", new HMCDBAccessor("./test.db"));
  print_r($ret);
  $ret = $selectNodes->getExcludeHosts(array("x", "y", "z"), array("x", "y", "z"));
  print_r($ret);

}

function updateDB($clustername) {
  $selectNodes = new SelectNodes();
  $masterToHostMap = array("NAMENODE" => "hrt9n22.cc1.ygridcore.net",
                           "SNAMENODE" => "hrt9n34.cc1.ygridcore.net",
                           "JOBTRACKER" => "hrt9n35.cc1.ygridcore.net");
  $ret = $selectNodes->updateDBWithRoles("DataCluster", new HMCDBAccessor("./test.db"), $masterToHostMap);
  print_r($ret);
  $ret = $selectNodes->getExcludeHosts(array("x", "y", "z"), array("x", "y", "z"));
  print_r($ret);

}


//testSelectNodes("tmp");
updateDB("tmp");
?>
