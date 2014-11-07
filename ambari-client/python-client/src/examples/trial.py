 #
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import os 
import sys 
import logging
from ambari_client.ambari_api import  AmbariClient 


def main():

    path = os.getcwd() ;
    print path
    sys.path.append(path)
    
    logging.basicConfig(filename="ambari_api.log", level=logging.DEBUG , filemode="w")
    logging.info("Program started")
     
     
     
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1)
    print client.version
    print client.host_url
    print"\n"
    
#    s = client.get_config("1.3.0", "HDFS")
#    print s
#    
#    s = client.get_components("1.3.0", "HDFS")
#    print s
#
#    mycluster = client.create_cluster("test46", "HDP-1.3.0")
#    print mycluster
#
    mycluster = client.get_cluster('test46')
    print mycluster
    print mycluster.to_json_dict()
    print"\n"
#    
#    services_list = ["HDFS", "MAPREDUCE", "NAGIOS", "GANGLIA"]
#    s2 = mycluster.create_services(services_list)
#    print s2
#
#
#    propr_dict = {"dfs_name_dir":"/data/1/hadoop/hdfs/namenode,/data/2/hadoop/hdfs/namenode,/data/3/hadoop/hdfs/namenode,/data/4/hadoop/hdfs/namenode,/data/5/hadoop/hdfs/namenode,/data/6/hadoop/hdfs/namenode,/data/7/hadoop/hdfs/namenode,/data/8/hadoop/hdfs/namenode", "namenode_heapsize":"1024m", "namenode_opt_newsize":"200m", "fs_checkpoint_dir":"/data/1/hadoop/hdfs/namesecondary", "dfs_data_dir":"/data/1/hadoop/hdfs/data,/data/2/hadoop/hdfs/data,/data/3/hadoop/hdfs/data,/data/4/hadoop/hdfs/data,/data/5/hadoop/hdfs/data,/data/6/hadoop/hdfs/data,/data/7/hadoop/hdfs/data,/data/8/hadoop/hdfs/data,/data/9/hadoop/hdfs/data,/data/10/hadoop/hdfs/data", "dtnode_heapsize":"1024m", "dfs_datanode_failed_volume_tolerated":"0", "dfs_webhdfs_enabled":"true", "hadoop_heapsize":"1024", "datanode_du_reserved":"0", "fs_checkpoint_period":"21600", "fs_checkpoint_size":"67108864", "hdfs_log_dir_prefix":"/var/log/hadoop", "hadoop_pid_dir_prefix":"/var/run/hadoop", "namenode_opt_maxnewsize":"200m", "dfs_exclude":"dfs.exclude", "dfs_include":"dfs.include", "dfs_replication":"3", "dfs_block_local_path_access_user":"hbase", "dfs_datanode_data_dir_perm":"750", "security_enabled":"false", "namenode_formatted_mark_dir":"/var/run/hadoop/hdfs/namenode/formatted/", "hcat_conf_dir":"", "jtnode_opt_newsize":"200m", "jtnode_opt_maxnewsize":"200m", "jtnode_heapsize":"1024m", "mapred_local_dir":"/data/1/hadoop/mapred,/data/2/hadoop/mapred,/data/3/hadoop/mapred,/data/4/hadoop/mapred,/data/5/hadoop/mapred,/data/6/hadoop/mapred,/data/7/hadoop/mapred,/data/8/hadoop/mapred,/data/9/hadoop/mapred,/data/10/hadoop/mapred", "mapred_map_tasks_max":"4", "mapred_red_tasks_max":"2", "mapred_child_java_opts_sz":"768", "scheduler_name":"org.apache.hadoop.mapred.CapacityTaskScheduler", "mapred_cluster_map_mem_mb":"1536", "mapred_cluster_red_mem_mb":"2048", "mapred_cluster_max_map_mem_mb":"6144", "mapred_cluster_max_red_mem_mb":"4096", "mapred_job_map_mem_mb":"1536", "mapred_job_red_mem_mb":"2048", "io_sort_mb":"200", "io_sort_spill_percent":"0.9", "mapreduce_userlog_retainhours":"24", "maxtasks_per_job":"-1", "lzo_enabled":"true", "snappy_enabled":"true", "rca_enabled":"true", "mapred_system_dir":"/mapred/system", "mapred_hosts_exclude":"mapred.exclude", "mapred_hosts_include":"mapred.include", "mapred_jobstatus_dir":"file:////mapred/jobstatus", "nagios_web_login":"nagiosadmin", "nagios_web_password":"admin", "nagios_contact":"admin@admin.com", "nagios_group":"nagios", "hbase_conf_dir":"/etc/hbase", "proxyuser_group":"users", "dfs_datanode_address":"50010", "dfs_datanode_http_address":"50075", "apache_artifacts_download_url":"", "ganglia_runtime_dir":"/var/run/ganglia/hdp", "java64_home":"/usr/jdk/jdk1.6.0_31", "run_dir":"/var/run/hadoop", "hadoop_conf_dir":"/etc/hadoop", "hdfs_user":"hdfs", "mapred_user":"mapred", "hbase_user":"hbase", "hive_user":"hive", "hcat_user":"hcat", "webhcat_user":"hcat", "oozie_user":"oozie", "zk_user":"zookeeper", "gmetad_user":"nobody", "gmond_user":"nobody", "nagios_user":"nagios", "smokeuser":"ambari-qa", "user_group":"hadoop", "rrdcached_base_dir":"/var/lib/ganglia/rrds"}
#    print propr_dict
#    s3 = mycluster.add_config("global", "version1" , propr_dict)
#    print s3
#
#    s2 = mycluster.create_service_components("1.3.0", "HDFS")
#    print s2    
#    s2 = mycluster.create_service_components("1.3.0", "MAPREDUCE")
#    print s2 
#    s2 = mycluster.create_service_components("1.3.0", "GANGLIA")
#    print s2 
#    s2 = mycluster.create_service_components("1.3.0", "NAGIOS")
#    print s2 
#
#    h_l = ['apspal44-83', 'apspal44-84', 'apspal44-85', 'apspal44-86', 'apspal44-87', 'apspal44-88', 'apspal44-89', 'r01hn01', 'r01wn01', 'r01wn02', 'r01wn03']
#    print h_l
#    s3 = mycluster.create_hosts(h_l)
#    print s3
#    print"\n"
#
#    # 6) add hosts roles
#    host1 = mycluster.get_host('r01hn01')
#    print host1
#    s4 = host1.assign_role("NAMENODE")
#    print s4
#    print"\n"
#
#    s4 = mycluster.install_all_services()
#    print s4
#    print"\n"

#    s4 = mycluster.start_all_services(run_smoke_test=True)
#    print s4
#    print"\n"
#
#    s4 = mycluster.stop_all_services()
#    print s4
#    print"\n"

#    s2 = mycluster.create_service("ZOOKEEPER")
#    print s2
    
#    s2 = mycluster.create_service_components("1.3.0", "ZOOKEEPER")
#    print s2
#    



#    host1 = mycluster.get_host('r01wn01')
#    print host1
#    s4 = host1.assign_role("ZOOKEEPER_SERVER")
#    print s4
#    host1 = mycluster.get_host('r01wn02')
#    print host1
#    s4 = host1.assign_role("ZOOKEEPER_SERVER")
#    print s4
#    host1 = mycluster.get_host('r01wn03')
#    print host1
#    s4 = host1.assign_role("ZOOKEEPER_SERVER")
#    print s4
#    host1 = mycluster.get_host('r01wn03')
#    print host1
#    s4 = host1.assign_role("ZOOKEEPER_CLIENT")
#    print s4

    zk = mycluster.get_service("ZOOKEEPER")
    print zk
    
    s = zk.stop()
    print s
    
    
    ######################################
    #    create cluster
    ######################################
#    ganglia.stop()
#    ganglia.start()
       

########################################################################
#
# The "main" entry
#
########################################################################
if __name__ == '__main__':
    main()
######################################################################## 
