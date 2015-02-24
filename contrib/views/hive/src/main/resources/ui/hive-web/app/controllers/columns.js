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

  getColumnsPage: function (databaseName, table, searchTerm, firstSearchPage) {
    var defer = Ember.RSVP.defer();

    var url = this.get('baseUrl') +
              databaseName +
              '/table/' +
              table.get('name');

    url += '.page?searchId&count=' + this.get('pageCount');
    url += '&columns=3,5';

    if (searchTerm) {
      url += '&searchId=searchColumns' + '&like=' + searchTerm;

      if (firstSearchPage) {
        url += '&first=true';
      }
    } else if (!table.get('columns.length')) {
      url += '&first=true';
    }

    Ember.$.getJSON(url).then(function (data) {
      Ember.run(function () {
        var columns;

        columns = data.rows.map(function (row) {
          return Ember.Object.create({
            name: row[0],
            type: row[1]
          });
        });

        defer.resolve({
          columns: columns,
          hasNext: data.hasNext
        });
      });
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  },

  getColumns: function (databaseName, tableName) {
    var defer = Ember.RSVP.defer();

    var url = this.get('baseUrl') +
              databaseName +
              '/table/' +
              tableName;

    Ember.$.getJSON(url).then(function (data) {
      Ember.run(function () {
        var columns = data.columns.map(function (column) {
          return Ember.Object.create({
            name: column[0],
            type: column[1]
          });
        });

        defer.resolve(columns);
      });
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  }
});
