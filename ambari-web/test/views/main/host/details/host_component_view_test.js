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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
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

    var tests = ['STARTED'];
    var testE = false;
    var defaultE = true;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.set('content', {workStatus: status});
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
        hostComponentView.set('content', {workStatus: status});
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isDeleteComponentDisabled')).to.equal(e);
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
        e: 'health-status-started'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'IMPLIED'}),
        e: 'health-status-started'
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

    var tests = ['STOPPING', 'STARTING'];
    var testE = true;
    var defaultE = false;

    App.HostComponentStatus.getStatusesList().forEach(function(status) {
      it(status, function() {
        hostComponentView.set('content', {workStatus: status});
        var e = tests.contains(status) ? testE : defaultE;
        expect(hostComponentView.get('isInProgress')).to.equal(e);
      });
    });

  });

});
