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

function runStage($nodesToSendKick, $_txnId, $_subTxnId, $clusterId, $hostsForSitepp,
    $configsForSitepp, $hostRolesForSitepp, $sitePPDir, $versionFile, $reportDir, $dryRun) {
  $db = "../db/test.db";
  $invoker = new PuppetInvoker($db);
  $txnId = $_txnId . "-" . $_subTxnId;
  $results = $invoker->genKickWait($nodesToSendKick, $txnId, $clusterId, $hostsForSitepp,
      $configsForSitepp, $hostRolesForSitepp, $sitePPDir, $versionFile, $dryRun);
  system("cp " . $sitePPDir . "/site.pp ".  $reportDir . "/" . $_subTxnId . "-site.pp");
  system("cp " . $versionFile . " ".  $reportDir . "/" . $_subTxnId . "-kick-version.txt");
  if (!$dryRun) {
    foreach($results as $key => $val) {
      echo "$key" . ": ";
      foreach($val as $n) {
        echo $n . ",";
      }
      echo "\n";
    }

    $failedNodes = $results["failed"];
    if (!empty($failedNodes)) {
      echo "Getting Reports from failed nodes..." . "\n";
      $reports = $invoker->getReports($failedNodes, $_txnId, $_subTxnId);
      foreach ($failedNodes as $n) {
        if (isset($reports[$n])) {
          echo "Report from node " . $n . ":\n";
        } else {
          echo "No report obtained from " . $n . "\n";
        }
        if (isset($reports[$n]["overall"])) {
          echo "summary: " . $reports[$n]["overall"] . "\n" ;
        }
        if (isset($reports[$n]["message"])) {
          echo "logs: " . $reports[$n]["message"] . "\n" ;
        }
        if (isset($reports[$n]["finishtime"])) {
          echo "Finish time: " . $reports[$n]["finishtime"] . "\n" ;
        }
        echo "\n\n";
      }
    }

  }
}

$__configInfo__ = array(
    "mapred_user" => "hrt_mr",
    "hdfs_user" => "hrt_hdfs",
    "dfs_data_dir" => "/grid/0/hdp/hdfs/data",
    "dfs_name_dir" => "/grid/0/hdp/hdfs/name",
    "dfs_replication" => "3",
    "mapred_local_dir" => "/grid/0/hdp/mapred/local",
    "hadoop_logdirprefix" => "/grid/0/var/log/hadoop",
    "hadoop_piddirprefix" => "/grid/0/var/run/hadoop",
    "zk_user" => "zookeeper",
    "zk_log_dir" => "/grid/0/var/log/zookeeper",
    "zk_data_dir" => "/grid/0/hdp/zookeeper",
    "zk_pid_dir" => "/grid/0/var/run/zookeeper",
    "hbase_user" => "hbase",
    "hbase_log_dir" => "/grid/0/var/log/hbase",
    "hbase_pid_dir" => "/grid/0/var/run/hbase",
    "hcat_user" => "hcat",
    "hcat_database_name" => "puppetdb",
    "hcat_metastore_user_name" => "puppetuser",
    "hcat_metastore_user_passwd" => "puppetpswd",
    "hcat_logdirprefix" => "/grid/0/var/log/hcat",
    "hcat_piddirprefix" => "/grid/0/var/run/hcat",
    "oozie_user" => "oozie",
    "oozie_log_dir" => "/var/log/oozie",
    "oozie_pid_dir" => "/var/run/oozie",
    "oozie_data_dir" => "/var/data/oozie",
    "templeton_user" => "templeton",
    "templeton_log_dir" => "/var/log/templeton",
    "templeton_pid_dir" => "/var/run/templeton",
    );

$__hostNames__ = array (
    "namenode_host" => "hrt9n22.cc1.ygridcore.net",
    "jtnode_host" => "hrt9n37.cc1.ygridcore.net",
    "snamenode_host" => "hrt9n21.cc1.ygridcore.net",
    "zookeeper_hosts" => array('hrt9n34.cc1.ygridcore.net'),
    "hbase_master_host" => "hrt9n34.cc1.ygridcore.net",
    "gateway_host" => "hrt9n38.cc1.ygridcore.net",
    "slave_hosts" => array('hrt9n34.cc1.ygridcore.net','hrt9n35.cc1.ygridcore.net','hrt9n36.cc1.ygridcore.net'),
    "hbase_rs_hosts" => array('hrt9n35.cc1.ygridcore.net','hrt9n36.cc1.ygridcore.net','hrt9n37.cc1.ygridcore.net')
    );

$nodesToKick = array ("hrt9n22.cc1.ygridcore.net",
    "hrt9n34.cc1.ygridcore.net",
    "hrt9n35.cc1.ygridcore.net",
    "hrt9n36.cc1.ygridcore.net",
    "hrt9n37.cc1.ygridcore.net");

$_dryRun = false;
if (isset($argv[1]) && ($argv[1] == "dry")) {
  $_dryRun = true;
}

echo "Running stage 0..." . "\n";

$__hostRoleStates__install_conf = array (
    "hrt9n22.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::namenode" => array("service_state" => "installed_and_configured"),
      ),
    "hrt9n34.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "installed_and_configured"),
      "hdp-hadoop::tasktracker" => array("service_state" => "installed_and_configured"),
      ),
    "hrt9n35.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "installed_and_configured"),
      "hdp-hadoop::tasktracker" => array("service_state" => "installed_and_configured"),
      ),
    "hrt9n36.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "installed_and_configured"),
      "hdp-hadoop::tasktracker" => array("service_state" => "installed_and_configured"),
      ),
    "hrt9n37.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::jobtracker" => array("service_state" => "installed_and_configured"),
      ),
    );

runStage($nodesToKick, "txn1", "subtxn1", "cluster1", $__hostNames__,
    $__configInfo__, $__hostRoleStates__install_conf, "/etc/puppet/master/manifests/",
    "/var/lib/puppet/puppet_kick_version.txt", "/var/lib/puppet/reports", $_dryRun);

echo "\nPress any key for next stage \n";
fgetc(STDIN);
echo "Running Stage 1..." . "\n";

$__hostRoleStates__ = array (
    "hrt9n22.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::namenode" => array("service_state" => "running", "format" => "true"),
      )
    );
$nodesToKick = array ("hrt9n22.cc1.ygridcore.net");

runStage($nodesToKick, "txn1", "subtxn2", "cluster1", $__hostNames__,
    $__configInfo__, $__hostRoleStates__, "/etc/puppet/master/manifests/",
    "/var/lib/puppet/puppet_kick_version.txt", "/var/lib/puppet/reports", $_dryRun);

echo "\nPress any key for next stage \n";
fgetc(STDIN);
echo "Running Stage 2..." . "\n";

$__hostRoleStates__stage2 = array (
    "hrt9n22.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::namenode" => array("service_state" => "running", "format" => "true"),
      ),
    "hrt9n34.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      ),
    "hrt9n35.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      ),
    "hrt9n36.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      )
    );

$nodesToKick = array ("hrt9n22.cc1.ygridcore.net",
    "hrt9n34.cc1.ygridcore.net",
    "hrt9n35.cc1.ygridcore.net",
    "hrt9n36.cc1.ygridcore.net");

runStage($nodesToKick, "txn1", "subtxn3", "cluster1", $__hostNames__,
    $__configInfo__, $__hostRoleStates__stage2, "/etc/puppet/master/manifests/",
    "/var/lib/puppet/puppet_kick_version.txt", "/var/lib/puppet/reports", $_dryRun);

echo "\nPress any key for next stage \n";
fgetc(STDIN);
echo "Running Stage 3..." . "\n";

$__hostRoleStates__stage3 = array (
    "hrt9n22.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::namenode" => array("service_state" => "running", "format" => "true"),
      ),
    "hrt9n34.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      ),
    "hrt9n35.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      ),
    "hrt9n36.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      ),
    "hrt9n37.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::jobtracker" => array("service_state" => "running"),
      )
    );

$nodesToKick = array ("hrt9n22.cc1.ygridcore.net",
    "hrt9n34.cc1.ygridcore.net",
    "hrt9n35.cc1.ygridcore.net",
    "hrt9n36.cc1.ygridcore.net",
    "hrt9n37.cc1.ygridcore.net");

runStage($nodesToKick, "txn1", "subtxn4", "cluster1", $__hostNames__,
    $__configInfo__, $__hostRoleStates__stage3, "/etc/puppet/master/manifests/",
    "/var/lib/puppet/puppet_kick_version.txt", "/var/lib/puppet/reports", $_dryRun);

echo "\nPress any key for next stage \n";
fgetc(STDIN);
echo "Running Stage 4..." . "\n";

$__hostRoleStates__stage4 = array (
    "hrt9n22.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::namenode" => array("service_state" => "running", "format" => "true"),
      ),
    "hrt9n34.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      "hdp-hadoop::tasktracker" => array("service_state" => "running"),
      ),
    "hrt9n35.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      "hdp-hadoop::tasktracker" => array("service_state" => "running"),
      ),
    "hrt9n36.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::datanode" => array("service_state" => "running"),
      "hdp-hadoop::tasktracker" => array("service_state" => "running"),
      ),
    "hrt9n37.cc1.ygridcore.net" =>  array (
      "hdp-hadoop::jobtracker" => array("service_state" => "running"),
      )
    );

runStage($nodesToKick, "txn1", "subtxn5", "cluster1", $__hostNames__,
    $__configInfo__, $__hostRoleStates__stage4, "/etc/puppet/master/manifests/",
    "/var/lib/puppet/puppet_kick_version.txt", "/var/lib/puppet/reports", $_dryRun);
?>
