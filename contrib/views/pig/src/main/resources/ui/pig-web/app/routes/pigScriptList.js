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

App.PigScriptListRoute = Em.Route.extend({
  actions:{
    createScript:function () {
      var newScript = this.store.createRecord('script');
      this.controllerFor('pigModal').set('content', newScript);
      return this.send('openModal','createScript');
      
    },
    deletescript:function (script) {
      this.controllerFor('pigModal').set('content', script);
      return this.send('openModal','confirmDelete');
    },
    confirmcreate:function (script,filePath) {
      // /tmp/.pigscripts/admin/39.pig
      var route = this;
      var sendAlert = function (status) {
        var alerts = {
          success:Em.I18n.t('scripts.alert.script_created',{title:'New script'}), 
          error: Em.I18n.t('scripts.alert.create_failed')
        };
        return function (data) {
          var trace = null;
          if (status=='error'){
            script.deleteRecord();
            trace = data.responseJSON.trace;
          }
          route.send('showAlert', {message:alerts[status],status:status,trace:trace});
        };
      };
      if (filePath) {
        var file = this.store.createRecord('file',{
          id:filePath,
          fileContent:''
        });
        return file.save().then(function(file){
          script.set('pigScript',file);
          script.save().then(sendAlert('success'),sendAlert('error'));
        },function () {
          file.deleteRecord();
          route.store.find('file', filePath).then(function(file) {
            route.send('showAlert', {message:Em.I18n.t('scripts.alert.file_exist_error'),status:'success'});
            script.set('pigScript',file);
            script.save().then(sendAlert('success'),sendAlert('error'));
          }, sendAlert('error'));
        });
      } else {
          script.save().then(sendAlert('success'),sendAlert('error'));
      }


    },
    confirmdelete:function (script) {
      var router = this;
      var onSuccess = function(model){
            router.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_deleted',{title : model.get('title')}),status:'success'});
          };
      var onFail = function(error){
            var trace = null;
            if (error && error.responseJSON.trace)
              trace = error.responseJSON.trace;
            router.send('showAlert', {'message':Em.I18n.t('scripts.alert.delete_failed'),status:'error',trace:trace});
          };
      script.deleteRecord();
      return script.save().then(onSuccess,onFail);
    }
  },
  enter: function() {
    this.controllerFor('pig').set('category',"scripts");
  },
  model: function(object,transition) {
    return this.modelFor('pig');
  }
});
