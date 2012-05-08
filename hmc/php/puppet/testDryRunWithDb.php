<?php

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
                      "hrt8n38.cc1.ygridcore.net");

$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";
$GLOBALS["puppetManifestDir"] = "/tmp";
$GLOBALS["puppetKickVersionFile"] = "/tmp/kickversion.txt";
$GLOBALS["puppetReportsDir"] = "/tmp/reports";
$GLOBALS["DRYRUN"] = TRUE;
$invoker = new PuppetInvoker("./test.db");
$hostRolesToKick = array("HBASE_MASTER" => "hrt8n38.cc1.ygridcore.net",
                         "NAGIOS_SERVER" => "hrt10n27.cc1.ygridcore.net",
                         "TEMPLETON_SERVER" => hrt9n27.cc1.ygridcore.net);
$invoker->kickPuppet($nodesToKick, new Transaction("txn1", "subtxn2", "parentTxn3"), "DataCluster", $hostRolesToKick);

?>
