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
from resource_management.core.logger import Logger

import socket
import urllib2
import urllib
import subprocess

def makeHTTPCall(url, header={}, body=None):
  # timeout in seconds
  timeout = 10
  socket.setdefaulttimeout(timeout)

  try:
    data = None
    if body:
      data = urllib.urlencode(body)
    req = urllib2.Request(url, data, header)

    response = urllib2.urlopen(req)
    responseContent = response.read()
    return responseContent
  except urllib2.URLError as e:
    if hasattr(e, 'reason'):
      Logger.error( 'Reason' + str(e.reason))
    if hasattr(e, 'code'):
      Logger.error('Error code: ' + str(e.code))
    raise e
    

def runLocalCmd(cmd):
  return subprocess.call(cmd, shell=True)  

