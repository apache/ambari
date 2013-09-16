#!/usr/bin/env python2.6

'''
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
'''


from mock.mock import MagicMock, patch
from ambari_client.ambari_api import  AmbariClient 

import unittest

class TestAmbariClient(unittest.TestCase):

  def test_init(self):
    """
    AmbariClient is the top-level root resources.
    This testcase checks if when the  init method was called &
    the httpclient was initialized
    """
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1)
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
      
    client = AmbariClient(host_name="localhost", user_name="admin", password="admin")
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
      
    client = AmbariClient(host_name="localhost")
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
    
  
    
  @patch("ambari_client.core.http_client.HttpClient")  
  def test_get_all_clusters_valid(self , http_client):
    """
    Get all clusters.
    This testcase checks if get_all_clusters returns a list of ModelList.
    """
    http_client_mock = MagicMock()
    http_client.return_value = http_client_mock
    
    mocked_code = "200" 
    mocked_content = "text/plain"
    expected_output = {'items': [{'cluster_name': u'test1', 'version': u'HDP-1.2.1'}]}
    
    linestring = open('json/get_all_clusters.json', 'r').read()
    mocked_response = linestring
    http_client_mock.invoke.return_value = mocked_response , mocked_code , mocked_content
   
      
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1 , client=http_client_mock)
    all_clusters = client.get_all_clusters()
      
    self.assertEqual(len(all_clusters), 1, "There should be a cluster from the response")
    self.assertEqual(all_clusters.to_json_dict(), expected_output, "to_json_dict should convert ModelList")
    
  @patch("ambari_client.core.http_client.HttpClient")  
  def test_get_hosts_clusters_valid(self , http_client):
    """
    Get all hosts.
    This testcase checks if get_all_hosts returns a list of ModelList.
    """
    http_client_mock = MagicMock()
    http_client.return_value = http_client_mock
    
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    linestring = open('json/get_all_hosts.json', 'r').read()
    mocked_response = linestring
    http_client_mock.invoke.return_value = mocked_response , mocked_code , mocked_content
   
      
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1 , client=http_client_mock)
    all_hosts = client.get_all_hosts()
      
    self.assertEqual(len(all_hosts), 12, "There should be 12 hosts from the response")
    
       
  @patch("ambari_client.core.http_client.HttpClient")  
  def test_get_cluster_valid(self , http_client):
    """
    Get all clusters.
    This testcase checks if get_all_clusters returns a list of ModelList.
    """
    http_client_mock = MagicMock()
    http_client.returned_obj = http_client_mock
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    linestring = open('json/get_cluster.json', 'r').read()
    mocked_response = linestring
    expected_dict_output = {'cluster_name': u'test1', 'version': u'HDP-1.2.1'}
    
    http_client_mock.invoke.return_value = mocked_response , mocked_code , mocked_content
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    
    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")



  @patch("ambari_client.core.http_client.HttpClient")  
  def test_get_cluster_services_valid(self , http_client):
    """
    Get all services of a cluster.
    This testcase checks if get_all_services returns a list of ModelList.
    """
    http_client_mock = MagicMock()
    http_client.returned_obj = http_client_mock
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    expected_dict_output = {'cluster_name': u'test1', 'version': u'HDP-1.2.1'}
    
    http_client_mock.invoke.side_effect = http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    serviceList = cluster.get_all_services()
    
    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    self.assertEqual(len(serviceList), 3, "There should be a 3 services from the response")
 
  @patch("ambari_client.core.http_client.HttpClient")  
  def test_get_cluster_service_valid(self , http_client):
    """
    Get the service of a cluster
    This testcase checks if get_service returns a list of ServiceModel.
    """
    http_client_mock = MagicMock()
    http_client.returned_obj = http_client_mock
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    expected_dict_output = {'cluster_name': u'test1', 'version': u'HDP-1.2.1'}
    
    http_client_mock.invoke.side_effect = http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    serviceList = cluster.get_all_services()
    ganglia = cluster.get_service("GANGLIA")  

    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    self.assertEqual(len(serviceList), 3, "There should be a 3 services from the response")
    self.assertEqual(str(ganglia.state), "STARTED", "The ganglia service state should be fetched as STARTED")
    self.assertEqual(ganglia.clusterRef.cluster_name, cluster.cluster_name, "The clusterRef value for  service  should be fetched ")
    


  

def http_client_invoke_side_effects(*args, **kwargs):
    print locals()
    mocked_code = "200" 
    mocked_content = "text/plain"
    if args[1] == "//clusters/test1":
        mocked_response = open('json/get_cluster.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//hosts":
        mocked_response = open('json/get_all_hosts.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/hosts/r01wn01":
        mocked_response = open('json/get_cluster_host.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/hosts?fields=*":
        mocked_response = open('json/get_cluster_hosts.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/services/GANGLIA":
        mocked_response = open('json/get_cluster_service.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test1/services?fields=*":
        mocked_response = open('json/get_cluster_services.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/hosts/r01wn01/host_components/NAMENODE":
        mocked_response = open('json/get_host_component.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/hosts/r01wn01/host_components?ServiceComponentInfo":
        mocked_response = open('json/get_host_components.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/services/GANGLIA/components/GANGLIA_MONITOR":
        mocked_response = open('json/get_service_component.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test6/services/GANGLIA/components?fields=*":
        mocked_response = open('json/get_service_components.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
    elif args[1] == "//clusters/test1/services/GANGLIA":
        mocked_response = open('json/get_service.json', 'r').read()
        return mocked_response, mocked_code , mocked_content
