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

class hdp-hue::params() inherits hdp::params {

  ## Global configuration properties

  $hue_conf_file = "${hdp::params::hue_conf_dir}/hue.ini"
  $hue_pid_dir = hdp_default("hue_pid_dir", "/var/run/hue")
  $hue_log_dir = hdp_default("hue_log_dir", "/var/log/hue")

  # Other properties - not exposed

  $hue_hadoop_home = $hdp::params::hadoop_lib_home
  $hue_hadoop_mapred_home = $hue_hadoop_home
  $security_enabled = $hdp::params::security_enabled
  $hue_hive_conf_dir = $hdp::params::hive_conf_dir
  $hue_pig_java_home = $hdp::params::java64_home

  # All non-global properties

  if has_key($configuration, 'hue-site') {
    $hue-site = $configuration['hue-site']

    # Hadoop Configuration properties

    $hue_hadoop_fs_defaultfs = $hue-site["fs_defaultfs"]
    $hue_hadoop_webhdfs_url = $hue-site["webhdfs_url"]
    $hue_hadoop_jt_host = $hue-site["jobtracker_host"]
    $hue_hadoop_jt_port = $hue-site["jobtracker_port"]
    $hue_hive_home_dir = $hue-site["hive_home_dir"]
    $hue_templeton_url = $hue-site["templeton_url"]

    # Database Configuration properties

    $hue_db_engine = $hue-site["db_engine"]
    $hue_db_port = $hue-site["db_port"]
    $hue_db_host = $hue-site["db_host"]
    $hue_db_user = $hue-site["db_user"]
    $hue_db_password = $hue-site["db_password"]
    $hue_db_name = $hue-site["db_name"]

    # Hue Email Configuration properties

    $hue_smtp_host = $hue-site["smtp_host"]
    $hue_smtp_port = $hue-site["smtp_port"]
    $hue_smtp_user = $hue-site["smtp_user"]
    $hue_smtp_password = $hue-site["smtp_password"]
    $hue_smtp_tls = $hue-site["tls"]
    $hue_default_from_email = $hue-site["default_from_email"]

    # Hue Configuration properties

    $hue_debug_messages = $hue-site["send_debug_messages"]
    $hue_database_logging = $hue-site["database_logging"]
    $hue_secret_key = $hue-site["secret_key"]
    $hue_http_host = $hue-site["http_host"]
    $hue_http_port = $hue-site["http_port"]
    $hue_time_zone = $hue-site["time_zone"]
    $hue_django_debug_mode = $hue-site["django_debug_mode"]
    $hue_use_cherrypy_server = $hue-site["use_cherrypy_server"]
    $hue_http_500_debug_mode = $hue-site["http_500_debug_mode"]
    $hue_server_user = $hue-site["server_user"]
    $hue_server_group = $hue-site["server_group"]
    $hue_backend_auth_policy = $hue-site["backend_auth"]

    $hue_hadoop_yarn_host = $hue-site["resourcemanager_host"]
    $hue_hadoop_yarn_port = $hue-site["resourcemanager_port"]

    # Shell Configuration properties

    $hue_pig_shell_command = $hue-site["pig_shell_command"]
    $hue_hbase_nice_name = $hue-site["hbase_nice_name"]
    $hue_hbase_shell_command = $hue-site["hbase_shell_command"]
    $hue_bash_nice_name = $hue-site["bash_nice_name"]
    $hue_bash_shell_command = $hue-site["bash_shell_command"]

    $hue_whitelist = $hue-site["whitelist"]

    # Security Configuration properties

    $hue_keytab_path = $hue-site["hue_keytab"]
    $hue_principal = $hue-site["hue_principal"]

  } else {
    ##TODO: Temporary intialization
    # Hue Configuration properties

    $hue_debug_messages = "1"
    $hue_database_logging = "0"
    $hue_secret_key = "ThisisusedforsecurehashinginthesessionstoreSetthistoarandomstringthelongerthebetter"
    $hue_http_host = "0.0.0.0"
    $hue_http_port = "8000"
    $hue_time_zone = "America/Los_Angeles"
    $hue_django_debug_mode = "1"
    $hue_use_cherrypy_server = "false"
    $hue_http_500_debug_mode = "1"
    $hue_server_user = "sandbox"
    $hue_server_group = "sandbox"
    $hue_backend_auth_policy = "desktop.auth.backend.AllowAllBackend"

    $hue_hadoop_jt_host = hdp_default("jtnode_host")
    $webhcat_server_host = hdp_default("webhcat_server_host")
    $hue_templeton_url = "http://${webhcat_server_host}:50111/templeton/v1"
    $hue_hive_home_dir = "/usr/lib/hive"
    $hue_hadoop_yarn_host = ""
    $hue_hadoop_yarn_port = ""

    # Shell Configuration properties

    $hue_pig_shell_command = "/usr/bin/pig -l /dev/null"
    $hue_hbase_nice_name = "HBase Shell"
    $hue_hbase_shell_command = "/usr/bin/hbase shell"
    $hue_bash_nice_name = "Bash (Test only!!!)"
    $hue_bash_shell_command = "/bin/bash"

    $hue_whitelist = "(localhost|127\\.0\\.0\\.1):(${jtnode_port}|${namenode_port}|${tasktracker_port}|${datanode_port}|${jobhistory_port})"
  }

}