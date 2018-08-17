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

__all__ = ["ClusterSettings"]

class ClusterSettings(object):
  """
  This class maps to "/clusterSettings" in command.json which includes cluster setting information of a cluster
  """

  def __init__(self, clusterSettings):
    self.__cluster_settings = clusterSettings

  def __get_value(self, key):
    """
    Get corresponding value from the key
    :param key:
    :return: value if key exist else None
    """
    return self.__cluster_settings.get(key)

  def is_cluster_security_enabled(self):
    """
    Check cluster security enabled or not
    :return: "True" or "False" string
    """
    security_enabled = self.__get_value("security_enabled")
    return True if security_enabled.lower() == "true" else False

  def get_recovery_max_count(self):
    """
    Retrieve cluster recovery count
    :return: String, need to convert to int
    """
    return int(self.__get_value("recovery_max_count"))

  def check_recovery_enabled(self):
    """
    Check if the cluster can be enabled or not
    :return: "True" or "False" string
    """
    recovery_enabled =  self.__get_value("recovery_enabled")
    return True if recovery_enabled.lower() == "true" else False

  def get_recovery_type(self):
    """
    Retrieve cluster recovery type
    :return: recovery type, i.e "AUTO_START"
    """
    return self.__get_value("recovery_type")

  def get_kerberos_domain(self):
    """
    Retrieve kerberos domain
    :return: String as kerberos domain
    """
    return self.__get_value("kerberos_domain")

  def get_smokeuser(self):
    """
    Retrieve smokeuser
    :return: smkeuser string
    """
    return self.__get_value("smokeuser")

  def get_user_group(self):
    """
    Retrieve cluster usergroup
    :return: usergroup string
    """
    return self.__get_value("user_group")

  def get_repo_suse_rhel_template(self):
    """
    Retrieve template of suse and rhel repo
    :return: template string
    """
    return self.__get_value("repo_suse_rhel_template")

  def get_repo_ubuntu_template(self):
    """
    Retrieve template of ubuntu repo
    :return: template string
    """
    return self.__get_value("repo_ubuntu_template")

  def check_override_uid(self):
    """
    Check if override_uid is true or false
    :return: "true" or "false" string
    """
    override_uid =  self.__get_value("override_uid")
    return True if override_uid.lower() == "true" else False

  def check_sysprep_skip_copy_fast_jar_hdfs(self):
    """
    Check sysprep_skip_copy_fast_jar_hdfs is true or false
    :return: "true" or "false" string
    """
    skip = self.__get_value("sysprep_skip_copy_fast_jar_hdfs")
    return True if skip.lower() == "true" else False

  def check_sysprep_skip_setup_jce(self):
    skip = self.__get_value("sysprep_skip_setup_jce")
    return True if skip.lower() == "true" else False

  def check_ignore_groupsusers_create(self):
    ignored = self.__get_value("ignore_groupsusers_create")
    return True if ignored.lower() == "true" else False