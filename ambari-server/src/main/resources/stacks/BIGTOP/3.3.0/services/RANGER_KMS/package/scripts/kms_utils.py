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

from resource_management.core.exceptions import Fail
from urllib.parse import urlencode, urlsplit


def strict_bool(value, property_name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{property_name} must be true or false")


def validate_ranger_url(url):
  try:
    parsed_url = urlsplit(url)
    hostname = parsed_url.hostname
    port = parsed_url.port
  except (TypeError, ValueError) as error:
    raise Fail("Ranger Admin URL is invalid") from error
  if (
    parsed_url.scheme not in ("http", "https")
    or not hostname
    or parsed_url.username is not None
    or parsed_url.password is not None
    or parsed_url.query
    or parsed_url.fragment
    or (port is not None and not 1 <= port <= 65535)
  ):
    raise Fail("Ranger Admin URL is invalid")
  return url.rstrip("/")


def ranger_service_api_url(url, **query):
  service_url = validate_ranger_url(url) + "/service/public/v2/api/service"
  return f"{service_url}?{urlencode(query)}" if query else service_url
