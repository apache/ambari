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
var Ember = require('ember');

require('models/host');
require('controllers/wizard/stack_upgrade/step3_controller');

App.router = Ember.Object.create({
  stackUpgradeController: Ember.Object.create({
    save: function(val) {}
  })
});

describe('App.StackUpgradeStep3Controller', function() {

  var stackUpgradeStep3Controller = App.StackUpgradeStep3Controller.create();

  describe('#runUpgradeErrorCallback', function() {
    var processes = [
      Ember.Object.create({
        status: '',
        isRetry: false,
        name: 'UPGRADE_SERVICES'
      })
    ];

    stackUpgradeStep3Controller.set('processes', processes);
    stackUpgradeStep3Controller.set('content', {cluster: {}, controllerName:'stackUpgradeController'});

    it('check process condition', function() {
      App.testMode = true;
      stackUpgradeStep3Controller.runUpgradeErrorCallback();
      expect(stackUpgradeStep3Controller.get('processes').findProperty('name', 'UPGRADE_SERVICES').get('status')).to.equal('FAILED');
      expect(stackUpgradeStep3Controller.get('processes').findProperty('name', 'UPGRADE_SERVICES').get('isRetry')).to.equal(true);
      expect(stackUpgradeStep3Controller.get('submitButton')).to.equal(false);
      App.testMode = false;
    });
  });

});
