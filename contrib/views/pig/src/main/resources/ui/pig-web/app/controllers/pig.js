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

App.PigController = Em.ArrayController.extend({
  needs:['scriptEdit'],
  category: 'scripts',
  actions:{
    closeScript:function () {
      this.transitionToRoute('pig.scripts');
    },
    saveScript: function (script,onSuccessCallback) {
      var onSuccess = onSuccessCallback || function(model){
          //this.set('disableScriptControls',false);
          this.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_saved',{title: script.get('title')}),status:'success'});
        }.bind(this),
        onFail = function(error){
          var trace = null;
          if (error && error.responseJSON.trace)
            trace = error.responseJSON.trace;
          this.send('showAlert', {'message':Em.I18n.t('scripts.alert.save_error'),status:'error',trace:trace});
        }.bind(this);

      return script.get('pigScript').then(function(file){
        return Ember.RSVP.all([file.save(),script.save()]).then(onSuccess,onFail);
      },onFail);
    },
    deletescript:function (script) {
      return this.send('openModal','confirmDelete',script);
    },
    confirmdelete:function (script) {
      var onSuccess = function(model){
            this.transitionToRoute('pig.scripts');
            this.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_deleted',{title : model.get('title')}),status:'success'});
          }.bind(this);
      var onFail = function(error){
            var trace = null;
            if (error && error.responseJSON.trace)
              trace = error.responseJSON.trace;
            this.send('showAlert', {'message':Em.I18n.t('scripts.alert.delete_failed'),status:'error',trace:trace});
          }.bind(this);
      script.deleteRecord();
      return script.save().then(onSuccess,onFail);
    },
    copyScript:function (script) {
      script.get('pigScript').then(function (file) {

        var newScript = this.store.createRecord('script',{
          title:script.get('title')+' (copy)',
          templetonArguments:script.get('templetonArguments')
        });

        newScript.save().then(function (savedScript) {
          savedScript.get('pigScript').then(function (newFile) {
            newFile.set('fileContent',file.get('fileContent'));
            newFile.save().then(function () {
              this.send('showAlert', {'message':script.get('title') + ' is copied.',status:'success'});
              if (this.get('activeScript')) {
                this.send('openModal','gotoCopy',savedScript);
              }
            }.bind(this));
          }.bind(this));
        }.bind(this));
      }.bind(this));
    }
  },

  activeScriptId:null,

  disableScriptControls:Em.computed.alias('controllers.scriptEdit.isRenaming'),

  activeScript:function () {
    return (this.get('activeScriptId'))?this.get('content').findBy('id',this.get('activeScriptId').toString()):null;
  }.property('activeScriptId',"content.[]"),

  /*
   *Is script or script file is dirty.
   * @return {boolean}
   */
  scriptDirty:function () {
    return this.get('activeScript.isDirty') || this.get('activeScript.pigScript.isDirty');
  }.property('activeScript.pigScript.isDirty','activeScript.isDirty'),

  saveEnabled:function () {
    return this.get('scriptDirty') && !this.get('disableScriptControls');
  }.property('scriptDirty','disableScriptControls')
});
