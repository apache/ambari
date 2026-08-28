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

from collections import OrderedDict, defaultdict
import hashlib
import json
import math
import re
import threading
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from ambari_agent.metrics.core import (
  LABEL_NAME_PATTERN,
  METRIC_NAME_PATTERN,
  MetricFamily,
  render_metrics,
)


PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8"
ROUTE_ID_PATTERN = re.compile(r"^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$")
SUPPORTED_FORMATS = frozenset(("prometheus_text", "jmx_json"))
SUPPORTED_TYPES = frozenset(("counter", "gauge"))
PROMETHEUS_SAMPLE_PATTERN = re.compile(
  r"^[a-zA-Z_:][a-zA-Z0-9_:]*(?:\{.*\})?[ \t]+"
  r"(?:[-+]?(?:(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][-+]?[0-9]+)?)"
  r"|NaN|[+-]Inf)(?:[ \t]+[-+]?[0-9]+)?$"
)
MAX_PROMETHEUS_LINE_BYTES = 1024 * 1024


class TelemetryConfigError(ValueError):
  pass


class TelemetryScrapeError(Exception):
  def __init__(self, message, status=502):
    super().__init__(message)
    self.status = status


class TelemetryResponse:
  def __init__(self, body, content_type=PROMETHEUS_CONTENT_TYPE):
    self.body = body
    self.content_type = content_type


class _RejectRedirects(HTTPRedirectHandler):
  def redirect_request(self, req, fp, code, msg, headers, newurl):
    return None


def profile_digest(profile):
  encoded = json.dumps(
    profile, sort_keys=True, separators=(",", ":"), ensure_ascii=True
  ).encode("utf-8")
  return "sha256:" + hashlib.sha256(encoded).hexdigest()


def validate_prometheus_text(payload):
  if not isinstance(payload, bytes) or not payload:
    raise TelemetryScrapeError(
      "Telemetry upstream returned an empty Prometheus response"
    )
  try:
    document = payload.decode("utf-8")
  except UnicodeDecodeError as err:
    raise TelemetryScrapeError(
      "Telemetry upstream returned non-UTF-8 Prometheus text"
    ) from err
  if "\x00" in document:
    raise TelemetryScrapeError(
      "Telemetry upstream returned invalid Prometheus text"
    )

  sample_count = 0
  for line in document.splitlines():
    if len(line.encode("utf-8")) > MAX_PROMETHEUS_LINE_BYTES:
      raise TelemetryScrapeError(
        "Telemetry upstream returned an oversized Prometheus line"
      )
    line = line.strip()
    if not line or line.startswith("#"):
      continue
    if not PROMETHEUS_SAMPLE_PATTERN.match(line):
      raise TelemetryScrapeError(
        "Telemetry upstream returned malformed Prometheus text"
      )
    sample_count += 1

  if sample_count == 0:
    raise TelemetryScrapeError(
      "Telemetry upstream returned no Prometheus samples"
    )


def validate_telemetry_bundle(assignment, profiles):
  if not isinstance(assignment, dict) or assignment.get("schemaVersion") != 1:
    raise TelemetryConfigError("Telemetry assignment schemaVersion must be 1")
  targets = assignment.get("targets")
  if not isinstance(targets, list):
    raise TelemetryConfigError("Telemetry assignment targets must be a list")

  route_ids = set()
  for target in targets:
    _validate_target(target, profiles)
    route_id = target["id"]
    if route_id in route_ids:
      raise TelemetryConfigError('Duplicate telemetry route "{}"'.format(route_id))
    route_ids.add(route_id)


def validate_profile(profile):
  if not isinstance(profile, dict) or profile.get("schemaVersion") != 1:
    raise TelemetryConfigError("Telemetry profile schemaVersion must be 1")
  if not isinstance(profile.get("id"), str) or not profile["id"]:
    raise TelemetryConfigError("Telemetry profile id must be a non-empty string")
  rules = profile.get("rules")
  if not isinstance(rules, list) or not rules:
    raise TelemetryConfigError("Telemetry profile rules must be a non-empty list")
  max_series = profile.get("maxSeries", 10000)
  if (
    not isinstance(max_series, int)
    or isinstance(max_series, bool)
    or not 1 <= max_series <= 100000
  ):
    raise TelemetryConfigError("Telemetry profile maxSeries must be between 1 and 100000")

  families = {}
  for rule in rules:
    _validate_rule(rule, families)


def _validate_target(target, profiles):
  if not isinstance(target, dict):
    raise TelemetryConfigError("Telemetry target must be an object")
  route_id = target.get("id")
  if not isinstance(route_id, str) or not ROUTE_ID_PATTERN.match(route_id):
    raise TelemetryConfigError('Invalid telemetry route id "{}"'.format(route_id))
  source_format = target.get("format")
  if source_format not in SUPPORTED_FORMATS:
    raise TelemetryConfigError('Unsupported telemetry format "{}"'.format(source_format))

  source_url = target.get("url")
  parsed_url = urlsplit(source_url if isinstance(source_url, str) else "")
  if (
    parsed_url.scheme not in ("http", "https")
    or not parsed_url.hostname
    or parsed_url.username
    or parsed_url.password
    or parsed_url.query
    or parsed_url.fragment
  ):
    raise TelemetryConfigError('Invalid telemetry target URL "{}"'.format(source_url))

  timeout = target.get("timeoutSeconds", 5)
  if (
    isinstance(timeout, bool)
    or not isinstance(timeout, (int, float))
    or not 0 < timeout <= 60
  ):
    raise TelemetryConfigError("Telemetry timeoutSeconds must be greater than 0 and at most 60")
  max_bytes = target.get("maxResponseBytes", 32 * 1024 * 1024)
  if (
    isinstance(max_bytes, bool)
    or not isinstance(max_bytes, int)
    or not 1024 <= max_bytes <= 64 * 1024 * 1024
  ):
    raise TelemetryConfigError("Telemetry maxResponseBytes must be between 1024 and 67108864")
  max_concurrent = target.get("maxConcurrentRequests", 2)
  if (
    isinstance(max_concurrent, bool)
    or not isinstance(max_concurrent, int)
    or not 1 <= max_concurrent <= 16
  ):
    raise TelemetryConfigError(
      "Telemetry maxConcurrentRequests must be between 1 and 16"
    )

  auth = target.get("auth", {})
  if not isinstance(auth, dict):
    raise TelemetryConfigError("Telemetry authentication must be an object")
  auth_type = auth.get("type", "none")
  if auth_type not in ("none", "kerberos"):
    raise TelemetryConfigError('Unsupported telemetry authentication "{}"'.format(auth_type))
  if auth_type == "kerberos":
    for field in ("principal", "keytab"):
      if not isinstance(auth.get(field), str) or not auth[field]:
        raise TelemetryConfigError(
          'Kerberos telemetry authentication requires "{}"'.format(field)
        )

  profile_hash = target.get("profileHash")
  if source_format == "jmx_json":
    if profile_hash not in profiles:
      raise TelemetryConfigError(
        'Telemetry route "{}" references a missing profile'.format(route_id)
      )
    validate_profile(profiles[profile_hash])
  elif profile_hash is not None:
    raise TelemetryConfigError(
      'Prometheus pass-through route "{}" must not reference a profile'.format(route_id)
    )


def _validate_rule(rule, families):
  if not isinstance(rule, dict):
    raise TelemetryConfigError("Telemetry profile rule must be an object")
  bean = rule.get("bean")
  if (
    not isinstance(bean, dict)
    or not isinstance(bean.get("domain"), str)
    or not bean["domain"]
  ):
    raise TelemetryConfigError("Telemetry rule bean domain must be a non-empty string")
  properties = bean.get("properties")
  if not isinstance(properties, dict) or not properties:
    raise TelemetryConfigError("Telemetry rule bean properties must be a non-empty object")
  if any(
    not isinstance(key, str) or not isinstance(value, str)
    for key, value in properties.items()
  ):
    raise TelemetryConfigError("Telemetry bean property names and values must be strings")

  labels = rule.get("labels", {})
  if not isinstance(labels, dict):
    raise TelemetryConfigError("Telemetry rule labels must be an object")
  for label_name, source in labels.items():
    if not LABEL_NAME_PATTERN.match(label_name) or label_name.startswith("__"):
      raise TelemetryConfigError('Invalid telemetry label "{}"'.format(label_name))
    if not isinstance(source, dict) or set(source) not in ({"property"}, {"value"}):
      raise TelemetryConfigError(
        'Telemetry label "{}" must define property or value'.format(label_name)
      )
    if "property" in source and (
      not isinstance(source["property"], str) or not source["property"]
    ):
      raise TelemetryConfigError(
        'Telemetry label "{}" property must be a non-empty string'.format(
          label_name
        )
      )
    if "value" in source and not isinstance(source["value"], str):
      raise TelemetryConfigError(
        'Telemetry label "{}" value must be a string'.format(label_name)
      )

  attributes = rule.get("attributes")
  if not isinstance(attributes, dict) or not attributes:
    raise TelemetryConfigError("Telemetry rule attributes must be a non-empty object")
  for source_name, definition in attributes.items():
    if not isinstance(source_name, str) or not source_name:
      raise TelemetryConfigError("Telemetry source attribute must be a non-empty string")
    _validate_metric_definition(definition, families)


def _validate_metric_definition(definition, families):
  if not isinstance(definition, dict):
    raise TelemetryConfigError("Telemetry metric definition must be an object")
  name = definition.get("name")
  metric_type = definition.get("type")
  help_text = definition.get("help")
  if not isinstance(name, str) or not METRIC_NAME_PATTERN.match(name):
    raise TelemetryConfigError('Invalid Prometheus metric name "{}"'.format(name))
  if metric_type not in SUPPORTED_TYPES:
    raise TelemetryConfigError('Unsupported Prometheus metric type "{}"'.format(metric_type))
  if metric_type == "counter" and not name.endswith("_total"):
    raise TelemetryConfigError('Counter metric "{}" must end with _total'.format(name))
  if not isinstance(help_text, str) or not help_text.strip():
    raise TelemetryConfigError('Metric "{}" must define help text'.format(name))
  if not isinstance(definition.get("unit", "unitless"), str):
    raise TelemetryConfigError('Metric "{}" unit must be a string'.format(name))
  scale = definition.get("scale", 1.0)
  if isinstance(scale, bool) or not isinstance(scale, (int, float)) or not math.isfinite(scale):
    raise TelemetryConfigError('Metric "{}" scale must be finite'.format(name))

  metadata = (metric_type, help_text)
  existing = families.get(name)
  if existing is not None and existing != metadata:
    raise TelemetryConfigError('Conflicting metric family "{}"'.format(name))
  families[name] = metadata


class TelemetryHttpClient:
  def __init__(self, opener=None, kerberos_fetcher=None):
    self.opener = opener or build_opener(_RejectRedirects())
    self.kerberos_fetcher = kerberos_fetcher

  def fetch(self, target):
    if target.get("auth", {}).get("type", "none") == "kerberos":
      if self.kerberos_fetcher is None:
        raise TelemetryScrapeError("Kerberos telemetry fetcher is not configured")
      return self.kerberos_fetcher(target)

    request = Request(
      target["url"],
      headers={"Accept": "text/plain, application/json", "User-Agent": "ambari-agent"},
    )
    timeout = target.get("timeoutSeconds", 5)
    max_bytes = target.get("maxResponseBytes", 32 * 1024 * 1024)
    try:
      with self.opener.open(request, timeout=timeout) as response:
        body = response.read(max_bytes + 1)
        if len(body) > max_bytes:
          raise TelemetryScrapeError("Telemetry response exceeded the configured size limit")
        content_type = response.headers.get("Content-Type", PROMETHEUS_CONTENT_TYPE)
        return TelemetryResponse(body, content_type)
    except TelemetryScrapeError:
      raise
    except HTTPError as err:
      raise TelemetryScrapeError("Telemetry upstream returned HTTP {}".format(err.code))
    except (URLError, OSError) as err:
      reason = getattr(err, "reason", err)
      if isinstance(reason, TimeoutError):
        raise TelemetryScrapeError("Telemetry upstream timed out", status=504)
      raise TelemetryScrapeError("Telemetry upstream request failed")


class KerberosTelemetryFetcher:
  def __init__(self, config):
    self.tmp_dir = config.get("agent", "tmp_dir")
    self.user = config.get("agent", "run_as_user", "root")

  def __call__(self, target):
    from resource_management.libraries.functions.curl_krb_request import (
      curl_krb_request,
    )

    auth = target["auth"]
    max_bytes = target.get("maxResponseBytes", 32 * 1024 * 1024)
    try:
      body, _, _ = curl_krb_request(
        self.tmp_dir,
        auth["keytab"],
        auth["principal"],
        target["url"],
        "telemetry",
        auth.get("kerberosExecutableSearchPaths"),
        False,
        target["id"],
        self.user,
        connection_timeout=target.get("timeoutSeconds", 5),
        ca_certs=auth.get("caFile"),
        follow_redirects=False,
        fail_on_http_error=True,
        max_response_bytes=max_bytes,
        verify_ssl=True,
      )
    except Exception as err:
      raise TelemetryScrapeError(
        "Kerberos telemetry upstream request failed"
      ) from err

    if isinstance(body, str):
      body = body.encode("utf-8")
    if not isinstance(body, bytes) or not body:
      raise TelemetryScrapeError(
        "Kerberos telemetry upstream returned an empty response"
      )
    if len(body) > max_bytes:
      raise TelemetryScrapeError(
        "Telemetry response exceeded the configured size limit"
      )
    return TelemetryResponse(body)


class TelemetryRouteRegistry:
  def __init__(self, config_cache, http_client=None, clock=None, wall_clock=None):
    self.name = "component_telemetry"
    self.config_cache = config_cache
    self.http_client = http_client or TelemetryHttpClient()
    self.clock = clock or time.monotonic
    self.wall_clock = wall_clock or time.time
    self._lock = threading.RLock()
    self._requests = defaultdict(int)
    self._last_duration = {}
    self._last_success = {}
    self._conversion_failures = defaultdict(int)
    self._route_semaphores = {}

  def scrape(self, route_id):
    snapshot = self.config_cache.snapshot()
    targets = {target["id"]: target for target in snapshot["assignment"]["targets"]}
    target = targets.get(route_id)
    if target is None:
      raise KeyError(route_id)

    started = self.clock()
    result = "success"
    semaphore = self._route_semaphore(
      route_id, target.get("maxConcurrentRequests", 2)
    )
    acquired = semaphore.acquire(blocking=False)
    if not acquired:
      with self._lock:
        self._requests[(route_id, "error")] += 1
        self._last_duration[route_id] = 0
      raise TelemetryScrapeError(
        "Telemetry route has reached its concurrent scrape limit", status=503
      )
    try:
      upstream = self.http_client.fetch(target)
      if target["format"] == "prometheus_text":
        validate_prometheus_text(upstream.body)
        response = TelemetryResponse(upstream.body, PROMETHEUS_CONTENT_TYPE)
      else:
        profile = snapshot["profiles"][target["profileHash"]]
        try:
          response = TelemetryResponse(convert_jmx_json(upstream.body, profile))
        except Exception:
          with self._lock:
            self._conversion_failures[route_id] += 1
          raise
      with self._lock:
        self._last_success[route_id] = self.wall_clock()
      return response
    except Exception:
      result = "error"
      raise
    finally:
      with self._lock:
        self._requests[(route_id, result)] += 1
        self._last_duration[route_id] = max(0, self.clock() - started)
      semaphore.release()

  def _route_semaphore(self, route_id, limit):
    with self._lock:
      configured_limit, semaphore = self._route_semaphores.get(
        route_id, (None, None)
      )
      if semaphore is None or configured_limit != limit:
        semaphore = threading.BoundedSemaphore(limit)
        self._route_semaphores[route_id] = (limit, semaphore)
      return semaphore

  def collect(self):
    configured = MetricFamily(
      "ambari_agent_telemetry_target_configured",
      "Whether a component telemetry target is configured.",
      "gauge",
    )
    requests = MetricFamily(
      "ambari_agent_telemetry_scrape_requests_total",
      "Component telemetry scrape requests by result.",
      "counter",
    )
    duration = MetricFamily(
      "ambari_agent_telemetry_last_scrape_duration_seconds",
      "Duration of the latest component telemetry scrape.",
      "gauge",
    )
    last_success = MetricFamily(
      "ambari_agent_telemetry_last_success_timestamp_seconds",
      "Unix timestamp of the latest successful component telemetry scrape.",
      "gauge",
    )
    conversion_failures = MetricFamily(
      "ambari_agent_telemetry_conversion_failures_total",
      "JMX profile conversion failures by component telemetry route.",
      "counter",
    )
    reload_success = MetricFamily(
      "ambari_agent_telemetry_config_last_reload_successful",
      "Whether the latest telemetry configuration reload succeeded.",
      "gauge",
    )

    snapshot = self.config_cache.snapshot()
    for target in snapshot["assignment"]["targets"]:
      configured.add_sample(1, {"route": target["id"]})
    reload_success.add_sample(1 if self.config_cache.last_reload_successful else 0)

    with self._lock:
      for (route_id, result), count in sorted(self._requests.items()):
        requests.add_sample(count, {"route": route_id, "result": result})
      for route_id, value in sorted(self._last_duration.items()):
        duration.add_sample(value, {"route": route_id})
      for route_id, value in sorted(self._last_success.items()):
        last_success.add_sample(value, {"route": route_id})
      for route_id, value in sorted(self._conversion_failures.items()):
        conversion_failures.add_sample(value, {"route": route_id})

    return [
      configured,
      requests,
      duration,
      last_success,
      conversion_failures,
      reload_success,
    ]


def convert_jmx_json(payload, profile):
  validate_profile(profile)
  try:
    document = json.loads(payload.decode("utf-8") if isinstance(payload, bytes) else payload)
  except (TypeError, ValueError, UnicodeDecodeError):
    raise TelemetryScrapeError("Telemetry upstream returned invalid JMX JSON")
  beans = document.get("beans") if isinstance(document, dict) else None
  if not isinstance(beans, list):
    raise TelemetryScrapeError("Telemetry JMX JSON does not contain a beans list")

  families = OrderedDict()
  seen_samples = defaultdict(set)
  series_count = 0
  for bean in beans:
    if not isinstance(bean, dict) or not isinstance(bean.get("name"), str):
      continue
    try:
      domain, properties = parse_object_name(bean["name"])
    except ValueError:
      continue
    for rule in profile["rules"]:
      if not _bean_matches(rule["bean"], domain, properties):
        continue
      labels = _rule_labels(rule.get("labels", {}), properties)
      for source_name, definition in rule["attributes"].items():
        value = _attribute_value(bean, source_name)
        if isinstance(value, bool) or not isinstance(value, (int, float)):
          continue
        scale = definition.get("scale", 1)
        if scale != 1:
          value *= scale
        metric_name = definition["name"]
        family = families.get(metric_name)
        if family is None:
          family = MetricFamily(metric_name, definition["help"], definition["type"])
          families[metric_name] = family
        sample_key = tuple(sorted(labels.items()))
        if sample_key in seen_samples[metric_name]:
          raise TelemetryScrapeError('Duplicate JMX sample for metric "{}"'.format(metric_name))
        seen_samples[metric_name].add(sample_key)
        family.add_sample(value, labels)
        series_count += 1
        if series_count > profile.get("maxSeries", 10000):
          raise TelemetryScrapeError("JMX telemetry exceeded the profile series limit")

  if not families:
    raise TelemetryScrapeError("No JMX metrics matched the telemetry profile")
  return render_metrics(families.values())


def parse_object_name(value):
  if not isinstance(value, str) or ":" not in value:
    raise ValueError("Invalid JMX ObjectName")
  domain, properties_text = value.split(":", 1)
  if not domain or not properties_text:
    raise ValueError("Invalid JMX ObjectName")
  properties = {}
  for field in _split_unquoted(properties_text, ","):
    key_value = _split_unquoted(field, "=", max_parts=2)
    if len(key_value) != 2 or not key_value[0]:
      raise ValueError("Invalid JMX ObjectName property")
    properties[key_value[0]] = _unquote_object_name_value(key_value[1])
  return domain, properties


def _split_unquoted(value, delimiter, max_parts=None):
  parts = []
  start = 0
  quoted = False
  escaped = False
  for index, char in enumerate(value):
    if escaped:
      escaped = False
      continue
    if char == "\\":
      escaped = True
      continue
    if char == '"':
      quoted = not quoted
      continue
    if char == delimiter and not quoted and (max_parts is None or len(parts) + 1 < max_parts):
      parts.append(value[start:index])
      start = index + 1
  if quoted or escaped:
    raise ValueError("Invalid quoted JMX ObjectName")
  parts.append(value[start:])
  return parts


def _unquote_object_name_value(value):
  if len(value) >= 2 and value[0] == '"' and value[-1] == '"':
    value = value[1:-1]
  return re.sub(r"\\(.)", r"\1", value)


def _bean_matches(expected, domain, properties):
  if domain != expected["domain"]:
    return False
  for name, value in expected["properties"].items():
    if name not in properties or (value != "*" and properties[name] != value):
      return False
  return True


def _rule_labels(label_definitions, properties):
  labels = {}
  for name, source in label_definitions.items():
    if "property" in source:
      property_name = source["property"]
      if property_name not in properties:
        raise TelemetryScrapeError(
          'JMX bean is missing label property "{}"'.format(property_name)
        )
      labels[name] = properties[property_name]
    else:
      labels[name] = source["value"]
  return labels


def _attribute_value(bean, source_name):
  value = bean
  for part in source_name.split("."):
    if not isinstance(value, dict) or part not in value:
      return None
    value = value[part]
  return value
