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

SECURITY_ENABLED_SETTING_NAME = "security_enabled"


from resource_management.core.logger import Logger
from resource_management.libraries.functions import settings

def get_cluster_setting_entries(setting_names):
    """
    Retrieves the passed-in cluster setting entr(y/ies) and their values as a map.
    If 'setting_names' is passed-in as None : all the settings names and their corresponding values
    will be returned as map.
    If 'setting_names' is passed-in as empty set : None will be returned.

    :argument setting_names: A set/frozenset/tuple/list of settings passed-in for retrieval
    :return map of setting_names and their respective values or None.

    :type setting_names: set
    """
    return settings.get_setting_type_entries(settings.CLUSTER_SETTINGS_TYPE, setting_names)



def get_cluster_setting_value(setting_name):
    """
    Retrieves the passed-in cluster setting entry's value.

    :argument setting_name: cluster setting to be retrieved.
    :return value of the passed-in 'setting_name'

    :type setting_name: string.
    """
    return settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, setting_name)

def is_security_enabled():
    """
    Retrieves the cluster's security status.

    :return cluster's security status
    """
    return settings.get_setting_value(settings.CLUSTER_SETTINGS_TYPE, SECURITY_ENABLED_SETTING_NAME)
