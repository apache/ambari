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


  include_once "../util/Logger.php";
  include_once "PuppetClassDependencies.php";

  class RoleDependencies {

    private $sortedRoles;
    private $serviceCheckRoles;
    private $classDependencies;
    private $logger;

    private static $hadoop1_sortedRoles = array (
      "hdp",
      "hdp-hadoop::namenode",
      "hdp-hadoop::namenode::service_check",
      "hdp-hadoop::snamenode",
      "hdp-hadoop::datanode",
      "hdp-hadoop::client",
      "hdp-hadoop::hdfs::service_check",
      "hdp-hadoop::jobtracker",
      "hdp-hadoop::jobtracker::service_check",
      "hdp-hadoop::tasktracker",
      "hdp-hadoop::mapred::service_check",
      "hdp-zookeeper",
      "hdp-zookeeper::client",
      "hdp-zookeeper::zookeeper::service_check",
      "hdp-zookeeper::quorum::service_check",
      "hdp-hbase::master",
      "hdp-hbase::regionserver",
      "hdp-hbase::client",
      "hdp-hbase::hbase::service_check",
      "hdp-mysql::server",
      "hdp-hive::server",
      "hdp-hive::client",
      "hdp-hive::hive::service_check",
      "hdp-hcat",
      "hdp-hcat::hcat::service_check",
      "hdp-oozie::server",
      "hdp-oozie::client",
      "hdp-oozie::oozie::service_check",
      "hdp-pig",
      "hdp-pig::pig::service_check",
      "hdp-sqoop",
      "hdp-sqoop::sqoop::service_check",
      "hdp-templeton::server",
      "hdp-templeton::client",
      "hdp-templeton::templeton::service_check",
      "hdp-dashboard",
      "hdp-dashboard::dashboard::service_check",
      "hdp-nagios::server",
      "hdp-nagios::nagios::service_check",
      "hdp-ganglia::server",
      "hdp-ganglia::monitor",
      "hdp-ganglia::hdp-gmond::service_check",
      "hdp-ganglia::hdp-gmetad::service_check",
      "hdp-monitor-webserver",
    );

    private static $hadoop2_sortedRoles = array (
      "hdp2",
      "hdp2-hadoop::namenode",
      "hdp2-hadoop::namenode::service_check",
      "hdp2-hadoop::snamenode",
      "hdp2-hadoop::datanode",
      "hdp2-hadoop::client",
      "hdp2-hadoop::hdfs::service_check",
      "hdp2-hadoop::resourcemanager",
      "hdp2-hadoop::resourcemanager::service_check",
      "hdp2-hadoop::nodemanager",
      "hdp2-hadoop::mapred::service_check",
      "hdp2-zookeeper",
      "hdp2-zookeeper::client",
      "hdp2-zookeeper::zookeeper::service_check",
      "hdp2-zookeeper::quorum::service_check",
      "hdp2-hbase::master",
      "hdp2-hbase::regionserver",
      "hdp2-hbase::client",
      "hdp2-hbase::hbase::service_check",
      "hdp2-mysql::server",
      "hdp2-hive::server",
      "hdp2-hive::client",
      "hdp2-hive::hive::service_check",
      "hdp2-hcat",
      "hdp2-hcat::hcat::service_check",
      "hdp2-oozie::server",
      "hdp2-oozie::client",
      "hdp2-oozie::oozie::service_check",
      "hdp2-pig",
      "hdp2-pig::pig::service_check",
      "hdp-sqoop",
      "hdp-sqoop::sqoop::service_check",
      "hdp-templeton::server",
      "hdp-templeton::client",
      "hdp-templeton::templeton::service_check",
      "hdp-dashboard",
      "hdp-dashboard::dashboard::service_check",
      "hdp2-nagios::server",
      "hdp2-nagios::nagios::service_check",
      "hdp2-ganglia::server",
      "hdp2-ganglia::monitor",
      "hdp2-ganglia::hdp-gmond::service_check",
      "hdp2-ganglia::hdp-gmetad::service_check",
      "hdp-monitor-webserver",
    );

    private static $hadoop1_serviceCheckRoles = array (
      "HDFS" => "hdp-hadoop::hdfs::service_check",
      "MAPREDUCE" => "hdp-hadoop::mapred::service_check",
      "ZOOKEEPER" => "hdp-zookeeper::quorum::service_check",
      "HBASE" => "hdp-hbase::hbase::service_check",
      "PIG" => "hdp-pig::pig::service_check",
      "SQOOP" => "hdp-sqoop::sqoop::service_check",
      "HCATALOG" => "hdp-hcat::hcat::service_check",
      "HIVE" => "hdp-hive::hive::service_check",
      "OOZIE" => "hdp-oozie::oozie::service_check",
      "TEMPLETON" => "hdp-templeton::templeton::service_check",
    );

    private static $hadoop2_serviceCheckRoles = array (
      "HDFS2" => "hdp2-hadoop::hdfs::service_check",
      "YARN" => "hdp2-hadoop::mapred::service_check",
      "MAPREDUCE2" => "hdp2-hadoop::mapred::service_check",
      "ZOOKEEPER2" => "hdp2-zookeeper::quorum::service_check",
      "HBASE2" => "hdp2-hbase::hbase::service_check",
      "PIG2" => "hdp2-pig::pig::service_check",
      "SQOOP" => "hdp-sqoop::sqoop::service_check",
      "HCATALOG2" => "hdp2-hcat::hcat::service_check",
      "HIVE2" => "hdp2-hive::hive::service_check",
      "OOZIE2" => "hdp2-oozie::oozie::service_check",
      "TEMPLETON" => "hdp-templeton::templeton::service_check",
    );

    function __construct($hadoopStackVersion = AMBARI_HADOOP_1) {
       $this->logger = new HMCLogger("RoleDependencies");
       $this->classDependencies = new PuppetClassDependencies();
       $this->sortedRoles = ($hadoopStackVersion == AMBARI_HADOOP_2) ? self::$hadoop2_sortedRoles : self::$hadoop1_sortedRoles; 
       $this->serviceCheckRoles = ($hadoopStackVersion == AMBARI_HADOOP_2) ? self::$hadoop2_serviceCheckRoles : self::$hadoop1_serviceCheckRoles; 
       error_log ("hadoopStackVersion = $hadoopStackVersion");
    }

    public function getRolesStages($rolesList) {
      $stage=1;
      $rolesStages = array();
      foreach ($this->sortedRoles as $aRole) {
        if (in_array($aRole, $rolesList)) {
          $rolesStages[$aRole] = $stage;
          $stage ++;
        }
      }
      return $rolesStages;
    }

    public function getHostLevelDependencies($role, $serviceState) {
      return $this->classDependencies->getHostLevelDependencies($role, $serviceState);
    }

    public function getServiceCheckRole($service) {
      if (!isset($this->serviceCheckRoles[$service])) {
        return NULL;
      }
      return $this->serviceCheckRoles[$service];
    }
  }
?>
