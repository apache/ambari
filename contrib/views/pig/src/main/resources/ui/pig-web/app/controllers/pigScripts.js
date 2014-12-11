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

App.PigScriptsController = Em.ArrayController.extend(App.Pagination,{
  sortProperties: ['dateCreatedUnix'],
  sortAscending: false,
  needs:['pig'],
  actions:{
    createScript:function () {
      var newScript = this.store.createRecord('script');
      return this.send('openModal', 'createScript', newScript);
    },
    confirmcreate:function (script,filePath) {
      var _this = this;
      var bnd = Em.run.bind;

      if (filePath) {
        try {
          var file = this.store.createRecord('file',{
            id:filePath,
            fileContent:''
          });
        } catch (e) {
          return this.createScriptError(script,e);
        }
        return file.save()
          .then(function(file){
              script.set('pigScript',file);
              script.save().then(bnd(_this,_this.createScriptSuccess),bnd(_this,_this.createScriptError,script));
            },
            function () {
              file.deleteRecord();
              _this.store.find('file', filePath).then(function(file) {
                _this.send('showAlert', {message:Em.I18n.t('scripts.alert.file_exist_error'),status:'success'});
                script.set('pigScript',file);
                script.save().then(bnd(_this,_this.createScriptSuccess),bnd(_this,_this.createScriptError,script));
              },bnd(_this,_this.createScriptError,script));
          });
      } else {
          script.save().then(bnd(this,this.createScriptSuccess),bnd(this,this.createScriptError,script));
      }
    },
    deletescript:function (script) {
      this.get('controllers.pig').send('deletescript',script);
    },
    copyScript:function (script) {
      this.get('controllers.pig').send('copyScript',script);
    }
  },

  createScriptSuccess:function (script,s) {
    this.send('showAlert', { message:Em.I18n.t('scripts.alert.script_created',{title:script.get('title')}), status:'success' });
    this.transitionToRoute('script.edit',script);
  },
  createScriptError:function (script,error) {
    var trace = null;
    trace = (error.responseJSON)?error.responseJSON.trace:error.message;

    script.deleteRecord();

    this.send('showAlert', {message:Em.I18n.t('scripts.alert.create_failed'),status:'error',trace:trace});
  },

  jobs:function () {
    return this.store.find('job');
  }.property()
});
