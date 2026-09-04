<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Ambari Agent Prometheus Telemetry Plan

## Status

Approved for implementation. This document records the design agreed for the
initial community contribution. Implementation status is tracked in the
milestones at the end of this document.

## Goals

* Expose Linux host metrics from Ambari Agent without installing Categraf or a
  separate node exporter.
* Expose metrics for NameNode, DataNode, ResourceManager, NodeManager, HBase
  Master, HBase RegionServer, and HiveServer2 through independent Prometheus
  scrape targets.
* Prefer a component's native Prometheus HTTP endpoint and use HTTP JMX JSON
  conversion only when a native endpoint is unavailable or unsuitable.
* Resolve component topology, URLs, protocols, and authentication on Ambari
  Server. Keep the Agent independent of Stack configuration rules.
* Keep the last valid telemetry configuration available while Ambari Server is
  temporarily unavailable.
* Preserve the existing `metrics.json` contract and existing Ambari metric
  property providers.

## Non-goals

* The Agent will not push component samples in heartbeat messages.
* The Agent will not cache component metric values.
* The Agent will not accept an arbitrary upstream URL from a scrape request.
* The first version will not synthesize Prometheus histograms from JMX summary
  attributes.
* Existing Categraf `hadoop_*` and `hbase_*` metric names are not a compatibility
  contract.
* Grafana dashboard provisioning or migration is outside this contribution.

## Confirmed Decisions

1. Host and component metrics use independent HTTP paths.
2. Stack `telemetry.json` and focused JMX profiles define the new telemetry
   behavior. `metrics.json` will not be extended for Prometheus.
3. Configuration is cached; metric values are always collected for the current
   Prometheus scrape.
4. The first component set is NameNode, DataNode, ResourceManager, NodeManager,
   HBase Master, HBase RegionServer, and HiveServer2.
5. JMX fallback metrics do not preserve old Categraf metric names. Dashboard
   provisioning and migration are handled outside this contribution.

## Component Capability Matrix

| Component | Stack version | HTTP source | Agent handling |
| --- | --- | --- | --- |
| NameNode | Hadoop 3.3.4 | `/prom` | Prometheus text pass-through |
| DataNode | Hadoop 3.3.4 | `/prom` | Prometheus text pass-through |
| ResourceManager | Hadoop 3.3.4 | `/prom` | Prometheus text pass-through |
| NodeManager | Hadoop 3.3.4 | `/jmx` | JMX JSON conversion |
| HBase Master | HBase 2.4.13 | `/jmx` | JMX JSON conversion |
| HBase RegionServer | HBase 2.4.13 | `/jmx` | JMX JSON conversion |
| HiveServer2 | Hive 3.1.3 | `/jmx` on the Web UI | JMX JSON conversion |

Hadoop enables `/prom` with `hadoop.prometheus.endpoint.enabled=true`. On the
verified Hadoop 3.3.4 deployment, the NodeManager Timeline Collector registers
the process-global `prometheus` sink first. Registration by the fixed 8042 Web
UI then fails with `Sink prometheus already exists!`, leaving `8042/prom` as an
empty HTTP 200 response while a random Timeline Collector port owns the live
sink. The random port is unsuitable for assignment, so NodeManager uses the
stable `8042/jmx` endpoint. HBase 2.6 and later expose `/prometheus`, but this
source is selected only after its output has passed compatibility tests.
HiveServer2 3.1.3 uses Web UI port 10002 by default; hard-coded Categraf ports
are not reused.

## Runtime Architecture

Prometheus obtains Agent scrape targets from Ambari Server HTTP service
discovery. Host metrics use `/metrics`. Each installed component uses a stable,
opaque route such as `/metrics/components/{routeId}`. Target labels supplied by
service discovery identify the cluster, service, component, and host.

The Agent handles two upstream formats:

* `prometheus_text` validates the upstream response and passes the body through
  without renaming metrics or changing labels.
* `jmx_json` parses the Hadoop-style JMX response and applies a typed profile to
  render Prometheus text.

Separate component paths prevent common JVM and RPC metric families from
colliding. They also ensure an unavailable component marks only its own target
down.

## Stack Descriptor

Each participating service may provide `telemetry.json`. The descriptor states
how Ambari Server resolves an endpoint and which response format it produces.
It is not a second inventory of native Prometheus metrics.

```json
{
  "schemaVersion": 1,
  "components": {
    "NAMENODE": {
      "format": "prometheus_text",
      "path": "/prom"
    },
    "HBASE_REGIONSERVER": {
      "format": "jmx_json",
      "path": "/jmx",
      "profile": "hbase-regionserver-2.4"
    }
  }
}
```

Ambari Server resolves the endpoint using the active Stack, cluster topology,
component state, and configuration. It selects HTTP or HTTPS and resolves
Kerberos settings before producing an Agent assignment.

## JMX Profiles

Only components without an accepted native Prometheus endpoint need a JMX
profile. Profiles match structured ObjectName fields and explicit attributes.
They never expose the complete bean name as a metric label.

```json
{
  "schemaVersion": 1,
  "id": "hbase-regionserver-2.4",
  "rules": [{
    "bean": {
      "domain": "Hadoop",
      "properties": {
        "service": "HBase",
        "name": "RegionServer",
        "sub": "Server"
      }
    },
    "attributes": {
      "regionCount": {
        "name": "hbase_regionserver_regions",
        "type": "gauge",
        "unit": "regions",
        "help": "Number of online regions."
      }
    }
  }]
}
```

Profile validation enforces Prometheus names, required help text, counter
suffixes, finite scale factors, explicit labels, and consistent family types.
Time values are converted to seconds and byte values use a `_bytes` suffix.
Dynamic bean or attribute captures require an explicit bounded label policy.

## Server-to-Agent Protocol

The Server compiles a host-specific assignment. The assignment contains no
passwords, keytab bytes, or administrator credentials.

```json
{
  "schemaVersion": 1,
  "hash": "sha256:assignment",
  "targets": [{
    "id": "cluster1-hbase-regionserver",
    "format": "jmx_json",
    "url": "https://host1:16030/jmx",
    "profileHash": "sha256:profile",
    "timeoutSeconds": 5,
    "maxResponseBytes": 33554432
  }]
}
```

Assignments are host-specific and profiles are content-addressed by SHA-256.
The Agent activates an assignment only after all referenced profiles pass
schema and digest validation. Cache files are written through a temporary file
and an atomic rename. An invalid update leaves the previous assignment active.

A dedicated hash-aware STOMP request and update topic follows the existing
topology, configuration, and alert-definition cache pattern. Relevant topology,
configuration, security, Stack, or component-state changes invalidate the
Server-side assignment. Registration performs an immediate hash comparison and
the Agent performs a low-frequency reconciliation every five minutes.

## HTTP Collection

The Agent fetches the upstream endpoint during each component scrape. Request
timeouts remain below the Prometheus scrape timeout. Response size, redirect
behavior, scheme, and target host are constrained by the assignment. Concurrent
requests to the same route are bounded.

HTTPS and SPNEGO use existing Ambari Agent request helpers, including keytab,
principal, ccache, and `curl --negotiate` support. Assignments may contain a
principal and a local keytab path but never secret file contents.

An upstream HTTP, authentication, size, or JSON failure returns a non-2xx
response so Prometheus records `up=0`. A missing optional JMX attribute omits
that sample without failing unrelated rules. The Agent exports its own request,
duration, profile-error, last-success, and configuration-reload metrics from
the host `/metrics` endpoint.

## Prometheus Service Discovery

Ambari Server provides a read-only HTTP service discovery endpoint:

```text
GET /api/v1/clusters/{cluster}/prometheus_targets
```

The response contains the Agent address, `__metrics_path__`, and target labels.
It is cached by topology/configuration revision and supports an ETag. Prometheus
uses a read-only Ambari service account. Agents do not use this REST endpoint
and do not store Ambari administrator credentials.

## Compatibility and Migration

Existing `metrics.json` files and the `metrics_descriptor` Stack Artifact API
remain unchanged. Migration tools may read legacy definitions to seed a JMX
profile, but runtime code does not join or extend legacy metric entries.

Native component metric names are retained. JMX fallback metrics use stable,
component-owned names such as `hbase_regionserver_regions` and
`hive_server2_open_sessions`. Categraf-compatible aliases are not emitted.
Dashboard and recording-rule consumers must query this emitted metric
inventory directly; they are not part of this implementation.

## Test Strategy

* Agent unit tests cover manifest/profile validation, atomic recovery, path
  routing, native pass-through, JMX conversion, limits, timeouts, and failures.
* Server unit tests cover descriptor parsing, endpoint resolution, assignment
  hashes, host filtering, event invalidation, and service discovery labels.
* Fixture tests use representative Hadoop, HBase, and Hive responses.
* Rendered output is checked with Prometheus tooling when available.
* Existing Stack Artifact and `metrics.json` tests must remain unchanged and
  pass.
* Integration tests verify that one failed component does not affect host or
  sibling component targets.

## Reviewable Implementation Milestones

- [x] Record the approved architecture and scope.
- [x] Add Agent telemetry assignment/profile cache and validators.
- [x] Add independent component routes and the two HTTP source adapters.
- [x] Add Server descriptor parsing and host assignment compilation.
- [x] Add hash-aware STOMP delivery and recovery behavior.
- [x] Add Stack descriptors and focused YARN/HBase/Hive profiles.
- [x] Add Prometheus HTTP service discovery.
- [x] Run focused tests, static format checks, and compatibility checks without
  invoking the Java build.

The implementation must use multiple topic commits because it crosses Agent,
Server, Stack, and dashboard boundaries. Tests remain in the same commit as the
behavior they verify.
