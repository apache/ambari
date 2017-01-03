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

export default Ember.Route.extend({

  model() {
    return this.store.findAll('database');
  },

  query: Ember.inject.service(),

  afterModel(model) {
    if (model.get('length') > 0) {
      this.selectDatabase(model);
    }
  },

  setupController(controller, model) {

    let sortedModel = model.sortBy('name');
    let selectedModel = sortedModel.filterBy('selected', true).get('firstObject');
    sortedModel.removeObject(selectedModel);
    let finalList = [];
    let selectedDB = [];

    finalList.pushObject(selectedModel);
    finalList.pushObjects(sortedModel);
    controller.set('model', finalList);

    selectedDB.pushObject(selectedModel);
    controller.set('selectedModel',selectedDB);

    let selecteDBName = selectedModel.get('name');

    let self = this;
    let selectedTablesModels =[];

    selectedTablesModels.pushObject(
      {
        'dbname': selecteDBName ,
        'tables': this.store.query('table', {databaseId: selecteDBName})
      }
    )

    controller.set('selectedTablesModels',selectedTablesModels );
    controller.set('currentQuery', '');
    controller.set('isQueryRunning', false);
    controller.set('defaultQueryResult', {'schema' :[], 'rows' :[]});
    controller.set('queryResult', controller.get('defaultQueryResult'));
    controller.set('hidePreviousButton', true);

    //For Pagination
    localStorage.setItem("jobData", JSON.stringify([]));
    controller.set('prevPage', -1 );
    controller.set('currentPage', 0 );
    controller.set('nextPage', 1 );
    controller.set('currentJobData', null );

  },

  selectDatabase(model) {
    // check if default database is present
    let toSelect = model.findBy('name', 'default');
    if (Ember.isEmpty(toSelect)) {
      let sortedModel = model.sortBy('name');
      toSelect = sortedModel.get('firstObject');
    }
    toSelect.set('selected', true);
  },

  actions: {

    xyz(selectedDBs){

      let self = this;
      let selectedTablesModels =[];

      selectedDBs.forEach(function(db){
        selectedTablesModels.pushObject(
          {
            'dbname': db ,
            'tables':self.store.query('table', {databaseId: db})
          }
        )
      });

      this.get('controller').set('selectedTablesModels', selectedTablesModels );

    },

    tableSelected(){
      console.log('I am in tableSelected');
    },

    showTables(db){
      //should we do this by writing a seperate component.
      $('.collapse').hide();
      $('#' + db).toggle();
    },

    notEmptyDialogClosed() {
      this.get('controller').set('databaseNotEmpty', false);
      this.get('controller').set('databaseName', undefined);
    },

    executeQuery(isFirstCall){

      let self = this;
      let queryInput = this.get('controller').get('currentQuery');
      let dbid = this.get('controller').get('selectedTablesModels')[0]['dbname']

      self.get('controller').set('isQueryRunning', true);
      self.get('controller').set('queryResult', self.get('controller').get('defaultQueryResult'));

      let payload ={
        "title":"Worksheet",
        "hiveQueryId":null,
        "queryFile":null,
        "owner":null,
        "dataBase":dbid,
        "status":null,
        "statusMessage":null,
        "dateSubmitted":null,
        "forcedContent":queryInput,
        "logFile":null,
        "dagName":null,
        "dagId":null,
        "sessionTag":null,
        "statusDir":null,
        "referrer":"job",
        "confFile":null,
        "globalSettings":""};


      this.get('query').createJob(payload).then(function(data) {
        // applying a timeout otherwise it goes for status code 409, although that condition is also handled in the code.
        setTimeout(function(){
          self.get('controller').set('currentJobData', data);
          self.send('getJob', data);
        }, 2000);
      }, function(reason) {
        console.log(reason);
      });

    },
    getJob(data){

      var self = this;
      var data = data;

      let jobId = data.job.id;
      let dateSubmitted = data.job.dateSubmitted;

      this.get('query').getJob(jobId, dateSubmitted, true).then(function(data) {
        // on fulfillment
        console.log('getJob route', data );
        self.get('controller').set('queryResult', data);
        self.get('controller').set('isQueryRunning', false);

        let localArr = JSON.parse(localStorage.getItem("jobData"));
        localArr.push(data);
        localStorage.setItem("jobData", JSON.stringify(localArr));

        self.get('controller').set('currentPage', localArr.length);
        self.get('controller').set('prevPage', localArr.length-1);


      }, function(reason) {
        // on rejection
        console.log('reason' , reason);

        if( reason.errors[0].status == 409 ){
          setTimeout(function(){
            self.send('getJob',data);
          }, 2000);
        }
      });

    },
    goNextPage(){

      let nextPage = this.get('controller').get('nextPage');
      let totalPages = JSON.parse(localStorage.getItem("jobData")).length;

      if(nextPage >= totalPages){
        var self = this;
        var data = this.get('controller').get('currentJobData');

        let jobId = data.job.id;
        let dateSubmitted = data.job.dateSubmitted;


        this.get('query').getJob(jobId, dateSubmitted, false).then(function(data) {
          // on fulfillment
          console.log('getJob route', data );
          self.get('controller').set('queryResult', data);
          self.get('controller').set('isQueryRunning', false);
          self.get('controller').set('hidePreviousButton', false);

          let localArr = JSON.parse(localStorage.getItem("jobData"));
          localArr.push(data);


          localStorage.setItem("jobData", JSON.stringify(localArr));


          self.get('controller').set('currentPage', localArr.length);
          self.get('controller').set('prevPage', localArr.length-1);

          self.get('controller').set('nextPage', localArr.length+1);

        }, function(reason) {
          // on rejection
          console.log('reason' , reason);

          if( reason.errors[0].status == 409 ){
            setTimeout(function(){
              self.send('getJob',data);
            }, 2000);
          }
        });
      } else {

        let currentPage = this.get('controller').get('currentPage');
        let prevPage = this.get('controller').get('prevPage');
        let nextPage = this.get('controller').get('nextPage');
        let totalPages = JSON.parse(localStorage.getItem("jobData")).length;

        if(nextPage < totalPages ){
          this.get('controller').set('currentPage', currentPage+1 );
          this.get('controller').set('prevPage', prevPage + 1 );
          this.get('controller').set('nextPage', nextPage + 1);

          this.get('controller').set('hidePreviousButton', false);

          this.get('controller').set('queryResult', JSON.parse(localStorage.getItem("jobData"))[this.get('controller').get('currentPage')] );
        } else {

          console.log('upper limit exceed');
          this.send('goNextPage');
        }
      }


    },
    goPrevPage(){

      let currentPage = this.get('controller').get('currentPage');
      let prevPage = this.get('controller').get('prevPage');
      let nextPage = this.get('controller').get('nextPage');
      let totalPages = JSON.parse(localStorage.getItem("jobData")).length;

      if(prevPage > -1){
        this.get('controller').set('currentPage', currentPage-1 );
        this.get('controller').set('prevPage', prevPage - 1 );
        this.get('controller').set('nextPage', this.get('controller').get('currentPage')+1);

        this.get('controller').set('queryResult', JSON.parse(localStorage.getItem("jobData"))[this.get('controller').get('currentPage')] );
      } else {
        //console.log('previous limit over');
        this.get('controller').set('hidePreviousButton', true);
      }
    },

    expandQueryEdidorPanel(){
      Ember.$('.query-editor-panel').toggleClass('query-editor-full-width');
      Ember.$('.database-panel').toggleClass("hide");
    },

    expandQueryResultPanel(){

      Ember.$('.query-editor-panel').toggleClass('query-editor-full-width');
      Ember.$('.query-editor-container').toggleClass("hide");
      Ember.$('.database-panel').toggleClass("hide");
      this.send('adjustPanelSize');
    },

    adjustPanelSize(){
      let isFullHeight = ($(window).height() ==(parseInt(Ember.$('.ember-light-table').css('height'), 10)) ) || false;
      if(!isFullHeight){
        Ember.$('.ember-light-table').css('height', '100vh');
      }else {
        Ember.$('.ember-light-table').css('height', '70vh');
      }
    }

  }
});
