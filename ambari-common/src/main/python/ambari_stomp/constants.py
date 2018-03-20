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
"""The STOMP command and header name strings.
"""

HDR_ACCEPT_VERSION = 'accept-version'
HDR_ACK = 'ack'
HDR_CONTENT_LENGTH = 'content-length'
HDR_CONTENT_TYPE = 'content-type'
HDR_DESTINATION = 'destination'
HDR_HEARTBEAT = 'heart-beat'
HDR_HOST = 'host'
HDR_ID = 'id'
HDR_MESSAGE_ID = 'message-id'
HDR_LOGIN = 'login'
HDR_PASSCODE = 'passcode'
HDR_RECEIPT = 'receipt'
HDR_SUBSCRIPTION = 'subscription'
HDR_TRANSACTION = 'transaction'

CMD_ABORT = 'ABORT'
CMD_ACK = 'ACK'
CMD_BEGIN = 'BEGIN'
CMD_COMMIT = 'COMMIT'
CMD_CONNECT = 'CONNECT'
CMD_DISCONNECT = 'DISCONNECT'
CMD_NACK = 'NACK'
CMD_STOMP = 'STOMP'
CMD_SEND = 'SEND'
CMD_SUBSCRIBE = 'SUBSCRIBE'
CMD_UNSUBSCRIBE = 'UNSUBSCRIBE'
