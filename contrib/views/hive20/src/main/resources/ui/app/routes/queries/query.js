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

  query: Ember.inject.service(),

  beforeModel(){
    let existingWorksheets = this.store.peekAll('worksheet');
    existingWorksheets.forEach((worksheet) => {
      worksheet.set('selected', false);
    });
  },

  afterModel(model) {

    let dbmodel = this.store.findAll('database');

    if (dbmodel.get('length') > 0) {
      this.selectDatabase(dbmodel);
    }
  },

  model(params) {
    let selectedWs = this.modelFor('queries').filterBy('title', params.worksheetId).get('firstObject');
    selectedWs.set('selected', true);
    return selectedWs;
  },

  setupController(controller, model) {

    this._super(...arguments);

    let self = this;
    let alldatabases = this.store.findAll('database');

    controller.set('alldatabases',alldatabases);

    let selecteDBName = model.get('selectedDb');

    let selectedTablesModels =[];
    let selectedMultiDb = [];

    selectedTablesModels.pushObject(
      {
        'dbname': selecteDBName ,
        'tables': this.store.query('table', {databaseId: selecteDBName})
      }
    )

    selectedMultiDb.pushObject(selecteDBName);

    controller.set('worksheet', model);
    controller.set('selectedTablesModels',this.get('controller.model').get('selectedTablesModels') || selectedTablesModels );

    controller.set('selectedMultiDb', this.get('controller.model').get('selectedMultiDb') || selectedMultiDb);
    controller.set('isQueryRunning', false);
    controller.set('currentQuery', model.get('query'));
    controller.set('queryResult', model.get('queryResult'));

  },


  actions: {

    changeDbHandler(selectedDBs){

      let self = this;
      let selectedTablesModels =[];
      let selectedMultiDb = [];

      selectedDBs.forEach(function(db){
        selectedTablesModels.pushObject(
          {
            'dbname': db ,
            'tables':self.store.query('table', {databaseId: db})
          }
        )
        selectedMultiDb.pushObject(db);

      });

      this.get('controller').set('selectedTablesModels', selectedTablesModels );
      this.get('controller.model').set('selectedTablesModels', selectedTablesModels );

      this.get('controller').set('selectedMultiDb', selectedMultiDb );
      this.get('controller.model').set('selectedMultiDb', selectedMultiDb );


    },

    showTables(db){
      //should we do this by writing a seperate component.
      $('#' + db).toggle();
      this.get('controller.model').set('selectedDb', db);
    },

    executeQuery(isFirstCall){
      let self = this;
      let queryInput = this.get('controller').get('currentQuery');
      this.get('controller.model').set('query', queryInput);

      let dbid = this.get('controller.model').get('selectedDb');
      let worksheetTitle = this.get('controller.model').get('title');

      self.get('controller').set('isQueryRunning', true);

      //Making the result set emply every time query runs.
      self.get('controller').set('queryResult', self.get('controller').get('queryResult'));
      self.get('controller.model').set('queryResult', self.get('controller').get('queryResult'));

      let payload ={
        "title":worksheetTitle,
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
          self.get('controller.model').set('currentJobData', data);
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

      let currentPage = this.get('controller.model').get('currentPage');
      let previousPage = this.get('controller.model').get('previousPage');
      let nextPage = this.get('controller.model').get('nextPage');

      this.get('query').getJob(jobId, dateSubmitted, true).then(function(data) {
        // on fulfillment
        console.log('getJob route', data );

        self.get('controller').set('queryResult', data);
        self.get('controller.model').set('queryResult', data);
        self.get('controller').set('isQueryRunning', false);

        let localArr = self.get('controller.model').get("jobData");
        localArr.push(data);
        self.get('controller.model').set('jobData', localArr);
        self.get('controller.model').set('currentPage', currentPage+1);
        self.get('controller.model').set('previousPage', previousPage + 1 );
        self.get('controller.model').set('nextPage', nextPage + 1);

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

    updateQuery(){
      console.log('I am in update query.')
    },

    goNextPage(){

      let currentPage = this.get('controller.model').get('currentPage');
      let previousPage = this.get('controller.model').get('previousPage');
      let nextPage = this.get('controller.model').get('nextPage');
      let totalPages = this.get('controller.model').get("jobData").length;

      if(nextPage > totalPages){ //Pages from server
        var self = this;
        var data = this.get('controller.model').get('currentJobData');
        let jobId = data.job.id;
        let dateSubmitted = data.job.dateSubmitted;

        this.get('query').getJob(jobId, dateSubmitted, false).then(function(data) {
          // on fulfillment
          console.log('getJob route', data );
          self.get('controller').set('queryResult', data);
          self.get('controller.model').set('queryResult', data);
          self.get('controller').set('isQueryRunning', false);
          self.get('controller.model').set('hidePreviousButton', false);

          let localArr = self.get('controller.model').get("jobData");
          localArr.push(data);

          self.get('controller.model').set('jobData', localArr);
          self.get('controller.model').set('currentPage', currentPage+1);
          self.get('controller.model').set('previousPage', previousPage + 1 );
          self.get('controller.model').set('nextPage', nextPage + 1);
        }, function(reason) {
          // on rejection
          console.log('reason' , reason);

          if( reason.errors[0].status == 409 ){
            setTimeout(function(){
              self.send('getJob',data);
            }, 2000);
          }
        });
      } else { //Pages from cache object
          this.get('controller.model').set('currentPage', currentPage+1);
          this.get('controller.model').set('previousPage', previousPage + 1 );
          this.get('controller.model').set('nextPage', nextPage + 1);
          this.get('controller.model').set('hidePreviousButton', false);
          this.get('controller').set('queryResult', this.get('controller.model').get("jobData")[this.get('controller.model').get('currentPage')-1] );
          this.get('controller.model').set('queryResult', this.get('controller.model').get("jobData")[this.get('controller.model').get('currentPage')-1] );
      }
    },

    goPrevPage(){
      let currentPage = this.get('controller.model').get('currentPage');
      let previousPage = this.get('controller.model').get('previousPage');
      let nextPage = this.get('controller.model').get('nextPage');
      let totalPages = this.get('controller.model').get("jobData").length;

      if(previousPage > 0){
        this.get('controller.model').set('currentPage', currentPage-1 );
        this.get('controller.model').set('previousPage', previousPage - 1 );
        this.get('controller.model').set('nextPage', nextPage-1);

        this.get('controller').set('queryResult', this.get('controller.model').get("jobData")[this.get('controller.model').get('currentPage') -1 ]);
        this.get('controller.model').set('queryResult', this.get('controller.model').get("jobData")[this.get('controller.model').get('currentPage') -1 ]);
      } else {
        this.get('controller.model').set('hidePreviousButton', true);
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
