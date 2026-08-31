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

from ambari_commons.logging_utils import print_warning_msg


# overrides default httplib.HTTPSConnection implementation to use specified ssl version
class HTTPSConnectionWithCustomSslVersion(http.client.HTTPSConnection):
  def __init__(self, host, port, ssl_version, **kwargs):
    self.ssl_version = ssl_version
    context = ssl.SSLContext(ssl_version)
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE
    super(HTTPSConnectionWithCustomSslVersion, self).__init__(
      host, port, context=context, **kwargs
    )


def get_http_connection(
  host, port, https_enabled=False, ca_certs=None, ssl_version=ssl.PROTOCOL_TLS_CLIENT
):
  if https_enabled:
    if ca_certs:
      check_ssl_certificate_and_return_ssl_version(host, port, ca_certs, ssl_version)
    return HTTPSConnectionWithCustomSslVersion(host, port, ssl_version)
  else:
    return http.client.HTTPConnection(host, port)


def check_ssl_certificate_and_return_ssl_version(
  host, port, ca_certs, ssl_version=ssl.PROTOCOL_TLS_CLIENT
):
  try:
    ssl.get_server_certificate((host, port), ssl_version=ssl_version, ca_certs=ca_certs)
  except ssl.SSLError as ssl_error:
    from resource_management.core.exceptions import Fail

    raise Fail(
      f"Failed to verify the SSL certificate for https://{host}:{port} with CA certificate in {ca_certs}. Error : {str(ssl_error)}"
    )
  return ssl_version


def reconfigure_urllib2_opener(ignore_system_proxy=False):
  """
  Reconfigure urllib opener

  :type ignore_system_proxy bool
  """

  if ignore_system_proxy:
    proxy_handler = urllib.request.ProxyHandler({})
    opener = urllib.request.build_opener(proxy_handler)
    urllib.request.install_opener(opener)
