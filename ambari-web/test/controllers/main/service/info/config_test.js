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
require('controllers/main/service/info/configs');
var batchUtils = require('utils/batch_scheduled_requests');
var mainServiceInfoConfigsController = null;
var testHelpers = require('test/helpers');

function getController() {
  return App.MainServiceInfoConfigsController.create({
    dependentServiceNames: [],
    loadDependentConfigs: function () {
      return {done: Em.K}
    },
    loadConfigTheme: function () {
      return $.Deferred().resolve().promise();
    }
  });
}

describe("App.MainServiceInfoConfigsController", function () {

  beforeEach(function () {
    sinon.stub(App.themesMapper, 'generateAdvancedTabs').returns(Em.K);
    sinon.stub(App.router.get('mainController'), 'startPolling');
    sinon.stub(App.router.get('mainController'), 'stopPolling');
    mainServiceInfoConfigsController = getController();
  });

  App.TestAliases.testAsComputedAlias(getController(), 'serviceConfigs', 'App.config.preDefinedServiceConfigs', 'array');

  afterEach(function() {
    App.themesMapper.generateAdvancedTabs.restore();
    App.router.get('mainController').startPolling.restore();
    App.router.get('mainController').stopPolling.restore();
  });

  describe("#getHash", function () {

    var tests = [
      {
        msg: "properties only used for ui purpose should be excluded from hash",
        configs: [
          Em.Object.create({
            id: "hive.llap.daemon.task.scheduler.enable.preemption",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "ambari.copy.hive.llap.daemon.num.executors",
            isRequiredByAgent: false,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'hive.llap.daemon.task.scheduler.enable.preemption': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      },
      {
        msg: "properties should be sorted in alphabetical order",
        configs: [
          Em.Object.create({
            id: "b.b",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "b.a",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "b.c",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          }),
          Em.Object.create({
            id: "a.b",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'a.b': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.a': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.b': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'b.c': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      },{
        msg: "properties without id should be sorted with",
        configs: [
          Em.Object.create({
            isRequiredByAgent: true,
            isFinal: false,
            value: '',
            name: 'name',
            filename: 'filename'
          }),
          Em.Object.create({
            id: "a",
            isRequiredByAgent: true,
            isFinal: false,
            value: ''
          })
        ],
        result: JSON.stringify({
          'a': {
            value: '',
            overrides: [],
            isFinal: false
          },
          'name__filename': {
            value: '',
            overrides: [],
            isFinal: false
          }
        })
      }
    ];

    afterEach(function () {
      mainServiceInfoConfigsController.set('selectedService', '');
    });

    tests.forEach(function (t) {
      it(t.msg, function () {
        mainServiceInfoConfigsController.set('selectedService', {configs: t.configs});
        expect(mainServiceInfoConfigsController.getHash()).to.equal(t.result);
      });
    });
  });


  describe("#showSavePopup", function () {
    var tests = [
      {
        transitionCallback: false,
        callback: false,
        action: "onSave",
        m: "save configs without transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        transitionCallback: true,
        callback: true,
        action: "onSave",
        m: "save configs with transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        transitionCallback: false,
        callback: false,
        action: "onDiscard",
        m: "discard changes without transitionCallback/callback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          }
        ]
      },
      {
        transitionCallback: false,
        callback: true,
        action: "onDiscard",
        m: "discard changes with callback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "callback",
            called: true
          },
          {
            field: "hash",
            value: "hash"
          }
        ]
      },
      {
        transitionCallback: true,
        callback: false,
        action: "onDiscard",
        m: "discard changes with transitionCallback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "transitionCallback",
            called: true
          }
        ]
      }
    ];

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen({
        passwordConfigsAreChanged: false
      });
      sinon.stub(mainServiceInfoConfigsController, "get", function(key) {
        return key === 'isSubmitDisabled' ? false : Em.get(mainServiceInfoConfigsController, key);
      });
      sinon.stub(mainServiceInfoConfigsController, "restartServicePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      sinon.stub(mainServiceInfoConfigsController, 'trackRequest');
    });

    afterEach(function () {
      mainServiceInfoConfigsController.get.restore();
      mainServiceInfoConfigsController.restartServicePopup.restore();
      mainServiceInfoConfigsController.getHash.restore();
      mainServiceInfoConfigsController.trackRequest.restore();
    });

    tests.forEach(function (t) {
      t.results.forEach(function (r) {
        describe(t.m + " " + r.method + " " + r.field, function () {

          beforeEach(function () {
            if (t.callback) {
              t.callback = sinon.stub();
            }
            if (t.transitionCallback) {
              t.transitionCallback = sinon.stub();
            }
            mainServiceInfoConfigsController.showSavePopup(t.transitionCallback, t.callback)[t.action]();
          });


          if (r.method) {
            if (r.method === 'callback') {
              it('callback is ' + (r.called ? '' : 'not') + ' called once', function () {
                expect(t.callback.calledOnce).to.equal(r.called);
              });
            }
            else {
              if (r.method === 'transitionCallback') {
                it('transitionCallback is ' + (r.called ? '' : 'not') + ' called once', function () {
                  expect(t.transitionCallback.calledOnce).to.equal(r.called);
                });
              }
              else {
                it(r.method + ' is ' + (r.called ? '' : 'not') + ' called once', function () {
                  expect(mainServiceInfoConfigsController[r.method].calledOnce).to.equal(r.called);
                });
              }
            }
          }
          else {
            if (r.field) {
              it(r.field + ' is equal to ' + r.value, function () {
                expect(mainServiceInfoConfigsController.get(r.field)).to.equal(r.value);
              });

            }
          }
        }, this);
      });
    }, this);
  });

  describe("#hasUnsavedChanges", function () {
    var cases = [
      {
        hash: null,
        hasUnsavedChanges: false,
        title: 'configs not rendered'
      },
      {
        hash: 'hash1',
        hasUnsavedChanges: true,
        title: 'with unsaved'
      },
      {
        hash: 'hash',
        hasUnsavedChanges: false,
        title: 'without unsaved'
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
    });
    afterEach(function () {
      mainServiceInfoConfigsController.getHash.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        mainServiceInfoConfigsController.set('hash', item.hash);
        expect(mainServiceInfoConfigsController.hasUnsavedChanges()).to.equal(item.hasUnsavedChanges);
      });
    });
  });

  describe("#showComponentsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker'],
              'publicHostName2': ['JobTracker', 'TaskTracker']
            }
          }
        },
        components: "2 TaskTrackers, 1 JobTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.components'))
      },
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker']
            }
          }
        },
        components: "1 TaskTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.component'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with components", function () {
        mainServiceInfoConfigsController.showComponentsShouldBeRestarted(t.input);
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.components, t.text)).to.equal(true);
      });
    });
  });

  describe("#showHostsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker'],
              'publicHostName2': ['JobTracker', 'TaskTracker']
            }
          }
        },
        hosts: "publicHostName1, publicHostName2",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.hosts'))
      },
      {
        input: {
          context: {
            restartRequiredHostsAndComponents: {
              'publicHostName1': ['TaskTracker']
            }
          }
        },
        hosts: "publicHostName1",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.host'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with hosts", function () {
        mainServiceInfoConfigsController.showHostsShouldBeRestarted(t.input);
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.hosts, t.text)).to.equal(true);
      });
    });
  });

  describe("#rollingRestartStaleConfigSlaveComponents", function () {
    var tests = [
      {
        componentName: {
          context: "ComponentName"
        },
        displayName: "displayName",
        passiveState: "ON"
      },
      {
        componentName: {
          context: "ComponentName1"
        },
        displayName: "displayName1",
        passiveState: "OFF"
      }
    ];

    beforeEach(function () {
      mainServiceInfoConfigsController.set("content", {displayName: "", passiveState: ""});
      sinon.stub(batchUtils, "launchHostComponentRollingRestart", Em.K);
    });
    afterEach(function () {
      batchUtils.launchHostComponentRollingRestart.restore();
    });
    tests.forEach(function (t) {
      it("trigger rollingRestartStaleConfigSlaveComponents", function () {
        mainServiceInfoConfigsController.set("content.displayName", t.displayName);
        mainServiceInfoConfigsController.set("content.passiveState", t.passiveState);
        mainServiceInfoConfigsController.rollingRestartStaleConfigSlaveComponents(t.componentName);
        expect(batchUtils.launchHostComponentRollingRestart.calledWith(t.componentName.context, t.displayName, t.passiveState === "ON", true)).to.equal(true);
      });
    });
  });

  describe("#restartAllStaleConfigComponents", function () {

    beforeEach(function () {
      sinon.stub(batchUtils, "restartAllServiceHostComponents", Em.K);
    });

    afterEach(function () {
      batchUtils.restartAllServiceHostComponents.restore();
    });

    it("trigger restartAllServiceHostComponents", function () {
      mainServiceInfoConfigsController.restartAllStaleConfigComponents().onPrimary();
      expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
    });

    describe("trigger check last check point warning before triggering restartAllServiceHostComponents", function () {
      var mainConfigsControllerHdfsStarted = App.MainServiceInfoConfigsController.create({
        content: {
          serviceName: "HDFS",
          hostComponents: [{
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          }],
          restartRequiredHostsAndComponents: {
            "host1": ['NameNode'],
            "host2": ['DataNode', 'ZooKeeper']
          }
        }
      });
      var mainServiceItemController = App.MainServiceItemController.create({});

      beforeEach(function () {
        sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
          return true;
        });
        sinon.stub(App.router, 'get', function(k) {
          if ('mainServiceItemController' === k) {
            return mainServiceItemController;
          }
          return Em.get(App.router, k);
        });
        mainConfigsControllerHdfsStarted.restartAllStaleConfigComponents();
      });

      afterEach(function () {
        mainServiceItemController.checkNnLastCheckpointTime.restore();
        App.router.get.restore();
      });

      it('checkNnLastCheckpointTime is called once', function () {
        expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      });


    });
  });

  describe("#doCancel", function () {
    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
      sinon.stub(mainServiceInfoConfigsController, 'loadSelectedVersion');
      sinon.spy(mainServiceInfoConfigsController, 'clearRecommendations');
      mainServiceInfoConfigsController.set('groupsToSave', { HDFS: 'my cool group'});
      mainServiceInfoConfigsController.set('recommendations', Em.A([{name: 'prop_1'}]));
      mainServiceInfoConfigsController.doCancel();
    });
    afterEach(function () {
      Em.run.once.restore();
      mainServiceInfoConfigsController.loadSelectedVersion.restore();
      mainServiceInfoConfigsController.clearRecommendations.restore();
    });

    it("should launch recommendations cleanup", function() {
      expect(mainServiceInfoConfigsController.clearRecommendations.calledOnce).to.be.true;
    });

    it("should clear dependent configs", function() {
      expect(App.isEmptyObject(mainServiceInfoConfigsController.get('recommendations'))).to.be.true;
    });
  });

  describe("#putChangedConfigurations", function () {
      var sc = [
      Em.Object.create({
        configs: [
          Em.Object.create({
            name: '_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: '_newsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: '_maxnewsize',
            value: '1024m'
          })
        ]
      })
    ],
    scExc = [
      Em.Object.create({
        configs: [
          Em.Object.create({
            name: 'hadoop_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'yarn_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'nodemanager_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'resourcemanager_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'apptimelineserver_heapsize',
            value: '1024m'
          }),
          Em.Object.create({
            name: 'jobhistory_heapsize',
            value: '1024m'
          })
        ]
      })
    ];
    beforeEach(function () {
      sinon.stub(App.router, 'getClusterName', function() {
        return 'clName';
      });
    });
    afterEach(function () {
      App.router.getClusterName.restore();
    });
    it("ajax request to put cluster cfg", function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      var args = testHelpers.findAjaxRequest('name', 'common.across.services.configurations');
      expect(args[0]).exists;
    });
    it('values should be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
    });
    it('values should not be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', scExc);
      mainServiceInfoConfigsController.putChangedConfigurations([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
    });
  });

  describe("#isDirChanged", function() {

    describe("when service name is HDFS", function() {
      beforeEach(function() {
        mainServiceInfoConfigsController.set('content', Ember.Object.create ({ serviceName: 'HDFS' }));
      });

      describe("for hadoop 2", function() {

        var tests = [
          {
            it: "should set dirChanged to false if none of the properties exist",
            expect: false,
            config: Ember.Object.create ({})
          },
          {
            it: "should set dirChanged to true if dfs.namenode.name.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.namenode.name.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.namenode.name.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.namenode.name.dir',
              isNotDefaultValue: false
            })
          },
          {
            it: "should set dirChanged to true if dfs.namenode.checkpoint.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.namenode.checkpoint.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.namenode.checkpoint.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.namenode.checkpoint.dir',
              isNotDefaultValue: false
            })
          },
          {
            it: "should set dirChanged to true if dfs.datanode.data.dir is not default",
            expect: true,
            config: Ember.Object.create ({
              name: 'dfs.datanode.data.dir',
              isNotDefaultValue: true
            })
          },
          {
            it: "should set dirChanged to false if dfs.datanode.data.dir is default",
            expect: false,
            config: Ember.Object.create ({
              name: 'dfs.datanode.data.dir',
              isNotDefaultValue: false
            })
          }
        ];

        beforeEach(function() {
          sinon.stub(App, 'get').returns(true);
        });

        afterEach(function() {
          App.get.restore();
        });

        tests.forEach(function(test) {
          it(test.it, function() {
            mainServiceInfoConfigsController.set('stepConfigs', [Ember.Object.create ({ configs: [test.config], serviceName: 'HDFS' })]);
            expect(mainServiceInfoConfigsController.isDirChanged()).to.equal(test.expect);
          })
        });
      });
    });

  });

  describe("#formatConfigValues", function () {
    var t = {
      configs: [
        Em.Object.create({ name: "p1", value: " v1 v1 ", displayType: "" }),
        Em.Object.create({ name: "p2", value: true, displayType: "" }),
        Em.Object.create({ name: "p3", value: " d1 ", displayType: "directory" }),
        Em.Object.create({ name: "p4", value: " d1 d2 d3 ", displayType: "directories" }),
        Em.Object.create({ name: "p5", value: " v1 ", displayType: "password" }),
        Em.Object.create({ name: "p6", value: " v ", displayType: "host" }),
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1 ", displayType: "string" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1 ", displayType: "string" })
      ],
      result: [
        Em.Object.create({ name: "p1", value: " v1 v1", displayType: "" }),
        Em.Object.create({ name: "p2", value: "true", displayType: "" }),
        Em.Object.create({ name: "p3", value: "d1", displayType: "directory" }),
        Em.Object.create({ name: "p4", value: "d1,d2,d3", displayType: "directories" }),
        Em.Object.create({ name: "p5", value: " v1 ", displayType: "password" }),
        Em.Object.create({ name: "p6", value: "v", displayType: "host" }),
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1", displayType: "string" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1", displayType: "string" })
      ]
    };

    it("format config values", function () {
      mainServiceInfoConfigsController.formatConfigValues(t.configs);
      expect(t.configs).to.deep.equal(t.result);
    });

  });

  describe("#checkOverrideProperty", function () {
    var tests = [{
      overrideToAdd: {
        name: "name1",
        filename: "filename1"
      },
      componentConfig: {
        configs: [
          {
            name: "name1",
            filename: "filename2"
          },
          {
            name: "name1",
            filename: "filename1"
          }
        ]
      },
      add: true,
      m: "add property"
    },
      {
        overrideToAdd: {
          name: "name1"
        },
        componentConfig: {
          configs: [
            {
              name: "name2"
            }
          ]
        },
        add: false,
        m: "don't add property, different names"
      },
      {
        overrideToAdd: {
          name: "name1",
          filename: "filename1"
        },
        componentConfig: {
          configs: [
            {
              name: "name1",
              filename: "filename2"
            }
          ]
        },
        add: false,
        m: "don't add property, different filenames"
      },
      {
        overrideToAdd: null,
        componentConfig: {},
        add: false,
        m: "don't add property, overrideToAdd is null"
      }];

    beforeEach(function() {
      sinon.stub(App.config,"createOverride", Em.K)
    });
    afterEach(function() {
      App.config.createOverride.restore();
    });
    tests.forEach(function(t) {
      it(t.m, function() {
        mainServiceInfoConfigsController.set("overrideToAdd", t.overrideToAdd);
        mainServiceInfoConfigsController.checkOverrideProperty(t.componentConfig);
        if(t.add) {
          expect(App.config.createOverride.calledWith(t.overrideToAdd)).to.equal(true);
          expect(mainServiceInfoConfigsController.get("overrideToAdd")).to.equal(null);
        } else {
          expect(App.config.createOverride.calledOnce).to.equal(false);
        }
      });
    });
  });

  describe("#trackRequest()", function () {
    after(function(){
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
    });
    it("should set requestsInProgress", function () {
      var dfd = $.Deferred();
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      mainServiceInfoConfigsController.trackRequest(dfd);
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql(
        {
          request: dfd,
          id: 0,
          status: 'pending',
          completed: false
        }
      );
    });
    it('should update request status when it become resolved', function() {
      var request = $.Deferred();
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      mainServiceInfoConfigsController.trackRequest(request);
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql({
        request: request,
        id: 0,
        status: 'pending',
        completed: false
      });
      request.resolve();
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql({
        request: request,
        id: 0,
        status: 'resolved',
        completed: true
      });
    });
  });

  describe('#trackRequestChain', function() {
    beforeEach(function() {
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
    });
    it('should set 2 requests in to requestsInProgress list', function() {
      mainServiceInfoConfigsController.trackRequestChain($.Deferred());
      expect(mainServiceInfoConfigsController.get('requestsInProgress')).to.have.length(2);
    });
    it('should update status for both requests when tracked requests become resolved', function() {
      var request = $.Deferred(),
          requests;
      mainServiceInfoConfigsController.trackRequestChain(request);
      requests = mainServiceInfoConfigsController.get('requestsInProgress');
      assert.deepEqual(requests.mapProperty('status'), ['pending', 'pending'], 'initial statuses');
      assert.deepEqual(requests.mapProperty('completed'), [false, false], 'initial completed');
      request.reject();
      assert.deepEqual(requests.mapProperty('status'), ['rejected', 'resolved'], 'update status when rejected');
      assert.deepEqual(requests.mapProperty('completed'), [true, true], 'initial complete are false');
    });
  });

  describe('#abortRequests', function() {
    var pendingRequest, finishedRequest;

    beforeEach(function() {
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      finishedRequest = {
        abort: sinon.spy(),
        readyState: 4,
        state: sinon.spy(),
        always: sinon.spy()
      };
      pendingRequest = {
        abort: sinon.spy(),
        readyState: 0,
        state: sinon.spy(),
        always: sinon.spy()
      };
    });

    it('should clear requests when abort called', function() {
      mainServiceInfoConfigsController.trackRequest($.Deferred());
      mainServiceInfoConfigsController.abortRequests();
      expect(mainServiceInfoConfigsController.get('requestsInProgress')).to.have.length(0);
    });

    it('should abort requests which are not finished', function() {
      mainServiceInfoConfigsController.trackRequest(pendingRequest);
      mainServiceInfoConfigsController.trackRequest(finishedRequest);
      mainServiceInfoConfigsController.abortRequests();
      expect(pendingRequest.abort.calledOnce).to.be.true;
      expect(finishedRequest.abort.calledOnce).to.be.false;
    });
  });

  describe("#setCompareDefaultGroupConfig", function() {
    beforeEach(function() {
      sinon.stub(mainServiceInfoConfigsController, "getComparisonConfig").returns("compConfig");
      sinon.stub(mainServiceInfoConfigsController, "getMockComparisonConfig").returns("mockConfig");
      sinon.stub(mainServiceInfoConfigsController, "hasCompareDiffs").returns(true);
    });
    afterEach(function() {
      mainServiceInfoConfigsController.getComparisonConfig.restore();
      mainServiceInfoConfigsController.getMockComparisonConfig.restore();
      mainServiceInfoConfigsController.hasCompareDiffs.restore();
    });
    it("empty service config passed, expect that setCompareDefaultGroupConfig will not run anything", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({}).compareConfigs.length).to.equal(0);
    });
    it("empty service config and comparison passed, expect that setCompareDefaultGroupConfig will not run anything", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({},{}).compareConfigs).to.eql(["compConfig"]);
    });
    it("expect that serviceConfig.compareConfigs will be getMockComparisonConfig", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, null)).to.eql({compareConfigs: ["mockConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, {})).to.eql({compareConfigs: ["compConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig (2)", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig (3)", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true, isMock: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isMock: true, isComparison: true, hasCompareDiffs: true});
    });
    it("property was created during upgrade and have no comparison, compare with 'Undefined' value should be created", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({name: 'prop1', isUserProperty: false}, null)).to.eql({
        name: 'prop1', isUserProperty: false, compareConfigs: ["mockConfig"],
        isComparison: true, hasCompareDiffs: true
      });
    });
  });

  describe('#showSaveConfigsPopup', function () {

    var bodyView;

    describe('#bodyClass', function () {
      beforeEach(function() {
        sinon.stub(App.StackService, 'find').returns([{dependentServiceNames: []}]);
        // default implementation
        bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup().get('bodyClass').create({
          parentView: Em.View.create()
        });
      });

      afterEach(function() {
        App.StackService.find.restore();
      });

      describe('#componentsFilterSuccessCallback', function () {
        it('check components with unknown state', function () {
          bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup('', true, '', {}, '', 'unknown', '').get('bodyClass').create({
            didInsertElement: Em.K,
            parentView: Em.View.create()
          });
          bodyView.componentsFilterSuccessCallback({
            items: [
              {
                ServiceComponentInfo: {
                  total_count: 4,
                  started_count: 2,
                  installed_count: 1,
                  component_name: 'c1'
                },
                host_components: [
                  {HostRoles: {host_name: 'h1'}}
                ]
              }
            ]
          });
          var unknownHosts = bodyView.get('unknownHosts');
          expect(unknownHosts.length).to.equal(1);
          expect(unknownHosts[0]).to.eql({name: 'h1', components: 'C1'});
        });
      });
    });
  });

  describe('#errorsCount', function () {

    it('should ignore configs with isInDefaultTheme=false', function () {

      mainServiceInfoConfigsController.reopen({selectedService: Em.Object.create({
        configsWithErrors: Em.A([
          Em.Object.create({isInDefaultTheme: true}),
          Em.Object.create({isInDefaultTheme: null})
        ])
      })});

      expect(mainServiceInfoConfigsController.get('errorsCount')).to.equal(1);

    });

  });

  describe('#_onLoadComplete', function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'next', Em.K);
      mainServiceInfoConfigsController.setProperties({
        dataIsLoaded: false,
        versionLoaded: false,
        isInit: true
      });
    });

    afterEach(function () {
      Em.run.next.restore();
    });

    it('should update flags', function () {

      mainServiceInfoConfigsController._onLoadComplete();
      expect(mainServiceInfoConfigsController.get('dataIsLoaded')).to.be.true;
      expect(mainServiceInfoConfigsController.get('versionLoaded')).to.be.true;
      expect(mainServiceInfoConfigsController.get('isInit')).to.be.false;

    });

  });

  describe('#hasCompareDiffs', function () {

    it('should return false for `password`-configs', function () {

      var hasCompareDiffs = mainServiceInfoConfigsController.hasCompareDiffs({displayType: 'password'}, {});
      expect(hasCompareDiffs).to.be.false;

    });

  });

  describe('#getServicesDependencies', function() {
    var createService = function(serviceName, dependencies) {
      return Em.Object.create({
        serviceName: serviceName,
        dependentServiceNames: dependencies || []
      });
    };
    var stackServices = [
      createService('STORM', ['RANGER', 'ATLAS', 'ZOOKEEPER']),
      createService('RANGER', ['HIVE', 'HDFS']),
      createService('HIVE', ['YARN']),
      createService('ZOOKEEPER', ['HDFS']),
      createService('ATLAS'),
      createService('HDFS', ['ZOOKEEPER']),
      createService('YARN', ['HIVE'])
    ];
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function(serviceName) {
        return stackServices.findProperty('serviceName', serviceName);
      });
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should returns all service dependencies STORM service', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('STORM');
      expect(result).to.be.eql(['RANGER', 'ATLAS', 'ZOOKEEPER', 'HIVE', 'HDFS', 'YARN']);
    });

    it('should returns all service dependencies for ATLAS', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('ATLAS');
      expect(result).to.be.eql([]);
    });

    it('should returns all service dependencies for RANGER', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('RANGER');
      expect(result).to.be.eql(['HIVE', 'HDFS', 'YARN', 'ZOOKEEPER']);
    });

    it('should returns all service dependencies for YARN', function() {
      var result = mainServiceInfoConfigsController.getServicesDependencies('YARN');
      expect(result).to.be.eql(['HIVE']);
    });
  });

  describe('#activeServiceTabs', function () {

    beforeEach(function () {
      sinon.stub(App.Tab, 'find').returns([
        {
          serviceName: 's1',
          isCategorized: true
        },
        {
          serviceName: 's1',
        }
      ]);
    });

    afterEach(function () {
      App.Tab.find.restore();
    });

    it('should return active service tabs', function () {
      mainServiceInfoConfigsController.set('selectedService', {
        serviceName: 's1',
      });
      mainServiceInfoConfigsController.propertyDidChange('activeServiceTabs');
      expect(mainServiceInfoConfigsController.get('activeServiceTabs')).to.eql([{
        serviceName: 's1',
      }]);
    });

    it('should return empty array', function () {
      mainServiceInfoConfigsController.set('selectedService', {
        serviceName: null,
      });
      mainServiceInfoConfigsController.propertyDidChange('activeServiceTabs');
      expect(mainServiceInfoConfigsController.get('activeServiceTabs')).to.eql([]);
    });
  });

  describe('#configGroups()', function () {

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen(Em.Object.create({
        groupsStore: [
          {
            serviceName: 's1'
          }
        ]
      }))
    });

    it('should return config groups', function () {
      mainServiceInfoConfigsController.set('content', {
        serviceName: 's1'
      });
      mainServiceInfoConfigsController.propertyDidChange('configGroups');
      expect(mainServiceInfoConfigsController.get('configGroups')).to.eql([
        {
          serviceName: 's1'
        }
      ]);
    });
  });

  describe('#defaultGroup()', function () {

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen(Em.Object.create({
        groupsStore: [
          {
            serviceName: 's1',
            isDefault: true
          }
        ]
      }))
    });

    it('should return default group', function () {
      mainServiceInfoConfigsController.set('content', {
        serviceName: 's1'
      });
      mainServiceInfoConfigsController.propertyDidChange('defaultGroup');
      expect(mainServiceInfoConfigsController.get('defaultGroup')).to.eql(
        {
          serviceName: 's1',
          isDefault: true
        }
      );
    });
  });

  describe('#isNonDefaultGroupSelectedInCompare()', function () {

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen(Em.Object.create({
        groupsStore: [
          Em.Object.create({
            serviceName: 's1',
            isDefault: true
          })
        ]
      }))
    });

    it('should return true', function () {
      mainServiceInfoConfigsController.set('isCompareMode', true);
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({
        serviceName: 's1',
        isDefault: false
      }));
      mainServiceInfoConfigsController.propertyDidChange('isNonDefaultGroupSelectedInCompare');
      expect(mainServiceInfoConfigsController.get('isNonDefaultGroupSelectedInCompare')).to.be.true;
    });

    it('should return false', function () {
      mainServiceInfoConfigsController.set('isCompareMode', false);
      mainServiceInfoConfigsController.set('selectedConfigGroup', null);
      mainServiceInfoConfigsController.propertyDidChange('isNonDefaultGroupSelectedInCompare');
      expect(mainServiceInfoConfigsController.get('isNonDefaultGroupSelectedInCompare')).to.be.false;
    });
  });

  describe('#dependentConfigGroups()', function () {

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen(Em.Object.create({
        groupsStore: [
          Em.Object.create({
            serviceName: 's1',
            isDefault: true
          })
        ]
      }))
    });

    it('should return dependent config groups', function () {
      mainServiceInfoConfigsController.set('dependentServiceNames', ['s1', 's2']);
      mainServiceInfoConfigsController.propertyDidChange('dependentConfigGroups');
      expect(mainServiceInfoConfigsController.get('dependentConfigGroups')).to.eql([
        Em.Object.create({
          serviceName: 's1',
          isDefault: true
        })
      ]);
    });

    it('should return empty array', function () {
      mainServiceInfoConfigsController.set('dependentServiceNames', []);
      mainServiceInfoConfigsController.propertyDidChange('dependentConfigGroups');
      expect(mainServiceInfoConfigsController.get('dependentConfigGroups')).to.eql([]);
    });
  });

  describe('#selectedVersionRecord()', function () {

    beforeEach(function () {
      sinon.stub(App.ServiceConfigVersion, 'find').withArgs('s1_1.0').returns({
        id: 's1_1.0'
      });
    });

    afterEach(function () {
      App.ServiceConfigVersion.find.restore();
    });

    it('should return false', function () {
      mainServiceInfoConfigsController.set('content', {
        serviceName: 's1'
      });
      mainServiceInfoConfigsController.set('selectedVersion', '1.0');
      mainServiceInfoConfigsController.propertyDidChange('selectedVersionRecord');
      expect(mainServiceInfoConfigsController.get('selectedVersionRecord')).to.eql({id: 's1_1.0'});
    });
  });

  describe('#isCurrentSelected()', function () {

    beforeEach(function () {
      sinon.stub(App.ServiceConfigVersion, 'find').returns(Em.Object.create({
        id: 's1_1.0',
        isCurrent: true
      }));
    });

    afterEach(function () {
      App.ServiceConfigVersion.find.restore();
    });

    it('should return true', function () {
      mainServiceInfoConfigsController.set('content', {
        serviceName: 's1'
      });
      mainServiceInfoConfigsController.set('selectedVersion', '1.0');
      mainServiceInfoConfigsController.propertyDidChange('isCurrentSelected');
      expect(mainServiceInfoConfigsController.get('isCurrentSelected')).to.be.true;
    });
  });

  describe('#canEdit()', function () {

    beforeEach(function () {
      sinon.stub(App, 'isAuthorized').returns(true);
    });

    afterEach(function () {
      App.isAuthorized.restore();
    });

    it('should return true', function () {
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({
        serviceName: 's1',
        isDefault: false
      }));
      mainServiceInfoConfigsController.set('selectedVersion', '1.0');
      mainServiceInfoConfigsController.set('currentDefaultVersion', '1.0');
      mainServiceInfoConfigsController.set('isCompareMode', false);
      mainServiceInfoConfigsController.propertyDidChange('canEdit');
      expect(mainServiceInfoConfigsController.get('canEdit')).to.be.true;
    });
  });

  describe('#isSubmitDisabled()', function () {

    beforeEach(function () {
      sinon.stub(App, 'isAuthorized').returns(true);
    });

    afterEach(function () {
      App.isAuthorized.restore();
    });

    it('should return true{1}', function () {
      mainServiceInfoConfigsController.set('selectedService', null);
      mainServiceInfoConfigsController.propertyDidChange('isSubmitDisabled');
      expect(mainServiceInfoConfigsController.get('isSubmitDisabled')).to.be.true;
    });

    it('should return true{2}', function () {
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({
        serviceName: 's1',
        errorCount: 1
      }));
      mainServiceInfoConfigsController.set('saveInProgress', false);
      mainServiceInfoConfigsController.set('recommendationsInProgress', false);
      mainServiceInfoConfigsController.propertyDidChange('isSubmitDisabled');
      expect(mainServiceInfoConfigsController.get('isSubmitDisabled')).to.be.true;
    });

    it('should return true{3}', function () {
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({
        serviceName: 's1',
        errorCount: 0
      }));
      mainServiceInfoConfigsController.set('saveInProgress', true);
      mainServiceInfoConfigsController.set('recommendationsInProgress', false);
      mainServiceInfoConfigsController.propertyDidChange('isSubmitDisabled');
      expect(mainServiceInfoConfigsController.get('isSubmitDisabled')).to.be.true;
    });

    it('should return true{4}', function () {
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({
        serviceName: 's1',
        errorCount: 0
      }));
      mainServiceInfoConfigsController.set('saveInProgress', false);
      mainServiceInfoConfigsController.set('recommendationsInProgress', true);
      mainServiceInfoConfigsController.propertyDidChange('isSubmitDisabled');
      expect(mainServiceInfoConfigsController.get('isSubmitDisabled')).to.be.true;
    });

    it('should return false', function () {
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({
        serviceName: 's1',
        errorCount: 0
      }));
      mainServiceInfoConfigsController.set('saveInProgress', false);
      mainServiceInfoConfigsController.set('recommendationsInProgress', false);
      mainServiceInfoConfigsController.propertyDidChange('isSubmitDisabled');
      expect(mainServiceInfoConfigsController.get('isSubmitDisabled')).to.be.false;
    });
  });

  describe('#filterColumns()', function () {

    it('should return filters', function () {
      mainServiceInfoConfigsController.propertyDidChange('filterColumns');
      expect(JSON.stringify(mainServiceInfoConfigsController.get('filterColumns'))).to.equal('[{"attributeName":"isOverridden","attributeValue":true,"name":"Overridden properties","selected":false,"isDisabled":false},{"attributeName":"isFinal","attributeValue":true,"name":"Final properties","selected":false,"isDisabled":false},{"attributeName":"hasIssues","attributeValue":true,"name":"Show property issues","selected":false,"isDisabled":false}]');
    });
  });

  describe('#passwordConfigsAreChanged()', function () {

    it('should return true', function () {
      mainServiceInfoConfigsController.set('stepConfigs', [
        Em.Object.create({
          serviceName: 's1',
          configs: [
            Em.Object.create({
              displayType: 'password',
              isNotDefaultValue: true
            })
          ]
        })
      ]);
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({serviceName: 's1'}));
      mainServiceInfoConfigsController.propertyDidChange('passwordConfigsAreChanged');
      expect(mainServiceInfoConfigsController.get('passwordConfigsAreChanged')).to.be.true;
    });

    it('should return false', function () {
      mainServiceInfoConfigsController.set('stepConfigs', [
        Em.Object.create({
          serviceName: 's1',
          configs: [
            Em.Object.create({
              displayType: 'password'
            })
          ]
        })
      ]);
      mainServiceInfoConfigsController.set('selectedService', Em.Object.create({serviceName: 's1'}));
      mainServiceInfoConfigsController.propertyDidChange('passwordConfigsAreChanged');
      expect(mainServiceInfoConfigsController.get('passwordConfigsAreChanged')).to.be.false;
    });
  });

  describe('#isVersionDefault()', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App.ServiceConfigVersion, 'find');
      mainServiceInfoConfigsController.set('content', Em.Object.create({ serviceName: 'HDFS' }));
    });

    afterEach(function () {
      this.mock.restore();
    });

    it('should return true', function () {
      this.mock.returns(Em.Object.create({
          groupName: 'Default'
        })
      );
      expect(mainServiceInfoConfigsController.isVersionDefault('1.0')).to.be.true;
    });

    it('should return false', function () {
      this.mock.returns(Em.Object.create({
          groupName: 'group1'
        })
      );
      expect(mainServiceInfoConfigsController.isVersionDefault('1.0')).to.be.false;
    });
  });

  describe('#clearStep()', function () {

    beforeEach(function () {
      sinon.spy(mainServiceInfoConfigsController, 'abortRequests');
      sinon.spy(mainServiceInfoConfigsController, 'clearLoadInfo');
      sinon.spy(mainServiceInfoConfigsController, 'clearSaveInfo');
      sinon.spy(mainServiceInfoConfigsController, 'clearRecommendations');
      sinon.spy(mainServiceInfoConfigsController, 'setProperties');
      sinon.spy(mainServiceInfoConfigsController, 'clearConfigs');
    });

    afterEach(function () {
      mainServiceInfoConfigsController.abortRequests.restore();
      mainServiceInfoConfigsController.clearLoadInfo.restore();
      mainServiceInfoConfigsController.clearSaveInfo.restore();
      mainServiceInfoConfigsController.clearRecommendations.restore();
      mainServiceInfoConfigsController.setProperties.restore();
      mainServiceInfoConfigsController.clearConfigs.restore();
    });

    it('should clear steps', function () {
      var props = {
        saveInProgress: false,
        isInit: true,
        hash: null,
        dataIsLoaded: false,
        versionLoaded: false,
        filter: '',
        serviceConfigVersionNote: '',
        dependentServiceNames: [],
        configGroupsAreLoaded: false
      };

      mainServiceInfoConfigsController.clearStep();
      expect(mainServiceInfoConfigsController.abortRequests.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.clearLoadInfo.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.clearSaveInfo.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.clearRecommendations.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.setProperties.calledWith(props)).to.be.true;
      expect(mainServiceInfoConfigsController.clearConfigs.calledOnce).to.be.true;
    });
  });

  describe('#saveConfigs()', function () {

    beforeEach(function () {
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        Em.Object.create({
          version: 1
        })
      ]);
    });

    afterEach(function () {
      App.ServiceConfigVersion.find.restore();
    });

    it('should return default version{1}', function () {
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({name: 'Default'}));
      mainServiceInfoConfigsController.saveConfigs();
      expect(mainServiceInfoConfigsController.get('currentDefaultVersion')).to.equal(2);
    });

    it('should return default version{2}', function () {
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({name: 'group1'}));
      mainServiceInfoConfigsController.set('currentDefaultVersion', 1);
      mainServiceInfoConfigsController.saveConfigs();
      expect(mainServiceInfoConfigsController.get('currentDefaultVersion')).to.equal(1);
    });
  });

  describe('#loadStep()', function () {

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, 'clearStep');
      sinon.stub(mainServiceInfoConfigsController, 'trackRequestChain');
      sinon.stub(mainServiceInfoConfigsController, 'getServicesDependencies').returns(['s2','s3']);
    });

    afterEach(function () {
      mainServiceInfoConfigsController.clearStep.restore();
      mainServiceInfoConfigsController.trackRequestChain.restore();
      mainServiceInfoConfigsController.getServicesDependencies.restore();
    });

    it('should load step without preSelectedConfigVersion', function () {
      mainServiceInfoConfigsController.set('content', Em.Object.create({serviceName: 's1'}));
      mainServiceInfoConfigsController.set('preSelectedConfigVersion', null);
      mainServiceInfoConfigsController.loadStep();
      expect(mainServiceInfoConfigsController.get('dependentServiceNames')).to.eql(['s2','s3']);
      expect(mainServiceInfoConfigsController.clearStep.calledOnce).to.be.true;
    });

    it('should load step with preSelectedConfigVersion', function () {
      mainServiceInfoConfigsController.set('content', Em.Object.create({serviceName: 's1'}));
      mainServiceInfoConfigsController.set('preSelectedConfigVersion', 1);
      mainServiceInfoConfigsController.loadStep();
      expect(mainServiceInfoConfigsController.get('dependentServiceNames')).to.eql(['s2','s3']);
      expect(mainServiceInfoConfigsController.clearStep.calledOnce).to.be.true;
    });
  });

  describe('#parseConfigData()', function () {

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, 'loadKerberosIdentitiesConfigs').returns(
        {
          done: function (callback) {
            return callback([])
          }
        }
      );
      sinon.stub(mainServiceInfoConfigsController, 'loadCompareVersionConfigs').returns(
        {
          done: Em.clb
        }
      );
      sinon.stub(mainServiceInfoConfigsController, 'prepareConfigObjects');
      sinon.stub(mainServiceInfoConfigsController, 'addOverrides');
      sinon.stub(mainServiceInfoConfigsController, 'onLoadOverrides');
      sinon.stub(mainServiceInfoConfigsController, 'updateAttributesFromTheme');
    });

    afterEach(function () {
      mainServiceInfoConfigsController.loadKerberosIdentitiesConfigs.restore();
      mainServiceInfoConfigsController.prepareConfigObjects.restore();
      mainServiceInfoConfigsController.addOverrides.restore();
      mainServiceInfoConfigsController.onLoadOverrides.restore();
      mainServiceInfoConfigsController.updateAttributesFromTheme.restore();
    });

    it('should parse config data', function () {
      mainServiceInfoConfigsController.set('allConfigs', []);
      mainServiceInfoConfigsController.set('content', {
        serviceName: 's1'
      });
      mainServiceInfoConfigsController.parseConfigData({});
      expect(mainServiceInfoConfigsController.loadKerberosIdentitiesConfigs.calledOnce).to.be.true;
    });
  });

  describe('#prepareConfigObjects()', function () {

    beforeEach(function () {
      sinon.stub(App.config, 'getConfigsFromJSON');
      sinon.stub(App.config, 'sortConfigs');
      sinon.stub(App.config, 'addYarnCapacityScheduler').returns([{}]);
      sinon.stub(mainServiceInfoConfigsController, 'mergeWithStackProperties').returns([
        {
          fileName: 'capacity-scheduler.xml'
        }
      ]);
      sinon.stub(mainServiceInfoConfigsController, 'setPropertyIsVisible');
      sinon.stub(mainServiceInfoConfigsController, 'setPropertyIsEditable');
    });

    afterEach(function () {
      App.config.getConfigsFromJSON.restore();
      App.config.sortConfigs.restore();
      App.config.addYarnCapacityScheduler.restore();
      mainServiceInfoConfigsController.mergeWithStackProperties.restore();
      mainServiceInfoConfigsController.setPropertyIsVisible.restore();
      mainServiceInfoConfigsController.setPropertyIsEditable.restore();
    });

    it('should prepare config objects', function () {
      var data = {
        items: [
          {
            group_name: 'Default',
            configurations: [{}]
          }
        ]
      };

      mainServiceInfoConfigsController.prepareConfigObjects(data, []);
      expect(App.config.getConfigsFromJSON.called).to.be.true;
      expect(App.config.sortConfigs.calledOnce).to.be.true;
      expect(App.config.addYarnCapacityScheduler.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.mergeWithStackProperties.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.setPropertyIsVisible.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.setPropertyIsEditable.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.get('allConfigs')).to.eql([{}]);
    });
  });

  describe('#setPropertyIsVisible()', function () {
    var testCases = [
      {
        value: 'none',
        props: ['kdc_type', 'kdc_hosts', 'admin_server_host', 'domains'],
        test: 'should set isVisible properties for kdc_type value is none',
        result: '[{"name":"kdc_type","value":"none"},{"name":"kdc_hosts","isVisible":false},{"name":"admin_server_host","isVisible":false},{"name":"domains","isVisible":false}]'
      },
      {
        value: 'active-directory',
        props: ['kdc_type', 'container_dn', 'ldap_url'],
        test: 'should set isVisible properties for kdc_type value is active-directory',
        result: '[{"name":"kdc_type","value":"active-directory"},{"name":"container_dn","isVisible":true},{"name":"ldap_url","isVisible":true}]'
      },
      {
        value: 'ipa',
        props: ['kdc_type', 'group', 'manage_krb5_conf', 'install_packages', 'admin_server_host', 'domains'],
        test: 'should set isVisible and value properties for kdc_type value is ipa',
        result: '[{"name":"kdc_type","value":"ipa"},{"name":"group","isVisible":true},{"name":"manage_krb5_conf","value":false},{"name":"install_packages","value":false},{"name":"admin_server_host","isVisible":false},{"name":"domains","isVisible":false}]'
      }
    ];

    testCases.forEach(function (c) {
      it(c.test, function () {
        var configs = c.props.map(function(prop){
          return Em.Object.create({name: prop})
        });
        mainServiceInfoConfigsController.set('content', Em.Object.create({serviceName: 'KERBEROS'}));
        configs.findProperty('name', 'kdc_type').set('value', c.value);
        mainServiceInfoConfigsController.setPropertyIsVisible(configs);
        expect(JSON.stringify(configs)).to.equal(c.result);
      });
    })
  });

  describe('#setPropertyIsEditable()', function () {
    var configs = [
      Em.Object.create({id: 'conf1', isEditable: true}),
      Em.Object.create({id: 'conf2', isEditable: true}),
    ];
    var identitiesMap = {'conf1': {}};

    beforeEach(function () {
      this.mock = sinon.stub(App, 'get');
      sinon.stub(App.config, 'kerberosIdentitiesDescription').returns('some text');
      sinon.stub(App, 'isAuthorized').returns(true);
    });

    afterEach(function () {
      this.mock.restore();
      App.config.kerberosIdentitiesDescription.restore();
      App.isAuthorized.restore();
    });

    it('should set property is editable with disabled kerberos', function () {
      this.mock.returns(false);
      mainServiceInfoConfigsController.set('isCompareMode', true);
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({
        serviceName: 's1',
        isDefault: false
      }));
      mainServiceInfoConfigsController.setPropertyIsEditable(configs, identitiesMap);
      expect(JSON.stringify(configs)).to.equal('[{"id":"conf1","isEditable":false},{"id":"conf2","isEditable":false}]');
    });

    it('should set property is editable with enabled kerberos', function () {
      this.mock.returns(true);
      mainServiceInfoConfigsController.set('isCompareMode', false);
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({
        serviceName: 's1',
        isDefault: true
      }));
      mainServiceInfoConfigsController.setPropertyIsEditable(configs, identitiesMap);
      expect(JSON.stringify(configs)).to.equal('[{"id":"conf1","isEditable":false,"isConfigIdentity":true,"isSecureConfig":true,"description":"some text"},{"id":"conf2","isEditable":false}]');
    });
  });

  describe('#loadKerberosIdentitiesConfigs()', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').returns(true);
      sinon.stub(App.config, 'parseDescriptor');
      sinon.spy(mainServiceInfoConfigsController, 'loadClusterDescriptorConfigs');
    });

    afterEach(function () {
      App.get.restore();
      App.config.parseDescriptor.restore();
      mainServiceInfoConfigsController.loadClusterDescriptorConfigs.restore();
    });

    it('should load kerberos identities configs', function () {
      mainServiceInfoConfigsController.loadKerberosIdentitiesConfigs();
      expect(mainServiceInfoConfigsController.loadClusterDescriptorConfigs.calledOnce).to.be.true;
    });
  });

  describe('#mergeWithStackProperties()', function () {

    beforeEach(function () {
      sinon.stub(App.config, 'getPropertiesFromTheme').returns(['conf1']);
      sinon.stub(App.configsCollection, 'getConfig').returns({
        name: 'conf2',
        id: 'conf2',
        isRequiredByAgent: true
      });
    });

    afterEach(function () {
      App.config.getPropertiesFromTheme.restore();
      App.configsCollection.getConfig.restore();
    });

    it('should return merged configs', function () {
      expect(JSON.stringify(mainServiceInfoConfigsController.mergeWithStackProperties([]))).to.equal('[{"name":"conf2","id":"conf2","isRequiredByAgent":true,"savedValue":null,"isNotSaved":true,"previousValue":"","initialValue":"","errorMessage":"This is required","warnMessage":""}]');
    });
  });

  describe('#addOverrides()', function () {
    var data = {
      items: [
        {
          group_name: 'group1',
          service_name: 's1',
          configurations: [{
            type: 'type1.xml',
            properties: {
              'prop1': {},
              'prop2': {}
            }
          }]
        }
      ]
    };
    var allConfigs = [
      {
        name: 'prop2',
        filename: 'type1.xml'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.config, 'createOverride');
      sinon.stub(App.config, 'createDefaultConfig').returns(Em.Object.create({
        overrides: []
      }));
      sinon.stub(App.config, 'createCustomGroupConfig').returns({
        name: 'newConf'
      });
      sinon.stub(App.ServiceConfigGroup, 'find').returns([
        Em.Object.create({
          serviceName: 's1',
          name: 'group1'
        })
      ]);
    });

    afterEach(function () {
      App.config.createOverride.restore();
      App.config.createDefaultConfig.restore();
      App.config.createCustomGroupConfig.restore();
      App.ServiceConfigGroup.find.restore();
    });

    it('should push new config and add overrides', function () {
      mainServiceInfoConfigsController.set('selectedConfigGroup', Em.Object.create({
        name: 'group1',
        isDefault: false
      }));
      mainServiceInfoConfigsController.addOverrides(data, allConfigs);
      expect(JSON.stringify(allConfigs)).to.equal('[{"name":"prop2","filename":"type1.xml"},{"overrides":[{"name":"newConf"}],"previousValue":"","initialValue":"","errorMessage":"This is required","warnMessage":""}]');
      expect(App.config.createOverride.calledWith()).to.be.true;
    });
  });

  describe('#addHostNamesToConfigs()', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create(
        {
          displayName: 'c1'
        }
      ));
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    it('should add host names to configs', function () {
      var serviceConfig = Em.Object.create({
        serviceName: 's1',
        configs: [],
        configCategories: [
          {
            showHost: true,
            name: 'c1'
          }
        ]
      });

      mainServiceInfoConfigsController.addHostNamesToConfigs(serviceConfig);
      expect(JSON.stringify(serviceConfig)).to.equal('{"serviceName":"s1","configs":[{"id":"c1_host__s1-site","name":"c1_host","displayName":"c1 host","value":[],"recommendedValue":[],"description":"The host that has been assigned to run c1","displayType":"componentHost","isOverridable":false,"isRequiredByAgent":false,"serviceName":"s1","filename":"s1-site.xml","category":"c1","index":0,"previousValue":[],"initialValue":[],"errorMessage":"","warnMessage":""}],"configCategories":[{"showHost":true,"name":"c1"}]}');
    });
  });

  describe('#getComponentHostValue()', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create(
        {
          displayName: 'c1',
          isMaster: true
        }
      ));
      sinon.stub(App.MasterComponent, 'find').returns(Em.Object.create(
        {
          displayName: 'c1',
          hostNames: ['host1', 'host2']
        }
      ));
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
      App.MasterComponent.find.restore();
    });

    it('should return component host names', function () {
      expect(mainServiceInfoConfigsController.getComponentHostValue('c1')).to.eql(['host1', 'host2']);
    });
  });

  describe('#showItemsShouldBeRestarted()', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('should show modal popup', function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted('some text', 'label');
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#manageConfigurationGroup()', function () {
    var manageConfigGroupsController = {
      manageConfigurationGroups: Em.K
    };

    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(manageConfigGroupsController);
      sinon.spy(manageConfigGroupsController, 'manageConfigurationGroups');
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it('should manage configuration groups', function () {
      mainServiceInfoConfigsController.manageConfigurationGroup();
      expect(manageConfigGroupsController.manageConfigurationGroups.calledOnce).to.be.true;
    });
  });

  describe('#selectConfigGroup()', function () {

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, 'doSelectConfigGroup');
      sinon.stub(mainServiceInfoConfigsController, 'showSavePopup');
      sinon.stub(mainServiceInfoConfigsController, 'hasUnsavedChanges').returns(true);
    });

    afterEach(function () {
      mainServiceInfoConfigsController.doSelectConfigGroup.restore();
      mainServiceInfoConfigsController.showSavePopup.restore();
      mainServiceInfoConfigsController.hasUnsavedChanges.restore();
    });

    it('should manage configuration groups with isInit is false', function () {
      mainServiceInfoConfigsController.set('isInit', false)
      mainServiceInfoConfigsController.selectConfigGroup({});
      expect(mainServiceInfoConfigsController.showSavePopup.calledOnce).to.be.true;
    });

    it('should manage configuration groups with isInit is true', function () {
      mainServiceInfoConfigsController.set('isInit', true)
      mainServiceInfoConfigsController.selectConfigGroup({});
      expect(mainServiceInfoConfigsController.doSelectConfigGroup.calledOnce).to.be.true;
    });
  });

  describe('#doSelectConfigGroup()', function () {

    beforeEach(function () {
      sinon.stub(App.loadTimer, 'start');
      sinon.stub(mainServiceInfoConfigsController, 'loadCurrentVersions');
      sinon.stub(mainServiceInfoConfigsController, 'loadSelectedVersion');
      sinon.stub(App.ServiceConfigVersion, 'find').returns([
        Em.Object.create({
          groupId: 1,
          isCurrent: true,
          version: '1.0'
        })
      ]);
    });

    afterEach(function () {
      App.loadTimer.start.restore();
      mainServiceInfoConfigsController.loadCurrentVersions.restore();
      mainServiceInfoConfigsController.loadSelectedVersion.restore();
      App.ServiceConfigVersion.find.restore();
    });

    it('should switch view to selected group{1}', function () {
      var event = {
        context: Em.Object.create({
          isDefault: true,
          id: 1
        })
      };

      mainServiceInfoConfigsController.selectConfigGroup(event);
      expect(App.loadTimer.start.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.loadCurrentVersions.calledOnce).to.be.true;
    });

    it('should switch view to selected group{2}', function () {
      var event = {
        context: Em.Object.create({
          isDefault: false,
          id: 1
        })
      };

      mainServiceInfoConfigsController.selectConfigGroup(event);
      expect(App.loadTimer.start.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.loadSelectedVersion.calledWith('1.0', event.context)).to.be.true;
    });

    it('should switch view to selected group{3}', function () {
      var event = {
        context: Em.Object.create({
          isDefault: false,
          id: 2
        })
      };

      mainServiceInfoConfigsController.selectConfigGroup(event);
      expect(App.loadTimer.start.calledOnce).to.be.true;
      expect(mainServiceInfoConfigsController.loadSelectedVersion.calledWith(null, event.context)).to.be.true;
    });
  });
});
