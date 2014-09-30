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
      Em.Object.create({id: 1, required: true, viewConfigName: 'hdfs.url', displayName: 'hdfsAddress', linkedService: 'HDFS'}),
      Em.Object.create({id: 2, required: true, viewConfigName: 'yarn.rm.url', displayName: 'yarnResourceManager', linkedService: 'YARN'}),
      Em.Object.create({id: 3, required: true, viewConfigName: 'yarn.rm.webapp.url', displayName: 'yarnResourceManagerWebapp', linkedService: 'YARN'}),
      Em.Object.create({id: 4, required: true, viewConfigName: 'yarn.rm.scheduler.url',  displayName: 'yarnResourceManagerScheduler'}),
      Em.Object.create({id: 5, required: true, viewConfigName: 'zookeeper.quorum', displayName: 'zookeeperQuorum', linkedService: 'ZOOKEEPER'}),
      Em.Object.create({id: 6, required: false, viewConfigName: 'ganglia.server.hostname', displayName: 'gangliaServer'}),
      Em.Object.create({id: 7, required: false, viewConfigName: 'ganglia.additional.clusters', displayName: 'gangliaClusters'}),
      Em.Object.create({id: 8, required: false, viewConfigName: 'slider.user', displayName: 'sliderUser'}),
      Em.Object.create({id: 9, required: true, viewConfigName: 'slider.security.enabled', displayName: 'sliderSecurityEnabled'}),
      Em.Object.create({id: 10, required: false, requireDependsOn: 9, viewConfigName: 'yarn.rm.kerberos.principal', displayName: 'yarnResourceManagerPrincipal'}),
      Em.Object.create({id: 11, required: false, requireDependsOn: 9, viewConfigName: 'dfs.namenode.kerberos.principal', displayName: 'dfsNamenodeKerberosPrincipal'}),
      Em.Object.create({id: 12, required: false, requireDependsOn: 9, viewConfigName: 'view.kerberos.principal', displayName: 'viewKerberosPrincipal'}),
      Em.Object.create({id: 13, required: false, requireDependsOn: 9, viewConfigName: 'view.kerberos.principal.keytab', displayName: 'ViewKerberosPrincipalKeytab'})
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