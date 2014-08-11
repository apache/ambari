#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
import posixpath
import sys
try:
    import pycurl
# pycurl is not necessary for testcases, mock it
except ImportError:
    from mock.mock import MagicMock
    pycurl = MagicMock()
import cStringIO
import StringIO
import pdb
try:
    import json
except ImportError:
    import simplejson as json
from ambari_client.core.http_utils import uri_encoding

__docformat__ = "epytext"

LOG = logging.getLogger(__name__)


class HttpClient(object):

    """
    Basic HTTP client for rest APIs.
    """

    def __init__(self, host_url, user_name, password):
        """
        @param host_url: The base url to the API.

        """

        self._host_url = host_url.rstrip('/')
        self._headers = {}
        self.c = pycurl.Curl()
        if user_name is not None:
            self.c.setopt(pycurl.HTTPAUTH, pycurl.HTTPAUTH_BASIC)
            userpass = user_name + ':'
            if password is not None:
                userpass += password
        LOG.debug("pycurl.USERPWD value = " + str(userpass))
        self.c.setopt(pycurl.USERPWD, userpass)

    def set_headers(self, headers):
        """
        Add headers to the request
        """
        self._headers = headers
        return self

    @property
    def host_url(self):
        return self._host_url

    def _get_headers(self, headers):
        res = self._headers.copy()
        if headers:
            res.update(headers)
        return res

    def invoke(self, http_method, path, payload=None, headers=None):
        """
        Submit an HTTP request.
        @param http_method: GET, POST, PUT, DELETE
        @param path: The path of the resource.
        @param payload: The payload to attach to the body of the request.
        @param headers: The headers to set for this request.

        @return: The result of REST request
        """
        # pdb.set_trace()
        LOG.debug("invoke : http_method = " + str(http_method))
        # Prepare URL and params
        url = self._normalize(path)
        if http_method in ("GET", "DELETE"):
            if payload is not None:
                self.logger.warn(
                    "GET http_method does not pass any payload. Path '%s'" %
                    (path,))
                payload = None

	self.c.unsetopt(pycurl.CUSTOMREQUEST)
        buf = cStringIO.StringIO()
        self.c.setopt(pycurl.WRITEFUNCTION, buf.write)
        self.c.setopt(pycurl.SSL_VERIFYPEER, 0)


        LOG.debug("invoke : url = " + str(url))
        # set http_method
        if http_method == "GET":
            self.c.setopt(pycurl.HTTPGET, 1)
        elif http_method == "HEAD":
            self.c.setopt(pycurl.HTTPGET, 1)
            self.c.setopt(pycurl.NOBODY, 1)
        elif http_method == "POST":
            self.c.setopt(pycurl.POST, 1)
        elif http_method == "PUT":
            self.c.setopt(pycurl.UPLOAD, 1)
        else:
            self.c.setopt(pycurl.CUSTOMREQUEST, http_method)

        data = None
        if http_method in ('POST', 'PUT'):
            LOG.debug("data..........." + str(payload))
            data = json.dumps(payload)
            # data= data.decode('unicode-escape')
            # LOG.debug( "after unicode decode")
            # LOG.debug( data)
            data = self._to_bytestring(data)
            LOG.debug("after _to_bytestring")
            LOG.debug(data)
            content = StringIO.StringIO(data)
            LOG.debug(content)
            content_length = len(data)
            LOG.debug("content_length........." + str(content_length))

            if http_method == 'POST':
                self.c.setopt(pycurl.POSTFIELDSIZE, content_length)
            else:
                self.c.setopt(pycurl.INFILESIZE, content_length)

            self.c.setopt(pycurl.READFUNCTION, content.read)

        self.c.setopt(self.c.URL, url)
        headers = self._get_headers(headers)
        headers_l = ["%s: %s" % pair for pair in sorted(headers.iteritems())]
        LOG.debug(headers_l)
        self.c.setopt(pycurl.HTTPHEADER, headers_l)

        LOG.debug(
            "invoke : pycurl.EFFECTIVE_URL = " +
            self.c.getinfo(
                pycurl.EFFECTIVE_URL))
        try:
            self.c.perform()
        except Exception as ex:
            LOG.debug(sys.stderr, str(ex))
            raise ex
        contents_type = self.c.getinfo(pycurl.CONTENT_TYPE)
        LOG.debug("invoke : pycurl.CONTENT_TYPE = " + contents_type)
        code = self.c.getinfo(pycurl.RESPONSE_CODE)
        LOG.debug("invoke : pycurl.RESPONSE_CODE = " + str(code))
        response = buf.getvalue()
        buf.close()
        LOG.debug("invoke : COMPLETED ")
        return response, code, contents_type

    def _to_bytestring(self, s):
        #    if not isinstance(s, basestring):
        #      raise TypeError("value should be a str or unicode")
        if isinstance(s, unicode):
            return s.encode('utf-8')
        return s

    def _normalize(self, path):
        res = self._host_url
        if path:
            res += posixpath.normpath('/' + path.lstrip('/'))
        return uri_encoding(res)
