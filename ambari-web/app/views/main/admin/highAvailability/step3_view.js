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

App.HighAvailabilityWizardStep3View = Em.View.extend({

  templateName: require('templates/main/admin/highAvailability/step3'),
  didInsertElement: function () {
    this.get('controller').loadStep();
  },
  curNameNode: function () {
    var nN = this.get('controller.content.masterComponentHosts').findProperty('isCurNameNode', true);
    return nN.hostName;
  }.property('controller.content.masterComponentHosts'),
  addNameNode: function () {
    var addNN = this.get('controller.content.masterComponentHosts').findProperty('isAddNameNode', true);
    return addNN.hostName;
  }.property('controller.content.masterComponentHosts'),
  secondaryNameNode: function () {
    var sn = this.get('controller.content.masterComponentHosts').findProperty('component', "SECONDARY_NAMENODE");
    return sn.hostName;
  }.property('controller.content.masterComponentHosts'),
  journalNodes: function () {
    return this.get('controller.content.masterComponentHosts').filterProperty('component', "JOURNALNODE");
  }.property('controller.content.masterComponentHosts')

});
