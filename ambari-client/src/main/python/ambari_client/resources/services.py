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

from ambari_client.model import  service
__docformat__ = "epytext"


def get_service(resource_root, service_name, cluster_name="default"):
  """
  Get a service by service_name
  @param resource_root: The root Resource .
  @param service_name: Service service_name.
  @param cluster_name: Cluster service_name.
  @return: ServiceModel object.
  """
  return  service.get_service(resource_root, service_name, cluster_name)



def get_all_services(resource_root, cluster_name="default"):
  """
  Get all services.
  @param resource_root: The root Resource.
  @param cluster_name: Cluster name.
  @return: A list of ServiceModel objects in ModelList.
  """
  return service.get_all_services(resource_root, cluster_name)