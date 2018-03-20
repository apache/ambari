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
var validator = require('utils/validator');

App.NameNodeFederationWizardStep1Controller = Em.Controller.extend({
  name: "nameNodeFederationWizardStep1Controller",

  existingNameServices: function () {
    var isMetricsLoaded = App.router.get('clusterController.isHostComponentMetricsLoaded');
    return isMetricsLoaded ? App.HDFSService.find().objectAt(0).get('masterComponentGroups').mapProperty('name').join(', ') : '';
  }.property('App.router.clusterController.isHostComponentMetricsLoaded'),

  isNameServiceIdValid: function () {
    return validator.isValidNameServiceId(this.get('content.nameServiceId'));
  }.property('content.nameServiceId'),

  next: function () {
    if (this.get('isNameServiceIdValid')) {
      App.router.send('next');
    }
  }
});

