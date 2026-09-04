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

from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management import Script
from resource_management import Template
from resource_management.libraries.functions.curl_krb_request import curl_krb_request

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.parallel_processing import (
  execute_in_parallel,
  SUCCESS,
)
from metrics_utils import url_host

import http.client
import ambari_commons.network as network
import urllib.parse
import json
import os
import random
import time


class AMSServiceCheck(Script):
  AMS_METRICS_POST_URL = "/ws/v1/timeline/metrics/"
  AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics?%s"
  AMS_CONNECT_TRIES = 10
  AMS_CONNECT_TIMEOUT = 10
  AMS_READ_TRIES = 5
  AMS_READ_TIMEOUT = 10

  def service_check_for_single_host(self, metric_collector_host, params):
    random_value1 = random.random()

    current_time = int(time.time()) * 1000
    metric_json = Template(
      "smoketest_metrics.json.j2",
      hostname=params.hostname,
      random1=random_value1,
      current_time=current_time,
    ).get_content()
    try:
      if is_spnego_enabled(params):
        header = "Content-Type: application/json"
        method = "POST"
        tmp_dir = Script.get_tmp_dir()

        protocol = "https" if params.metric_collector_https_enabled else "http"
        port = str(params.metric_collector_port)
        uri = f"{protocol}://{url_host(metric_collector_host)}:{port}{self.AMS_METRICS_POST_URL}"

        call_curl_krb_request(
          tmp_dir,
          params.smoke_user_keytab,
          params.smoke_user_princ,
          uri,
          params.kinit_path_local,
          params.smoke_user,
          self.AMS_CONNECT_TIMEOUT,
          method,
          metric_json,
          header,
          tries=self.AMS_CONNECT_TRIES,
        )
      else:
        headers = {"Content-type": "application/json"}
        ca_certs = os.path.join(
          params.ams_monitor_conf_dir, params.metric_truststore_ca_certs
        )
        post_metrics_to_collector(
          self.AMS_METRICS_POST_URL,
          metric_collector_host,
          params.metric_collector_port,
          params.metric_collector_https_enabled,
          metric_json,
          headers,
          ca_certs,
          self.AMS_CONNECT_TRIES,
          self.AMS_CONNECT_TIMEOUT,
        )

      get_metrics_parameters = {
        "metricNames": "AMBARI_METRICS.SmokeTest.FakeMetric",
        "appId": "amssmoketestfake",
        "hostname": params.hostname,
        "startTime": current_time - 60000,
        "endTime": current_time + 61000,
        "precision": "seconds",
        "grouped": "false",
      }
      encoded_get_metrics_parameters = urllib.parse.urlencode(get_metrics_parameters)

      if is_spnego_enabled(params):
        method = "GET"
        uri = "{0}://{1}:{2}{3}".format(
          protocol,
          url_host(metric_collector_host),
          port,
          self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters,
        )

        call_curl_krb_request(
          tmp_dir,
          params.smoke_user_keytab,
          params.smoke_user_princ,
          uri,
          params.kinit_path_local,
          params.smoke_user,
          self.AMS_READ_TIMEOUT,
          method,
          tries=self.AMS_READ_TRIES,
          current_time=current_time,
          random_value=random_value1,
        )
      else:
        Logger.info(
          "Connecting (GET) to %s:%s%s"
          % (
            metric_collector_host,
            params.metric_collector_port,
            self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters,
          )
        )
        for i in range(0, self.AMS_READ_TRIES):
          conn = None
          try:
            conn = network.get_http_connection(
              metric_collector_host,
              int(params.metric_collector_port),
              params.metric_collector_https_enabled,
              ca_certs,
              ssl_version=Script.get_force_https_protocol_value(),
            )
            conn.timeout = self.AMS_CONNECT_TIMEOUT
            conn.request(
              "GET", self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters
            )
            response = conn.getresponse()
            Logger.info(
              f"Http response for host {metric_collector_host} : {response.status} {response.reason}"
            )
            data = response.read()
          except (http.client.HTTPException, OSError) as error:
            if i + 1 < self.AMS_READ_TRIES:
              Logger.info(
                f"Metrics read failed; retrying in {self.AMS_READ_TIMEOUT} seconds"
              )
              time.sleep(self.AMS_READ_TIMEOUT)
              continue
            raise Fail(f"Metrics read failed: {error}") from error
          finally:
            if conn is not None:
              conn.close()

          if response.status == 200:
            Logger.info(f"Metrics were retrieved from host {metric_collector_host}")
          else:
            raise Fail(
              "Metrics were not retrieved from host %s. GET request status: %s %s"
              % (metric_collector_host, response.status, response.reason)
            )
          values_are_present = metrics_response_contains_values(
            data, current_time, random_value1
          )
          if values_are_present:
            Logger.info(
              f"Smoke-test values were found in the response from {metric_collector_host}"
            )

          if not values_are_present:
            if (
              i < self.AMS_READ_TRIES - 1
            ):
              Logger.info(
                "Values weren't stored yet. Retrying in %s seconds."
                % (self.AMS_READ_TIMEOUT)
              )
              time.sleep(self.AMS_READ_TIMEOUT)
            else:
              raise Fail(
                f"Values {random_value1} and {current_time} were not found in the response."
              )
          else:
            break
    except Fail as error:
      Logger.warning(
        f"Ambari Metrics service check failed on collector host {metric_collector_host}: {error}"
      )
      raise

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def service_check(self, env):
    import params

    Logger.info("Ambari Metrics service check was started.")
    env.set_params(params)

    collector_hosts = [
      host.strip() for host in params.ams_collector_hosts.split(",") if host.strip()
    ]
    results = execute_in_parallel(
      self.service_check_for_single_host, collector_hosts, params
    )

    for host in collector_hosts:
      if host in results:
        if results[host].status == SUCCESS:
          Logger.info("Ambari Metrics service check passed on host " + host)
          return
        else:
          Logger.warning(results[host].result)
    raise Fail("All metrics collectors are unavailable.")


def is_spnego_enabled(params):
  return (
    params.security_enabled
    and "core-site" in params.config["configurations"]
    and "hadoop.http.authentication.type"
    in params.config["configurations"]["core-site"]
    and params.config["configurations"]["core-site"]["hadoop.http.authentication.type"]
    == "kerberos"
    and "hadoop.http.filter.initializers"
    in params.config["configurations"]["core-site"]
    and "org.apache.hadoop.security.AuthenticationFilterInitializer"
    in params.config["configurations"]["core-site"]["hadoop.http.filter.initializers"]
  )


def metrics_response_contains_values(data, current_time, random_value):
  try:
    payload = json.loads(data)
    metrics = payload["metrics"]
    if not isinstance(metrics, list):
      raise TypeError("metrics must be a list")
    for metric in metrics:
      values = metric["metrics"]
      if not isinstance(values, dict):
        raise TypeError("metric values must be an object")
      first_value = values.get(str(current_time))
      second_value = values.get(str(current_time + 1000))
      if (
        isinstance(first_value, (int, float))
        and not isinstance(first_value, bool)
        and isinstance(second_value, (int, float))
        and not isinstance(second_value, bool)
        and abs(first_value - random_value) < 0.0000001
        and abs(second_value - current_time) < 1
      ):
        return True
  except (KeyError, TypeError, ValueError) as error:
    raise Fail("Metrics Collector returned an invalid metrics response") from error
  return False


def call_curl_krb_request(
  tmp_dir,
  user_keytab,
  user_princ,
  uri,
  kinit_path,
  user,
  connection_timeout,
  method="GET",
  metric_json="",
  header="",
  tries=1,
  current_time=0,
  random_value=0,
):
  for i in range(0, tries):
    try:
      Logger.info(f"Connecting ({method}) to {uri}")
      response = None
      errmsg = None
      response, errmsg, _ = curl_krb_request(
        tmp_dir,
        user_keytab,
        user_princ,
        uri,
        "ams_service_check",
        kinit_path,
        False,
        "AMS Service Check",
        user,
        connection_timeout=connection_timeout,
        kinit_timer_ms=0,
        method=method,
        body=metric_json,
        header=header,
      )
    except Exception as exception:
      if i < tries - 1:
        time.sleep(connection_timeout)
        Logger.info(
          f"Connection failed for {uri}. Next retry in {connection_timeout} seconds."
        )
        continue
      else:
        raise Fail(f"Unable to {method} metrics on: {uri}. Exception: {str(exception)}")
    if not response:
      Logger.error(f"Unable to {method} metrics on: {uri}. Error: {errmsg}")
      if i + 1 < tries:
        time.sleep(connection_timeout)
        continue
      raise Fail(f"Unable to {method} metrics on: {uri}. Error: {errmsg}")

    if method == "GET":
      values_are_present = metrics_response_contains_values(
        response, current_time, random_value
      )
      if values_are_present:
        Logger.info(f"Smoke-test values were found in the response from {uri}")

      if not values_are_present:
        if i < tries - 1:
          Logger.info(f"Values weren't stored yet. Retrying in {tries} seconds.")
          time.sleep(connection_timeout)
        else:
          raise Fail(
            f"Values {random_value} and {current_time} were not found in the response."
          )
      else:
        break
    else:
      break


def post_metrics_to_collector(
  ams_metrics_post_url,
  metric_collector_host,
  metric_collector_port,
  metric_collector_https_enabled,
  metric_json,
  headers,
  ca_certs,
  tries=1,
  connect_timeout=10,
):
  for i in range(0, tries):
    conn = None
    try:
      Logger.info(
        "Connecting (POST) to %s:%s%s"
        % (metric_collector_host, metric_collector_port, ams_metrics_post_url)
      )
      conn = network.get_http_connection(
        metric_collector_host,
        int(metric_collector_port),
        metric_collector_https_enabled,
        ca_certs,
        ssl_version=Script.get_force_https_protocol_value(),
      )
      conn.timeout = connect_timeout
      conn.request("POST", ams_metrics_post_url, metric_json, headers)

      response = conn.getresponse()
      Logger.info(
        f"Http response for host {metric_collector_host}: {response.status} {response.reason}"
      )
      response.read()
    except (http.client.HTTPException, OSError) as ex:
      if i < tries - 1:
        time.sleep(connect_timeout)
        Logger.info(
          "Connection failed for host %s. Next retry in %s seconds."
          % (metric_collector_host, connect_timeout)
        )
        continue
      else:
        raise Fail("Metrics were not saved. Connection failed.") from ex
    finally:
      if conn is not None:
        conn.close()

    if response.status == 200:
      Logger.info("Metrics were saved.")
      break
    else:
      Logger.info("Metrics were not saved.")
      if i < tries - 1:
        time.sleep(connect_timeout)
        Logger.info(f"Next retry in {connect_timeout} seconds.")
      else:
        raise Fail(
          "Metrics were not saved. POST request status: %s %s"
          % (response.status, response.reason)
        )


if __name__ == "__main__":
  AMSServiceCheck().execute()
