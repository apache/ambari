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

App.JobRoute = Em.Route.extend({
    actions: {
      error: function(error, transition) {
        Em.warn(error.stack);
        var trace = null;
        if (error && error.responseJSON.trace)
          trace = error.responseJSON.trace;
        transition.send('showAlert', {'message':Em.I18n.t('job.alert.load_error',{message:error.message}), status:'error', trace:trace});
        this.transitionTo('pig.scriptList');
      },
      navigate:function (argument) {
        return this.transitionTo(argument.route)
      }
    },
    setupController: function(controller, model) {
      controller.set('model', model);
      this.controllerFor('poll').set('model', model);
    },
    afterModel:function (job,arg) {
      this.controllerFor('poll').get('pollster').start(job);
    },
    deactivate: function() {
      this.controllerFor('poll').get('pollster').stop();
    },
    renderTemplate: function() {
      this.render('pig/scriptEdit', {controller: 'job' });
      this.render('pig/job', {into:'pig/scriptEdit',outlet: 'main', controller: 'poll' });
      this.render('pig/scriptResultsNav',{into:'pig/scriptEdit',outlet: 'nav'});
    }
});

App.JobIndexRoute = Em.Route.extend({
  enter: function() {
      this.controllerFor('pig').set('category',"");
      this.controllerFor('job').set('category',"edit");
    },
});
