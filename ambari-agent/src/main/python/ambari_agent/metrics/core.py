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

import logging
import math
import re
import time
from collections import OrderedDict


logger = logging.getLogger(__name__)

METRIC_NAME_PATTERN = re.compile(r"^[a-zA-Z_:][a-zA-Z0-9_:]*$")
LABEL_NAME_PATTERN = re.compile(r"^[a-zA-Z_][a-zA-Z0-9_]*$")
METRIC_TYPES = frozenset(("counter", "gauge", "histogram", "summary", "untyped"))


class Sample:
  def __init__(self, value, labels=None):
    self.value = value
    self.labels = dict(labels or {})


class MetricFamily:
  def __init__(self, name, help_text, metric_type, samples=None):
    if not METRIC_NAME_PATTERN.match(name):
      raise ValueError(f'Invalid Prometheus metric name "{name}"')
    if metric_type not in METRIC_TYPES:
      raise ValueError(f'Invalid Prometheus metric type "{metric_type}"')

    self.name = name
    self.help_text = help_text
    self.metric_type = metric_type
    self.samples = samples or []

  def add_sample(self, value, labels=None):
    labels = labels or {}
    for label_name in labels:
      if not LABEL_NAME_PATTERN.match(label_name):
        raise ValueError(f'Invalid Prometheus label name "{label_name}"')
    self.samples.append(Sample(value, labels))


class CollectorRegistry:
  def __init__(self, collectors=None, clock=None):
    self.collectors = list(collectors or [])
    self.clock = clock or time.monotonic

  def register(self, collector):
    self.collectors.append(collector)

  def collect(self):
    families = []
    collector_health = MetricFamily(
      "ambari_agent_metrics_collector_up",
      "Whether the Ambari Agent metric collector completed successfully.",
      "gauge",
    )
    collector_duration = MetricFamily(
      "ambari_agent_metrics_collector_duration_seconds",
      "Time spent collecting one Ambari Agent metric collector.",
      "gauge",
    )

    for collector in self.collectors:
      collector_name = getattr(collector, "name", collector.__class__.__name__)
      start = self.clock()
      up = 1
      try:
        families.extend(collector.collect())
      except Exception:
        up = 0
        logger.exception('Metric collector "%s" failed', collector_name)
      finally:
        collector_health.add_sample(up, {"collector": collector_name})
        collector_duration.add_sample(
          max(0, self.clock() - start), {"collector": collector_name}
        )

    families.extend((collector_health, collector_duration))
    return families

  def render(self):
    return render_metrics(self.collect())


def render_metrics(families):
  merged_families = OrderedDict()
  for family in families:
    existing = merged_families.get(family.name)
    if existing is None:
      merged_families[family.name] = MetricFamily(
        family.name,
        family.help_text,
        family.metric_type,
        list(family.samples),
      )
      continue
    if (
      existing.help_text != family.help_text
      or existing.metric_type != family.metric_type
    ):
      raise ValueError(f'Conflicting Prometheus metric family "{family.name}"')
    existing.samples.extend(family.samples)

  output = []
  for family in merged_families.values():
    output.append(f"# HELP {family.name} {_escape_help(family.help_text)}")
    output.append(f"# TYPE {family.name} {family.metric_type}")
    for sample in family.samples:
      output.append(
        f"{family.name}{_format_labels(sample.labels)} {_format_value(sample.value)}"
      )

  return ("\n".join(output) + "\n").encode("utf-8")


def _escape_help(value):
  return str(value).replace("\\", "\\\\").replace("\n", "\\n")


def _escape_label_value(value):
  return str(value).replace("\\", "\\\\").replace("\n", "\\n").replace('"', '\\"')


def _format_labels(labels):
  if not labels:
    return ""

  values = [f'{name}="{_escape_label_value(labels[name])}"' for name in sorted(labels)]
  return "{" + ",".join(values) + "}"


def _format_value(value):
  if isinstance(value, bool):
    return "1" if value else "0"
  if isinstance(value, int):
    return str(value)

  number = float(value)
  if math.isnan(number):
    return "NaN"
  if math.isinf(number):
    return "+Inf" if number > 0 else "-Inf"
  return repr(number)
