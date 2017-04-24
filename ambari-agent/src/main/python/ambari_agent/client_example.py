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

# TODO: remove this

import time
import ambari_stomp
from ambari_stomp.adapter import websocket
import base64

correlationId = 0

def get_headers():
  global correlationId
  correlationId += 1
  headers = {
    "correlationId": correlationId
  }
  return headers

class MyListener(ambari_stomp.ConnectionListener):
  def on_message(self, headers, message):
    print('MyListener:\nreceived a message "{0}"\n'.format(message))
    global read_messages
    print headers
    print message
    read_messages.append({'id': headers['message-id'], 'subscription':headers['subscription']})


class MyStatsListener(ambari_stomp.StatsListener):
  def on_disconnected(self):
    super(MyStatsListener, self).on_disconnected()
    print('MyStatsListener:\n{0}\n'.format(self))

read_messages = []

#conn = websocket.WsConnection('ws://gc6401:8080/api/stomp/v1')
#conn.transport.ws.extra_headers = [("Authorization", "Basic " + base64.b64encode('admin:admin'))]
conn = websocket.WsConnection('wss://gc6401:8441/agent/stomp/v1')
conn.set_listener('my_listener', MyListener())
conn.set_listener('stats_listener', MyStatsListener())
conn.start()

conn.connect(wait=True, headers=get_headers())

conn.subscribe(destination='/user/', id='sub-0', ack='client-individual')

#conn.send(body="", destination='/test/time', headers=get_headers())
conn.send(body="{}", destination='/register', headers=get_headers())
time.sleep(1)
for message in read_messages:
  conn.ack(message['id'], message['subscription'])

conn.disconnect()