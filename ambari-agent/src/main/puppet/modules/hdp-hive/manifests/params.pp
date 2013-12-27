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
class hdp-hive::params() inherits hdp::params
{

  #TODO: will move to globals
  $hive_metastore_user_name = hdp_default("hive-site/javax.jdo.option.ConnectionUserName","dbusername")
  $hive_metastore_user_passwd = hdp_default("hive-site/javax.jdo.option.ConnectionPassword","dbpassword")
  $hive_server_conf_dir = hdp_default("hive_server_conf_dir", "/etc/hive/conf.server")
  $hive_jdbc_connection_url = hdp_default("hive-site/javax.jdo.option.ConnectionURL", "")

  ### users
  $hive_user = $hdp::params::hive_user
  
  ### JDBC driver jar name
  if ($hive_jdbc_driver == "com.mysql.jdbc.Driver"){
    $jdbc_jar_name = "mysql-connector-java.jar"
  } elsif ($hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver") {
    $jdbc_jar_name = "ojdbc6.jar"  
  }
  
  ### common
  $hive_metastore_port = hdp_default("hive_metastore_port",9083)
  $hive_lib = hdp_default("hive_lib","/usr/lib/hive/lib/") #TODO: should I remove and just use hive_dbroot
  $hive_var_lib = hdp_default("hive_var_lib","/var/lib/hive")  
  $hive_url = "jdbc:hive2://${hive_server_host}:10000"

  ### hive-env
  $hive_conf_dir = $hdp::params::hive_conf_dir

  $hive_dbroot = hdp_default("hive_dbroot",$hive_lib)

  $hive_log_dir = hdp_default("hive_log_dir","/var/log/hive")

  $hive_pid_dir = hdp_default("hive_pid_dir","/var/run/hive")
  $hive_pid = hdp_default("hive_pid","hive-server.pid")

  
  ### hive-site
  $hive_database_name = hdp_default("hive-site/hive.database.name","hive")

  if ($hdp::params::security_enabled == true) {
    $hive_metastore_sasl_enabled = true
  } else {
    $hive_metastore_sasl_enabled = false
  }

  $keytab_path = hdp_default("keytab_path","/etc/security/keytabs")
  $hive_metatore_keytab_path = hdp_default("hive-site/hive.metastore.kerberos.keytab.file","/etc/security/keytabs/hive.service.keytab")

  #TODO: using instead hive_server_host in hdp::params 
  #$hive_metastore_server_host = hdp_default("hive-site/hive.metastore.server.host")
  
  ###mysql connector
  $download_url = $hdp::params::gpl_artifacts_download_url
  $mysql_connector_url = "${download_url}/mysql-connector-java-5.1.18.zip"
  $hive_aux_jars_path =  hdp_default("hive_aux_jars_path","/usr/lib/hcatalog/share/hcatalog/hcatalog-core.jar")

  ##smoke test
  $smoke_test_sql_file = 'hiveserver2.sql'
  $smoke_test_script = 'hiveserver2Smoke.sh'

  ##Starting hiveserver2
  $start_hiveserver2_script = 'startHiveserver2.sh'

  ##Starting metastore
  $start_metastore_script = 'startMetastore.sh'
}
