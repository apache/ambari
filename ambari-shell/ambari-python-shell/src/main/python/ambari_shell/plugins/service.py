#!/usr/bin/env python
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


import logging
import textwrap


LOG = logging.getLogger(__name__)


def do_start_service(self, service):
    if not service:
        self.help_stop_service()
        return None
    clustername = self.global_shell_config['clustername']
    if not clustername:
        print("Error! No cluster currently selected")
        return None

    if self.t_service_action(service=service, action="start"):
        print("%s is being started" % (service))
    else:
        print("Error! cannot start service")
    return


def do_stop_service(self, service):
    if not service:
        self.help_start_service()
        return None

    clustername = self.global_shell_config['clustername']
    if not clustername:
        print("Error! No cluster currently selected")
        return None

    if self.t_service_action(service=service, action="stop"):
        print("%s is being stopped" % (service))
    else:
        print("Error! cannot stop service")
        return


def help_stop_service(self):
    print textwrap.dedent("""
    Usage:
        > stop_service <service_name>
    """)


def help_start_service(self):
    print textwrap.dedent("""
    Usage:
        > start_service <service_name>
    """)


def complete_start_service(self, pattern, line, start_index, end_index):
    return self.services_complete(pattern, line, start_index, end_index)


def complete_stop_service(self, pattern, line, start_index, end_index):
    client = self.global_shell_config['client']
    clustername = self.global_shell_config['clustername']
    if not clustername:
        return None
    else:
        if not self.SERVICES:
            services = [
                s.service_name for s in client.get_cluster(clustername).get_all_services()]
            self.SERVICES = services

    if pattern:
        return [s for s in self.SERVICES if s.startswith(pattern)]
    else:
        return self.SERVICES


def services_complete(self, pattern, line, start_index, end_index, append=[]):
    client = self.global_shell_config['client']
    clustername = self.global_shell_config['clustername']
    if not clustername:
        return None
    else:
        if not self.SERVICES:
            services = [
                s.service_name for s in client.get_cluster(clustername).get_all_services()]
            self.SERVICES = services

    if pattern:
        return [s for s in self.SERVICES + append if s.startswith(pattern)]
    else:
        return self.SERVICES + append


def t_service_action(self, service, action):
    try:
        client = self.global_shell_config['client']
        cluster = self.global_shell_config['clustername']
        service = client.get_cluster(cluster).get_service(service)
    except Exception:
        print("Service not found")
        return None

    if action == "start":
        service.start(message='started by Ambari python CLI')
    if action == "stop":
        service.stop(message='stopped by Ambari python CLI')
    return True
