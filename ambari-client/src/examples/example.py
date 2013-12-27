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
    
    ######################################
    #    High level
    ######################################
    all_clusters = client.get_all_clusters()
    print all_clusters.to_json_dict()
    print all_clusters
    print"\n"
    
    all_hosts = client.get_all_hosts()
    print all_hosts
    print all_hosts.to_json_dict()
    print"\n"

    
    ######################################
    #    going into a specific cluster
    ######################################
    cluster = client.get_cluster('test46')
    print cluster
    print cluster.to_json_dict()
    print"\n"


    clusters_hosts = cluster.get_all_hosts()
    print clusters_hosts.to_json_dict()
    print clusters_hosts
    print"\n"


    #host1 = cluster.get_host('r01wn01')
    host1 = cluster.get_host('r01hn01')
    print host1
    print host1.clusterRef.cluster_name
    print host1.to_json_dict()
    print"\n"
    
    
    host1_comp = host1.get_host_components()
    print host1_comp
    print host1_comp.to_json_dict()
    print"\n"


    nn = host1.get_host_component("NAMENODE")
    print nn
    print nn.to_json_dict()
    print nn.clusterRef.cluster_name
    print"\n"


    serviceList = cluster.get_all_services()
    print serviceList
    print serviceList.to_json_dict()
    print"\n"


    ganglia = cluster.get_service("GANGLIA")     
    print  ganglia  
    print ganglia.to_json_dict()
    print"\n"
   
   
    ganglia_comps = ganglia.get_service_components()
    print ganglia_comps  
    print ganglia_comps.to_json_dict()
    print"\n"
   
   
    ganglia_comp1 = ganglia.get_service_component('GANGLIA_MONITOR')
    print ganglia_comp1  
    print ganglia_comp1.to_json_dict()
    print ganglia_comp1.clusterRef.cluster_name
    print"\n"
    
    s = client.get_config("1.3.0", "HDFS")
    print s
    print"\n"
     
    s = client.get_components("1.3.0", "HDFS")
    print s   
    
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
