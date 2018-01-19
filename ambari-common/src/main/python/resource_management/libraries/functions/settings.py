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

# Setting types currently supported.
STACK_SETTINGS_TYPE = "/stackSettings"
CLUSTER_SETTINGS_TYPE = "/clusterSettings"


def get_setting_type_entries(setting_type, setting_names=None):
    """
    Retrieves the passed-in settingType's settings entr(y/ies) and their values as a map.
    Caters to Setting types : '/stackSettings' and '/clusterSettings'.

    If 'setting_names' is passed-in as None, all the settings names and their corresponding values
    will be returned as map.
    If 'setting_names' is passed-in is empty : {} will be returned.

    :argument setting_type: Setting type
    :argument setting_names: A set/frozenset/tuple/list of settings passed-in for retrieval
    :return map of setting_names and their respective values or None.

    :type setting_names: set
    """
    from resource_management.libraries.functions.default import default

    Logger.info("In get_setting_type_entries(). Passed-in settings type : {0}, setting(s) : {1}".format(setting_type, setting_names))

    if not is_setting_type_supported(setting_type):
        Logger.error("Does not support retrieving settings for settings_type : {0}".format(setting_type))
        return None

    settings = default(setting_type, None)

    if settings is None:
        Logger.info("Couldn't retrieve '"+setting_type+"'.")
        return None

    if setting_names is None: # Return all settings
        return settings

    if not isinstance(setting_names, (set, frozenset, tuple, list)):
        Logger.error("'setting_names' type expected to be either a : set, frozenset, tuple, or list. "
                     "Passed-in type is : {0}".format(type(setting_names)))
        return None

    elif len(setting_names) == 0:
        Logger.error("Passed-in settings set is EMPTY")
        return None
    else:
        result = dict((setting, settings[setting]) for setting in setting_names if setting in settings)
        if not result:
            Logger.error("Passed-in setting(s) in set not present.")
            return {}
        else:
            return result

def get_setting_value(setting_type, setting_name):
    """
    Retrieves the passed-in settingType's setting entry's value.

    :argument setting_type: Setting type
    :argument setting_name: setting name to be retrieved.
    :return value of the passed-in 'setting_name'

    :type setting_name: string.
    """
    from resource_management.libraries.functions.default import default

    Logger.info("In get_setting_value(). Passed-in settings type : {0}, setting(s) : {1}".format(setting_type, setting_name))

    if not is_setting_type_supported(setting_type):
        Logger.error("Does not support retrieving settings for settings_type : {0}".format(setting_type))
        return None

    if setting_name is None:
        return None

    settings = default(setting_type, None)

    if settings is None:
        Logger.info("Couldn't retrieve '"+setting_type+"'.")
        return None

    return settings.get(setting_name)


def is_setting_type_supported(setting_type):
    """
    Checks if the passed in setting type is supported or not.
    :argument setting_type: Setting type
    :return: True or False
    """
    return setting_type in (STACK_SETTINGS_TYPE, CLUSTER_SETTINGS_TYPE)
