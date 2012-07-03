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

    function __construct() {
       $this->logger = new HMCLogger("RoleDependencies");
       $this->classDependencies = new PuppetClassDependencies();
	     $this->sortedRoles = array (
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

      $this->serviceCheckRoles = array (
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
