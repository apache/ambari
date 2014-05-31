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
    # cluster delete
    ###############################
    print dir(client)
    
    s1 = client.delete_cluster("test33")
    print s1
    print s1.to_json_dict()
    
    s1 = client.delete_host("test33")
    print s1
    print s1.to_json_dict()
    
    s1 = client.delete_host("r01wn01")
    print s1
    print s1.to_json_dict()
    


########################################################################
#
# The "main" entry
#
########################################################################
if __name__ == '__main__':
    main()
######################################################################## 
