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


$INSTALL_ROOT = "/usr/share/hmc/";

$GLOBALS["HMC_CLUSTER_PATH"] = "/var/run/hmc/";
$GLOBALS["BACKGROUND_EXECUTOR_PATH"] = $INSTALL_ROOT . "/php/util/BackgroundExecutor.php";
$GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
$GLOBALS["CLUSTERMAIN_PATH"] = $INSTALL_ROOT . "/php/orchestrator/ClusterMain.php";
$GLOBALS["DB_PATH"] = "/var/db/hmc/data/data.db";

$GLOBALS["puppetMaxParallelKicks"] = 10;
$GLOBALS["puppetMasterModulesDirectory"] = "/etc/puppet/master/modules";
$GLOBALS["puppetKickVersionFile"] = "/var/lib/puppet/puppet_kick_version.txt";
$GLOBALS["puppetReportsDir"] = "/var/lib/puppet/reports";
$GLOBALS["puppetKickTimeout"] = 120;
$GLOBALS["puppetAgentModulesDirectory"] = "/etc/puppet/agent/modules";
$GLOBALS["manifestloaderDir"] = "/etc/puppet/master/manifestloader";
$GLOBALS["manifestloaderDestinationDir"] = "/etc/puppet/master/manifests";
$GLOBALS["puppet_install"] = "/usr/share/hmc/ShellScripts/puppet_agent_install.sh";
$GLOBALS["DRYRUN"] = FALSE;

$GLOBALS["HMC_LOG_DIR"] = "/var/log/hmc";
$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::INFO;
$GLOBALS["HMC_LOG_FILE"] = $GLOBALS["HMC_LOG_DIR"] . "/hmc.log";

/*
// TODO fix - currently for testing only
$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "/tmp/orchestrator-test.log";
$GLOBALS["HMC_CLUSTER_PATH"] = "/var/run/hmc/";
*/

/*
For testHMC layer
$GLOBALS["puppetManifestDir"] = "/etc/puppet/master/manifests/";
$GLOBALS["puppetKickVersionFile"] = "/var/lib/puppet/puppet_kick_version.txt";
$GLOBALS["puppetReportsDir"] = "/var/lib/puppet/reports/";
$GLOBALS["puppetKickTimeout"] = 60;

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "/tmp/orchestrator-test.log";
$GLOBALS["BACKGROUND_EXECUTOR_PATH"] = "../util/BackgroundExecutor.php";
$GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
$GLOBALS["CLUSTERMAIN_PATH"] = "./ClusterMain.php";
$GLOBALS["DRYRUN"] = TRUE;
*/

?>
