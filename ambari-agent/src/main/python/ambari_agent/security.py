import httplib
import urllib2
from urllib2 import Request
import socket
import ssl
import os
import logging
from subprocess import Popen, PIPE
import AmbariConfig

logger = logging.getLogger()

GEN_AGENT_KEY="openssl req -new -newkey rsa:1024 -nodes -keyout %(keysdir)s/%(hostname)s.key\
	-subj /OU=%(hostname)s/\
        -out %(keysdir)s/%(hostname)s.csr"

class CertificateManager():
    def __init__(self, config):
        self.config = config
        self.keysdir = self.config.get('security', 'keysdir')
        self.server_crt=self.config.get('security', 'server_crt')
    def getAgentKeyName(self):
        return self.keysdir + os.sep + socket.gethostname() + ".key"
    def getAgentCrtName(self):
        return self.keysdir + os.sep + socket.gethostname() + ".key"
    def getSrvrCrtName(self):
        return self.keysdir + os.sep + "ca.crt"
        
    def checkCertExists(self):
        
        server_crt_exists = os.path.exists(self.getSrvrCrtName())
        
        if not server_crt_exists:
            logger.info("Server certicate not exists, downloading")
            self.loadSrvrCrt()
        else:
            logger.info("Server certicate exists, ok")
            
        agent_crt_exists = os.path.exists(self.getAgentCrtName())
        
        logger.info(self.getAgentCrtName())
        
        if not agent_crt_exists:
            logger.info("Agent certicate not exists, generating request")
            self.genAgentCrtReq()
        else:
            logger.info("Agent certicate exists, ok")
            
        
    def loadSrvrCrt(self):
      get_ca_url = self.config.get('server', 'url') + '/cert/ca/'
      stream = urllib2.urlopen(get_ca_url)
      response = stream.read()
      stream.close()
      srvr_crt_f = open(self.getSrvrCrtName(), 'w+')
      srvr_crt_f.write(response)
      
    def genAgentCrtReq(self):
        generate_script = GEN_AGENT_KEY % {'hostname': socket.gethostname(),
                                           'keysdir' : self.config.get('security', 'keysdir')}
        logger.info(generate_script)
        pp = Popen([generate_script], shell=True, stdout=PIPE)

    def initSecurity(self):
        self.checkCertExists()