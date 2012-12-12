from httplib import HTTP
from urlparse import urlparse
import time

class NetUtil:

  CONNECT_SERVER_RETRY_INTERVAL_SEC = 10
  HEARTBEAT_IDDLE_INTERVAL_SEC = 3
  HEARTBEAT_NOT_IDDLE_INTERVAL_SEC = 1

  # Url within server to request during status check. This url
  # should return HTTP code 200
  SERVER_STATUS_REQUEST = "{0}/api/check"

  # For testing purposes
  DEBUG_STOP_RETRIES_FLAG = False

  def checkURL(self, url):
    """Try to connect to a given url. Result is True if url returns HTTP code 200, in any other case
    (like unreachable server or wrong HTTP code) result will be False
    """
    try:
      p = urlparse(url)
      h = HTTP(p[1])
      h.putrequest('HEAD', p[2])
      h.endheaders()
      if h.getreply()[0] == 200: return True
      else: return False
    except Exception, e:
      return False

  def try_to_connect(self, server_url, max_retries, logger = None):
    """Try to connect to a given url, sleeping for CONNECT_SERVER_RETRY_INTERVAL_SEC seconds
    between retries. No more than max_retries is performed. If max_retries is -1, connection
    attempts will be repeated forever until server is not reachable
    Returns count of retries
    """
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

