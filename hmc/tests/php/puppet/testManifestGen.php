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


include "genmanifest/generateManifest.php";

$__configInfo__ = array(
              "mapred_user" => "hrt_mr",
              "hdfs_user" => "hrt_hdfs",
              "dfs_data_dir" => "/grid/0/hdp/hdfs/data,/grid/1/hdp/hdfs/data,/grid/2/hdp/hdfs/data,/grid/3/hdp/hdfs/data",
              "dfs_name_dir" => "/grid/0/hdp/hdfs/name",
              "dfs_replication" => "3",
              "mapred_local_dir" => "/grid/0/hdp/mapred/local,/grid/1/hdp/mapred/local,/grid/2/hdp/mapred/local,/grid/3/hdp/mapred/local",
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
                "namenode_host" => "hrt9n37.cc1.ygridcore.net",
                "jtnode_host" => "hrt9n22.cc1.ygridcore.net",
                "snamenode_host" => "hrt9n21.cc1.ygridcore.net",
                "zookeeper_hosts" => "['hrt9n24.cc1.ygridcore.net','hrt9n25.cc1.ygridcore.net','hrt9n26.cc1.ygridcore.net']",
                "hbase_master_host" => "hrt9n35.cc1.ygridcore.net",
                "hcat_server_host" => "hrt9n21.cc1.ygridcore.net",
                "hcat_mysql_host" => "hrt6n04.cc1.ygridcore.net",
                "oozie_server" => "hrt9n31.cc1.ygridcore.net",
                "templeton_server" => "hrt9n30.cc1.ygridcore.net",
                "gateway_host" => "hrt9n38.cc1.ygridcore.net",
                "dashboard_host" => "hrt9n38.cc1.ygridcore.net",
                "nagios_server_host" => "hrt9n38.cc1.ygridcore.net",
                "ganglia_server_host" => "hrt9n38.cc1.ygridcore.net",
                "slave_hosts" => array('hrt9n23.cc1.ygridcore.net','hrt9n24.cc1.ygridcore.net','hrt9n25.cc1.ygridcore.net','hrt9n26.cc1.ygridcore.net','hrt9n27.cc1.ygridcore.net','hrt9n28.cc1.ygridcore.net','hrt9n30.cc1.ygridcore.net','hrt9n32.cc1.ygridcore.net','hrt9n33.cc1.ygridcore.net','hrt9n34.cc1.ygridcore.net','hrt9n35.cc1.ygridcore.net','hrt9n36.cc1.ygridcore.net'),
                "hbase_rs_hosts" => "['hrt9n23.cc1.ygridcore.net','hrt9n24.cc1.ygridcore.net','hrt9n25.cc1.ygridcore.net','hrt9n26.cc1.ygridcore.net','hrt9n27.cc1.ygridcore.net','hrt9n28.cc1.ygridcore.net','hrt9n30.cc1.ygridcore.net','hrt9n32.cc1.ygridcore.net','hrt9n33.cc1.ygridcore.net','hrt9n34.cc1.ygridcore.net']"
       );

$__hostRoleStates__ =
       array (
           "hrt8n36.cc1.ygridcore.net" =>  array (
              "hdp-hadoop::namenode" => array("service_state" => "running"),
              "hdp-zookeeper" => array("service_state" => "running"),
              "hdp-hbase::master" => array("service_state" => "running")
           ),
           "hrt8n37.cc1.ygridcore.net" =>  array (
              "hdp-hadoop::namenode" => array("service_state" => "running"),
              "hdp-zookeeper" => array("service_state" => "running"),
              "hdp-hbase::master" => array("service_state" => "running")
           )
       );

ManifestGenerator::generateManifest("/tmp", $__hostNames__, $__configInfo__, $__hostRoleStates__);

?>
