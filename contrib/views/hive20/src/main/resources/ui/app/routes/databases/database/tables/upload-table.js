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

export default NewTable.extend({
  COLUMN_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  TABLE_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  HDFS_PATH_REGEX: "^[/]{1}.+",  // unix path allows everything but here we have to mention full path so starts with /
  init: function () {
    this._super();
  },

  COLUMN_NAME_PREFIX : "column",
  i18n : Ember.inject.service('i18n'),
  jobService: Ember.inject.service(constants.services.jobs),
  notifyService: Ember.inject.service(constants.services.alertMessages),
  showErrors: false,
  baseUrl: "/resources/upload",
  header: null,  // header received from server
  files: null, // files that need to be uploaded only file[0] is relevant
  firstRow: [], // the actual first row of the table.
  rows: null,  // preview rows received from server
  databaseName: null,
  selectedDatabase: null,
  filePath: null,
  tableName: null,
  uploadProgressInfos : [],
  DEFAULT_DB_NAME : 'default',
  showPreview : false,
  containsEndlines: false,
  storedAsTextFile : Ember.computed.equal("selectedFileType","TEXTFILE"),
  storedAsNotTextFile : Ember.computed.not("storedAsTextFile"),
  setupController(controller, model) {
    this._super(controller, model);
    this.controller.set("showUploadTableModal", false);
  },
  onChangeSelectedFileType: function(){
    if(this.get('selectedFileType') === this.get('fileTypes')[1] && this.get('containsEndlines') === true){
      this.set('containsEndlines', false);
    }
  }.observes("selectedFileType", "containsEndlines"),
  getUploader(){
    return this.get('store').adapterFor('upload-table');
  },
  onChangeUploadSource : function(){
    this.clearFields();
  }.observes("uploadSource"),
  showCSVFormatInput: false,
  uploadProgressInfo : Ember.computed("uploadProgressInfos.[]",function(){
    var info = "";
    for( var i = 0 ; i < this.get('uploadProgressInfos').length ; i++)
      info += this.get('uploadProgressInfos').objectAt(i);

    return new Ember.Handlebars.SafeString(info);
  }),
  _setHeaderElements : function(header,valueArray){
    header.forEach(function (item, index) {
      Ember.set(item, 'name',  valueArray[index]);
    }, this);
  },
  isFirstRowHeaderDidChange: function () {
    if (this.get('isFirstRowHeader') != null && typeof this.get('isFirstRowHeader') !== 'undefined') {
      if (this.get('isFirstRowHeader') == false) {
        if (this.get('rows')) {
          this.get('rows').unshiftObject({row: this.get('firstRow')});
          this._setHeaderElements(this.get('header'),this.get('defaultColumnNames'));
        }
      } else if (this.get('header')) { // headers are available
        // take first row of
        this._setHeaderElements(this.get('header'),this.get('firstRow'));
        this.get('rows').removeAt(0);
      }

      this.printValues();
    }
  }.observes('isFirstRowHeader'),

  popUploadProgressInfos: function () {
    // var msg = this.get('uploadProgressInfos').popObject();
  },

  pushUploadProgressInfos : function(info){
    this.controller.set("uploadTableMessage", info);
    this.showUploadModal();
    // this.get('uploadProgressInfos').pushObject(info);
  },
  clearUploadProgressModal : function(){
    var len = this.get('uploadProgressInfos').length;
    for( var i = 0 ; i < len ; i++){
      this.popUploadProgressInfos();
    }
  },

  hideUploadModal : function(){
    this.controller.set("showUploadTableModal", false);
    this.clearUploadProgressModal();
  },

  showUploadModal : function(){
    this.controller.set("showUploadTableModal", true);
  },

  clearFields: function () {
    this.set("showPreview",false);
    this.set("hdfsPath");
    this.set("header");
    this.set("rows");
    this.set("escapedBy");
    this.set("fieldsTerminatedBy");
    this.set("error");
    this.set('files');
    this.set("firstRow");
    this.set("selectedDatabase",null);
    this.set("databaseName");
    this.set("filePath");
    this.set('tableName');
    this.clearUploadProgressModal();
    this.printValues();
  },

  printValues: function () {
    console.log("header : ", this.get('header'),
      ". rows : ",this.get('rows'),". error : ", this.get('error'),
      " isFirstRowHeader : ", this.get('isFirstRowHeader'),
      "firstRow : ", this.get('firstRow'));
  },

  generateTempTableName: function () {
    var text = "";
    var possible = "abcdefghijklmnopqrstuvwxyz";

    for (var i = 0; i < 30; i++)
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  },

  waitForJobStatus: function (jobId, resolve, reject) {
    console.log("finding status of job: ", jobId);
    var self = this;
    var fetchJobPromise = this.get('jobService').getJob(jobId);
    fetchJobPromise.then(function (data) {
      console.log("waitForJobStatus : data : ", data);
      var job = JSON.parse(JSON.stringify(data));
      var status = job.status;
      if (status == constants.statuses.succeeded ) {
        console.log("resolving waitForJobStatus with : " , status);
        resolve(job);
      } else if (status == constants.statuses.canceled || status == constants.statuses.closed || status == constants.statuses.error) {
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
    })
  },

  uploadForPreview: function (sourceObject) {
    console.log("uploaderForPreview called.");
    let files = sourceObject.get("fileInfo.files");
    let csvParams = sourceObject.get("fileFormatInfo.csvParams");

    return this.getUploader().uploadFiles('preview', files, {
      "isFirstRowHeader": sourceObject.get("isFirstRowHeader"),
      "inputFileType": sourceObject.get("fileFormatInfo.inputFileType").id,
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },

  getAsciiChar : function(key){
    if(!key){
      return null;
    }

    var value = this.get(key);
    if(value && value.id != -1) {
      return String.fromCharCode(value.id);
    }else{
      return null;
    }
  },
  getCSVParams : function(){
    var csvd = this.getAsciiChar('csvDelimiter');
    if(!csvd && csvd != 0) csvd = this.get('DEFAULT_CSV_DELIMITER');

    var csvq = this.getAsciiChar('csvQuote');
    if(!csvq && csvq != 0) csvq = this.get('DEFAULT_CSV_QUOTE');

    var csve = this.getAsciiChar('csvEscape');
    if(!csve && csve != 0) csve = this.get('DEFAULT_CSV_ESCAPE');

    return {"csvDelimiter": csvd, "csvQuote" : csvq, "csvEscape": csve};
  },

  uploadForPreviewFromHDFS: function (sourceObject) {
    console.log("uploadForPreviewFromHDFS called.");
    // this.validateHDFSPath(hdfsPath);
    var self = sourceObject;
    var hdfsPath = sourceObject.get("fileInfo.hdfsPath");
    var csvParams = sourceObject.get("fileFormatInfo.csvParams");

    return this.getUploader().previewFromHDFS({
      "isFirstRowHeader": sourceObject.get("fileFormatInfo.isFirstRowHeader"),
      "inputFileType": sourceObject.get("fileFormatInfo.inputFileType").id,
      "hdfsPath": hdfsPath,
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
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.generatingPreview'))
  },

  previewTable: function (data) {
    console.log('inside previewTable. data : ', data);
    var self = this;
    var defaultColumnNames = data.header.map(function(item,index){
      return { "name": self.COLUMN_NAME_PREFIX + (index + 1) }
    });
    this.set("defaultColumnNames",defaultColumnNames);
    this.set("previewData", data);
    this.set("header", this.get("previewData.header"));
    this.set('isFirstRowHeader', this.get("previewData.isFirstRowHeader"));
    this.set('tableName', this.get("previewData.tableName"));
    if (data.isFirstRowHeader == true) {
      this.set("firstRow", this.get("previewData.header"));
    }else {
      if(data.rows.length > 0){
        this.set("firstRow", this.get("previewData.rows")[0].row);
      }else{
        this.set("firstRow", Ember.A());
      }
    }
    this.set("rows", this.get("previewData.rows"));
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
    })
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
    // var self = this;
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateActualTable'));
    // var headers = this.get('header');
    // var selectedDatabase = this.get('selectedDatabase');
    // if (!selectedDatabase) {
    //   throw new Error(this.translate('hive.errors.emptyDatabase', {database : this.translate("hive.words.database")}));
    // }
    //
    // this.set('databaseName', this.get('selectedDatabase.id'));
    // var databaseName = this.get('databaseName');
    // var tableName = this.get("tableMeta").name;
    // var isFirstRowHeader = this.get('isFirstRowHeader');
    // var filetype = this.get("selectedFileType");
    //
    // this.validateInput(headers,tableName,databaseName,isFirstRowHeader);
    // this.showUploadModal();
    // var rowFormat = this.getRowFormat();
    // return this.getUploader().createTable({
    //   "isFirstRowHeader": isFirstRowHeader,
    //   "header": headers,
    //   "tableName": tableName,
    //   "databaseName": databaseName,
    //   "hiveFileType":filetype,
    //   "rowFormat": { "fieldsTerminatedBy" : rowFormat.fieldsTerminatedBy, "escapedBy" : rowFormat.escapedBy}
    // });
  },
  getRowFormat : function(){
    var fieldsTerminatedBy = this.getAsciiChar('fieldsTerminatedBy');
    var escapedBy = this.getAsciiChar('escapedBy');
    return {"fieldsTerminatedBy": fieldsTerminatedBy, "escapedBy" : escapedBy};
  },
  waitForCreateActualTable: function (jobId) {
    console.log("waitForCreateActualTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToCreateActualTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },
  onCreateActualTableSuccess: function () {
    console.log("onCreateTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyCreatedActualTable'));
  },
  onCreateActualTableFailure: function (error) {
    console.log("onCreateActualTableFailure");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToCreateActualTable'));
    this.setError(error);
  },
  createTempTable: function (tableData) {
    let tableMeta = JSON.parse(JSON.stringify(tableData.get("tableMeta")));
    // manually copy the columns as they are missing members when copying
    let columns = tableData.get("tableMeta").columns.map(function(col){
      return col.copy();
    });
    tableMeta.columns = columns;

    console.log("tableMeta : ", tableMeta);

    var self = this;
    console.log("createTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateTemporaryTable'));
    var tempTableName = this.generateTempTableName();
    tableMeta.name = tempTableName;

    var headers = tableMeta.columns.map(function(column){
      if(tableData.fileFormatInfo.containsEndlines){
        column.type.label = "STRING";
        delete column.scale;
        delete column.precision;
      }
      return column;
    });

    tableMeta.columns = headers;
    tableMeta.settings = {};
    tableMeta.properties = [];
    tableMeta.settings.fileFormat = {};
    tableMeta.settings.fileFormat.type = "TEXTFILE";
    this.set("tableData.tempTableMeta", tableMeta);
    return this.createTable(tableMeta);
    // return this.getUploader().createTable({
    //   "isFirstRowHeader": this.get("isFirstRowHeader"),
    //   "header": headers,
    //   "tableName": tempTableName,
    //   "databaseName": this.get('databaseName'),
    //   "hiveFileType":"TEXTFILE",
    //   "rowFormat": { "fieldsTerminatedBy" : parseInt('1', 10), "escapedBy" : null}
    // });
  },

  waitForCreateTempTable: function (jobId) {
    console.log("waitForCreateTempTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToCreateTemporaryTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onCreateTempTableSuccess: function () {
    console.log("onCreateTempTableSuccess");
    this.popUploadProgressInfos();
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
      self.popUploadProgressInfos();
      self.pushUploadProgressInfos(this.formatMessage('hive.messages.succesfullyDeletedTable',{table:tableLabel}));
      return Ember.RSVP.Promise.resolve();
    }, function (err) {
      self.popUploadProgressInfos();
      self.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToDeleteTable',{table:tableLabel}));
      self.setError(err);
      return Ember.RSVP.Promise.reject();
    });
  },

  rollBackActualTableCreation : function(){
    return this.deleteTableOnError(this.get("database"),this.get("tableMeta").name,this.translate('hive.words.actual'));
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
    this.popUploadProgressInfos();
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
    this.popUploadProgressInfos();
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
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyUploadedFile') );
  },

  rollBackTempTableCreation: function () {
    var self = this;
    return this.deleteTableOnError(this.get("database"),this.get("tempTableMeta").name,this.translate('hive.words.temporary')).then(function(data){
      return self.rollBackActualTableCreation();
    },function(err){
      return self.rollBackActualTableCreation();
    })
  },

  onUploadingFileFailure: function (error) {
    console.log("onUploadingFileFailure");
    this.setError(error);
    this.popUploadProgressInfos();
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

    let headers = tableData.get("tableMeta").columns.map(function(column){
        var header = JSON.parse(JSON.stringify(column));
        header.type = column.type.label;
        return header;
    });

    return this.getUploader().insertIntoTable({
      "fromDatabase": tableData.get("database"),
      "fromTable": tableData.get("tempTableMeta").name,
      "toDatabase": tableData.get("database"),
      "toTable": tableData.get("tableMeta").name,
      "header": headers,
      "unhexInsert": tableData.fileFormatInfo.containsEndlines
    });
  },

  waitForInsertIntoTable: function (jobId) {
    console.log("waitForInsertIntoTable");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToInsertRows'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },

  onInsertIntoTableSuccess: function () {
    console.log("onInsertIntoTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyInsertedRows'));
  },

  onInsertIntoTableFailure: function (error) {
    console.log("onInsertIntoTableFailure");
    this.setError(error);
    this.popUploadProgressInfos();
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
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.waitingToDeleteTemporaryTable'));
    var self = this;
    var p = new Ember.RSVP.Promise(function (resolve, reject) {
      self.waitForJobStatus(jobId, resolve, reject);
    });

    return p;
  },
  onDeleteTempTableSuccess: function () {
    console.log("onDeleteTempTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyDeletedTemporaryTable'));
    this.onUploadSuccessfull();
  },
  onDeleteTempTableFailure: function (error) {
    console.log("onDeleteTempTableFailure");
    this.setError(error);
    this.setError(this.formatMessage('hive.messages.manuallyDeleteTable',{databaseName:this.get('databaseName'), tableName: this.get("tempTableName")}));
  },
  validateHDFSPath: function (hdfsPath) {
    if (null == hdfsPath || hdfsPath == "") throw new Error(this.translate('hive.errors.emptyHdfsPath'));
    var hdfsRegex = new RegExp(this.get("HDFS_PATH_REGEX"), "g");
    var mArr = hdfsPath.match(hdfsRegex);
    if (mArr == null || mArr.length != 1) throw new Error(this.translate('hive.errors.illegalHdfPath', {"hdfsPath": hdfsPath} ));
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
    if(!headers || headers.length == 0) throw new Error(this.translate('hive.errors.emptyHeaders'));

    var regex = new RegExp(this.get("COLUMN_NAME_REGEX"),"g");

    headers.forEach(function(column,index){
      if( !column  ) throw new Error(this.translate('hive.errors.emptyColumnName'));
      var matchArr = column.name.match(regex);
      if(matchArr == null || matchArr.length != 1 ) throw new Error(this.translate('hive.errors.illegalColumnName',{ columnName : column.name, index : (index + 1)}));
    },this);

    if(!tableName) throw new Error(this.translate('hive.errors.emptyTableName', {tableNameField : this.translate('hive.ui.tableName')}));
    var tableRegex = new RegExp(this.get("TABLE_NAME_REGEX"),"g");
    var mArr = tableName.match(tableRegex);
    if(mArr == null || mArr.length != 1 ) throw new Error(this.translate('hive.errors.illegalTableName', {tableNameField:this.translate('hive.ui.tableName'),tableName:tableName}) );

    if(!databaseName) throw new Error(this.translate('hive.errors.emptyDatabase', {database:this.translate('hive.words.database')}));

    if (null == isFirstRowHeader || typeof isFirstRowHeader === 'undefined') { //this can be true or false. so explicitly checking for null/ undefined.
      throw new Error(this.translate('hive.errors.emptyIsFirstRow', {isFirstRowHeaderField:this.translate('hive.ui.isFirstRowHeader')}));
    }
  },
  setError: function (error) {
    if(error){
      console.log(" error : ", error);
      this.set('error', JSON.stringify(error));
      // this.get('notifyService').warn(error);
      // TODO : add notifyService warn message.
      console.log("TODO : add notifyService warn message.");
    }else{
      this.set("error");
    }
  },
  previewError: function (error) {
    this.setError(error);
  },
  uploadTableFromHdfs : function(tableData){
    console.log("uploadTableFromHdfs called.");
    // if(!(this.get("inputFileTypeCSV") == true && this.get("isFirstRowHeader") == false) ){
      this.pushUploadProgressInfos(this.formatMessage('uploadingFromHdfs'));
    // }
    var csvParams = tableData.get("fileFormatInfo.csvParams");
    let columns = tableData.get("tableMeta").columns.map(function(column){
      return {"name": column.get("name"), "type": column.get("type.label")};
    });
    let header = columns; //JSON.stringify(columns);

    return this.getUploader().uploadFromHDFS({
      "isFirstRowHeader": tableData.get("fileFormatInfo.isFirstRowHeader"),
      "databaseName": tableData.get("database"),
      "tableName": tableData.get("tempTableMeta").name,
      "inputFileType": tableData.get("fileFormatInfo.inputFileType").id,
      "hdfsPath": tableData.get("fileInfo.hdfsPath"),
      "header": header,
      "containsEndlines": tableData.get("fileFormatInfo.containsEndlines"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },
  uploadTable: function (tableData) {
    this.printValues();
    var csvParams = tableData.get("fileFormatInfo.csvParams");
    let columns = tableData.get("tableMeta").columns.map(function(column){
      return {"name": column.get("name"), "type": column.get("type.label")};
    });
    let header = JSON.stringify(columns);
    return this.getUploader().uploadFiles('upload', tableData.get("fileInfo.files"), {
      "isFirstRowHeader": tableData.get("fileFormatInfo.isFirstRowHeader"),
      "databaseName" :  tableData.get("database"),
      "tableName" : tableData.get("tempTableMeta").name,
      "inputFileType" : tableData.get("fileFormatInfo.inputFileType").id,
      "header": header,
      "containsEndlines": tableData.get("fileFormatInfo.containsEndlines"),
      "csvDelimiter": csvParams.get("csvDelimiter").name,
      "csvEscape": csvParams.get("csvEscape").name,
      "csvQuote": csvParams.get("csvQuote").name
    });
  },

  onUploadSuccessfull: function (data) {
    console.log("onUploadSuccessfull : ", data);
    this._transitionToCreatedTable(this.get("tableData").get('database'), this.get("tableData").get('tableMeta').name);

    // this.get('notifyService').success(this.translate('hive.messages.successfullyUploadedTableHeader'),
    //   this.translate('hive.messages.successfullyUploadedTableMessage' ,{tableName:this.get("tableData").get("tableMeta").name ,databaseName:this.get("tableData").get("database")}));
    this.clearFields();
  },

  onUploadError: function (error) {
    console.log("onUploadError : ", error);
    this.setError(error);
  },
  showOrHide: function () {
    if (this.get('show') == false) {
      this.set("displayOption", "display:none");
      this.set("showMoreOrLess", "Show More");
    } else {
      this.set("displayOption", "display:table-row");
      this.set("showMoreOrLess", "Show Less");
    }
  },

  displayOption: "display:none",
  actions: {
  toggleCSVFormat: function() {
    console.log("inside toggleCSVFormat");
    this.toggleProperty('showCSVFormatInput');
  },
  hideInputParamModal : function(){
      Ember.$("#inputParamsModal").modal("hide");
    },
    showInputParamModal : function(){
      if(this.get('inputFileTypeCSV')){
        Ember.$("#inputParamsModal").modal("show");
      }
    },
    hideRowFormatModal : function(){
      Ember.$("#rowFormatModal").modal("hide");
    },
    showRowFormatModal : function(){
      if(this.get('storedAsTextFile')) {
        Ember.$("#rowFormatModal").modal("show");
      }
    },
    toggleErrors: function () {
      this.toggleProperty('showErrors');
    },
    // filesUploaded: function (files) {
    //   console.log("upload-table.js : uploaded new files : ", files);
    //   this.clearFields();
    //
    //   this.set('files', files);
    //   var name = files[0].name;
    //   var i = name.indexOf(".");
    //   var tableName = name.substr(0, i);
    //   this.set('tableName', tableName);
    //   var self = this;
    //   return this.generatePreview(sourceObject)
    // },
    preview: function (previewObject) {
      console.log("upload-table.js : uploaded new files : ", previewObject);
      this.clearFields();

      this.set('previewObject', previewObject);
      // var name = previewObject.get("fileInfo").get("files")[0].name;
      // var i = name.indexOf(".");
      // var tableName = name.substr(0, i);
      // this.set('tableName', tableName);
      // var self = this;
      return this.generatePreview(previewObject)
    },
    previewFromHdfs: function () {
      return this.generatePreview();
    },
    uploadTable: function (tableData) {
      console.log("tableData", tableData);
      try {
        this.createTableAndUploadFile(tableData);
      } catch (e) {
        console.log("exception occured : ", e);
        this.setError(e);
        this.hideUploadModal();
      }
    },
    uploadFromHDFS: function () {
      this.set("isLocalUpload", false);
    }
  }
});
