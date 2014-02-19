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

import optparse
import sys
import os
import logging
import tempfile
import urllib2
import socket
import json
import base64
import time

AMBARI_HOSTNAME = None
AMBARI_PORT = 8080
CLUSTER_NAME = None
PROTOCOL = "http"
USERNAME = "admin"
PASSWORD = "admin"
DEFAULT_TIMEOUT = 10 # seconds
START_ON_RELOCATE = False

# Supported Actions
RELOCATE_ACTION = 'relocate'
ALLOWED_ACTUAL_STATES_FOR_RELOCATE = [ 'INIT', 'UNKNOWN', 'DISABLED', 'UNINSTALLED' ]
ALLOWED_HOST_STATUS_FOR_RELOCATE = [ 'HEALTHY' ]
STATUS_WAIT_TIMEOUT = 120 # seconds
STATUS_CHECK_INTERVAL = 10 # seconds

# API calls
GET_CLUSTERS_URI = "/api/v1/clusters/"
GET_HOST_COMPONENTS_URI = "/api/v1/clusters/{0}/services/{1}/components/{2}" +\
                          "?fields=host_components"
GET_HOST_COMPONENT_DESIRED_STATE_URI = "/api/v1/clusters/{0}/hosts/{1}" +\
                                       "/host_components/{2}" +\
                                       "?fields=HostRoles/desired_state"
GET_HOST_COMPONENT_STATE_URI = "/api/v1/clusters/{0}/hosts/{1}" +\
                               "/host_components/{2}" +\
                               "?fields=HostRoles/state"
GET_HOST_STATE_URL = "/api/v1/clusters/{0}/hosts/{1}?fields=Hosts/host_state"
HOST_COMPONENT_URI = "/api/v1/clusters/{0}/hosts/{1}/host_components/{2}"
ADD_HOST_COMPONENT_URI = "/api/v1/clusters/{0}/hosts?Hosts/host_name={1}"

logger = logging.getLogger()



class PreemptiveBasicAuthHandler(urllib2.BaseHandler):

  def __init__(self):
    password_mgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
    password_mgr.add_password(None, getUrl(''), USERNAME, PASSWORD)
    self.passwd = password_mgr
    self.add_password = self.passwd.add_password

  def http_request(self, req):
    uri = req.get_full_url()
    user = USERNAME
    pw = PASSWORD
    raw = "%s:%s" % (user, pw)
    auth = 'Basic %s' % base64.b64encode(raw).strip()
    req.add_unredirected_header('Authorization', auth)
    return req


class AmbariResource:

  def __init__(self, serviceName, componentName):
    self.serviveName = serviceName
    self.componentName = componentName
    self.isInitialized = False

  def initializeResource(self):
    global CLUSTER_NAME
    if CLUSTER_NAME is None:
      CLUSTER_NAME = self.findClusterName()

    if self.serviveName is None:
      raise Exception('Service name undefined')

    if self.componentName is None:
      raise Exception('Component name undefined')

    handler = PreemptiveBasicAuthHandler()
    opener = urllib2.build_opener(handler)
    # Install opener for all requests
    urllib2.install_opener(opener)
    self.urlOpener = opener

    self.old_hostname = self.getHostname()

    self.isInitialized = True


  def relocate(self, new_hostname):
    if not self.isInitialized:
      raise Exception('Resource not initialized')

    # If old and new hostname are the same exit harmlessly
    if self.old_hostname == new_hostname:
      logger.error('New hostname is same as existing host name, %s' % self.old_hostname)
      sys.exit(2)
    pass

    try:
      self.verifyHostComponentStatus(self.old_hostname, new_hostname, self.componentName)
    except Exception, e:
      logger.error("Exception caught on verify relocate request.")
      logger.error(e.message)
      sys.exit(3)

    # Put host component in Maintenance state
    self.updateHostComponentStatus(self.old_hostname, self.componentName,
                                   "Disable", "DISABLED")

    # Delete current host component
    self.deleteHostComponent(self.old_hostname, self.componentName)

    # Add component on the new host
    self.addHostComponent(new_hostname, self.componentName)

    # Install host component
    self.updateHostComponentStatus(new_hostname, self.componentName,
                                   "Installing", "INSTALLED")

    # Wait on install
    self.waitOnHostComponentUpdate(new_hostname, self.componentName,
                                   "INSTALLED")

    if START_ON_RELOCATE:
      # Start host component
      self.updateHostComponentStatus(new_hostname, self.componentName,
                                     "Starting", "STARTED")

      # Wait on start
      self.waitOnHostComponentUpdate(new_hostname, self.componentName, "STARTED")
    pass
  pass

  def waitOnHostComponentUpdate(self, hostname, componentName, status):
    logger.info("Waiting for host component status to update ...")
    sleep_itr = 0
    state = None
    while sleep_itr < STATUS_WAIT_TIMEOUT:
      try:
        state = self.getHostComponentState(hostname, componentName)
        if status == state:
          logger.info("Status update successful. status: %s" % state)
          return
        pass
      except Exception, e:
        logger.error("Caught an exception waiting for status update.. "
                     "continuing to wait...")
      pass

      time.sleep(STATUS_CHECK_INTERVAL)
      sleep_itr += STATUS_CHECK_INTERVAL
    pass
    if state and state != status:
      logger.error("Timed out on wait, status unchanged. status = %s" % state)
      sys.exit(1)
    pass
  pass

  def addHostComponent(self, hostname, componentName):
    data = '{"host_components":[{"HostRoles":{"component_name":"%s"}}]}' % self.componentName
    req = urllib2.Request(getUrl(ADD_HOST_COMPONENT_URI.format(CLUSTER_NAME,
                          hostname)), data)

    req.add_header("X-Requested-By", "ambari_probe")
    req.get_method = lambda: 'POST'
    try:
      logger.info("Adding host component: %s" % req.get_full_url())
      resp = self.urlOpener.open(req)
      self.logResponse('Add host component response: ', resp)
    except Exception, e:
      logger.error('Create host component failed, component: {0}, host: {1}'
                    .format(componentName, hostname))
      logger.error(e)
      raise e
    pass

  def deleteHostComponent(self, hostname, componentName):
    req = urllib2.Request(getUrl(HOST_COMPONENT_URI.format(CLUSTER_NAME,
                                hostname, componentName)))
    req.add_header("X-Requested-By", "ambari_probe")
    req.get_method = lambda: 'DELETE'
    try:
      logger.info("Deleting host component: %s" % req.get_full_url())
      resp = self.urlOpener.open(req)
      self.logResponse('Delete component response: ', resp)
    except Exception, e:
      logger.error('Delete {0} failed.'.format(componentName))
      logger.error(e)
      raise e
    pass

  def updateHostComponentStatus(self, hostname, componentName, contextStr, status):
    # Update host component
    data = '{"RequestInfo":{"context":"%s %s"},"Body":{"HostRoles":{"state":"%s"}}}' % (contextStr, self.componentName, status)
    req = urllib2.Request(getUrl(HOST_COMPONENT_URI.format(CLUSTER_NAME,
                                hostname, componentName)), data)
    req.add_header("X-Requested-By", "ambari_probe")
    req.get_method = lambda: 'PUT'
    try:
      logger.info("%s host component: %s" % (contextStr, req.get_full_url()))
      resp = self.urlOpener.open(req)
      self.logResponse('Update host component response: ', resp)
    except Exception, e:
      logger.error('Update Status {0} failed.'.format(componentName))
      logger.error(e)
      raise e
    pass

  def verifyHostComponentStatus(self, old_hostname, new_hostname, componentName):
    # Check desired state of host component is not STOPPED or host is
    # unreachable
    actualState = self.getHostComponentState(old_hostname, componentName)

    if actualState not in ALLOWED_ACTUAL_STATES_FOR_RELOCATE:
      raise Exception('Aborting relocate action since host component '
                      'state is %s' % actualState)

    hostState = self.getHostSatus(new_hostname)
    if hostState not in ALLOWED_HOST_STATUS_FOR_RELOCATE:
      raise Exception('Aborting relocate action since host state is %s' % hostState)

    pass

  def getHostSatus(self, hostname):
    hostStateUrl = getUrl(GET_HOST_STATE_URL.format(CLUSTER_NAME, hostname))

    logger.info("Requesting host status: %s " % hostStateUrl)
    urlResponse = self.urlOpener.open(hostStateUrl)
    state = None

    if urlResponse:
      response = urlResponse.read()
      data = json.loads(response)
      logger.debug('Response from getHostSatus: %s' % data)
      if data:
        try:
          hostsInfo = data.get('Hosts')
          if not hostsInfo:
            raise Exception('Cannot find host state for host: {1}'.format(hostname))

          state = hostsInfo.get('host_state')
        except Exception, e:
          logger.error('Unable to parse json data. %s' % data)
          raise e
        pass

      else:
        logger.error("Unable to retrieve host state.")
      pass

    return state


  def getHostComponentState(self, hostname, componentName):
    hostStatusUrl = getUrl(GET_HOST_COMPONENT_STATE_URI.format(CLUSTER_NAME,
                                hostname, componentName))

    logger.info("Requesting host component state: %s " % hostStatusUrl)
    urlResponse = self.urlOpener.open(hostStatusUrl)
    state = None

    if urlResponse:
      response = urlResponse.read()
      data = json.loads(response)
      logger.debug('Response from getHostComponentState: %s' % data)
      if data:
        try:
          hostRoles = data.get('HostRoles')
          if not hostRoles:
            raise Exception('Cannot find host component state for component: ' +\
                            '{0}, host: {1}'.format(componentName, hostname))

          state = hostRoles.get('state')
        except Exception, e:
          logger.error('Unable to parse json data. %s' % data)
          raise e
        pass

      else:
        logger.error("Unable to retrieve host component desired state.")
      pass

    return state


  # Log response for PUT, POST or DELETE
  def logResponse(self, text=None, response=None):
    if response is not None:
      resp = str(response.getcode())
      if text is None:
        text = 'Logging response from server: '
      if resp is not None:
        logger.info(text + resp)

  def findClusterName(self):
    clusterUrl = getUrl(GET_CLUSTERS_URI)
    clusterName = None

    logger.info("Requesting clusters: " + clusterUrl)
    urlResponse = self.urlOpener.open(clusterUrl)
    if urlResponse is not None:
      response = urlResponse.read()
      data = json.loads(response)
      logger.debug('Response from findClusterName: %s' % data)
      if data:
        try:
          clusters = data.get('items')
          if len(clusters) > 1:
            raise Exception('Multiple clusters found. %s' % clusters)

          clusterName = clusters[0].get('Clusters').get('cluster_name')
        except Exception, e:
          logger.error('Unable to parse json data. %s' % data)
          raise e
        pass
      else:
        logger.error("Unable to retrieve clusters data.")
      pass

    return clusterName

  def getHostname(self):
    hostsUrl = getUrl(GET_HOST_COMPONENTS_URI.format(CLUSTER_NAME,
                  self.serviveName, self.componentName))

    logger.info("Requesting host info: " + hostsUrl)
    urlResponse = self.urlOpener.open(hostsUrl)
    hostname = None

    if urlResponse is not None:
      response = urlResponse.read()
      data = json.loads(response)
      logger.debug('Response from getHostname: %s' % data)
      if data:
        try:
          hostRoles = data.get('host_components')
          if not hostRoles:
            raise Exception('Cannot find host component data for service: ' +\
                            '{0}, component: {1}'.format(self.serviveName, self.componentName))
          if len(hostRoles) > 1:
            raise Exception('More than one hosts found with the same role')

          hostname = hostRoles[0].get('HostRoles').get('host_name')
        except Exception, e:
          logger.error('Unable to parse json data. %s' % data)
          raise e
        pass

      else:
        logger.error("Unable to retrieve host component data.")
      pass

    return hostname


def getUrl(partial_url):
  return PROTOCOL + "://" + AMBARI_HOSTNAME + ":" + AMBARI_PORT + partial_url

def get_supported_actions():
  return [ RELOCATE_ACTION ]

#
# Main.
#
def main():
  tempDir = tempfile.gettempdir()
  outputFile = os.path.join(tempDir, "ambari_reinstall_probe.out")

  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is a Ambari thin client and '
                         'supports relocation of ambari host components on '
                         'Ambari managed clusters.')

  parser.add_option("-v", "--verbose", dest="verbose", action="store_false",
                  default=False, help="output verbosity.")
  parser.add_option("-s", "--host", dest="server_hostname",
                  help="Ambari server host name.")
  parser.add_option("-p", "--port", dest="server_port",
                  default="8080" ,help="Ambari server port. [default: 8080]")
  parser.add_option("-r", "--protocol", dest="protocol", default = "http",
                  help="Protocol for communicating with Ambari server ("
                       "http/https) [default: http].")
  parser.add_option("-c", "--cluster-name", dest="cluster_name",
                  help="Ambari cluster to operate on.")
  parser.add_option("-e", "--service-name", dest="service_name",
                  help="Ambari Service to which the component belongs to.")
  parser.add_option("-m", "--component-name", dest="component_name",
                  help="Ambari Service Component to operate on.")
  parser.add_option("-n", "--new-host", dest="new_hostname",
                  help="New host to relocate the component to.")
  parser.add_option("-a", "--action", dest="action", default = "relocate",
                  help="Script action. [default: relocate]")
  parser.add_option("-o", "--output-file", dest="outputfile",
                  default = outputFile, metavar="FILE",
                  help="Output file. [default: %s]" % outputFile)
  parser.add_option("-u", "--username", dest="username",
                  default="admin" ,help="Ambari server admin user. [default: admin]")
  parser.add_option("-w", "--password", dest="password",
                  default="admin" ,help="Ambari server admin password.")
  parser.add_option("-d", "--start-component", dest="start_component",
                  action="store_false", default=False,
                  help="Should the script start the component after relocate.")

  (options, args) = parser.parse_args()

  # set verbose
  if options.verbose:
    logging.basicConfig(level=logging.DEBUG)
  else:
    logging.basicConfig(level=logging.INFO)

  global AMBARI_HOSTNAME
  AMBARI_HOSTNAME = options.server_hostname

  global AMBARI_PORT
  AMBARI_PORT = options.server_port

  global CLUSTER_NAME
  CLUSTER_NAME = options.cluster_name

  global PROTOCOL
  PROTOCOL = options.protocol

  global USERNAME
  USERNAME = options.username

  global PASSWORD
  PASSWORD = options.password

  global START_ON_RELOCATE
  START_ON_RELOCATE = options.start_component

  global logger
  logger = logging.getLogger('AmbariProbe')
  handler = logging.FileHandler(options.outputfile)
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  handler.setFormatter(formatter)
  logger.addHandler(handler)

  action = RELOCATE_ACTION

  if options.action is not None:
    if options.action not in get_supported_actions():
      logger.error("Unsupported action: " + options.action + ", "
                  "valid actions: " + str(get_supported_actions()))
      sys.exit(1)
    else:
      action = options.action

  socket.setdefaulttimeout(DEFAULT_TIMEOUT)

  ambariResource = AmbariResource(serviceName=options.service_name,
                                  componentName=options.component_name)
  ambariResource.initializeResource()

  if action == RELOCATE_ACTION:
    if options.new_hostname is not None:
      ambariResource.relocate(options.new_hostname)

if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
