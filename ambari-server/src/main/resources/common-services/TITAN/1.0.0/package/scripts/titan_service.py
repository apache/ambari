"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""
import os
from resource_management import *
from resource_management.libraries.functions.validate import call_and_match_output
from resource_management.libraries.functions import solr_cloud_util
from resource_management.libraries.resources.xml_config import XmlConfig

def titan_service(action='start'):
  import params
  import params_server
  cmd = format("{titan_bin_dir}/gremlin-server-script.sh")
  cmd_params = params_server.titan_pid_file + " " + params.titan_log_file +" " + params.titan_err_file + " " +  params.titan_bin_dir + " " + params.titan_server_conf_dir + " " +params.titan_log_dir
  if action == 'start':
    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {titan_keytab_path} {titan_jaas_princ};")
      Execute(kinit_cmd,
              user=params.titan_user
              )
    XmlConfig("hbase-site.xml",
              not_if = params.hbase_master_installed,
              conf_dir=params.titan_conf_dir,
              configurations=params.config['configurations']['hbase-site'],
              configuration_attributes=params.config['configuration_attributes']['hbase-site'],
              group=params.user_group,
              owner=params.titan_user,
              mode=0644
              )

    #Add for SparkGraphComputer, prepare dir /user/titan/data on HDFS, and upload spark jars to /user/spark/share/lib/spark for spark.yarn.jars of Spark on YARN.
    #create hdfs dir /user/titan/data
    titan_create_data_dir_command = format("hadoop fs -mkdir -p {titan_hdfs_data_dir}; hadoop fs -chown -R titan:hdfs /user/titan")
    titan_data_exist_command = format("hadoop fs -test -e {titan_hdfs_data_dir}>/dev/null 2>&1")
    Execute(titan_create_data_dir_command,
            not_if = titan_data_exist_command,
            logoutput=True,user=params.hdfs_user)

    #create spark plugin dir for spark jars
    titan_create_spark_plugin_dir_command = format("mkdir -p {titan_ext_spark_plugin_dir}")
    titan_ext_spark_plugin_dir_exist_command = format("ls {titan_ext_spark_plugin_dir}>/dev/null 2>&1")
    Execute(titan_create_spark_plugin_dir_command,
            not_if = titan_ext_spark_plugin_dir_exist_command,
            logoutput=True,user=params.titan_user)

    #get spark arhive from hdfs
    titan_get_spark_tar_command = format("hadoop fs -get {titan_spark2_archive_dir}/{titan_spark2_archive_file} {titan_ext_spark_plugin_dir}")
    titan_sparktargz_exist_command= format("ls {titan_ext_spark_plugin_dir}/{titan_spark2_archive_file}>/dev/null 2>&1")
    Execute(titan_get_spark_tar_command,
            not_if = titan_sparktargz_exist_command,
            logoutput=True,user=params.titan_user)

    #extract spark targz
    titan_x_spark_targz_command = format("tar -xzvf {titan_ext_spark_plugin_dir}/{titan_spark2_archive_file} -C {titan_ext_spark_plugin_dir}/>/dev/null 2>&1")
    titan_sparkjars_exist_command= format("ls {titan_ext_spark_plugin_dir}/*.jar>/dev/null 2>&1")
    Execute(titan_x_spark_targz_command,
            not_if = titan_sparkjars_exist_command,
            logoutput=True,user=params.titan_user)

    #create hdfs dir /user/spark/share/lib/spark
    titan_create_spark_dir_command = format("hadoop fs -mkdir -p {titan_hdfs_spark_lib_dir}")
    titan_spark_exist_command = format("hadoop fs -test -e {titan_hdfs_spark_lib_dir}>/dev/null 2>&1")
    Execute(titan_create_spark_dir_command,
            not_if = titan_spark_exist_command,
            logoutput=True,user=params.hdfs_user)

    #upload spark jars to hdfs /user/spark/share/lib/spark
    titan_put_spark_jar_command = format("hadoop fs -put -f {titan_ext_spark_plugin_dir}/* {titan_hdfs_spark_lib_dir}; hadoop fs -rm -r {titan_hdfs_spark_lib_dir}/guava*.jar; hadoop fs -put -f {titan_home_dir}/lib/guava*.jar {titan_hdfs_spark_lib_dir}")
    titan_sparkjar_exist_command = format("hadoop fs -test -e {titan_hdfs_spark_lib_dir}/*.jar>/dev/null 2>&1")
    Execute(titan_put_spark_jar_command,
            not_if = titan_sparkjar_exist_command,
            logoutput=True,user=params.hdfs_user)

    #rm guava*.jar slf4j-log4j12*.jar spark-core*.jar for conflict
    titan_rm_conflict_jars_command = format("rm -rf {titan_ext_spark_plugin_dir}/guava*.jar; rm -rf {titan_ext_spark_plugin_dir}/slf4j-log4j12*.jar; rm -rf {titan_ext_spark_plugin_dir}/spark-core*.jar; ")
    titan_guava_exist_command = format("ls {titan_ext_spark_plugin_dir}/guava*.jar>/dev/null 2>&1")
    Execute(titan_rm_conflict_jars_command,
            only_if = titan_guava_exist_command,
            logoutput=True,user=params.titan_user)

    #generate yarn-site.xml in Titan conf if no yarn-client installed
    XmlConfig("yarn-site.xml",
              not_if = params.yarn_client_installed,
              conf_dir=params.titan_conf_dir,
              configurations=params.config['configurations']['yarn-site'],
              configuration_attributes=params.config['configuration_attributes']['yarn-site'],
              group=params.user_group,
              owner=params.titan_user,
              mode=0644
              )

    #create jaas file for solr when security enabled
    jaas_file = format('{titan_solr_jaas_file}')
    if not os.path.isfile(jaas_file) and params.security_enabled:
      File(jaas_file,
           owner   = params.titan_user,
           group   = params.user_group,
           mode    = 0644,
           content = Template('titan_solr_jaas.conf.j2')
           )
    #upload config to zookeeper
    solr_cloud_util.upload_configuration_to_zk(
        zookeeper_quorum = params.zookeeper_quorum,
        solr_znode = params.solr_znode,
        config_set = params.titan_solr_configset,
        config_set_dir = params.titan_solr_conf_dir,
        tmp_dir = params.tmp_dir,
        java64_home = params.java64_home,
        jaas_file=jaas_file,
        retry=30, interval=5)

    #create solr collection
    solr_cloud_util.create_collection(
        zookeeper_quorum = params.zookeeper_quorum,
        solr_znode = params.solr_znode,
        collection = params.titan_solr_collection_name,
        config_set = params.titan_solr_configset,
        java64_home = params.java64_home,
        shards = params.titan_solr_shards,
        replication_factor = int(params.infra_solr_replication_factor),
        jaas_file = jaas_file)

    daemon_cmd = format(cmd+" start " + cmd_params)
    no_op_test = format("ls {params_server.titan_pid_file} >/dev/null 2>&1 && ps `cat {params_server.titan_pid_file}` >/dev/null 2>&1")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.titan_user
    )
      
  elif action == 'stop':
    import params_server
    daemon_cmd = format("{titan_bin_dir}/gremlin-server-script.sh stop " + params_server.titan_pid_file)
    Execute(daemon_cmd, user=params.titan_user)
