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

import ipaddress
import re

from resource_management.core.exceptions import Fail


ROUTER_NAMESERVICE = "ns-fed"
HOSTNAME_PATTERN = re.compile(r"[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?")


def format_router_host(host):
  if not isinstance(host, str) or host != host.strip() or not host:
    raise Fail(f"Invalid HDFS Router host: {host!r}")

  try:
    address = ipaddress.ip_address(host)
  except ValueError:
    if HOSTNAME_PATTERN.fullmatch(host) is None:
      raise Fail(f"Invalid HDFS Router host: {host!r}")
    return host

  return f"[{address}]" if address.version == 6 else str(address)


def build_router_client_sites(hdfs_site, core_site, router_hosts, rpc_port):
  if not isinstance(hdfs_site, dict) or not isinstance(core_site, dict):
    raise Fail("HDFS Router requires hdfs-site and core-site configurations")
  if not isinstance(router_hosts, (list, tuple)) or not router_hosts:
    raise Fail("HDFS Router requires at least one Router host")
  if isinstance(rpc_port, bool) or not isinstance(rpc_port, int):
    raise Fail(f"Invalid HDFS Router RPC port: {rpc_port!r}")
  if not 0 < rpc_port <= 65535:
    raise Fail(f"Invalid HDFS Router RPC port: {rpc_port!r}")

  configured_nameservices = hdfs_site.get("dfs.nameservices")
  if not isinstance(configured_nameservices, str):
    raise Fail("HDFS Router requires a non-empty dfs.nameservices property")
  nameservices = [
    nameservice.strip()
    for nameservice in configured_nameservices.split(",")
    if nameservice.strip()
  ]
  if not nameservices:
    raise Fail("HDFS Router requires a non-empty dfs.nameservices property")
  if ROUTER_NAMESERVICE not in nameservices:
    nameservices.append(ROUTER_NAMESERVICE)

  zk_quorum = core_site.get("ha.zookeeper.quorum")
  if not isinstance(zk_quorum, str) or not zk_quorum.strip():
    raise Fail("HDFS Router requires core-site/ha.zookeeper.quorum")

  router_hdfs_site = dict(hdfs_site)
  router_core_site = dict(core_site)
  router_hdfs_site["dfs.nameservices"] = ",".join(nameservices)
  router_hdfs_site[f"dfs.client.failover.proxy.provider.{ROUTER_NAMESERVICE}"] = (
    "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"
  )
  router_hdfs_site["dfs.client.failover.random.order"] = "true"

  router_ids = [f"r{index}" for index in range(1, len(router_hosts) + 1)]
  router_hdfs_site[f"dfs.ha.namenodes.{ROUTER_NAMESERVICE}"] = ",".join(
    router_ids
  )
  for router_id, router_host in zip(router_ids, router_hosts):
    property_name = (
      f"dfs.namenode.rpc-address.{ROUTER_NAMESERVICE}.{router_id}"
    )
    router_hdfs_site[property_name] = (
      f"{format_router_host(router_host)}:{rpc_port}"
    )

  router_core_site["fs.defaultFS"] = f"hdfs://{ROUTER_NAMESERVICE}"
  router_core_site["hadoop.zk.address"] = zk_quorum.strip()
  return router_hdfs_site, router_core_site
