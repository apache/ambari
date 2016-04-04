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
from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management import Template
from collections import namedtuple
from urlparse import urlparse
from base64 import b64encode
import time
import socket
import ambari_simplejson as json
import network

GRAFANA_CONNECT_TRIES = 5
GRAFANA_CONNECT_TIMEOUT = 10
GRAFANA_SEARCH_BULTIN_DASHBOARDS = "/api/search?tag=builtin"
GRAFANA_DATASOURCE_URL = "/api/datasources"
GRAFANA_DASHBOARDS_URL = "/api/dashboards/db"
METRICS_GRAFANA_DATASOURCE_NAME = "AMBARI_METRICS"

Server = namedtuple('Server', [ 'protocol', 'host', 'port', 'user', 'password' ])

def perform_grafana_get_call(url, server):
  grafana_https_enabled = server.protocol.lower() == 'https'
  response = None

  for i in xrange(0, GRAFANA_CONNECT_TRIES):
    try:
      conn = network.get_http_connection(server.host,
                                         int(server.port),
                                         grafana_https_enabled)

      userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
      headers = { 'Authorization' : 'Basic %s' %  userAndPass }

      Logger.info("Connecting (GET) to %s:%s%s" % (server.host, server.port, url))

      conn.request("GET", url, headers = headers)
      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < GRAFANA_CONNECT_TRIES - 1:
        time.sleep(GRAFANA_CONNECT_TIMEOUT)
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (GRAFANA_CONNECT_TIMEOUT))
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return response

def perform_grafana_put_call(url, id, payload, server):
  response = None
  data = None
  userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
  headers = {"Content-Type": "application/json",
             'Authorization' : 'Basic %s' %  userAndPass }
  grafana_https_enabled = server.protocol.lower() == 'https'

  for i in xrange(0, GRAFANA_CONNECT_TRIES):
    try:
      conn = network.get_http_connection(server.host, int(server.port), grafana_https_enabled)
      conn.request("PUT", url + "/" + str(id), payload, headers)
      response = conn.getresponse()
      data = response.read()
      Logger.info("Http data: %s" % data)
      conn.close()
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < GRAFANA_CONNECT_TRIES - 1:
        time.sleep(GRAFANA_CONNECT_TIMEOUT)
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (GRAFANA_CONNECT_TIMEOUT))
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return (response, data)

def perform_grafana_post_call(url, payload, server):
  response = None
  data = None
  userAndPass = b64encode('{0}:{1}'.format(server.user, server.password))
  Logger.debug('POST payload: %s' % payload)
  headers = {"Content-Type": "application/json", "Content-Length" : len(payload),
             'Authorization' : 'Basic %s' %  userAndPass}
  grafana_https_enabled = server.protocol.lower() == 'https'

  for i in xrange(0, GRAFANA_CONNECT_TRIES):
    try:
      Logger.info("Connecting (POST) to %s:%s%s" % (server.host, server.port, url))
      conn = network.get_http_connection(server.host,
                                         int(server.port),
                                         grafana_https_enabled)
      
      conn.request("POST", url, payload, headers)

      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
      if response.status == 401: #Intermittent error thrown from Grafana
        if i < GRAFANA_CONNECT_TRIES - 1:
          time.sleep(GRAFANA_CONNECT_TIMEOUT)
          Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                  % (GRAFANA_CONNECT_TIMEOUT))
          continue
      data = response.read()
      Logger.info("Http data: %s" % data)
      conn.close()
      break
    except (httplib.HTTPException, socket.error) as ex:
      if i < GRAFANA_CONNECT_TRIES - 1:
        time.sleep(GRAFANA_CONNECT_TIMEOUT)
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (GRAFANA_CONNECT_TIMEOUT))
        continue
      else:
        raise Fail("Ambari Metrics Grafana update failed due to: %s" % str(ex))
      pass

  return (response, data)

def is_unchanged_datasource_url(datasource_url):
  import params
  parsed_url = urlparse(datasource_url)
  Logger.debug("parsed url: scheme = %s, host = %s, port = %s" % (
    parsed_url.scheme, parsed_url.hostname, parsed_url.port))
  Logger.debug("collector: scheme = %s, host = %s, port = %s" %
              (params.metric_collector_protocol, params.metric_collector_host,
               params.metric_collector_port))

  return parsed_url.scheme.strip() == params.metric_collector_protocol.strip() and \
         parsed_url.hostname.strip() == params.metric_collector_host.strip() and \
         str(parsed_url.port) == params.metric_collector_port


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
  ams_datasource_json = Template('metrics_grafana_datasource.json.j2',
                                 ams_datasource_name=METRICS_GRAFANA_DATASOURCE_NAME).get_content()

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

        if is_unchanged_datasource_url(datasource_url):
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
  response = perform_grafana_get_call(GRAFANA_SEARCH_BULTIN_DASHBOARDS, server)
  if response and response.status == 200:
    data = response.read()
    try:
      datasources = json.loads(data)
    except:
      Logger.error("Unable to parse JSON response from grafana request: %s" %
                   GRAFANA_SEARCH_BULTIN_DASHBOARDS)
      Logger.info(data)
      return

    for datasource in datasources:
      existing_dashboards.append(
          Dashboard(uri = datasource['uri'], id = datasource['id'],
                    title = datasource['title'], tags = datasource['tags'])
        )
    pass
  else:
    Logger.error("Failed to execute search query on Grafana dashboards. "
                 "query = %s\n statuscode = %s\n reason = %s\n data = %s\n" %
                 (GRAFANA_SEARCH_BULTIN_DASHBOARDS, response.status, response.reason, response.read()))
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

        dashboard_def['overwrite'] = True
        
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
          dashboard_def_payload = { "dashboard" : dashboard_def }
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



