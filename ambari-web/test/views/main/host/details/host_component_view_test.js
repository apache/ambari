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
        e: 'disabled'
      },
      {
        parentView: {content: {healthClass: 'another-class'}},
        e: ''
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          parentView: test.parentView
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

    var tests = ['INSTALLED', 'UNKNOWN', 'INSTALL_FAILED', 'UPGRADE_FAILED', 'INIT'];
    var testE = false;
    var defaultE = true;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.get('hostComponent').set('workStatus', status);
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isDeleteComponentDisabled')).to.equal(e);
      });
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
          customCommands: ['CUSTOM']
        });
      });
    });

    it('Should get custom commands for slaves', function() {
      hostComponentView.set('content', content);
      expect(hostComponentView.get('customCommands')).to.have.length(1);
    });

    after(function() {
      App.StackServiceComponent.find.restore();
    });
  });

});
