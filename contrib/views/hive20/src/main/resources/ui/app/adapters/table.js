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

import Ember from 'ember';
import DDLAdapter from './ddl';

export default DDLAdapter.extend({
  buildURL(modelName, id, snapshot, requestType, query) {
    // Check if the query is to find all tables for a particular database
    if (Ember.isEmpty(id) && (requestType === 'query' || requestType === 'queryRecord')) {
      let dbId = query.databaseId;
      let tableName = query.tableName;
      let origFindAllUrl = this._super(...arguments);
      let prefix = origFindAllUrl.substr(0, origFindAllUrl.lastIndexOf("/"));
      delete query.databaseId;
      delete query.tableName;
      if (Ember.isEmpty(tableName)) {
        return `${prefix}/databases/${dbId}/tables`;
      } else {
        return `${prefix}/databases/${dbId}/tables/${tableName}`;
      }
    }
    return this._super(...arguments);
  },


  createTable(tableMetaInfo) {
    let postURL = this.buildURL('table', null, null, 'query', { databaseId: tableMetaInfo.database });
    return this.ajax(postURL, 'POST', { data: { tableInfo: tableMetaInfo } });
  },

  editTable(tableMetaInfo) {
    let postURL = this.buildURL('table', null, null, 'query',
      { databaseId: tableMetaInfo.database, tableName: tableMetaInfo.table });
    return this.ajax(postURL, 'PUT', { data: { tableInfo: tableMetaInfo } });
  },

  deleteTable(database, tableName) {
    let deletURL = this.buildURL('table', null, null, 'query', { databaseId: database, tableName: tableName });
    return this.ajax(deletURL, 'DELETE');
  },

  renameTable(database, newTableName, oldTableName) {
    let renameUrl = this.buildURL('table', null, null, 'query', { databaseId: database, tableName: oldTableName }) + '/rename';
    let data = {
      newDatabase: database,
      newTable: newTableName
    };
    return this.ajax(renameUrl, 'PUT', {data: data});
  },

  analyseTable(databaseName, tableName, withColumns = false) {
    let analyseUrl = this.buildURL('table', null, null, 'query', { databaseId: databaseName, tableName: tableName }) +
      '/analyze' +
      (withColumns ? '?analyze_columns=true' : '');
    return this.ajax(analyseUrl, 'PUT');
  },

  generateColumnStats(databaseName, tableName, columnName) {
    let url = this.buildURL('table', null, null, 'query', {databaseId: databaseName, tableName: tableName}) + `/column/${columnName}/stats`;
    return this.ajax(url, 'GET');
  },

  fetchColumnStats(databaseName, tableName, columnName, jobId) {
    let url = this.buildURL('table', null, null, 'query', {databaseId: databaseName, tableName: tableName}) + `/column/${columnName}/fetch_stats?job_id=${jobId}`;
    return this.ajax(url, 'GET');
  }
});
