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
from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management import Template

import httplib
import time
import socket
import json

def create_ams_datasource():

  import params
  GRAFANA_CONNECT_TRIES = 5
  GRAFANA_CONNECT_TIMEOUT = 15
  GRAFANA_URL = "/api/datasources"
  METRICS_GRAFANA_DATASOURCE_NAME = "AMBARI_METRICS"

  headers = {"Content-type": "application/json"}

  Logger.info("Checking if AMS Grafana datasource already exists")
  Logger.info("Connecting (GET) to %s:%s%s" % (params.hostname,
                                               params.ams_grafana_port,
                                               GRAFANA_URL))
# TODO add https support
  conn = httplib.HTTPConnection(params.hostname,
                                int(params.ams_grafana_port))

  conn.request("GET", GRAFANA_URL)
  response = conn.getresponse()
  Logger.info("Http response: %s %s" % (response.status, response.reason))

  if(response.status == 200):
    datasources = response.read()
    datasources_json = json.loads(datasources)
    for i in xrange(0, len(datasources_json)):
      datasource_name = datasources_json[i]["name"]
      if(datasource_name == METRICS_GRAFANA_DATASOURCE_NAME):

        Logger.info("Ambari Metrics Grafana datasource already present. Checking Metrics Collector URL")
        datasource_url = datasources_json[i]["url"]

        if datasource_url == (params.ams_grafana_protocol + "://"
                                + params.metric_collector_host + ":"
                                + params.metric_collector_port):
          Logger.info("Metrics Collector URL validation succeeded. Skipping datasource creation")
          GRAFANA_CONNECT_TRIES = 0 # No need to create datasource again

        else: # Metrics datasource present, but collector host is wrong.

          Logger.info("Metrics Collector URL validation failed.")
          datasource_id = datasources_json[i]["id"]
          Logger.info("Deleting obselete Metrics datasource.")
          conn = httplib.HTTPConnection(params.hostname, int(params.ams_grafana_port))
          conn.request("DELETE", GRAFANA_URL + "/" + str(datasource_id))
          response = conn.getresponse()
          Logger.info("Http response: %s %s" % (response.status, response.reason))

        break
  else:
    Logger.info("Error checking for Ambari Metrics Grafana datasource. Will attempt to create.")

  if GRAFANA_CONNECT_TRIES > 0:
    Logger.info("Attempting to create Ambari Metrics Grafana datasource")

  for i in xrange(0, GRAFANA_CONNECT_TRIES):
    try:
      ams_datasource_json = Template('metrics_grafana_datasource.json.j2',
                             ams_datasource_name=METRICS_GRAFANA_DATASOURCE_NAME,
                             ams_grafana_protocol=params.ams_grafana_protocol,
                             ams_collector_host=params.metric_collector_host,
                             ams_collector_port=params.metric_collector_port).get_content()

      Logger.info("Generated datasource:\n%s" % ams_datasource_json)

      Logger.info("Connecting (POST) to %s:%s%s" % (params.hostname,
                                                    params.ams_grafana_port,
                                                    GRAFANA_URL))
      conn = httplib.HTTPConnection(params.hostname,
                                    int(params.ams_grafana_port))
      conn.request("POST", GRAFANA_URL, ams_datasource_json, headers)

      response = conn.getresponse()
      Logger.info("Http response: %s %s" % (response.status, response.reason))
    except (httplib.HTTPException, socket.error) as ex:
      if i < GRAFANA_CONNECT_TRIES - 1:
        time.sleep(GRAFANA_CONNECT_TIMEOUT)
        Logger.info("Connection to Grafana failed. Next retry in %s seconds."
                    % (GRAFANA_CONNECT_TIMEOUT))
        continue
      else:
        raise Fail("Ambari Metrics Grafana datasource not created")

    data = response.read()
    Logger.info("Http data: %s" % data)
    conn.close()

    if response.status == 200:
      Logger.info("Ambari Metrics Grafana data source created.")
      break
    elif response.status == 500:
      Logger.info("Ambari Metrics Grafana data source creation failed. Not retrying.")
      raise Fail("Ambari Metrics Grafana data source creation failed. POST request status: %s %s \n%s" %
                 (response.status, response.reason, data))
    else:
      Logger.info("Ambari Metrics Grafana data source creation failed.")
      if i < GRAFANA_CONNECT_TRIES - 1:
        time.sleep(GRAFANA_CONNECT_TIMEOUT)
        Logger.info("Next retry in %s seconds."
                  % (GRAFANA_CONNECT_TIMEOUT))
      else:
        raise Fail("Ambari Metrics Grafana data source creation failed. POST request status: %s %s \n%s" %
                 (response.status, response.reason, data))


