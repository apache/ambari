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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";

system("rm -rf ./test.db");
system("rm -rf ./hmc.log");

assert_options(ASSERT_BAIL, 1);

system("sqlite3 ./test.db < ../../../db/schema.dump");

$db = new HMCDBAccessor("./test.db");

print "Creating Cluster - DataCluster\n";
$clusterName = "DataCluster";
$version = "1.0.0";
$state = "";
$result = $db->createCluster($clusterName, $version, $state);
assert(is_array($result) && isset($result["clusterName"]));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Add Hosts To Cluster\n";
print "Adding hrt9n22.cc1.ygridcore.net\n";
print "Adding hrt9n34.cc1.ygridcore.net\n";
print "Adding hrt9n35.cc1.ygridcore.net\n";
print "Adding hrt9n36.cc1.ygridcore.net\n";
print "Adding hrt9n37.cc1.ygridcore.net\n";

$hosts = array (
  array (
        "hostName" => "hrt9n22.cc1.ygridcore.net",
        "ip" => "127.0.0.1",
        "totalMem" => 64,
        "cpuCount" => 4,
        "osArch" => "x86_64",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n34.cc1.ygridcore.net",
        "ip" => "127.0.0.2",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n35.cc1.ygridcore.net",
        "ip" => "127.0.0.3",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n36.cc1.ygridcore.net",
        "ip" => "127.0.0.4",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n37.cc1.ygridcore.net",
        "ip" => "127.0.0.5",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  )
);

$result = $db->addHostsToCluster($clusterName, $hosts);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 5);

print "Add Services To Cluster\n";
$services = array(
   array (
     "serviceName" => "HDFS",
     "state" => "UNINSTALLED",
     "isEnabled" => TRUE
  ),
  array (
     "serviceName" => "MAPREDUCE",
     "state" => "UNINSTALLED",
     "isEnabled" => TRUE
  ),
  array (
     "serviceName" => "HBASE",
     "state" => "UNKNOWN",
     "isEnabled" => FALSE
  )
);

$result = $db->addServicesToCluster($clusterName, $services);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Add Service Components To Cluster\n";
$comps = array (
   "HDFS" => array (
       "NAMENODE" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "DATANODE" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "SNAMENODE" => array( "state" => "UNINSTALLED", "desiredState" => "")
      ),
   "MAPREDUCE" => array (
       "JOBTRACKER" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "TASKTRACKER" => array( "state" => "UNINSTALLED", "desiredState" => "")
      )
);
$result = $db->addServiceComponentsToCluster($clusterName, $comps);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Map Hosts to Roles\n";

/*
$mapping = array (
   "NAMENODE" => array ("hrt9n22.cc1.ygridcore.net"),
   "SNAMENODE" => array ("hrt9n22.cc1.ygridcore.net"),
   "DATANODE" => array ("hrt9n34.cc1.ygridcore.net",
                        "hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net"),
   "JOBTRACKER" => array ("hrt9n37.cc1.ygridcore.net"),
   "TASKTRACKER" => array ("hrt9n34.cc1.ygridcore.net",
                        "hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net")
);

foreach ($mapping as $comp => $hosts) {
  $db->addHostsToComponent($clusterName, $comp, $hosts, "", "");
  assert(is_array($result));
  assert($result["result"] == 0);
  assert($result["error"] == "");
}
*/
print "Add Service Configs\n";
$query = "INSERT OR REPLACE INTO ServiceConfig ( cluster_name, key, value ) "
    . " SELECT \"" . $clusterName . "\" , key, default_value "
    . " FROM ConfigProperties ";

$command = "sqlite3 ./test.db '" . $query . "'";
system($command);

print "Verify Service Config Defaults Added to DB\n";
$result = $db->getServiceConfig($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["properties"])
       && count($result["properties"]) > 0);



?>
