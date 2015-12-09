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
require('views/common/quick_view_link_view');
require('models/host_component');
require('models/stack_service_component');
var modelSetup = require('test/init_model_test');

describe('App', function () {

  describe('#stackVersionURL', function () {

    App.QuickViewLinks.reopen({
      loadTags: function () {
      }
    });
    App.set('defaultStackVersion', "HDP-1.2.2");
    App.set('currentStackVersion', "HDP-1.2.2");

    var testCases = [
      {
        title: 'if currentStackVersion and defaultStackVersion are empty then stackVersionURL should contain prefix',
        currentStackVersion: '',
        defaultStackVersion: '',
        result: '/stacks/HDP/versions/'
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/versions/1.3.1"',
        currentStackVersion: 'HDP-1.3.1',
        defaultStackVersion: '',
        result: '/stacks/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/versions/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion and currentStackVersion are different then stackVersionURL should have currentStackVersion value',
        currentStackVersion: 'HDP-1.3.2',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/versions/1.3.2'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('defaultStackVersion', test.defaultStackVersion);
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('stackVersionURL')).to.equal(test.result);
        App.set('defaultStackVersion', "HDP-1.2.2");
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#falconServerURL', function () {

    var testCases = [
      {
        title: 'No services installed, url should be empty',
        service: Em.A([]),
        result: ''
      },
      {
        title: 'FALCON is not installed, url should be empty',
        service: Em.A([
          {
            serviceName: 'HDFS'
          }
        ]),
        result: ''
      },
      {
        title: 'FALCON is installed, url should be "host1"',
        service: Em.A([
          Em.Object.create({
            serviceName: 'FALCON',
            hostComponents: [
              Em.Object.create({
                componentName: 'FALCON_SERVER',
                hostName: 'host1'
              })
            ]
          })
        ]),
        result: 'host1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        sinon.stub(App.Service, 'find', function () {
          return test.service;
        });
        expect(App.get('falconServerURL')).to.equal(test.result);
        App.Service.find.restore();
      });
    });
  });

  describe('#currentStackVersionNumber', function () {

    var testCases = [
      {
        title: 'if currentStackVersion is empty then currentStackVersionNumber should be empty',
        currentStackVersion: '',
        result: ''
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDP-1.3.1',
        result: '1.3.1'
      },
      {
        title: 'if currentStackVersion is "HDPLocal-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDPLocal-1.3.1',
        result: '1.3.1'
      }
    ];
    before(function () {
      App.set('defaultStackVersion', '');
    });
    after(function () {
      App.set('defaultStackVersion', 'HDP-2.0.5');
    });
    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('currentStackVersionNumber')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#isHaEnabled when HDFS is installed:', function () {

    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return [
          {
            id: 'HDFS',
            serviceName: 'HDFS'
          }
        ];
      });
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it('if hadoop stack version higher than 2 then isHaEnabled should be true', function () {
      App.propertyDidChange('isHaEnabled');
      expect(App.get('isHaEnabled')).to.equal(true);
    });
    it('if cluster has SECONDARY_NAMENODE then isHaEnabled should be false', function () {
      App.store.load(App.HostComponent, {
        id: 'SECONDARY_NAMENODE',
        component_name: 'SECONDARY_NAMENODE'
      });
      App.propertyDidChange('isHaEnabled');
      expect(App.get('isHaEnabled')).to.equal(false);
    });
  });

  describe('#isHaEnabled when HDFS is not installed:', function () {

    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return [
          {
            id: 'ZOOKEEPER',
            serviceName: 'ZOOKEEPER'
          }
        ];
      });
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it('if hadoop stack version higher than 2 but HDFS not installed then isHaEnabled should be false', function () {
      App.set('currentStackVersion', 'HDP-2.1');
      expect(App.get('isHaEnabled')).to.equal(false);
      App.set('currentStackVersion', "HDP-1.2.2");
    });

  });


  describe('#services', function () {
    var stackServices = [
      Em.Object.create({
        serviceName: 'S1',
        isClientOnlyService: true
      }),
      Em.Object.create({
        serviceName: 'S2',
        hasClient: true
      }),
      Em.Object.create({
        serviceName: 'S3',
        hasMaster: true
      }),
      Em.Object.create({
        serviceName: 'S4',
        hasSlave: true
      }),
      Em.Object.create({
        serviceName: 'S5',
        isNoConfigTypes: true
      }),
      Em.Object.create({
        serviceName: 'S6',
        isMonitoringService: true
      }),
      Em.Object.create({
        serviceName: 'S7'
      })
    ];

    it('distribute services by categories', function () {
      sinon.stub(App.StackService, 'find', function () {
        return stackServices;
      });

      expect(App.get('services.all')).to.eql(['S1', 'S2', 'S3', 'S4', 'S5', 'S6', 'S7']);
      expect(App.get('services.clientOnly')).to.eql(['S1']);
      expect(App.get('services.hasClient')).to.eql(['S2']);
      expect(App.get('services.hasMaster')).to.eql(['S3']);
      expect(App.get('services.hasSlave')).to.eql(['S4']);
      expect(App.get('services.noConfigTypes')).to.eql(['S5']);
      expect(App.get('services.monitoring')).to.eql(['S6']);
      App.StackService.find.restore();
    });
  });


  describe('#components', function () {
    var i = 0,
      testCases = [
        {
          key: 'allComponents',
          data: [
            Em.Object.create({
              componentName: 'C1'
            })
          ],
          result: ['C1']
        },
        {
          key: 'reassignable',
          data: [
            Em.Object.create({
              componentName: 'C2',
              isReassignable: true
            })
          ],
          result: ['C2']
        },
        {
          key: 'restartable',
          data: [
            Em.Object.create({
              componentName: 'C3',
              isRestartable: true
            })
          ],
          result: ['C3']
        },
        {
          key: 'deletable',
          data: [
            Em.Object.create({
              componentName: 'C4',
              isDeletable: true
            })
          ],
          result: ['C4']
        },
        {
          key: 'rollinRestartAllowed',
          data: [
            Em.Object.create({
              componentName: 'C5',
              isRollinRestartAllowed: true
            })
          ],
          result: ['C5']
        },
        {
          key: 'decommissionAllowed',
          data: [
            Em.Object.create({
              componentName: 'C6',
              isDecommissionAllowed: true
            })
          ],
          result: ['C6']
        },
        {
          key: 'refreshConfigsAllowed',
          data: [
            Em.Object.create({
              componentName: 'C7',
              isRefreshConfigsAllowed: true
            })
          ],
          result: ['C7']
        },
        {
          key: 'addableToHost',
          data: [
            Em.Object.create({
              componentName: 'C8',
              isAddableToHost: true
            })
          ],
          result: ['C8']
        },
        {
          key: 'addableMasterInstallerWizard',
          data: [
            Em.Object.create({
              componentName: 'C9',
              isMasterAddableInstallerWizard: true,
              showAddBtnInInstall: true
            })
          ],
          result: ['C9']
        },
        {
          key: 'multipleMasters',
          data: [
            Em.Object.create({
              componentName: 'C10',
              isMasterWithMultipleInstances: true
            })
          ],
          result: ['C10']
        },
        {
          key: 'slaves',
          data: [
            Em.Object.create({
              componentName: 'C11',
              isSlave: true
            })
          ],
          result: ['C11']
        },
        {
          key: 'clients',
          data: [
            Em.Object.create({
              componentName: 'C12',
              isClient: true
            })
          ],
          result: ['C12']
        }
      ];

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return testCases[i].data;
      });
    });

    afterEach(function () {
      i++;
      App.StackServiceComponent.find.restore();
    });

    testCases.forEach(function (test) {
      it(test.key + ' should contain ' + test.result, function () {
        expect(App.get('components.' + test.key)).to.eql(test.result);
      })
    })
  });

  describe("#isAccessible()", function() {

    beforeEach(function () {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });

    it("Upgrade running, element should be blocked", function() {
      App.set('upgradeState', "IN_PROGRESS");
      App.set('isAdmin', true);
      expect(App.isAccessible('ADMIN')).to.be.false;
    });
    it("Upgrade running, upgrade element should not be blocked", function() {
      App.set('upgradeState', "IN_PROGRESS");
      App.set('isAdmin', true);
      expect(App.isAccessible('upgrade_ADMIN')).to.be.true;
    });
    it("Upgrade running, upgrade element should not be blocked", function() {
      App.set('upgradeState', "IN_PROGRESS");
      App.set('isAdmin', true);
      App.set('supports.opsDuringRollingUpgrade', true);
      expect(App.isAccessible('ADMIN')).to.be.true;
      App.set('supports.opsDuringRollingUpgrade', false);
    });
    it("ADMIN type, isAdmin true", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', true);
      expect(App.isAccessible('ADMIN')).to.be.true;
    });
    it("ADMIN type, isAdmin false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', false);
      expect(App.isAccessible('ADMIN')).to.be.false;
    });
    it("MANAGER type, isOperator false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', true);
      App.set('isOperator', false);
      expect(App.isAccessible('MANAGER')).to.be.true;
    });
    it("MANAGER type, isAdmin false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', false);
      App.set('isOperator', true);
      expect(App.isAccessible('MANAGER')).to.be.true;
    });
    it("MANAGER type, isAdmin and isOperator false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', false);
      App.set('isOperator', false);
      expect(App.isAccessible('MANAGER')).to.be.false;
    });
    it("OPERATOR type, isOperator false", function() {
      App.set('upgradeState', "INIT");
      App.set('isOperator', false);
      expect(App.isAccessible('OPERATOR')).to.be.false;
    });
    it("OPERATOR type, isOperator false", function() {
      App.set('upgradeState', "INIT");
      App.set('isOperator', true);
      expect(App.isAccessible('OPERATOR')).to.be.true;
    });
    it("ONLY_ADMIN type, isAdmin false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', false);
      expect(App.isAccessible('ONLY_ADMIN')).to.be.false;
    });
    it("ONLY_ADMIN type, isAdmin true, isOperator false", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', true);
      App.set('isOperator', false);
      expect(App.isAccessible('ONLY_ADMIN')).to.be.true;
    });
    it("ONLY_ADMIN type, isAdmin true, isOperator true", function() {
      App.set('upgradeState', "INIT");
      App.set('isAdmin', true);
      App.set('isOperator', true);
      expect(App.isAccessible('ONLY_ADMIN')).to.be.false;
    });
    it("unknown type", function() {
      App.set('upgradeState', "INIT");
      expect(App.isAccessible('')).to.be.false;
    });
    it("ONLY_ADMIN type, isAdmin true, isOperator true, isSuspended true", function() {
      App.set('upgradeState', "ABORTED");
      App.set('isAdmin', true);
      App.set('isOperator', false);
      this.mock.returns(true);
      expect(App.isAccessible('ONLY_ADMIN')).to.be.true;
    });
  });

  describe('#isHadoop20Stack', function () {

    Em.A([
      {
        currentStackVersion: 'HDP-2.2',
        e: false
      },
        {
          currentStackVersion: 'HDP-2.1',
          e: false
        },
        {
          currentStackVersion: 'HDP-2.0',
          e: true
        },
        {
          currentStackVersion: 'HDP-2.0.0',
          e: true
        },
        {
          currentStackVersion: 'HDP-2.0.6',
          e: true
        },
        {
          currentStackVersion: 'HDPLocal-2.2',
          e: false
        },
        {
          currentStackVersion: 'HDPLocal-2.1',
          e: false
        },
        {
          currentStackVersion: 'HDPLocal-2.0',
          e: true
        },
        {
          currentStackVersion: 'HDPLocal-2.0.0',
          e: true
        },
        {
          currentStackVersion: 'HDPLocal-2.0.6',
          e: true
        }
    ]).forEach(function (test) {
        it('for ' + test.currentStackVersion + ' isHadoop20Stack = ' + test.e.toString(), function () {
          App.set('currentStackVersion', test.currentStackVersion);
          expect(App.get('isHadoop20Stack')).to.equal(test.e);
        });
      });

  });

  describe('#upgradeIsRunning', function () {

    Em.A([
        {
          upgradeState: 'IN_PROGRESS',
          m: 'should be true (1)',
          e: true
        },
        {
          upgradeState: 'HOLDING',
          m: 'should be true (2)',
          e: true
        },
        {
          upgradeState: 'FAKE',
          m: 'should be false',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          App.set('upgradeState', test.upgradeState);
          expect(App.get('upgradeIsRunning')).to.equal(test.e);
        });
      });

  });

  describe('#upgradeAborted', function () {

    var cases = [
      {
        upgradeState: 'INIT',
        upgradeAborted: false
      },
      {
        upgradeState: 'INIT',
        upgradeAborted: false
      },
      {
        upgradeState: 'ABORTED',
        upgradeAborted: true
      }
    ];

    cases.forEach(function (item) {
      it(item.upgradeState + ", ", function () {
        App.set('upgradeState', item.upgradeState);
        App.propertyDidChange('upgradeAborted');
        expect(App.get('upgradeAborted')).to.equal(item.upgradeAborted);
      });
    });
  });

  describe('#upgradeIsNotFinished', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });

    var cases = [
      {
        upgradeState: 'INIT',
        isSuspended: false,
        upgradeIsNotFinished: false
      },
      {
        upgradeState: 'IN_PROGRESS',
        isSuspended: false,
        upgradeIsNotFinished: true
      },
      {
        upgradeState: 'HOLDING',
        isSuspended: false,
        upgradeIsNotFinished: true
      },
      {
        upgradeState: 'HOLDING_TIMEDOUT',
        isSuspended: false,
        upgradeIsNotFinished: true
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: false,
        upgradeIsNotFinished: true
      },
      {
        upgradeState: 'ABORTED',
        isSuspended: true,
        upgradeIsNotFinished: true
      }
    ];

    cases.forEach(function (item) {
      it(item.upgradeState, function () {
        App.set('upgradeState', item.upgradeState);
        this.mock.returns(item.isSuspended);
        App.propertyDidChange('upgradeIsNotFinished');
        expect(App.get('upgradeIsNotFinished')).to.equal(item.upgradeIsNotFinished);
      });
    });
  });
});
