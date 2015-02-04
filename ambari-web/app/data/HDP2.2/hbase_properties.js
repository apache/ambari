/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var properties = {

  'SSLRangerSettings': [
    'SSL_KEYSTORE_FILE_PATH',
    'SSL_KEYSTORE_PASSWORD',
    'SSL_TRUSTSTORE_FILE_PATH',
    'SSL_TRUSTSTORE_PASSWORD'
  ],

  'RepositoryConfigs': [
    'POLICY_USER',
    'REPOSITORY_CONFIG_PASSWORD',
    'REPOSITORY_CONFIG_USERNAME'
  ],

  'HDFSAuditSettings': [
    'XAAUDIT.HDFS.DESTINATION_DIRECTORY',
    'XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY',
    'XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY',
    'XAAUDIT.HDFS.DESTINTATION_FILE',
    'XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS',
    'XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS',
    'XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS',
    'XAAUDIT.HDFS.LOCAL_BUFFER_FILE',
    'XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS',
    'XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS',
    'XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'
  ]
};

var props = [];

for (var category in properties) {
  props = props.concat(App.config.generateConfigPropertiesByName(properties[category],
    { category: category, serviceName: 'HBASE', filename: 'ranger-hbase-plugin-properties.xml'}));
}

module.exports = props;

