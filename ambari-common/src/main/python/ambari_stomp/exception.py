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
"""Errors thrown by stomp.py connections.
"""

class StompException(Exception):
    """
    Common exception class. All specific stomp.py exceptions are subclasses
    of StompException, allowing the library user to catch all current and
    future library exceptions.
    """


class ConnectionClosedException(StompException):
    """
    Raised in the receiver thread when the connection has been closed
    by the server.
    """


class NotConnectedException(StompException):
    """
    Raised when there is currently no server connection.
    """


class ConnectFailedException(StompException):
    """
    Raised by Connection.attempt_connection when reconnection attempts
    have exceeded Connection.__reconnect_attempts_max.
    """


class InterruptedException(StompException):
    """
    Raised by receive when data read is interrupted.
    """
