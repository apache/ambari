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

from kerberos_common import *
from resource_management import *

# hashlib is supplied as of Python 2.5 as the replacement interface for md5
# and other secure hashes.  In 2.6, md5 is deprecated.  Import hashlib if
# available, avoiding a deprecation warning under 2.6.  Import md5 otherwise,
# preserving 2.4 compatibility.
try:
  import hashlib
  _md5 = hashlib.md5
except ImportError:
  import md5
  _md5 = md5.new

class KerberosServiceCheck(KerberosScript):
  def service_check(self, env):
    import params

    if ((params.smoke_test_principal is not None) and
          (params.smoke_test_keytab_file is not None) and
          os.path.isfile(params.smoke_test_keytab_file)):
      print "Performing kinit using %s" % params.smoke_test_principal

      ccache_file_name = _md5("{0}|{1}".format(params.smoke_test_principal,params.smoke_test_keytab_file)).hexdigest()
      ccache_file_path = "{0}{1}kerberos_service_check_cc_{2}".format(params.tmp_dir, os.sep, ccache_file_name)

      kinit_path_local = functions.get_kinit_path()
      kinit_command = "{0} -c {1} -kt {2} {3}".format(kinit_path_local, ccache_file_path, params.smoke_test_keytab_file, params.smoke_test_principal)

      try:
        # kinit
        Execute(kinit_command)
      finally:
        if os.path.isfile(ccache_file_path): # Since kinit might fail to write to the cache file for various reasons, an existence check should be done before cleanup
          os.remove(ccache_file_path)
    else:
      err_msg = Logger.filter_text("Failed to execute kinit test due to principal or keytab not found or available")
      raise Fail(err_msg)

if __name__ == "__main__":
  KerberosServiceCheck().execute()
