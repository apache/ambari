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
import tabs from '../../configs/result-tabs';
import UILoggerMixin from '../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {
  query: Ember.inject.service(),
  jobs: Ember.inject.service(),
  savedQueries: Ember.inject.service(),
  isQueryEdidorPaneExpanded: false,
  isQueryResultPanelExpanded: false,
  globalSettings: '',
  tezViewInfo: Ember.inject.service(),

  beforeModel(params){
    console.log('worksheetId', params.params['queries.query'].worksheetId);
    let existingWorksheets = this.store.peekAll('worksheet');
    existingWorksheets.setEach('selected', false);
  },

  afterModel(model) {
    let dbmodel = this.store.findAll('database');
    if (dbmodel.get('length') > 0) {
      this.selectDatabase(dbmodel);
    }

    this.store.findAll('setting').then((data) => {
      let localStr = '';
      data.forEach(x => {
        localStr = localStr + 'set '+ x.get('key')+ '='+ x.get('value') + '\n';
      });
      this.set('globalSettings', localStr);
    });

    //lastResultRoute
    console.log('lastResultRoute:: ', model.get('lastResultRoute'));
    let lastResultRoute = model.get('lastResultRoute');

    if(Ember.isEmpty(lastResultRoute)){
      if(model.get('jobData').length > 0){
        this.transitionTo('queries.query.results');
      } else {
        this.transitionTo('queries.query');
      }
    } else {
      this.transitionTo('queries.query' + lastResultRoute);
    }
    return dbmodel;
  },

  model(params) {
    let selectedWs = this.store.peekAll('worksheet').filterBy('title', params.worksheetId).get('firstObject');

    if(selectedWs) {
      selectedWs.set('selected', true);
      return selectedWs;
    } else {
      this.transitionTo('queries');
    }
  },

  setupController(controller, model) {

    this._super(...arguments);
    this.get("tezViewInfo").getTezViewInfo();

    let self = this, selectedDb;
    let alldatabases = this.store.peekAll('database');
    controller.set('alldatabases',alldatabases);

    selectedDb = this.checkIfDeafultDatabaseExists(alldatabases);

    let selectedTablesModels =[];
    let selectedMultiDb = [];
    if(selectedDb) {
      selectedTablesModels.pushObject({
        'dbname': selectedDb ,
        'tables': this.store.query('table', {databaseId: selectedDb}),
        'isSelected': true
      })
      selectedMultiDb.pushObject(selectedDb);
    }

    this.store.findAll('file-resource').then((data) => {
      let fileResourceList = [];
      data.forEach(x => {
        let localFileResource = {'id': x.get('id'),
          'name': x.get('name'),
          'path': x.get('path'),
          'owner': x.get('owner')
        };
        fileResourceList.push(localFileResource);
      });
      controller.set('fileResourceList', fileResourceList);
    });

    this.store.findAll('udf').then((data) => {
      let allUDFList = [];
      data.forEach(x => {
        let localUDF = {'id': x.get('id'),
          'name': x.get('name'),
          'classname': x.get('classname'),
          'fileResource': x.get('fileResource'),
          'owner': x.get('owner')
        };
        allUDFList.push(localUDF);
      });
      controller.set('allUDFList', allUDFList);
    });


    controller.set('worksheet', model);

    controller.set('selectedTablesModels',model.get('selectedTablesModels') || selectedTablesModels );
    controller.set('selectedMultiDb', model.get('selectedMultiDb') || selectedMultiDb);

    controller.set('isQueryRunning', model.get('isQueryRunning'));
    controller.set('currentQuery', model.get('query'));
    controller.set('currentJobId', null);
    controller.set('queryResult', model.get('queryResult'));
    controller.set('isJobSuccess', false);
    controller.set('isJobCancelled', false);
    controller.set('isJobCreated', false);

    controller.set('isExportResultSuccessMessege', false);
    controller.set('isExportResultFailureMessege', false);
    controller.set('showSaveHdfsModal', false);

    controller.set('logResults', model.get('logResults') || '');

    controller.set('isVisualExplainQuery', false);
    controller.set('visualExplainJson', model.get('visualExplainJson'));

    controller.set('showWorksheetModal',false);
    controller.set('worksheetModalSuccess',false);
    controller.set('worksheetModalFail',false);

    controller.set('tabs', tabs);

  },
  checkIfDeafultDatabaseExists(alldatabases){
    if(this.get('controller.model').get('selectedDb')) {
      return this.get('controller.model').get('selectedDb');
    }
    let defaultDB = alldatabases.findBy('name', 'default'), selectedDb;
    if(defaultDB) {
      selectedDb = defaultDB.get("name");
      this.get('controller.model').set('selectedDb', selectedDb);
    }
    return selectedDb;
  },
  setSelectedDB(selectedDBs) {
    let selectedDb = this.get('controller.model').get('selectedDb');
    if(selectedDBs && selectedDBs.indexOf(selectedDb) === -1) {
      this.get('controller.model').set('selectedDb', selectedDBs[0]);
    }
    else if(selectedDBs.length === 0) {
      this.get('controller.model').set('selectedDb', null);
    }
  },
  actions: {

    resetDefaultWorksheet(){
      this.get('controller.model').set('queryResult',{'schema' :[], 'rows' :[]});
      this.get('controller.model').set('currentPage',0);
      this.get('controller.model').set('previousPage',-1);
      this.get('controller.model').set('nextPage',1);
      //this.get('controller.model').set('selected',false);
      this.get('controller.model').set('jobData',[]);
      this.get('controller.model').set('currentJobData',null);
      this.get('controller.model').set('queryFile',"");
      this.get('controller.model').set('logFile',"");
      this.get('controller.model').set('logResults',"");
      this.get('controller.model').set('isQueryRunning',false);
      this.get('controller.model').set('isQueryResultContainer',false);
    },

    changeDbHandler(selectedDBs){
      let self = this;
      let selectedTablesModels =[];
      let selectedMultiDb = [];
      this.setSelectedDB(selectedDBs);
      selectedDBs.forEach(function(db, index){
        selectedTablesModels.pushObject(
          {
            'dbname': db ,
            'tables':self.store.query('table', {databaseId: db}),
            'isSelected': (index === 0) ? true :false
          }
        )
        selectedMultiDb.pushObject(db);
      });

      this.get('controller').set('selectedTablesModels', selectedTablesModels );
      this.get('controller.model').set('selectedTablesModels', selectedTablesModels );

      this.get('controller').set('selectedMultiDb', selectedMultiDb );
      this.get('controller.model').set('selectedMultiDb', selectedMultiDb );
    },

    showQueryResultContainer(){
      this.get('controller.model').set('isQueryResultContainer', true);
    },

    showTables(db){
      let self = this;
      Ember.$('#' + db).toggle();
      this.get('controller.model').set('selectedDb', db);
    },

    visualExplainQuery(){
      this.send('executeQuery', true);
    },

    updateQuery(query){
      this.get('controller').set('currentQuery', query);
      this.get('controller.model').set('query', query);
    },

    executeQuery(isVisualExplainQuery){

      let self = this;
      this.get('controller').set('currentJobId', null);

      if(!Ember.isEmpty(isVisualExplainQuery)){
        isVisualExplainQuery = true;
        this.get('controller').set('isVisualExplainQuery', true);
      } else {
        isVisualExplainQuery = false;
        this.get('controller').set('isVisualExplainQuery', false);
      }

      let originalQuery = this.get('controller').get('currentQuery');
      if(Ember.isBlank(originalQuery)) {
        this.get('logger').danger('Query cannot be empty.');
        this.send('resetDefaultWorksheet');
        return;
      }
      let queryInput = originalQuery;

      if (isVisualExplainQuery) {
        queryInput = "";
        let queries = this.get('controller').get('currentQuery').split(";").filter(function (query) {
          if (query && query.trim()) return true;
        });

        for (let i = 0; i < queries.length; i++) {
          if (i == queries.length - 1) {
            if(queries[i].toLowerCase().startsWith("explain formatted ")){
              queryInput += queries[i] + ";";
            } else{
              queryInput += "explain formatted " + queries[i] + ";";
            }
          } else {
            queryInput += queries[i] + ";";
          }
        }
      }

      this.get('controller.model').set('query', originalQuery);


      let dbid = this.get('controller.model').get('selectedDb');
      let worksheetTitle = this.get('controller.model').get('title');

      this.get('controller.model').set('jobData', []);
      self.get('controller.model').set('currentPage', 0);
      self.get('controller.model').set('previousPage', -1 );
      self.get('controller.model').set('nextPage', 1);
      self.get('controller.model').set('queryResult', {'schema' :[], 'rows' :[]});
      self.get('controller.model').set('visualExplainJson', null);


      this.get('controller.model').set('isQueryRunning', true);
      this.get('controller.model').set('isJobCreated',false);

      //this.get('controller').set('queryResult', self.get('controller').get('queryResult'));
      //this.get('controller.model').set('queryResult', self.get('controller').get('queryResult'));

      let globalSettings = this.get('globalSettings');

      this.send('showQueryResultContainer');

      let payload ={
        "title":worksheetTitle,
        "dataBase":dbid,
        "forcedContent":queryInput,
        "referrer":"job",
        "globalSettings":globalSettings};


      this.get('query').createJob(payload).then(function(data) {
        self.get('controller.model').set('currentJobData', data);
        self.get('controller.model').set('queryFile', data.job.queryFile);
        self.get('controller.model').set('logFile', data.job.logFile);
        self.get('controller').set('currentJobId', data.job.id);
        self.get('controller').set('isJobCreated',true);

        self.get('jobs').waitForJobToComplete(data.job.id, 2 * 1000, false)
          .then((status) => {
            self.get('controller').set('isJobSuccess', true);
            self.get('controller').set('isJobCancelled', false);
            self.get('controller').set('isJobCreated', false);
            let jobDetails = self.store.peekRecord('job', data.job.id);
            console.log(jobDetails);
            self.send('getJobResult', data, payload.title, jobDetails);
            self.get('logger').success('Query has been submitted.');
            self.transitionTo('queries.query.loading');

          }, (error) => {
            console.log('error', error);
            self.get('controller').set('isJobSuccess', false);
            self.get('controller').set('isJobCancelled', false);
            self.get('controller').set('isJobCreated', false);
            self.get('logger').danger('Failed to execute query.', self.extractError(error));
            self.send('resetDefaultWorksheet');
          });

      }, function(error) {
        console.log(error);
        self.get('logger').danger('Failed to execute query.', self.extractError(error));
        self.send('resetDefaultWorksheet');
      });
    },

    stopQuery(){
      let jobId = this.get('controller').get('currentJobId');
      this.get('jobs').stopJob(jobId)
        .then( data => this.get('controller').set('isJobCancelled', true));
    },

    getJobResult(data, payloadTitle, jobDetails){
      let self = this;

      let isVisualExplainQuery = this.get('controller').get('isVisualExplainQuery');

      let jobId = data.job.id;

      let currentPage = this.get('controller.model').get('currentPage');
      let previousPage = this.get('controller.model').get('previousPage');
      let nextPage = this.get('controller.model').get('nextPage');

      this.get('query').getJob(jobId, true).then(function(data) {

        let existingWorksheets = self.get('store').peekAll('worksheet');
        let myWs = null;
        if(existingWorksheets.get('length') > 0) {
          myWs = existingWorksheets.filterBy('title', payloadTitle).get('firstObject');
        }
        if(!Ember.isBlank(jobDetails.get("dagId"))) {
          self.get('controller.model').set('tezUrl', self.get("tezViewInfo").getTezViewURL() + jobDetails.get("dagId"));
        }
        myWs.set('queryResult', data);
        myWs.set('isQueryRunning', false);
        myWs.set('hasNext', data.hasNext);

        let localArr = myWs.get("jobData");
        localArr.push(data);
        myWs.set('jobData', localArr);
        myWs.set('currentPage', currentPage+1);
        myWs.set('previousPage', previousPage + 1 );
        myWs.set('nextPage', nextPage + 1);

        if(isVisualExplainQuery){
          self.send('showVisualExplain', payloadTitle);
        } else {
          self.get('controller.model').set('visualExplainJson', null);
        }

        if( self.paramsFor('queries.query').worksheetId == payloadTitle){
          self.transitionTo('queries.query.results');
        }

      }, function(error) {
        console.log('error' , error);
        self.get('logger').danger('Failed to execute query.', self.extractError(error));
      });
    },

    showVisualExplain(payloadTitle){
       if( this.paramsFor('queries.query').worksheetId == payloadTitle){
         Ember.run.later(() => {
           this.transitionTo('queries.query.visual-explain');
         }, 1);
       }
    },

    goNextPage(payloadTitle){

      let currentPage = this.get('controller.model').get('currentPage');
      let previousPage = this.get('controller.model').get('previousPage');
      let nextPage = this.get('controller.model').get('nextPage');
      let totalPages = this.get('controller.model').get("jobData").length;

      if(nextPage > totalPages){ //Pages from server
        var self = this;
        var data = this.get('controller.model').get('currentJobData');
        let jobId = data.job.id;

        this.get('query').getJob(jobId, false).then(function(data) {
          self.get('controller.model').set('queryResult', data);
          self.get('controller.model').set('isQueryRunning', false);
          self.get('controller.model').set('hasNext', data.hasNext);
          self.get('controller.model').set('hasPrevious', true);

          let localArr = self.get('controller.model').get("jobData");
          localArr.push(data);

          self.get('controller.model').set('jobData', localArr);
          self.get('controller.model').set('currentPage', currentPage+1);
          self.get('controller.model').set('previousPage', previousPage + 1 );
          self.get('controller.model').set('nextPage', nextPage + 1);
        }, function(error) {
            console.log('error' , error);
        });
      } else {
        //Pages from cache object
        this.get('controller.model').set('currentPage', currentPage+1);
        this.get('controller.model').set('previousPage', previousPage + 1 );
        this.get('controller.model').set('nextPage', nextPage + 1);
        this.get('controller.model').set('hasNext', this.get('controller.model').get('jobData')[this.get('controller.model').get('currentPage')-1].hasNext);
        this.get('controller.model').set('hasPrevious', (this.get('controller.model').get('currentPage') > 1) ? true : false );
        this.get('controller.model').set('queryResult', this.get('controller.model').get("jobData")[this.get('controller.model').get('currentPage')-1] );
      }

      let existingWorksheets = this.get('store').peekAll('worksheet');
      let myWs = null;
      if(existingWorksheets.get('length') > 0) {
        myWs = existingWorksheets.filterBy('title', payloadTitle).get('firstObject');
      }

      this.transitionTo('queries.query.loading');

      Ember.run.later(() => {
        this.transitionTo('queries.query.results', myWs);
      }, 1 * 1000);
    },

    goPrevPage(payloadTitle){
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
        this.get('controller.model').set('hasNext', true);
        this.get('controller.model').set('hasPrevious', (this.get('controller.model').get('currentPage') > 1) ? true : false );
      }

      let existingWorksheets = this.get('store').peekAll('worksheet');
      let myWs = null;
      if(existingWorksheets.get('length') > 0) {
        myWs = existingWorksheets.filterBy('title', payloadTitle).get('firstObject');
      }

      this.transitionTo('queries.query.loading');

      Ember.run.later(() => {
        this.transitionTo('queries.query.results', myWs);
      }, 1 * 1000);
    },

    openWorksheetModal(){
      this.get('controller').set('showWorksheetModal', true);
    },

    saveWorksheetModal(){
      console.log('I am in saveWorksheetModal');
      let newTitle = $('#worksheet-title').val();

      let currentQuery = this.get('controller.model').get('query');
      let selectedDb = this.get('controller.model').get('selectedDb');
      let owner = this.get('controller.model').get('owner');
      let queryFile = this.get('controller.model').get('queryFile');
      let logFile = this.get('controller.model').get('logFile');

      let payload = {"title" : newTitle,
        "dataBase": selectedDb,
        "owner" : owner,
        "shortQuery" : (currentQuery.length > 0) ? currentQuery : ";",
        "queryFile" : queryFile,
        "logFile" : logFile};

      let newSaveQuery = this.get('store').createRecord('saved-query',
        { dataBase:selectedDb,
          title:newTitle,
          queryFile: queryFile,
          owner: owner,
          shortQuery: (currentQuery.length > 0) ? currentQuery : ";"
        });


      newSaveQuery.save().then((data) => {
        console.log('saved query saved');

        this.get('controller.model').set('title', newTitle);
        this.get('controller').set('worksheetModalSuccess', true);

        Ember.run.later(() => {
          this.get('controller').set('showWorksheetModal', false);
        }, 2 * 1000);

      });

    },

    closeWorksheetModal(){
      this.get('controller').set('showWorksheetModal', false);
    },

    saveToHDFS(jobId, path){

      console.log('saveToHDFS query route with jobId == ', jobId);
      console.log('saveToHDFS query route with path == ', path);

      this.get('query').saveToHDFS(jobId, path)
        .then((data) => {
          console.log('successfully saveToHDFS', data);
          this.get('controller').set('isExportResultSuccessMessege', true);
          this.get('controller').set('isExportResultFailureMessege', false);

          Ember.run.later(() => {
            this.get('controller').set('showSaveHdfsModal', false);
          }, 2 * 1000);

        }, (error) => {
          console.log("Error encountered", error);
          this.get('controller').set('isExportResultFailureMessege', true);
          this.get('controller').set('isExportResultSuccessMessege', false);

          Ember.run.later(() => {
            this.get('controller').set('showSaveHdfsModal', false);
          }, 2 * 1000);

        });
    },

    downloadAsCsv(jobId, path){

      console.log('downloadAsCsv query route with jobId == ', jobId);
      console.log('downloadAsCsv query route with path == ', path);

      let downloadAsCsvUrl = this.get('query').downloadAsCsv(jobId, path) || '';

      this.get('controller').set('showDownloadCsvModal', false);
      window.open(downloadAsCsvUrl);

    },

    expandQueryEdidorPanel(){
      if(!this.get('isQueryEdidorPaneExpanded')){
        this.set('isQueryEdidorPaneExpanded', true);
      } else {
        this.set('isQueryEdidorPaneExpanded', false);
      }
      Ember.$('.query-editor-panel').toggleClass('query-editor-full-width');
      Ember.$('.database-panel').toggleClass("hide");
    },

    expandQueryResultPanel(){
      if(!this.get('isQueryResultPanelExpanded')){
        if(!this.get('isQueryEdidorPaneExpanded')){
          Ember.$('.query-editor-container').addClass("hide");
          Ember.$('.database-panel').addClass("hide");
          Ember.$('.query-editor-panel').addClass('query-editor-full-width');
        } else {

          Ember.$('.query-editor-container').addClass("hide");
        }
        this.set('isQueryResultPanelExpanded', true);
      } else {
        if(!this.get('isQueryEdidorPaneExpanded')){
          Ember.$('.query-editor-container').removeClass("hide");
          Ember.$('.database-panel').removeClass("hide");
          Ember.$('.query-editor-panel').removeClass('query-editor-full-width');
        } else {
          Ember.$('.query-editor-container').removeClass("hide");
        }
        this.set('isQueryResultPanelExpanded', false);
      }
    },

    adjustPanelSize(){
      let isFullHeight = ($(window).height() ==(parseInt(Ember.$('.ember-light-table').css('height'), 10)) ) || false;
      if(!isFullHeight){
        Ember.$('.ember-light-table').css('height', '100vh');
      }else {
        Ember.$('.ember-light-table').css('height', '70vh');
      }
    },

    createQuery(udfName, udfClassname, fileResourceName, fileResourcePath){
      let query = "add jar "+ fileResourcePath + ";\ncreate temporary function " + udfName + " as '"+ udfClassname+ "';";
      this.get('controller').set('currentQuery', query);
      this.get('controller.model').set('query', query );
    }

  }

});
