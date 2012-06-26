<?php
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

//The script takes the first argument as the path of sqlite database
$db = new PDO("sqlite:".$argv[1]);


/***
 * Adding additional parameters on UI front for HBase
 */

// maps to hstore_compactionthreshold
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hstore_compactionthreshold\", \"3\", \"HBase HStore compaction threshold\", \"If more than this number of HStoreFiles in any one HStore then a compaction is run to rewrite all HStoreFiles files as one.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"int\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hfile_blockcache_size
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hfile_blockcache_size\", \"0.25\", \"HFile block cache size \", \"Percentage of maximum heap (-Xmx setting) to allocate to block cache used by HFile/StoreFile. Set to 0 to disable but it's not recommended.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"int\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hstorefile_maxsize.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hstorefile_maxsize\", \"1073741824\", \"Maximum HStoreFile Size\", \"If any one of a column families' HStoreFiles has grown to exceed this value, the hosting HRegion is split in two.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"bytes\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to regionserver_handlers.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"regionserver_handlers\", \"30\", \"HBase Region Server Handler\", \"Count of RPC Listener instances spun up on RegionServers\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"int\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hregion_majorcompaction.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hregion_majorcompaction\", \"86400000\", \"HBase Region Major Compaction\", \"The time between major compactions of all HStoreFiles in a region. Set to 0 to disable automated major compactions.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"ms\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hregion_blockmultiplier.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hregion_blockmultiplier\", \"2\", \"HBase Region Block Multiplier\", \"Block updates if memstore has \"\"Multiplier * HBase Region Memstore Flush Size\"\" bytes. Useful preventing runaway memstore during spikes in update traffic\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"int\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hregion_memstoreflushsize.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hregion_memstoreflushsize\", \"134217728\", \"HBase Region Memstore Flush Size\", \"Memstore will be flushed to disk if size of the memstore exceeds this number of bytes.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"bytes\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to client_scannercaching.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"client_scannercaching\", \"100\", \"HBase Client Scanner Caching\", \"Number of rows that will be fetched when calling next on a scanner if it is not served from (local, client) memory. Do not set this value such that the time between invocations is greater than the scanner timeout\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"int\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to zookeeper_sessiontimeout.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"zookeeper_sessiontimeout\", \"60000\", \"Zookeeper timeout for HBase Session\", \"HBase passes this to the zk quorum as suggested maximum time for a session\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"ms\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));

// maps to hfile_max_keyvalue_size.
$query = "INSERT OR REPLACE INTO \"ConfigProperties\" ( key, default_value, display_name, description, service_name, display_type, display_attributes ) VALUES ( \"hfile_max_keyvalue_size\", \"10485760\", \"HBase Client Maximum key-value Size\", \"Specifies the combined maximum allowed size of a KeyValue instance. It should be set to a fraction of the maximum region size.\", \"HBASE\" , \"\", '{ \"isPassword\": false, \"noDisplay\": false, \"reconfigurable\": true, \"displayType\": \"text\", \"unit\":\"bytes\" }' )";
$db->exec($query) or die(print_r($db->errorInfo(), true));


/***
 * changes the display on UI from "HIVE SERVER to HIVE METASTORE"
 */

$query = "UPDATE ServiceComponents SET display_name = 'Hive Metastore' WHERE service_name = 'HIVE' AND component_name = 'HIVE_SERVER'"; 
$db->exec($query) or die(print_r($db->errorInfo(), true));
?>

