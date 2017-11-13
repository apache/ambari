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

App.WizardSelectMpacksView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  }
});

/**
 * View for each mpack in the registry
 */
App.WizardMpackView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/mpack'),

  services: function () {
    return this.get('mpack.versions').filterProperty('displayed')[0].services;
  }.property('mpack.versions.@each.displayed'),

  /**
   * Handle mpack version changed
   * 
   * @param {any} event 
   */
  changeVersion: function (event) {
    const versionId = event.target.value;
    this.get('controller').displayMpackVersion(versionId);
  },

  /**
   * Handle add service button clicked
   *
   * @param  {type} event
   */
  addService: function (event) {
    const serviceId = event.context;
    this.get('controller').addServiceHandler(serviceId);
  }
});

/**
 * View for each selected mpack
 */
App.WizardSelectedMpackVersionView = Em.View.extend({
  templateName: require('templates/wizard/selectMpacks/selectedMpackVersion'),

  mpack: function () {
    return this.get('mpackVersion.mpack.name');
  }.property(),

  /**
   * Handle remove service button clicked.
   *
   * @param  {type} event
   */
  removeService: function (event) {
    const serviceId = event.context;
    this.get('controller').removeServiceHandler(serviceId);
  }
});
