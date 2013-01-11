#!/usr/bin/env python2.6

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


import httplib
import urllib2
from urllib2 import Request
import socket
import ssl
import os
import logging
from subprocess import Popen, PIPE
import AmbariConfig
import json
import pprint
import traceback
logger = logging.getLogger()

GEN_AGENT_KEY="openssl req -new -newkey rsa:1024 -nodes -keyout %(keysdir)s/%(hostname)s.key\
	-subj /OU=%(hostname)s/\
        -out %(keysdir)s/%(hostname)s.csr"


class VerifiedHTTPSConnection(httplib.HTTPSConnection):
  """ Connecting using ssl wrapped sockets """
  def __init__(self, host, port=None, key_file=None, cert_file=None,
                     strict=None, timeout=socket._GLOBAL_DEFAULT_TIMEOUT):
    httplib.HTTPSConnection.__init__(self, host, port=port)
    pass
     
  def connect(self):
    if self.sock:
      self.sock.close()
    logger.info("SSL Connect being called.. connecting to the server")
    sock = socket.create_connection((self.host, self.port), 60)
    sock.setsockopt( socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    if self._tunnel_host:
      self.sock = sock
      self._tunnel()
    agent_key = AmbariConfig.config.get('security', 'keysdir') + os.sep + \
     socket.gethostname() + ".key"
    agent_crt = AmbariConfig.config.get('security', 'keysdir') + os.sep \
    + socket.gethostname() + ".crt" 
    server_crt = AmbariConfig.config.get('security', 'keysdir') + os.sep \
    + "ca.crt"
    
    self.sock = ssl.wrap_socket(sock,
                                keyfile=agent_key,
                                certfile=agent_crt,
                                cert_reqs=ssl.CERT_REQUIRED,
                                ca_certs=server_crt)


class CachedHTTPSConnection:
  """ Caches a ssl socket and uses a single https connection to the server. """
  
  def __init__(self, config):
    self.connected = False;
    self.config = config
    self.server = config.get('server', 'hostname')
    self.port = config.get('server', 'secured_url_port')
    self.connect()
  
  def connect(self):
      if  not self.connected:
        self.httpsconn = VerifiedHTTPSConnection(self.server, self.port)
        self.httpsconn.connect()
        self.connected = True
      # possible exceptions are catched and processed in Controller

  
  def forceClear(self):
    self.httpsconn = VerifiedHTTPSConnection(self.server, self.port)
    self.connect()
    
  def request(self, req): 
    self.connect()
    try:
      self.httpsconn.request(req.get_method(), req.get_full_url(), 
                                  req.get_data(), req.headers)
      response = self.httpsconn.getresponse()
      readResponse = response.read()
    except Exception as ex:
      # This exception is catched later in Controller
      logger.debug("Error in sending/receving data from the server " +
                   traceback.format_exc())
      self.connected = False
      raise IOError("Error occured during connecting to the server: " + str(ex))
    return readResponse
  
class CertificateManager():
  def __init__(self, config):
    self.config = config
    self.keysdir = self.config.get('security', 'keysdir')
    self.server_crt=self.config.get('security', 'server_crt')
    self.server_url = 'https://' + self.config.get('server', 'hostname') + ':' \
       + self.config.get('server', 'url_port')
    
  def getAgentKeyName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + socket.gethostname() + ".key"
  def getAgentCrtName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + socket.gethostname() + ".crt"
  def getAgentCrtReqName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + socket.gethostname() + ".csr"
  def getSrvrCrtName(self):
    keysdir = self.config.get('security', 'keysdir')
    return keysdir + os.sep + "ca.crt"
    
  def checkCertExists(self):
    
    s = self.config.get('security', 'keysdir') + os.sep + "ca.crt"

    server_crt_exists = os.path.exists(s)
    
    if not server_crt_exists:
      logger.info("Server certicate not exists, downloading")
      self.loadSrvrCrt()
    else:
      logger.info("Server certicate exists, ok")
      
    agent_key_exists = os.path.exists(self.getAgentKeyName())
    
    if not agent_key_exists:
      logger.info("Agent key not exists, generating request")
      self.genAgentCrtReq()
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
    stream = urllib2.urlopen(get_ca_url)
    response = stream.read()
    stream.close()
    srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
    srvr_crt_f.write(response)
      
  def reqSignCrt(self):
    sign_crt_req_url = self.server_url + '/certs/' + socket.gethostname()
    agent_crt_req_f = open(self.getAgentCrtReqName())
    agent_crt_req_content = agent_crt_req_f.read()
    passphrase_env_var = self.config.get('security', 'passphrase_env_var_name')
    passphrase = os.environ[passphrase_env_var]
    register_data = {'csr'       : agent_crt_req_content,
                    'passphrase' : passphrase}
    data = json.dumps(register_data)
    req = urllib2.Request(sign_crt_req_url, data, {'Content-Type': 'application/json'})
    f = urllib2.urlopen(req)
    response = f.read()
    f.close()
    data = json.loads(response)
    logger.debug("Sign response from Server: \n" + pprint.pformat(data))
    result=data['result']
    if result == 'OK':
      agentCrtContent=data['signedCa']
      agentCrtF = open(self.getAgentCrtName(), "w")
      agentCrtF.write(agentCrtContent)
    else:
      logger.error("Certificate signing failed")

  def genAgentCrtReq(self):
    generate_script = GEN_AGENT_KEY % {'hostname': socket.gethostname(),
                                     'keysdir' : self.config.get('security', 'keysdir')}
    logger.info(generate_script)
    p = Popen([generate_script], shell=True, stdout=PIPE)
    p.wait()
      
  def initSecurity(self):
    self.checkCertExists()
