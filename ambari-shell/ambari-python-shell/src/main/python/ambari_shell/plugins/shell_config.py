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


def do_get_shell_config(self, config_name):
    rows = []
    headers = ["KEY", "VAlUE"]
    if not config_name:
        for i in self.global_shell_config.items():
            rows.append([i[0], i[1]])
    else:
        if config_name in self.global_shell_config.keys():
            rows.append([config_name, self.global_shell_config[config_name]])

    self.generate_output(headers, rows)


def do_set_shell_config(self, config=None):
    kv = config.split(" ")
    if len(kv) != 2:
        self.help_set_shell_config()
        return
    config_name = kv[0]
    config_value = kv[1]
    if config_name in self.global_shell_config.keys():
        self.global_shell_config[config_name] = config_value

    self.do_get_shell_config(config_name=None)


def help_get_shell_config(self):
    print textwrap.dedent("""
    Usage:
        > get_shell_config <config_name>     get all shell config
    """)


def help_set_shell_config(self):
    print textwrap.dedent("""
    Usage:
        > set_shell_config <config_name> <config_value>     sets shell config
    """)


def complete_get_shell_config(self, pattern, line, start_index, end_index):
    if pattern:
        return [
            c for c in self.global_shell_config.keys() if c.startswith(pattern)]
    else:
        return self.CLUSTERS


def complete_set_shell_config(self, pattern, line, start_index, end_index):
    if pattern:
        return [
            c for c in self.global_shell_config.keys() if c.startswith(pattern)]
    else:
        return self.CLUSTERS
