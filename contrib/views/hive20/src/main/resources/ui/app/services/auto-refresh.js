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

export default Ember.Service.extend({
  store: Ember.inject.service(),
  isDatabaseRefresherRunning: false,
  tablesRefresherRunningStatus: {},


  startDatabasesAutoRefresh(databaseRefreshStartingCallback, databaseRefreshedCallback, startAfter = 30 * 1000, interval = 30 * 1000) {
    if (this.get('isDatabaseRefresherRunning')) {
      return;
    }

    console.log("Starting database auto refresh");

    this.set('isDatabaseRefresherRunning', true);
    Ember.run.later(() => {
      this._refreshDatabases(databaseRefreshStartingCallback, databaseRefreshedCallback, interval);
    }, startAfter);


  },

  _refreshDatabases(databaseRefreshStartingCallback, databaseRefreshedCallback, interval) {
    let reRun = () => {
      Ember.run.later(() => {
        this._refreshDatabases(databaseRefreshStartingCallback, databaseRefreshedCallback, interval);
      }, interval);
    };

    if (this.get('isDatabaseRefresherRunning')) {
      databaseRefreshStartingCallback();
      let oldDatabases = this.get('store').peekAll('database').mapBy('name');
      this.get('store').query('database', {}).then((data) => {
        let deletedDbCount = 0;
        let newDatabases = data.mapBy('name');
        oldDatabases.forEach((oldDB) => {
          if (!newDatabases.contains(oldDB)) {
            deletedDbCount++;
            let oldRecord = this.get('store').peekRecord('database', oldDB);
            this.get('store').unloadRecord(oldRecord);
          }
        });

        // Hack: Had to wrap the refreshed call inside run later because, unloadRecord is not synchronously unloading
        // records from store.
        Ember.run.later(() => databaseRefreshedCallback(deletedDbCount));
        reRun();
      }).catch((err) => {
        reRun();
      });
    }
  },

  stopDatabasesAutoRefresh() {
    console.log("Stopping database auto refresh");
    this.set('isDatabaseRefresherRunning', false);
  },

  startTablesAutoRefresh(databaseName, tablesRefreshStartingCallback, tablesRefreshedCallback, startAfter = 15 * 1000, interval = 15 * 1000) {
    if(!Ember.isEmpty(this.get('tablesRefresherRunningStatus')[databaseName])) {
      if (this.get('tablesRefresherRunningStatus')[databaseName]["started"]) {
        return;
      }
    }


    console.log("Starting tables auto refresh for " + databaseName);
    this.set('tablesRefresherRunningStatus',{});

    this.get('tablesRefresherRunningStatus')[databaseName] = {};
    this.get('tablesRefresherRunningStatus')[databaseName]["started"] = true;
    Ember.run.later(() => {
      this.refreshTables(databaseName, tablesRefreshStartingCallback, tablesRefreshedCallback, false, interval);
    }, startAfter);
  },

  refreshTables(databaseName, tablesRefreshStartingCallback, tablesRefreshedCallback, runOnce = false, interval) {
    let reRun = () => {
      let intervalRef = Ember.run.later(() => {
        this.refreshTables(databaseName, tablesRefreshStartingCallback, tablesRefreshedCallback, false, interval);
      }, interval);
      this.get('tablesRefresherRunningStatus')[databaseName]["intervalRef"] = intervalRef;
    };

    if (this.get('tablesRefresherRunningStatus')[databaseName]) {
      tablesRefreshStartingCallback(databaseName);
      let oldTableNames = this.get('store').peekAll('table').filterBy('database.name', databaseName).mapBy('name');
      this.get('store').query('table', {databaseId: databaseName}).then((data) => {
        let deletedTablesCount = 0;
        let newTableNames = data.mapBy('name');
        oldTableNames.forEach((oldTable) => {
          if (!newTableNames.contains(oldTable)) {
            deletedTablesCount++;
            let oldRecord = this.get('store').peekRecord('table', `${databaseName}/${oldTable}`);
            this.get('store').unloadRecord(oldRecord);
          }
        });

        newTableNames.forEach((newTable) => {
          if(!oldTableNames.contains(newTable)) {
            //table has been added
            let tableRecord = this.get('store').peekRecord('table', `${databaseName}/${newTable}`);
            let dbRecord = this.get('store').peekRecord('database', databaseName);
            dbRecord.get('tables').pushObject(tableRecord);
          }
        });

        // Hack: Had to wrap the refreshed call inside run later because, unloadRecord is not synchronously unloading
        // records from store.
        Ember.run.later(() => tablesRefreshedCallback(databaseName, deletedTablesCount));
        if(!runOnce) {
          reRun();
        }
      }).catch((err) => {
        if(!runOnce) {
          reRun();
        }
      });
    }
  },

  stopTablesAutoRefresh(databaseName) {
    console.log("Stopping tables auto refresh for " + databaseName);
    this.get('tablesRefresherRunningStatus')[databaseName]["started"] = false;
    let intervalRef = this.get('tablesRefresherRunningStatus')[databaseName]["intervalRef"];
    if (intervalRef) {
      Ember.run.cancel(intervalRef);
    }
  }
});
