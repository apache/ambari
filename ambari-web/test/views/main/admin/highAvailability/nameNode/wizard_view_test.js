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
require('views/main/admin/highAvailability/nameNode/wizard_view');

describe('App.HighAvailabilityWizardView', function () {
  var view = App.HighAvailabilityWizardView.create({
    controller: Em.Object.create({
      content: {},
      setLowerStepsDisable: Em.K,
      isStepDisabled: []
    })
  });

  describe("#willInsertElement()", function () {
    before(function () {
      sinon.stub(view, 'loadHosts', Em.K);
    });
    after(function () {
      view.loadHosts.restore();
    });
    it("", function () {
      view.set('isLoaded', true);
      view.willInsertElement();
      expect(view.get('isLoaded')).to.be.false;
      expect(view.loadHosts.calledOnce).to.be.true;
    });
  });

  describe("#loadHosts()", function () {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("send ajax call", function () {
      view.loadHosts();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'hosts.high_availability.wizard',
        data: {},
        sender: view,
        success: 'loadHostsSuccessCallback',
        error: 'loadHostsErrorCallback'
      });
    });
  });

  describe("#loadHostsSuccessCallback()", function () {
    before(function () {
      sinon.stub(App.db, 'setHosts', Em.K);
    });
    after(function () {
      App.db.setHosts.restore();
    });
    it("", function () {
      var data = {
          items: [
            {
              Hosts: {
                host_name: 'host1',
                cpu_count: 1,
                total_mem: 1,
                disk_info: []
              }
            }
          ]
        },
        expectedHosts = {
          "host1": {
            "name": "host1",
            "cpu": 1,
            "memory": 1,
            "disk_info": [],
            "bootStatus": "REGISTERED",
            "isInstalled": true
          }
        };
      view.set('isLoaded', false);
      view.loadHostsSuccessCallback(data);
      expect(App.db.setHosts.getCall(0).args[0]).to.eql(expectedHosts);
      expect(view.get('isLoaded')).to.be.true;
      expect(view.get('controller.content.hosts')).to.eql(expectedHosts);
    });
  });

  describe("#loadHostsErrorCallback()", function () {
    it("", function () {
      view.set('isLoaded', false);
      view.loadHostsErrorCallback();
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.spy(view.get('controller'), 'setLowerStepsDisable');
    });
    afterEach(function () {
      view.get('controller').setLowerStepsDisable.restore();
    });
    it("currentStep is 0", function () {
      view.set('controller.currentStep', 0);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.called).to.be.false;
    });
    it("currentStep is 4", function () {
      view.set('controller.currentStep', 4);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.called).to.be.false;
    });
    it("currentStep is 5", function () {
      view.set('controller.currentStep', 5);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.calledWith(5)).to.be.true;
    });
  });
});