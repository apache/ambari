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

import base64
import getpass
import os
import string
import subprocess
import sys
import tempfile
from tempfile import gettempdir

from resource_management import *
from utils import get_property_value
from ambari_commons.os_utils import remove_file
from ambari_agent import Constants
from collections import namedtuple
from resource_management.core import sudo
from resource_management.core.resources.klist import Klist

class KerberosScript(Script):
  KRB5_REALM_PROPERTIES = [
    'kdc',
    'admin_server',
    'default_domain',
    'master_kdc'
  ]

  KRB5_SECTION_NAMES = [
    'libdefaults',
    'logging',
    'realms',
    'domain_realm',
    'capaths',
    'ca_paths',
    'appdefaults',
    'plugins'
  ]

  @staticmethod
  def create_random_password():
    import random

    chars = string.digits + string.ascii_letters
    return ''.join(random.choice(chars) for x in range(13))

  @staticmethod
  def write_conf_section(output_file, section_name, section_data):
    if section_name is not None:
      output_file.write('[%s]\n' % section_name)

      if section_data is not None:
        for key, value in section_data.iteritems():
          output_file.write(" %s = %s\n" % (key, value))


  @staticmethod
  def _write_conf_realm(output_file, realm_name, realm_data):
    """ Writes out realm details

    Example:

     EXAMPLE.COM = {
      kdc = kerberos.example.com
      admin_server = kerberos.example.com
     }

    """
    if realm_name is not None:
      output_file.write(" %s = {\n" % realm_name)

      if realm_data is not None:
        for key, value in realm_data.iteritems():
          if key in KerberosScript.KRB5_REALM_PROPERTIES:
            output_file.write("  %s = %s\n" % (key, value))

      output_file.write(" }\n")

  @staticmethod
  def write_conf_realms_section(output_file, section_name, realms_data):
    if section_name is not None:
      output_file.write('[%s]\n' % section_name)

      if realms_data is not None:
        for realm, realm_data in realms_data.iteritems():
          KerberosScript._write_conf_realm(output_file, realm, realm_data)
          output_file.write('\n')

  @staticmethod
  def write_krb5_conf():
    import params

    Directory(params.krb5_conf_dir,
              owner='root',
              create_parents = True,
              group='root',
              mode=0755
    )

    content = InlineTemplate(params.krb5_conf_template)

    File(params.krb5_conf_path,
         content=content,
         owner='root',
         group='root',
         mode=0644
    )

  @staticmethod
  def invoke_kadmin(query, admin_identity=None, default_realm=None):
    """
    Executes the kadmin or kadmin.local command (depending on whether auth_identity is set or not
    and returns command result code and standard out data.

    :param query: the kadmin query to execute
    :param admin_identity: the identity for the administrative user (optional)
    :param default_realm: the default realm to assume
    :return: return_code, out
    """
    if (query is not None) and (len(query) > 0):
      auth_principal = None
      auth_keytab_file = None

      if admin_identity is not None:
        auth_principal = get_property_value(admin_identity, 'principal')

      if auth_principal is None:
        kadmin = 'kadmin.local'
        credential = ''
      else:
        kadmin = 'kadmin -p "%s"' % auth_principal

        auth_password = get_property_value(admin_identity, 'password')

        if auth_password is None:
          auth_keytab = get_property_value(admin_identity, 'keytab')

          if auth_keytab is not None:
            (fd, auth_keytab_file) = tempfile.mkstemp()
            keytab_file_path = keytab_file_path.replace("_HOST", params.hostname)
            os.write(fd, base64.b64decode(auth_keytab))
            os.close(fd)

          credential = '-k -t %s' % auth_keytab_file
        else:
          credential = '-w "%s"' % auth_password

      if (default_realm is not None) and (len(default_realm) > 0):
        realm = '-r %s' % default_realm
      else:
        realm = ''

      try:
        command = '%s %s %s -q "%s"' % (kadmin, credential, realm, query.replace('"', '\\"'))
        return shell.checked_call(command)
      except:
        raise
      finally:
        if auth_keytab_file is not None:
          os.remove(auth_keytab_file)

  @staticmethod
  def create_keytab_file(principal, path, auth_identity=None):
    success = False

    if (principal is not None) and (len(principal) > 0):
      if (auth_identity is None) or (len(auth_identity) == 0):
        norandkey = '-norandkey'
      else:
        norandkey = ''

      if (path is not None) and (len(path) > 0):
        keytab_file = '-k %s' % path
      else:
        keytab_file = ''

      try:
        result_code, output = KerberosScript.invoke_kadmin(
          'ktadd %s %s %s' % (keytab_file, norandkey, principal),
          auth_identity)

        success = (result_code == 0)
      except:
        raise Fail("Failed to create keytab for principal: %s (in %s)" % (principal, path))

    return success

  @staticmethod
  def create_keytab(principal, auth_identity=None):
    keytab = None

    (fd, temp_path) = tempfile.mkstemp()
    os.remove(temp_path)

    try:
      if KerberosScript.create_keytab_file(principal, temp_path, auth_identity):
        with open(temp_path, 'r') as f:
          keytab = base64.b64encode(f.read())
    finally:
      if os.path.isfile(temp_path):
        os.remove(temp_path)

    return keytab

  @staticmethod
  def principal_exists(identity, auth_identity=None):
    exists = False

    if identity is not None:
      principal = get_property_value(identity, 'principal')

      if (principal is not None) and (len(principal) > 0):
        try:
          result_code, output = KerberosScript.invoke_kadmin('getprinc %s' % principal,
                                                             auth_identity)
          exists = (output is not None) and (("Principal: %s" % principal) in output)
        except:
          raise Fail("Failed to determine if principal exists: %s" % principal)

    return exists

  @staticmethod
  def change_principal_password(identity, auth_identity=None):
    success = False

    if identity is not None:
      principal = get_property_value(identity, 'principal')

      if (principal is not None) and (len(principal) > 0):
        password = get_property_value(identity, 'password')

        if password is None:
          credentials = '-randkey'
        else:
          credentials = '-pw "%s"' % password

        try:
          result_code, output = KerberosScript.invoke_kadmin(
            'change_password %s %s' % (credentials, principal),
            auth_identity)

          success = (result_code == 0)
        except:
          raise Fail("Failed to create principal: %s" % principal)

    return success

  @staticmethod
  def create_principal(identity, auth_identity=None):
    success = False

    if identity is not None:
      principal = get_property_value(identity, 'principal')

      if (principal is not None) and (len(principal) > 0):
        password = get_property_value(identity, 'password')

        if password is None:
          credentials = '-randkey'
        else:
          credentials = '-pw "%s"' % password

        try:
          result_code, out = KerberosScript.invoke_kadmin(
            'addprinc %s %s' % (credentials, principal),
            auth_identity)

          success = (result_code == 0)
        except:
          raise Fail("Failed to create principal: %s" % principal)

    return success

  @staticmethod
  def clear_tmp_cache():
    tmp_dir = Constants.AGENT_TMP_DIR
    if tmp_dir is None:
      tmp_dir = gettempdir()
    curl_krb_cache_path = os.path.join(tmp_dir, "curl_krb_cache")
    Directory(curl_krb_cache_path, action="delete")

  @staticmethod
  def create_principals(identities, auth_identity=None):
    if identities is not None:
      for identity in identities:
        KerberosScript.create_principal(identity, auth_identity)

  @staticmethod
  def create_or_update_administrator_identity():
    import params

    if params.realm is not None:
      admin_identity = params.get_property_value(params.realm, 'admin_identity')

      if KerberosScript.principal_exists(admin_identity):
        KerberosScript.change_principal_password(admin_identity)
      else:
        KerberosScript.create_principal(admin_identity)

  @staticmethod
  def test_kinit(identity, user="root"):
    principal = get_property_value(identity, 'principal')
    kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
    kdestroy_path_local = functions.get_kdestroy_path(default('/configurations/kerberos-env/executable_search_paths', None))

    if principal is not None:
      keytab_file = get_property_value(identity, 'keytab_file')
      keytab = get_property_value(identity, 'keytab')
      password = get_property_value(identity, 'password')

      # If a test keytab file is available, simply use it
      if (keytab_file is not None) and (os.path.isfile(keytab_file)):
        keytab_file = keytab_file.replace("_HOST", params.hostname)
        command = '%s -k -t %s %s' % (kinit_path_local, keytab_file, principal)
        Execute(command,
          user = user,
        )
        return shell.checked_call(kdestroy_path_local)

      # If base64-encoded test keytab data is available; then decode it, write it to a temporary file
      # use it, and then remove the temporary file
      elif keytab is not None:
        (fd, test_keytab_file) = tempfile.mkstemp()
        os.write(fd, base64.b64decode(keytab))
        os.close(fd)

        try:
          command = '%s -k -t %s %s' % (kinit_path_local, test_keytab_file, principal)
          Execute(command,
            user = user,
          )
          return shell.checked_call(kdestroy_path_local)
        except:
          raise
        finally:
          if test_keytab_file is not None:
            os.remove(test_keytab_file)

      # If no keytab data is available and a password was supplied, simply use it.
      elif password is not None:
        process = subprocess.Popen([kinit_path_local, principal], stdin=subprocess.PIPE)
        stdout, stderr = process.communicate(password)
        if process.returncode:
          err_msg = Logger.filter_text("Execution of kinit returned %d. %s" % (process.returncode, stderr))
          raise Fail(err_msg)
        else:
          return shell.checked_call(kdestroy_path_local)
      else:
        return 0, ''
    else:
      return 0, ''


  def write_keytab_file(self):
    import params
    import stat

    if params.kerberos_command_params is not None:
      for item  in params.kerberos_command_params:
        keytab_content_base64 = get_property_value(item, 'keytab_content_base64')
        if (keytab_content_base64 is not None) and (len(keytab_content_base64) > 0):
          keytab_file_path = get_property_value(item, 'keytab_file_path')
          if (keytab_file_path is not None) and (len(keytab_file_path) > 0):
            keytab_file_path = keytab_file_path.replace("_HOST", params.hostname)
            head, tail = os.path.split(keytab_file_path)
            if head:
              Directory(head, create_parents = True, mode=0755, owner="root", group="root")

            owner = "root"
            group = "root"
            mode = 0

            mode |= stat.S_IREAD | stat.S_IWRITE
            mode |= stat.S_IRGRP | stat.S_IWGRP

            keytab_content = base64.b64decode(keytab_content_base64)

            # to hide content in command output
            def make_lambda(data):
              return lambda: data

            File(keytab_file_path,
                 content=make_lambda(keytab_content),
                 mode=mode,
                 owner=owner,
                 group=group)

            principal = get_property_value(item, 'principal')
            if principal is not None:
              curr_content = Script.structuredOut

              if "keytabs" not in curr_content:
                curr_content['keytabs'] = {}

              curr_content['keytabs'][principal.replace("_HOST", params.hostname)] = keytab_file_path

              self.put_structured_out(curr_content)

  def delete_keytab_file(self):
    import params

    if params.kerberos_command_params is not None:
      for item in params.kerberos_command_params:
        keytab_file_path = get_property_value(item, 'keytab_file_path')
        if (keytab_file_path is not None) and (len(keytab_file_path) > 0):
          keytab_file_path = keytab_file_path.replace("_HOST", params.hostname)
          # Delete the keytab file
          File(keytab_file_path, action="delete")

          principal = get_property_value(item, 'principal')
          if principal is not None:
            curr_content = Script.structuredOut

            if "keytabs" not in curr_content:
              curr_content['keytabs'] = {}

            curr_content['keytabs'][principal.replace("_HOST", params.hostname)] = '_REMOVED_'

            self.put_structured_out(curr_content)

  def find_missing_keytabs(self):
    import params
    missing_keytabs = MissingKeytabs.fromKerberosRecords(params.kerberos_command_params, params.hostname)
    Logger.info(str(missing_keytabs))
    curr_content = Script.structuredOut
    curr_content['missing_keytabs'] = missing_keytabs.as_dict()
    self.put_structured_out(curr_content)

class MissingKeytabs:
  class Identity(namedtuple('Identity', ['principal', 'keytab_file_path'])):
    @staticmethod
    def fromKerberosRecord(item, hostname):
      return MissingKeytabs.Identity(
        get_property_value(item, 'principal').replace("_HOST", hostname),
        get_property_value(item, 'keytab_file_path'))

    def __str__(self):
      return "Keytab: %s Principal: %s" % (self.keytab_file_path, self.principal)

  @classmethod
  def fromKerberosRecords(self, kerberos_record, hostname):
    with_missing_keytab = (each for each in kerberos_record \
                           if not self.keytab_exists(each) or not self.keytab_has_principal(each, hostname))
    return MissingKeytabs(set(MissingKeytabs.Identity.fromKerberosRecord(each, hostname) for each in with_missing_keytab))

  @staticmethod
  def keytab_exists(kerberos_record):
    return sudo.path_exists(get_property_value(kerberos_record, 'keytab_file_path'))

  @staticmethod
  def keytab_has_principal(kerberos_record, hostname):
    principal = get_property_value(kerberos_record, 'principal').replace("_HOST", hostname)
    keytab = get_property_value(kerberos_record, 'keytab_file_path')
    klist = Klist.find_in_search_path()
    return principal in klist.list_principals(keytab)

  def __init__(self, items):
    self.items = items

  def as_dict(self):
    return [each._asdict() for each in self.items]

  def __str__(self):
    return "Missing keytabs:\n%s" % ("\n".join(map(str, self.items))) if self.items else 'No missing keytabs'
