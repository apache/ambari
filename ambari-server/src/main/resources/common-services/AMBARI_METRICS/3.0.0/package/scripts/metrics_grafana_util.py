#!/usr/bin/env python3
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

import http.client

from ambari_commons.parallel_processing import (
  execute_in_parallel,
  SUCCESS,
)
from service_check import post_metrics_to_collector
from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management.libraries.script.script import Script
from resource_management import Template
from metrics_utils import url_host
from collections import namedtuple
from urllib.parse import urlparse
from base64 import b64encode
import random
import time
import json
import ambari_commons.network as network
import os

GRAFANA_SEARCH_BUILTIN_DASHBOARDS = "/api/search?tag=builtin"
GRAFANA_DATASOURCE_URL = "/api/datasources"
GRAFANA_USER_URL = "/api/user"
GRAFANA_DASHBOARDS_URL = "/api/dashboards/db"
METRICS_GRAFANA_DATASOURCE_NAME = "AMBARI_METRICS"

Server = namedtuple("Server", ["protocol", "host", "port", "user", "password"])


class GrafanaResponse(namedtuple("GrafanaResponseBase", ["status", "reason", "data"])):
  __slots__ = ()

  def read(self):
    return self.data


def _perform_grafana_request(method, url, server, payload=None, retry_unauthorized=False):
  import params

  grafana_https_enabled = server.protocol.lower() == "https"
  ca_certs = params.ams_grafana_ca_cert if grafana_https_enabled else None
  credentials = b64encode(f"{server.user}:{server.password}".encode()).decode()
  headers = {
    "Authorization": f"Basic {credentials}",
  }
  if payload is not None:
    headers["Content-Type"] = "application/json"
    headers["Content-Length"] = str(len(payload.encode("utf-8")))

  last_error = None
  for attempt in range(params.grafana_connect_attempts):
    connection = None
    try:
      Logger.info(f"Connecting ({method}) to {server.host}:{server.port}{url}")
      connection = network.get_http_connection(
        server.host,
        int(server.port),
        grafana_https_enabled,
        ca_certs,
        ssl_version=Script.get_force_https_protocol_value(),
      )
      connection.timeout = params.grafana_request_timeout
      connection.request(method, url, body=payload, headers=headers)
      response = connection.getresponse()
      data = response.read()
      Logger.info(f"Http response: {response.status} {response.reason}")
      buffered_response = GrafanaResponse(response.status, response.reason, data)
      if not (retry_unauthorized and response.status == 401):
        return buffered_response, data
    except (http.client.HTTPException, OSError) as error:
      last_error = error
    finally:
      if connection is not None:
        connection.close()

    if attempt + 1 < params.grafana_connect_attempts:
      Logger.info(
        f"Connection to Grafana failed. Next retry in {params.grafana_connect_retry_delay} seconds."
      )
      time.sleep(params.grafana_connect_retry_delay)

  if last_error is not None:
    raise Fail(f"Ambari Metrics Grafana update failed due to: {last_error}")
  return buffered_response, data


def perform_grafana_get_call(url, server):
  response, _ = _perform_grafana_request("GET", url, server)
  return response


def perform_grafana_put_call(url, id, payload, server):
  return _perform_grafana_request("PUT", f"{url}/{id}", server, payload)


def perform_grafana_post_call(url, payload, server):
  return _perform_grafana_request(
    "POST", url, server, payload, retry_unauthorized=True
  )


def perform_grafana_delete_call(url, server):
  response, _ = _perform_grafana_request("DELETE", url, server)
  return response


def is_unchanged_datasource_url(grafana_datasource_url, new_datasource_host):
  import params

  try:
    parsed_url = urlparse(grafana_datasource_url)
    parsed_port = parsed_url.port
  except (TypeError, ValueError):
    return False
  Logger.debug(
    "parsed url: scheme = %s, host = %s, port = %s"
    % (parsed_url.scheme, parsed_url.hostname, parsed_port)
  )
  Logger.debug(
    "collector: scheme = %s, host = %s, port = %s"
    % (
      params.metric_collector_protocol,
      new_datasource_host,
      params.metric_collector_port,
    )
  )

  return (
    parsed_url.scheme == params.metric_collector_protocol
    and parsed_url.hostname == new_datasource_host.strip("[]")
    and str(parsed_port) == params.metric_collector_port
  )


def do_ams_collector_post(metric_collector_host, params):
  ams_metrics_post_url = "/ws/v1/timeline/metrics/"
  random_value1 = random.random()
  headers = {"Content-type": "application/json"}
  ca_certs = os.path.join(
    params.ams_grafana_conf_dir, params.metric_truststore_ca_certs
  )

  current_time = int(time.time()) * 1000
  metric_json = Template(
    "smoketest_metrics.json.j2",
    hostname=params.hostname,
    random1=random_value1,
    current_time=current_time,
  ).get_content()

  post_metrics_to_collector(
    ams_metrics_post_url,
    metric_collector_host,
    params.metric_collector_port,
    params.metric_collector_https_enabled,
    metric_json,
    headers,
    ca_certs,
  )


def create_grafana_admin_pwd():
  import params

  serverCall1 = Server(
    protocol=params.ams_grafana_protocol.strip(),
    host=params.ams_grafana_host.strip(),
    port=params.ams_grafana_port,
    user=params.ams_grafana_admin_user,
    password=params.ams_grafana_admin_pwd,
  )

  response = perform_grafana_get_call(GRAFANA_USER_URL, serverCall1)
  if response.status == 401:
    serverCall2 = Server(
      protocol=params.ams_grafana_protocol.strip(),
      host=params.ams_grafana_host.strip(),
      port=params.ams_grafana_port,
      user=params.ams_grafana_admin_user,
      password="admin",
    )

    Logger.debug("Setting grafana admin password")
    pwd_data = {
      "oldPassword": "admin",
      "newPassword": params.ams_grafana_admin_pwd,
      "confirmNew": params.ams_grafana_admin_pwd,
    }
    password_json = json.dumps(pwd_data)

    (response, _) = perform_grafana_put_call(
      GRAFANA_USER_URL, "password", password_json, serverCall2
    )

    if response.status == 200:
      Logger.info("Ambari Metrics Grafana password updated.")

    elif response.status == 500:
      Logger.info("Ambari Metrics Grafana password update failed. Not retrying.")
      raise Fail(
        "Ambari Metrics Grafana password update failed. PUT request status: %s %s"
        % (response.status, response.reason)
      )
    else:
      raise Fail(
        "Ambari Metrics Grafana password creation failed. "
        "PUT request status: %s %s" % (response.status, response.reason)
      )
  elif response.status == 200:
    Logger.info("Grafana password update not required.")
  else:
    raise Fail(
      "Grafana user query failed: status %s %s"
      % (response.status, response.reason)
    )


def create_ams_datasource():
  import params

  server = Server(
    protocol=params.ams_grafana_protocol.strip(),
    host=params.ams_grafana_host.strip(),
    port=params.ams_grafana_port,
    user=params.ams_grafana_admin_user,
    password=params.ams_grafana_admin_pwd,
  )

  """
  Create AMS datasource in Grafana, if exsists make sure the collector url is accurate
  """
  Logger.info("Trying to find working metric collector")
  results = execute_in_parallel(
    do_ams_collector_post, params.ams_collector_hosts.split(","), params
  )
  new_datasource_host = ""

  for host in params.ams_collector_hosts.split(","):
    if host in results:
      if results[host].status == SUCCESS:
        new_datasource_host = host
        Logger.info(f"Found working collector on host {new_datasource_host}")
        break
      else:
        Logger.warning(results[host].result)

  if new_datasource_host == "":
    raise Fail("All Metrics Collectors are unavailable for Grafana datasource setup")

  Logger.info(f"New datasource host will be {new_datasource_host}")

  ams_datasource_json = Template(
    "metrics_grafana_datasource.json.j2",
    ams_datasource_name=METRICS_GRAFANA_DATASOURCE_NAME,
    ams_datasource_host=url_host(new_datasource_host),
  ).get_content()
  Logger.info("Checking if AMS Grafana datasource already exists")

  response = perform_grafana_get_call(GRAFANA_DATASOURCE_URL, server)
  create_datasource = True

  if response and response.status == 200:
    datasources = response.read()
    try:
      datasources_json = json.loads(datasources)
      if not isinstance(datasources_json, list):
        raise TypeError("datasources response must be a list")
    except (TypeError, ValueError) as error:
      raise Fail("Grafana returned an invalid datasource response") from error
    for i in range(0, len(datasources_json)):
      datasource = datasources_json[i]
      if not isinstance(datasource, dict):
        raise Fail("Grafana returned an invalid datasource entry")
      datasource_name = datasource.get("name")
      if datasource_name == METRICS_GRAFANA_DATASOURCE_NAME:
        create_datasource = False  # datasource already exists
        Logger.info(
          "Ambari Metrics Grafana datasource already present. Checking Metrics Collector URL"
        )
        datasource_url = datasource.get("url")

        update_datasource = False
        if is_unchanged_datasource_url(datasource_url, new_datasource_host):
          Logger.info("Metrics Collector URL validation succeeded.")
        else:
          Logger.info("Metrics Collector URL validation failed.")
          update_datasource = True

        datasource_type = datasource.get("type")
        try:
          new_datasource_def = json.loads(ams_datasource_json)
        except (TypeError, ValueError) as error:
          raise Fail("Generated Grafana datasource definition is invalid") from error
        new_datasource_type = new_datasource_def["type"]

        if datasource_type == new_datasource_type:
          Logger.info("Grafana datasource type validation succeeded.")
        else:
          Logger.info(
            f"Grafana datasource type validation failed. Old type = {datasource_type}, New type = {new_datasource_type}"
          )
          update_datasource = True

        if update_datasource:  # Metrics datasource present, but collector host is wrong or the datasource type is outdated.
          datasource_id = datasource.get("id")
          if not isinstance(datasource_id, int) or isinstance(datasource_id, bool):
            raise Fail("Grafana returned an invalid datasource identifier")
          Logger.info(f"Updating datasource, id = {datasource_id}")

          (response, _) = perform_grafana_put_call(
            GRAFANA_DATASOURCE_URL, datasource_id, ams_datasource_json, server
          )

          if response.status == 200:
            Logger.info("Ambari Metrics Grafana data source updated.")

          elif response.status == 500:
            Logger.info(
              "Ambari Metrics Grafana data source update failed. Not retrying."
            )
            raise Fail(
              "Ambari Metrics Grafana data source update failed. PUT request status: %s %s"
              % (response.status, response.reason)
            )
          else:
            raise Fail(
              "Ambari Metrics Grafana data source creation failed. "
              "PUT request status: %s %s"
              % (response.status, response.reason)
            )
  else:
    raise Fail(
      "Grafana datasource query failed: status %s %s"
      % (response.status, response.reason)
    )

  if not create_datasource:
    return
  Logger.info("Creating the Ambari Metrics Grafana datasource")

  (response, _) = perform_grafana_post_call(
    GRAFANA_DATASOURCE_URL, ams_datasource_json, server
  )

  if response.status == 200:
    Logger.info("Ambari Metrics Grafana data source created.")
  elif response.status == 500:
    Logger.info("Ambari Metrics Grafana data source creation failed. Not retrying.")
    raise Fail(
      "Ambari Metrics Grafana data source creation failed. POST request status: %s %s"
      % (response.status, response.reason)
    )
  else:
    Logger.info("Ambari Metrics Grafana data source creation failed.")
    raise Fail(
      "Ambari Metrics Grafana data source creation failed. POST request status: %s %s"
      % (response.status, response.reason)
    )


def create_ams_dashboards():
  """
  Create dashboards in grafana from the json files
  """
  import params

  server = Server(
    protocol=params.ams_grafana_protocol.strip(),
    host=params.ams_grafana_host.strip(),
    port=params.ams_grafana_port,
    user=params.ams_grafana_admin_user,
    password=params.ams_grafana_admin_pwd,
  )

  dashboard_files = params.get_grafana_dashboard_defs()
  version = params.get_ambari_version()
  if not version:
    raise Fail("Could not determine the Ambari version for Grafana dashboards")
  Logger.info(f"Checking dashboards to update for Ambari version : {version}")
  # Friendly representation of dashboard
  Dashboard = namedtuple("Dashboard", ["uri", "id", "title", "tags"])

  existing_dashboards = []
  response = perform_grafana_get_call(GRAFANA_SEARCH_BUILTIN_DASHBOARDS, server)
  if response and response.status == 200:
    data = response.read()
    try:
      dashboards = json.loads(data)
      if not isinstance(dashboards, list):
        raise TypeError("dashboard search response must be a list")
    except (TypeError, ValueError) as error:
      raise Fail("Grafana returned an invalid dashboard search response") from error

    for dashboard in dashboards:
      if not isinstance(dashboard, dict):
        raise Fail("Grafana returned an invalid dashboard search entry")
      required_fields = ("uri", "id", "title", "tags")
      if any(field not in dashboard for field in required_fields) or not isinstance(
        dashboard["tags"], list
      ):
        raise Fail("Grafana returned an incomplete dashboard search entry")
      if dashboard["title"] == "HBase - Performance":
        delete_response = perform_grafana_delete_call(
          "/api/dashboards/" + dashboard["uri"], server
        )
        if delete_response.status != 200:
          raise Fail(
            "Failed deleting obsolete Grafana dashboard: status %s %s"
            % (delete_response.status, delete_response.reason)
          )
      else:
        existing_dashboards.append(
          Dashboard(
            uri=dashboard["uri"],
            id=dashboard["id"],
            title=dashboard["title"],
            tags=dashboard["tags"],
          )
        )
  else:
    raise Fail(
      "Grafana dashboard search failed: status %s %s"
      % (response.status, response.reason)
    )

  Logger.debug(f"Dashboard definitions found = {str(dashboard_files)}")

  if not dashboard_files:
    raise Fail("No packaged Grafana dashboard definitions were found")

  for dashboard_file in dashboard_files:
    try:
      with open(dashboard_file, encoding="utf-8") as file:
        dashboard_def = json.load(file)
    except (OSError, TypeError, ValueError) as error:
      raise Fail(f"Unable to load dashboard JSON file {dashboard_file}") from error

    if not isinstance(dashboard_def, dict) or not isinstance(
      dashboard_def.get("title"), str
    ):
      raise Fail(f"Dashboard file {dashboard_file} has an invalid definition")
    update_def = True
    if "id" in dashboard_def:
      dashboard_def["id"] = None

    dashboard_version = str(dashboard_def.get("version", "-1"))
    if "tags" in dashboard_def:
      if not isinstance(dashboard_def["tags"], list):
        raise Fail(f"Dashboard file {dashboard_file} has invalid tags")
      dashboard_def["tags"].extend(["builtin", version, dashboard_version])
    else:
      dashboard_def["tags"] = ["builtin", version, dashboard_version]
    for dashboard in existing_dashboards:
      if dashboard.title == dashboard_def["title"]:
        if version not in dashboard.tags:
          update_def = True
        elif dashboard_version not in dashboard.tags:
          update_def = True
          Logger.info(
            "Dashboard definition for %s with tags: %s will be updated as the dashboard version is changed to %s"
            % (dashboard.title, dashboard.tags, dashboard_version)
          )
        else:
          update_def = False
    if update_def:
      Logger.info(
        "Updating dashboard definition for %s with tags: %s"
        % (dashboard_def["title"], dashboard_def["tags"])
      )

      dashboard_def_payload = {"dashboard": dashboard_def, "overwrite": True}
      payload = json.dumps(dashboard_def_payload).strip()

      (response, _) = perform_grafana_post_call(
        GRAFANA_DASHBOARDS_URL, payload, server
      )

      if response and response.status == 200:
        Logger.info(f"Dashboard {dashboard_def['title']} updated successfully")
      else:
        raise Fail(
          f"Failed updating Grafana dashboard {dashboard_def['title']}: "
          f"status {response.status} {response.reason}"
        )
    else:
      Logger.info(f"No update needed for dashboard = {dashboard_def['title']}")
