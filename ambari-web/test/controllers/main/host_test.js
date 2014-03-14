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
var validator = require('utils/validator');
require('utils/component');
require('utils/batch_scheduled_requests');
require('controllers/main/host');
require('mappers/server_data_mapper');

describe('MainHostController', function () {

  var hostController;

  describe('#bulkOperation', function() {

    beforeEach(function() {
      hostController = App.MainHostController.create({
        bulkOperationForHostsRestart: function(){},
        bulkOperationForHosts: function(){},
        bulkOperationForHostComponentsRestart: function(){},
        bulkOperationForHostComponentsDecommission: function(){},
        bulkOperationForHostComponents: function(){},
        bulkOperationForHostsPassiveState: function(){}
      });
      sinon.spy(hostController, 'bulkOperationForHostsRestart');
      sinon.spy(hostController, 'bulkOperationForHosts');
      sinon.spy(hostController, 'bulkOperationForHostComponentsRestart');
      sinon.spy(hostController, 'bulkOperationForHostComponentsDecommission');
      sinon.spy(hostController, 'bulkOperationForHostComponents');
      sinon.spy(hostController, 'bulkOperationForHostsPassiveState');
    });

    afterEach(function() {
      hostController.bulkOperationForHosts.restore();
      hostController.bulkOperationForHostsRestart.restore();
      hostController.bulkOperationForHostComponentsRestart.restore();
      hostController.bulkOperationForHostComponentsDecommission.restore();
      hostController.bulkOperationForHostComponents.restore();
      hostController.bulkOperationForHostsPassiveState.restore();

    });

    it('RESTART for hosts', function() {
      var operationData = {
        action: 'RESTART'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostsRestart.calledOnce).to.equal(true);
    });

    it('START for hosts', function() {
      var operationData = {
        action: 'STARTED'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHosts.calledOnce).to.equal(true);
    });

    it('STOP for hosts', function() {
      var operationData = {
        action: 'INSTALLED'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHosts.calledOnce).to.equal(true);
    });

    it('PASSIVE_STATE for hosts', function() {
      var operationData = {
        action: 'PASSIVE_STATE'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostsPassiveState.calledOnce).to.equal(true);
    });

    it('RESTART for hostComponents', function() {
      var operationData = {
        action: 'RESTART',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsRestart.calledOnce).to.equal(true);
    });

    it('START for hostComponents', function() {
      var operationData = {
        action: 'STARTED',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponents.calledOnce).to.equal(true);
    });

    it('STOP for hostComponents', function() {
      var operationData = {
        action: 'INSTALLED',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponents.calledOnce).to.equal(true);
    });

    it('DECOMMISSION for hostComponents', function() {
      var operationData = {
        action: 'DECOMMISSION',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsDecommission.calledOnce).to.equal(true);
    });

    it('RECOMMISSION for hostComponents', function() {
      var operationData = {
        action: 'DECOMMISSION_OFF',
        componentNameFormatted: 'DataNodes'
      };
      hostController.bulkOperation(operationData, []);
      expect(hostController.bulkOperationForHostComponentsDecommission.calledOnce).to.equal(true);
    });

  });

  describe('#bulkOperationForHosts', function() {

    beforeEach(function(){
      hostController = App.MainHostController.create({});
      sinon.spy($, 'ajax');
    });

    afterEach(function() {
      $.ajax.restore();
    });

    var tests = [
      {
        operationData: {},
        hosts: [],
        m: 'no hosts',
        e: false
      },
      {
        operationData: {
          actionToCheck: 'STARTED'
        },
        hosts: [
          Em.Object.create({
            hostComponents: Em.A([
              Em.Object.create({isMaster: true, isSlave: false, host: {hostName:'host1'}, workStatus: 'STARTED', componentName: 'NAMENODE', passiveState: 'OFF'}),
              Em.Object.create({isMaster: false, isSlave: true, host: {hostName:'host1'}, workStatus: 'STARTED', componentName: 'DATANODE', passiveState: 'OFF'})
            ])
          })
        ],
        m: '1 host. components are in proper state',
        e: true
      },
      {
        operationData: {
          actionToCheck: 'INSTALLED'
        },
        hosts: [
          Em.Object.create({
            hostComponents: Em.A([
              Em.Object.create({isMaster: true, isSlave: false, host: {hostName:'host1'}, workStatus: 'STARTED', componentName: 'NAMENODE', passiveState: 'OFF'}),
              Em.Object.create({isMaster: false, isSlave: true, host: {hostName:'host1'}, workStatus: 'STARTED', componentName: 'DATANODE', passiveState: 'OFF'})
            ])
          })
        ],
        m: '1 host. components are not in proper state',
        e: false
      },
      {
        operationData: {
          actionToCheck: 'INSTALLED'
        },
        hosts: [
          Em.Object.create({
            hostComponents: Em.A([
              Em.Object.create({isMaster: true, isSlave: false, host: {hostName:'host1'}, workStatus: 'INSTALLED', componentName: 'NAMENODE', passiveState: 'OFF'}),
              Em.Object.create({isMaster: false, isSlave: true, host: {hostName:'host1'}, workStatus: 'STARTED', componentName: 'DATANODE', passiveState: 'OFF'})
            ])
          })
        ],
        m: '1 host. some components are in proper state',
        e: true
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        hostController.bulkOperationForHosts(test.operationData, test.hosts);
        expect($.ajax.called).to.equal(test.e);
      });
    });

  });

  describe('#bulkOperationForHostsRestart', function() {

    beforeEach(function(){
      hostController = App.MainHostController.create({});
      sinon.spy($, 'ajax');
    });

    afterEach(function() {
      $.ajax.restore();
    });

    var tests = Em.A([
      {
        hosts: Em.A([]),
        m: 'No hosts',
        e: false
      },
      {
        hosts: Em.A([
          Em.Object.create({
            hostComponents: Em.A([Em.Object.create({passiveState: 'OFF'}), Em.Object.create({passiveState: 'OFF'})])
          })
        ]),
        m: 'One host',
        e: true
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostController.bulkOperationForHostsRestart({}, test.hosts);
        expect($.ajax.calledOnce).to.equal(test.e)
      });
    });

  });

  describe('#bulkOperationForHostsPassiveState', function() {

    beforeEach(function(){
      hostController = App.MainHostController.create({});
      sinon.spy($, 'ajax');
    });

    afterEach(function() {
      $.ajax.restore();
    });

    var tests = [
      {
        hosts: Em.A([]),
        operationData: {},
        m: 'No hosts',
        e: false
      },
      {
        hosts: Em.A([
          Em.Object.create({
            passiveState: 'OFF'
          })
        ]),
        operationData: {
          state: 'OFF'
        },
        m: 'One host, but in state that should get',
        e: false
      },
      {
        hosts: Em.A([
          Em.Object.create({
            passiveState: 'OFF'
          })
        ]),
        operationData: {
          state: 'ON'
        },
        m: 'One host with proper state',
        e: true
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        hostController.bulkOperationForHostsPassiveState(test.operationData, test.hosts);
        expect($.ajax.calledOnce).to.equal(test.e)
      });
    });

  });

});