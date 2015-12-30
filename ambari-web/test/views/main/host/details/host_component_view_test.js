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
require('models/host_component');
require('views/main/host/details/host_component_view');

var hostComponentView;

describe('App.HostComponentView', function() {

  beforeEach(function() {
    sinon.stub(App.router, 'get', function (k) {
      if (k === 'mainHostDetailsController.content') return Em.Object.create({
        hostComponents: [
          {
            componentName: 'component'
          }
        ]
      });
      return Em.get(App.router, k);
    });
    hostComponentView = App.HostComponentView.create({
      startBlinking: function(){},
      doBlinking: function(){},
      getDesiredAdminState: function(){return $.ajax({});},
      content: Em.Object.create({
        componentName: 'component'
      }),
      hostComponent: Em.Object.create()
    });
  });

  afterEach(function () {
    App.router.get.restore();
  });

  describe('#disabled', function() {

    var tests = Em.A([
      {
        parentView: {content: {healthClass: 'health-status-DEAD-YELLOW'}},
        noActionAvailable: '',
        isRestartComponentDisabled: true,
        e: 'disabled'
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: '',
        isRestartComponentDisabled: true,
        e: ''
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: 'hidden',
        isRestartComponentDisabled: true,
        e: 'disabled'
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        noActionAvailable: 'hidden',
        isRestartComponentDisabled: false,
        e: ''
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          parentView: test.parentView,
          noActionAvailable: test.noActionAvailable,
          isRestartComponentDisabled: test.isRestartComponentDisabled
        });
        expect(hostComponentView.get('disabled')).to.equal(test.e);
      });
    });

  });

  describe('#isUpgradeFailed', function() {

    var tests = ['UPGRADE_FAILED'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isUpgradeFailed')).to.equal(e);
      });
    });

  });

  describe('#isInstallFailed', function() {

    var tests = ['INSTALL_FAILED'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isInstallFailed')).to.equal(e);
      });
    });

  });

  describe('#isStart', function() {

    var tests = ['STARTED','STARTING'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isStart')).to.equal(e);
      });
    });

  });

  describe('#isStop', function() {

    var tests = ['INSTALLED'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isStop')).to.equal(e);
      });
    });

  });

  describe('#isInstalling', function() {

    var tests = ['INSTALLING'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isInstalling')).to.equal(e);
      });
    });

  });

  describe('#isInit', function() {

    var tests = ['INIT'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isInit')).to.equal(e);
      });
    });

  });

  describe('#noActionAvailable', function() {

    var tests = ['STARTING', 'STOPPING', 'UNKNOWN', 'DISABLED'];
    var testE = 'hidden';
    var defaultE = '';

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('noActionAvailable')).to.equal(e);
      });
    });

  });

  describe('#isActive', function() {

    var tests = Em.A([
      {passiveState: 'OFF', e: true},
      {passiveState: 'ON', e: false},
      {passiveState: 'IMPLIED', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.get('content').set('passiveState', test.passiveState);
        expect(hostComponentView.get('isActive')).to.equal(test.e);
      });
    });

  });

  describe('#isRestartComponentDisabled', function() {

    var tests = ['STARTED'];
    var testE = false;
    var defaultE = true;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isRestartComponentDisabled')).to.equal(e);
      });
    });

  });

  describe('#isDeleteComponentDisabled', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(App.HostComponent, 'getCount').returns(1);
    });
    afterEach(function() {
      this.mock.restore();
      App.HostComponent.getCount.restore();
    });

    it('delete is disabled because min cardinality 1', function() {
      this.mock.returns(Em.Object.create({minToInstall: 1}));
      hostComponentView.get('hostComponent').set('componentName', 'C1');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.true;
    });

    it('delete is disabled because min cardinality 0 and status INSTALLED', function() {
      this.mock.returns(Em.Object.create({minToInstall: 0}));
      hostComponentView.get('hostComponent').set('workStatus', 'INIT');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.false;
    });

    it('delete is enabled because min cardinality 0 and status STARTED', function() {
      this.mock.returns(Em.Object.create({minToInstall: 0}));
      hostComponentView.get('hostComponent').set('workStatus', 'STARTED');
      hostComponentView.propertyDidChange('isDeleteComponentDisabled');
      expect(hostComponentView.get('isDeleteComponentDisabled')).to.be.true;
    });
  });

  describe('#componentTextStatus', function() {

    var tests = Em.A([
      {
        componentTextStatus: 'status',
        hostComponent: null,
        e: 'status',
        m: 'get content status'
      },
      {
        componentTextStatus: 'status',
        hostComponent: Em.Object.create({componentTextStatus: 'new_status'}),
        e: 'new_status',
        m: 'get hostComponent status'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          hostComponent: test.hostComponent,
          content: Em.Object.create()
        });
        hostComponentView.get('content').set('componentTextStatus', test.componentTextStatus);
        expect(hostComponentView.get('componentTextStatus')).to.equal(test.e);
      });
    });

  });

  describe('#workStatus', function() {

    var tests = Em.A([
      {
        workStatus: 'status',
        hostComponent: null,
        e: 'status',
        m: 'get content workStatus'
      },
      {
        workStatus: 'status',
        hostComponent: Em.Object.create({workStatus: 'new_status'}),
        e: 'new_status',
        m: 'get hostComponent workStatus'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          hostComponent: test.hostComponent,
          content: Em.Object.create()
        });
        hostComponentView.get('content').set('workStatus', test.workStatus);
        expect(hostComponentView.get('workStatus')).to.equal(test.e);
      });
    });

  });

  describe('#statusClass', function() {

    var tests = Em.A([
      {
        workStatus: App.HostComponentStatus.install_failed,
        passiveState: 'OFF',
        e: 'health-status-color-red icon-cog'
      },
      {
        workStatus: App.HostComponentStatus.installing,
        passiveState: 'OFF',
        e: 'health-status-color-blue icon-cog'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'ON',
        e: 'health-status-started'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'IMPLIED',
        e: 'health-status-started'
      },
      {
        workStatus: 'STARTED',
        passiveState: 'OFF',
        e: 'health-status-started'
      }
    ]);

    tests.forEach(function(test) {
      it(test.workStatus + ' ' + test.passiveState, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          content: Em.Object.create(),
          hostComponent: Em.Object.create()
        });
        hostComponentView.get('hostComponent').set('workStatus',test.workStatus);
        hostComponentView.get('content').set('passiveState', test.passiveState);
        expect(hostComponentView.get('statusClass')).to.equal(test.e);
      });
    });

  });

  describe('#isInProgress', function() {

    var tests = ['STOPPING', 'STARTING'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isInProgress')).to.equal(e);
      });
    });

  });

  describe('#statusIconClass', function() {
    var tests = Em.A([
      {s: 'health-status-started', e: App.healthIconClassGreen},
      {s: 'health-status-starting', e: App.healthIconClassGreen},
      {s: 'health-status-installed', e: App.healthIconClassRed},
      {s: 'health-status-stopping', e: App.healthIconClassRed},
      {s: 'health-status-unknown', e: App.healthIconClassYellow},
      {s: 'health-status-DEAD-ORANGE', e: App.healthIconClassOrange},
      {s: 'other', e: ''}
    ]);

    tests.forEach(function(test) {
      it(test.s, function() {
        hostComponentView.reopen({statusClass: test.s});
        expect(hostComponentView.get('statusIconClass')).to.equal(test.e);
      })
    });
  });

  describe('#slaveCustomCommands', function() {

    var content = [
      {
        componentName: 'SLAVE_COMPONENT',
        hostName: '01'
      },
      {
        componentName: 'NOT_SLAVE_COMPONENT',
        hostName: '02'
      }
    ];
    before(function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          componentName: 'SLAVE_COMPONENT',
          isSlave: true,
          customCommands: ['SLAVE_CUSTOM_COMMAND']
        });
      });
      sinon.stub(App.HostComponentActionMap, 'getMap', function () {
        return {
          SLAVE_CUSTOM_COMMAND: {
            customCommand: 'SLAVE_CUSTOM_COMMAND',
            cssClass: 'icon-play-circle',
            label: 'Custom Command',
            context: 'Custom Command',
            isHidden: false,
            disabled: false
          }
        }
      });
    });

    it('Should get custom commands for slaves', function() {
      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(1);
    });

    after(function() {
      App.StackServiceComponent.find.restore();
      App.HostComponentActionMap.getMap.restore();
    });
  });

  describe('#masterCustomCommands', function() {
    var content = [
      {
        componentName: 'MASTER_COMPONENT',
        hostName: '01'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.HostComponentActionMap, 'getMap', function () {
        return {
          REFRESHQUEUES: {
            action: 'refreshYarnQueues',
            customCommand: 'REFRESHQUEUES',
            context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
            cssClass: 'icon-refresh',
            disabled: false
          }
        }
      });
    });


    //two components, one running, active one is stopped
    it('Should not get custom commands for master component if component not running', function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          componentName: 'MASTER_COMPONENT',
          isSlave: false,
          isMaster: true,
          isStart: false,
          customCommands: ['DECOMMISSION', 'REFRESHQUEUES']
        });
      });

      sinon.stub(App.HostComponent, 'getCount').returns(2);

      sinon.stub(hostComponentView, 'runningComponentCounter', function () {
        return 1;
      });

      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(0);
    });

    //two components, none running
    it('Should get custom commands for master component when all components are stopped', function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          componentName: 'MASTER_COMPONENT',
          isSlave: false,
          isMaster: true,
          isStart: false,
          customCommands: ['DECOMMISSION', 'REFRESHQUEUES']
        });
      });

      sinon.stub(App.HostComponent, 'getCount').returns(2);

      sinon.stub(hostComponentView, 'runningComponentCounter', function () {
        return 0;
      });

      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(1);
    });

    //two components, two running, only commission and decommission custom commands
    it('Should not show COMMISSION and DECOMMISSION on master', function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          componentName: 'MASTER_COMPONENT',
          isSlave: false,
          isMaster: true,
          isStart: false,
          customCommands: ['DECOMMISSION', 'RECOMMISSION']
        });
      });

      sinon.stub(App.HostComponent, 'getCount').returns(2);

      sinon.stub(hostComponentView, 'runningComponentCounter', function () {
        return 2;
      });

      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(0);
    });

    //one component, one running, cardinality 1
    it('Should show custom command for cardinality 1', function() {
      sinon.stub(App.StackServiceComponent, 'find', function() {
        return Em.Object.create({
          isSlave: false,
          cardinality: '1',
          isMaster: true,
          isStart: true,
          customCommands: ['DECOMMISSION', 'REFRESHQUEUES']
        });
      });

      sinon.stub(App.HostComponent, 'getCount').returns(1);

      sinon.stub(hostComponentView, 'runningComponentCounter', function () {
        return 1;
      });

      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(1);
    });

    afterEach(function() {
      App.HostComponentActionMap.getMap.restore();
      App.StackServiceComponent.find.restore();
      App.HostComponent.getCount.restore();
      hostComponentView.runningComponentCounter.restore();
    });
  });

  describe('#getCustomCommandLabel', function() {

    beforeEach(function () {
      sinon.stub(App.HostComponentActionMap, 'getMap', function () {
        return {
          MASTER_CUSTOM_COMMAND: {
            action: 'executeCustomCommand',
            cssClass: 'icon-play-circle',
            isHidden: false,
            disabled: false
          },
          REFRESHQUEUES: {
            action: 'refreshYarnQueues',
            customCommand: 'REFRESHQUEUES',
            context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
            cssClass: 'icon-refresh',
            disabled: false
          }
        }
      });
    });
    afterEach(function() {
      App.HostComponentActionMap.getMap.restore();
    });

    var tests = Em.A([
      {
        msg: 'Component not present in `App.HostComponentActionMap.getMap()` should have a default valid label',
        command: 'CUSTOM',
        e: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('CUSTOM')
      },
      {
        msg: 'Component present in `App.HostComponentActionMap.getMap()` with no label should have a default valid label',
        command: 'MASTER_CUSTOM_COMMAND',
        e: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('MASTER_CUSTOM_COMMAND')
      },
      {
        msg: 'Component present in `App.HostComponentActionMap.getMap()` with label should have a custom valid label',
        command: 'REFRESHQUEUES',
        e: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu')
      }
    ]);

    tests.forEach(function(test) {
      it(test.msg, function() {
        expect(hostComponentView.getCustomCommandLabel(test.command)).to.equal(test.e);
      })
    });
  });
});
