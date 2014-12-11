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

class KerberosServiceCheck(KerberosScript):
  def service_check(self, env):
    import params

    # First attempt to test using the smoke test user, if data is available
    if ((params.smoke_test_principal is not None) and
          (params.smoke_test_keytab_file is not None) and
          os.path.isfile(params.smoke_test_keytab_file)):
      print "Performing kinit using smoke test user: %s" % params.smoke_test_principal
      code, out = self.test_kinit({
        'principal': params.smoke_test_principal,
        'keytab_file': params.smoke_test_keytab_file
      })
      test_performed = True

    # Else if a test credentials is specified, try to test using that
    elif params.test_principal is not None:
      print "Performing kinit using test user: %s" % params.test_principal
      code, out = self.test_kinit({
        'principal': params.test_principal,
        'keytab_file': params.test_keytab_file,
        'keytab': params.test_keytab,
        'password': params.test_password
      })
      test_performed = True

    else:
      code = 0
      out = ''
      test_performed = False

    if test_performed:
      if code == 0:
        print "Test executed successfully."
      else:
        print "Test failed with error code %d: %s." % (code, out)

if __name__ == "__main__":
  KerberosServiceCheck().execute()
