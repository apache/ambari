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
import NewTable from './new';
import constants from '../../../../utils/constants';
import Column from '../../../../models/column';
import datatypes from '../../../../configs/datatypes';
import Helpers from '../../../../configs/helpers';
import UILoggerMixin from '../../../../mixins/ui-logger';

export default NewTable.extend(UILoggerMixin, {
  COLUMN_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  TABLE_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  HDFS_PATH_REGEX: "^[/]{1}.+",  // unix path allows everything but here we have to mention full path so starts with /
  i18n : Ember.inject.service("i18n"),
  jobService: Ember.inject.service(constants.services.jobs),
  notifyService: Ember.inject.service(constants.services.alertMessages),
  showErrors: false,
  init: function () {
    this._super();
  },

  afterModel(){
    return this.store.findAll('setting').then((data) => {
      let localStr = '';
      data.forEach(x => {
        localStr = localStr + 'set '+ x.get('key')+ '='+ x.get('value') + ';\n';
      });
      this.set('globalSettings', localStr);
    });
  },
  setupController(controller, model) {
    this._super(controller, model);
    this.controller.set("showUploadTableModal", false);
    this.controller.set("options", { 'isDeleteColumnDisabled' : true});
  },
  getCharOptionByCharCode: function(charCode){
    return Helpers.getAllTerminationCharacters().findBy("id", charCode + "");
  },
  getUploader(){
    return this.get('store').adapterFor('upload-table');
  },
  onChangeUploadSource : function(){
    this.clearFields();
  }.observes("uploadSource"),
  showCSVFormatInput: false,
  uploadProgressInfo : Ember.computed("uploadProgressInfos.[]",function(){
    var info = "";
    for( var i = 0 ; i < this.get('uploadProgressInfos').length ; i++) {
      info += this.get('uploadProgressInfos').objectAt(i);
    }

    return new Ember.Handlebars.SafeString(info);
  }),
  _setHeaderElements : function(header,valueArray){
    header.forEach(function (item, index) {
      Ember.set(item, 'name',  valueArray[index]);
    }, this);
  },

  pushUploadProgressInfos : function(info){
    this.controller.set("uploadTableMessage", info);
    this.showUploadModal();
  },
  clearUploadProgressModal : function(){
  },

  hideUploadModal : function(){
    this.controller.set("showUploadTableModal", false);
    this.clearUploadProgressModal();
  },

  showUploadModal : function(){
    this.controller.set("showUploadTableModal", true);
  },

  clearFields: function () {
    this.set("error");
    this.clearUploadProgressModal();
  },

  generateTempTableName: function () {
    var text = "";
    var possible = "abcdefghijklmnopqrstuvwxyz";

    for (var i = 0; i < 30; i++) {
      text += possible.charAt(Math.floor(Math.random() * possible.length));
    }

    return text;
  },

  waitForJobStatus: function (jobId, resolve, reject) {
    console.log("finding status of job: ", jobId);
    var self = this;
    var fetchJobPromise = this.get('jobService').getJob(jobId);
    fetchJobPromise.then(function (data) {
      var job = JSON.parse(JSON.stringify(data));
      var status = job.status;
      if (status === constants.statuses.succeeded ) {
        console.log("resolving waitForJobStatus with : " , status);
        resolve(job);
      } else if (status === constants.statuses.canceled || status === constants.statuses.closed || status === constants.statuses.error) {
        console.log("rejecting waitForJobStatus with : " + status);
        reject(new Error(job.statusMessage));
      } else {
        Ember.run.later(function(){
          console.log("retrying waitForJobStatus : ", jobId);
          self.waitForJobStatus(jobId, resolve, reject);
        }, 2000);
      }
    }, function (error) {
      console.log("rejecting waitForJobStatus with : " + error);
      reject(error);
    });
  },

  uploadForPreview: function (sourceObject) {
    console.log("uploaderForPreview called.");
    let files = sourceObject.get("fileInfo.files");
    let csvParams = sourceObject.get("fileFormatInfo.csvParams");

    return this.getUploader().uploadFiles('preview', files, {
      "inputFileType": sourceObject.get("fileFormatInfo.inputFileType").id,
      "isFirstRowHeader": csvParams.get("isFirstRowHeader"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },

  uploadForPreviewFromHDFS: function (sourceObject) {
    console.log("uploadForPreviewFromHDFS called.");
    // this.validateHDFSPath(hdfsPath);
    var self = sourceObject;
    var hdfsPath = sourceObject.get("fileInfo.hdfsPath");
    var csvParams = sourceObject.get("fileFormatInfo.csvParams");

    return this.getUploader().previewFromHDFS({
      "inputFileType": sourceObject.get("fileFormatInfo.inputFileType").id,
      "hdfsPath": hdfsPath,
      "isFirstRowHeader": csvParams.get("isFirstRowHeader"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },

  generatePreview: function (sourceObject) {
    var self = this;
    var promise = null;
    try {
      this.waitForGeneratingPreview();
      if (sourceObject.get('fileInfo.uploadSource') === "local" ) {
        promise = this.uploadForPreview(sourceObject);
      } else {
        promise = this.uploadForPreviewFromHDFS(sourceObject);
      }

      return promise.then(function (data) {
        self.onGeneratePreviewSuccess(data);
      }, function (error) {
        self.onGeneratePreviewFailure(error);
      }).catch(function (error) {
        console.log("inside catch : ", error);
        throw error;
      }).finally(function () {
        console.log("finally hide the modal always after preview.");
        self.hideUploadModal();
      });
    }catch(e){
      // exception before promise will be caught here.
      console.log("exception before promise : ", e);
      self.setError(e);
    }finally{
      console.log("finally hide the modal always after preview.");
      self.hideUploadModal();
    }
  },

  waitForGeneratingPreview: function () {
    console.log("waitForGeneratingPreview");
    this.showUploadModal();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.generatingPreview'));
  },

  previewTable: function (data) {
    console.log('inside previewTable. data : ', data);
    this.set("previewData", data);
    this.controller.set('tableName', this.get("previewData.tableName"));
    this.controller.set('tableName', this.get("previewData.tableName"));
    this.controller.set("rows", this.get("previewData.rows"));
    this.controller.set("columns", this.transformToColumnModelList(this.get("previewData.header")));
  },

  transformToColumnModelList : function(columns){
    var _this = this;
    if(columns){
      return columns.map(function(column){
        return _this.transformToColumnModel(column);
      });
    }
    return Ember.A();
  },

  transformToColumnModel: function (column) {
    return Column.create({
      name: column.name,
      type: datatypes.findBy("label", column.type),
      editing: true
    });
  },
  onGeneratePreviewSuccess: function (data) {
    console.log("onGeneratePreviewSuccess");
    this.set("showPreview",true);
    this.hideUploadModal();
    this.previewTable(data);
  },

  onGeneratePreviewFailure: function (error) {
    console.log("onGeneratePreviewFailure");
    this.set("showPreview",false);
    this.hideUploadModal();
    this.setError(error);
  },

  createActualTable: function (tableData) {
    console.log("createActualTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateActualTable'));
    var retValue = this.createTable(tableData.get("tableMeta"));
    return retValue;
  },
  getRowFormat : function(){
    var fieldsTerminatedBy = this.getAsciiChar('fieldsTerminatedBy');
    var escapedBy = this.getAsciiChar('escapedBy');
    return {"fieldsTerminatedBy": fieldsTerminatedBy, "escapedBy" : escapedBy};
  },
  waitForCreateActualTable: function (jobId) {
    console.log("waitForCreateActualTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToCreateActualTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },
  onCreateActualTableSuccess: function () {
    console.log("onCreateTableSuccess");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyCreatedActualTable'));
  },
  onCreateActualTableFailure: function (error) {
    console.log("onCreateActualTableFailure");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToCreateActualTable'));
    this.setError(error);
  },
  copyTableMeta: function(tableMeta){
    let colArray = Ember.copy(tableMeta.columns, true);
    let tableMetaCopy = JSON.parse(JSON.stringify(tableMeta));
    tableMetaCopy.columns = colArray;
    return tableMetaCopy;
  },
  createTempTable: function (tableData) {
    console.log("createTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateTemporaryTable'));
    let tableMeta = this.copyTableMeta(tableData.get("tableMeta")); // deep copy or otherwise it does make separate
    var tempTableName = this.generateTempTableName();
    tableMeta.name = tempTableName;

    var headers = tableMeta.columns.map(function(column){
      if(tableData.fileFormatInfo.containsEndlines){
        column.set("type", datatypes.findBy("label","STRING"));
        column.set("scale");
        column.set("precision");
      }
      column.set("isPartitioned", false); // remove all partitioning information from temp table.
      return column;
    });

    tableMeta.columns = headers;
    tableMeta.settings = {};
    tableMeta.properties = [];
    tableMeta.settings.fileFormat = {};
    tableMeta.settings.fileFormat.type = "TEXTFILE";
    this.set("tableData.tempTableMeta", tableMeta);
    if(!tableMeta.settings){
      tableMeta.settings = {};
    }
    if(!tableMeta.settings.rowFormat){
      tableMeta.settings.rowFormat = {};
    }
    tableMeta.settings.rowFormat.fieldTerminatedBy = this.getCharOptionByCharCode(1);
    tableMeta.settings.rowFormat.escapeDefinedAs = this.getCharOptionByCharCode(2);
    return this.createTable(tableMeta);
  },

  waitForCreateTempTable: function (jobId) {
    console.log("waitForCreateTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToCreateTemporaryTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onCreateTempTableSuccess: function () {
    console.log("onCreateTempTableSuccess");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyCreatedTemporaryTable'));
  },

  deleteTable : function(databaseName, tableName){
    console.log("deleting table ", databaseName , "." , tableName);
    return this.getUploader().deleteTable({
      "database":  databaseName,
      "table": tableName
    });
  },

  deleteTableOnError: function (databaseName, tableName, tableLabel) {
    //delete table and wait for delete job
    var self = this;
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.deletingTable',{table:tableLabel}));

    return this.deleteTable(databaseName, tableName).then(function (job) {
      return new Ember.RSVP.Promise(function (resolve, reject) {
        self.waitForJobStatus(job.id, resolve, reject);
      });
    }).then(function () {
      self.pushUploadProgressInfos(this.formatMessage('hive.messages.succesfullyDeletedTable',{table:tableLabel}));
      return Ember.RSVP.Promise.resolve();
    }, function (err) {
      self.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToDeleteTable',{table:tableLabel}));
      self.setError(err);
      return Ember.RSVP.Promise.reject();
    });
  },

  rollBackActualTableCreation : function(){
    return this.deleteTableOnError(this.get("tableData.database"),this.get("tableData.tableMeta").name,this.translate('hive.words.actual'));
  },

  translate : function(str,vars){
    return this.get('i18n').t(str,vars);
  },
  formatMessage : function(messageId, vars){
    return this.translate(messageId, vars);
  },
  onCreateTempTableFailure : function(error){
    console.log("onCreateTempTableFailure");
    this.setError(error);
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToCreateTemporaryTable'));
    return this.rollBackActualTableCreation().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    }, function (err) {
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  uploadFile: function (tableData) {
    console.log("uploadFile");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToUploadFile'));
    let uploadSource = tableData.get("fileInfo").get("uploadSource");
    if(uploadSource === "local"){
      return this.uploadTable(tableData);
    }else{
      return this.uploadTableFromHdfs(tableData);
    }
  },

  waitForUploadingFile: function (data) {
    console.log("waitForUploadingFile");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToUploadFile'));
    if( data.jobId ){
      var self = this;
      var p = new Ember.RSVP.Promise(function (resolve, reject) {
        self.waitForJobStatus(data.jobId, resolve, reject);
      });
      return p;
    }else{
      return  Ember.RSVP.Promise.resolve(data);
    }
  },

  onUploadingFileSuccess: function () {
    console.log("onUploadingFileSuccess");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyUploadedFile') );
  },

  rollBackTempTableCreation: function () {
    var self = this;
    return this.deleteTableOnError(this.get("tableData.database"),this.get("tableData.tempTableMeta").name,this.translate('hive.words.temporary')).then(function(data){
      return self.rollBackActualTableCreation();
    },function(err){
      return self.rollBackActualTableCreation();
    });
  },

  onUploadingFileFailure: function (error) {
    console.log("onUploadingFileFailure");
    this.setError(error);
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToUploadFile'));
    return this.rollBackTempTableCreation().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  rollBackUploadFile : function(){
    return this.rollBackTempTableCreation();
  },

  insertIntoTable : function(tableData){
    console.log("insertIntoTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToInsertRows'));
    let globalSettings = this.get('globalSettings');

    let partitionedColumns = tableData.get("tableMeta").columns.filter(function(column){
      return column.isPartitioned;
    }).map(function(column){
        var header = JSON.parse(JSON.stringify(column));
        header.type = column.type.label;
        return header;
    });

    let normalColumns = tableData.get("tableMeta").columns.filter(function(column){
      return !column.isPartitioned;
    }).map(function(column){
        var header = JSON.parse(JSON.stringify(column));
        header.type = column.type.label;
        return header;
    });

    return this.getUploader().insertIntoTable({
      "fromDatabase": tableData.get("database"),
      "fromTable": tableData.get("tempTableMeta").name,
      "toDatabase": tableData.get("database"),
      "toTable": tableData.get("tableMeta").name,
      "partitionedColumns": partitionedColumns,
      "normalColumns": normalColumns,
      "globalSettings": globalSettings,
      "unhexInsert": tableData.fileFormatInfo.containsEndlines
    });
  },

  waitForInsertIntoTable: function (jobId) {
    console.log("waitForInsertIntoTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToInsertRows'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onInsertIntoTableSuccess: function () {
    console.log("onInsertIntoTableSuccess");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyInsertedRows'));
  },

  onInsertIntoTableFailure: function (error) {
    console.log("onInsertIntoTableFailure");
    this.setError(error);
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToInsertRows'));
    return this.rollBackUploadFile().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },
  deleteTempTable : function(tableData){
    console.log("deleteTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToDeleteTemporaryTable'));

    return this.deleteTable(
      tableData.get("database"),
      tableData.get("tempTableMeta").name
    );
  },
  waitForDeleteTempTable: function (jobId) {
    console.log("waitForDeleteTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToDeleteTemporaryTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },
  onDeleteTempTableSuccess: function () {
    console.log("onDeleteTempTableSuccess");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyDeletedTemporaryTable'));
    this.onUploadSuccessfull();
  },
  onDeleteTempTableFailure: function (error) {
    console.log("onDeleteTempTableFailure");
    this.setError(error);
    this.setError(this.formatMessage('hive.messages.manuallyDeleteTable',{databaseName:this.get('databaseName'), tableName: this.get("tempTableName")}));
  },
  validateHDFSPath: function (hdfsPath) {
    if (null === hdfsPath || hdfsPath === "") {
      throw new Error(this.translate('hive.errors.emptyHdfsPath'));
    }
    var hdfsRegex = new RegExp(this.get("HDFS_PATH_REGEX"), "g");
    var mArr = hdfsPath.match(hdfsRegex);
    if (mArr === null || mArr.length !== 1) {
      throw new Error(this.translate('hive.errors.illegalHdfPath', {"hdfsPath": hdfsPath} ));
    }
  },
  createTableAndUploadFile: function (tableData) {
    let databaseModel = this.controllerFor('databases.database').get('model');
    let database = databaseModel.get('name');
    tableData.set("database", database);
    this.set("tableData", tableData);

    var self = this;
    self.setError();
    self.createActualTable(tableData)
      .then(function(job){
        console.log("1. received job : ", job);
        return self.waitForCreateActualTable(job.id);
      },function(error){
        console.log("Error occurred: ", error);
        self.onCreateActualTableFailure(error);
        throw error;
      })
      .then(function(data){
        self.onCreateActualTableSuccess(data);
        return self.createTempTable(tableData);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onCreateActualTableFailure(error);
        }
        throw error;
      })
      .then(function(job){
        return self.waitForCreateTempTable(job.id);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onCreateTempTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        self.onCreateTempTableSuccess(data);
        return self.uploadFile(tableData);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onCreateTempTableFailure(error);
        }
        throw error;
      }).then(function(data){
      return self.waitForUploadingFile(data);
    },function(error){
      if(!self.get('error')){
        console.log("Error occurred: ", error);
        return self.onUploadingFileFailure(error);
      }
      throw error;
    })
      .then(function(data){
        self.onUploadingFileSuccess(data);
        return self.insertIntoTable(tableData);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onUploadingFileFailure(error);
        }
        throw error;
      })
      .then(function(job){
        return self.waitForInsertIntoTable(job.id);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onInsertIntoTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        self.onInsertIntoTableSuccess(data);
        return self.deleteTempTable(tableData);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          return self.onInsertIntoTableFailure(error);
        }
        throw error;
      })
      .then(function(job){
        return self.waitForDeleteTempTable(job.id);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onDeleteTempTableFailure(error);
        }
        throw error;
      })
      .then(function(data){
        self.onDeleteTempTableSuccess(data);
      },function(error){
        if(!self.get('error')){
          console.log("Error occurred: ", error);
          self.onDeleteTempTableFailure(error);
        }
        throw error;
      })
      .catch(function(error){
        console.log("inside catch : ", error);
      })
      .finally(function(){
        console.log("finally hide the modal always");
        self.hideUploadModal();
      });
  },
  validateInput: function (headers,tableName,databaseName,isFirstRowHeader) {
    // throw exception if invalid.
    if(!headers || headers.length === 0) {
      throw new Error(this.translate('hive.errors.emptyHeaders'));
    }

    var regex = new RegExp(this.get("COLUMN_NAME_REGEX"),"g");

    headers.forEach(function(column,index){
      if( !column  ) {
        throw new Error(this.translate('hive.errors.emptyColumnName'));
      }
      var matchArr = column.name.match(regex);
      if(matchArr === null || matchArr.length !== 1 ) {
        throw new Error(this.translate('hive.errors.illegalColumnName',{ columnName : column.name, index : (index + 1)}));
      }
    },this);

    if(!tableName) {
      throw new Error(this.translate('hive.errors.emptyTableName', {tableNameField : this.translate('hive.ui.tableName')}));
    }
    var tableRegex = new RegExp(this.get("TABLE_NAME_REGEX"),"g");
    var mArr = tableName.match(tableRegex);
    if(mArr === null || mArr.length !== 1 ) {
      throw new Error(this.translate('hive.errors.illegalTableName', {tableNameField:this.translate('hive.ui.tableName'),tableName:tableName}) );
    }

    if(!databaseName) {
      throw new Error(this.translate('hive.errors.emptyDatabase', {database:this.translate('hive.words.database')}));
    }

    if (null === isFirstRowHeader || typeof isFirstRowHeader === 'undefined') { //this can be true or false. so explicitly checking for null/ undefined.
      throw new Error(this.translate('hive.errors.emptyIsFirstRow', {isFirstRowHeaderField:this.translate('hive.ui.isFirstRowHeader')}));
    }
  },
  setError: function (error) {
    if(error){
      console.log(" error : ", error);
      this.set('error', JSON.stringify(error));
      this.get('notifyService').error( this.extractMessage(error), this.extractError(error));
    }else{
      this.set("error");
    }
  },
  uploadTableFromHdfs : function(tableData){
    console.log("uploadTableFromHdfs called.");
    this.pushUploadProgressInfos(this.formatMessage('uploadingFromHdfs'));
    var csvParams = tableData.get("fileFormatInfo.csvParams");
    let columns = tableData.get("tableMeta").columns.map(function (column) {
      return {"name": column.get("name"), "type": column.get("type.label")};
    });
    let header = columns;

    return this.getUploader().uploadFromHDFS({
      "databaseName": tableData.get("database"),
      "tableName": tableData.get("tempTableMeta").name,
      "inputFileType": tableData.get("fileFormatInfo.inputFileType").id,
      "hdfsPath": tableData.get("fileInfo.hdfsPath"),
      "header": header,
      "containsEndlines": tableData.get("fileFormatInfo.containsEndlines"),
      "isFirstRowHeader": csvParams.get("isFirstRowHeader"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },
  uploadTable: function (tableData) {
    var csvParams = tableData.get("fileFormatInfo.csvParams");
    let columns = tableData.get("tableMeta").columns.map(function(column){
      return {"name": column.get("name"), "type": column.get("type.label")};
    });
    let header = JSON.stringify(columns);
    return this.getUploader().uploadFiles('upload', tableData.get("fileInfo.files"), {
      "databaseName" :  tableData.get("database"),
      "tableName" : tableData.get("tempTableMeta").name,
      "inputFileType" : tableData.get("fileFormatInfo.inputFileType").id,
      "header": header,
      "containsEndlines": tableData.get("fileFormatInfo.containsEndlines"),
      "isFirstRowHeader": csvParams.get("isFirstRowHeader"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },

  onUploadSuccessfull: function (data) {
    console.log("onUploadSuccessfull : ", data);
    this._transitionToCreatedTable(this.get("tableData").get('database'), this.get("tableData").get('tableMeta').name);

    this.get('notifyService').success(this.translate('hive.messages.successfullyUploadedTableHeader'),
      this.translate('hive.messages.successfullyUploadedTableMessage' ,{tableName:this.get("tableData").get("tableMeta").name ,databaseName:this.get("tableData").get("database")}));
    this.clearFields();
  },

  validateInputs: function(tableData){
    let tableMeta = tableData.get("tableMeta");
    let containsEndlines = tableData.get("fileFormatInfo.containsEndlines");
    if(containsEndlines === true && tableMeta.settings && tableMeta.settings.fileFormat &&
      tableMeta.settings.fileFormat.type && tableMeta.settings.fileFormat.type === "TEXTFILE"){
      throw new Error(`Cannot support endlines in fields when the  File Format is TEXTFILE. Please uncheck '${this.translate('hive.ui.csvFormatParams.containsEndlines')}'`);
    }
  },

  actions: {
    preview: function (previewObject) {
      console.log("upload-table.js : uploaded new files : ", previewObject);
      this.clearFields();

      this.set('previewObject', previewObject);
      return this.generatePreview(previewObject);
    },
    uploadTable: function (tableData) {
      console.log("tableData", tableData);
      try {
        this.validateInputs(tableData);
        this.createTableAndUploadFile(tableData);
      } catch (e) {
        console.log("exception occured : ", e);
        this.setError(e);
        this.hideUploadModal();
      }
    },
  }
});
