#!/usr/bin/env python
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

"""
import httplib

from ambari_commons.parallel_processing import PrallelProcessResult, execute_in_parallel, SUCCESS
from service_check import post_metrics_to_collector
from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management.libraries.script.script import Script
from resource_management import Template
from collections import namedtuple
from urlparse import urlparse
from base64 import b64encode
import random
import time
import socket
import ambari_simplejson as json
import ambari_commons.network as network
import os

GRAFANA_SEARCH_BUILTIN_DASHBOARDS = "/api/search?tag=builtin"
GRAFANA_DATASOURCE_URL = "/api/datasources"
GRAFANA_USER_URL = "/api/user"
GRAFANA_DASHBOARDS_URL = "/api/dashboards/db"
METRICS_GRAFANA_DATASOURCE_NAME = "AMBARI_METRICS"

Server = namedtuple('Server', [ 'protocol', 'host', 'port', 'user', 'password' ])

def perform_grafana_get_call(url, server):
  import params

  grafana_https_enabled = server.protocol.lower() == 'https'
  response = None
  ca_certs = None
  if grafana_https_enabled:
    ca_certs = params.ams_grafana_ca_cert

  for i in xrange(0, params.grafana_connect_attempts):
    try:
      conn = network.get_http_connection(
        server.host,
        int(server.port),
        grafana_https_enabled,
        ca_certs,
        ssl_version=Script.get_force_https_protocol_value()
      )

      userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
      headers = { 'Authorization' : 'Basic %s' %  userAndPass }

      Logger.info("Connecting (GET) to %s:%s%s" % (server.host, server.port, url))

      conn.request("GET", url, headers = headers)
      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < params.grafana_connect_attempts - 1:
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (params.grafana_connect_retry_delay))
        time.sleep(params.grafana_connect_retry_delay)
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return response

def perform_grafana_put_call(url, id, payload, server):
  import params

  response = None
  data = None
  userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
  headers = {"Content-Type": "application/json",
             'Authorization' : 'Basic %s' %  userAndPass }
  grafana_https_enabled = server.protocol.lower() == 'https'

  ca_certs = None
  if grafana_https_enabled:
    ca_certs = params.ams_grafana_ca_cert

  for i in xrange(0, params.grafana_connect_attempts):
    try:
      conn = network.get_http_connection(
        server.host,
        int(server.port),
        grafana_https_enabled,
        ca_certs,
        ssl_version=Script.get_force_https_protocol_value()
      )
      conn.request("PUT", url + "/" + str(id), payload, headers)
      response = conn.getresponse()
      data = response.read()
      Logger.info("Http data: %s" % data)
      conn.close()
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < params.grafana_connect_attempts - 1:
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (params.grafana_connect_retry_delay))
        time.sleep(params.grafana_connect_retry_delay)
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return (response, data)

def perform_grafana_post_call(url, payload, server):
  import params

  response = None
  data = None
  userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
  Logger.debug('POST payload: %s' % payload)
  headers = {"Content-Type": "application/json", "Content-Length" : len(payload),
             'Authorization' : 'Basic %s' %  userAndPass}
  grafana_https_enabled = server.protocol.lower() == 'https'

  ca_certs = None
  if grafana_https_enabled:
    ca_certs = params.ams_grafana_ca_cert

  for i in xrange(0, params.grafana_connect_attempts):
    try:
      Logger.info("Connecting (POST) to %s:%s%s" % (server.host, server.port, url))
      conn = network.get_http_connection(
        server.host,
        int(server.port),
        grafana_https_enabled, ca_certs,
        ssl_version=Script.get_force_https_protocol_value()
      )

      conn.request("POST", url, payload, headers)

      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
      if response.status == 401: #Intermittent error thrown from Grafana
        if i < params.grafana_connect_attempts - 1:
          Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                  % (params.grafana_connect_retry_delay))
          time.sleep(params.grafana_connect_retry_delay)
          continue
      data = response.read()
      Logger.info("Http data: %s" % data)
      conn.close()
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < params.grafana_connect_attempts - 1:
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (params.grafana_connect_retry_delay))
        time.sleep(params.grafana_connect_retry_delay)
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return (response, data)

def perform_grafana_delete_call(url, server):
  import params

  grafana_https_enabled = server.protocol.lower() == 'https'
  response = None

  ca_certs = None
  if grafana_https_enabled:
    ca_certs = params.ams_grafana_ca_cert

  for i in xrange(0, params.grafana_connect_attempts):
    try:
      conn = network.get_http_connection(
        server.host,
        int(server.port),
        grafana_https_enabled, ca_certs,
        ssl_version=Script.get_force_https_protocol_value()
      )

      userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
      headers = { 'Authorization' : 'Basic %s' %  userAndPass }

      Logger.info("Connecting (DELETE) to %s:%s%s" % (server.host, server.port, url))

      conn.request("DELETE", url, headers = headers)
      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < params.grafana_connect_attempts - 1:
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (params.grafana_connect_retry_delay))
        time.sleep(params.grafana_connect_retry_delay)
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return response

def is_unchanged_datasource_url(grafana_datasource_url, new_datasource_host):
  import params
  parsed_url = urlparse(grafana_datasource_url)
  Logger.debug("parsed url: scheme = %s, host = %s, port = %s" % (
    parsed_url.scheme, parsed_url.hostname, parsed_url.port))
  Logger.debug("collector: scheme = %s, host = %s, port = %s" %
              (params.metric_collector_protocol, new_datasource_host,
               params.metric_collector_port))

  return parsed_url.scheme.strip() == params.metric_collector_protocol.strip() and \
         parsed_url.hostname.strip() == new_datasource_host.strip() and \
         str(parsed_url.port) == params.metric_collector_port

def do_ams_collector_post(metric_collector_host, params):
    ams_metrics_post_url = "/ws/v1/timeline/metrics/"
    random_value1 = random.random()
    headers = {"Content-type": "application/json"}
    ca_certs = os.path.join(params.ams_grafana_conf_dir,
                            params.metric_truststore_ca_certs)

    current_time = int(time.time()) * 1000
    metric_json = Template('smoketest_metrics.json.j2', hostname=params.hostname, random1=random_value1,
                           current_time=current_time).get_content()

    post_metrics_to_collector(ams_metrics_post_url, metric_collector_host, params.metric_collector_port, params.metric_collector_https_enabled,
                                metric_json, headers, ca_certs)

def create_grafana_admin_pwd():
  import params

  serverCall1 = Server(protocol = params.ams_grafana_protocol.strip(),
                       host = params.ams_grafana_host.strip(),
                       port = params.ams_grafana_port,
                       user = params.ams_grafana_admin_user,
                       password = params.ams_grafana_admin_pwd)

  response = perform_grafana_get_call(GRAFANA_USER_URL, serverCall1)
  if response and response.status != 200:

    serverCall2 = Server(protocol = params.ams_grafana_protocol.strip(),
                         host = params.ams_grafana_host.strip(),
                         port = params.ams_grafana_port,
                         user = params.ams_grafana_admin_user,
                         password = 'admin')

    Logger.debug("Setting grafana admin password")
    pwd_data = {  "oldPassword": "admin",
                  "newPassword": params.ams_grafana_admin_pwd,
                  "confirmNew": params.ams_grafana_admin_pwd
                  }
    password_json = json.dumps(pwd_data)

    (response, data) = perform_grafana_put_call(GRAFANA_USER_URL, 'password', password_json, serverCall2)

    if response.status == 200:
      Logger.info("Ambari Metrics Grafana password updated.")

    elif response.status == 500:
      Logger.info("Ambari Metrics Grafana password update failed. Not retrying.")
      raise Fail("Ambari Metrics Grafana password update failed. PUT request status: %s %s \n%s" %
                 (response.status, response.reason, data))
    else:
      raise Fail("Ambari Metrics Grafana password creation failed. "
                 "PUT request status: %s %s \n%s" % (response.status, response.reason, data))
  else:
    Logger.info("Grafana password update not required.")
  pass

def create_ams_datasource():
  import params
  server = Server(protocol = params.ams_grafana_protocol.strip(),
                  host = params.ams_grafana_host.strip(),
                  port = params.ams_grafana_port,
                  user = params.ams_grafana_admin_user,
                  password = params.ams_grafana_admin_pwd)

  """
  Create AMS datasource in Grafana, if exsists make sure the collector url is accurate
  """
  Logger.info("Trying to find working metric collector")
  results = execute_in_parallel(do_ams_collector_post, params.ams_collector_hosts.split(','), params)
  new_datasource_host = ""

  for host in params.ams_collector_hosts.split(','):
    if host in results:
      if results[host].status == SUCCESS:
        new_datasource_host = host
        Logger.info("Found working collector on host %s" % new_datasource_host)
        break
      else:
        Logger.warning(results[host].result)

  if new_datasource_host == "":
    Logger.warning("All metric collectors are unavailable. Will use random collector as datasource host.")
    new_datasource_host = params.metric_collector_host

  Logger.info("New datasource host will be %s" % new_datasource_host)

  ams_datasource_json = Template('metrics_grafana_datasource.json.j2',
                            ams_datasource_name=METRICS_GRAFANA_DATASOURCE_NAME, ams_datasource_host=new_datasource_host).get_content()
  Logger.info("Checking if AMS Grafana datasource already exists")

  response = perform_grafana_get_call(GRAFANA_DATASOURCE_URL, server)
  create_datasource = True

  if response and response.status == 200:
    datasources = response.read()
    datasources_json = json.loads(datasources)
    for i in xrange(0, len(datasources_json)):
      datasource_name = datasources_json[i]["name"]
      if datasource_name == METRICS_GRAFANA_DATASOURCE_NAME:
        create_datasource = False # datasource already exists
        Logger.info("Ambari Metrics Grafana datasource already present. Checking Metrics Collector URL")
        datasource_url = datasources_json[i]["url"]

        if is_unchanged_datasource_url(datasource_url, new_datasource_host):
          Logger.info("Metrics Collector URL validation succeeded.")
          return
        else: # Metrics datasource present, but collector host is wrong.
          datasource_id = datasources_json[i]["id"]
          Logger.info("Metrics Collector URL validation failed. Updating "
                      "datasource, id = %s" % datasource_id)

          (response, data) = perform_grafana_put_call(GRAFANA_DATASOURCE_URL, datasource_id,
                                                      ams_datasource_json, server)

          if response.status == 200:
            Logger.info("Ambari Metrics Grafana data source updated.")

          elif response.status == 500:
            Logger.info("Ambari Metrics Grafana data source update failed. Not retrying.")
            raise Fail("Ambari Metrics Grafana data source update failed. PUT request status: %s %s \n%s" %
                       (response.status, response.reason, data))
          else:
            raise Fail("Ambari Metrics Grafana data source creation failed. "
                       "PUT request status: %s %s \n%s" % (response.status, response.reason, data))
        pass
      pass
    pass
  else:
    Logger.info("Error checking for Ambari Metrics Grafana datasource. Will attempt to create.")

  if not create_datasource:
    return
  else:
    Logger.info("Generating datasource:\n%s" % ams_datasource_json)

    (response, data) = perform_grafana_post_call(GRAFANA_DATASOURCE_URL, ams_datasource_json, server)

    if response.status == 200:
      Logger.info("Ambari Metrics Grafana data source created.")
    elif response.status == 500:
      Logger.info("Ambari Metrics Grafana data source creation failed. Not retrying.")
      raise Fail("Ambari Metrics Grafana data source creation failed. POST request status: %s %s \n%s" %
                 (response.status, response.reason, data))
    else:
      Logger.info("Ambari Metrics Grafana data source creation failed.")
      raise Fail("Ambari Metrics Grafana data source creation failed. POST request status: %s %s \n%s" %
                 (response.status, response.reason, data))
  pass

def create_ams_dashboards():
  """
  Create dashboards in grafana from the json files
  """
  import params
  server = Server(protocol = params.ams_grafana_protocol.strip(),
                  host = params.ams_grafana_host.strip(),
                  port = params.ams_grafana_port,
                  user = params.ams_grafana_admin_user,
                  password = params.ams_grafana_admin_pwd)

  dashboard_files = params.get_grafana_dashboard_defs()
  version = params.get_ambari_version()
  Logger.info("Checking dashboards to update for Ambari version : %s" % version)
  # Friendly representation of dashboard
  Dashboard = namedtuple('Dashboard', ['uri', 'id', 'title', 'tags'])

  existing_dashboards = []
  response = perform_grafana_get_call(GRAFANA_SEARCH_BUILTIN_DASHBOARDS, server)
  if response and response.status == 200:
    data = response.read()
    try:
      dashboards = json.loads(data)
    except:
      Logger.error("Unable to parse JSON response from grafana request: %s" %
                   GRAFANA_SEARCH_BUILTIN_DASHBOARDS)
      Logger.info(data)
      return

    for dashboard in dashboards:
      if dashboard['title'] == 'HBase - Performance':
        perform_grafana_delete_call("/api/dashboards/" + dashboard['uri'], server)
      else:
        existing_dashboards.append(
            Dashboard(uri = dashboard['uri'], id = dashboard['id'],
                    title = dashboard['title'], tags = dashboard['tags'])
          )
    pass
  else:
    Logger.error("Failed to execute search query on Grafana dashboards. "
                 "query = %s\n statuscode = %s\n reason = %s\n data = %s\n" %
                 (GRAFANA_SEARCH_BUILTIN_DASHBOARDS, response.status, response.reason, response.read()))
    return

  Logger.debug('Dashboard definitions found = %s' % str(dashboard_files))

  if dashboard_files:
    for dashboard_file in dashboard_files:
      try:
        with open(dashboard_file, 'r') as file:
          dashboard_def = json.load(file)
      except Exception, e:
        Logger.error('Unable to load dashboard json file %s' % dashboard_file)
        Logger.error(str(e))
        continue

      if dashboard_def:
        update_def = True
        # Make sure static json does not have id
        if "id" in dashboard_def:
          dashboard_def['id'] = None
        # Set correct tags
        if 'tags' in dashboard_def:
          dashboard_def['tags'].append('builtin')
          dashboard_def['tags'].append(version)
        else:
          dashboard_def['tags'] = [ 'builtin', version ]
        
        for dashboard in existing_dashboards:
          if dashboard.title == dashboard_def['title']:
            if version not in dashboard.tags:
              # Found existing dashboard with wrong version - update dashboard
              update_def = True
            else:
              update_def = False # Skip update
        pass

        if update_def:
          Logger.info("Updating dashboard definition for %s with tags: %s" %
                      (dashboard_def['title'], dashboard_def['tags']))

          # Discrepancy in grafana export vs import format
          dashboard_def_payload = { "dashboard" : dashboard_def, 'overwrite': True }
          paylaod = json.dumps(dashboard_def_payload).strip()

          (response, data) = perform_grafana_post_call(GRAFANA_DASHBOARDS_URL, paylaod, server)

          if response and response.status == 200:
            Logger.info("Dashboard created successfully.\n %s" % str(data))
          else:
            Logger.error("Failed creating dashboard: %s" % dashboard_def['title'])
          pass
        else:
          Logger.info('No update needed for dashboard = %s' % dashboard_def['title'])
      pass
    pass



