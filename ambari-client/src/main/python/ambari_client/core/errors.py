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

    def __init__(self, msg=None, http_code=None, response=None):
        self.msg = msg or ''
        self.status_code = http_code
        self.response = response
        Exception.__init__(self)
        
    def _get_message(self):
        return self.msg
    def _set_message(self, msg):
        self.msg = msg or ''
    message = property(_get_message, _set_message)    
    
    def __str__(self):
        if self.msg:
            return self.msg
        try:
            return self._fmt % self.__dict__
        except (NameError, ValueError, KeyError), e:
            return 'exception %s: %s' \
                % (self.__class__.__name__, str(e))
        
class ResourceNotFound(ResourceError):
    """Exception raised when no resource was found. 
    """

class RequestError(Exception):
    """Exception for incorrect request """
    
class Unauthorized(ResourceError):
    """Exception when an authorization is required """

class RequestFailed(ResourceError):
    """Exception for unexpected HTTP error  """