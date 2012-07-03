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


include "PuppetInvoker.php";
include_once "PuppetConfigs.php";
include_once "../db/Transaction.php";

$nodesToKick = array ("hrt9n22.cc1.ygridcore.net",
                      "hrt9n27.cc1.ygridcore.net",
                      "hrt9n34.cc1.ygridcore.net",
                      "hrt9n35.cc1.ygridcore.net",
                      "hrt9n36.cc1.ygridcore.net",
                      "hrt9n37.cc1.ygridcore.net",
                      "hrt10n27.cc1.ygridcore.net",
                      "hrt22n44.cc1.ygridcore.net",
                      "hrt22n43.cc1.ygridcore.net",
                      "hrt8n38.cc1.ygridcore.net");

$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";
$GLOBALS["puppetManifestDir"] = "/tmp";
$GLOBALS["puppetKickVersionFile"] = "/tmp/kickversion.txt";
$GLOBALS["puppetReportsDir"] = "/tmp/reports";
$GLOBALS["DRYRUN"] = TRUE;
$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::TRACE;
$invoker = new PuppetInvoker("./test.db");
$hostRolesToKick = array("HBASE_MASTER" => "hrt8n38.cc1.ygridcore.net",
                         "NAGIOS_SERVER" => "hrt10n27.cc1.ygridcore.net",
                         "TEMPLETON_SERVER" => "hrt9n27.cc1.ygridcore.net");
$invoker->kickPuppet($nodesToKick, new Transaction("txn1", "subtxn2", "parentTxn3"), "DataCluster", $hostRolesToKick);

?>
