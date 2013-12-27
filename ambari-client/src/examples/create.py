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
    
    
    ###############################
    # cluster creation
    ###############################
    # 1) create cluster
    cluster = client.create_cluster("test33", "HDP-1.3.0")
    print cluster
    

    cluster = client.get_cluster('test33')
    print cluster
    print cluster.to_json_dict()
    print"\n"
    
    # 2) create services
    services_list = ["HDFS", "MAPREDUCE", "NAGIOS", "GANGLIA"]
    s2 = cluster.create_services(services_list)
    print s2
    
    s2 = cluster.create_service("ZOOKEEPER")
    print s2
    
    # 3) create global config
    s3 = cluster.add_config("global", "version1" , {})
    print s3
    s3 = cluster.add_config("core-site", "version1" , {})
    print s3
    s3 = cluster.add_config("hdfs-site", "version1" , {})
    print s3
    s3 = cluster.add_config("mapred-site", "version1" , {})
    print s3
#    s3 = cluster.add_config("hbase-site", "version1" , {})
#    print s3
#    s3 = cluster.add_config("oozie-site", "version1" , {})
#    print s3
#    s3 = cluster.add_config("hive-site", "version1" , {})
#    print s3
#    s3 = cluster.add_config("webhcat-site", "version1" , {})
#    print s3
    
    
    
    
#    hdfs_components = client.get_components("1.3.0", "HDFS")
#    print hdfs_components
#    mr_components = client.get_components("1.3.0", "MAPREDUCE")
#    print mr_components
#    ganglia_components = client.get_components("1.3.0", "GANGLIA")
#    print ganglia_components
#    nagios_components = client.get_components("1.3.0", "NAGIOS")
#    print nagios_components


    # 4) add service components
    s2 = cluster.create_service_components("1.3.0", "HDFS")
    print s2    
    s2 = cluster.create_service_components("1.3.0", "MAPREDUCE")
    print s2 
    s2 = cluster.create_service_components("1.3.0", "GANGLIA")
    print s2 
    s2 = cluster.create_service_components("1.3.0", "NAGIOS")
    print s2 


    all_hosts = client.get_all_hosts()
    h_l = [x.host_name for x in all_hosts]
    print h_l
    
    # 5) add hosts
    s3 = cluster.create_hosts(h_l)
    print s3
    print"\n"
    
    # 6) add hosts roles
    host1 = cluster.get_host('r01wn01')
    print host1
    s4 = host1.assign_role("NAMENODE")
    print s4
    print"\n"
     
    # 7) add hosts roles
    s4 = cluster.start_all_services()
    print s4
    print"\n"


    all_clusters = client.get_all_clusters()
    print all_clusters.to_json_dict()
    print all_clusters
    print"\n"
    
    all_hosts = client.get_all_hosts()
    print all_hosts
    print all_hosts.to_json_dict()
    print"\n"
    
    serviceList = cluster.get_all_services()
    print serviceList
    print serviceList.to_json_dict()
    print"\n"
########################################################################
#
# The "main" entry
#
########################################################################
if __name__ == '__main__':
    main()
######################################################################## 
