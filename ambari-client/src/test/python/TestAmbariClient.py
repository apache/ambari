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
from ambari_client.core.errors import BadRequest
from HttpClientInvoker import HttpClientInvoker
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
    
    
    client = AmbariClient("localhost", 8443, "admin", "admin", use_https=True)
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "https://localhost:8443/api/v1",
                       "host_url should be https://localhost:8443/api/v1")
    
  
    
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
  def test_get_all_hosts(self , http_client):
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
  def test_get_host_valid(self , http_client):
    """
    Get host
    This testcase checks if client.get_host returns a correct host
    """
    http_client_mock = MagicMock()
    http_client.returned_obj = http_client_mock
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    linestring = open('json/get_host.json', 'r').read()
    mocked_response = linestring
    expected_dict_output = {'ip': '10.0.2.15', 'host_name': 'dev06.hortonworks.com', 'rack_info': '/default-rack'}
    
    http_client_mock.invoke.return_value = mocked_response , mocked_code , mocked_content
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    host = client.get_host('dev06.hortonworks.com')
    
    self.assertEqual(host.to_json_dict(), expected_dict_output)
    
       
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
    
    linestring = open('json/clustermodel_get_cluster.json', 'r').read()
    mocked_response = linestring
    expected_dict_output = {'cluster_name': u'test1', 'version': u'HDP-1.2.1'}
    
    http_client_mock.invoke.return_value = mocked_response , mocked_code , mocked_content
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    
    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    
  @patch("ambari_client.core.http_client.HttpClient")  
  def test_exceptions(self , http_client):
    """
    Test exceptions from ambari.client.core.errors
    """
    http_client_mock = MagicMock()
    http_client.returned_obj = http_client_mock
    mocked_code = "200" 
    mocked_content = "text/plain"
    
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    
    try:
      cluster.delete_host('deleted_nonexistant_cluster')
      print http_client_mock.invoke.call_args_list
      self.fail('Exception should have been thrown!')
    except BadRequest, ex:
      self.assertEquals(str(ex), 'exception: 400. Attempted to add unknown hosts to a cluster.  These hosts have not been registered with the server: dev05')
    except Exception, ex:
      self.fail('Wrong exception thrown!')
