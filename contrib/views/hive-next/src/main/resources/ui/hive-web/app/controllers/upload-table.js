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
import Uploader from 'hive/adapters/upload-table'
import constants from 'hive/utils/constants';


export default Ember.Controller.extend({
  DEFAULT_CSV_DELIMITER: ',',
  DEFAULT_CSV_QUOTE: '"',
  DEFAULT_CSV_ESCAPE: '\\',
  NON_PRINTABLE_CHARS:[{"id":"0", "name":"NUL", "description":"(null)"},
    {"id":"1", "name":"SOH", "description":"(start of heading)"},
    {"id":"2", "name":"STX", "description":"(start of text)"},
    {"id":"3", "name":"ETX", "description":"(end of text)"},
    {"id":"4", "name":"EOT", "description":"(end of transmission)"},
    {"id":"5", "name":"ENQ", "description":"(enquiry)"},
    {"id":"6", "name":"ACK", "description":"(acknowledge)"},
    {"id":"7", "name":"BEL", "description":"(bell)"},
    {"id":"8", "name":"BS", "description":"(backspace)"},
    {"id":"9", "name":"TAB", "description":"(horizontal tab)"},
    {"id":"11", "name":"VT", "description":"(vertical tab)"},
    {"id":"12", "name":"FF", "description":"(NP form feed - new page)"},
    {"id":"14", "name":"SO", "description":"(shift out)"},
    {"id":"15", "name":"SI", "description":"(shift in)"},
    {"id":"16", "name":"DLE", "description":"(data link escape)"},
    {"id":"17", "name":"DC1", "description":"(device control 1)"},
    {"id":"18", "name":"DC2", "description":"(device control 2)"},
    {"id":"19", "name":"DC3", "description":"(device control 3)"},
    {"id":"20", "name":"DC4", "description":"(device control 4)"},
    {"id":"21", "name":"NAK", "description":"(negative ackowledge)"},
    {"id":"22", "name":"SYN", "description":"(synchronous idle)"},
    {"id":"23", "name":"ETB", "description":"(end of trans. block)"},
    {"id":"24", "name":"CAN", "description":"(cancel)"},
    {"id":"25", "name":"EM", "description":"(end of medium)"},
    {"id":"26", "name":"SUB", "description":"(substitute)"},
    {"id":"27", "name":"ESC", "description":"(escape)"},
    {"id":"28", "name":"FS", "description":"(file separator)"},
    {"id":"29", "name":"GS", "description":"(group separator)"},
    {"id":"30", "name":"RS", "description":"(record separator)"},
    {"id":"31", "name":"US", "description":"(unit separator)"},
    {"id":"32", "name":"Space", "description":""},
    {"id":"127", "name":"DEL", "description":""}
  ],
  COLUMN_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  TABLE_NAME_REGEX: "^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  HDFS_PATH_REGEX: "^[/]{1}.+",  // unix path allows everything but here we have to mention full path so starts with /
  isLocalUpload: Ember.computed.equal("uploadSource", "local"),
  uploadSource: "local",
  COLUMN_NAME_PREFIX : "column",
  hdfsPath: "",
  jobService: Ember.inject.service(constants.namingConventions.job),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  databaseService : Ember.inject.service(constants.namingConventions.database),
  databases : Ember.computed.alias("databaseService.databases"),
  showErrors: false,
  uploader: Uploader.create(),
  baseUrl: "/resources/upload",
  isFirstRowHeader: false, // is first row  header
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
  inputFileTypes :[
    {id : "CSV", name : "CSV"},
    {id : "JSON", name : "JSON"},
    {id : "XML", name : "XML"}
  ],
  inputFileType: null,
  inputFileTypeCSV : Ember.computed.equal('inputFileType.id',"CSV"),
  storedAsTextFile : Ember.computed.equal("selectedFileType","TEXTFILE"),
  storedAsNotTextFile : Ember.computed.not("storedAsTextFile"),
  csvDelimiter: null,
  csvQuote : null,
  csvEscape : null,
  asciiList:[],
  fieldsTerminatedBy: null,
  escapedBy: null,
  fileTypes:[
    "SEQUENCEFILE",
    "TEXTFILE"    ,
    "RCFILE"      ,
    "ORC"         ,
    "PARQUET"     ,
    "AVRO"
  ],
  selectedFileType: null,
  onChangeSelectedFileType: function(){
    if(this.get('selectedFileType') === this.get('fileTypes')[1] && this.get('containsEndlines') === true){
      this.set('containsEndlines', false);
    }
  }.observes("selectedFileType", "containsEndlines"),
  dataTypes: [
    "TINYINT", //
    "SMALLINT", //
    "INT", //
    "BIGINT", //
    "BOOLEAN", //
    "FLOAT", //
    "DOUBLE", //
    "STRING", //
    "BINARY", // -- (Note: Available in Hive 0.8.0 and later)
    "TIMESTAMP", // -- (Note: Available in Hive 0.8.0 and later)
    "DECIMAL", // -- (Note: Available in Hive 0.11.0 and later)
    "DATE", // -- (Note: Available in Hive 0.12.0 and later)
    "VARCHAR", // -- (Note: Available in Hive 0.12.0 and later)
    "CHAR" // -- (Note: Available in Hive 0.13.0 and later)
  ],
  setDefaultDB : function(){
    var self = this;
    var defaultDatabase = this.get('databases').find(
      function(item,index){
        if(item.id == self.DEFAULT_DB_NAME )
          return true;
      }
    );

    console.log("setting the initial database to : " + defaultDatabase);
    self.set("selectedDatabase",defaultDatabase);
  },
  init: function () {
    this.setDefaultDB();
    this.fillAsciiList();
    this.set("selectedFileType", this.get("fileTypes")[3]);
    this.set("inputFileType", this.get("inputFileTypes")[0]);
  },
  onChangeUploadSource : function(){
    this.clearFields();
  }.observes("uploadSource"),
  asciiFormatter: function( option, escape ){
    if( option.data.id  != -1 )
      return "<div><span style='font-weight: bold;margin: 5px'>" + option.data.id + "</span><span style='font-style: italic; color: grey; margin: 5px'>" + option.data.name + "</span></div>";
    else return "<div></div>";
  },
  fillAsciiList: function(){
    var list = this.get('asciiList');
    list.push({"id": -1, "name": ""});
    var nonPrintable = this.get('NON_PRINTABLE_CHARS');
    for( var i = 0 ; i <= 127 ; i++ ){
      if( i == 10 || i == 13 ) continue;
      var charInfo = nonPrintable.find(function(item){
        return item.id == i;
      });
      if(!charInfo){
        charInfo = {"id": i, "name": String.fromCodePoint(i), "description":"" };
      }
      var option = {"id": i, "name": charInfo.name + charInfo.description};
      list.push(option);
      if(i === 44){
        this.set("csvDelimiter", option);
      }
      else if(i === 34){
        this.set("csvQuote", option);
      }
      else if(i === 92){
        this.set("csvEscape", option);
      }
    }
  },
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
    var msg = this.get('uploadProgressInfos').popObject();
  },

  pushUploadProgressInfos : function(info){
    this.get('uploadProgressInfos').pushObject(info);
  },

  clearUploadProgressModal : function(){
    var len = this.get('uploadProgressInfos').length;
    for( var i = 0 ; i < len ; i++){
      this.popUploadProgressInfos();
    }
  },

  hideUploadModal : function(){
    this.clearUploadProgressModal();
    Ember.$("#uploadProgressModal").modal("hide");
  },

  showUploadModal : function(){
    Ember.$("#uploadProgressModal").modal("show");
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
    this.setDefaultDB();
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
    var fetchJobPromise = this.get('jobService').fetchJob(jobId);
      fetchJobPromise.then(function (data) {
        console.log("waitForJobStatus : data : ", data);
        var job = data.job;
        var status = job.status.toUpperCase();
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
          },1000);
        }
      }, function (error) {
        console.log("rejecting waitForJobStatus with : " + error);
        reject(error);
    })
  },

  uploadForPreview: function (files) {
    console.log("uploaderForPreview called.");
    var self = this;
    var csvParams = this.getCSVParams();

    return this.get('uploader').uploadFiles('preview', files, {
      "isFirstRowHeader": self.get("isFirstRowHeader"),
      "inputFileType": self.get("inputFileType").id,
      "csvDelimiter": csvParams.csvDelimiter,
      "csvEscape": csvParams.csvEscape,
      "csvQuote": csvParams.csvQuote
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

  uploadForPreviewFromHDFS: function () {
    console.log("uploadForPreviewFromHDFS called.");
    var self = this;
    var hdfsPath = this.get("hdfsPath");
    this.validateHDFSPath(hdfsPath);
    var csvParams = this.getCSVParams();

    return this.get('uploader').previewFromHDFS({
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "inputFileType": this.get("inputFileType").id,
      "hdfsPath": hdfsPath,
      "csvDelimiter": csvParams.csvDelimiter,
      "csvEscape": csvParams.csvEscape ,
      "csvQuote": csvParams.csvQuote
    });
  },

  generatePreview: function (files) {
    var self = this;
    var promise = null;
    try {
      this.waitForGeneratingPreview();
      if (this.get('isLocalUpload')) {
        promise = this.uploadForPreview(files);
      } else {
        promise = this.uploadForPreviewFromHDFS();
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
    console.log('inside previewTable');
    var self = this;
    var defaultColumnNames = data.header.map(function(item,index){
      return self.COLUMN_NAME_PREFIX + (index + 1);
    });
    this.set("defaultColumnNames",defaultColumnNames);
    this.set("header", data.header);
    this.set('isFirstRowHeader', data.isFirstRowHeader);
    this.set('tableName', data.tableName);
    var firstRow = null;
    if (data.isFirstRowHeader == true) {
      firstRow = data.header.map(function(columnDesc){
        return columnDesc.name;
      });
    }else {
      if(data.rows.length > 0){
        firstRow = data.rows[0].row;
      }else{
        firstRow = [];
      }
    }
    this.set("firstRow", firstRow);
    this.set("rows", data.rows);
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

  createActualTable: function () {
    console.log("createActualTable");
    var self = this;
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateActualTable'));
    var headers = this.get('header');
    var selectedDatabase = this.get('selectedDatabase');
    if (!selectedDatabase) {
      throw new Error(this.translate('hive.errors.emptyDatabase', {database : this.translate("hive.words.database")}));
    }

    this.set('databaseName', this.get('selectedDatabase.id'));
    var databaseName = this.get('databaseName');
    var tableName = this.get('tableName');
    var isFirstRowHeader = this.get('isFirstRowHeader');
    var filetype = this.get("selectedFileType");

    this.validateInput(headers,tableName,databaseName,isFirstRowHeader);
    this.showUploadModal();
    var rowFormat = this.getRowFormat();
    return this.get('uploader').createTable({
      "isFirstRowHeader": isFirstRowHeader,
      "header": headers,
      "tableName": tableName,
      "databaseName": databaseName,
      "hiveFileType":filetype,
      "rowFormat": { "fieldsTerminatedBy" : rowFormat.fieldsTerminatedBy, "escapedBy" : rowFormat.escapedBy}
    });
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
  createTempTable: function () {
    var self = this;
    console.log("createTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateTemporaryTable'));
    var tempTableName = this.generateTempTableName();
    this.set('tempTableName', tempTableName);

    var headers = this.get("header");
    if(this.get("containsEndlines")){
      headers = this.get("header").map(function(item){
        var header = JSON.parse(JSON.stringify(item));
        header.type = "STRING";
        return header;
      });
    }
    return this.get('uploader').createTable({
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "header": headers,
      "tableName": tempTableName,
      "databaseName": this.get('databaseName'),
      "hiveFileType":"TEXTFILE",
      "rowFormat": { "fieldsTerminatedBy" : parseInt('1', 10), "escapedBy" : null}
    });
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

    return this.get('uploader').deleteTable({
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
    return this.deleteTableOnError(this.get("databaseName"),this.get("tableName"),this.translate('hive.words.actual'));
  },

  translate : function(str,vars){
    return Ember.I18n.t(str,vars);
  },
  formatMessage : function(messageId, vars){
    return "<li>" + this.translate(messageId,vars) + "</li>";
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

  uploadFile: function () {
    console.log("uploadFile");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToUploadFile'));
    if( this.get("isLocalUpload")){
      return this.uploadTable();
    }else{
      return this.uploadTableFromHdfs();
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
    return this.deleteTableOnError(this.get("databaseName"),this.get("tempTableName"),this.translate('hive.words.temporary')).then(function(data){
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

  insertIntoTable : function(){
    console.log("insertIntoTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToInsertRows'));

    return this.get('uploader').insertIntoTable({
      "fromDatabase": this.get("databaseName"),
      "fromTable": this.get("tempTableName"),
      "toDatabase": this.get("databaseName"),
      "toTable": this.get("tableName"),
      "header": this.get("header"),
      "unhexInsert": this.get("containsEndlines")
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

  deleteTempTable : function(){
    console.log("deleteTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToDeleteTemporaryTable'));

    return this.deleteTable(
      this.get("databaseName"),
      this.get("tempTableName")
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
  createTableAndUploadFile: function () {
    var self = this;
    self.setError();
    self.createActualTable()
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
        return self.createTempTable(data);
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
        return self.uploadFile(data);
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
        return self.insertIntoTable(data);
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
        return self.deleteTempTable(data);
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
      this.get('notifyService').error(error);
    }else{
      this.set("error");
    }
  },
  previewError: function (error) {
    this.setError(error);
  },
  uploadTableFromHdfs : function(){
    console.log("uploadTableFromHdfs called.");
    if(!(this.get("inputFileTypeCSV") == true && this.get("isFirstRowHeader") == false) ){
      this.pushUploadProgressInfos(this.formatMessage('uploadingFromHdfs'));
    }
    var csvParams = this.getCSVParams();

    return this.get('uploader').uploadFromHDFS({
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "databaseName": this.get('databaseName'),
      "tableName": this.get("tempTableName"),
      "inputFileType": this.get("inputFileType").id,
      "hdfsPath": this.get("hdfsPath"),
      "header": this.get("header"),
      "containsEndlines": this.get("containsEndlines"),
      "csvDelimiter": csvParams.csvDelimiter,
      "csvEscape": csvParams.csvEscape,
      "csvQuote": csvParams.csvQuote
    });
  },
  uploadTable: function () {
    this.printValues();
    var csvParams = this.getCSVParams();

    return this.get('uploader').uploadFiles('upload', this.get('files'), {
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "databaseName" :  this.get('databaseName'),
      "tableName" : this.get("tempTableName"),
      "inputFileType" : this.get("inputFileType").id,
      "header": JSON.stringify(this.get("header")),
      "containsEndlines": this.get("containsEndlines"),
      "csvDelimiter": csvParams.csvDelimiter,
      "csvEscape": csvParams.csvEscape ,
      "csvQuote": csvParams.csvQuote
    });
  },

  onUploadSuccessfull: function (data) {
    console.log("onUploadSuccessfull : ", data);
    this.get('notifyService').success(this.translate('hive.messages.successfullyUploadedTableHeader'),
      this.translate('hive.messages.successfullyUploadedTableMessage' ,{tableName:this.get('tableName') ,databaseName:this.get("databaseName")}));
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
    filesUploaded: function (files) {
      console.log("upload-table.js : uploaded new files : ", files);
      this.clearFields();

      this.set('files', files);
      var name = files[0].name;
      var i = name.indexOf(".");
      var tableName = name.substr(0, i);
      this.set('tableName', tableName);
      var self = this;
      return this.generatePreview(files)
    },
    previewFromHdfs: function () {
      return this.generatePreview();
    },
    uploadTable: function () {
      try {
        this.createTableAndUploadFile();
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
