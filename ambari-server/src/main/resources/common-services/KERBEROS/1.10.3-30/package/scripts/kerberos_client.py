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
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.core.exceptions import ClientComponentHasNoStatus

class KerberosClient(KerberosScript):
  def install(self, env):
    install_packages = default('/configurations/kerberos-env/install_packages', "true")
    if install_packages:
      self.install_packages(env)
    else:
      print "Kerberos client packages are not being installed, manual installation is required."

    self.configure(env)


  def configure(self, env):
    import params
    env.set_params(params)
    if params.manage_krb5_conf:
      self.write_krb5_conf()
    #delete krb cache to prevent using old krb tickets on fresh kerberos setup
    self.clear_tmp_cache()

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def set_keytab(self, env):
    self.write_keytab_file()

  def remove_keytab(self, env):
    self.delete_keytab_file()

  def check_keytabs(self, env):
    self.find_missing_keytabs()

if __name__ == "__main__":
  KerberosClient().execute()
