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

# action commands
SETUP_ACTION = "setup"
START_ACTION = "start"
PSTART_ACTION = "pstart"
STOP_ACTION = "stop"
RESET_ACTION = "reset"
UPGRADE_ACTION = "upgrade"
UPGRADE_STACK_ACTION = "upgradestack"
REFRESH_STACK_HASH_ACTION = "refresh-stack-hash"
STATUS_ACTION = "status"
SETUP_HTTPS_ACTION = "setup-https"
LDAP_SETUP_ACTION = "setup-ldap"
SETUP_GANGLIA_HTTPS_ACTION = "setup-ganglia-https"
SETUP_NAGIOS_HTTPS_ACTION = "setup-nagios-https"
ENCRYPT_PASSWORDS_ACTION = "encrypt-passwords"
SETUP_SECURITY_ACTION = "setup-security"

ACTION_REQUIRE_RESTART = [RESET_ACTION, UPGRADE_ACTION, UPGRADE_STACK_ACTION,
                          SETUP_SECURITY_ACTION, LDAP_SETUP_ACTION]
