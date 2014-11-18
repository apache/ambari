#!/usr/bin/env python
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
from resource_management.libraries.functions import get_unique_id_and_date

def hcat_service_check():
    import params
    unique = get_unique_id_and_date()
    output_file = format("/apps/hive/warehouse/hcatsmoke{unique}")
    test_cmd = format("fs -test -e {output_file}")

    if params.security_enabled:
      kinit_cmd = format(
        "{kinit_path_local} -kt {smoke_user_keytab} {smokeuser}; ")
    else:
      kinit_cmd = ""

    File(format("{tmp_dir}/hcatSmoke.sh"),
         content=StaticFile("hcatSmoke.sh"),
         mode=0755
    )

    prepare_cmd = format("{kinit_cmd}env JAVA_HOME={java64_home} {tmp_dir}/hcatSmoke.sh hcatsmoke{unique} prepare")

    Execute(prepare_cmd,
            tries=3,
            user=params.smokeuser,
            try_sleep=5,
            path=['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin', params.execute_path],
            logoutput=True)

    if params.security_enabled:
      ExecuteHadoop(test_cmd,
                    user=params.hdfs_user,
                    logoutput=True,
                    conf_dir=params.hadoop_conf_dir,
                    security_enabled=params.security_enabled,
                    kinit_path_local=params.kinit_path_local,
                    keytab=params.hdfs_user_keytab,
                    principal=params.hdfs_principal_name,
                    bin_dir=params.execute_path
      )
    else:
      ExecuteHadoop(test_cmd,
                    user=params.hdfs_user,
                    logoutput=True,
                    conf_dir=params.hadoop_conf_dir,
                    security_enabled=params.security_enabled,
                    kinit_path_local=params.kinit_path_local,
                    keytab=params.hdfs_user_keytab,
                    bin_dir=params.execute_path
      )

    cleanup_cmd = format("{kinit_cmd} {tmp_dir}/hcatSmoke.sh hcatsmoke{unique} cleanup")

    Execute(cleanup_cmd,
            tries=3,
            user=params.smokeuser,
            try_sleep=5,
            path=['/usr/sbin', '/usr/local/nin', '/bin', '/usr/bin', params.execute_path],
            logoutput=True
    )
