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
import math
import os
import re
from pathlib import Path

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  require_external_ranger_credentials,
)


_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)
_VERSION_PATTERN = re.compile(
  r"[0-9]+(?:\.[0-9]+){1,3}(?:[-.][A-Za-z0-9]+)*", re.ASCII
)
_CONFIG_SEGMENT_PATTERN = re.compile(
  r"[A-Za-z0-9][A-Za-z0-9._-]{0,254}", re.ASCII
)
_HOSTNAME_PATTERN = re.compile(
  r"(?=.{1,253}\.?\Z)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)*"
  r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.?",
  re.ASCII,
)


def _normalize_network_host(value, name, bracket_ipv6=True):
  if not isinstance(value, str) or not value or value != value.strip():
    raise Fail(f"{name} must be a non-empty host without surrounding whitespace")
  if any(character.isspace() or ord(character) < 32 for character in value):
    raise Fail(f"{name} contains whitespace or control characters")
  if value.startswith("[") and value.endswith("]"):
    value = value[1:-1]
  elif "[" in value or "]" in value:
    raise Fail(f"{name} contains invalid IPv6 brackets")
  if ":" in value:
    if "%" in value:
      raise Fail(f"{name} must not use a scoped IPv6 host")
    try:
      parsed = ipaddress.IPv6Address(value)
    except ipaddress.AddressValueError as error:
      raise Fail(f"{name} contains an invalid IPv6 host") from error
    return f"[{parsed}]" if bracket_ipv6 else str(parsed)
  if re.fullmatch(r"[0-9.]+", value):
    try:
      return str(ipaddress.IPv4Address(value))
    except ipaddress.AddressValueError as error:
      raise Fail(f"{name} contains an invalid IPv4 host") from error
  if _HOSTNAME_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} contains an invalid hostname")
  return value.lower().rstrip(".")


def normalize_network_hosts(hosts, name, require_hosts=True):
  if not isinstance(hosts, (list, tuple)) or (require_hosts and not hosts):
    raise Fail(f"{name} must contain at least one host")
  normalized = tuple(
    _normalize_network_host(host, f"{name} entry") for host in hosts
  )
  if len(set(normalized)) != len(normalized):
    raise Fail(f"{name} must not contain duplicate hosts")
  return normalized


def normalize_ipv4_addresses(addresses, name, require_addresses=True):
  if not isinstance(addresses, (list, tuple)) or (
    require_addresses and not addresses
  ):
    raise Fail(f"{name} must contain at least one IPv4 address")
  normalized = []
  for address in addresses:
    if not isinstance(address, str) or address != address.strip():
      raise Fail(f"{name} entries must be IPv4 address strings")
    try:
      normalized.append(str(ipaddress.IPv4Address(address)))
    except ipaddress.AddressValueError as error:
      raise Fail(f"{name} contains an invalid IPv4 address") from error
  return tuple(normalized)


def validate_rack_paths(racks, name, require_racks=True):
  if not isinstance(racks, (list, tuple)) or (require_racks and not racks):
    raise Fail(f"{name} must contain at least one rack path")
  normalized = []
  for rack in racks:
    if (
      not isinstance(rack, str)
      or rack != rack.strip()
      or re.fullmatch(r"/[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*", rack) is None
    ):
      raise Fail(f"{name} contains an invalid rack path")
    normalized.append(rack)
  return tuple(normalized)


def parse_network_host_csv(value, name):
  if value is None or value == "":
    return ()
  if not isinstance(value, str):
    raise Fail(f"{name} must be a comma-separated host list")
  entries = tuple(entry.strip() for entry in value.split(","))
  if any(not entry for entry in entries):
    raise Fail(f"{name} must not contain empty host entries")
  return normalize_network_hosts(entries, name)


def build_topology_mappings(all_hosts, all_ipv4_addresses, racks, service_hosts):
  if len(all_hosts) != len(all_ipv4_addresses) or len(all_hosts) != len(racks):
    raise Fail("Cluster topology host, IPv4, and rack arrays must have equal lengths")
  unknown_hosts = set(service_hosts) - set(all_hosts)
  if unknown_hosts:
    raise Fail(
      "Cluster topology is missing service hosts: "
      + ", ".join(sorted(unknown_hosts))
    )
  service_host_set = set(service_hosts)
  return tuple(
    (host, address, rack)
    for host, address, rack in zip(all_hosts, all_ipv4_addresses, racks)
    if host in service_host_set
  )


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("YARN scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def require_bigtop_component_version(version, feature_name):
  if version is None:
    raise Fail(f"{feature_name} requires a resolved BIGTOP component version")
  return validate_bigtop_stack("BIGTOP", version)


def validate_absolute_path(path, name):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
    or path == os.sep
    or _ABSOLUTE_PATH_PATTERN.fullmatch(path) is None
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return path


def validate_runtime_directory_prefix(path, name):
  normalized = validate_absolute_path(path, name)
  allowed_roots = ("/run", "/var/run")
  if not any(
    normalized == root or os.path.commonpath((normalized, root)) == root
    for root in allowed_roots
  ):
    raise Fail(f"{name} must be within /run or /var/run")
  return normalized


def parse_rm_ha_ids(value):
  if value is None or (isinstance(value, str) and not value.strip()):
    return ()
  if not isinstance(value, str):
    raise Fail("yarn.resourcemanager.ha.rm-ids must be a comma-separated string")
  rm_ids = tuple(part.strip() for part in value.split(","))
  if any(_CONFIG_SEGMENT_PATTERN.fullmatch(rm_id) is None for rm_id in rm_ids):
    raise Fail("yarn.resourcemanager.ha.rm-ids contains an invalid or empty RM ID")
  if len(set(rm_ids)) != len(rm_ids):
    raise Fail("yarn.resourcemanager.ha.rm-ids must not contain duplicate RM IDs")
  return rm_ids


def validate_rm_ha_ids(enabled, value):
  if not enabled:
    return ()
  rm_ids = parse_rm_ha_ids(value)
  if len(rm_ids) < 2:
    raise Fail(
      "yarn.resourcemanager.ha.enabled requires at least two "
      "yarn.resourcemanager.ha.rm-ids"
    )
  return rm_ids


def yarn_artifact_paths(stack_version, rm_ha_id=None):
  if (
    not isinstance(stack_version, str)
    or _CONFIG_SEGMENT_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("A safe stack version is required for YARN artifact paths")
  service_path = f"/bigtop/apps/{stack_version}/yarn"
  hbase_path = f"/bigtop/apps/{stack_version}/hbase"
  if rm_ha_id is not None:
    if (
      not isinstance(rm_ha_id, str)
      or _CONFIG_SEGMENT_PATTERN.fullmatch(rm_ha_id) is None
    ):
      raise Fail("A safe ResourceManager HA ID is required")
    hbase_path = f"{hbase_path}/{rm_ha_id}"
  return service_path, hbase_path


def resolve_local_rm_ha_id(rm_hostnames, local_hostname, require_match=True):
  def canonical_hostname(value):
    return _normalize_network_host(
      value, "ResourceManager hostname", bracket_ipv6=False
    )

  def hostnames_match(left, right):
    if left == right:
      return True
    if ":" in left or ":" in right:
      return False
    return left.split(".", 1)[0] == right.split(".", 1)[0] and (
      "." not in left or "." not in right
    )

  if not isinstance(local_hostname, str):
    raise Fail("ResourceManager hostname must be a non-empty host")
  local_hostname = canonical_hostname(local_hostname.strip())
  matches = [
    rm_id
    for rm_id, rm_hostname in rm_hostnames.items()
    if hostnames_match(canonical_hostname(str(rm_hostname).strip()), local_hostname)
  ]
  if len(matches) > 1:
    raise Fail(
      "At most one ResourceManager HA ID may match the local hostname; "
      f"matched {matches}"
    )
  if require_match and not matches:
    raise Fail("A ResourceManager HA ID must match the local hostname")
  return matches[0] if matches else None


def select_rm_webapp_address(addresses, local_rm_id=None):
  if not isinstance(addresses, dict) or not addresses:
    raise Fail("At least one ResourceManager web address is required")
  normalized = {}
  for rm_id, address in addresses.items():
    parse_address_port(
      address, f"ResourceManager {rm_id or 'default'} web address"
    )
    normalized[rm_id] = address.strip()
  selected_id = local_rm_id if local_rm_id in normalized else next(iter(normalized))
  return normalized[selected_id]


def parse_boolean(value):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise ValueError(f"Expected a boolean value, got {value!r}")


def parse_yes_no(value, name):
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "yes":
      return True
    if normalized == "no":
      return False
  raise Fail(f"{name} must be Yes or No")


def parse_port(value, name):
  port_text = str(value).strip()
  if isinstance(value, bool) or re.fullmatch(r"[0-9]+", port_text) is None:
    raise Fail(f"{name} must be an integer from 1 through 65535")
  port = int(port_text)
  if port < 1 or port > 65535:
    raise Fail(f"{name} must be an integer from 1 through 65535")
  return port


def parse_positive_int(value, name):
  value_text = str(value).strip()
  if isinstance(value, bool) or re.fullmatch(r"[0-9]+", value_text) is None:
    raise Fail(f"{name} must be a positive integer")
  parsed = int(value_text)
  if parsed < 1:
    raise Fail(f"{name} must be a positive integer")
  return parsed


def parse_nonnegative_int(value, name):
  value_text = str(value).strip()
  if isinstance(value, bool) or re.fullmatch(r"[0-9]+", value_text) is None:
    raise Fail(f"{name} must be a non-negative integer")
  return int(value_text)


def validate_single_line_value(value, name, allow_empty=True):
  if not isinstance(value, str) or (not allow_empty and not value):
    requirement = "a string or empty" if allow_empty else "a non-empty string"
    raise Fail(f"{name} must be {requirement}")
  if any(ord(character) < 32 or ord(character) == 127 for character in value):
    raise Fail(f"{name} must not contain control characters")
  return value


def parse_docker_capabilities(value, name):
  if not isinstance(value, str):
    raise Fail(f"{name} must be a comma-separated string")
  if not value.strip():
    return ""
  capabilities = tuple(item.strip() for item in value.split(","))
  if any(
    not capability
    or re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", capability) is None
    for capability in capabilities
  ):
    raise Fail(f"{name} contains an invalid Linux capability")
  return ",".join(capabilities)


def validate_config_segment(value, name):
  value = validate_single_line_value(value, name, allow_empty=False)
  if _CONFIG_SEGMENT_PATTERN.fullmatch(value) is None:
    raise Fail(
      f"{name} must be a single filesystem-safe configuration segment"
    )
  return value


def validate_jar_file_name(value, name):
  value = validate_config_segment(value, name)
  if not value.lower().endswith(".jar"):
    raise Fail(f"{name} must be a JAR file name")
  return value


def escape_java_quoted_string(value, name):
  value = validate_single_line_value(value, name, allow_empty=False)
  return value.replace("\\", "\\\\").replace('"', '\\"')


def escape_java_properties_value(value, name):
  value = validate_single_line_value(value, name)
  escaped = []
  for character in value:
    if character in "\\:=#! ":
      escaped.append("\\")
    escaped.append(character)
  return "".join(escaped)


def local_file_uri(path, name):
  path = validate_single_line_value(path, name, allow_empty=False)
  if (
    not os.path.isabs(path)
    or path.startswith("//")
    or path == os.sep
    or os.path.normpath(path) != path
  ):
    raise Fail(f"{name} must be a normalized absolute local file path")
  return Path(path).as_uri()


def validate_unix_name(value, name):
  value = validate_single_line_value(value, name, allow_empty=False)
  if re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.-]*", value) is None:
    raise Fail(f"{name} must be a valid user or group name")
  return value


def parse_fraction(value, name):
  if isinstance(value, bool):
    raise Fail(f"{name} must be greater than 0 and no greater than 1")
  value_text = str(value).strip()
  if re.fullmatch(r"(?:0|[1-9][0-9]*)(?:\.[0-9]+)?", value_text) is None:
    raise Fail(f"{name} must be greater than 0 and no greater than 1")
  parsed = float(value_text)
  if not math.isfinite(parsed) or parsed <= 0 or parsed > 1:
    raise Fail(f"{name} must be greater than 0 and no greater than 1")
  return parsed


def parse_address_port(value, name):
  if not isinstance(value, str) or not value.strip():
    raise Fail(f"{name} must be a host and port")
  address = value.strip()
  if any(character.isspace() or ord(character) < 32 for character in address):
    raise Fail(f"{name} must not contain whitespace")
  if address.startswith("["):
    closing_bracket = address.find("]")
    if closing_bracket < 2 or address[closing_bracket + 1 : closing_bracket + 2] != ":":
      raise Fail(f"{name} must be a bracketed IPv6 host and port")
    if ":" not in address[1:closing_bracket]:
      raise Fail(f"{name} must be a bracketed IPv6 host and port")
    _normalize_network_host(
      address[: closing_bracket + 1], name, bracket_ipv6=False
    )
    port = address[closing_bracket + 2 :]
  else:
    host, separator, port = address.rpartition(":")
    if not separator or not host or ":" in host:
      raise Fail(f"{name} must be a host and port; IPv6 hosts must be bracketed")
    _normalize_network_host(host, name, bracket_ipv6=False)
  return parse_port(port, f"{name} port")


def format_zookeeper_quorum(hosts, name):
  return ",".join(normalize_network_hosts(hosts, name))


def timeline_service_v2_enabled(enabled, version, versions):
  if not enabled:
    return False
  selected = versions if isinstance(versions, str) and versions.strip() else version
  if not isinstance(selected, str) or not selected.strip():
    raise Fail("A YARN timeline service version is required when it is enabled")
  normalized_versions = []
  for raw_version in selected.split(","):
    normalized = raw_version.strip().lower()
    if normalized.endswith("f"):
      normalized = normalized[:-1]
    if normalized not in ("1.5", "2.0"):
      raise Fail(f"Unsupported YARN timeline service version: {raw_version!r}")
    normalized_versions.append(normalized)
  return "2.0" in normalized_versions


def validate_hbase_backend_mode(
  hbase_within_cluster,
  is_hbase_system_service_launch,
  use_external_hbase,
  hbase_master_hosts,
  has_hbase_site,
):
  if is_hbase_system_service_launch and (use_external_hbase or hbase_within_cluster):
    raise Fail(
      "YARN embedded HBase system-service mode cannot be combined with external HBase"
    )
  if hbase_within_cluster and not use_external_hbase:
    raise Fail("hbase_within_cluster requires use_external_hbase=true")
  if hbase_within_cluster and not hbase_master_hosts:
    raise Fail("hbase_within_cluster requires at least one managed HBase master")
  if hbase_within_cluster and not has_hbase_site:
    raise Fail("hbase_within_cluster requires the managed hbase-site configuration")


def calc_heap_memory(memorysize, heapmemory_factor):
  """
  @param memorysize_str: str (e.g '4096m')
  @param heapmemory_factor: float (e.g 0.8)
  """
  return int(math.floor(memorysize * heapmemory_factor))


def ensure_unit_for_memory(memory_size):
  match = re.fullmatch(r"([1-9][0-9]*)([bkmgtp]?)", str(memory_size).strip().lower())
  if match is None:
    raise ValueError(f"Invalid positive memory size: {memory_size!r}")
  value, unit = match.groups()
  return f"{value}{unit or 'm'}"
