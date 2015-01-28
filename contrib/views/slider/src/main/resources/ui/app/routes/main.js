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

App.ApplicationRoute = Ember.Route.extend({
  renderTemplate: function() {
    this.render();
    var controller = this.controllerFor('tooltip-box');
    this.render("bs-tooltip-box", {
      outlet: "bs-tooltip-box",
      controller: controller,
      into: "application"
    });
  }
});

App.IndexRoute = Ember.Route.extend({

  model: function () {
    return this.modelFor('sliderApps');
  },

  redirect: function () {
    this.transitionTo('slider_apps');
  }

});

App.SliderAppsRoute = Ember.Route.extend({

  model: function () {
    return this.store.all('sliderApp');
  },


  setupController: function(controller, model) {
    controller.set('model', model);

    // Load sliderConfigs to storage
    App.SliderApp.store.pushMany('sliderConfig', Em.A([
      Em.Object.create({id: 1, required: false, viewConfigName: 'site.global.metric_collector_host', displayName: 'metricsServer'}),
      Em.Object.create({id: 2, required: false, viewConfigName: 'site.global.metric_collector_port', displayName: 'metricsPort'}),
      Em.Object.create({id: 3, required: false, viewConfigName: 'site.global.metric_collector_lib', displayName: 'metricsLib'}),
      Em.Object.create({id: 4, required: false, viewConfigName: 'yarn.rm.webapp.url', displayName: 'yarnRmWebappUrl'})
    ]));
  },

  actions: {
    createApp: function () {
      this.transitionTo('createAppWizard');
    }
  }
});

App.SliderAppRoute = Ember.Route.extend({

  model: function(params) {
    return this.store.all('sliderApp', params.slider_app_id);
  }

});