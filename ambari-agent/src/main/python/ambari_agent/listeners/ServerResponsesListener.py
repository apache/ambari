#!/usr/bin/env python3

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

import logging
import threading

from ambari_agent.listeners import EventListener
from ambari_agent import Utils
from ambari_agent import Constants

logger = logging.getLogger(__name__)


class ServerResponsesListener(EventListener):
  """
  Listener of Constants.SERVER_RESPONSES_TOPIC events from server.
  """

  RESPONSE_STATUS_STRING = "status"
  RESPONSE_STATUS_SUCCESS = "OK"

  def __init__(self, initializer_module):
    super(ServerResponsesListener, self).__init__(initializer_module)
    self.callbacks_lock = threading.RLock()
    self.reset_responses()

  def register_response_callback(
    self, correlation_id, on_success=None, on_error=None
  ):
    with self.callbacks_lock:
      if on_success is not None:
        self.listener_functions_on_success[correlation_id] = on_success
      if on_error is not None:
        self.listener_functions_on_error[correlation_id] = on_error
    return lambda: self.discard_response_callback(correlation_id)

  def register_logging_handler(self, correlation_id, handler):
    with self.callbacks_lock:
      self.logging_handlers[correlation_id] = handler
    return lambda: self.discard_logging_handler(correlation_id)

  def register_synchronous_response(self, correlation_id):
    with self.callbacks_lock:
      self.synchronous_response_ids.add(correlation_id)
    return lambda: self.discard_synchronous_response(correlation_id)

  def discard_response_callback(self, correlation_id):
    with self.callbacks_lock:
      self.listener_functions_on_success.pop(correlation_id, None)
      self.listener_functions_on_error.pop(correlation_id, None)

  def discard_logging_handler(self, correlation_id):
    with self.callbacks_lock:
      self.logging_handlers.pop(correlation_id, None)

  def discard_synchronous_response(self, correlation_id):
    with self.callbacks_lock:
      self.synchronous_response_ids.discard(correlation_id)
      with self.responses.dict_lock:
        self.responses.dict.pop(correlation_id, None)

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.SERVER_RESPONSES_TOPIC topic is received from server.
    This type of event is general response to the agent request and contains 'correlationId', which is an int value
    of the request it responds to.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    if Constants.CORRELATION_ID_STRING in headers:
      correlation_id = int(headers[Constants.CORRELATION_ID_STRING])

      with self.callbacks_lock:
        synchronous_response = correlation_id in self.synchronous_response_ids
        self.synchronous_response_ids.discard(correlation_id)
        had_callback = (
          correlation_id in self.listener_functions_on_success
          or correlation_id in self.listener_functions_on_error
        )
        if (
          self.RESPONSE_STATUS_STRING in message
          and message[self.RESPONSE_STATUS_STRING] == self.RESPONSE_STATUS_SUCCESS
        ):
          callback = self.listener_functions_on_success.pop(correlation_id, None)
        else:
          callback = self.listener_functions_on_error.pop(correlation_id, None)

        self.listener_functions_on_success.pop(correlation_id, None)
        self.listener_functions_on_error.pop(correlation_id, None)
        if not had_callback and synchronous_response:
          self.responses.put(correlation_id, message)
      if had_callback:
        if callback is not None:
          callback(headers, message)
    else:
      logger.warning(
        f"Received a message from server without a '{Constants.CORRELATION_ID_STRING}' header. Ignoring the message"
      )

  def get_handled_path(self):
    return Constants.SERVER_RESPONSES_TOPIC

  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type
    """
    if Constants.CORRELATION_ID_STRING in headers:
      correlation_id = int(headers[Constants.CORRELATION_ID_STRING])

      with self.callbacks_lock:
        logging_handler = self.logging_handlers.pop(correlation_id, None)
      if logging_handler is not None:
        message_json = logging_handler(headers, message_json)
        if message_json.startswith(" :"):
          message_json = message_json[2:]
        return f" (correlation_id={correlation_id}): {message_json}"

      status = (
        message_json.get(self.RESPONSE_STATUS_STRING)
        if isinstance(message_json, dict)
        else None
      )
      return (
        f" (correlation_id={correlation_id}, status={status}, "
        f"payload-type={type(message_json).__name__})"
      )

    status = (
      message_json.get(self.RESPONSE_STATUS_STRING)
      if isinstance(message_json, dict)
      else None
    )
    return f" (status={status}, payload-type={type(message_json).__name__})"

  def reset_responses(self):
    """
    Resets data saved on per-response basis.
    Should be called when correlactionIds are reset to 0 aka. re-registration case.
    """
    command_statuses = getattr(self.initializer_module, "commandStatuses", None)
    if command_statuses is not None:
      command_statuses.clear_pending_batches()

    with self.callbacks_lock:
      self.responses = Utils.BlockingDictionary()
      self.listener_functions_on_success = {}
      self.listener_functions_on_error = {}
      self.logging_handlers = {}
      self.synchronous_response_ids = set()
