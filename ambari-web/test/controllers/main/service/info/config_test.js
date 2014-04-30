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
    mainServiceInfoConfigsController = App.MainServiceInfoConfigsController.create({});
  });

  describe("#showSavePopup", function () {
    var tests = [
      {
        path: false,
        event: false,
        action: "onSave",
        m: "save configs without path/event",
        results: [
          {
            method: "restartServicePopup",
            called: true
          },
          {
            method: "selectConfigGroup",
            called: false
          }
        ]
      },
      {
        path: true,
        event: true,
        action: "onSave",
        m: "save configs with path/event",
        results: [
          {
            method: "restartServicePopup",
            called: true
          },
          {
            method: "selectConfigGroup",
            called: false
          }
        ]
      },
      {
        path: false,
        event: false,
        action: "onDiscard",
        m: "discard changes without path/event",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "selectConfigGroup",
            called: false
          }
        ]
      },
      {
        path: false,
        event: true,
        action: "onDiscard",
        m: "discard changes with event",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "selectConfigGroup",
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
        event: false,
        action: "onDiscard",
        m: "discard changes with path",
        results: [
          {
            method: "restartServicePopup",
            called: false
          },
          {
            method: "selectConfigGroup",
            called: false
          },
          {
            field: "forceTransition",
            value: true
          }
        ]
      }
    ];

    var rRoute = App.router.route;
    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "restartServicePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "selectConfigGroup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      App.router.route = Em.K;
    });
    afterEach(function () {
      mainServiceInfoConfigsController.restartServicePopup.restore();
      mainServiceInfoConfigsController.selectConfigGroup.restore();
      mainServiceInfoConfigsController.getHash.restore();
      App.router.route = rRoute;
    });

    tests.forEach(function (t) {
      t.results.forEach(function (r) {
        it(t.m + " " + r.method + " " + r.field, function () {
          mainServiceInfoConfigsController.showSavePopup(t.path, t.event)[t.action]();
          if (r.method) {
            expect(mainServiceInfoConfigsController[r.method].calledOnce).to.equal(r.called);
          } else if (r.field) {
            expect(mainServiceInfoConfigsController.get(r.field)).to.equal(r.value);
          }
        }, this);
      });
    }, this);
  });

  describe("#hasUnsavedChanges", function () {
    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
    });
    afterEach(function () {
      mainServiceInfoConfigsController.getHash.restore();
    });

    it("with unsaved", function () {
      mainServiceInfoConfigsController.set("hash", "hash1");
      expect(mainServiceInfoConfigsController.hasUnsavedChanges()).to.equal(true);
    });

    it("without unsaved", function () {
      mainServiceInfoConfigsController.set("hash", "hash");
      expect(mainServiceInfoConfigsController.hasUnsavedChanges()).to.equal(false);
    });
  });

  describe("#selectConfigGroup", function () {

    var tests = [
      {
        event: {
          context: "cfgGroup"
        },
        isInit: true,
        showPopup: false,
        m: "setup new cfg group"
      },
      {
        event: {
          context: "cfgGroup"
        },
        isInit: false,
        hash: "hash",
        showPopup: false,
        m: "setup new cfg group, has some changes"
      },
      {
        event: {
          context: "cfgGroup"
        },
        isInit: false,
        hash: "hash1",
        showPopup: true,
        m: "show popup, doesn't setup new cfg group"
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showSavePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      sinon.stub(mainServiceInfoConfigsController, "onConfigGroupChange", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showSavePopup.restore();
      mainServiceInfoConfigsController.getHash.restore();
      mainServiceInfoConfigsController.onConfigGroupChange.restore();
    });
    tests.forEach(function (t) {
      it(t.m, function () {
        mainServiceInfoConfigsController.set("isInit", t.isInit);
        mainServiceInfoConfigsController.set("hash", t.hash);
        mainServiceInfoConfigsController.selectConfigGroup(t.event);
        if (!t.showPopup) {
          expect(mainServiceInfoConfigsController.get("selectedConfigGroup")).to.equal(t.event.context);
          expect(mainServiceInfoConfigsController.showSavePopup.calledOnce).to.equal(false);
        } else {
          expect(mainServiceInfoConfigsController.showSavePopup.calledWith(null, t.event)).to.equal(true);
        }
      });
    });

  });

  describe("#manageConfigurationGroup", function () {
    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "manageConfigurationGroups", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.manageConfigurationGroups.restore();
    });
    it("run manageConfigurationGroups", function () {
      mainServiceInfoConfigsController.manageConfigurationGroup();
      expect(mainServiceInfoConfigsController.manageConfigurationGroups.calledOnce).to.equal(true);
    });
  });

  describe("#addOverrideProperty", function () {
    var serviceConfigProperty = Em.Object.create({
      overrides: []
    });

    var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
    newSCP.set('value', '');
    newSCP.set('isOriginalSCP', false);
    newSCP.set('parentSCP', serviceConfigProperty);
    newSCP.set('isEditable', true);

    it("add new overridden property", function () {
      mainServiceInfoConfigsController.addOverrideProperty(serviceConfigProperty);
      expect(serviceConfigProperty.get("overrides")[0]).to.eql(newSCP);
    });
  });

  describe("#showComponentsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          'publicHostName1': ['TaskTracker'],
          'publicHostName2': ['JobTracker', 'TaskTracker']
        },
        components: "2 TaskTrackers, 1 JobTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.components'))
      },
      {
        input: {
          'publicHostName1': ['TaskTracker']
        },
        components: "1 TaskTracker",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.component'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
      mainServiceInfoConfigsController.set("content", {restartRequiredHostsAndComponents: ""});
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
      mainServiceInfoConfigsController.set("content", undefined);
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with components", function () {
        mainServiceInfoConfigsController.set("content.restartRequiredHostsAndComponents", t.input);
        mainServiceInfoConfigsController.showComponentsShouldBeRestarted();
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.components, t.text)).to.equal(true);
      });
    });
  });

  describe("#showHostsShouldBeRestarted", function () {

    var tests = [
      {
        input: {
          'publicHostName1': ['TaskTracker'],
          'publicHostName2': ['JobTracker', 'TaskTracker']
        },
        hosts: "publicHostName1, publicHostName2",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.hosts'))
      },
      {
        input: {
          'publicHostName1': ['TaskTracker']
        },
        hosts: "publicHostName1",
        text: Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(Em.I18n.t('common.host'))
      }
    ];

    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "showItemsShouldBeRestarted", Em.K);
      mainServiceInfoConfigsController.set("content", {restartRequiredHostsAndComponents: ""});
    });
    afterEach(function () {
      mainServiceInfoConfigsController.showItemsShouldBeRestarted.restore();
      mainServiceInfoConfigsController.set("content", undefined);
    });

    tests.forEach(function (t) {
      it("trigger showItemsShouldBeRestarted popup with hosts", function () {
        mainServiceInfoConfigsController.set("content.restartRequiredHostsAndComponents", t.input);
        mainServiceInfoConfigsController.showHostsShouldBeRestarted();
        expect(mainServiceInfoConfigsController.showItemsShouldBeRestarted.calledWith(t.hosts, t.text)).to.equal(true);
      });
    });
  });

  describe("rollingRestartStaleConfigSlaveComponents", function () {
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
  });

  describe("#doCancel", function () {
    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "loadStep", Em.K);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.loadStep.restore();
    });
    it("trigger loadStep", function () {
      mainServiceInfoConfigsController.doCancel();
      expect(mainServiceInfoConfigsController.loadStep.calledOnce).to.equal(true);
    });
  });

  describe("#getCurrentServiceComponents", function () {
    var t = Em.Object.create({
      content: Em.Object.create({
        hostComponents: [
          Em.Object.create({
            componentName: "componentName1",
            displayName: "displayName1"
          }),
          Em.Object.create({
            componentName: "componentName2",
            displayName: "displayName2"
          })
        ]
      }),
      validComponents: Em.A([
        Em.Object.create({
          componentName: "componentName1",
          displayName: "displayName1",
          selected: false
        }),
        Em.Object.create({
          componentName: "componentName2",
          displayName: "displayName2",
          selected: false
        })
      ])
    });

    beforeEach(function () {
      mainServiceInfoConfigsController.set("content", { hostComponents: Em.A([])});
    });

    it("get current service components", function () {
      mainServiceInfoConfigsController.get("content.hostComponents").push(t.content.hostComponents[0]);
      var com = mainServiceInfoConfigsController.get("getCurrentServiceComponents");
      expect(com[0]).to.eql(t.validComponents[0]);
    });
  });

  describe("#getMasterComponentHostValue", function () {
    var t = Em.Object.create({
      content: Em.Object.create({
        hostComponents: [
          Em.Object.create({
            componentName: "componentName2",
            displayName: "displayName2",
            host: Em.Object.create({
              hostName: "hostName"
            })
          })
        ]
      })
    });
    beforeEach(function () {
      mainServiceInfoConfigsController.set("content", { hostComponents: Em.A([])});
    });

    it("returns hostname", function () {
      mainServiceInfoConfigsController.get("content.hostComponents").push(t.content.hostComponents[0]);
      expect(mainServiceInfoConfigsController.getMasterComponentHostValue(t.content.hostComponents[0].componentName)).to.equal(t.content.hostComponents[0].host.hostName);
    });
  });

  describe("#setServerConfigValue", function () {

    it("parsing storm.zookeeper.servers property in non standart method", function () {
      expect(mainServiceInfoConfigsController.setServerConfigValue("storm.zookeeper.servers", ["a", "b"])).to.equal('[\'a\',\'b\']');
    });
    it("parsing content property in non standart method", function () {
      expect(mainServiceInfoConfigsController.setServerConfigValue("content", "value")).to.equal("value");
    });
    it("parsing default properties", function () {
      expect(mainServiceInfoConfigsController.setServerConfigValue("any.other.property", "value&lt;")).to.equal("value<");
    });
  });

  describe("#createSiteObj", function () {

    var tests = [
      {
        siteName: "hdfs-site",
        tagName: "version1",
        siteObj: Em.A([
          {
            name: "property1",
            value: "value1"
          },
          {
            name: "property2",
            value: "value2&lt;"
          }
        ]),
        result: {
          "type": "hdfs-site",
          "tag": "version1",
          "properties": {
            "property1": "value1",
            "property2": "value2<"
          }
        },
        m: "default"
      },
      {
        siteName: "falcon-startup.properties",
        tagName: "version1",
        siteObj: Em.A([
          {
            name: "property1",
            value: "value1"
          },
          {
            name: "property2",
            value: "value2&lt;"
          }
        ]),
        result: {
          "type": "falcon-startup.properties",
          "tag": "version1",
          "properties": {
            "property1": "value1",
            "property2": "value2&lt;"
          }
        },
        m: "for falcon-startup.properties"

      }
    ];
    tests.forEach(function (t) {
      it("create site object " + t.m, function () {
        expect(mainServiceInfoConfigsController.createSiteObj(t.siteName, t.tagName, t.siteObj)).to.deep.eql(t.result)
      });
    });
  });

  describe("#createCoreSiteObj", function () {

    var tests = [
      {
        tagName: "version1",
        uiConfigs: Em.A([
          Em.Object.create({
            name: "property1",
            value: "value1",
            filename: "core-site.xml"
          }),
          Em.Object.create({
            name: "property2",
            value: "value2&lt;",
            filename: "core-site.xml"
          })
        ]),
        result: {
          "type": "core-site",
          "tag": "version1",
          "properties": {
            "property1": "value1",
            "property2": "value2<"
          }
        }
      }
    ];
    tests.forEach(function (t) {
      it("create core object", function () {
        mainServiceInfoConfigsController.set("uiConfigs", t.uiConfigs);
        expect(mainServiceInfoConfigsController.createCoreSiteObj(t.tagName)).to.deep.eql(t.result);
      });
    });
  });

  describe("#createGlobalSiteObj", function () {

    var t = {
      tagName: "version1",
      globalConfigs: Em.A([
        Em.Object.create({
          name: "property1",
          value: "value1"
        }),
        Em.Object.create({
          name: "property2",
          value: "value2&lt;"
        }),
        Em.Object.create({
          name: "some_heapsize",
          value: "1000"
        }),
        Em.Object.create({
          name: "some_newsize",
          value: "1000"
        }),
        Em.Object.create({
          name: "some_maxnewsize",
          value: "1000"
        }),
        Em.Object.create({
          name: "hadoop_heapsize",
          value: "1000"
        })
      ]),
      result: {
        "type": "global",
        "tag": "version1",
        "properties": {
          "property1": "value1",
          "property2": "value2<",
          "some_heapsize": "1000m",
          "some_newsize": "1000m",
          "some_maxnewsize": "1000m",
          "hadoop_heapsize": "1000"
        }
      }
    };
    it("create global object", function () {
      expect(mainServiceInfoConfigsController.createGlobalSiteObj(t.tagName, t.globalConfigs)).to.deep.eql(t.result);
    });
  });

  describe("#doPUTClusterConfigurationSiteErrorCallback", function () {
    it("set doPUTClusterConfigurationSiteResult to false", function () {
      mainServiceInfoConfigsController.doPUTClusterConfigurationSiteErrorCallback({responseText: ""});
      expect(mainServiceInfoConfigsController.get("doPUTClusterConfigurationSiteResult")).to.equal(false);
    });
  });

  describe("#doPUTClusterConfigurationSiteSuccessCallback", function () {
    it("set doPUTClusterConfigurationSiteResult to true", function () {
      mainServiceInfoConfigsController.doPUTClusterConfigurationSiteSuccessCallback();
      expect(mainServiceInfoConfigsController.get("doPUTClusterConfigurationSiteResult")).to.equal(true);
    });
  });

  describe("#doPUTClusterConfigurationSite", function () {
    var t = {
      data: "data",
      request: {
        Clusters: {
          desired_config: "data"
        }
      }
    };
    var temp = App.router.getClusterName;
    beforeEach(function () {
      App.router.getClusterName = function () {
        return "clName";
      };
      sinon.spy($, "ajax");
    });
    afterEach(function () {
      $.ajax.restore();
      App.router.getClusterName = temp;
    });
    it("ajax request to put clsuter cfg", function () {
      expect(mainServiceInfoConfigsController.doPUTClusterConfigurationSite(t.data)).to.equal(mainServiceInfoConfigsController.get("doPUTClusterConfigurationSiteResult"));
      expect(JSON.parse($.ajax.args[0][0].data)).to.deep.equal(t.request);
    });
  });

  describe("#isConfigChanged", function () {

    var tests = [
      {
        loadedConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase",
          lzo_enabled: "true"
        },
        savingConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase",
          lzo_enabled: "true"
        },
        m: "configs doesn't changed",
        res: false
      },
      {
        loadedConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase",
          lzo_enabled: "true"
        },
        savingConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase",
          lzo_enabled: "false"
        },
        m: "configs changed",
        res: true
      },
      {
        loadedConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase"
        },
        savingConfig: {
          apptimelineserver_heapsize: "1024",
          hbase_log_dir: "/var/log/hbase",
          lzo_enabled: "false"
        },
        m: "add new config",
        res: true
      }
    ];

    tests.forEach(function(t){
      it(t.m, function () {
        expect(mainServiceInfoConfigsController.isConfigChanged(t.loadedConfig, t.savingConfig)).to.equal(t.res);
      });
    });
  });

});
