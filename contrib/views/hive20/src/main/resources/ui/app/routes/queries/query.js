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
        localStr = localStr + 'set '+ x.get('key')+ '='+ x.get('value') + ';\n';
      });
      this.set('globalSettings', localStr);
    });

    //lastResultRoute
    console.log('lastResultRoute:: ', model.get('lastResultRoute'));
    let lastResultRoute = model.get('lastResultRoute');



    if(Ember.isEmpty(lastResultRoute)){
      if(model.get('jobData') && model.get('jobData').length > 0){
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
    let selectedWs = this.store.peekAll('worksheet').filterBy('id', params.worksheetId.toLowerCase()).get('firstObject');

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
      });
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
    controller.set('currentJobId', model.get('currentJobId'));
    controller.set('queryResult', model.get('queryResult'));
    controller.set('isJobSuccess', model.get('isJobSuccess'));
    controller.set('isJobCancelled', model.get('isJobCancelled'));
    controller.set('isJobCreated', model.get('isJobCreated'));
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
    let previewJobData = this.get('controller').get('previewJobData'), ctrlr = this.get('controller'), ctrlrModel = this.get('controller.model');


    if(previewJobData) {

      ctrlrModel.set('isJobSuccess', true);
      ctrlrModel.set('isJobCancelled', false);
      ctrlrModel.set('isJobCreated', false);
      ctrlr.set('isJobSuccess', true);
      ctrlr.set('isJobCancelled', false);
      ctrlr.set('isJobCreated', false);
      ctrlrModel.set('currentJobId', previewJobData.id);
      this.get('controller.model').set('currentJobData', {job: previewJobData});

      this.getJobResult({job: previewJobData}, previewJobData.title, Ember.Object.create({name: 'query'}), ctrlrModel, true);
    }
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
  closeWorksheetAfterSave(){
    let tabDataToClose = this.get('controller.model').get('tabDataToClose');
    if(tabDataToClose) {
      this.send('closeWorksheet', tabDataToClose.index, tabDataToClose.id);
    }
  },

  getJobResult(data, payloadTitle, jobDetails, ctrlrModel, isDataPreview){
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
        if(isDataPreview) {
          myWs = existingWorksheets.filterBy('id', jobId).get('firstObject');
        } else {
          myWs = existingWorksheets.filterBy('id', payloadTitle).get('firstObject');
        }
      }
      if(!Ember.isBlank(jobDetails.get("dagId"))) {
        let tezData = self.get("tezViewInfo").getTezViewData();
        if(tezData && tezData.error) {
          self.get('controller.model').set('tezError', tezData.errorMsg);
        } else if(tezData.tezUrl) {
          self.get('controller.model').set('tezUrl', tezData.tezUrl + jobDetails.get("dagId"));
        }
      }
      myWs.set('queryResult', data);
      myWs.set('isQueryRunning', false);
      myWs.set('hasNext', data.hasNext);
      self.get('controller.model').set('queryResult', data);


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

        if( self.paramsFor('queries.query').worksheetId && (self.paramsFor('queries.query').worksheetId.toLowerCase() === payloadTitle) || (isDataPreview && self.paramsFor('queries.query').worksheetId && (self.paramsFor('queries.query').worksheetId.toLowerCase() === jobId))){
          self.transitionTo('queries.query.loading');

          Ember.run.later(() => {
            self.transitionTo('queries.query.results');
          }, 1 * 100);
        }
      }

    }, function(error) {
      console.log('error' , error);
      if(!isDataPreview){
        self.get('logger').danger('Failed to execute query.', self.extractError(error));
      } else {
        self.get('logger').danger('Query expired. Please execute the query again.', self.extractError(error));
      }
      self.send('resetDefaultWorksheet', ctrlrModel);
    });
  },

  actions: {

    resetDefaultWorksheet(currModel){
      if(!currModel) {
        currModel = this.get('controller.model');
      }
      currModel.set('queryResult',{'schema' :[], 'rows' :[]});
      currModel.set('currentPage',0);
      currModel.set('previousPage',-1);
      currModel.set('nextPage',1);
      //this.get('controller.model').set('selected',false);
      currModel.set('jobData',[]);
      currModel.set('currentJobData',null);
      currModel.set('queryFile',"");
      currModel.set('logFile',"");
      currModel.set('logResults',"");
      currModel.set('isQueryRunning',false);
      currModel.set('isQueryResultContainer',false);
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
        );
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
      Ember.$('#' + db).toggle();
      this.get('controller.model').set('selectedDb', db);
    },

    visualExplainQuery(){
      this.send('executeQuery', true);
    },

    updateQuery(query){
      this.get('controller.model').set('query', query);
      if(Ember.isBlank(query)){
        this.get('controller.model').set('isQueryDirty', false);
      } else if(this.get('controller').get('currentQuery').indexOf(query) !== 0){
        this.get('controller.model').set('isQueryDirty', true);
      }
      this.get('controller').set('currentQuery', query);
    },

    executeQuery(isVisualExplainQuery){

      let self = this, ctrlr = self.get('controller'), ctrlrModel = self.get('controller.model');
      this.get('controller.model').set('currentJobId', null);
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
        this.send('resetDefaultWorksheet', ctrlrModel);
        return;
      }
      let queryInput = originalQuery;

      if (isVisualExplainQuery) {
        queryInput = "";
        let queries = this.get('controller').get('currentQuery').split(";").filter(function (query) {
          if (query && query.trim()) {
            return true;
          }
        });

        for (let i = 0; i < queries.length; i++) {
          if (i === queries.length - 1) {
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
      ctrlrModel.set('isJobCreated',false);
      ctrlr.set('isJobCreated',false);

      //this.get('controller').set('queryResult', self.get('controller').get('queryResult'));
      //this.get('controller.model').set('queryResult', self.get('controller').get('queryResult'));

      let globalSettings = this.get('globalSettings');

      let forcedContent = globalSettings + queryInput;
      this.send('showQueryResultContainer');

      let payload ={
        "id":this.get('controller.model').get('id'),
        "title":worksheetTitle,
        "dataBase":dbid,
        "forcedContent":forcedContent,
        "referrer":"job",
        "globalSettings":globalSettings};


      this.get('query').createJob(payload).then(function(data) {
        self.get('controller.model').set('currentJobData', data);
        self.get('controller.model').set('queryFile', data.job.queryFile);
        self.get('controller.model').set('logFile', data.job.logFile);
        self.get('controller').set('currentJobId', data.job.id);
        self.get('controller.model').set('currentJobId', data.job.id);
        ctrlrModel.set('isJobCreated',true);
        ctrlr.set('isJobCreated',true);

        self.get('jobs').waitForJobToComplete(data.job.id, 2 * 1000, false)
          .then((status) => {
            ctrlrModel.set('isJobSuccess', true);
            ctrlrModel.set('isJobCancelled', false);
            ctrlrModel.set('isJobCreated', false);
            ctrlr.set('isJobSuccess', true);
            ctrlr.set('isJobCancelled', false);
            ctrlr.set('isJobCreated', false);
            let jobDetails = self.store.peekRecord('job', data.job.id);
            console.log(jobDetails);
            self.getJobResult(data, payload.id, jobDetails, ctrlrModel);
            self.get('logger').success('Query has been submitted.');

          }, (error) => {
            console.log('error', error);
            ctrlrModel.set('isJobSuccess', false);
            ctrlrModel.set('isJobCancelled', false);
            ctrlrModel.set('isJobCreated', false);
            ctrlr.set('isJobSuccess', false);
            ctrlr.set('isJobCancelled', false);
            ctrlr.set('isJobCreated', false);
            self.get('logger').danger('Failed to execute query.', self.extractError(error));
            self.send('resetDefaultWorksheet', ctrlrModel);
          });

      }, function(error) {
        console.log(error);
        self.get('logger').danger('Failed to execute query.', self.extractError(error));
        self.send('resetDefaultWorksheet', ctrlrModel);
      });
    },

    stopQuery(){
      Ember.run.later(() => {
        let jobId = this.get('controller').get('currentJobId'), self = this, ctrlr = self.get('controller'), ctrlrModel = self.get('controller.model');
        this.get('jobs').stopJob(jobId)
          .then( data => {
             this.get('controller').set('isJobCancelled', true);
          }).catch(function (response) {
             self.get('controller').set('isJobCancelled', true);
          });
      }, 1000);
    },

    showVisualExplain(payloadTitle){
       if( this.paramsFor('queries.query').worksheetId && this.paramsFor('queries.query').worksheetId.toLowerCase() === payloadTitle){
         this.transitionTo('queries.query.loading');
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
        var previewJobData = this.get('controller').get('previewJobData');
        if(previewJobData) {
          myWs = existingWorksheets.filterBy('id', previewJobData.id).get('firstObject');
        } else {
          myWs = existingWorksheets.filterBy('id', payloadTitle.toLowerCase()).get('firstObject');
        }
      }

      this.transitionTo('queries.query.loading');

      Ember.run.later(() => {
        this.transitionTo('queries.query.results', myWs);
      }, 1 * 100);
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
        var previewJobData = this.get('controller').get('previewJobData');
        if(previewJobData) {
          myWs = existingWorksheets.filterBy('id', previewJobData.id).get('firstObject');
        } else {
          myWs = existingWorksheets.filterBy('id', payloadTitle.toLowerCase()).get('firstObject');
        }

      }

      this.transitionTo('queries.query.loading');

      Ember.run.later(() => {
        this.transitionTo('queries.query.results', myWs);
      }, 1 * 100);
    },
    confirmWorksheetClose(index, id) {
      let existingWorksheets = this.store.peekAll('worksheet');
      let selectedWorksheet = existingWorksheets.filterBy('id', id.toLowerCase()).get('firstObject');
      if(selectedWorksheet.get("isQueryDirty")){
        this.transitionTo('queries.query', id);
      }
      Ember.run.later(() => {

      if(this.get('controller.model').get('isQueryDirty')) {
        this.get('controller.model').set('tabDataToClose', {index:index, id:id});
        this.send('openWorksheetModal');
      } else {
         this.send('closeWorksheet', index, id);
      }
      }, 1 * 100);
    },
    closeWorksheet(index, id) {
      let existingWorksheets = this.store.peekAll('worksheet');
      let selectedWorksheet = existingWorksheets.filterBy('id', id.toLowerCase()).get('firstObject');
      this.store.unloadRecord(selectedWorksheet);
      this.controllerFor('queries').set('worksheets', this.store.peekAll('worksheet'));
      let idToTransition = 0;
      if(selectedWorksheet.get('selected')) {
        if(index){
          idToTransition = existingWorksheets.get('content')[parseInt(index)-1].id;
        } else {
          idToTransition = existingWorksheets.get('content')[1].id;
        }
        this.transitionTo('queries.query', idToTransition);
      } else {
        idToTransition = existingWorksheets.get('content')[existingWorksheets.get('length')-1].id;
      }
    },
    openWorksheetRenameModal(title){
      let wf = this.store.peekAll('worksheet').filterBy('id', this.paramsFor('queries.query').worksheetId.toLowerCase()).get('firstObject');
      this.get('controller').set('worksheetTitle', title);
      this.get('controller').set('showWorksheetRenameModal', true);
      this.get('controller').set('renameWorksheetModalSuccess', false);
    },
    closeRenameWorksheetModal() {
      this.get('controller').set('showWorksheetRenameModal', false);
    },
    renameWorksheetModal(){
      let wf = this.store.peekAll('worksheet').filterBy('id', this.paramsFor('queries.query').worksheetId.toLowerCase()).get('firstObject');
      let newTitle = Ember.$('#worksheet-title').val();
      if(wf) {
        wf.set('title', newTitle);
        this.get('controller').set('renameWorksheetModalSuccess', true);
     } else {
        this.get('controller').set('renameWorksheetModalFail', true);
      }

      Ember.run.later(() => {
        this.get('controller').set('showWorksheetRenameModal', false);
      }, 2 * 1000);
    },

    openWorksheetModal(){
      let originalQuery = this.get('controller').get('currentQuery');
      if(Ember.isBlank(originalQuery)) {
        this.get('logger').danger('Query cannot be empty.');
        this.send('resetDefaultWorksheet', this.get('controller.model'));
        return;
      }
      let wf = this.store.peekAll('worksheet').filterBy('id', this.paramsFor('queries.query').worksheetId.toLowerCase()).get('firstObject');
      this.get('controller').set('worksheetTitle', wf.get('title'));
      this.get('controller').set('showWorksheetModal', true);
    },

    saveWorksheetModal(){
      console.log('I am in saveWorksheetModal');
      let newTitle = Ember.$('#worksheet-title').val();

      let currentQuery = this.get('controller.model').get('query');
      let selectedDb = this.get('controller.model').get('selectedDb');
      let owner = this.get('controller.model').get('owner');
      let queryFile = this.get('controller.model').get('queryFile');
      let logFile = this.get('controller.model').get('logFile');
      let shortQuery = (currentQuery.length > 0) ? currentQuery : ";";
      let savedQueryId = this.get('controller.model').get('id')

      let payload = {"title" : newTitle,
        "dataBase": selectedDb,
        "owner" : owner,
        "shortQuery" : (currentQuery.length > 0) ? currentQuery : ";",
        "queryFile" : queryFile,
        "logFile" : logFile};


      let existingSavedQuery = this.get('store').peekRecord('saved-query', savedQueryId);

      if(existingSavedQuery){

          this.get('savedQueries').updateSavedQuery(existingSavedQuery.get('id'), shortQuery, selectedDb, owner).then( data => {
              console.log('saved query updated.');
              this.get('controller.model').set('title', newTitle);
              this.get('controller.model').set('isQueryDirty', false);
              this.get('controller').set('worksheetModalSuccess', true);

              Ember.run.later(() => {
                this.get('controller').set('showWorksheetModal', false);
                this.closeWorksheetAfterSave();
              }, 2 * 1000);

            }).catch(function (response) {
               console.log('error', response);
            });

      } else{

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
          this.get('controller.model').set('isQueryDirty', false);
          this.get('controller').set('worksheetModalSuccess', true);

          Ember.run.later(() => {
            this.get('controller').set('showWorksheetModal', false);
            this.closeWorksheetAfterSave();
          }, 2 * 1000);

        });

      }
    },

  closeWorksheetModal(){
      this.get('controller').set('showWorksheetModal', false);
      this.closeWorksheetAfterSave();
      this.get('controller.model').set('tabDataToClose', null);
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
      let isFullHeight = (Ember.$(window).height() ===(parseInt(Ember.$('.ember-light-table').css('height'), 10)) ) || false;
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
