#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from StringIO import StringIO
import gzip
import httplib
import urllib2
import socket
import ssl
import os
import logging
import subprocess
import ambari_simplejson as json
import pprint
import traceback
import hostname
import platform

logger = logging.getLogger(__name__)

GEN_AGENT_KEY = 'openssl req -new -newkey rsa:1024 -nodes -keyout "%(keysdir)s' \
                + os.sep + '%(hostname)s.key" -subj /OU=%(hostname)s/ ' \
                '-out "%(keysdir)s' + os.sep + '%(hostname)s.csr"'
KEY_FILENAME = '%(hostname)s.key'


class VerifiedHTTPSConnection(httplib.HTTPSConnection):
  """ Connecting using ssl wrapped sockets """
  def __init__(self, host, port=None, config=None):
    httplib.HTTPSConnection.__init__(self, host, port=port)
    self.two_way_ssl_required = False
    self.config = config

  def connect(self):
    self.two_way_ssl_required = self.config.isTwoWaySSLConnection()
    logger.debug("Server two-way SSL authentication required: %s" % str(
      self.two_way_ssl_required))
    if self.two_way_ssl_required is True:
      logger.info(
        'Server require two-way SSL authentication. Use it instead of one-way...')

    if not self.two_way_ssl_required:
      try:
        sock = self.create_connection()
        self.sock = ssl.wrap_socket(sock, cert_reqs=ssl.CERT_NONE)
        logger.info('SSL connection established. Two-way SSL authentication is '
                    'turned off on the server.')
      except (ssl.SSLError, AttributeError):
        self.two_way_ssl_required = True
        logger.info(
          'Insecure connection to https://' + self.host + ':' + self.port +
          '/ failed. Reconnecting using two-way SSL authentication..')

    if self.two_way_ssl_required:
      self.certMan = CertificateManager(self.config)
      self.certMan.initSecurity()
      agent_key = self.certMan.getAgentKeyName()
      agent_crt = self.certMan.getAgentCrtName()
      server_crt = self.certMan.getSrvrCrtName()

      sock = self.create_connection()

      try:
        self.sock = ssl.wrap_socket(sock,
                                    keyfile=agent_key,
                                    certfile=agent_crt,
                                    cert_reqs=ssl.CERT_REQUIRED,
                                    ca_certs=server_crt)
        logger.info('SSL connection established. Two-way SSL authentication '
                    'completed successfully.')
      except ssl.SSLError as err:
        logger.error('Two-way SSL authentication failed. Ensure that '
                     'server and agent certificates were signed by the same CA '
                     'and restart the agent. '
                     '\nIn order to receive a new agent certificate, remove '
                     'existing certificate file from keys directory. As a '
                     'workaround you can turn off two-way SSL authentication in '
                     'server configuration(ambari.properties) '
                     '\nExiting..')
        raise err

  def create_connection(self):
    if self.sock:
      self.sock.close()
    logger.info("SSL Connect being called.. connecting to the server")
    sock = socket.create_connection((self.host, self.port), 60)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    if self._tunnel_host:
      self.sock = sock
      self._tunnel()

    return sock


class CachedHTTPSConnection:
  """ Caches a ssl socket and uses a single https connection to the server. """

  def __init__(self, config):
    self.connected = False
    self.config = config
    self.server = hostname.server_hostname(config)
    self.port = config.get('server', 'secured_url_port')
    self.connect()

  def connect(self):
    if not self.connected:
      self.httpsconn = VerifiedHTTPSConnection(self.server, self.port,
                                               self.config)
      self.httpsconn.connect()
      self.connected = True
    # possible exceptions are caught and processed in Controller

  def forceClear(self):
    self.httpsconn = VerifiedHTTPSConnection(self.server, self.port,
                                             self.config)
    self.connect()

  def request(self, req):
    self.connect()
    try:
      self.httpsconn.request(req.get_method(), req.get_full_url(),
                             req.get_data(), req.headers)
      response = self.httpsconn.getresponse()
      # Ungzip if gzipped
      if response.getheader('Content-Encoding') == 'gzip':
        buf = StringIO(response.read())
        response = gzip.GzipFile(fileobj=buf)
      readResponse = response.read()
    except Exception as ex:
      # This exception is caught later in Controller
      logger.debug("Error in sending/receving data from the server " +
                   traceback.format_exc())
      logger.info("Encountered communication error. Details: " + repr(ex))
      self.connected = False
      raise IOError("Error occured during connecting to the server: " + str(ex))
    return readResponse


class CertificateManager():
  def __init__(self, config):
    self.config = config
    self.keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    self.server_crt = self.config.get('security', 'server_crt')
    self.server_url = 'https://' + hostname.server_hostname(config) + ':' \
                      + self.config.get('server', 'url_port')

  def getAgentKeyName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".key"

  def getAgentCrtName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".crt"

  def getAgentCrtReqName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + hostname.hostname(self.config) + ".csr"

  def getSrvrCrtName(self):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    return keysdir + os.sep + "ca.crt"

  def checkCertExists(self):

    s = os.path.abspath(
      self.config.get('security', 'keysdir')) + os.sep + "ca.crt"

    server_crt_exists = os.path.exists(s)

    if not server_crt_exists:
      logger.info("Server certicate not exists, downloading")
      self.loadSrvrCrt()
    else:
      logger.info("Server certicate exists, ok")

    agent_key_exists = os.path.exists(self.getAgentKeyName())

    if not agent_key_exists:
      logger.info("Agent key not exists, generating request")
      self.genAgentCrtReq(self.getAgentKeyName())
    else:
      logger.info("Agent key exists, ok")

    agent_crt_exists = os.path.exists(self.getAgentCrtName())

    if not agent_crt_exists:
      logger.info("Agent certificate not exists, sending sign request")
      self.reqSignCrt()
    else:
      logger.info("Agent certificate exists, ok")

  def loadSrvrCrt(self):
    get_ca_url = self.server_url + '/cert/ca/'
    logger.info("Downloading server cert from " + get_ca_url)
    proxy_handler = urllib2.ProxyHandler({})
    opener = urllib2.build_opener(proxy_handler)
    stream = opener.open(get_ca_url)
    response = stream.read()
    stream.close()
    srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
    srvr_crt_f.write(response)

  def reqSignCrt(self):
    sign_crt_req_url = self.server_url + '/certs/' + hostname.hostname(
      self.config)
    agent_crt_req_f = open(self.getAgentCrtReqName())
    agent_crt_req_content = agent_crt_req_f.read()
    passphrase_env_var = self.config.get('security', 'passphrase_env_var_name')
    passphrase = os.environ[passphrase_env_var]
    register_data = {'csr': agent_crt_req_content,
                     'passphrase': passphrase}
    data = json.dumps(register_data)
    proxy_handler = urllib2.ProxyHandler({})
    opener = urllib2.build_opener(proxy_handler)
    urllib2.install_opener(opener)
    req = urllib2.Request(sign_crt_req_url, data,
                          {'Content-Type': 'application/json'})
    f = urllib2.urlopen(req)
    response = f.read()
    f.close()
    try:
      data = json.loads(response)
      logger.debug("Sign response from Server: \n" + pprint.pformat(data))
    except Exception:
      logger.warn("Malformed response! data: " + str(data))
      data = {'result': 'ERROR'}
    result = data['result']
    if result == 'OK':
      agentCrtContent = data['signedCa']
      agentCrtF = open(self.getAgentCrtName(), "w")
      agentCrtF.write(agentCrtContent)
    else:
      # Possible exception is catched higher at Controller
      logger.error('Certificate signing failed.'
                   '\nIn order to receive a new agent'
                   ' certificate, remove existing certificate file from keys '
                   'directory. As a workaround you can turn off two-way SSL '
                   'authentication in server configuration(ambari.properties) '
                   '\nExiting..')
      raise ssl.SSLError

  def genAgentCrtReq(self, keyname):
    keysdir = os.path.abspath(self.config.get('security', 'keysdir'))
    generate_script = GEN_AGENT_KEY % {
      'hostname': hostname.hostname(self.config),
      'keysdir': keysdir}
    
    logger.info(generate_script)
    if platform.system() == 'Windows':
      p = subprocess.Popen(generate_script, stdout=subprocess.PIPE)
      p.communicate()
    else:
      p = subprocess.Popen([generate_script], shell=True,
                           stdout=subprocess.PIPE)
      p.communicate()
    # this is required to be 600 for security concerns.
    os.chmod(keyname, 0600)

  def initSecurity(self):
    self.checkCertExists()
