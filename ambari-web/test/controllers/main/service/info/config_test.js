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
describe("App.MainServiceInfoConfigsController", function () {

  beforeEach(function () {
    sinon.stub(App.themesMapper, 'generateAdvancedTabs').returns(Em.K);
    mainServiceInfoConfigsController = App.MainServiceInfoConfigsController.create({
      dependentServiceNames: [],
      loadDependentConfigs: function () {
        return {done: Em.K}
      },
      loadConfigTheme: function () {
        return $.Deferred().resolve().promise();
      }
    });
  });

  afterEach(function() {
    App.themesMapper.generateAdvancedTabs.restore();
  });

  describe("#showSavePopup", function () {
    var tests = [
      {
        path: false,
        callback: null,
        action: "onSave",
        m: "save configs without path/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        path: true,
        callback: true,
        action: "onSave",
        m: "save configs with path/callback",
        results: [
          {
            method: "restartServicePopup",
            called: true
          }
        ]
      },
      {
        path: false,
        callback: false,
        action: "onDiscard",
        m: "discard changes without path/callback",
        results: [
          {
            method: "restartServicePopup",
            called: false
          }
        ]
      },
      {
        path: false,
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
        path: true,
        callback: null,
        action: "onDiscard",
        m: "discard changes with path",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            field: "forceTransition",
            value: true
          }
        ]
      }
    ];

    beforeEach(function () {
      mainServiceInfoConfigsController.reopen({
        passwordConfigsAreChanged: false
      });
      sinon.stub(mainServiceInfoConfigsController, "get", function(key) {
        return key == 'isSubmitDisabled' ?  false : Em.get(mainServiceInfoConfigsController, key);
      });
      sinon.stub(mainServiceInfoConfigsController, "restartServicePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      sinon.stub(App.router, "route", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.get.restore();
      mainServiceInfoConfigsController.restartServicePopup.restore();
      mainServiceInfoConfigsController.getHash.restore();
      App.router.route.restore();
    });

    tests.forEach(function (t) {
      t.results.forEach(function (r) {
        it(t.m + " " + r.method + " " + r.field, function () {
          if (t.callback) {
            t.callback = sinon.stub();
          }
          mainServiceInfoConfigsController.showSavePopup(t.path, t.callback)[t.action]();
          if (r.method) {
            if (r.method === 'callback') {
              expect(t.callback.calledOnce).to.equal(r.called);
            } else {
              expect(mainServiceInfoConfigsController[r.method].calledOnce).to.equal(r.called);
            }
          } else if (r.field) {
            expect(mainServiceInfoConfigsController.get(r.field)).to.equal(r.value);
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
        expect(batchUtils.launchHostComponentRollingRestart.calledWith(t.componentName.context, t.displayName, t.passiveState == "ON", true)).to.equal(true);
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
    it("trigger check last check point warning before triggering restartAllServiceHostComponents", function () {
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
      expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      mainServiceItemController.checkNnLastCheckpointTime.restore();
      App.router.get.restore();
    });
  });

  describe("#doCancel", function () {
    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
    });
    afterEach(function () {
      Em.run.once.restore();
    });

    it("should clear dependent configs", function() {
      mainServiceInfoConfigsController.set('groupsToSave', { HDFS: 'my cool group'});
      mainServiceInfoConfigsController.set('_dependentConfigValues', Em.A([{name: 'prop_1'}]));
      mainServiceInfoConfigsController.doCancel();
      expect(App.isEmptyObject(mainServiceInfoConfigsController.get('_dependentConfigValues'))).to.be.true;
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
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
      App.router.getClusterName.restore();
    });
    it("ajax request to put cluster cfg", function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      expect(mainServiceInfoConfigsController.putChangedConfigurations([]));
      expect(App.ajax.send.calledOnce).to.be.true;
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

  describe("#putConfigGroupChanges", function() {

    var t = {
      data: {
        ConfigGroup: {
          id: "id"
        }
      },
      request: [{
        ConfigGroup: {
          id: "id"
        }
      }]
    };

    beforeEach(function() {
      sinon.spy($,"ajax");
    });
    afterEach(function() {
      $.ajax.restore();
    });

    it("updates configs groups", function() {
      mainServiceInfoConfigsController.putConfigGroupChanges(t.data);
      expect(JSON.parse($.ajax.args[0][0].data)).to.deep.equal(t.request);
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
      mainServiceInfoConfigsController.get('requestsInProgress').clear();
      mainServiceInfoConfigsController.trackRequest({'request': {}});
      expect(mainServiceInfoConfigsController.get('requestsInProgress')[0]).to.eql({'request': {}});
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
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function() {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function() {
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
        sinon.stub(App.ajax, 'send', Em.K);
        // default implementation
        bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup().get('bodyClass').create({
          parentView: Em.View.create()
        });
      });

      afterEach(function() {
        App.ajax.send.restore();
        App.StackService.find.restore();
      });

      describe('#componentsFilterSuccessCallback', function () {
        it('check components with unknown state', function () {
          bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup('', true, '', {}, '', 'unknown', '').get('bodyClass').create({
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

    it('should ignore configs with widgets (enhanced configs)', function () {

      mainServiceInfoConfigsController.reopen({selectedService: {
        configs: [
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: false}),
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: true}),
          Em.Object.create({isVisible: true, isValid: true}),
          Em.Object.create({isVisible: true, isValid: false})
        ]
      }});

      expect(mainServiceInfoConfigsController.get('errorsCount')).to.equal(1);

    });

    it('should ignore configs with widgets (enhanced configs) and hidden configs', function () {

      mainServiceInfoConfigsController.reopen({selectedService: {
        configs: [
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: false}),
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: true}),
          Em.Object.create({isVisible: false, isValid: false}),
          Em.Object.create({isVisible: true, isValid: true}),
          Em.Object.create({isVisible: true, isValid: false})
        ]
      }});

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

});
