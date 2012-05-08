<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';
include_once './uninstall/uninstallUtil.php';

$logger = new HMCLogger("CleanupProcesses");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

function getCommandLine() {
  $cmdLine = "touch /etc/default/hadoop-env.sh ; killall puppet; killall java; killall jsvc; rm -rf /grid/?/hadoop/*;  rm -rf /var/log/puppet_agent.log; yum erase -y zookeeper* oozie* sqoop* pig* hbase* hadoop* hadoop-conf-pseudo* hdp_mon_dashboard; rm -rf /etc/hadoop/; rm -rf /etc/hbase; rm -rf /etc/oozie; rm -rf /etc/pig; rm -rf /etc/zookeeper; rm -rf /etc/sqoop ; rm -rf /etc/hive ; rm -rf /etc/hcatalog; rm -rf /etc/yum.repos.d/hdp* ; rm -rf /etc/yum.repos.d/cloudera* ; rm -rf /etc/yum.repos.d/cdh* ; rm -rf /var/run/hbase; rm -rf /var/run/zookeeper /var/run/hadoop /var/run/oozie /var/run/templeton /var/run/hive";
  $cmdLine = $cmdLine . 'sleep $[ $RANDOM % 5 ]; ';
  return $cmdLine;
}

$clusterName = $argv[1];
$deployUser = $argv[2];
$rootTxnId = $argv[3];
$mySubTxnId = $argv[4];
$parentSubTxnId = $argv[5];
$hostsStr = $argv[6];
// stage name should match corr. stage in stages.php
$stageName = "CleanupProcesses";

$cmdLine = getCommandLine();

handleUninstallTransaction($clusterName, $deployUser, $rootTxnId,
  $mySubTxnId, $parentSubTxnId, $hostsStr,
  $stageName, $cmdLine, $dbAccessor, $logger);

?>
