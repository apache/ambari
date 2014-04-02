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


from urlparse import urlparse
import time
import logging
import httplib
from ssl import SSLError

logger = logging.getLogger()

class NetUtil:

  CONNECT_SERVER_RETRY_INTERVAL_SEC = 10
  HEARTBEAT_IDDLE_INTERVAL_SEC = 10
  MINIMUM_INTERVAL_BETWEEN_HEARTBEATS = 0.1

  # Url within server to request during status check. This url
  # should return HTTP code 200
  SERVER_STATUS_REQUEST = "{0}/cert/ca"

  # For testing purposes
  DEBUG_STOP_RETRIES_FLAG = False

  def checkURL(self, url):
    """Try to connect to a given url. Result is True if url returns HTTP code 200, in any other case
    (like unreachable server or wrong HTTP code) result will be False
    """
    logger.info("Connecting to the following url " + url);
    try:
      parsedurl = urlparse(url)
      ca_connection = httplib.HTTPSConnection(parsedurl[1])
      ca_connection.request("GET", parsedurl[2])
      response = ca_connection.getresponse()  
      status = response.status    
      logger.info("Calling url received " + str(status))
      
      if status == 200: 
        return True
      else: 
        return False
    except SSLError as slerror:
      logger.error(str(slerror))
      logger.error("SSLError: Failed to connect. Please check openssl library versions. \n" +
                   "Refer to: https://bugzilla.redhat.com/show_bug.cgi?id=1022468 for more details.")
      return False
    
    except Exception, e:
      logger.warning("Failed to connect to " + str(url) + " due to " + str(e) + "  ")
      return False

  def try_to_connect(self, server_url, max_retries, logger = None):
    """Try to connect to a given url, sleeping for CONNECT_SERVER_RETRY_INTERVAL_SEC seconds
    between retries. No more than max_retries is performed. If max_retries is -1, connection
    attempts will be repeated forever until server is not reachable
    Returns count of retries
    """
    if logger is not None:
      logger.info("DEBUG: Trying to connect to the server at " + server_url)
    retries = 0
    while (max_retries == -1 or retries < max_retries) and not self.DEBUG_STOP_RETRIES_FLAG:
      server_is_up = self.checkURL(self.SERVER_STATUS_REQUEST.format(server_url))
      if server_is_up:
        break
      else:
        if logger is not None:
          logger.info('Server at {0} is not reachable, sleeping for {1} seconds...'.format(server_url,
            self.CONNECT_SERVER_RETRY_INTERVAL_SEC))
        retries += 1
        time.sleep(self.CONNECT_SERVER_RETRY_INTERVAL_SEC)
    return retries

