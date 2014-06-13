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

App.PigRoute = Em.Route.extend({
  beforeModel: function(transition) {
    App.set('previousTransition', transition);
  },
  redirect: function () {
    testsConducted = App.get("smokeTests");
    if (!testsConducted) {
        App.set("smokeTests", true);
        this.transitionTo('splash');
    }
  },
  actions: {
    gotoSection: function(nav) {
      var location = (nav.hasOwnProperty('url'))?[nav.url]:['pig.scriptEdit',nav.get('id')];
      this.transitionTo.apply(this,location);
    },
    close:function (script) {
      var self = this;
      script.close().save().then(function() {
        if (self.get('controller.category') == script.get('name')) {
          opened = self.get('controller.openScripts');
          if (opened.length > 0 && opened.filterBy('id',script.get('id')).length == 0){
            self.transitionTo('pig.scriptEdit',opened.get(0));
          } else {
            self.transitionTo('pig.scriptList');
          }
        }
        self.send('showAlert', {'message':Em.I18n.t('scripts.alert.script_saved',{title: script.get('title')}), status:'success'});
      },function (error) {
        //script.open();
        var trace = null;
        if (error && error.responseJSON.trace)
          trace = error.responseJSON.trace;
        self.send('showAlert', {'message': Em.I18n.t('scripts.alert.save_error_reason',{message:error.statusText}) , status:'error', trace:trace});
      });
    },
    showAlert:function (alert) {
      var pigUtilAlert = this.controllerFor('pigUtilAlert');
      return pigUtilAlert.content.pushObject(Em.Object.create(alert));
    },
    openModal: function(modalName,controller) {
      return this.render(modalName, {
        into: 'pig',
        outlet: 'modal',
        controller:'pigModal'
      });
    },
    closeModal: function() {
      return this.disconnectOutlet({
        outlet: 'modal',
        parentView: 'pig'
      });
    }
  },
  model: function() {
    return this.store.find('script');
  },
  renderTemplate: function() {
    this.render('pig');
    this.render('pig/util/alert', {into:'pig',outlet:'alert',controller: 'pigUtilAlert' });
  }
});
