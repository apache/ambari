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

try:
    import json
except ImportError:
    import simplejson as json
import logging
import posixpath

LOG = logging.getLogger(__name__)


class RestResource(object):

    """
    RestResource wrapper.
    """

    def __init__(self, client, path=""):
        """
        @param client: A Client object.
        @param path: The relative path of the resource.
        """
        self._client = client
        self._path = path.strip('/')

    @property
    def host_url(self):
        return self._client.host_url

    def _join_uri(self, relpath):
        if relpath is None:
            return self._path
        return self._path + posixpath.normpath('/' + relpath)

    def _set_headers(self, content_type=None):
        if content_type:
            return {'Content-Type': content_type}
        return None

    def _make_invoke(self, http_method, payload, headers, path):
        return self._client.invoke(
            http_method,
            path,
            payload=payload,
            headers=headers)

    def invoke(self, http_method, url_path=None, payload=None, headers=None):
        """
        Invoke an API http_method.
        """
        path = self._join_uri(url_path)
        resp, code, content = self._make_invoke(
            http_method, payload, headers, path)

        LOG.debug("RESPONSE from the REST request >>>>>>> \n" + str(resp))
        LOG.debug(
            "\n===========================================================")
        # take care of REST calls with no response

        try:
            isOK = (code == 200 or code == 201 or code == 202)

            if isOK and not resp:
                json_dict = {"status": code}
            else:
                json_dict = json.loads(resp)

            return json_dict
        except Exception as ex:
            LOG.error(
                "Command '%s %s' failed with error %s\n%s" %
                (http_method, path, code, resp))
            return {
                "status": code, "message": "Command '%s %s' failed with error %s" %
                (http_method, path, code)}

    def get(self, path=None):
        """
        Invoke the GET method .
        @param path: resource path
        @return: A dictionary of the REST result.
        """
        return self.invoke("GET", path)

    def put(self, path=None, payload=None, content_type=None):
        """
        Invoke the PUT method on a resource.
        @param path: resource path
        @param payload: Body of the request.
        @param content_type:
        @return: A dictionary of the REST result.
        """
        return self.invoke(
            "PUT",
            path,
            payload,
            self._set_headers(content_type))

    def post(self, path=None, payload=None, content_type=None):
        """
        Invoke the POST method on a resource.
        @param path: resource path
        @param payload: Body of the request.
        @param content_type:
        @return: A dictionary of the REST result.
        """
        return self.invoke(
            "POST",
            path,
            payload,
            self._set_headers(content_type))

    def delete(self, path=None, payload=None,):
        """
        Invoke the DELETE method on a resource.
        @param path: resource path
        @param payload: Body of the request.
        @return: A dictionary of the REST result.
        """
        return self.invoke("DELETE", path, payload)
