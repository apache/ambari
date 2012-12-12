#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
class hdp-sqoop::params() inherits hdp::params
{
  $conf_dir = $hdp::params::sqoop_conf_dir

  $hbase_home = hdp_default("hbase_home","/usr")
  $hive_home = hdp_default("hive_home","/usr")
  $zoo_conf_dir = $hdp::params::zk_conf_dir 
  $sqoop_lib = hdp_default("sqoop_lib","/usr/lib/sqoop/lib/") #TODO: should I remove and just use sqoop_dbroot
  $keytab_path = hdp_default("keytab_path","/etc/security/keytabs")
}
