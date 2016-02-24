#!/usr/bin/env python

'''
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
'''

import logging
import ssl
import socket
import httplib

logger = logging.getLogger()

# TODO merge this with security.py in ambari-agent and move to ambrari commons

class VerifiedHTTPSConnection(httplib.HTTPSConnection):
  """ Connecting using ssl wrapped sockets """

  def __init__(self, host, port, timeout, ca_certs):
    httplib.HTTPSConnection.__init__(self, host, port=port, timeout=timeout)
    self.ca_certs = ca_certs

  def connect(self):

    try:
      sock = self.create_connection()
      self.sock = ssl.wrap_socket(sock, cert_reqs=ssl.CERT_REQUIRED,
                                  ca_certs=self.ca_certs)
      logger.info('SSL connection established.')
    except (ssl.SSLError, AttributeError) as ex:
      logger.info('Insecure connection to https://{0}:{1}/ failed'
                  .format(self.host, self.port))

  def create_connection(self):
    if self.sock:
      self.sock.close()
    logger.info("SSL Connect being called.. connecting to https://{0}:{1}/"
                .format(self.host, self.port))
    sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    if self._tunnel_host:
      self.sock = sock
      self._tunnel()

    return sock

class CachedHTTPConnection:
  """ Caches a socket and uses a single http connection to the server. """

  def __init__(self, host, port, timeout):
    self.connected = False
    self.host = host
    self.port = port
    self.timeout = timeout

  def connect(self):
    if not self.connected:
      self.httpconn = self.create_connection()
      self.httpconn.connect()
      self.connected = True

  def request(self, method, url, body=None, headers={}):
    self.connect()
    try:
      return self.httpconn.request(method, url, body, headers)
    except Exception as e:
      self.connected = False
      raise e

  def getresponse(self):
    return self.httpconn.getresponse()

  def create_connection(self):
    return httplib.HTTPConnection(self.host, self.port, self.timeout)

class CachedHTTPSConnection(CachedHTTPConnection):
  """ Caches an ssl socket and uses a single https connection to the server. """

  def __init__(self, host, port, timeout, ca_certs):
    self.ca_certs = ca_certs
    CachedHTTPConnection.__init__(self, host, port, timeout)

  def create_connection(self):
    return VerifiedHTTPSConnection(self.host, self.port, self.timeout, self.ca_certs)
