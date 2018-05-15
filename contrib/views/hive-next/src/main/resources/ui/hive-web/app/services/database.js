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

export default Ember.Service.extend({
  store: Ember.inject.service(),

  pageCount: 10,
  selectedDatabase: null,
  selectedTables: null,
  databases: [],

  init: function () {
    this._super();

    var databaseAdapter = this.container.lookup('adapter:database');
    var baseUrl = databaseAdapter.buildURL() + '/' +
                  databaseAdapter.pathForType(constants.namingConventions.database) + '/';

    this.set('baseUrl', baseUrl);
  },

  getDatabases: function () {
    var defer = Ember.RSVP.defer();
    var self = this;

    this.get('store').unloadAll(constants.namingConventions.database);
    this.get('store').fetchAll(constants.namingConventions.database).then(function (databases) {
      self.set('databases', databases);
      defer.resolve(databases);
    }, function (error) {
      defer.reject(error);
    })

    return defer.promise;
  },

  // This will do a ajax call to fetch the current database by by-passing the store.
  // As we want to retain the current state of databases in store and just want to
  // find the current databases in the server
  getDatabasesFromServer: function() {
    var defer = Ember.RSVP.defer();
    var url = this.get('baseUrl');
    Ember.$.getJSON(url).then(function(data) {
      defer.resolve(data.databases);
    }, function(err) {
      defer.reject(err);
    });
    return defer.promise;
  },

  setDatabaseByName: function (name) {
    var database = this.databases.findBy('name', name);

    if (database) {
      this.set('selectedDatabase', database);
    }
  },

  getColumnsPage: function (databaseName, table, searchTerm, firstSearchPage) {
    var defer = Ember.RSVP.defer();

    var url = this.get('baseUrl') +
              databaseName +
              '/table/' +
              table.get('name');

    url += '.page?searchId&count=' + this.get('pageCount');
    url += '&columns=3,5,6,8';

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
              type: row[1],
              precision : row[2],
              scale : row[3]
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

  getAllTables: function (db) {
    var defer = Ember.RSVP.defer();
    var database = db || this.get('selectedDatabase');
    var self;
    var url;

    if (!database) {
      defer.resolve();
    } else if (database.tables && !database.get('hasNext')) {
      this.set('selectedTables', database.tables.mapProperty('name'));
      defer.resolve();
    } else {
      self = this;
      url = this.get('baseUrl') + database.get('name') + '/table';

      Ember.$.getJSON(url).then(function (data) {
        var tables = data.tables.map(function (table) {
          return Ember.Object.create({
            name: table
          });
        });

        //don't use Ember.Object.set since it can be very expensive for large collections (e.g. 15000 tables),
        //thus we should not do any bindings directly on the 'tables' collection.
        database.tables = tables;

        Ember.run(function () {
          self.set('selectedTables', tables.mapProperty('name'));
        });

        defer.resolve();
      }, function (err) {
        defer.reject(err);
      });
    }

    return defer.promise;
  },

  getAllColumns: function (tableName, db) {
    var database = db || this.get('selectedDatabase');
    var defer = Ember.RSVP.defer();
    var table;
    var self;
    var url;

    if (!database) {
      defer.resolve();
    } else {
      table = database.tables.findBy('name', tableName);

      if (!table) {
        defer.resolve();
      } else if (table.columns && !table.get('hasNext')) {
        this.get('selectedTables')[tableName] = table.columns.mapProperty('name');
        defer.resolve();
      } else {
        self = this;
        url = this.get('baseUrl') + database.get('name') + '/table/' + tableName

        Ember.$.getJSON(url).then(function (data) {
          var columns = data.columns.map(function (column) {
            return Ember.Object.create({
              name: column[0],
              type: column[1]
            });
          });

          table.columns = columns;
          table.set('hasNext', false);

          self.get('selectedTables')[tableName] = columns.mapProperty('name');

          defer.resolve();
        }, function (err) {
          defer.reject(err);
        });
      }
    }

    return defer.promise;
  }
});
