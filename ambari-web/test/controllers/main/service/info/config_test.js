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
      sinon.stub(mainServiceInfoConfigsController, "restartServicePopup", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getHash", function () {
        return "hash"
      });
      App.router.route = Em.K;
    });
    afterEach(function () {
      mainServiceInfoConfigsController.restartServicePopup.restore();
      mainServiceInfoConfigsController.getHash.restore();
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
  });

  describe("#doCancel", function () {
    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
    });
    afterEach(function () {
      Em.run.once.restore();
    });
    it("trigger onConfigGroupChange", function () {
      mainServiceInfoConfigsController.doCancel();
      expect(Em.run.once.calledWith(mainServiceInfoConfigsController, "onConfigGroupChange")).to.equal(true);
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
    var tests = [
      {
        content: {
          hostComponents: [
            Em.Object.create({
              componentName: "componentName1",
              hostName: "hostName"
            })
          ]
        },
        result: "hostName",
        multiple: false,
        m: "returns hostname"
      },
      {
        content: {
          hostComponents: [
            Em.Object.create({
              componentName: "componentName2",
              hostName: "hostName1"
            }),
            Em.Object.create({
              componentName: "componentName2",
              hostName: "hostName2"
            })
          ]
        },
        result: ["hostName1","hostName2"],
        multiple: true,
        m: "returns hostnames"
      }
    ];
    tests.forEach(function(t){
      beforeEach(function () {
        mainServiceInfoConfigsController.set("content", { hostComponents: []});
      });

      it(t.m, function () {
        mainServiceInfoConfigsController.set("content.hostComponents", t.content.hostComponents);
        expect(mainServiceInfoConfigsController.getMasterComponentHostValue(t.content.hostComponents[0].componentName, t.multiple)).to.eql(t.result);
      });
    });
  });

  describe("#setServerConfigValue", function () {

    it("parsing storm.zookeeper.servers property in non standart method", function () {
      expect(mainServiceInfoConfigsController.setServerConfigValue("storm.zookeeper.servers", ["a", "b"])).to.equal('[\'a\',\'b\']');
    });
    it("parsing default properties", function () {
      expect(mainServiceInfoConfigsController.setServerConfigValue("any.other.property", "value")).to.equal("value");
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
          },
          {
            name: "property_heapsize",
            value: "value3"
          },
          {
            name: "property_permsize",
            value: "value4m"
          }
        ]),
        result: {
          "type": "hdfs-site",
          "tag": "version1",
          "properties": {
            "property1": "value1",
            "property2": "value2&lt;",
            "property_heapsize": "value3m",
            "property_permsize": "value4m"
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
            "property2": "value2&lt;"
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

  describe("#doPUTClusterConfigurationSites", function () {
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
    it("ajax request to put clsuter cfg", function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      expect(mainServiceInfoConfigsController.doPUTClusterConfigurationSites([])).to.equal(mainServiceInfoConfigsController.get("doPUTClusterConfigurationSiteResult"));
      expect(App.ajax.send.calledOnce).to.be.true;
    });
    it('values should be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', sc);
      mainServiceInfoConfigsController.doPUTClusterConfigurationSites([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
    });
    it('values should not be parsed', function () {
      mainServiceInfoConfigsController.set('stepConfigs', scExc);
      mainServiceInfoConfigsController.doPUTClusterConfigurationSites([]);
      expect(mainServiceInfoConfigsController.get('stepConfigs')[0].get('configs').mapProperty('value').uniq()).to.eql(['1024m']);
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

  describe("#addDynamicProperties", function() {

    var tests = [
      {
        stepConfigs: [Em.Object.create({
          serviceName: "HIVE",
          configs: []
        })],
        content: Em.Object.create({
          serviceName: "HIVE"
        }),
        m: "add dynamic property",
        addDynamic: true
      },
      {
        stepConfigs: [Em.Object.create({
          serviceName: "HIVE",
          configs: [
            Em.Object.create({
              name: "templeton.hive.properties"
            })
          ]
        })],
        content: Em.Object.create({
          serviceName: "HIVE"
        }),
        m: "don't add dynamic property (already included)",
        addDynamic: false
      },
      {
        stepConfigs: [Em.Object.create({
          serviceName: "HDFS",
          configs: []
        })],
        content: Em.Object.create({
          serviceName: "HDFS"
        }),
        m: "don't add dynamic property (wrong service)",
        addDynamic: false
      }
    ];
    var dynamicProperty = {
      "name": "templeton.hive.properties",
      "templateName": ["hivemetastore_host"],
      "foreignKey": null,
      "value": "hive.metastore.local=false,hive.metastore.uris=thrift://<templateName[0]>:9083,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse",
      "filename": "webhcat-site.xml"
    };



    tests.forEach(function(t) {
      it(t.m, function() {
        mainServiceInfoConfigsController.set("content", t.content);
        mainServiceInfoConfigsController.set("stepConfigs", t.stepConfigs);
        var configs = [];
        mainServiceInfoConfigsController.addDynamicProperties(configs);
        if (t.addDynamic){
          expect(configs.findProperty("name","templeton.hive.properties")).to.deep.eql(dynamicProperty);
        }
      });
    });
  });

  describe("#loadUiSideConfigs", function () {

    var t = {
      configMapping: [
        {
          foreignKey: null,
          templateName: "",
          value: "default",
          name: "name1",
          filename: "filename1"
        },
        {
          foreignKey: "notNull",
          templateName: "",
          value: "default2",
          name: "name2",
          filename: "filename2"
        }
      ],
      configMappingf: [
        {
          foreignKey: null,
          templateName: "",
          value: "default",
          name: "name1",
          filename: "filename1"
        }
      ],
      valueWithOverrides: {
        "value": "default",
        "overrides": {
          "value1": "value1",
          "value2": "value2"
        }
      },
      uiConfigs: [
        {
          "id": "site property",
          "name": "name1",
          "value": "default",
          "filename": "filename1",
          "overrides": {
            "value1": "value1",
            "value2": "value2"
          }
        }
      ]
    };

    beforeEach(function(){
      sinon.stub(mainServiceInfoConfigsController, "addDynamicProperties", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "getGlobConfigValueWithOverrides", function () {
        return t.valueWithOverrides
      });
    });

    afterEach(function(){
      mainServiceInfoConfigsController.addDynamicProperties.restore();
      mainServiceInfoConfigsController.getGlobConfigValueWithOverrides.restore();
    });

    it("load ui config", function() {
      expect(mainServiceInfoConfigsController.loadUiSideConfigs(t.configMapping)[0]).to.deep.equal(t.uiConfigs[0]);
      expect(mainServiceInfoConfigsController.addDynamicProperties.calledWith(t.configMappingf)).to.equal(true);
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
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1 ", displayType: "advanced" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1 ", displayType: "advanced" })
      ],
      result: [
        Em.Object.create({ name: "p1", value: " v1 v1", displayType: "" }),
        Em.Object.create({ name: "p2", value: "true", displayType: "" }),
        Em.Object.create({ name: "p3", value: "d1", displayType: "directory" }),
        Em.Object.create({ name: "p4", value: "d1,d2,d3", displayType: "directories" }),
        Em.Object.create({ name: "p5", value: " v1 ", displayType: "password" }),
        Em.Object.create({ name: "p6", value: "v", displayType: "host" }),
        Em.Object.create({ name: "javax.jdo.option.ConnectionURL", value: " v1", displayType: "advanced" }),
        Em.Object.create({ name: "oozie.service.JPAService.jdbc.url", value: " v1", displayType: "advanced" })
      ]
    };

    it("format config values", function () {
      mainServiceInfoConfigsController.formatConfigValues(t.configs);
      expect(t.configs).to.deep.equal(t.result);
    });

  });

  describe("#createConfigObject", function() {
    var tests = [
      {
        siteName: "core-site",
        serviceName: "HDFS",
        method: "createCoreSiteObj"
      },
      {
        siteName: "core-site",
        serviceName: "ANY",
        method: false
      },
      {
        siteName: "any",
        method: "createSiteObj"
      },
      {
        siteName: "mapred-queue-acls",
        method: false,
        capacitySchedulerUi: false
      },
      {
        siteName: "mapred-queue-acls",
        method: "createSiteObj",
        capacitySchedulerUi: true
      }
    ];

    var capacitySchedulerUi = App.supports.capacitySchedulerUi;
    beforeEach(function() {
      sinon.stub(mainServiceInfoConfigsController, "createCoreSiteObj", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "createSiteObj", Em.K);
      mainServiceInfoConfigsController.set("content", {});
    });

    afterEach(function() {
      mainServiceInfoConfigsController.createCoreSiteObj.restore();
      mainServiceInfoConfigsController.createSiteObj.restore();
      App.supports.capacitySchedulerUi = capacitySchedulerUi;
    });

    tests.forEach(function(t) {
      it("create object for " + t.siteName + " run method " + t.method, function() {
        App.supports.capacitySchedulerUi = t.capacitySchedulerUi;
        mainServiceInfoConfigsController.set("content.serviceName", t.serviceName);
        mainServiceInfoConfigsController.createConfigObject(t.siteName, "versrion1");
        if (t.method) {
          expect(mainServiceInfoConfigsController[t.method].calledOnce).to.equal(true);
        } else {
          expect(mainServiceInfoConfigsController["createCoreSiteObj"].calledOnce).to.equal(false);
          expect(mainServiceInfoConfigsController["createSiteObj"].calledOnce).to.equal(false);
        }
      });
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

  describe("#setValueForCheckBox", function() {
    var tests = [
      {
        serviceConfigPropertyInput: Em.Object.create({
          value: "true",
          defaultValue: "true",
          displayType: 'checkbox'
        }),
        serviceConfigProperty: Em.Object.create({
          value: true,
          defaultValue: true,
          displayType: 'checkbox'
        })
      },
      {
        serviceConfigPropertyInput: Em.Object.create({
          value: "false",
          defaultValue: "false",
          displayType: 'checkbox'
        }),
        serviceConfigProperty: Em.Object.create({
          value: false,
          defaultValue: false,
          displayType: 'checkbox'
        })
      },
      {
        serviceConfigPropertyInput: Em.Object.create({
          value: "false",
          defaultValue: "false"
        }),
        serviceConfigProperty: Em.Object.create({
          value: "false",
          defaultValue: "false"
        })
      }
    ];
    tests.forEach(function(t) {
      it("set " + t.serviceConfigPropertyInput.value, function(){
        var serviceConfigProperty = t.serviceConfigPropertyInput;
        mainServiceInfoConfigsController.setValueForCheckBox(serviceConfigProperty);
        expect(serviceConfigProperty).to.eql(t.serviceConfigProperty);
      });
    });
  });

  describe("#setEditability", function () {

    var tests = [
      {
        isAdmin: true,
        isHostsConfigsPage: false,
        defaultGroupSelected: true,
        isReconfigurable: true,
        isEditable: true,
        m: ""
      },
      {
        isAdmin: false,
        isHostsConfigsPage: false,
        defaultGroupSelected: true,
        isReconfigurable: true,
        isEditable: false,
        m: "(non admin)"
      },
      {
        isAdmin: true,
        isHostsConfigsPage: true,
        defaultGroupSelected: true,
        isReconfigurable: true,
        isEditable: false,
        m: "(isHostsConfigsPage)"
      },
      {
        isAdmin: true,
        isHostsConfigsPage: false,
        defaultGroupSelected: false,
        isReconfigurable: true,
        isEditable: false,
        m: "(defaultGroupSelected is false)"
      },
      {
        isAdmin: true,
        isHostsConfigsPage: false,
        defaultGroupSelected: true,
        isReconfigurable: false,
        isEditable: false,
        m: "(isReconfigurable is false)"
      }
    ];

    var a = App.get('isAdmin');
    afterEach(function () {
      App.set('isAdmin', a);
    });
    tests.forEach(function(t) {
      it("set isEditable " + t.isEditable + t.m, function(){
        App.set('isAdmin', t.isAdmin);
        mainServiceInfoConfigsController.set("isHostsConfigsPage", t.isHostsConfigsPage);
        var serviceConfigProperty = Em.Object.create({
          isReconfigurable: t.isReconfigurable
        });
        mainServiceInfoConfigsController.setEditability(serviceConfigProperty, t.defaultGroupSelected);
        expect(serviceConfigProperty.get("isEditable")).to.equal(t.isEditable);
      });
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
      sinon.stub(mainServiceInfoConfigsController,"addOverrideProperty", Em.K)
    });
    afterEach(function() {
      mainServiceInfoConfigsController.addOverrideProperty.restore();
    });
    tests.forEach(function(t) {
      it(t.m, function() {
        mainServiceInfoConfigsController.set("overrideToAdd", t.overrideToAdd);
        mainServiceInfoConfigsController.checkOverrideProperty(t.componentConfig);
        if(t.add) {
          expect(mainServiceInfoConfigsController.addOverrideProperty.calledWith(t.overrideToAdd)).to.equal(true);
          expect(mainServiceInfoConfigsController.get("overrideToAdd")).to.equal(null);
        } else {
          expect(mainServiceInfoConfigsController.addOverrideProperty.calledOnce).to.equal(false);
        }
      });
    });
  });

  describe("#setValuesForOverrides", function() {
    var tests = [
      {
        overrides: [
          {name: "override1"},
          {name: "override2"}
        ],
        _serviceConfigProperty: {},
        serviceConfigProperty: Em.Object.create({overrides: Em.A([])}),
        defaultGroupSelected: true
      }
    ];
    beforeEach(function() {
      sinon.stub(mainServiceInfoConfigsController, "createNewSCP", function(override) {return {name: override.name}})
    });
    afterEach(function() {
      mainServiceInfoConfigsController.createNewSCP.restore();
    });
    tests.forEach(function(t) {
      it("set values for overrides. use createNewSCP method to do this", function() {
        var serviceConfigProperty = t.serviceConfigProperty;
        mainServiceInfoConfigsController.setValuesForOverrides(t.overrides, serviceConfigProperty, t.serviceConfigProperty, t.defaultGroupSelected);
        expect(serviceConfigProperty.get("overrides")[0]).to.eql(t.overrides[0]);
        expect(serviceConfigProperty.get("overrides")[1]).to.eql(t.overrides[1]);
      });
    });
  });

  describe("#createConfigProperty", function() {
    var tests = [
      {
        _serviceConfigProperty: {
          overrides: {

          }
        },
        defaultGroupSelected: true,
        restartData: {},
        serviceConfigsData: {},
        serviceConfigProperty: {
          overrides: null,
          isOverridable: true
        }
      }];
    beforeEach(function() {
      sinon.stub(mainServiceInfoConfigsController, "setValueForCheckBox", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "setValidator", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "setValuesForOverrides", Em.K);
      sinon.stub(mainServiceInfoConfigsController, "setEditability", Em.K);
    });
    afterEach(function() {
      mainServiceInfoConfigsController.setValueForCheckBox.restore();
      mainServiceInfoConfigsController.setValidator.restore();
      mainServiceInfoConfigsController.setValuesForOverrides.restore();
      mainServiceInfoConfigsController.setEditability.restore();
    });
    tests.forEach(function(t) {
      it("create service config. run methods to correctly set object fileds", function() {
        var result = mainServiceInfoConfigsController.createConfigProperty(t._serviceConfigProperty, t.defaultGroupSelected, t.restartData, t.serviceConfigsData);
        expect(mainServiceInfoConfigsController.setValueForCheckBox.calledWith(t.serviceConfigProperty));
        expect(mainServiceInfoConfigsController.setValidator.calledWith(t.serviceConfigProperty, t.serviceConfigsData));
        expect(mainServiceInfoConfigsController.setValuesForOverrides.calledWith(t._serviceConfigProperty.overrides, t._serviceConfigProperty, t.serviceConfigProperty, t.defaultGroupSelected));
        expect(mainServiceInfoConfigsController.setValidator.calledWith(t.serviceConfigProperty, t.defaultGroupSelected));
        expect(result.getProperties('overrides','isOverridable')).to.eql(t.serviceConfigProperty);
      });
    });
  });

  describe("#createNewSCP", function() {
    var tests = [
      {
        overrides: {
          value: "value",
          group: {
            value: "group1"
          }
        },
        _serviceConfigProperty: {},
        serviceConfigProperty: Em.Object.create({
          value: "parentSCP",
          supportsFinal: true
        }),
        defaultGroupSelected: true,

        newSCP: {
          value: "value",
          isOriginalSCP: false,
          parentSCP:Em.Object.create({
            value: "parentSCP",
            supportsFinal: true
          }),
          group: {
            value: "group1"
          },
          isEditable: false
        }
      }
    ];
    tests.forEach(function(t) {
      it("", function() {
        var newSCP = mainServiceInfoConfigsController.createNewSCP(t.overrides, t._serviceConfigProperty, t.serviceConfigProperty, t.defaultGroupSelected);
        expect(newSCP.getProperties("value", "isOriginalSCP", "parentSCP", "group", "isEditable")).to.eql(t.newSCP);
      });
    });
  });

  describe("setCompareDefaultGroupConfig", function () {
    beforeEach(function () {
      sinon.stub(mainServiceInfoConfigsController, "getComparisonConfig").returns("compConfig");
      sinon.stub(mainServiceInfoConfigsController, "getMockComparisonConfig").returns("mockConfig");
      sinon.stub(mainServiceInfoConfigsController, "hasCompareDiffs").returns(true);
    });
    afterEach(function () {
      mainServiceInfoConfigsController.getComparisonConfig.restore();
      mainServiceInfoConfigsController.getMockComparisonConfig.restore();
      mainServiceInfoConfigsController.hasCompareDiffs.restore();
    });
    it("expect that setCompareDefaultGroupConfig will not run anything", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({}).compareConfigs.length).to.equal(0);
    });
    it("expect that setCompareDefaultGroupConfig will not run anything", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({}, {}).compareConfigs.length).to.equal(0);
    });
    it("expect that serviceConfig.compareConfigs will be getMockComparisonConfig", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, null)).to.eql({compareConfigs: ["mockConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isUserProperty: true}, {})).to.eql({compareConfigs: ["compConfig"], isUserProperty: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isComparison: true, hasCompareDiffs: true});
    });
    it("expect that serviceConfig.compareConfigs will be getComparisonConfig", function () {
      expect(mainServiceInfoConfigsController.setCompareDefaultGroupConfig({isReconfigurable: true, isMock: true}, {})).to.eql({compareConfigs: ["compConfig"], isReconfigurable: true, isMock: true, isComparison: true, hasCompareDiffs: true});
    });
  });

  describe("#trackRequest()", function () {
    after(function(){
      mainServiceInfoConfigsController.set('requestInProgress', null);
    });
    it("should set requestInProgress", function () {
      mainServiceInfoConfigsController.trackRequest({'request': {}});
      expect(mainServiceInfoConfigsController.get('requestInProgress')).to.eql({'request': {}});
    });
  });
});

describe("#setCompareDefaultGroupConfig", function() {

  describe('#showSaveConfigsPopup', function () {

    var bodyView;

    describe('#bodyClass', function () {
      beforeEach(function() {
        sinon.stub(App.ajax, 'send', Em.K);
        // default implementation
        bodyView = mainServiceInfoConfigsController.showSaveConfigsPopup().get('bodyClass').create({
          parentView: Em.View.create()
        });
      });

      afterEach(function() {
        App.ajax.send.restore();
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

});
