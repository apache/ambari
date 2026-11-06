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

from urllib.parse import urlsplit

from resource_management.libraries.functions.namenode_ha_utils import get_nameservices


def get_local_namenode(hdfs_site, hostname, public_hostname=None):
  """Resolve the local NN without renaming IDs or matching hostname substrings."""
  local_hosts = {h.lower().rstrip(".") for h in (hostname, public_hostname) if h}
  matches = []
  # Match the effective scope used by params_linux and shared HA state helpers.
  for ns in get_nameservices(hdfs_site):
    ids = hdfs_site.get("dfs.ha.namenodes." + ns, "")
    for nn_id in (value.strip() for value in ids.split(",") if value.strip()):
      address = hdfs_site.get("dfs.namenode.rpc-address." + ns + "." + nn_id)
      rpc_host = urlsplit("//" + address).hostname if address else None
      if rpc_host and rpc_host.lower().rstrip(".") in local_hosts:
        matches.append((ns, nn_id, address))
  if len(matches) > 1:
    raise ValueError("Local host maps to more than one NameNode identity")
  return matches[0] if matches else (None, None, None)
