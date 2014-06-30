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


class ResourceError(Exception):

    def __init__(self, response, resource_root=None):
        """
        Create new exception based on not successful server response
        @param response: StatusModel response
        @param resource_root: The resource which sent an error response
        """
        self.response = response
        self.resource_root = resource_root
        Exception.__init__(self)

    def get_message(self):
        """ Get an error message """
        return self.response.get_message()

    def get_status_code(self):
        """ Get a status(error) code from the server response """
        return self.response.status

    def get_reponse(self):
        """ StatusModel object """
        return self.reponse

    def get_root_resource(self):
        """ AmbariClient object """
        return self.resource_root

    def __str__(self):
        if self.get_message():
            return "exception: %s. %s" % (
                self.response.status, self.get_message())
        try:
            return self._fmt % self.__dict__
        except (NameError, ValueError, KeyError) as e:
            return 'exception %s: %s' \
                   % (self.__class__.__name__, str(e))


class ResourceConflict(ResourceError):

    """ 409 status code """


class ResourceNotFound(ResourceError):

    """ 404 status code """


class BadRequest(ResourceError):

    """ 400 status code """


class AuthorizationError(ResourceError):

    """ 401 status code """


class ForbiddenError(ResourceError):

    """ 403 status code """


class InternalServerError(ResourceError):

    """ 500 status code """


class MethodNotAllowed(ResourceError):

    """ 405 status code """


class UnknownServerError(ResourceError):

    """ Received other response code """

_exceptions_to_codes = {409: ResourceConflict,
                        404: ResourceNotFound,
                        400: BadRequest,
                        401: AuthorizationError,
                        403: ForbiddenError,
                        500: InternalServerError,
                        405: MethodNotAllowed}
