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
      doBlinking: function(){}
    });
  });

  describe('#componentTextStatus', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({passiveState: 'PASSIVE'}),
        m: 'PASSIVE state',
        e: Em.I18n.t('hosts.component.passive.short.mode')
      },
      {
        content: Em.Object.create({passiveState: 'IMPLIED'}),
        m: 'IMPLIED state',
        e: Em.I18n.t('hosts.component.passive.short.mode')
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView.set('content', test.content);
        expect(hostComponentView.get('componentTextStatus')).to.equal(test.e);
      });
    });

  });

  describe('#passiveImpliedTextStatus', function() {

    var tests = Em.A([
      {
        content: {service: {passiveState: 'PASSIVE'}},
        parentView: {content: {passiveState: 'PASSIVE'}},
        m: 'service in PASSIVE, host in PASSIVE',
        e: Em.I18n.t('hosts.component.passive.implied.host.mode.tooltip')
      },
      {
        content: {service: {passiveState: 'PASSIVE', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'ACTIVE'}},
        m: 'service in PASSIVE, host in ACTIVE',
        e: Em.I18n.t('hosts.component.passive.implied.service.mode.tooltip').format('SERVICE_NAME')
      },
      {
        content: {service: {passiveState: 'ACTIVE'}},
        parentView: {content: {passiveState: 'ACTIVE'}},
        m: 'service in ACTIVE, host in ACTIVE',
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
      {passiveState: 'ACTIVE', e: true},
      {passiveState: 'PASSIVE', e: false},
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
        content: {service: {passiveState: 'PASSIVE'}},
        parentView: {content: {passiveState: 'PASSIVE'}},
        m: 'service in PASSIVE, host in PASSIVE',
        e: true
      },
      {
        content: {service: {passiveState: 'PASSIVE', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'ACTIVE'}},
        m: 'service in PASSIVE, host in ACTIVE',
        e: true
      },
      {
        content: {service: {passiveState: 'ACTIVE', serviceName:'SERVICE_NAME'}},
        parentView: {content: {passiveState: 'PASSIVE'}},
        m: 'service in ACTIVE, host in PASSIVE',
        e: true
      },
      {
        content: {service: {passiveState: 'ACTIVE'}},
        parentView: {content: {passiveState: 'ACTIVE'}},
        m: 'service in ACTIVE, host in ACTIVE',
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

});
