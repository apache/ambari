#!/usr/bin/env python

import logging
import logging.handlers
import os
import select
import socket
import sys
#import pybonjour ''' TODO: findBonjour needs more refinement, disable for now '''
from shell import shellRunner
import time

logger = logging.getLogger()
timeout = 5
queried = []
resolved = []

class dnsResolver:
  def find(self, type):
    if os.path.isfile('/usr/bin/avahi-browse'):
      return self.findAvahi(type)
    else:
      return self.findBonjour(type)

  def findAvahi(self, type):
    address = []
    shell = shellRunner()
    script = []
    script.append('/etc/init.d/avahi-daemon')
    script.append('status')
    result = shell.run(script)
    if result['exit_code']!=0:
      logger.info('Starting Avahi daemon.')
      script = []
      script.append('/etc/init.d/avahi-daemon')
      script.append('start')
      result = shell.run(script)
      if result['exit_code']==0:
        logger.info('Avahi daemon started.')
      else:
        logger.error(result['output'])
      time.sleep(2)
    script = []
    script.append('avahi-browse')
    script.append('-t')
    script.append('-r')
    script.append(type)
    result = shell.run(script)
    list = result['output'].rsplit("\n")
    ip =""
    port = "2181"
    for line in list:
      if 'address = ' in line:
        start = line.find('[') + 1
        end = line.find(']')
        ip = line[start:end]
      if 'port = ' in line:
        start = line.find('[') + 1
        end = line.find(']')
        port = line[start:end]
        address.append(ip+':'+port)
    return ",".join(address)

  ''' For system with mDNSResponder installed (i.e. MacOSX) use native Apple Bonjour API'''
  def findBonjour(self, type):
    browse_sdRef = pybonjour.DNSServiceBrowse(regtype = regtype, callBack = browse_callback)
    try:
      try:
        while True:
          ready = select.select([browse_sdRef], [], [])
          if browse_sdRef in ready[0]:
            pybonjour.DNSServiceProcessResult(browse_sdRef)
      except KeyboardInterrupt:
        pass
    finally:
      browse_sdRef.close()

    def query_record_callback(sdRef, flags, interfaeeIndex, errorCode, fullname, rrtype, rrclass, rdata, ttl):
      if errorCode == pybonjour.kDNSServiceErr_NoError:
        logger.info('Located '+self.type+' IP ='+socket.inet_ntoa(rdata))
        queried.append(True)


    def resolve_callback(sdRef, flags, interfaceIndex, errorCode, fullname, hosttarget, port, txtRecord):
      if errorCode != pybonjour.kDNSServiceErr_NoError:
        return
      logger.info('Located '+fullname+' Host Target '+hosttarget+' Port '+port)
      query_sdRef = pybonjour.DNSServiceQueryRecord(
                      interfaceIndex = interfaceIndex,
                      fullname = hosttarget,
                      rrtype = pybonjour.kDNSServiceType_A,
                      callBack = query_record_callback)

      try:
        while not queried:
            ready = select.select([query_sdRef], [], [], timeout)
            if query_sdRef not in ready[0]:
                logger.warn('Query record timed out')
                break
            pybonjour.DNSServiceProcessResult(query_sdRef)
        else:
            queried.pop()
      finally:
        query_sdRef.close()

      resolved.append(True)

    def browse_callback(sdRef, flags, interfaceIndex, errorCode, serviceName,
                        regtype, replyDomain):
      if errorCode != pybonjour.kDNSServiceErr_NoError:
        return

      if not (flags & pybonjour.kDNSServiceFlagsAdd):
        logger.info('Service removed')
        return

      logger.info('Service added; resolving')

      resolve_sdRef = pybonjour.DNSServiceResolve(0,
                                                interfaceIndex,
                                                serviceName,
                                                regtype,
                                                replyDomain,
                                                resolve_callback)

      try:
        while not resolved:
            ready = select.select([resolve_sdRef], [], [], timeout)
            if resolve_sdRef not in ready[0]:
                logger.info('Resolve timed out')
                break
            pybonjour.DNSServiceProcessResult(resolve_sdRef)
        else:
            resolved.pop()
      finally:
        resolve_sdRef.close()


    

if __name__ == "__main__":
  test = dnsResolver()
  print test.find('_zookeeper._tcp')

