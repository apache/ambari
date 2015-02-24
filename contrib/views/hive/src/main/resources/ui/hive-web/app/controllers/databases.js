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

export default Ember.ArrayController.extend({
  pageCount: 10,

  needs: [ constants.namingConventions.tables,
           constants.namingConventions.columns,
           constants.namingConventions.openQueries ],

  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  dbTables: Ember.computed.alias('controllers.' + constants.namingConventions.tables),
  dbColumns: Ember.computed.alias('controllers.' + constants.namingConventions.columns),

  _handleTablesError: function (err) {
    this.send('addAlert', constants.alerts.error, err.responseText, "alerts.errors.get.tables");
    this.set('isLoading', false);
  },

  _handleColumnsError: function (err) {
    this.send('addAlert', constants.alerts.error, err.responseText, "alerts.errors.get.columns");
    this.set('isLoading', false);
  },

  init: function () {
    this._super();

    var databaseAdapter = this.container.lookup('adapter:database');
    var baseUrl = databaseAdapter.buildURL() + '/' +
                  databaseAdapter.pathForType(constants.namingConventions.database) + '/';

    this.set('baseUrl', baseUrl);
    this.set('tableSearchResults', Ember.Object.create());

    this.set('tabs', Ember.ArrayProxy.create({ content: Ember.A([
      Ember.Object.create({
        name: Ember.I18n.t('titles.explorer'),
        visible: true,
        view: constants.namingConventions.databaseTree
      }),
      Ember.Object.create({
        name: Ember.I18n.t('titles.results'),
        view: constants.namingConventions.databaseSearch
      })
    ])}));
  },

  setTablePageAvailability: function (database) {
    var result;

    if (database.get('hasNext')) {
      result = true;
    } else if (database.tables.length > database.get('visibleTables.length')) {
      //if there are hidden tables
      result = true;
    }

    database.set('canGetNextPage', result);
  },

  setColumnPageAvailability: function (table) {
    var result;

    if (table.get('hasNext')) {
      result = true;
    } else if (table.columns.length > table.get('visibleColumns.length')) {
      //if there are hidden columns
      result = true;
    }

    table.set('canGetNextPage', result);
  },

  selectedDatabaseChanged: function () {
    var self = this,
        database = this.get('selectedDatabase');

    //if no selected database or database has already fully loaded tables
    if (!database || (database.tables && !database.get('hasNext'))) {
      return;
    }

    this.set('isLoading', true);

    this.get('dbTables').getTables(database.get('name')).then(function (tables) {
      var mappedTables = {};

      //don't use Ember.Object.set since it can be very expensive for large collections (e.g. 15000 tables),
      //thus we should not do any bindings directly on the 'tables' collection.
      database.tables = tables;

      tables.forEach(function (table) {
        mappedTables[table.name] = [];
      });

      self.set('openQueries.selectedTables', mappedTables);
      self.set('isLoading', false);
    }, function (err) {
      self._handleTablesError(err);
    });
  }.observes('selectedDatabase'),

  getNextColumnPage: function (database, table) {
    var self = this;

    this.set('isLoading', true);

    if (!table.columns) {
      table.columns = [];
      table.set('visibleColumns', []);
    }

    this.get('dbColumns').getColumnsPage(database.get('name'), table).then(function (result) {
      table.columns.pushObjects(result.columns);
      table.get('visibleColumns').pushObjects(result.columns);
      table.set('hasNext', result.hasNext);

      self.setColumnPageAvailability(table);
      self.set('isLoading', false);
    }, function (err) {
      self._handleColumnsError(err);
    });
  },

  getNextTablePage: function (database) {
    var self = this;

    this.set('isLoading', true);

    if (!database.tables) {
      database.tables = [];
      database.set('visibleTables', []);
    }

    this.get('dbTables').getTablesPage(database).then(function (result) {
      database.tables.pushObjects(result.tables);
      database.get('visibleTables').pushObjects(result.tables);
      database.set('hasNext', result.hasNext);

      self.setTablePageAvailability(database);
      self.set('isLoading', false);
    }, function (err) {
      self._handleTablesError(err);
    });
  },

  getAllColumns: function (tableName) {
    var defer = Ember.RSVP.defer();
    var self = this;
    var database = this.get('selectedDatabase');
    var table = database.get('tables').findBy('name', tableName);

    //if all the columns were already loaded for this table, do not get them again.
    if (table.columns && !table.get('hasNext')) {
      defer.resolve();
    } else {
      this.set('isLoading', true);

      this.get('dbColumns').getColumns(database.get('name'), tableName).then(function (columns) {
        table.columns = columns;
        table.set('hasNext', false);

        self.get('openQueries.selectedTables')[tableName] = columns.mapProperty('name');
        self.set('isLoading', false);

        defer.resolve();
      }, function (err) {
        self._handleColumnsError(err);
        defer.reject(err);
      });
    }

    return defer.promise;
  },

  actions: {
    getTables: function (dbName) {
      var database = this.findBy('name', dbName),
          tables = database.tables,
          pageCount = this.get('pageCount');

      if (!tables) {
        this.getNextTablePage(database);
      } else {
        database.set('visibleTables', tables.slice(0, pageCount));
        this.setTablePageAvailability(database);
      }
    },

    getColumns: function (tableName, database) {
      var table = database.get('visibleTables').findBy('name', tableName),
          pageCount = this.get('pageCount'),
          columns = table.columns;

      if (!columns) {
        this.getNextColumnPage(database, table);
      } else {
        table.set('visibleColumns', columns.slice(0, pageCount));
        this.setColumnPageAvailability(table);
      }
    },

    showMoreTables: function (database) {
      var tables = database.tables,
          visibleTables = database.get('visibleTables'),
          visibleCount = visibleTables.length;

      if (!tables) {
        this.getNextTablePage(database);
      } else {
        if (tables.length > visibleCount) {
          visibleTables.pushObjects(tables.slice(visibleCount, visibleCount + this.get('pageCount')));
          this.setTablePageAvailability(database);
        } else {
          this.getNextTablePage(database);
        }
      }
    },

    showMoreColumns: function (table, database) {
      var columns = table.columns,
          visibleColumns = table.get('visibleColumns'),
          visibleCount = visibleColumns.length;

      if (!columns) {
        this.getNextColumnPage(database, table);
      } else {
        if (columns.length > visibleCount) {
          visibleColumns.pushObjects(columns.slice(visibleCount, visibleCount + this.get('pageCount')));
          this.setColumnPageAvailability(table);
        } else {
          this.getNextColumnPage(database, table);
        }
      }
    },

    searchTables: function (searchTerm) {
      var self = this,
          resultsTab = this.get('tabs').findBy('view', constants.namingConventions.databaseSearch),
          tableSearchResults = this.get('tableSearchResults');

      resultsTab.set('visible', true);
      this.set('selectedTab', resultsTab);
      this.set('columnSearchTerm', '');
      this.set('isLoading', true);

      this.get('dbTables').getTablesPage(this.get('selectedDatabase'), searchTerm, true).then(function (result) {
        tableSearchResults.set('tables', result.tables);
        tableSearchResults.set('hasNext', result.hasNext);

        self.set('isLoading', false);
      }, function (err) {
        self._handleTablesError(err);
      });
    },

    searchColumns: function (searchTerm) {
      var self = this,
          database = this.get('selectedDatabase'),
          resultsTab = this.get('tabs').findBy('view', constants.namingConventions.databaseSearch),
          tables = this.get('tableSearchResults.tables');

      this.set('selectedTab', resultsTab);

      this.set('isLoading', true);

      tables.forEach(function (table) {
        self.get('dbColumns').getColumnsPage(database.get('name'), table, searchTerm, true).then(function (result) {
          table.set('columns', result.columns);
          table.set('hasNext', result.hasNext);

          if (tables.indexOf(table) === tables.get('length') -1) {
            self.set('isLoading', false);
          }
        }, function (err) {
          self._handleColumnsError(err);
        });
      });
    },

    showMoreResultTables: function () {
      var self = this,
          database = this.get('selectedDatabase'),
          tableSearchResults = this.get('tableSearchResults'),
          searchTerm = this.get('tableSearchTerm');

      this.set('isLoading', true);

      this.get('dbTables').getTablesPage(database, searchTerm).then(function (tablesResult) {
        var tables = tableSearchResults.get('tables');
        var shouldGetColumns = tables.any(function (table) {
          return table.get('columns.length') > 0;
        });

        tables.pushObjects(tablesResult.tables);
        tableSearchResults.set('hasNext', tablesResult.hasNext);

        //if user has already searched for columns for the previously loaded tables,
        //load the columns search results for the newly loaded tables.
        if (shouldGetColumns) {
          tablesResult.tables.forEach(function (table) {
            self.get('dbColumns').getColumnsPage(database.get('name'), table, self.get('columnSearchTerm'), true).then(function (result) {
              table.set('columns', result.columns);
              table.set('hasNext', result.hasNext);

              if (tablesResult.tables.indexOf(table) === tablesResult.tables.get('length') -1) {
                self.set('isLoading', false);
              }
            }, function (err) {
              self._handleColumnsError(err);
            });
          });
        } else {
          self.set('isLoading', false);
        }
      }, function (err) {
        self._handleTablesError(err);
      });
    },

    showMoreResultColumns: function (table) {
      var self = this;

      this.set('isLoading', true);

      this.get('dbColumns').getColumnsPage(this.get('selectedDatabase.name'), table, this.get('columnSearchTerm')).then(function (result) {
        table.get('columns').pushObjects(result.columns);
        table.set('hasNext', result.hasNext);

        self.set('isLoading', false);
      }, function (err) {
        self._handleColumnsError(err);
      });
    }
  }
});