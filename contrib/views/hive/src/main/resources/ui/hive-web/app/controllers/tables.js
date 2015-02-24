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
import constants from 'hive/utils/constants';

export default Ember.ObjectController.extend({
  pageCount: 10,

  init: function () {
    this._super();

    var databaseAdapter = this.container.lookup('adapter:database');
    var baseUrl = databaseAdapter.buildURL() + '/' +
                  databaseAdapter.pathForType(constants.namingConventions.database) + '/';

    this.set('baseUrl', baseUrl);
  },

  getTablesPage: function (database, searchTerm, firstSearchPage) {
    var defer = Ember.RSVP.defer(),
        url = this.get('baseUrl') +
              database.get('name') +
              '/table.page?count=';

    url += this.get('pageCount');

    if (searchTerm) {
      url += '&searchId=searchTables' + '&like=' + searchTerm;

      if (firstSearchPage) {
        url += '&first=true';
      }
    } else if (!database.get('tables.length')) {
      url += '&first=true';
    }

    Ember.$.getJSON(url).then(function (data) {
      var tables;

      tables = data.rows.map(function (row) {
        return Ember.Object.create({
          name: row[0]
        });
      });

      defer.resolve({
        tables: tables,
        hasNext: data.hasNext
      });
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  },

  getTables: function (databaseName) {
    var defer = Ember.RSVP.defer(),
        url = this.get('baseUrl') +
              databaseName +
              '/table';

    Ember.$.getJSON(url).then(function (data) {
      var tables = data.tables.map(function (table) {
        return Ember.Object.create({
          name: table
        });
      });

      defer.resolve(tables);
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  }
});
