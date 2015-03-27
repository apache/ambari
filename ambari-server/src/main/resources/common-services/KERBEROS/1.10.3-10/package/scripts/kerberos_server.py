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

from kerberos_common import *
from ambari_commons.os_check import OSCheck

class KerberosServer(KerberosScript):
  @staticmethod
  def write_kadm5_acl():
    import params

    Directory(params.kadm5_acl_dir,
              owner='root',
              recursive=True,
              group='root',
              mode=0700
    )

    if (params.kadm5_acl_template is None) or not params.kadm5_acl_template.strip():
      content = Template('kadm5_acl.j2')
    else:
      content = InlineTemplate(params.kadm5_acl_template)

    File(params.kadm5_acl_path,
         content=content,
         owner='root',
         group='root',
         mode=0600
    )

  @staticmethod
  def write_kdc_conf():
    import params

    Directory(params.kdc_conf_dir,
              owner='root',
              recursive=True,
              group='root',
              mode=0700
    )

    if (params.kdc_conf_template is None) or not params.kdc_conf_template.strip():
      content = Template('kdc_conf.j2')
    else:
      content = InlineTemplate(params.kdc_conf_template)

    File(params.kdc_conf_path,
         content=content,
         owner='root',
         group='root',
         mode=0600
    )

  def install(self, env):
    import params

    self.install_packages(env)
    self.configure(env)

    # Create the Kerberos database (only on install, for now)
    Execute(
      "%s create -s -P '%s'" % (params.kdb5_util_path, KerberosScript.create_random_password()))

    # Create or update the administrator account
    KerberosScript.create_or_update_administrator_identity()


  def start(self, env):
    # Attempt to reconfigure the service before starting
    self.configure(env)

    # Create or update the administrator account
    KerberosScript.create_or_update_administrator_identity()

    if OSCheck.is_suse_family():
      Execute('rckadmind start')
      Execute('rckrb5kdc start')
    elif OSCheck.is_ubuntu_family():
      Execute('service krb5-kdc start')
      Execute('service krb5-admin-server start')
    else:
      Execute('service krb5kdc start')
      Execute('service kadmin start')

  def stop(self, env):
    if OSCheck.is_suse_family():
      Execute('rckadmind stop')
      Execute('rckrb5kdc stop')
    elif OSCheck.is_ubuntu_family():
      Execute('service krb5-kdc stop')
      Execute('service krb5-admin-server stop')
    else:
      Execute('service krb5kdc stop')
      Execute('service kadmin stop')


  def configure(self, env):
    import params
    env.set_params(params)

    KerberosServer.write_krb5_conf()
    KerberosServer.write_kdc_conf()
    KerberosServer.write_kadm5_acl()

  def status(self, env):
    import params

    if OSCheck.is_suse_family():
      try:
        Execute('checkproc `which krb5kdc`')
        Execute('checkproc `which kadmind`')
      except Fail as ex:
        raise ComponentIsNotRunning()

    elif OSCheck.is_ubuntu_family():
      check_process_status(params.kdamin_pid_path)
      check_process_status(params.krb5kdc_pid_path)

    else:
      check_process_status(params.kdamin_pid_path)
      check_process_status(params.krb5kdc_pid_path)


if __name__ == "__main__":
  KerberosServer().execute()
