#!/usr/bin/env python

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
import logging
import unittest
class HttpClientInvoker():
  @staticmethod
  def http_client_invoke_side_effects(*args, **kwargs):
      localss = locals()
      logger = logging.getLogger()
      logger.info(localss)
      http_method = args[0]
      url = args[1]
      payload = kwargs.get("payload",None)

      mocked_code = 200 
      mocked_content = "text/plain"
      
      if http_method == "GET":
        # ClusterModel mocking
        if url == "//clusters/test1":
          mocked_response = open('json/clustermodel_get_cluster.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/hosts/myhost":
          mocked_response = open('json/clustermodel_get_host.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/hosts?fields=*":
          mocked_response = open('json/clustermodel_get_all_hosts.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/configurations?type=global&tag=version1":
          mocked_response = open('json/clustermodel_get_global_config.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/configurations?type=core-site&tag=version1":
          mocked_response = open('json/clustermodel_get_core_site_config.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/configurations?type=hdfs-site&tag=version1":
          mocked_response = open('json/clustermodel_get_hdfs_site_config.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/configurations?type=mapred-site&tag=version1":
          mocked_response = open('json/clustermodel_get_mapred_site_config.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        # HostModel mocking
        elif url == '//clusters/test1/hosts/myhost/host_components/DATANODE':
          mocked_response = open('json/hostmodel_get_host_component.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == '//clusters/test1/hosts/myhost/host_components?fields=HostRoles/state':
          mocked_response = open('json/hostmodel_get_host_components.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        # ComponentModel mocking
        elif url == "//clusters/cl1/hosts/myhost/host_components/DATANODE?fields=metrics":
          mocked_response = open('json/componentmodel_get_metrics.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        # ServiceModel mocking
        elif url == "//clusters/test1/services/GANGLIA/components?fields=*":
          mocked_response = open('json/servicemodel_get_components.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/services/GANGLIA/components/GANGLIA_SERVER":
          mocked_response = open('json/servicemodel_get_component.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        # AmbariClient mocking
        elif url == "//clusters":
          mocked_response = open('json/ambariclient_get_all_clusters.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//hosts":
          mocked_response = open('json/ambariclient_get_all_hosts.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//hosts/dev06.hortonworks.com":
          mocked_response = open('json/ambariclient_get_host.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//stacks2/HDP/versions/1.3.0/stackServices/HDFS/configurations?fields=*":
          mocked_response = open('json/ambariclient_get_config.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//stacks2/HDP/versions/1.3.0/stackServices/HDFS/serviceComponents?fields=*":
          mocked_response = open('json/ambariclient_get_components.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        # others
        elif url == "//clusters/test1/services/GANGLIA":
          mocked_response = open('json/clustermodel_get_service.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test1/services?fields=*":
          mocked_response = open('json/clustermodel_get_all_services.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//stacks2/HDP/versions/2.0.5/stackServices/HDFS/serviceComponents?fields=*":
          mocked_response = open('json/get_components_from_stack.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test6/services/GANGLIA":
          mocked_response = open('json/get_cluster_service.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test6/hosts/r01wn01/host_components/NAMENODE":
          mocked_response = open('json/get_host_component.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test6/hosts/r01wn01/host_components?ServiceComponentInfo":
          mocked_response = open('json/get_host_components.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test6/services/GANGLIA/components/GANGLIA_MONITOR":
          mocked_response = open('json/get_service_component.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == "//clusters/test6/services/GANGLIA/components?fields=*":
          mocked_response = open('json/get_service_components.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        else:
          print "Unknown get request on url: %s" % url
      elif http_method == "DELETE":
        # ClusterModel
        if url == "//clusters/test1/hosts/deleted_nonexistant_cluster":
          mocked_response = open('json/clustermodel_error_deleting_host.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        else: # DELETE (generally does not require any response)
          return "", mocked_code , mocked_content
      elif http_method == "POST":
        # AmbariClient
        if url == "//bootstrap":
          mocked_response = open('json/ambariclient_bootstrap_hosts.json', 'r').read()
          return mocked_response, mocked_code , mocked_content
        elif url == '//clusters/test1/hosts?Hosts/host_name=myhost':
          mocked_code = 201
          return "", mocked_code , mocked_content
        else: # POST (generally does not require any response)
          return "", mocked_code , mocked_content
      else: # PUT (generally does not require any response)
        # ServiceModel mocking
        if url == "//clusters/test1/services/GANGLIA":
          payload_stop = {'ServiceInfo': {'state': 'INSTALLED'}}
          payload_started = {'ServiceInfo': {'state': 'STARTED'}}
          payload_illegal = {'ServiceInfo': {'state': 'ILLEGAL_STATE'}}
          if payload_stop == payload:
            mocked_response = open('json/status_ok_with_id.json', 'r').read()
            return mocked_response, mocked_code , mocked_content
          elif payload_started == payload:
            mocked_response = open('json/status_ok_with_id.json', 'r').read()
            return mocked_response, mocked_code , mocked_content
          elif payload_illegal == payload:
            mocked_response = open('json/status_error_with_message.json', 'r').read()
            return mocked_response, mocked_code , mocked_content
        else:
          return "", mocked_code , mocked_content
        