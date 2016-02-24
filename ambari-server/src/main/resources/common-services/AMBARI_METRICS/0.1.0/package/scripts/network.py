#!/usr/bin/env python
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

import httplib
import ssl

from resource_management.core.exceptions import Fail

def get_http_connection(host, port, https_enabled=False, ca_certs=None):
  if https_enabled:
    if ca_certs:
      check_ssl_certificate(host, port, ca_certs)
    return httplib.HTTPSConnection(host, port)
  else:
    return httplib.HTTPConnection(host, port)

def check_ssl_certificate(host, port, ca_certs):
  try:
    ssl.get_server_certificate((host, port), ssl_version=ssl.PROTOCOL_SSLv23, ca_certs=ca_certs)
  except (ssl.SSLError) as ssl_error:
    raise Fail("Failed to verify the SSL certificate for AMS Collector https://{0}:{1} with CA certificate in {2}"
               .format(host, port, ca_certs))
