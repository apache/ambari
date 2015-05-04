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

import os
from resource_management import *

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

def _ldap_common():
    import params

    File(os.path.join(params.knox_conf_dir, 'ldap-log4j.properties'),
         mode=params.mode,
         group=params.knox_group,
         owner=params.knox_user,
         content=params.ldap_log4j
    )

    File(os.path.join(params.knox_conf_dir, 'users.ldif'),
         mode=params.mode,
         group=params.knox_group,
         owner=params.knox_user,
         content=params.users_ldif
    )

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def ldap():
  import params

  # Manually overriding service logon user & password set by the installation package
  ServiceConfig(params.knox_ldap_win_service_name,
                action="change_user",
                username = params.knox_user,
                password = Script.get_password(params.knox_user))

  _ldap_common()

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def ldap():
  _ldap_common()