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

from http.server import BaseHTTPRequestHandler, HTTPServer
import logging
from socketserver import ThreadingMixIn
import threading
from urllib.parse import urlsplit

from ambari_agent.metrics.core import CollectorRegistry
from ambari_agent.metrics.linux import default_collectors
from ambari_agent.metrics.telemetry import (
  PROMETHEUS_CONTENT_TYPE,
  KerberosTelemetryFetcher,
  TelemetryHttpClient,
  TelemetryRouteRegistry,
  TelemetryScrapeError,
)


logger = logging.getLogger(__name__)


class MetricsHTTPServer(ThreadingMixIn, HTTPServer):
  allow_reuse_address = True
  daemon_threads = True


def create_request_handler(registry, telemetry_routes=None):
  class MetricsRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
      self._respond(include_body=True)

    def do_HEAD(self):
      self._respond(include_body=False)

    def _respond(self, include_body):
      path = urlsplit(self.path).path
      if path == "/metrics":
        body = registry.render()
        content_type = PROMETHEUS_CONTENT_TYPE
        status = 200
      elif path.startswith("/metrics/components/"):
        route_id = path[len("/metrics/components/") :]
        if telemetry_routes is None or not route_id or "/" in route_id:
          body = b"Not Found\n"
          content_type = "text/plain; charset=utf-8"
          status = 404
        else:
          try:
            response = telemetry_routes.scrape(route_id)
            body = response.body
            content_type = response.content_type
            status = 200
          except KeyError:
            body = b"Not Found\n"
            content_type = "text/plain; charset=utf-8"
            status = 404
          except TelemetryScrapeError as err:
            body = (str(err) + "\n").encode("utf-8")
            content_type = "text/plain; charset=utf-8"
            status = err.status
      elif path == "/-/healthy":
        body = b"Ambari Agent Prometheus exporter is healthy.\n"
        content_type = "text/plain; charset=utf-8"
        status = 200
      else:
        body = b"Not Found\n"
        content_type = "text/plain; charset=utf-8"
        status = 404

      self.send_response(status)
      self.send_header("Content-Type", content_type)
      self.send_header("Content-Length", str(len(body)))
      self.end_headers()
      if include_body:
        self.wfile.write(body)

    def log_message(self, message_format, *args):
      logger.debug("Prometheus endpoint: " + message_format, *args)

  return MetricsRequestHandler


class PrometheusMetricsServer(threading.Thread):
  def __init__(
    self,
    initializer_module,
    registry=None,
    server_class=None,
    telemetry_routes=None,
  ):
    super().__init__(name="prometheus-metrics-server")
    self.agent_stop_event = initializer_module.stop_event
    self.config = initializer_module.config
    self.enabled = self.config.prometheus_metrics_enabled
    self.bind_address = self.config.prometheus_metrics_bind_address
    self.port = self.config.prometheus_metrics_port
    self.registry = registry or CollectorRegistry(default_collectors())
    telemetry_cache = getattr(initializer_module, "telemetry_cache", None)
    self.telemetry_routes = telemetry_routes
    if self.telemetry_routes is None and telemetry_cache is not None:
      http_client = TelemetryHttpClient(
        kerberos_fetcher=KerberosTelemetryFetcher(self.config)
      )
      self.telemetry_routes = TelemetryRouteRegistry(
        telemetry_cache, http_client=http_client
      )
    if self.telemetry_routes is not None:
      self.registry.register(self.telemetry_routes)
    self.server_class = server_class or MetricsHTTPServer
    self.http_server = None
    self.startup_error = None
    self.ready_event = threading.Event()
    self.stop_event = threading.Event()

  def run(self):
    if not self.enabled:
      self.ready_event.set()
      return

    try:
      handler = create_request_handler(self.registry, self.telemetry_routes)
      self.http_server = self.server_class((self.bind_address, self.port), handler)
      self.http_server.timeout = 0.5
      logger.info(
        "Ambari Agent Prometheus endpoint listening on %s:%s",
        self.bind_address,
        self.http_server.server_address[1],
      )
      self.ready_event.set()

      while not self.agent_stop_event.is_set() and not self.stop_event.is_set():
        self.http_server.handle_request()
    except Exception as err:
      self.startup_error = err
      self.ready_event.set()
      logger.exception("Ambari Agent Prometheus endpoint failed")
    finally:
      if self.http_server is not None:
        self.http_server.server_close()
      self.ready_event.set()

  def stop(self):
    self.stop_event.set()

  @property
  def server_address(self):
    if self.http_server is None:
      return None
    return self.http_server.server_address
