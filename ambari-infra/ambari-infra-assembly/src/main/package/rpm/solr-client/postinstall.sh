#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

SOLR_CLOUD_CLI_LINK_NAME="/usr/bin/infra-solr-cloud-cli"
SOLR_CLOUD_CLI_SOURCE="/usr/lib/ambari-infra-solr-client/solrCloudCli.sh"

SOLR_INDEX_TOOL_LINK_NAME="/usr/bin/infra-lucene-index-tool"
SOLR_INDEX_TOOL_SOURCE="/usr/lib/ambari-infra-solr-client/solrIndexHelper.sh"

SOLR_DATA_MANAGER_LINK_NAME="/usr/bin/infra-solr-data-manager"
SOLR_DATA_MANAGER_SOURCE="/usr/lib/ambari-infra-solr-client/solrDataManager.py"

rm -f $SOLR_CLOUD_CLI_LINK_NAME ; ln -s $SOLR_CLOUD_CLI_SOURCE $SOLR_CLOUD_CLI_LINK_NAME
rm -f $SOLR_INDEX_TOOL_LINK_NAME ; ln -s $SOLR_INDEX_TOOL_SOURCE $SOLR_INDEX_TOOL_LINK_NAME
rm -f $SOLR_DATA_MANAGER_LINK_NAME ; ln -s $SOLR_DATA_MANAGER_SOURCE $SOLR_DATA_MANAGER_LINK_NAME