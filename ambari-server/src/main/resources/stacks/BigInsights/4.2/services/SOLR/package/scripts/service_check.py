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
from resource_management.libraries.functions.validate import call_and_match_output
import time

class SolrServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    url = "http://" + params.solr_server_host + ":" + str(params.solr_port) + "/solr/"

    if params.security_enabled:
        kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
        Execute(kinit_cmd,
                user = params.smokeuser,
                logoutput = True
        )

    create_collection_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh solr create -c smokeuser_ExampleCollection -s 2 -d data_driven_schema_configs")
    Logger.info("Creating solr collection from example: %s" % create_collection_cmd)
    Execute(create_collection_cmd,
            user = params.smokeuser,
            logoutput = True)

    list_collection_cmd = "su " + params.smokeuser + " -c 'curl -s --negotiate -u : " + url + "admin/collections?action=list'"
    list_collection_output = "<str>smokeuser_ExampleCollection</str>"
    Logger.info("List Collections: %s" % list_collection_cmd)
    call_and_match_output(list_collection_cmd, format("({list_collection_output})"), "Failed to create collection \"smokeuser_ExampleCollection\" or check that collection exists")

    delete_collection_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh solr delete -c smokeuser_ExampleCollection")
    Logger.info("Deleting solr collection : %s" % delete_collection_cmd)
    Execute(delete_collection_cmd,
      user = params.smokeuser,
      logoutput=True
    )


if __name__ == "__main__":
  SolrServiceCheck().execute()
