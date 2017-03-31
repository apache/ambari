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
    if(Ember.isEmpty(id) && (requestType === 'query' || requestType === 'queryRecord')) {
      let dbId = query.databaseId;
      let tableName = query.tableName;
      let origFindAllUrl = this._super(...arguments);
      let prefix = origFindAllUrl.substr(0, origFindAllUrl.lastIndexOf("/"));
      delete query.databaseId;
      delete query.tableName;
      return `${prefix}/databases/${dbId}/tables/${tableName}/info`;
    }
    return this._super(...arguments);
  }
});
