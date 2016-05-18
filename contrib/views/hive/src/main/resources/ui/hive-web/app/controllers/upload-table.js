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
  COLUMN_NAME_REGEX:"^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  TABLE_NAME_REGEX:"^[a-zA-Z]{1}[a-zA-Z0-9_]*$",
  isLocalUpload : Ember.computed.equal("uploadSource","local"),
  uploadSource : "local",
  COLUMN_NAME_PREFIX : "column",
  hdfsPath : "",
  jobService: Ember.inject.service(constants.namingConventions.job),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  needs: ['databases'],
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
  showPreview : false,
  onChangeUploadSource : function(){
    this.clearFields();
  }.observes("uploadSource"),
  uploadProgressInfo : Ember.computed("uploadProgressInfos.[]",function(){
    var info = "";
    for( var i = 0 ; i < this.get('uploadProgressInfos').length ; i++)
        info += this.get('uploadProgressInfos').objectAt(i);

    return new Ember.Handlebars.SafeString(info);
  }),
  inputFileTypes :[
    {id : "CSV", name : "CSV"},
    {id : "JSON", name : "JSON"},
    {id : "XML", name : "XML"}
  ],
  inputFileType : {id : "CSV", name : "CSV"},
  inputFileTypeCSV : Ember.computed.equal('inputFileType.id',"CSV"),
  fileTypes:[
    "SEQUENCEFILE",
    "TEXTFILE"    ,
    "RCFILE"      ,
    "ORC"         ,
    "PARQUET"     ,
    "AVRO"
    //,
    //"INPUTFORMAT"  -- not supported as of now.
  ],
  selectedFileType: "ORC",
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
  _setHeaderElements : function(header,valueArray){
    header.forEach(function (item, index) {
      Ember.set(item, 'name',  valueArray.objectAt(index));
    }, this);
  },
  isFirstRowHeaderDidChange: function () {
    if (this.get('isFirstRowHeader') != null && typeof this.get('isFirstRowHeader') !== 'undefined') {
      if (this.get('isFirstRowHeader') == false) {
        if (this.get('rows')) {
          this.get('rows').unshiftObject({row: this.get('firstRow')});
          this._setHeaderElements(this.get('header'),this.get('defaultColumnNames'));
        }
      } else if( this.get('header') ) { // headers are available
        // take first row of
        this._setHeaderElements(this.get('header'),this.get('firstRow'));
        this.get('rows').removeAt(0);
      }

      this.printValues();
    }
  }.observes('isFirstRowHeader'),

  popUploadProgressInfos : function(){
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
    this.set("error");
    this.set('isFirstRowHeader',false);
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

  generateTempTableName : function(){
    var text = "";
    var possible = "abcdefghijklmnopqrstuvwxyz";

    for( var i=0; i < 30; i++ )
      text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
  },

  waitForJobStatus: function (jobId, resolve, reject) {
    console.log("finding status of job: ", jobId);
    var self = this;
    var fetchJobPromise = this.get('jobService').fetchJobStatus(jobId);
      fetchJobPromise.then(function (data) {
        var status = data.jobStatus;
        if (status == "Succeeded") {
          resolve(status);
        } else if (status == "Canceled" || status == "Closed" || status == "Error") {
          reject(new Error(status));
        } else {
          self.waitForJobStatus(jobId, resolve, reject);
        }
      }, function (error) {
        console.log("rejecting waitForJobStatus with : " + error);
        reject(error);
    })
  },

  uploadForPreview: function (files) {
    console.log("uploaderForPreview called.");
    var self = this;
    return this.get('uploader').uploadFiles('preview', files, {"isFirstRowHeader" : self.get("isFirstRowHeader"), "inputFileType" : self.get("inputFileType").id});
  },

  uploadForPreviewFromHDFS : function(){
    console.log("uploadForPreviewFromHDFS called.");
    return this.get('uploader').previewFromHDFS({"isFirstRowHeader" : this.get("isFirstRowHeader"),"inputFileType" : this.get("inputFileType").id , "hdfsPath" : this.get("hdfsPath") });
  },

  generatePreview : function(files){
    var self = this;
    var promise = null;
    this.waitForGeneratingPreview();
    if(this.get('isLocalUpload')){
      promise = this.uploadForPreview(files);
    }else{
      promise = this.uploadForPreviewFromHDFS();
    }

    return promise.then(function (data) {
        self.onGeneratePreviewSuccess(data);
    }, function (error) {
        self.onGeneratePreviewFailure(error);
    });
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
      return self.COLUMN_NAME_PREFIX + index;
    });
    this.set("defaultColumnNames",defaultColumnNames);
    this.set("header", data.header);
    this.set("firstRow", data.rows[0].row);
    this.set('isFirstRowHeader', data.isFirstRowHeader);
    this.set('tableName',data.tableName);
    if(data.isFirstRowHeader == true){
        data.rows = data.rows.slice(1);
    }
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

  createActualTable : function(){
    console.log("createActualTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateActualTable'));
    var headers = this.get('header');
    var selectedDatabase = this.get('selectedDatabase');
    if (!selectedDatabase) {
      throw new Error(this.translate('hive.errors.emptyDatabase', {database : this.translate("hive.words.database")}));
    }

    this.set('databaseName', this.get('selectedDatabase').get('name'));
    var databaseName = this.get('databaseName');
    var tableName = this.get('tableName');
    var isFirstRowHeader = this.get('isFirstRowHeader');
    var filetype = this.get("selectedFileType");

    this.validateInput(headers,tableName,databaseName,isFirstRowHeader);
    this.showUploadModal();

    return this.get('uploader').createTable({
      "isFirstRowHeader": isFirstRowHeader,
      "header": headers,
      "tableName": tableName,
      "databaseName": databaseName,
      "fileType":filetype
    });
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

  onCreateActualTableSuccess : function(){
    console.log("onCreateTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyCreatedActualTable'));
  },

  onCreateActualTableFailure : function(error){
    console.log("onCreateActualTableFailure");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.failedToCreateActualTable'));
    this.setError(error);
  },

  createTempTable : function(){
    console.log("createTempTable");
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.startingToCreateTemporaryTable'));
    var tempTableName = this.generateTempTableName();
    this.set('tempTableName',tempTableName);
    return this.get('uploader').createTable({
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "header": this.get("header"),
      "tableName": tempTableName,
      "databaseName": this.get('databaseName'),
      "fileType":"TEXTFILE"
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

  onCreateTempTableSuccess : function(){
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

  deleteTableOnError : function(databaseName,tableName, tableLabel){
      //delete table and wait for delete job
    var self = this;
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.deletingTable',{table:tableLabel}));

    return this.deleteTable(databaseName,tableName).then(function(data){
      return new Ember.RSVP.Promise(function(resolve,reject){
        self.waitForJobStatus(data.jobId,resolve,reject);
      });
    }).then(function(){
      self.popUploadProgressInfos();
      self.pushUploadProgressInfos(this.formatMessage('hive.messages.succesfullyDeletedTable',{table:tableLabel}));
      return Ember.RSVP.Promise.resolve();
    },function(err){
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
    this.pushUploadProgressInfos();
    return this.rollBackActualTableCreation().then(function(data){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    },function(err){
      return Ember.RSVP.Promise.reject(error); // always reject for the flow to stop
    });
  },

  uploadFile : function(){
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

  rollBackTempTableCreation : function(){
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
      "fromDatabase":  this.get("databaseName"),
      "fromTable": this.get("tempTableName"),
      "toDatabase": this.get("databaseName"),
      "toTable": this.get("tableName")
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

  onInsertIntoTableSuccess : function(){
    console.log("onInsertIntoTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyInsertedRows'));
  },

  onInsertIntoTableFailure : function(error){
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

  onDeleteTempTableSuccess : function(){
    console.log("onDeleteTempTableSuccess");
    this.popUploadProgressInfos();
    this.pushUploadProgressInfos(this.formatMessage('hive.messages.successfullyDeletedTemporaryTable'));
    this.onUploadSuccessfull();
  },

  onDeleteTempTableFailure : function(error){
    console.log("onDeleteTempTableFailure");
    this.setError(error);
    this.setError(this.formatMessage('hive.messages.manuallyDeleteTable',{databaseName:this.get('databaseName'), tableName: this.get("tempTableName")}));
  },

  createTableAndUploadFile : function(){
    var self = this;
    self.setError();
    self.createActualTable()
      .then(function(data){
        return self.waitForCreateActualTable(data.jobId);
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
          self.onCreateActualTableFailure(new Error("Server job for creation of actual table failed."));
        }
        throw error;
      })
      .then(function(data){
        return self.waitForCreateTempTable(data.jobId);
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
          return self.onCreateTempTableFailure(new Error("Server job for creation of temporary table failed."));
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
          return self.onUploadingFileFailure(new Error("Server job for upload of file failed."));
        }
        throw error;
      })
      .then(function(data){
        return self.waitForInsertIntoTable(data.jobId);
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
          return self.onInsertIntoTableFailure(new Error("Server job for insert from temporary to actual table failed."));
        }
        throw error;
      })
      .then(function(data){
        return self.waitForDeleteTempTable(data.jobId);
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
          self.onDeleteTempTableFailure(new Error("Server job for deleting temporary table failed."));
        }
        throw error;
      }).catch(function(error){
        console.log("inside catch : ", error);
      }).finally(function(){
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
      console.log("upload table error : ", error);
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
    return  this.get('uploader').uploadFromHDFS({
        "isFirstRowHeader": this.get("isFirstRowHeader"),
        "databaseName" :  this.get('databaseName'),
        "tableName" : this.get("tempTableName"),
        "inputFileType" : this.get("inputFileType").id,
        "hdfsPath" : this.get("hdfsPath")
      });
  },
  uploadTable: function () {
    this.printValues();
    return this.get('uploader').uploadFiles('upload', this.get('files'), {
      "isFirstRowHeader": this.get("isFirstRowHeader"),
      "databaseName" :  this.get('databaseName'),
      "tableName" : this.get("tempTableName"),
      "inputFileType" : this.get("inputFileType").id
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
    previewFromHdfs : function(){
      return this.generatePreview();
    },
    uploadTable : function(){
      try{
        this.createTableAndUploadFile();
      }catch(e){
        console.log("exception occured : ", e);
        this.setError(e);
        this.hideUploadModal();
      }
    },
    uploadFromHDFS : function(){
      this.set("isLocalUpload",false);
    }
  }
});
