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

require('controllers/main/service/reassign/step2_controller');
require('models/host_component');

describe('App.ReassignMasterWizardStep2Controller', function () {


  var controller = App.ReassignMasterWizardStep2Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create({}),
      services: []
    }),
    renderComponents: Em.K,
    multipleComponents: []
  });
  controller.set('_super', Em.K);

  describe('#loadStep', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(controller, 'clearStep', Em.K);
      sinon.stub(controller, 'loadComponents', Em.K);
      sinon.stub(controller, 'loadStepCallback', Em.K);
      sinon.stub(controller, 'rebalanceSingleComponentHosts', Em.K);
    });

    afterEach(function () {
      App.router.send.restore();
      controller.clearStep.restore();
      controller.loadStepCallback.restore();
      controller.loadComponents.restore();
      controller.rebalanceSingleComponentHosts.restore();
    });

    it('SECONDARY_NAMENODE is absent, reassign component is NAMENODE', function () {
      sinon.stub(App, 'get', function (k) {
        if (k === 'isHaEnabled') return true;
        return Em.get(App, k);
      });
      controller.set('content.reassign.component_name', 'NAMENODE');
      controller.set('content.masterComponentHosts', []);

      controller.loadStep();
      expect(controller.get('showCurrentHost')).to.be.false;
      expect(controller.get('componentToRebalance')).to.equal('NAMENODE');
      expect(controller.get('rebalanceComponentHostsCounter')).to.equal(1);
      App.get.restore();
    });
    it('SECONDARY_NAMENODE is present, reassign component is NAMENODE', function () {
      sinon.stub(App, 'get', function (k) {
        if (k === 'isHaEnabled') return false;
        return Em.get(App, k);
      });
      controller.set('content.reassign.component_name', 'NAMENODE');
      controller.set('content.masterComponentHosts', [
        {
          component: 'SECONDARY_NAMENODE'
        }
      ]);

      controller.loadStep();
      expect(controller.get('showCurrentHost')).to.be.true;
      expect(controller.rebalanceSingleComponentHosts.calledWith('NAMENODE'));
      App.get.restore();
    });
    it('SECONDARY_NAMENODE is absent, reassign component is not NAMENODE', function () {
      controller.set('content.reassign.component_name', 'COMP');
      controller.set('content.masterComponentHosts', []);

      controller.loadStep();
      expect(controller.get('showCurrentHost')).to.be.true;
      expect(controller.rebalanceSingleComponentHosts.calledWith('COMP'));
    });
    it('if HA is enabled then multipleComponents should contain NAMENODE', function () {
      controller.get('multipleComponents').clear();
      sinon.stub(App, 'get', function (k) {
        if (k === 'isHaEnabled') return true;
        return Em.get(App, k);
      });

      controller.loadStep();
      expect(controller.get('multipleComponents')).to.contain('NAMENODE');
      expect(controller.get('multipleComponents')).to.have.length(1);
      App.get.restore();
    });
  });

  describe('#loadComponents', function () {
    it('masterComponentHosts is empty', function () {
      controller.set('content.masterComponentHosts', []);
      controller.set('content.reassign.host_id', 1);

      expect(controller.loadComponents()).to.be.empty;
      expect(controller.get('currentHostId')).to.equal(1);
    });
    it('masterComponentHosts does not contain reassign component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [Em.Object.create({
          componentName: 'COMP1',
          serviceName: 'SERVICE'
        })];
      });
      controller.set('content.masterComponentHosts', [{
        component: 'COMP1',
        hostName: 'host1'
      }]);
      controller.set('content.reassign.host_id', 1);
      controller.set('content.reassign.component_name', 'COMP2');

      expect(controller.loadComponents()).to.eql([
        {
          "component_name": "COMP1",
          "display_name": "Comp1",
          "selectedHost": "host1",
          "isInstalled": true,
          "serviceId": "SERVICE",
          "isServiceCoHost": false,
          "color": "grey"
        }
      ]);
      expect(controller.get('currentHostId')).to.equal(1);

      App.HostComponent.find.restore();
    });
    it('masterComponentHosts contains reassign component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [Em.Object.create({
          componentName: 'COMP1',
          serviceName: 'SERVICE'
        })];
      });
      controller.set('content.masterComponentHosts', [{
        component: 'COMP1',
        hostName: 'host1'
      }]);
      controller.set('content.reassign.host_id', 1);
      controller.set('content.reassign.component_name', 'COMP1');

      expect(controller.loadComponents()).to.eql([
        {
          "component_name": "COMP1",
          "display_name": "Comp1",
          "selectedHost": "host1",
          "isInstalled": true,
          "serviceId": "SERVICE",
          "isServiceCoHost": false,
          "color": "green"
        }
      ]);
      expect(controller.get('currentHostId')).to.equal(1);

      App.HostComponent.find.restore();
    });
  });

  describe('#rebalanceSingleComponentHosts', function () {
    it('hosts is empty', function () {
      controller.set('hosts', []);

      expect(controller.rebalanceSingleComponentHosts()).to.be.false;
    });
    it('currentHostId matches one available host', function () {
      controller.set('hosts', [Em.Object.create({
        host_name: 'host1'
      })]);
      controller.set('currentHostId', 'host1');

      expect(controller.rebalanceSingleComponentHosts()).to.be.false;
    });

    var testCases = [
      {
        title: 'selectedHost = currentHostId and component_name = content.reassign.component_name',
        arguments: {
          selectedHost: 'host1',
          reassignComponentName: 'COMP1'
        },
        result: 'host3'
      },
      {
        title: 'selectedHost not equal to currentHostId and component_name = content.reassign.component_name',
        arguments: {
          selectedHost: 'host2',
          reassignComponentName: 'COMP1'
        },
        result: 'host2'
      },
      {
        title: 'selectedHost = currentHostId and component_name not equal to content.reassign.component_name',
        arguments: {
          selectedHost: 'host1',
          reassignComponentName: 'COMP2'
        },
        result: 'host1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('hosts', [
          Em.Object.create({
            host_name: 'host3'
          }),
          Em.Object.create({
            host_name: 'host2'
          })
        ]);
        controller.set('currentHostId', 'host1');
        controller.set('content.reassign.component_name', test.arguments.reassignComponentName);
        controller.set('selectedServicesMasters', [Em.Object.create({
          component_name: 'COMP1',
          selectedHost: test.arguments.selectedHost
        })]);

        expect(controller.rebalanceSingleComponentHosts('COMP1')).to.be.true;
        expect(controller.get('selectedServicesMasters')[0].get('selectedHost')).to.equal(test.result);
        expect(controller.get('selectedServicesMasters')[0].get('availableHosts').mapProperty('host_name')).to.eql(['host2', 'host3']);
      });
    });
  });

  describe('#updateIsSubmitDisabled', function () {
    var hostComponents = [];
    var isSubmitDisabled = false;

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return hostComponents;
      });
      sinon.stub(controller, '_super', function() {
        return isSubmitDisabled;
      });
    });
    afterEach(function () {
      App.HostComponent.find.restore();
      controller._super.restore();
    });
    it('No host-components, reassigned equal 0', function () {
      expect(controller.updateIsSubmitDisabled()).to.be.true;
      expect(controller.get('submitDisabled')).to.be.true;
    });
    it('Reassign component match existed components, reassigned equal 0', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [Em.Object.create({
        componentName: 'COMP1',
        hostName: 'host1'
      })];
      controller.set('servicesMasters', [{
        selectedHost: 'host1'
      }]);

      expect(controller.updateIsSubmitDisabled()).to.be.true;
      expect(controller.get('submitDisabled')).to.be.true;
    });
    it('Reassign component do not match existed components, reassigned equal 1', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [Em.Object.create({
        componentName: 'COMP1',
        hostName: 'host1'
      })];
      controller.set('servicesMasters', []);

      expect(controller.updateIsSubmitDisabled()).to.be.false;
      expect(controller.get('submitDisabled')).to.be.false;
    });
    it('Reassign component do not match existed components, reassigned equal 2', function () {
      controller.set('content.reassign.component_name', 'COMP1');
      hostComponents = [
        Em.Object.create({
          componentName: 'COMP1',
          hostName: 'host1'
        }),
        Em.Object.create({
          componentName: 'COMP1',
          hostName: 'host2'
        })
      ];
      controller.set('servicesMasters', []);

      expect(controller.updateIsSubmitDisabled()).to.be.true;
      expect(controller.get('submitDisabled')).to.be.true;
    });

    it('submitDisabled is already true', function () {
      isSubmitDisabled = true;

      expect(controller.updateIsSubmitDisabled()).to.be.true;
      expect(controller.get('submitDisabled')).to.be.true;
    });
  });
});
