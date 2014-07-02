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


def help_show(self):
    print textwrap.dedent("""
    Usage:
        > show clusters     list clusters
        > show hosts        list hosts
        > show stacks       list stacks
        > show services     list services
        > show requests     list previous requests
        > show blueprints   list blueprints
    """)


def do_show(self, option):
    """
    Usage:
        > show clusters     list clusters
        > show hosts        list hosts
        > show stacks       list stacks
        > show services     list services
        > show requests     list previous requests
        > show blueprints   list blueprints
    """
    headers = []
    rows = []
    options = [
        "clusters",
        "stacks",
        "status",
        "services",
        "hosts",
        "requests",
        "blueprints"]

    if not option:
        self.help_show()
        return

    if option not in options:
        self.default(option)
        return

    client = self.global_shell_config['client']
    clustername = self.global_shell_config['clustername']
    # show clusters
    if option == "clusters":
        "Display list of clusters on system"
        headers = ["CLUSTER NAME"]
        clusters = client.get_all_clusters()
        for cluster in clusters:
            rows.append([cluster.cluster_name])

    # show clusters
    if option == "stacks":
        "Display list of stacks on system"
        headers = ["STACK NAME"]
        stacks = client.get_stacks(True)
        for stack in stacks:
            rows.append([stack.stack_version])

    if option == "blueprints":
        "Display list of blueprints on system"
        headers = ["BLUEPRINT NAME", "VERSION"]
        blues = client.get_blueprint()
        for b in blues:
            rows.append([b.blueprint_name, b.stack_version])

    if option == "status":
        "Display list of stacks on system"
        headers = ["HOST_NAME", "ROLE", "STATUS"]
        if not clustername:
            print("Error! No cluster currently selected")
            return
        else:
            tasks = client.get_task_status(clustername, 2)
            for task in tasks:
                rows.append([task.host_name, task.role, task.status])

    # show hosts
    if option == "hosts":
        "Display a list of hosts avaiable on the system"
        headers = ["HOSTNAME", "IP ADDRESS"]
        if not clustername:
            for host in client.get_all_hosts():
                rows.append([host.host_name, host.ip])
        else:
            c = client.get_cluster(clustername)
            for host in c.get_all_hosts():
                rows.append([host.host_name, host.ip])

    if option == "requests":
        headers = ["REQUEST-ID", "STATUS"]
        if not clustername:
            print("Error! No cluster currently selected")
            return
        else:
            c = client.get_cluster(clustername)
            for req in client.get_requests(clustername):
                rows.append([req.id, req.request_status])

    # show services
    if option == "services":
        "Show list of services on the cluster"
        headers = ["SERVICE", "STATUS"]

        if not clustername:
            print("Error! No cluster currently selected")
            return
        else:
            c = client.get_cluster(clustername)
            for service in c.get_all_services():
                rows.append([service.service_name, service.state])

    self.generate_output(headers, rows)


def complete_show(self, pattern, line, start_index, end_index):
    show_commands = [
        "clusters",
        "hosts",
        "services",
        "stacks",
        "blueprints",
        "requests"]
    if pattern:
        return [c for c in show_commands if c.startswith(pattern)]
    else:
        return show_commands

if __name__ == '__main__':
    do_show(None, None)
