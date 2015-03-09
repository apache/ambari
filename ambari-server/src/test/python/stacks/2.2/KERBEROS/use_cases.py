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

import json

krb5_conf_template = \
  '[libdefaults]\n' \
  '  renew_lifetime = 7d\n' \
  '  forwardable = true\n' \
  '  realm = {{realm|upper()}}\n' \
  '  ticket_lifetime = 24h\n' \
  '  dns_lookup_realm = false\n' \
  '  dns_lookup_kdc = false\n' \
  '\n' \
  '{% if domains %}\n' \
  '[domain_realm]\n' \
  '{% for domain in domains %}\n' \
  '  {{domain}} = {{realm|upper()}}\n' \
  '{% endfor %}\n' \
  '{% endif %}\n' \
  '\n' \
  '[logging]\n' \
  '  default = FILE:/var/log/krb5kdc.log\n' \
  '  admin_server = FILE:/var/log/kadmind.log\n' \
  '  kdc = FILE:/var/log/krb5kdc.log\n' \
  '\n' \
  '[realms]\n' \
  '  {{realm}} = {\n' \
  '    admin_server = {{admin_server_host|default(kdc_host, True)}}\n' \
  '    kdc = {{kdc_host}}\n' \
  '}\n' \
  '\n' \
  '{# Append additional realm declarations should be placed below #}\n'

kdc_conf_template = \
  '[kdcdefaults]\n' \
  '  kdc_ports = {{kdcdefaults_kdc_ports}}\n' \
  '  kdc_tcp_ports = {{kdcdefaults_kdc_tcp_ports}}\n' \
  '\n' \
  '[realms]\n' \
  '  {{realm}} = {\n' \
  '    acl_file = {{kadm5_acl_path}}\n' \
  '    dict_file = /usr/share/dict/words\n' \
  '    admin_keytab = {{kadm5_acl_dir}}/kadm5.keytab\n' \
  '    supported_enctypes = {{encryption_types}}\n' \
  '}\n' \
  '\n' \
  '{# Append additional realm declarations should be placed below #}\n'

kadm5_acl_template = '*/admin@{{realm}}	*'


def get_manged_kdc_use_case():
  config_file = "stacks/2.2/configs/default.json"
  with open(config_file, "r") as f:
    json_data = json.load(f)

  json_data['clusterHostInfo']['kdc_server_hosts'] = ['c6401.ambari.apache.org']
  json_data['configurations']['kerberos-env'] = {
    'kdc_type': 'mit-kdc',
    'kdc_host': 'c6401.ambari.apache.org'
  }
  json_data['configurations']['krb5-conf'] = {
    'realm': 'MANAGED_REALM.COM',
    'admin_principal': "admin/admin",
    'admin_password': "hadoop"
  }
  json_data['configurations']['kerberos-env'] = { 'encryption_types' : 'aes256-cts-hmac-sha1-96'}

  return json_data


def get_unmanged_kdc_use_case():
  config_file = "stacks/2.2/configs/default.json"
  with open(config_file, "r") as f:
    json_data = json.load(f)

  json_data['configurations']['kerberos-env'] = {
    'kdc_host': 'ad.oscorp_industries.com',
    'kdc_type': 'mit-kdc'
  }
  json_data['configurations']['krb5-conf'] = {
    'conf_dir': '/tmp',
    'conf_file': 'krb5_unmanaged.conf',
    'content': krb5_conf_template,
    'realm': 'OSCORPINDUSTRIES.COM',
    'admin_principal': "admin/admin",
    'admin_password': "hadoop"
  }
  json_data['configurations']['kdc-conf'] = {
    'content': kdc_conf_template
  }
  json_data['configurations']['kadm5-acl'] = {
    'content': kadm5_acl_template
  }
  json_data['configurations']['kerberos-env'] = { 'encryption_types' : 'aes256-cts-hmac-sha1-96'}


  return json_data

def get_unmanged_krb5conf_use_case():
  config_file = "stacks/2.2/configs/default.json"
  with open(config_file, "r") as f:
    json_data = json.load(f)

  json_data['clusterHostInfo']['kdc_server_hosts'] = ['c6401.ambari.apache.org']
  json_data['configurations']['krb5-conf'] = {
    'realm': 'MANAGED_REALM.COM',
    'kdc_type': 'mit-kdc',
    'admin_principal': "admin/admin",
    'admin_password': "hadoop",
    'manage_krb5_conf': "false"
  }
  json_data['configurations']['kerberos-env'] = {
    'kdc_host': 'c6401.ambari.apache.org',
    'encryption_types' : 'aes256-cts-hmac-sha1-96'
  }

  return json_data

def get_unmanged_ad_use_case():
  config_file = "stacks/2.2/configs/default.json"
  with open(config_file, "r") as f:
    json_data = json.load(f)

  json_data['configurations']['kerberos-env'] = {
    'kdc_host': 'ad.oscorp_industries.com',
    'kdc_type': 'active-directory',
  }
  json_data['configurations']['krb5-conf'] = {
    'conf_dir': '/tmp',
    'conf_file': 'krb5_ad.conf',
    'content': krb5_conf_template,
    'realm': 'OSCORPINDUSTRIES.COM',
    'admin_principal': "admin/admin",
    'admin_password': "hadoop"
  }
  json_data['configurations']['kdc-conf'] = {
    'content': kdc_conf_template
  }
  json_data['configurations']['kadm5-acl'] = {
    'content': kadm5_acl_template
  }
  json_data['configurations']['kerberos-env'] = { 'encryption_types' : 'aes256-cts-hmac-sha1-96'}
  return json_data

def get_cross_realm_use_case():
  config_file = "stacks/2.2/configs/default.json"
  with open(config_file, "r") as f:
    json_data = json.load(f)

  _krb5_conf_template = krb5_conf_template + \
                        '' \
                        '  OSCORPINDUSTRIES.COM = {\n' \
                        '    kdc = ad.oscorp_industries.com\n' \
                        '}\n'

  json_data['clusterHostInfo']['kdc_server_hosts'] = ['c6401.ambari.apache.org']
  json_data['configurations']['kerberos-env'] = {
    'kdc_host': 'c6401.ambari.apache.org',
    'kdc_type': 'mit-kdc'
  }
  json_data['configurations']['krb5-conf'] = {
    'content': _krb5_conf_template,
    'realm': 'MANAGED_REALM.COM',
    'admin_principal': "admin/admin",
    'admin_password': "hadoop"
  }
  json_data['configurations']['kdc-conf'] = {
    'content': kdc_conf_template
  }
  json_data['configurations']['kadm5-acl'] = {
    'content': kadm5_acl_template
  }
  json_data['configurations']['kerberos-env'] = { 'encryption_types' : 'aes256-cts-hmac-sha1-96'}
  return json_data

def get_value(dictionary, path, nullValue=None):
  if (dictionary is None) or (path is None) or (len(path) == 0):
    return nullValue
  else:
    name = path.pop()

    if name in dictionary:
      value = dictionary[name]

      if len(path) == 0:
        return value
      else:
        return get_value(value, path, nullValue)
    else:
      return nullValue

def get_krb5_conf_path(json_data):
  return get_value(json_data,
                   ['conf_path', 'krb5-conf', 'configurations'],
                   get_krb5_conf_dir(json_data) + '/' + get_krb5_conf_file(json_data))

def get_krb5_conf_file(json_data):
  return get_value(json_data, ['conf_file', 'krb5-conf', 'configurations'], 'krb5.conf')

def get_krb5_conf_dir(json_data):
  return get_value(json_data, ['conf_dir', 'krb5-conf', 'configurations'], '/etc')

def get_krb5_conf_template(json_data):
  return get_value(json_data, ['content', 'krb5-conf', 'configurations'], None)

def get_kdc_conf_path(json_data):
  return get_value(json_data,
                   ['conf_path', 'kdc-conf', 'configurations'],
                   get_kdc_conf_dir(json_data) + '/' + get_kdc_conf_file(json_data))

def get_kdc_conf_file(json_data):
  return get_value(json_data, ['conf_file', 'kdc-conf', 'configurations'], 'kdc.conf')

def get_kdc_conf_dir(json_data):
  return get_value(json_data, ['conf_dir', 'kdc-conf', 'configurations'],
                   '/var/lib/kerberos/krb5kdc')

def get_kdc_conf_template(json_data):
  return get_value(json_data, ['content', 'kdc-conf', 'configurations'], None)

def get_kadm5_acl_path(json_data):
  return get_value(json_data,
                   ['conf_path', 'kadm5-acl', 'configurations'],
                   get_kadm5_acl_dir(json_data) + '/' + get_kadm5_acl_file(json_data))

def get_kadm5_acl_file(json_data):
  return get_value(json_data, ['conf_file', 'kadm5-acl', 'configurations'], 'kadm5.acl')

def get_kadm5_acl_dir(json_data):
  return get_value(json_data, ['conf_dir', 'kadm5-acl', 'configurations'],
                   '/var/lib/kerberos/krb5kdc')

def get_kadm5_acl_template(json_data):
  return get_value(json_data, ['content', 'kadm5-acl', 'configurations'], None)
