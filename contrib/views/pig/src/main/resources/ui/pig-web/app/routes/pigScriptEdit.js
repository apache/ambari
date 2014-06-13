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

App.PigScriptEditRoute = Em.Route.extend({
  actions:{
    willTransition: function(transition){
      var model = this.controller.get('model');
      if (model.get('isDirty') || model.get('pigScript.isDirty')) {
        return this.send('saveScript',model);
      };
    },
    toresults:function (argument) {
      // DUMMY TRANSITION
      this.transitionTo('pigScriptEdit.results',argument);
    },
    saveScript: function (script) {
      var router = this,
        onSuccess = function(model){
          router.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_saved',{title: script.get('title')}),status:'success'});
        },
        onFail = function(error){
          var trace = null;
          if (error && error.responseJSON.trace)
            trace = error.responseJSON.trace;
          router.send('showAlert', {'message':Em.I18n.t('scripts.alert.save_error'),status:'error',trace:trace});
        };

      return script.get('pigScript').then(function(file){
        return Ember.RSVP.all([file.save(),script.save()]).then(onSuccess,onFail);
      },onFail);
    },
  },
  isExec:false,
  model: function(params) {
    var record;
    var isExist = this.store.all('script').some(function(script) {
      return script.get('id') === params.script_id;
    });
    if (isExist) { 
      record = this.store.find('script',params.script_id);
    } else {
      record = this.store.createRecord('script');
    }
    return record;
  },
  afterModel:function  (model) {
    if (model.get('length') == 0) {
      this.transitionTo('pig');
    }
    this.controllerFor('pig').set('category', model.get('name'));
    model.open();
  },
  renderTemplate: function() {
    this.render('pig/scriptEdit');
  }
});

