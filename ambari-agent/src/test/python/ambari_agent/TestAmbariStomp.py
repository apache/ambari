#!/usr/bin/env python3

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

from BaseStompServerTestCase import BaseStompServerTestCase
import ambari_stomp

class TestAmbariStomp(BaseStompServerTestCase):
  def test_ambari_stomp(self):
    conn = ambari_stomp.Connection10([('127.0.0.1', 21613)])
    conn.set_listener('', MyListener())
    conn.start()
    try:
      conn.connect("test", "test", wait=False)
      conn.subscribe(destination='/queue/test', id=1, ack='auto')
      conn.send(body='some text', destination='/queue/test', headers={"test": 'aaa', "test2": "bbb"})
    except AttributeError as e:
      raise AttributeError(e)

class MyListener(ambari_stomp.ConnectionListener):
  def on_error(self, headers, body):
    print('received an error "%s"' % body)

  def on_message(self, headers, body):
    print('received a message "%s"' % body)