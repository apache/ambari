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

"""
from resource_management import Script
from resource_management.core import Logger


def configure_for_plugin(command_data_file):
    import json
    savedConfig = Script.get_config()
    if savedConfig['hostLevelParams'].get('custom_command', '') == 'RESTART':
        try:
            with open(command_data_file) as f:
                pass
                Script.config = json.load(f)
                Script.config['hostLevelParams']['package_list'] = \
                    "[{\"name\":\"atlas-metadata*-hive-plugin\"}]"

        except IOError:
            Logger.error("Can not read json file with command parameters: ")

    return savedConfig

