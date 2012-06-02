<?php

$INSTALL_ROOT = "/usr/share/hmc/";

$GLOBALS["HMC_CLUSTER_PATH"] = "/var/run/hmc/";
$GLOBALS["BACKGROUND_EXECUTOR_PATH"] = $INSTALL_ROOT . "/php/util/BackgroundExecutor.php";
$GLOBALS["PHP_EXEC_PATH"] = "/usr/bin/php";
$GLOBALS["CLUSTERMAIN_PATH"] = $INSTALL_ROOT . "/php/orchestrator/ClusterMain.php";
$GLOBALS["DB_PATH"] = "/var/db/hmc/data/data.db";

$GLOBALS["puppetMaxParallelKicks"] = 10;
$GLOBALS["puppetManifestDir"] = "/etc/puppet/master/modules/catalog/files/";
$GLOBALS["puppetKickVersionFile"] = "/var/lib/puppet/puppet_kick_version.txt";
$GLOBALS["puppetReportsDir"] = "/var/lib/puppet/reports";
$GLOBALS["puppetKickTimeout"] = 180;
$GLOBALS["puppetModulesDirectory"] = "/etc/puppet/agent/modules";
$GLOBALS["manifestloaderFile"] = "/etc/puppet/master/manifestloader/site.pp";
$GLOBALS["manifestloaderDestinationDir"] = "/etc/puppet/master/manifests";
$GLOBALS["puppet_install"] = "/usr/share/hmc/ShellScripts/puppet_agent_install.sh";
$GLOBALS["DRYRUN"] = FALSE;

$GLOBALS["HMC_LOG_DIR"] = "/var/log/hmc";
$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
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
