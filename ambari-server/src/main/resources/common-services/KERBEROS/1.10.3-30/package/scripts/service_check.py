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

Ambari Agent

"""

import hashlib
import os

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.libraries import functions
from resource_management.libraries.functions import default
from resource_management.libraries.script.script import Script

# The hash algorithm to use to generate digests/hashes
HASH_ALGORITHM = hashlib.sha224

class KerberosServiceCheck(Script):
  def service_check(self, env):
    import params

    # If Ambari IS managing Kerberos identities (kerberos-env/manage_identities = true), it is
    # expected that a (smoke) test principal and its associated keytab file is available for use
    # **  If not available, this service check will fail
    # **  If available, this service check will execute
    #
    # If Ambari IS NOT managing Kerberos identities (kerberos-env/manage_identities = false), the
    # smoke test principal and its associated keytab file may not be available
    # **  If not available, this service check will execute
    # **  If available, this service check will execute

    if ((params.smoke_test_principal is not None) and
          (params.smoke_test_keytab_file is not None) and
          os.path.isfile(params.smoke_test_keytab_file)):
      print "Performing kinit using %s" % params.smoke_test_principal

      ccache_file_name = HASH_ALGORITHM("{0}|{1}".format(params.smoke_test_principal, params.smoke_test_keytab_file)).hexdigest()
      ccache_file_path = "{0}{1}kerberos_service_check_cc_{2}".format(params.tmp_dir, os.sep, ccache_file_name)

      kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
      kinit_command = "{0} -c {1} -kt {2} {3}".format(kinit_path_local, ccache_file_path, params.smoke_test_keytab_file,
                                                      params.smoke_test_principal)

      try:
        # kinit
        Execute(kinit_command,
                user=params.smoke_user,
                wait_for_finish=True,
                tries=9,
                try_sleep=15
                )
      finally:
        File(ccache_file_path,
             # Since kinit might fail to write to the cache file for various reasons, an existence check should be done before cleanup
             action="delete",
             )
    elif params.manage_identities:
      err_msg = Logger.filter_text("Failed to execute kinit test due to principal or keytab not found or available")
      raise Fail(err_msg)
    else:
      # Ambari is not managing identities so if the smoke user does not exist, indicate why....
      print "Skipping this service check since Ambari is not managing Kerberos identities and the smoke user " \
            "credentials are not available. To execute this service check, the smoke user principal name " \
            "and keytab file location must be set in the cluster_env and the smoke user's keytab file must" \
            "exist in the configured location."


if __name__ == "__main__":
  KerberosServiceCheck().execute()
