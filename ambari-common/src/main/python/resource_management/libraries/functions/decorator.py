#!/usr/bin/env python
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

Ambari Agent

"""

import time
__all__ = ['retry', 'safe_retry', ]

from resource_management.core.logger import Logger


def retry(times=3, sleep_time=1, max_sleep_time=8, backoff_factor=1, err_class=Exception):
  """
  Retry decorator for improved robustness of functions.
  :param times: Number of times to attempt to call the function.
  :param sleep_time: Initial sleep time between attempts
  :param backoff_factor: After every failed attempt, multiple the previous sleep time by this factor.
  :param err_class: Exception class to handle
  :return: Returns the output of the wrapped function.
  """
  def decorator(function):
    def wrapper(*args, **kwargs):
      _times = times
      _sleep_time = sleep_time
      _backoff_factor = backoff_factor
      _err_class = err_class

      while _times > 1:
        _times -= 1
        try:
          return function(*args, **kwargs)
        except _err_class, err:
          Logger.info("Will retry %d time(s), caught exception: %s. Sleeping for %d sec(s)" % (_times, str(err), _sleep_time))
          time.sleep(_sleep_time)
        if(_sleep_time * _backoff_factor <= max_sleep_time):
          _sleep_time *= _backoff_factor

      return function(*args, **kwargs)
    return wrapper
  return decorator


def safe_retry(times=3, sleep_time=1, max_sleep_time=8, backoff_factor=1, err_class=Exception, return_on_fail=None):
  """
  Retry decorator for improved robustness of functions. Instead of error generation on the last try, will return
  return_on_fail value.
  :param times: Number of times to attempt to call the function.
  :param sleep_time: Initial sleep time between attempts
  :param backoff_factor: After every failed attempt, multiple the previous sleep time by this factor.
  :param err_class: Exception class to handle
  :param return_on_fail value to return on the last try
  :return: Returns the output of the wrapped function.
  """
  def decorator(function):
    def wrapper(*args, **kwargs):
      _times = times
      _sleep_time = sleep_time
      _backoff_factor = backoff_factor
      _err_class = err_class
      _return_on_fail = return_on_fail

      while _times > 1:
        _times -= 1
        try:
          return function(*args, **kwargs)
        except _err_class, err:
          Logger.info("Will retry %d time(s), caught exception: %s. Sleeping for %d sec(s)" % (_times, str(err), _sleep_time))
          time.sleep(_sleep_time)
        if(_sleep_time * _backoff_factor <= max_sleep_time):
          _sleep_time *= _backoff_factor

      try:
        return function(*args, **kwargs)
      except _err_class, err:
        Logger.error(str(err))
        return _return_on_fail

    return wrapper
  return decorator
