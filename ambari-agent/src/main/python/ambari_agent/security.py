#!/usr/bin/env python2.6

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
logger = logging.getLogger()

GEN_AGENT_KEY="openssl req -new -newkey rsa:1024 -nodes -keyout %(keysdir)s/%(hostname)s.key\
	-subj /OU=%(hostname)s/\
        -out %(keysdir)s/%(hostname)s.csr"


class VerifiedHTTPSConnection(httplib.HTTPSConnection):
  def connect(self):
    sock = socket.create_connection((self.host, self.port), self.timeout)
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
class VerifiedHTTPSHandler(urllib2.HTTPSHandler):
  def __init__(self, connection_class = VerifiedHTTPSConnection):
    self.specialized_conn_class = connection_class
    urllib2.HTTPSHandler.__init__(self)
  def https_open(self, req):
    return self.do_open(self.specialized_conn_class, req)

def secured_url_open(req):
  logger.info("Secured url open")
  https_handler = VerifiedHTTPSHandler()
  url_opener = urllib2.build_opener(https_handler)
  stream = url_opener.open(req)
  return stream

class CertificateManager():
  def __init__(self, config):
    self.config = config
    self.keysdir = self.config.get('security', 'keysdir')
    self.server_crt=self.config.get('security', 'server_crt')
    
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
    get_ca_url = self.config.get('server', 'url') + '/cert/ca/'
    stream = urllib2.urlopen(get_ca_url)
    response = stream.read()
    stream.close()
    srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
    srvr_crt_f.write(response)
      
  def reqSignCrt(self):
    sign_crt_req_url = self.config.get('server', 'url') + '/certs/' + socket.gethostname()
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
    logger.info("Sign response from Server: \n" + pprint.pformat(data))
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