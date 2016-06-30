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
import ENV from '../config/environment';

export default Ember.Controller.extend({
  databaseService: Ember.inject.service(constants.namingConventions.database),
  notifyService: Ember.inject.service(constants.namingConventions.notify),

  pageCount: 10,

  previousSelectedDatabaseName : "" ,
  selectedDatabase: Ember.computed.alias('databaseService.selectedDatabase'),
  databases: Ember.computed.alias('databaseService.databases'),

  tableSearchResults: Ember.Object.create(),

  isDatabaseRefreshInProgress: false,
  showColumnsResultAlert: false,
  textColumnSearchTerm:'',

  tableControls: [
    {
      icon: 'fa-list',
      action: 'loadSampleData',
      tooltip: Ember.I18n.t('tooltips.loadSample')
    }
  ],

  panelIconActions: [
    {
      icon: 'fa-refresh',
      action: 'refreshDatabaseExplorer',
      tooltip: Ember.I18n.t('tooltips.refresh')
    }
  ],

  tabs: [
    Ember.Object.create({
      name: Ember.I18n.t('titles.explorer'),
      visible: true,
      view: constants.namingConventions.databaseTree
    }),
    Ember.Object.create({
      name: Ember.I18n.t('titles.results'),
      view: constants.namingConventions.databaseSearch
    })
  ],

  _handleError: function (error) {
    this.get('notifyService').error(error);
    this.set('isLoading', false);
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
    var self = this;

    this.resetSearch();

    this.set('isLoading', true);

    this.get('databaseService').getAllTables().then(function () {
      self.set('isLoading', false);
      self.set('previousSelectedDatabaseName',self.get('selectedDatabase').get('name'));
      self.get('notifyService').info("Selected database : "+self.get('selectedDatabase').get('name'));
    }, function (error) {
      self.get('notifyService').pushError("Error while selecting database : "+self.get('selectedDatabase').get('name'),error.responseJSON.message+"\n"+error.responseJSON.trace);
      self.get('databaseService').setDatabaseByName(self.get('previousSelectedDatabaseName'));
      self.set('isLoading', false);
    });
  }.observes('selectedDatabase'),

  getNextColumnPage: function (database, table) {
    var self = this;

    this.set('isLoading', true);

    if (!table.columns) {
      table.columns = [];
      table.set('visibleColumns', []);
    }

    this.get('databaseService').getColumnsPage(database.get('name'), table).then(function (result) {
      table.columns.pushObjects(result.columns);
      table.get('visibleColumns').pushObjects(result.columns);
      table.set('hasNext', result.hasNext);

      self.setColumnPageAvailability(table);
      self.set('isLoading', false);
    }, function (err) {
      self._handleError(err);
    });
  },

  getNextTablePage: function (database) {
    var self = this;

    this.set('isLoading', true);

    if (!database.tables) {
      database.tables = [];
      database.set('visibleTables', []);
    }

    this.get('databaseService').getTablesPage(database).then(function (result) {
      database.tables.pushObjects(result.tables);
      database.get('visibleTables').pushObjects(result.tables);
      database.set('hasNext', result.hasNext);

      self.setTablePageAvailability(database);
      self.set('isLoading', false);
    }, function (err) {
      self._handleError(err);
    });
  },

  getDatabases: function () {
    var self = this;
    var selectedDatabase = this.get('selectedDatabase.name') || 'default';

    this.set('isDatabaseRefreshInProgress', true);

    this.set('isLoading', true);

    this.get('databaseService').getDatabases().then(function (databases) {
      self.set('isLoading');
      self.get('databaseService').setDatabaseByName(selectedDatabase);
    }).catch(function (error) {
      self._handleError(error);

      if(error.status == 401) {
         self.send('passwordLDAPDB');
      }
    }).finally(function() {
      self.set('isDatabaseRefreshInProgress', false);
    });
  }.on('init'),

  syncDatabases: function() {
    this.set('isDatabaseRefreshInProgress', true);
    var oldDatabaseNames = this.store.all('database').mapBy('name');
    var self = this;
    return this.get('databaseService').getDatabasesFromServer().then(function(data) {
      // Remove the databases from store which are not in server
      data.forEach(function(dbName) {
        if(!oldDatabaseNames.contains(dbName)) {
          self.store.createRecord('database', {
            id: dbName,
            name: dbName
          });
        }
      });
      // Add the databases in store which are new in server
      oldDatabaseNames.forEach(function(dbName) {
        if(!data.contains(dbName)) {
          self.store.find('database', dbName).then(function(db) {
            self.store.unloadRecord(db);
          });
        }
      });
    }).finally(function() {
      self.set('isDatabaseRefreshInProgress', false);
    });
  },

  initiateDatabaseSync: function() {
    // This was required so that the unit test would not stall
    if(ENV.environment !== "test") {
      Ember.run.later(this, function() {
        if (this.get('isDatabaseRefreshInProgress') === false) {
          this.syncDatabases();
          this.initiateDatabaseSync();
        }
      }, 15000);
    }
  }.on('init'),

  resetSearch: function() {
    var resultsTab = this.get('tabs').findBy('view', constants.namingConventions.databaseSearch);
    var databaseExplorerTab = this.get('tabs').findBy('view', constants.namingConventions.databaseTree);
    var tableSearchResults = this.get('tableSearchResults');
    resultsTab.set('visible', false);
    this.set('selectedTab', databaseExplorerTab);
    this.set('tableSearchTerm', '');
    this.set('columnSearchTerm', '');
    tableSearchResults.set('tables', undefined);
    tableSearchResults.set('hasNext', undefined);
  },


  actions: {
    refreshDatabaseExplorer: function () {
      if (this.get('isDatabaseRefreshInProgress') === false) {
        this.getDatabases();
        this.resetSearch();
      } else {
        console.log("Databases refresh is in progress. Skipping this request.");
      }
    },

    passwordLDAPDB: function(){
      var self = this,
          defer = Ember.RSVP.defer();

      this.send('openModal', 'modal-save', {
        heading: "modals.authenticationLDAP.heading",
        text:"",
        type: "password",
        defer: defer
      });

      defer.promise.then(function (text) {
        // make a post call with the given ldap password.
        var password = text;
        var pathName = window.location.pathname;
        var pathNameArray = pathName.split("/");
        var hiveViewVersion = pathNameArray[3];
        var hiveViewName = pathNameArray[4];
        var ldapAuthURL = "/api/v1/views/HIVE/versions/"+ hiveViewVersion + "/instances/" + hiveViewName + "/jobs/auth";

        $.ajax({
          url: ldapAuthURL,
          type: 'post',
          headers: {'X-Requested-With': 'XMLHttpRequest', 'X-Requested-By': 'ambari'},
          contentType: 'application/json',
          data: JSON.stringify({ "password" : password}),
          success: function( data, textStatus, jQxhr ){
            console.log( "LDAP done: " + data );
            self.getDatabases();
            self.syncDatabases();
          },
          error: function( jqXhr, textStatus, errorThrown ){
            console.log( "LDAP fail: " + errorThrown );
            self.get('notifyService').error( "Wrong Credentials." );
          }
        });
      });
    },

    loadSampleData: function (tableName, database) {
      var self = this;
      this.send('addQuery', Ember.I18n.t('titles.tableSample', { tableName: tableName }));

      Ember.run.later(function () {
        var query = constants.sampleDataQuery.fmt(tableName);

        self.set('selectedDatabase', database);
        self.send('executeQuery', constants.jobReferrer.sample, query);
      });
    },

    getTables: function (dbName) {
      var database = this.get('databases').findBy('name', dbName),
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

      searchTerm = searchTerm ? searchTerm.toLowerCase() : '';

      this.set('showColumnsResultAlert', false);

      this.set('tablesSearchTerm', searchTerm);
      resultsTab.set('visible', true);
      this.set('selectedTab', resultsTab);
      this.set('columnSearchTerm', '');
      this.set('isLoading', true);

      this.get('databaseService').getTablesPage(this.get('selectedDatabase'), searchTerm, true).then(function (result) {
        tableSearchResults.set('tables', result.tables);
        tableSearchResults.set('hasNext', result.hasNext);

        self.set('isLoading', false);
      }, function (err) {
        self._handleError(err);
      });
    },

    searchColumns: function (searchTerm) {
      var self = this,
          database = this.get('selectedDatabase'),
          resultsTab = this.get('tabs').findBy('view', constants.namingConventions.databaseSearch),
          tables = this.get('tableSearchResults.tables');

      searchTerm = searchTerm ? searchTerm.toLowerCase() : '';

      this.set('columnSearchTerm', searchTerm);
      this.set('textColumnSearchTerm', searchTerm);

      this.set('selectedTab', resultsTab);
      this.set('isLoading', true);
      this.set('showColumnsResultAlert', false);

      var tableCount = tables.length || 0;
      var noColumnMatchTableCount = 0;

      tables.forEach(function (table) {
        self.get('databaseService').getColumnsPage(database.get('name'), table, searchTerm, true).then(function (result) {

          if(Ember.isEmpty(result.columns)){
            noColumnMatchTableCount = noColumnMatchTableCount + 1;
          }
          table.set('columns', result.columns);
          table.set('hasNext', result.hasNext);

          if (tables.indexOf(table) === tables.get('length') -1) {
            self.set('isLoading', false);
          }

          // This will execute only in the last interation
          if(noColumnMatchTableCount === tableCount) {
            self.set('showColumnsResultAlert', true);
          }
        }, function (err) {
          self._handleError(err);
        });
      });
    },

    showMoreResultTables: function () {
      var self = this,
          database = this.get('selectedDatabase'),
          tableSearchResults = this.get('tableSearchResults'),
          searchTerm = this.get('tableSearchTerm');

      this.set('isLoading', true);

      this.get('databaseService').getTablesPage(database, searchTerm).then(function (tablesResult) {
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
            self.get('databaseService').getColumnsPage(database.get('name'), table, self.get('columnSearchTerm'), true).then(function (result) {
              table.set('columns', result.columns);
              table.set('hasNext', result.hasNext);

              if (tablesResult.tables.indexOf(table) === tablesResult.tables.get('length') -1) {
                self.set('isLoading', false);
              }
            }, function (err) {
              self._handleError(err);
            });
          });
        } else {
          self.set('isLoading', false);
        }
      }, function (err) {
        self._handleError(err);
      });
    },

    showMoreResultColumns: function (table) {
      var self = this;

      this.set('isLoading', true);

      this.get('databaseService').getColumnsPage(this.get('selectedDatabase.name'), table, this.get('columnSearchTerm')).then(function (result) {
        table.get('columns').pushObjects(result.columns);
        table.set('hasNext', result.hasNext);

        self.set('isLoading', false);
      }, function (err) {
        self._handleError(err);
      });
    }
  }
});
