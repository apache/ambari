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
var testHelpers = require('test/helpers');

describe('App.RollbackHighAvailabilityWizardStep2Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.RollbackHighAvailabilityWizardStep2Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#pullCheckPointStatus', function() {

    it('App.ajax.send should be called', function() {
      controller.set('content.activeNNHost', 'host1');
      controller.pullCheckPointStatus();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getNnCheckPointStatus');
      expect(args[0]).to.be.eql({
        name: 'admin.high_availability.getNnCheckPointStatus',
        sender: controller,
        data: {
          hostName: 'host1'
        },
        success: 'checkNnCheckPointStatus'
      });
    });
  });

});

