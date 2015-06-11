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

var App = require('app');
var bind = Ember.run.bind;

App.FilesController = Ember.ArrayController.extend({
  actions:{
    moveFile:function (opt,fileArg) {
      var src, title,
          file = fileArg || this.get('selectedFiles.firstObject'),
          moving = this.get('movingFile');

      if (opt == 'cut') {
        src = file.toJSON({includeId: true});
        src = Em.merge(src,{name:file.get('name'),path:file.get('path')});
        this.set('movingFile',src);
      }

      if (opt == 'move') {
        this.store.move(moving.path,[this.get('path'),moving.name].join('/').replace('//','/'))
          .then(bind(this,this.set,'movingFile',null),bind(this,this.throwAlert));
      }

      if (opt == 'cancel') {
        this.set('movingFile',null);
      }
    },
    showRenameInput:function () {
      this.toggleProperty('isRenaming');
    },
    renameDir:function (path,newName) {
      var _this = this,
          basedir = path.substring(0,path.lastIndexOf('/')+1);
          newPath = basedir + newName;

      if (path === newPath) {
        return false;
      }

      this.store.listdir(basedir).then(function (listdir) {
        var recordExists = listdir.isAny('id',newPath);

        listdir.forEach(function (file) {
          _this.store.unloadRecord(file);
        });

        if (recordExists) {
          return _this.throwAlert({message:newPath + ' already exists.'});
        }

        return _this.store.move(path,newPath);
      }).then(function (newDir) {
        if (newDir) {
          _this.store.unloadRecord(newDir);
          _this.set('path',newPath);
        }
      }).catch(bind(this,this.throwAlert));

    },
    deleteFile:function (deleteForever) {
      var self = this,
          selected = this.get('selectedFiles'),
          moveToTrash = !deleteForever;
      if (this.get('content.meta.writeAccess')) {
        selected.forEach(function (file) {
          self.store.remove(file,moveToTrash).then(null,bind(self,self.deleteErrorCallback,file));
        });
      } else {
        this.throwAlert({message:'Permission denied'});
      }
    },
    download:function (option) {
      var files = this.get('selectedFiles').filterBy('readAccess',true);
      this.store.linkFor(files,option).then(function (link) {
        window.location.href = link;
      });
    },
    mkdir:function (newDirName) {
      this.store.mkdir(newDirName)
        .then(bind(this,this.mkdirSuccessCalback),bind(this,this.throwAlert));
    },
    upload:function (opt) {
      if (opt === 'open') {
        this.set('isUploading',true);
      }

      if (opt === 'close') {
        this.set('isUploading',false);
      }
    },
    sort:function (pr) {
      var currentProperty = this.get('sortProperties');
      if (pr == currentProperty[0] || pr == 'toggle') {
        this.toggleProperty('sortAscending');
      } else{
        this.set('sortProperties',[pr]);
        this.set('sortAscending',true);
      }
    },
    confirmChmod:function (file) {
      this.store
        .chmod(file)
        .then(null,Em.run.bind(this,this.chmodErrorCallback,file));
    },
    clearSearchField:function () {
      this.set('searchString','');
    }
  },
  init:function () {
    if (App.testing) {
      return this._super();
    }
    var controller = this;
    var adapter = controller.store.adapterFor('file');
    var url = adapter.buildURL('upload');
    this.uploader.set('url',url);
    this.uploader.on('didUpload', function (payload) {
      controller.store.pushPayload('file', {'file': payload });
    });
    this._super();
  },

  sortProperties: ['name'],
  sortAscending: true,

  needs: ["file"],
  movingFile:null,
  uploader:App.Uploader,
  isRenaming:false,
  isUploading:false,
  queryParams: ['path'],
  path: '/',
  isRootDir:Ember.computed.equal('path', '/'),
  hideMoving:function () {
    return (this.movingFile)?[this.path,this.movingFile.name].join('/').replace('//','/')===this.movingFile.path:false;
  }.property('movingFile','path'),
  currentDir:function () {
    return this.get('path').split('/').get('lastObject') || '/';
  }.property('path'),
  selectedOne:Ember.computed.equal('selectedFiles.length', 1),
  isSelected:Ember.computed.gt('selectedFiles.length', 0),
  selectedFiles:function () {
    return this.get('content').filterBy('selected', true);
  }.property('content.@each.selected'),
  canConcat:function () {
    return this.get('selectedFiles').filterProperty('isDirectory').get('length')===0;
  }.property('selectedFiles.length'),

  searchString:'',
  fileList: function () {
    var fileList = this.get('arrangedContent');
    var search = this.get('searchString');
    return (search)?fileList.filter(function (file) {
      return !!file.get('name').match(search);
    }):fileList;
  }.property('arrangedContent','searchString'),

  mkdirSuccessCalback:function (newDir) {
    if (newDir.get('path') != [this.get('path'),newDir.get('name')].join('/')){
      newDir.unloadRecord();
      newDir.store.listdir(this.get('path'));
    }
  },

  clearSearch:function () {
    this.set('searchString','');
  }.observes('path'),

  deleteErrorCallback:function (record,error) {
    this.model.pushRecord(record);
    this.throwAlert(error);
  },

  chmodErrorCallback:function (record,error) {
    record.rollback();
    this.throwAlert({message:'Permissions change failed'});
  },

  throwAlert:function (error) {
    this.send('showAlert',error);
  },

  showSpinner:function () {
    this.set('isLoadingFiles',true);
  },

  hideSpinner:function () {
    this.set('isLoadingFiles',false);
  }
});


