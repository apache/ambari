#!/usr/bin/env python

'''
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
'''

def terminate_thread(thread):
  """Terminates a python thread abruptly from another thread.
  
  This is consider a bad pattern to do this. 
  If possible, please consider handling stopping of the thread from inside of it
  or creating thread as a separate process (multiprocessing module).

  :param thread: a threading.Thread instance
  """
  import ctypes
  if not thread.isAlive():
      return

  exc = ctypes.py_object(SystemExit)
  res = ctypes.pythonapi.PyThreadState_SetAsyncExc(
      ctypes.c_long(thread.ident), exc)
  if res == 0:
      raise ValueError("nonexistent thread id")
  elif res > 1:
      # """if it returns a number greater than one, you're in trouble,
      # and you should call it again with exc=NULL to revert the effect"""
      ctypes.pythonapi.PyThreadState_SetAsyncExc(thread.ident, None)
      raise SystemError("PyThreadState_SetAsyncExc failed")