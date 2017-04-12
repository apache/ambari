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


COMMANDS_TOPIC = '/events/commands'
CONFIGURATIONS_TOPIC = '/events/configurations'
METADATA_TOPIC = '/events/metadata'
TOPOLOGIES_TOPIC = '/events/topologies'
SERVER_RESPONSES_TOPIC = '/user'

TOPICS_TO_SUBSCRIBE = [SERVER_RESPONSES_TOPIC, COMMANDS_TOPIC, CONFIGURATIONS_TOPIC, METADATA_TOPIC, TOPOLOGIES_TOPIC]

HEARTBEAT_ENDPOINT = '/agent/heartbeat'
REGISTRATION_ENDPOINT = '/agent/registration'

AGENT_TMP_DIR = "/var/lib/ambari-agent/tmp"
CORRELATION_ID_STRING = 'correlationId'