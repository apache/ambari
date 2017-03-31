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
import UILoggerMixin from '../mixins/ui-logger';

export default Ember.Component.extend(UILoggerMixin, {
  statsService: Ember.inject.service(),

  analyseWithStatistics: false,

  tableStats: Ember.computed.oneWay('table.tableStats'),
  tableStatisticsEnabled: Ember.computed.oneWay('table.tableStats.isTableStatsEnabled'),

  columnStatsAccurate: Ember.computed('table.tableStats.columnStatsAccurate', function () {
    let columnStatsJson = this.get('table.tableStats.columnStatsAccurate');
    return Ember.isEmpty(columnStatsJson) ? {} : JSON.parse(columnStatsJson.replace(/\\\"/g, '"'));
  }),

  columnsWithStatistics: Ember.computed('columnStatsAccurate', function () {
    let stats = this.get('columnStatsAccurate.COLUMN_STATS');
    return !stats ? [] : Object.keys(stats);
  }),

  columns: Ember.computed('table.columns', 'columnsWithStatistics', function () {
    let cols = this.get('table.columns');
    let colsWithStatistics = this.get('columnsWithStatistics');
    return cols.map((col) => {
      let copy = Ember.Object.create(col);
      copy.set('hasStatistics', colsWithStatistics.contains(copy.name));
      copy.set('isFetchingStats', false);
      copy.set('statsError', false);
      copy.set('showStats', true);
      return copy;
    });
  }),

  allColumnsHasStatistics: Ember.computed('table.columns', 'columnsWithStatistics', function () {
    let colsNames = this.get('table.columns').getEach('name');
    let colsWithStatistics = this.get('columnsWithStatistics');

    let colsNotIn = colsNames.filter((item) => !colsWithStatistics.contains(item));
    return colsNotIn.length === 0;
  }),

  performTableAnalysis(withColumns = false) {
    const tableName = this.get('table.table');
    const databaseName = this.get('table.database');

    let title = `Analyse table` + (withColumns ? ' for columns' : '');
    this.set('analyseTitle', title);
    this.set('analyseMessage', `Submitting job to generate statistics for table '${tableName}'`);

    this.set('showAnalyseModal', true);

    this.get('statsService').generateStatistics(databaseName, tableName, withColumns)
      .then((job) => {
        this.set('analyseMessage', 'Waiting for the job to complete');
        return this.get('statsService').waitForStatsGenerationToComplete(job);
      }).then(() => {
      this.set('analyseMessage', 'Finished analysing table for statistics');
      Ember.run.later(() => this.closeAndRefresh(), 2 * 1000);
    }).catch((err) => {
      this.set('analyseMessage', 'Job failed for analysing statistics of table');
      this.get('logger').danger(`Job failed for analysing statistics of table '${tableName}'`, this.extractError(err));
      Ember.run.later(() => this.closeAndRefresh(), 2 * 1000);
    });
  },

  fetchColumnStats(column) {
    const tableName = this.get('table.table');
    const databaseName = this.get('table.database');

    column.set('isFetchingStats', true);

    this.get('statsService').generateColumnStatistics(databaseName, tableName, column.name).then((job) => {
      return this.get('statsService').waitForStatsGenerationToComplete(job, false);
    }).then((job) => {
      return this.get('statsService').fetchColumnStatsResult(databaseName, tableName, column.name, job);
    }).then((data) => {
      column.set('isFetchingStats', false);
      column.set('stats', data);
    }).catch((err) => {
      column.set('isFetchingStats', false);
      column.set('statsError', true);
      this.get('logger').danger(`Job failed for fetching column statistics for column '${column.name}' of table '${tableName}'`, this.extractError(err));
    });
  },

  closeAndRefresh() {
    this.set('showAnalyseModal', false);
    this.sendAction('refresh');
  },

  actions: {
    analyseTable() {
      this.performTableAnalysis(this.get('analyseWithStatistics'));
    },

    fetchStats(column) {
      this.fetchColumnStats(column);
    },

    toggleShowStats(column) {
      column.toggleProperty('showStats');
    }
  }
});
