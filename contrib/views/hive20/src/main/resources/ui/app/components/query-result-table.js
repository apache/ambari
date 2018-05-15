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

export default Ember.Component.extend({

  classNames: ['query-result-table', 'clearfix'],

  jobId: null,

  queryResult: {'schema' :[], 'rows' :[]},

  columnFilterText: null,
  columnFilter: null,

  columnFilterDebounced: Ember.observer('columnFilterText', function() {
    Ember.run.debounce(this, () => {
      this.set('columnFilter', this.get('columnFilterText'));
    }, 500);
  }),

  columns: Ember.computed('queryResult', function() {
    let columnArr =[];

    this.get('queryResult').schema.forEach(function(column){
      let tempColumn = {};

      tempColumn['label'] = column[0];

      let localValuePath = column[0];
      tempColumn['valuePath'] = localValuePath.split(".").join('');

      columnArr.push(tempColumn);
    });
    return columnArr;
  }),

  rows: Ember.computed('queryResult','columns', function() {
    let rowArr = [], self = this;

    if(self.get('columns').length > 0) {
      self.get('queryResult').rows.forEach(function(row){
        var mylocalObject = {};
        self.get('columns').forEach(function(column, index){
          mylocalObject[self.get('columns')[index].valuePath] = row[index];
        });
        rowArr.push(mylocalObject);
      });
      return rowArr;
    }
    return rowArr;
  }),

  filteredColumns: Ember.computed('columns', 'columnFilter', function() {
    if (!Ember.isEmpty(this.get('columnFilter'))) {
      return this.get('columns').filter((item) => item.label.indexOf(this.get('columnFilter')) > -1 );
    }
    return this.get('columns');
  }),

  showSaveHdfsModal:false,

  showDownloadCsvModal: false,

  isExportResultSuccessMessege:false,

  isSaveHdfsErrorMessege:false,


  actions: {
    onScrolledToBottom() {
      //console.log('hook for INFINITE scroll');
    },

    onColumnClick(column) {
      console.log('column',column);
    },
    goNextPage(payloadTitle){
      this.sendAction('goNextPage', payloadTitle);
    },
    goPrevPage(payloadTitle){
      this.sendAction('goPrevPage', payloadTitle);
    },
    expandQueryResultPanel(){
      this.sendAction('expandQueryResultPanel');
    },

    openSaveHdfsModal(){
      this.set('showSaveHdfsModal',true);
      this.set('isExportResultSuccessMessege',false);
      this.set('isExportResultFailureMessege',false);
    },

    closeSaveHdfsModal(){
      this.set('showSaveHdfsModal',false);
      this.set('isExportResultSuccessMessege',false);
      this.set('isExportResultFailureMessege',false);
    },

    openDownloadCsvModal(){
      this.set('showDownloadCsvModal',true);
      this.set('isExportResultSuccessMessege',false);
      this.set('isExportResultFailureMessege',false);
    },

    closeDownloadCsvModal(){
      this.set('showDownloadCsvModal',false);
      this.set('isExportResultSuccessMessege',false);
      this.set('isExportResultFailureMessege',false);
    },

    saveToHDFS(jobId, pathName){
      console.log('saveToHDFS with jobId == ', jobId );
      console.log('saveToHDFS with pathName == ', pathName );
      this.sendAction('saveToHDFS', jobId,  pathName);
    },

    downloadAsCsv(jobId, pathName){
      console.log('downloadAsCsv with jobId == ', jobId );
      console.log('downloadAsCsv with pathName == ', pathName );
      this.sendAction('downloadAsCsv', jobId,  pathName);
    },

    showVisualExplain(){
      this.sendAction('showVisualExplain');
    },

    clearColumnsFilter() {
      this.set('columnFilterText');
    }

  }

});
