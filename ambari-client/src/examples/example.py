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
'''



'''
import os 
import sys 
import logging
from ambari_client.ambari_api import  AmbariClient 


def main():
  """
  This method has few examples on how to use the ambari_client api
  """
    path = os.getcwd() ;
    print path
    sys.path.append(path)
    
    logging.basicConfig(filename="ambari_client.log", level=logging.DEBUG ,filemode="w")
    logging.info("Program started")
     
     
     
    client = AmbariClient("localhost", 8080, "admin","admin",version=1)

    all_clusters = client.get_all_clusters()
    print all_clusters
    print all_clusters.to_json_dict()
    print"\n"
    
    cluster = client.get_cluster('test1')
    print cluster
    print"\n"
    
#    serviceList = cluster.get_all_services()
#    print serviceList
#    print"\n"
#    
#    
#    for service in serviceList:
#        print str(service.service_name)+" = "+str(service.state)
#    print"\n"    

    ganglia = cluster.get_service("GANGLIA")       
    print  ganglia.state
    print"\n"
    
    
    
#    cluster_ref =  ganglia.clusterRef
#    print cluster_ref.cluster_name
#    print"\n"
    
    
    ganglia.stop()
#    ganglia.start()
       

    
########################################################################
#
# The "main" entry
#
########################################################################
if __name__ == '__main__':
    main()
######################################################################## 
