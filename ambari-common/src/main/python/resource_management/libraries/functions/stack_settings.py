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

from resource_management.core.logger import Logger
from resource_management.libraries.functions import settings

# Stack related configs from stack's stack_settings.json
STACK_NAME_SETTING = "stack_name"
STACK_TOOLS_SETTING = "stack_tools"
STACK_FEATURES_SETTING = "stack_features"
STACK_PACKAGES_SETTING = "stack_packages"
STACK_ROOT_SETTING = "stack_root"

STACK_SELECT_SETTING = "stack_select"


def get_stack_setting_entries(setting_names):
    """
    Retrieves the passed-in stack setting entr(y/ies) and their values as a map.

    :argument setting_names: A set/frozenset/tuple/list of settings passed-in for retrieval
    :return map of setting_names and their respective values or None.

    :type setting_names: set
    """
    return settings.get_setting_type_entries(settings.STACK_SETTINGS_TYPE, setting_names)

def get_stack_setting_value(setting_name):
    """
    Retrieves the passed-in stack setting entry's value.

    :argument setting_name: stack setting to be retrieved.
    :return value of the passed-in 'setting_name'

    :type setting_name: string.
    """
    return settings.get_setting_value(settings.STACK_SETTINGS_TYPE, setting_name)

def get_stack_name():
    """
    Retrieves the stack name.

    :return: stack name as string.
    """
    return settings.get_setting_value(settings.STACK_SETTINGS_TYPE, STACK_NAME_SETTING)


def get_stack_root():
    """
    Retrieves the stack root.

    :return: stack root as string
    """
    return settings.get_setting_value(settings.STACK_SETTINGS_TYPE, STACK_ROOT_SETTING)