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
    hostComponentView = App.HostComponentView.create({
      startBlinking: function(){},
      doBlinking: function(){},
      getDesiredAdminState: function(){return $.ajax({});}
    });
  });

  describe('#componentStatusTooltip', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({componentTextStatus: 'status', passiveState: 'ON'}),
        m: 'ON state',
        e: Em.I18n.t('hosts.component.passive.short.mode')
      },
      {
        content: Em.Object.create({componentTextStatus: 'status', passiveState: 'IMPLIED'}),
        m: 'IMPLIED state',
        e: Em.I18n.t('hosts.component.passive.short.mode')
      },
      {
        content: Em.Object.create({componentTextStatus: 'status', passiveState: 'OFF'}),
        m: 'OFF state',
        e: 'status'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView.set('content', test.content);
        expect(hostComponentView.get('componentStatusTooltip')).to.equal(test.e);
      });
    });

  });

  describe('#passiveImpliedTextStatus', function() {

    var tests = Em.A([
      {
        content: {service: {passiveState: 'ON'}},
        parentView: {content: {passiveState: 'ON'}},
        m: 'service in ON, host in ON',
        e: Em.I18n.t('hosts.component.passive.implied.host.mode.tooltip')
      },
      {
        content: {service: {passiveState: 'ON', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'OFF'}},
        m: 'service in ON, host in OFF',
        e: Em.I18n.t('hosts.component.passive.implied.service.mode.tooltip').format('SERVICE_NAME')
      },
      {
        content: {service: {passiveState: 'OFF'}},
        parentView: {content: {passiveState: 'OFF'}},
        m: 'service in OFF, host in OFF',
        e: ''
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          parentView: test.parentView,
          content: test.content
        });
        expect(hostComponentView.get('passiveImpliedTextStatus')).to.equal(test.e);
      });
    });

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

    var tests = Em.A([
      {workStatus: 'UPGRADE_FAILED', e: true},
      {workStatus: 'OTHER_STATUS', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isUpgradeFailed')).to.equal(test.e);
      });
    });

  });

  describe('#isInstallFailed', function() {

    var tests = Em.A([
      {workStatus: 'INSTALL_FAILED', e: true},
      {workStatus: 'OTHER_STATUS', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isInstallFailed')).to.equal(test.e);
      });
    });

  });

  describe('#isStart', function() {

    var tests = Em.A([
      {workStatus: 'STARTED', e: true},
      {workStatus: 'STARTING', e: true},
      {workStatus: 'OTHER_STATUS', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isStart')).to.equal(test.e);
      });
    });

  });

  describe('#isStop', function() {

    var tests = Em.A([
      {workStatus: 'INSTALLED', e: true},
      {workStatus: 'OTHER_STATUS', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isStop')).to.equal(test.e);
      });
    });

  });

  describe('#isInstalling', function() {

    var tests = Em.A([
      {workStatus: 'INSTALLING', e: true},
      {workStatus: 'OTHER_STATUS', e: false}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isInstalling')).to.equal(test.e);
      });
    });

  });

  describe('#noActionAvailable', function() {

    var tests = Em.A([
      {workStatus: 'STARTING', e: 'hidden'},
      {workStatus: 'STOPPING', e: 'hidden'},
      {workStatus: 'UNKNOWN', e: 'hidden'},
      {workStatus: 'OTHER_STATUS', e: ''}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('noActionAvailable')).to.equal(test.e);
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
        hostComponentView.set('content', {passiveState: test.passiveState});
        expect(hostComponentView.get('isActive')).to.equal(test.e);
      });
    });

  });

  describe('#isImplied', function() {

    var tests = Em.A([
      {
        content: {service: {passiveState: 'ON'}},
        parentView: {content: {passiveState: 'ON'}},
        m: 'service in ON, host in ON',
        e: true
      },
      {
        content: {service: {passiveState: 'ON', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'OFF'}},
        m: 'service in ON, host in OFF',
        e: true
      },
      {
        content: {service: {passiveState: 'OFF', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'ON'}},
        m: 'service in OFF, host in ON',
        e: true
      },
      {
        content: {service: {passiveState: 'OFF'}},
        parentView: {content: {passiveState: 'OFF'}},
        m: 'service in OFF, host in OFF',
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          parentView: test.parentView,
          content: test.content
        });
        expect(hostComponentView.get('isImplied')).to.equal(test.e);
      });
    });

  });

  describe('#isRestartComponentDisabled', function() {

    var tests = Em.A([
      {workStatus: 'STARTED', e: false},
      {workStatus: 'OTHER_STATUS', e: true}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isRestartComponentDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#isDeleteComponentDisabled', function() {

    var tests = Em.A([
      {workStatus: 'INSTALLED', e: false},
      {workStatus: 'UNKNOWN', e: false},
      {workStatus: 'INSTALL_FAILED', e: false},
      {workStatus: 'UPGRADE_FAILED', e: false},
      {workStatus: 'OTHER_STATUS', e: true}
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', {workStatus: test.workStatus});
        expect(hostComponentView.get('isDeleteComponentDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#componentTextStatus', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: null,
        e: 'status',
        m: 'get content status'
      },
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
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
          content: test.content,
          hostComponent: test.hostComponent
        });
        expect(hostComponentView.get('componentTextStatus')).to.equal(test.e);
      });
    });

  });

  describe('#workStatus', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({workStatus: 'status'}),
        hostComponent: null,
        e: 'status',
        m: 'get content workStatus'
      },
      {
        content: Em.Object.create({workStatus: 'status'}),
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
          content: test.content,
          hostComponent: test.hostComponent
        });
        expect(hostComponentView.get('workStatus')).to.equal(test.e);
      });
    });

  });

  describe('#statusClass', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({workStatus: App.HostComponentStatus.install_failed,passiveState: 'OFF'}),
        e: 'health-status-color-red icon-cog'
      },
      {
        content: Em.Object.create({workStatus: App.HostComponentStatus.installing, passiveState: 'OFF'}),
        e: 'health-status-color-blue icon-cog'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'ON'}),
        e: 'icon-medkit'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'IMPLIED'}),
        e: 'icon-medkit'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'OFF'}),
        e: 'health-status-started'
      }
    ]);

    tests.forEach(function(test) {
      it(test.content.get('workStatus') + ' ' + test.content.get('passiveState'), function() {
        hostComponentView = App.HostComponentView.create({
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          content: test.content
        });
        expect(hostComponentView.get('statusClass')).to.equal(test.e);
      });
    });

  });

  describe('#isInProgress', function() {

    var tests = Em.A([
      {
        workStatus: App.HostComponentStatus.stopping,
        e: true
      },
      {
        workStatus: App.HostComponentStatus.starting,
        e: true
      },
      {
        workStatus: 'other_status',
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.workStatus, function() {
        hostComponentView.set('content', Em.Object.create({workStatus: test.workStatus}));
        expect(hostComponentView.get('isInProgress')).to.equal(test.e);
      });
    });

  });

});
