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

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import default
from ambari_commons.kerberos.kerberos_common import write_krb5_conf, clear_tmp_cache, write_keytab_file, \
  delete_keytab_file, find_missing_keytabs


class KerberosClient(Script):
  def install(self, env):
    install_packages = default('/configurations/kerberos-env/install_packages', "true")
    if install_packages:
      self.install_packages(env)
    else:
      print "Kerberos client packages are not being installed, manual installation is required."

    self.configure(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    if params.manage_krb5_conf:
      write_krb5_conf(params)
    # delete krb cache to prevent using old krb tickets on fresh kerberos setup
    clear_tmp_cache()

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def set_keytab(self, env):
    import params

    def output_hook(principal, keytab_file_path):
      if principal is not None:
        curr_content = Script.structuredOut

        if "keytabs" not in curr_content:
          curr_content['keytabs'] = {}

        curr_content['keytabs'][principal.replace("_HOST", params.hostname)] = keytab_file_path

        self.put_structured_out(curr_content)

    write_keytab_file(params, output_hook)

  def remove_keytab(self, env):
    import params

    def output_hook(principal, keytab_file_path):
      if principal is not None:
        curr_content = Script.structuredOut

        if "removedKeytabs" not in curr_content:
          curr_content['removedKeytabs'] = {}
        curr_content['removedKeytabs'][principal.replace("_HOST", params.hostname)] = keytab_file_path

        self.put_structured_out(curr_content)

    delete_keytab_file(params, output_hook)

  def check_keytabs(self, env):
    import params

    def output_hook(missing_keytabs):
      curr_content = Script.structuredOut
      curr_content['missing_keytabs'] = missing_keytabs
      self.put_structured_out(curr_content)

    find_missing_keytabs(params, output_hook)


if __name__ == "__main__":
  KerberosClient().execute()
