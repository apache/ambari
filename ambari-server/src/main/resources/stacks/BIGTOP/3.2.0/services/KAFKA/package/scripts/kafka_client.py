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


_LISTENER_PATTERN = re.compile(
  r"^(?P<name>[A-Za-z][A-Za-z0-9_-]*)://"
  r"(?:(?:\[(?P<ipv6>[^]]+)\])|(?P<host>[^:]*)):(?P<port>[0-9]+)$",
  re.ASCII,
)
_SECURITY_PROTOCOLS = {"PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL"}
_HOST_PATTERN = re.compile(r"[A-Za-z0-9](?:[A-Za-z0-9_.-]*[A-Za-z0-9])?", re.ASCII)


def _normalize_protocol(value):
  return value.strip().upper().replace("PLAINTEXTSASL", "SASL_PLAINTEXT")


def _parse_listener(value, description="listener"):
  match = _LISTENER_PATTERN.fullmatch(value)
  if match is None:
    raise Fail(f"Kafka {description} {value!r} is invalid")
  ipv6 = match.group("ipv6")
  host = match.group("host")
  if ipv6 is not None:
    try:
      ipaddress.IPv6Address(ipv6)
    except ValueError as error:
      raise Fail(f"Kafka {description} {value!r} has an invalid IPv6 host") from error
  elif host and _HOST_PATTERN.fullmatch(host) is None:
    raise Fail(f"Kafka {description} {value!r} has an invalid host")
  port = int(match.group("port"))
  if port < 1 or port > 65535:
    raise Fail(f"Kafka {description} {value!r} has an invalid port")
  return match


def _listener_protocol_map(value):
  result = {}
  for item in (value or "").split(","):
    if not item.strip():
      continue
    name, separator, protocol = item.partition(":")
    if not separator or not name.strip() or not protocol.strip():
      raise Fail("listener.security.protocol.map contains an invalid entry")
    protocol = _normalize_protocol(protocol)
    if protocol not in _SECURITY_PROTOCOLS:
      raise Fail(f"Unsupported Kafka listener security protocol {protocol}")
    name = name.strip().upper()
    if name in result:
      raise Fail(f"Kafka listener protocol map contains duplicate name {name}")
    result[name] = protocol
  return result


def sasl_listeners(value):
  updated = []
  listener_names = set()
  for listener in (value or "").split(","):
    listener = listener.strip()
    _parse_listener(listener)
    name, separator, endpoint = listener.partition("://")
    protocol = _normalize_protocol(name)
    if protocol == "PLAINTEXT":
      protocol = "SASL_PLAINTEXT"
    elif protocol == "SSL":
      protocol = "SASL_SSL"
    if protocol in listener_names:
      raise Fail(f"Kafka listener {protocol} is configured more than once")
    listener_names.add(protocol)
    updated.append(f"{protocol}{separator}{endpoint}")
  return ",".join(updated)


def sasl_listener_protocol_map(value):
  if not value:
    return value
  mappings = []
  listener_names = set()
  for item in value.split(","):
    name, separator, protocol = item.partition(":")
    if not separator or not name.strip() or not protocol.strip():
      raise Fail("listener.security.protocol.map contains an invalid entry")
    protocol = _normalize_protocol(protocol)
    if protocol == "PLAINTEXT":
      protocol = "SASL_PLAINTEXT"
    elif protocol == "SSL":
      protocol = "SASL_SSL"
    if protocol not in _SECURITY_PROTOCOLS:
      raise Fail(f"Unsupported Kafka listener security protocol {protocol}")
    name = name.strip().upper()
    if name in listener_names:
      raise Fail(f"Kafka listener protocol map contains duplicate name {name}")
    listener_names.add(name)
    mappings.append(f"{name}:{protocol}")
  return ",".join(mappings)


def merge_advertised_listeners(listeners, advertised_listeners):
  managed = []
  positions = {}
  for listener in listeners.split(","):
    listener = listener.strip()
    match = _parse_listener(listener)
    name = match.group("name").upper()
    if name in positions:
      raise Fail(f"Kafka listener {name} is configured more than once")
    positions[name] = len(managed)
    managed.append(listener)

  advertised_names = set()
  for listener in advertised_listeners.split(","):
    listener = listener.strip()
    match = _parse_listener(listener, "advertised listener")
    name = match.group("name").upper()
    if name in advertised_names:
      raise Fail(f"Kafka advertised listener {name} is configured more than once")
    advertised_names.add(name)
    if name not in positions:
      raise Fail(
        f"Kafka advertised listener {name} has no matching listener"
      )
    managed[positions[name]] = listener
  return ",".join(managed)


def select_listener(server_properties):
  listener_value = server_properties.get("listeners") or server_properties.get(
    "advertised.listeners"
  )
  if not listener_value:
    raise Fail("Kafka listeners are not configured")

  protocol_map = _listener_protocol_map(
    server_properties.get("listener.security.protocol.map")
  )
  listeners = []
  listener_names = set()
  for value in listener_value.split(","):
    value = value.strip()
    match = _parse_listener(value)
    port = int(match.group("port"))
    name = match.group("name").upper()
    if name in listener_names:
      raise Fail(f"Kafka listener {name} is configured more than once")
    listener_names.add(name)
    protocol = _normalize_protocol(protocol_map.get(name, name))
    if protocol not in _SECURITY_PROTOCOLS:
      raise Fail(
        f"Kafka listener {name} requires listener.security.protocol.map"
      )
    listeners.append((name, protocol, port))

  requested_name = server_properties.get("inter.broker.listener.name")
  requested_protocol = server_properties.get("security.inter.broker.protocol")
  if requested_name:
    requested_name = requested_name.upper()
    for listener in listeners:
      if listener[0] == requested_name:
        return listener
    raise Fail(f"Inter-broker listener {requested_name} is not configured")
  if requested_protocol:
    requested_protocol = _normalize_protocol(requested_protocol)
    if requested_protocol not in _SECURITY_PROTOCOLS:
      raise Fail(
        f"Unsupported inter-broker security protocol {requested_protocol}"
      )
    for listener in listeners:
      if listener[1] == requested_protocol:
        return listener
    raise Fail(
      f"Inter-broker security protocol {requested_protocol} has no matching listener"
    )
  return listeners[0]


def inter_broker_protocol(server_properties):
  if server_properties.get("inter.broker.listener.name"):
    return select_listener(server_properties)[1]
  protocol = _normalize_protocol(
    server_properties.get("security.inter.broker.protocol", "PLAINTEXT")
  )
  if protocol not in _SECURITY_PROTOCOLS:
    raise Fail(f"Unsupported inter-broker security protocol {protocol}")
  return protocol


def bootstrap_servers(server_properties, broker_hosts):
  if not broker_hosts:
    raise Fail("No Kafka broker hosts are configured")
  _, _, port = select_listener(server_properties)
  endpoints = []
  for host in broker_hosts:
    host = host.strip()
    if not host:
      raise Fail("Kafka broker host is empty")
    if ":" in host:
      try:
        ipaddress.IPv6Address(host)
      except ValueError as error:
        raise Fail(f"Kafka broker host {host!r} is invalid") from error
      endpoints.append(f"[{host}]:{port}")
    else:
      if _HOST_PATTERN.fullmatch(host) is None:
        raise Fail(f"Kafka broker host {host!r} is invalid")
      endpoints.append(f"{host}:{port}")
  return ",".join(endpoints)


def client_properties(server_properties, kerberos_service_name=None):
  _, protocol, _ = select_listener(server_properties)
  properties = {"security.protocol": protocol}
  if protocol.startswith("SASL_"):
    mechanism = server_properties.get(
      "sasl.mechanism.inter.broker.protocol", "GSSAPI"
    )
    properties["sasl.mechanism"] = mechanism
    if mechanism.upper() == "GSSAPI":
      if not kerberos_service_name:
        raise Fail("Kafka Kerberos service name is not configured")
      properties["sasl.kerberos.service.name"] = kerberos_service_name

  if protocol in ("SSL", "SASL_SSL"):
    for name in (
      "ssl.truststore.location",
      "ssl.truststore.password",
      "ssl.truststore.type",
      "ssl.endpoint.identification.algorithm",
    ):
      value = server_properties.get(name)
      if value:
        properties[name] = value
  return properties
