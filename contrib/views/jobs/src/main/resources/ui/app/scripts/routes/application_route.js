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

App.IndexRoute = Ember.Route.extend({

  model: function () {
    return this.modelFor('hiveJobs');
  },

  beforeModel: function () {
    this.transitionTo('jobs');
  }

});

App.JobsRoute = Ember.Route.extend({

  model: function () {
    return this.get('store').find('hiveJob');
  },

  setupController: function(controller, model) {
    this._super(controller, model);
    var hashArray = location.pathname.split('/');
    var view = hashArray[2];
    var version = hashArray[3];
    var instanceName = hashArray[4];
    App.set('view', view);
    App.set('version', version);
    App.set('instanceName', instanceName);

    controller.set('interval', 6000);
    controller.loop('loadJobs', true);
    // This observer should be set with addObserver
    // If it set like ".observes(....)" it triggers two times
    Em.addObserver(controller, 'filterObject.startTime', controller, 'startTimeObserver');
  }

});

App.JobRoute = Ember.Route.extend({

  setupController: function(controller, model) {
    this._super(controller, model);
    controller.set('loaded', false);
    controller.loop('loadJobDetails', true);
  },

  model: function (params) {
    return this.get('store').getById('hiveJob', params.hive_job_id);
  }

});