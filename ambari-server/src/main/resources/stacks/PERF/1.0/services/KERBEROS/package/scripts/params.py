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

from resource_management import *
from utils import get_property_value, get_unstructured_data
from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions.expect import expect

krb5_conf_dir = '/etc'
krb5_conf_file = 'krb5.conf'
krb5_conf_path = krb5_conf_dir + '/' + krb5_conf_file

if OSCheck.is_suse_family():
  kdc_conf_dir = '/var/lib/kerberos/krb5kdc'
elif OSCheck.is_ubuntu_family():
  kdc_conf_dir = '/etc/krb5kdc'
else:
  kdc_conf_dir = '/var/kerberos/krb5kdc'
kdc_conf_file = 'kdc.conf'
kdc_conf_path = kdc_conf_dir + '/' + kdc_conf_file

kadm5_acl_dir = kdc_conf_dir  # Typically kadm5.acl and kdc.conf exist in the same directory
kadm5_acl_file = 'kadm5.acl'
kadm5_acl_path = kadm5_acl_dir + '/' + kadm5_acl_file

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

configurations = None
keytab_details = None
default_group = None
kdc_server_host = None
cluster_host_info = None

hostname = config['hostname']

kdb5_util_path = 'kdb5_util'

kdamin_pid_path = '/var/run/kadmind.pid'
krb5kdc_pid_path = '/var/run/krb5kdc.pid'

smoke_test_principal = None
smoke_test_keytab_file = None

smoke_user = 'ambari-qa'

manage_identities = 'true'

artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jce_policy_zip = default("/hostLevelParams/jce_name", None) # None when jdk is already installed by user
jce_location = config['hostLevelParams']['jdk_location']
jdk_name = default("/hostLevelParams/jdk_name", None)
java_home = config['hostLevelParams']['java_home']
java_version = expect("/hostLevelParams/java_version", int)

security_enabled = config['configurations']['cluster-env']['security_enabled']

if config is not None:
  kerberos_command_params = get_property_value(config, 'kerberosCommandParams')

  cluster_host_info = get_property_value(config, 'clusterHostInfo')
  if cluster_host_info is not None:
    kdc_server_hosts = get_property_value(cluster_host_info, 'kdc_server_hosts')

    if (kdc_server_hosts is not None) and (len(kdc_server_hosts) > 0):
      kdc_server_host = kdc_server_hosts[0]

  configurations = get_property_value(config, 'configurations')
  if configurations is not None:
    cluster_env = get_property_value(configurations, 'cluster-env')

    if cluster_env is not None:
      smoke_test_principal = get_property_value(cluster_env, 'smokeuser_principal_name', None, True, None)
      smoke_test_keytab_file = get_property_value(cluster_env, 'smokeuser_keytab', None, True, None)
      smoke_user = get_property_value(cluster_env, 'smokeuser', smoke_user, True, smoke_user)

      default_group = get_property_value(cluster_env, 'user_group')

      if default_group is None:
        default_group = get_property_value(cluster_env, 'user-group')

    # ##############################################################################################
    # Get krb5.conf template data
    # ##############################################################################################
    realm = 'EXAMPLE.COM'
    domains = ''
    kdc_hosts = 'localhost'
    admin_server_host = None
    admin_principal = None
    admin_password = None
    admin_keytab = None
    test_principal = None
    test_password = None
    test_keytab = None
    test_keytab_file = None
    encryption_types = None
    manage_krb5_conf = "true"
    krb5_conf_template = None

    krb5_conf_data = get_property_value(configurations, 'krb5-conf')

    kerberos_env = get_property_value(configurations, "kerberos-env")

    if kerberos_env is not None:
      manage_identities = get_property_value(kerberos_env, "manage_identities", "true", True, "true")
      encryption_types = get_property_value(kerberos_env, "encryption_types", None, True, None)
      realm = get_property_value(kerberos_env, "realm", None, True, None)
      kdc_hosts = get_property_value(kerberos_env, 'kdc_hosts', kdc_hosts)
      admin_server_host = get_property_value(kerberos_env, 'admin_server_host', admin_server_host)

    if krb5_conf_data is not None:
      realm = get_property_value(krb5_conf_data, 'realm', realm)
      domains = get_property_value(krb5_conf_data, 'domains', domains)

      admin_principal = get_property_value(krb5_conf_data, 'admin_principal', admin_principal, True, None)
      admin_password = get_property_value(krb5_conf_data, 'admin_password', admin_password, True, None)
      admin_keytab = get_property_value(krb5_conf_data, 'admin_keytab', admin_keytab, True, None)

      test_principal = get_property_value(krb5_conf_data, 'test_principal', test_principal, True, None)
      test_password = get_property_value(krb5_conf_data, 'test_password', test_password, True, None)
      test_keytab = get_property_value(krb5_conf_data, 'test_keytab', test_keytab, True, None)
      test_keytab_file = get_property_value(krb5_conf_data, 'test_keytab_file', test_keytab_file, True, None)

      krb5_conf_template = get_property_value(krb5_conf_data, 'content', krb5_conf_template)
      krb5_conf_dir = get_property_value(krb5_conf_data, 'conf_dir', krb5_conf_dir)
      krb5_conf_file = get_property_value(krb5_conf_data, 'conf_file', krb5_conf_file)
      krb5_conf_path = krb5_conf_dir + '/' + krb5_conf_file

      manage_krb5_conf = get_property_value(krb5_conf_data, 'manage_krb5_conf', "true")

    # For backward compatibility, ensure that kdc_host exists. This may be needed if the krb5.conf
    # template in krb5-conf/content had not be updated during the Ambari upgrade to 2.4.0 - which
    # will happen if the template was altered from its stack-default value.
    kdc_host_parts = kdc_hosts.split(',')
    if kdc_host_parts:
      kdc_host = kdc_host_parts[0]
    else:
      kdc_host = kdc_hosts

    # ##############################################################################################
    # Get kdc.conf template data
    # ##############################################################################################
    kdcdefaults_kdc_ports = "88"
    kdcdefaults_kdc_tcp_ports = "88"

    kdc_conf_template = None

    kdc_conf_data = get_property_value(configurations, 'kdc-conf')

    if kdc_conf_data is not None:
      kdcdefaults_kdc_ports = get_property_value(kdc_conf_data, 'kdcdefaults_kdc_ports', kdcdefaults_kdc_ports)
      kdcdefaults_kdc_tcp_ports = get_property_value(kdc_conf_data, 'kdcdefaults_kdc_tcp_ports', kdcdefaults_kdc_tcp_ports)

      kdc_conf_template = get_property_value(kdc_conf_data, 'content', kdc_conf_template)
      kdc_conf_dir = get_property_value(kdc_conf_data, 'conf_dir', kdc_conf_dir)
      kdc_conf_file = get_property_value(kdc_conf_data, 'conf_file', kdc_conf_file)
      kdc_conf_path = kdc_conf_dir + '/' + kdc_conf_file

    # ##############################################################################################
    # Get kadm5.acl template data
    # ##############################################################################################
    kdcdefaults_kdc_ports = '88'
    kdcdefaults_kdc_tcp_ports = '88'

    kadm5_acl_template = None

    kadm5_acl_data = get_property_value(configurations, 'kadm5-acl')

    if kadm5_acl_data is not None:
      kadm5_acl_template = get_property_value(kadm5_acl_data, 'content', kadm5_acl_template)
      kadm5_acl_dir = get_property_value(kadm5_acl_data, 'conf_dir', kadm5_acl_dir)
      kadm5_acl_file = get_property_value(kadm5_acl_data, 'conf_file', kadm5_acl_file)
      kadm5_acl_path = kadm5_acl_dir + '/' + kadm5_acl_file

  # ################################################################################################
  # Get commandParams
  # ################################################################################################
  command_params = get_property_value(config, 'commandParams')
  if command_params is not None:
    keytab_details = get_unstructured_data(command_params, 'keytab')

    if manage_identities:
      smoke_test_principal = get_property_value(command_params, 'principal_name', smoke_test_principal)
      smoke_test_keytab_file = get_property_value(command_params, 'keytab_file', smoke_test_keytab_file)
