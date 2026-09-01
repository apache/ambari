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

import http.client
import ssl
import urllib.request, urllib.error, urllib.parse
from ambari_commons.inet_utils import create_ssl_context


# Override the default http.client.HTTPSConnection to use the requested SSL context.
class HTTPSConnectionWithCustomSslVersion(http.client.HTTPSConnection):
  def __init__(self, host, port, ssl_version, ca_certs=None, **kwargs):
    self.ssl_version = ssl_version
    context = create_ssl_context(ssl_version, ca_certs)
    super(HTTPSConnectionWithCustomSslVersion, self).__init__(
      host, port, context=context, **kwargs
    )


def get_http_connection(
  host, port, https_enabled=False, ca_certs=None, ssl_version=ssl.PROTOCOL_TLS_CLIENT
):
  if https_enabled:
    return HTTPSConnectionWithCustomSslVersion(
      host, port, ssl_version, ca_certs=ca_certs
    )
  else:
    return http.client.HTTPConnection(host, port)


def build_url_opener(ignore_system_proxy=False, ssl_context=None, *handlers):
  """Build a request-scoped opener without changing urllib process globals."""
  request_handlers = list(handlers)
  if ignore_system_proxy:
    request_handlers.insert(0, urllib.request.ProxyHandler({}))
  if ssl_context is not None:
    request_handlers.append(urllib.request.HTTPSHandler(context=ssl_context))
  return urllib.request.build_opener(*request_handlers)
