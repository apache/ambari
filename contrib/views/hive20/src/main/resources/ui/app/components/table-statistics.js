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
  columnStatsKeys : [
    {dataKey: 'min', label: 'MIN'},
    {dataKey: 'max', label: 'MAX'},
    {dataKey: 'numNulls', label: 'NUMBER OF NULLS'},
    {dataKey: 'distinctCount', label: 'DISTINCT COUNT'},
    {dataKey: 'avgColLen', label: 'AVERAGE COLUMN LENGTH'},
    {dataKey: 'maxColLen', label: 'MAX COLUMN LENGTH'},
    {dataKey: 'numTrues', label: 'NUMBER OF TRUE'},
    {dataKey: 'numFalse', label: 'NUMBER OF FALSE'},
  ],

  statsService: Ember.inject.service(),

  analyseWithStatistics: false,
  partitionStatSupportedVersion: "2.1",
  isTablePartitioned: Ember.computed("table.partitionInfo.columns", function(){
    return this.get("table.partitionInfo.columns") && this.get("table.partitionInfo.columns.length") > 0;
  }),
  partitionStatSupported: Ember.computed("table.tableStats.databaseMetadata.databaseMajorVersion",
    "table.tableStats.databaseMetadata.databaseMinorVersion", function(){
    if(this.get('table.tableStats.databaseMetadata.databaseMajorVersion') > 2){
      return true;
    }else if(this.get('table.tableStats.databaseMetadata.databaseMajorVersion') === 2
      && this.get('table.tableStats.databaseMetadata.databaseMinorVersion') >= 1){
      return true;
    }

    return false;
  }),
  showStats:Ember.computed("partitionStatSupported", "isTablePartitioned", function(){
    if(!this.get("isTablePartitioned")) {
      return true;
    }else{
      if(this.get("partitionStatSupported")){
        return true;
      }else{
        return false;
      }
    }
  }),
  tableStats: Ember.computed.oneWay('table.tableStats'),

  tableStatisticsEnabled: Ember.computed.oneWay('table.tableStats.isTableStatsEnabled'),

  basicStatsAccurate: Ember.computed.oneWay('columnStatsAccurate.BASIC_STATS'),

  columnStatsAccurate: Ember.computed('table.tableStats.columnStatsAccurate', function () {
    let columnStatsJson = this.get('table.tableStats.columnStatsAccurate');
    return Ember.isEmpty(columnStatsJson) ? {} : JSON.parse(columnStatsJson.replace(/\\\"/g, '"'));
  }),

  columnsWithStatistics: Ember.computed('columnStatsAccurate', function () {
    let stats = this.get('columnStatsAccurate.COLUMN_STATS');
    return !stats ? [] : Object.keys(stats);
  }),

  columns: Ember.computed('table.columns', function () {
    let cols = this.get('table.columns');
    if(this.get("table.partitionInfo.columns")){ // show stats for all columns
      cols = cols.concat(this.get("table.partitionInfo.columns"));
    }
    return cols.map((col) => {
      let copy = Ember.Object.create(col);
      copy.set('hasStatistics', true);
      copy.set('isFetchingStats', false);
      copy.set('statsError', false);
      copy.set('showStats', true);
      return copy;
    });
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
      let colStatAccurate = data["columnStatsAccurate"];
      let colStatAccurateJson = Ember.isEmpty(colStatAccurate) ? {} : JSON.parse(colStatAccurate.replace(/\\\"/g, '"'));
      if(this.get("partitionStatSupported")){
        if(!colStatAccurateJson["COLUMN_STATS"] || colStatAccurateJson["COLUMN_STATS"][column.name] === "false"){
          column.set('statsWarn', true);
          column.set('statsWarnMsg', "Column statistics might be stale. Please  consider recomputing with 'include columns' option checked.");
        }
      }else if( !this.get("partitionStatSupported") && !(this.get("columnsWithStatistics").contains(column.get("name")))){
        column.set('statsWarn', true);
        column.set('statsWarnMsg', "Column statistics might be stale. Please  consider recomputing with 'include columns' option checked.");
      }

      let statsData = this.get("columnStatsKeys").map((item) => {
        return {label: item.label, value: data[item.dataKey]};
      });

      column.set('stats', statsData);
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
