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
var modelSetup = require('test/init_model_test');
require('utils/ajax/ajax_queue');
require('controllers/main/admin/security');
require('controllers/main/service/info/configs');
require('controllers/wizard/step8_controller');
var installerStep8Controller, configurationController;

describe('App.WizardStep8Controller', function () {

  var configs = Em.A([
    Em.Object.create({filename: 'hdfs-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hdfs-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hue-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hue-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'mapred-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'mapred-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'yarn-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'yarn-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hbase-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hbase-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'oozie-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'oozie-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hive-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hive-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'pig-properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p2', value: 'v2'})
  ]);

  beforeEach(function () {
    installerStep8Controller = App.WizardStep8Controller.create({
      configs: configs
    });
    configurationController = App.MainServiceInfoConfigsController.create({});
  });

  var siteObjTests = Em.A([
    {name: 'createHdfsSiteObj', e: {type: 'hdfs-site', tag: 'version1', l: 2}},
    {name: 'createHueSiteObj', e: {type: 'hue-site', tag: 'version1', l: 2}},
    {name: 'createMrSiteObj', e: {type: 'mapred-site', tag: 'version1', l: 2}},
    {name: 'createYarnSiteObj', e: {type: 'yarn-site', tag: 'version1', l: 2}},
    {name: 'createCapacityScheduler', e: {type: 'capacity-scheduler', tag: 'version1', l: 2}},
    {name: 'createMapredQueueAcls', e: {type: 'mapred-queue-acls', tag: 'version1', l: 2}},
    {name: 'createHbaseSiteObj', e: {type: 'hbase-site', tag: 'version1', l: 2}},
    {name: 'createOozieSiteObj', e: {type: 'oozie-site', tag: 'version1', l: 2}},
    {name: 'createHiveSiteObj', e: {type: 'hive-site', tag: 'version1', l: 2}},
    {name: 'createWebHCatSiteObj', e: {type: 'webhcat-site', tag: 'version1', l: 2}},
    {name: 'createTezSiteObj', e: {type: 'tez-site', tag: 'version1', l: 2}},
    {name: 'createPigPropertiesSiteObj', e: {type: 'pig-properties', tag: 'version1', l: 1}},
    {name: 'createFalconStartupSiteObj', e: {type: 'falcon-startup.properties', tag: 'version1', l: 2}},
    {name: 'createFalconRuntimeSiteObj', e: {type: 'falcon-runtime.properties', tag: 'version1', l: 2}}
  ]);

  siteObjTests.forEach(function (test) {
    describe('#' + test.name, function () {

      it(test.name, function () {

        var siteObj = installerStep8Controller.createSiteObj(test.e.type);
        expect(siteObj.tag).to.equal(test.e.tag);
        expect(Em.keys(siteObj.properties).length).to.equal(test.e.l);
      });

    });
  });

  describe('#createConfigurations', function () {

    it('verify if its installerController', function () {
      installerStep8Controller.set('content', {controllerName: 'installerController', services: Em.A([])});
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(4);
      installerStep8Controller.clearStep();
    });

    it('verify if its not installerController', function () {
      installerStep8Controller.set('content', {controllerName: 'addServiceController', services: Em.A([])});
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(2);
      installerStep8Controller.clearStep();
    });

    it('verify not App.supports.capacitySchedulerUi', function () {
      installerStep8Controller = App.WizardStep8Controller.create({
        content: {controllerName: 'addServiceController', services: Em.A([
          {isSelected: true, isInstalled: false, serviceName: 'MAPREDUCE'}
        ])},
        configs: configs
      });
      App.set('supports.capacitySchedulerUi', false);
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(4);
      installerStep8Controller.clearStep();
    });

    it('verify App.supports.capacitySchedulerUi', function () {
      installerStep8Controller = App.WizardStep8Controller.create({
        content: {controllerName: 'addServiceController', services: Em.A([
          {isSelected: true, isInstalled: false, serviceName: 'MAPREDUCE'}
        ])},
        configs: configs
      });
      App.set('supports.capacitySchedulerUi', true);
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(6);
      installerStep8Controller.clearStep();
    });


    // e - without global and core!
    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']), e: 2},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN']), e: 5},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE']), e: 7},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE']), e: 9},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 12},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT']), e: 13},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE']), e: 14},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG']), e: 16},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON']), e: 18},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM']), e: 19},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ']), e: 20},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ', 'ZOOKEEPER']), e: 22}

    ]);

    tests.forEach(function (test) {
      it(test.selectedServices.join(','), function () {
        var services = test.selectedServices.map(function (serviceName) {
          return Em.Object.create({isSelected: true, isInstalled: false, serviceName: serviceName});
        });
        installerStep8Controller = App.WizardStep8Controller.create({
          content: {controllerName: 'addServiceController', services: services},
          configs: configs
        });
        installerStep8Controller.createConfigurations();
        expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(test.e + 2);
        installerStep8Controller.clearStep();
      });
    });

    // Verify xml character escaping is not done for log4j files and falcon startup-properties and runtime-properties files.
    it('escape xml character for installer wizard', function () {
      var services = Em.A([Em.Object.create({isSelected: true, isInstalled: false, serviceName: 'OOZIE'}),
        Em.Object.create({isSelected: true, isInstalled: false, serviceName: 'FALCON'})]);

      var nonXmlConfigs = [
        {filename: 'oozie-log4j.xml', name: 'p1', value: "'.'v1"},
        {filename: 'falcon-startup.properties.xml', name: 'p1', value: "'.'v1"} ,
        {filename: 'falcon-startup.properties.xml', name: 'p2', value: 'v2'},
        {filename: 'falcon-runtime.properties.xml', name: 'p1', value: "'.'v1"},
        {filename: 'falcon-runtime.properties.xml', name: 'p2', value: 'v2'}
      ];
      installerStep8Controller = App.WizardStep8Controller.create({
        content: {controllerName: 'installerController', services: services},
        configs: nonXmlConfigs
      });
      installerStep8Controller.createConfigurations();
      var nonXmlConfigTypes = ['oozie-log4j', 'falcon-startup.properties', 'falcon-runtime.properties'];
      nonXmlConfigTypes.forEach(function (_nonXmlConfigType) {
        var nonXmlConfigTypeObj = installerStep8Controller.get('serviceConfigTags').findProperty('type', _nonXmlConfigType);
        var nonXmlSitePropertyVal = nonXmlConfigTypeObj.properties['p1'];
        expect(nonXmlSitePropertyVal).to.equal("'.'v1");
      });
      installerStep8Controller.clearStep();
    });

  });

  describe('#createSelectedServicesData', function () {

    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']), e: 2},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN']), e: 5},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE']), e: 7},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE']), e: 9},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 12},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT']), e: 13},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE']), e: 14},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG']), e: 15},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON']), e: 17},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM']), e: 18},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ']), e: 19},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'WEBHCAT', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ', 'ZOOKEEPER']), e: 21}
    ]);

    tests.forEach(function (test) {
      it(test.selectedServices.join(','), function () {
        var services = test.selectedServices.map(function (serviceName) {
          return Em.Object.create({isSelected: true, isInstalled: false, serviceName: serviceName});
        });
        installerStep8Controller = App.WizardStep8Controller.create({
          content: {controllerName: 'addServiceController', services: services},
          configs: configs
        });
        var serviceData = installerStep8Controller.createSelectedServicesData();
        expect(serviceData.mapProperty('ServiceInfo.service_name')).to.eql(test.selectedServices.toArray());
        installerStep8Controller.clearStep();
      });
    });

  });

  describe('#getRegisteredHosts', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: ['h1'],
        m: 'Two hosts, one registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: [],
        m: 'Two hosts, zero registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2'})
        },
        e: ['h1', 'h2'],
        m: 'Two hosts, two registered'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHosts = installerStep8Controller.getRegisteredHosts();
        expect(registeredHosts.mapProperty('hostName').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#createRegisterHostData', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h1', 'h2'],
        m: 'two registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'two registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2', isInstalled: false})
        },
        e: [],
        m: 'zero registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: true})
        },
        e: [],
        m: 'two registered, zeto insInstalled false'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHostData = installerStep8Controller.createRegisterHostData();
        expect(registeredHostData.mapProperty('Hosts.host_name').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#clusterName', function () {
    it('should be equal to content.cluster.name', function () {
      installerStep8Controller.set('content', {cluster: {name: 'new_name'}});
      expect(installerStep8Controller.get('clusterName')).to.equal('new_name');
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'clearStep', Em.K);
      sinon.stub(installerStep8Controller, 'formatProperties', Em.K);
      sinon.stub(installerStep8Controller, 'loadGlobals', Em.K);
      sinon.stub(installerStep8Controller, 'loadConfigs', Em.K);
      sinon.stub(installerStep8Controller, 'loadClusterInfo', Em.K);
      sinon.stub(installerStep8Controller, 'loadServices', Em.K);
      installerStep8Controller.set('content', {controllerName: 'installerController'});
    });
    afterEach(function () {
      installerStep8Controller.clearStep.restore();
      installerStep8Controller.formatProperties.restore();
      installerStep8Controller.loadGlobals.restore();
      installerStep8Controller.loadConfigs.restore();
      installerStep8Controller.loadClusterInfo.restore();
      installerStep8Controller.loadServices.restore();
    });
    it('should call clearStep', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadClusterInfo', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadClusterInfo.calledOnce).to.equal(true);
    });
    it('should call loadServices', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call formatProperties if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call loadGlobals if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadGlobals.calledOnce).to.equal(true);
    });
    it('should call loadConfigs if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadConfigs.calledOnce).to.equal(true);
    });
    it('should set isSubmitDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
    it('should call setSecurityStatus for non-installerController', function () {
      var obj = Em.Object.create({
        setSecurityStatus: Em.K
      });
      sinon.stub(App.router, 'get', function () {
        return obj;
      });
      sinon.spy(obj, 'setSecurityStatus');
      installerStep8Controller.set('content.controllerName', 'addServiceController');
      installerStep8Controller.loadStep();
      expect(obj.setSecurityStatus.calledOnce).to.equal(true);
      obj.setSecurityStatus.restore();
      App.router.get.restore();

    });
  });

  describe('#loadGlobals', function () {
    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'removeHiveConfigs', function (o) {
        return o;
      });
      sinon.stub(installerStep8Controller, 'removeOozieConfigs', function (o) {
        return o;
      });
    });
    afterEach(function () {
      installerStep8Controller.removeHiveConfigs.restore();
      installerStep8Controller.removeOozieConfigs.restore();
    });
    Em.A([
        {
          configs: [],
          m: 'empty configs, removeHiveConfigs isn\'t called, removeOozieConfigs ins\'t called',
          e: {
            globals: [],
            removeHiveConfigs: false,
            removeOozieConfigs: false
          }
        },
        {
          configs: [
            Em.Object.create({id: 'puppet var', name: 'n1'})
          ],
          m: 'not empty configs, removeHiveConfigs isn\'t called, removeOozieConfigs ins\'t called',
          e: {
            globals: ['n1'],
            removeHiveConfigs: false,
            removeOozieConfigs: false
          }
        },
        {
          configs: [
            Em.Object.create({id: 'puppet var', name: 'n1'}),
            Em.Object.create({id: 'puppet var', name: 'hive_database'})
          ],
          m: 'not empty configs, removeHiveConfigs called, removeOozieConfigs ins\'t called',
          e: {
            globals: ['n1', 'hive_database'],
            removeHiveConfigs: true,
            removeOozieConfigs: false
          }
        },
        {
          configs: [
            Em.Object.create({id: 'puppet var', name: 'n1'}),
            Em.Object.create({id: 'puppet var', name: 'oozie_database'})
          ],
          m: 'not empty configs, removeHiveConfigs isn\'t called, removeOozieConfigs called',
          e: {
            globals: ['n1', 'oozie_database'],
            removeHiveConfigs: false,
            removeOozieConfigs: true
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.set('content', {serviceConfigProperties: test.configs});
          installerStep8Controller.loadGlobals();
          if (test.e.removeHiveConfigs) {
            expect(installerStep8Controller.removeHiveConfigs.calledOnce).to.equal(true);
          }
          else {
            expect(installerStep8Controller.removeHiveConfigs.called).to.equal(false);
          }
          if (test.e.removeOozieConfigs) {
            expect(installerStep8Controller.removeOozieConfigs.calledOnce).to.equal(true);
          }
          else {
            expect(installerStep8Controller.removeOozieConfigs.called).to.equal(false);
          }
          expect(installerStep8Controller.get('globals').mapProperty('name')).to.eql(test.e.globals);
        });
      });
  });

  describe('#loadServices()', function() {
    var serviceComponentGenerator = function(componentName, displayName, componentValue) {
      return Em.Object.create({
        component_name: componentName,
        display_name: displayName,
        component_value: componentValue
      })
    };

    var slaveComponentGenerator = function(componentName, hosts) {
      return Em.Object.create({
        componentName: componentName,
        hosts: hosts.map(function(host) { return {'hostName' : host } })
      });
    };
    var masterComponentGenerator = function(componentName, hostName) {
      return Em.Object.create({
        component: componentName,
        hostName: hostName
      });
    };

    var serviceConfigGenerator = function(name, value) {
      return Em.Object.create({
        name: name,
        value: value
      });
    }
    before(function() {
      modelSetup.setupStackServiceComponent();
      var services = ['HDFS', 'YARN', 'TEZ', 'NAGIOS', 'GANGLIA','OOZIE'];
      this.controller = App.WizardStep8Controller.create({
        content: {
          services: App.StackService.find().setEach('isSelected', true).setEach('isInstalled', false).filterProperty('stackVersion', '2.1').filter(function(service) {
            return services.contains(service.get('serviceName'));
          }),
          slaveComponentHosts: Em.A([
            slaveComponentGenerator('DATANODE', ['h1','h2']),
            slaveComponentGenerator('NODEMANAGER', ['h1']),
            slaveComponentGenerator('CLIENT', ['h1'])
          ]),
          masterComponentHosts: Em.A([
            masterComponentGenerator('NAMENODE', 'h1'),
            masterComponentGenerator('SECONDARY_NAMENODE', 'h2'),
            masterComponentGenerator('APP_TIMELINE_SERVER', 'h1'),
            masterComponentGenerator('RESOURCEMANAGER', 'h1'),
            masterComponentGenerator('NAGIOS_SERVER', 'h1'),
            masterComponentGenerator('GANGLIA_SERVER', 'h2'),
            masterComponentGenerator('OOZIE_SERVER', 'h2')
          ]),
          serviceConfigProperties: Em.A([
            serviceConfigGenerator('nagios_web_login', 'admin'),
            serviceConfigGenerator('nagios_contact', 'admin@admin.com'),
            serviceConfigGenerator('oozie_database', 'New Derby Database'),
            serviceConfigGenerator('oozie_derby_database', '')
          ])
        }
      });
      var _this = this;
      this.controller.reopen({
        wizardController: {
          getDBProperty: function() {
            return _this.controller.get('content.serviceConfigProperties')
          }
        }
      });
      this.controller.loadServices();
    });

    var tests = [
      {
        service_name: 'HDFS',
        display_name: 'HDFS',
        service_components: Em.A([
          serviceComponentGenerator('NAMENODE', 'NameNode', 'h1'),
          serviceComponentGenerator('DATANODE', 'DataNode', '2 hosts'),
          serviceComponentGenerator('SECONDARY_NAMENODE', 'SNameNode', 'h2')
        ])
      },
      {
        service_name: 'YARN',
        display_name: 'YARN + MapReduce2',
        service_components: Em.A([
          serviceComponentGenerator('RESOURCEMANAGER', 'ResourceManager', 'h1'),
          serviceComponentGenerator('NODEMANAGER', 'NodeManager', '1 host'),
          serviceComponentGenerator('APP_TIMELINE_SERVER', 'App Timeline Server', 'h1')
        ])
      },
      {
        service_name: 'TEZ',
        display_name: 'Tez',
        service_components: Em.A([
          serviceComponentGenerator('CLIENT', 'Clients', '1 host')
        ])
      },
      {
        service_name: 'NAGIOS',
        display_name: 'Nagios',
        service_components: Em.A([
          serviceComponentGenerator('NAGIOS_SERVER', 'Server', 'h1'),
          serviceComponentGenerator('Custom', 'Administrator', 'admin / (admin@admin.com)')
        ])
      },
      {
        service_name: 'GANGLIA',
        display_name: 'Ganglia',
        service_components: Em.A([
          serviceComponentGenerator('GANGLIA_SERVER', 'Server', 'h2')
        ])
      },
      {
        service_name: 'OOZIE',
        display_name: 'Oozie',
        service_components: Em.A([
          serviceComponentGenerator('OOZIE_SERVER', 'Server', 'h2'),
          serviceComponentGenerator('Custom', 'Database', ' (New Derby Database)')
        ])
      }
    ];

    tests.forEach(function(test) {
      describe('Load review for `' + test.service_name + '` service', function(){
        it('{0} service should be displayed as {1}'.format(test.service_name, test.display_name), function() {
          expect(this.controller.get('services').findProperty('service_name', test.service_name).get('display_name')).to.eql(test.display_name);
        });
        it('{0}: all components present'.format(test.service_name), function() {
          var serviceObj = this.controller.get('services').findProperty('service_name', test.service_name);
          expect(test.service_components.length).to.be.eql(serviceObj.get('service_components.length'));
        });
        test.service_components.forEach(function(serviceComponent) {
          var testMessage = '`{0}` component present with `{1}` value and displayed as `{2}`';
          it(testMessage.format(serviceComponent.get('component_name'), serviceComponent.get('component_value'), serviceComponent.get('display_name')), function() {
            var serviceObj = this.controller.get('services').findProperty('service_name', test.service_name);
            var component;
            if (serviceComponent.get('component_name') === 'Custom') {
              component = serviceObj.get('service_components').findProperty('display_name', serviceComponent.get('display_name'));
            } else
              component = serviceObj.get('service_components').findProperty('component_name', serviceComponent.get('component_name'));
            if (serviceComponent.get('component_name') !== 'Custom')
              expect(component.get('component_name')).to.eql(serviceComponent.get('component_name'));
            expect(component.get('component_value')).to.eql(serviceComponent.get('component_value'));
            expect(component.get('display_name')).to.eql(serviceComponent.get('display_name'));
          });
        });
      })
    });

    after(function() {
      modelSetup.cleanStackServiceComponent();
    });
  });

  describe('#removeHiveConfigs', function () {
    Em.A([
        {
          globals: [
            {name: 'hive_database', value: 'New MySQL Database'},
            {name: 'hive_ambari_host', value: 'h1'},
            {name: 'hive_hostname', value: 'h2'}
          ],
          removed: Em.A(['hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host',
            'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']),
          hive_database_type: 'mysql',
          m: 'hive_database: New MySQL Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'hive_database', value: 'Existing MySQL Database'},
            {name: 'hive_existing_mysql_host', value: 'h1'},
            {name: 'hive_hostname', value: 'h2'}
          ],
          removed: Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_oracle_host',
            'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']),
          hive_database_type: 'mysql',
          m: 'hive_database: Existing MySQL Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'hive_database', value: 'Existing PostgreSQL Database'},
            {name: 'hive_existing_postgresql_host', value: 'h1'},
            {name: 'hive_hostname', value: 'h2'}
          ],
          removed: Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_oracle_host',
            'hive_existing_oracle_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database']),
          hive_database_type: 'postgres',
          m: 'hive_database: Existing PostgreSQL Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'hive_database', value: 'Existing Oracle Database'},
            {name: 'hive_existing_oracle_host', value: 'h1'},
            {name: 'hive_hostname', value: 'h2'}
          ],
          removed: Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_mysql_host',
            'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']),
          hive_database_type: 'oracle',
          m: 'hive_database: Existing Oracle Database',
          host: 'h1'
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          test.removed.forEach(function (c) {
            test.globals.pushObject({name: c})
          });
          var configs = installerStep8Controller.removeHiveConfigs(test.globals);
          test.removed.forEach(function(name) {
            expect(Em.isNone(configs.findProperty('name', name))).to.equal(true);
          });
          expect(configs.findProperty('name', 'hive_database_type').value).to.equal(test.hive_database_type);
          expect(configs.findProperty('name', 'hive_hostname').value).to.equal(test.host);
        });
      });
  });

  describe('#removeOozieConfigs', function () {
    Em.A([
        {
          globals: [
            {name: 'oozie_database', value: 'New Derby Database'},
            {name: 'oozie_ambari_host', value: 'h1'},
            {name: 'oozie_hostname', value: 'h2'}
          ],
          removed: Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_mysql_host',
            'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database',
            'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']),
          oozie_database_type: 'derby',
          m: 'oozie_database: New Derby Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'oozie_database', value: 'Existing MySQL Database'},
            {name: 'oozie_existing_mysql_host', value: 'h1'},
            {name: 'oozie_hostname', value: 'h2'}
          ],
          removed: Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_oracle_host',
            'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']),
          oozie_database_type: 'mysql',
          m: 'oozie_database: Existing MySQL Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'oozie_database', value: 'Existing PostgreSQL Database'},
            {name: 'oozie_existing_postgresql_host', value: 'h1'},
            {name: 'oozie_hostname', value: 'h2'}
          ],
          removed: Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_oracle_host',
            'oozie_existing_oracle_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database']),
          oozie_database_type: 'postgresql',
          m: 'oozie_database: Existing PostgreSQL Database',
          host: 'h1'
        },
        {
          globals: [
            {name: 'oozie_database', value: 'Existing Oracle Database'},
            {name: 'oozie_existing_oracle_host', value: 'h1'},
            {name: 'oozie_hostname', value: 'h2'}
          ],
          removed: Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_mysql_host',
            'oozie_existing_mysql_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']),
          oozie_database_type: 'oracle',
          m: 'oozie_database: Existing Oracle Database',
          host: 'h1'
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          test.removed.forEach(function (c) {
            if (!test.globals.findProperty('name', c)) {
              test.globals.pushObject({name: c})
            }
          });
          var configs = installerStep8Controller.removeOozieConfigs(test.globals);
          test.removed.forEach(function(name) {
            expect(Em.isNone(configs.findProperty('name', name))).to.equal(true);
          });
          expect(configs.findProperty('name', 'oozie_database_type').value).to.equal(test.oozie_database_type);
          expect(configs.findProperty('name', 'oozie_hostname').value).to.equal(test.host);
        });
      });
  });

  describe('#getRegisteredHosts', function() {
    Em.A([
        {
          hosts: {},
          m: 'no content.hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: ''},
            h2:{bootStatus: ''}
          },
          m: 'no registered hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: 'REGISTERED', hostName: '', name: 'n1'},
            h2:{bootStatus: 'REGISTERED', hostName: '', name: 'n2'}
          },
          m: 'registered hosts available',
          e: ['n1', 'n2']
        }
      ]).forEach(function(test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {hosts: test.hosts});
          var hosts = installerStep8Controller.getRegisteredHosts();
          expect(hosts.mapProperty('hostName')).to.eql(test.e);
        });
    });
  });

  describe('#loadRepoInfo', function() {
    it('should use App.currentStackVersion', function() {
      var version = 'HDP-1.1.1';
      sinon.stub(App, 'get', function() {return version;});
      sinon.stub(App.ajax, 'send', Em.K);
      installerStep8Controller.loadRepoInfo();
      var data = App.ajax.send.args[0][0].data;
      expect(data).to.eql({stackName: 'HDP', stackVersion: '1.1.1'});
      App.ajax.send.restore();
      App.get.restore();
    });
  });

  describe('#loadRepoInfoSuccessCallback', function () {
    beforeEach(function () {
      installerStep8Controller.set('clusterInfo', Em.Object.create({}));
    });
    Em.A([
        {
          items: [],
          m: 'no data',
          e: {
            base_url: [],
            os_type: []
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'bulgenos',
                    base_url: 'url1'
                  }
                }
              ]
            }
          ],
          m: 'unsupported os',
          e: {
            base_url: [],
            os_type: []
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ]
            }
          ],
          m: 'only redhat5',
          e: {
            base_url: ['url1'],
            os_type: [Em.I18n.t("installer.step8.repoInfo.osType.redhat5")]
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ]
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ]
            }
          ],
          m: 'redhat5, redhat6',
          e: {
            base_url: ['url1', 'url2'],
            os_type: [Em.I18n.t("installer.step8.repoInfo.osType.redhat5"), Em.I18n.t("installer.step8.repoInfo.osType.redhat6")]
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ]
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ]
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'sles11',
                    base_url: 'url3'
                  }
                }
              ]
            }
          ],
          m: 'redhat5, redhat6, sles11',
          e: {
            base_url: ['url1', 'url2', 'url3'],
            os_type: [Em.I18n.t("installer.step8.repoInfo.osType.redhat5"), Em.I18n.t("installer.step8.repoInfo.osType.redhat6"), Em.I18n.t("installer.step8.repoInfo.osType.sles11")]
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.loadRepoInfoSuccessCallback({items: test.items});
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('base_url')).to.eql(test.e.base_url);
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('os_type')).to.eql(test.e.os_type);
        });
      });
  });

  describe('#loadRepoInfoErrorCallback', function() {
    it('should set [] to repoInfo', function() {
      installerStep8Controller.set('clusterInfo', Em.Object.create({repoInfo: [{}, {}]}));
      installerStep8Controller.loadRepoInfoErrorCallback({});
      expect(installerStep8Controller.get('clusterInfo.repoInfo.length')).to.eql(0);
    });
  });

  describe('#loadHiveDbValue', function() {
    beforeEach(function() {
      installerStep8Controller.set('wizardController', Em.Object.create({
        getDBProperty: Em.K
      }));
    });
    Em.A([
        {
          serviceConfigProperties: [
            {name: 'hive_database', value: 'New MySQL Database'}
          ],
          m: 'New MySQL Database',
          e: 'MySQL (New Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'hive_database', value: 'Existing MySQL Database'},
            {name: 'hive_existing_mysql_database', value: 'dbname'}
          ],
          m: 'Existing MySQL Database',
          e: 'dbname (Existing MySQL Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'hive_database', value: 'Existing PostgreSQL Database'},
            {name: 'hive_existing_postgresql_database', value: 'dbname'}
          ],
          m: 'Existing PostgreSQL Database',
          e: 'dbname (Existing PostgreSQL Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'hive_database', value: 'Existing Oracle Database'},
            {name: 'hive_existing_oracle_database', value: 'dbname'}
          ],
          m: 'Existing Oracle Database',
          e: 'dbname (Existing Oracle Database)'
        }
      ]).forEach(function(test) {
        it(test.m, function() {
          sinon.stub(installerStep8Controller.get('wizardController'), 'getDBProperty', function() {
            return test.serviceConfigProperties;
          });
          var dbComponent = Em.Object.create({});
          installerStep8Controller.loadHiveDbValue(dbComponent);
          expect(dbComponent.get('component_value')).to.equal(test.e);
          installerStep8Controller.get('wizardController').getDBProperty.restore();
        });
    });
  });

  describe('#loadHbaseMasterValue', function () {
    Em.A([
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'one host',
          e: 'h1'
        },
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}, {component: 'HBASE_MASTER', hostName: 'h2'}, {component: 'HBASE_MASTER', hostName: 'h3'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'many hosts',
          e: 'h1 ' + Em.I18n.t('installer.step8.other').format(2)
        }
      ]).forEach(function (test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadHbaseMasterValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#loadZkServerValue', function() {
    Em.A([
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: '1 host',
          e: '1 host'
        },
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: 'many hosts',
          e: '3 hosts'
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadZkServerValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#loadOozieDbValue', function() {
    beforeEach(function() {
      installerStep8Controller.set('wizardController', Em.Object.create({
        getDBProperty: Em.K
      }));
    });
    Em.A([
        {
          serviceConfigProperties: [
            {name: 'oozie_database', value: 'New Derby Database'},
            {name: 'oozie_derby_database', value: 'dbname'}
          ],
          m: 'New Derby Database',
          e: 'dbname (New Derby Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'oozie_database', value: 'Existing MySQL Database'},
            {name: 'oozie_existing_mysql_database', value: 'dbname'}
          ],
          m: 'Existing MySQL Database',
          e: 'dbname (Existing MySQL Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'oozie_database', value: 'Existing PostgreSQL Database'},
            {name: 'oozie_existing_postgresql_database', value: 'dbname'}
          ],
          m: 'Existing PostgreSQL Database',
          e: 'dbname (Existing PostgreSQL Database)'
        },
        {
          serviceConfigProperties: [
            {name: 'oozie_database', value: 'Existing Oracle Database'},
            {name: 'oozie_existing_oracle_database', value: 'dbname'}
          ],
          m: 'Existing Oracle Database',
          e: 'dbname (Existing Oracle Database)'
        }
      ]).forEach(function(test) {
        it(test.m, function() {
          sinon.stub(installerStep8Controller.get('wizardController'), 'getDBProperty', function() {
            return test.serviceConfigProperties;
          });
          var dbComponent = Em.Object.create({});
          installerStep8Controller.loadOozieDbValue(dbComponent);
          expect(dbComponent.get('component_value')).to.equal(test.e);
          installerStep8Controller.get('wizardController').getDBProperty.restore();
        });
      });
  });

  describe('#loadNagiosAdminValue', function() {
    it('should use serviceConfigProperties nagios_web_login and nagios_contact', function() {
      installerStep8Controller.set('content', {
        serviceConfigProperties: [
          {name: 'nagios_web_login', value: 'admin'},
          {name: 'nagios_contact', value: 'admin@admin.com'}
        ]
      });
      var nagiosAdmin = Em.Object.create({
        component_value: ''
      });
      installerStep8Controller.loadNagiosAdminValue(nagiosAdmin);
      expect(nagiosAdmin.get('component_value')).to.equal('admin / (admin@admin.com)');
    });
  });

  describe('#submit', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'submitProceed', Em.K);
      sinon.spy(App, 'showConfirmationPopup');
    });
    afterEach(function() {
      installerStep8Controller.submitProceed.restore();
      App.showConfirmationPopup.restore();
    });
    Em.A([
        {
          controllerName: 'addHostController',
          securityEnabled: true,
          e: true
        },
        {
          controllerName: 'addHostController',
          securityEnabled: false,
          e: false
        },
        {
          controllerName: 'addServiceController',
          securityEnabled: true,
          e: false
        },
        {
          controllerName: 'addServiceController',
          securityEnabled: false,
          e: false
        }
      ]).forEach(function (test) {
        it(test.controllerName + ' ' + test.securityEnabled.toString(), function () {
          installerStep8Controller.reopen({isSubmitDisabled: false, securityEnabled: test.securityEnabled, content: {controllerName: test.controllerName}});
          installerStep8Controller.submit();
          if (test.e) {
            expect(App.showConfirmationPopup.calledOnce).to.equal(true);
            expect(installerStep8Controller.submitProceed.called).to.equal(false);
          }
          else {
            expect(App.showConfirmationPopup.called).to.equal(false);
            expect(installerStep8Controller.submitProceed.calledOnce).to.equal(true);
          }
        });
      });
    it('should call submitProceed when Ok clicked', function() {
      installerStep8Controller.reopen({isSubmitDisabled: false, securityEnabled: true, content: {controllerName: 'addHostController'}});
      installerStep8Controller.submit().onPrimary();
      expect(installerStep8Controller.submitProceed.calledOnce).to.equal(true);
    });
    it('shouldn\'t do nothing if isSubmitDisabled is true', function() {
      installerStep8Controller.reopen({isSubmitDisabled: true});
      installerStep8Controller.submit();
      expect(App.showConfirmationPopup.called).to.equal(false);
      expect(installerStep8Controller.submitProceed.called).to.equal(false);
    });
  });

  describe('#getExistingClusterNamesSuccessCallBack', function() {
    it('should set clusterNames received from server', function() {
      var data = {
        items:[
          {Clusters: {cluster_name: 'c1'}},
          {Clusters: {cluster_name: 'c2'}},
          {Clusters: {cluster_name: 'c3'}}
        ]
      },
      clasterNames = ['c1','c2','c3'];
      installerStep8Controller.getExistingClusterNamesSuccessCallBack(data);
      expect(installerStep8Controller.get('clusterNames')).to.eql(clasterNames);
    });
  });

  describe('#getExistingClusterNamesErrorCallback', function() {
    it('should set [] to clusterNames', function() {
      installerStep8Controller.set('clusterNames', ['c1', 'c2']);
      installerStep8Controller.getExistingClusterNamesErrorCallback();
      expect(installerStep8Controller.get('clusterNames')).to.eql([]);
    });
  });

  describe('#deleteClusters', function() {
    it('should call App.ajax.send for each provided clusterName', function() {
      sinon.stub(App.ajax, 'send', Em.K);
      var clusterNames = ['h1', 'h2', 'h3'];
      installerStep8Controller.deleteClusters(clusterNames);
      expect(App.ajax.send.callCount).to.equal(clusterNames.length);
      clusterNames.forEach(function(n, i) {
        expect(App.ajax.send.getCall(i).args[0].data).to.eql({name: n});
      });
      App.ajax.send.restore();
    });
  });

  describe('#createSelectedServicesData', function() {
    it('should reformat provided data', function() {
      var selectedServices = [
        Em.Object.create({serviceName: 's1'}),
        Em.Object.create({serviceName: 's2'}),
        Em.Object.create({serviceName: 's3'})
      ];
      var expected = [
        {"ServiceInfo": { "service_name": 's1' }},
        {"ServiceInfo": { "service_name": 's2' }},
        {"ServiceInfo": { "service_name": 's3' }}
      ];
      installerStep8Controller.reopen({selectedServices: selectedServices});
      var createdData = installerStep8Controller.createSelectedServicesData();
      expect(createdData).to.eql(expected);
    });
  });

  describe('#createRegisterHostData', function() {
    it('should return empty data if no hosts', function() {
      sinon.stub(installerStep8Controller, 'getRegisteredHosts', function() {return [];});
      expect(installerStep8Controller.createRegisterHostData()).to.eql([]);
      installerStep8Controller.getRegisteredHosts.restore();
    });
    it('should return computed data', function() {
      var data = [
        {isInstalled: false, hostName: 'h1'},
        {isInstalled: true, hostName: 'h2'},
        {isInstalled: false, hostName: 'h3'}
      ];
      var expected = [
        {"Hosts": { "host_name": 'h1'}},
        {"Hosts": { "host_name": 'h3'}}
      ];
      sinon.stub(installerStep8Controller, 'getRegisteredHosts', function() {return data;});
      expect(installerStep8Controller.createRegisterHostData()).to.eql(expected);
      installerStep8Controller.getRegisteredHosts.restore();
    });
  });

  describe('#createZooCfgObj', function() {
    it('should affect zoo.cfg properties', function() {
      var configs = [
          {filename: 'zoo.cfg', value: 'a&amp;b', name: 'p1'},
          {filename: 'zoo.cfg', value: 'a&lt;b', name: 'p2'},
          {filename: 'zoo.cfg', value: 'a&gt;b', name: 'p3'},
          {filename: 'zoo.cfg', value: 'a&quot;b', name: 'p4'},
          {filename: 'zoo.cfg', value: 'a&apos;b', name: 'p5'}
        ],
        expected = {
          type: 'zoo.cfg',
          tag: 'version1',
          properties: {
            p1: 'a&b',
            p2: 'a<b',
            p3: 'a>b',
            p4: 'a"b',
            p5: 'a\'b'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createZooCfgObj()).to.eql(expected);
    });
  });

  describe('#createStormSiteObj', function() {
    it('should remove quotes for some properties', function() {
      var configs = [
          {filename: 'storm-site.xml', value: ["a", "b"], name: 'nimbus.childopts'},
          {filename: 'storm-site.xml', value: ["a", "b"], name: 'supervisor.childopts'},
          {filename: 'storm-site.xml', value: ["a", "b"], name: 'worker.childopts'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            'nimbus.childopts': '[a,b]',
            'supervisor.childopts': '[a,b]',
            'worker.childopts': '[a,b]'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj()).to.eql(expected);
    });
    it('should replace quote \'"\' to "\'" for some properties', function() {
      var configs = [
          {filename: 'storm-site.xml', value: ["a", "b"], name: 'storm.zookeeper.servers'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            'storm.zookeeper.servers': '[\'a\',\'b\']'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj()).to.eql(expected);
    });
    it('should affect storm-site.xml properties', function() {
      var configs = [
          {filename: 'storm-site.xml', value: 'a&amp;b', name: 'p1'},
          {filename: 'storm-site.xml', value: 'a&lt;b', name: 'p2'},
          {filename: 'storm-site.xml', value: 'a&gt;b', name: 'p3'},
          {filename: 'storm-site.xml', value: 'a&quot;b', name: 'p4'},
          {filename: 'storm-site.xml', value: 'a&apos;b', name: 'p5'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            p1: 'a&b',
            p2: 'a<b',
            p3: 'a>b',
            p4: 'a"b',
            p5: 'a\'b'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj()).to.eql(expected);
    });
  });

  describe('#ajaxQueueFinished', function() {
    it('should call App.router.next', function() {
      sinon.stub(App.router, 'send', Em.K);
      installerStep8Controller.ajaxQueueFinished();
      expect(App.router.send.calledWith('next')).to.equal(true);
      App.router.send.restore();
    });
  });

  describe('#addRequestToAjaxQueue', function() {
    describe('testMode = true', function() {
      before(function() {
        App.set('testMode', true);
      });
      after(function() {
        App.set('testMode', false);
      });
      it('shouldn\'t do nothing', function() {
        installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        installerStep8Controller.addRequestToAjaxQueue({});
        expect(installerStep8Controller.get('ajaxRequestsQueue.queue.length')).to.equal(0);
      });
    });
    describe('testMode = true', function() {
      before(function() {
        App.set('testMode', false);
      });
      it('should add request', function() {
        var clusterName = 'c1';
        installerStep8Controller.reopen({clusterName: clusterName});
        installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        installerStep8Controller.addRequestToAjaxQueue({name:'name', data:{}});
        var request = installerStep8Controller.get('ajaxRequestsQueue.queue.firstObject');
        expect(request.error).to.equal('ajaxQueueRequestErrorCallback');
        expect(request.data.cluster).to.equal(clusterName);
      });
    });
  });

  describe('#ajaxQueueRequestErrorCallback', function() {
    var obj = Em.Object.create({
      registerErrPopup: Em.K,
      setStepsEnable: Em.K
    });
    beforeEach(function() {
      sinon.stub(App.router, 'get', function() {
        return obj;
      });
      sinon.spy(obj, 'registerErrPopup');
      sinon.spy(obj, 'setStepsEnable');
    });
    afterEach(function() {
      App.router.get.restore();
      obj.registerErrPopup.restore();
      obj.setStepsEnable.restore();
    });
    it('should set hasErrorOccurred true', function () {
      installerStep8Controller.set('hasErrorOccurred', false);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('hasErrorOccurred')).to.equal(true);
    });
    it('should set isSubmitDisabled false', function () {
      installerStep8Controller.set('isSubmitDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled false', function () {
      installerStep8Controller.set('isBackBtnDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
    it('should call setStepsEnable', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.setStepsEnable.calledOnce).to.equal(true);
    });
    it('should call registerErrPopup', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.registerErrPopup.calledOnce).to.equal(true);
    });
  });

  describe('#removeInstalledServicesConfigurationGroups', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'deleteConfigGroup', Em.K);
    });
    afterEach(function() {
      App.config.deleteConfigGroup.restore();
    });
    it('should call App.config.deleteConfigGroup for each received group', function() {
      var groups = [{}, {}, {}];
      installerStep8Controller.removeInstalledServicesConfigurationGroups(groups);
      expect(App.config.deleteConfigGroup.callCount).to.equal(groups.length);
    });
  });

  describe('#assignComponentHosts', function() {
    it('component with custom handler', function() {
      var castom_value = 'custom',
        component = Em.Object.create({
        customHandler: 'customHandler'
      });
      installerStep8Controller.reopen({
        customHandler: function(o) {o.set('component_value', castom_value)}
      });
      installerStep8Controller.assignComponentHosts(component);
      expect(component.get('component_value')).to.equal(castom_value);
    });
    it('component is master', function() {
      var component = Em.Object.create({
          component_name: 'c1',
          isMaster: true
        }),
        masterComponentHosts = [
          {component: 'c1', hostName: 'h1'}
        ];
      installerStep8Controller.reopen({content: {masterComponentHosts: masterComponentHosts}});
      installerStep8Controller.assignComponentHosts(component);
      expect(component.get('component_value')).to.equal('h1');
    });
    it('component isn\'t master, 1 host', function() {
      var component = Em.Object.create({
          component_name: 'c1',
          isMaster: false
        }),
        slaveComponentHosts = [
          {componentName: 'c1', hosts: [{}]}
        ];
      installerStep8Controller.reopen({content: {slaveComponentHosts: slaveComponentHosts}});
      installerStep8Controller.assignComponentHosts(component);
      expect(component.get('component_value')).to.equal('1 host');
    });
    it('component isn\'t master, 2 hosts', function() {
      var component = Em.Object.create({
          component_name: 'c1',
          isMaster: false
        }),
        slaveComponentHosts = [
          {componentName: 'c1', hosts: [{}, {}]}
        ];
      installerStep8Controller.reopen({content: {slaveComponentHosts: slaveComponentHosts}});
      installerStep8Controller.assignComponentHosts(component);
      expect(component.get('component_value')).to.equal('2 hosts');
    });
  });

  describe('#addDynamicProperties', function() {
    it('shouldn\'t add property', function() {
      var serviceConfigProperties = [
          {name: 'templeton.hive.properties'}
        ],
        configs = [];
      installerStep8Controller.reopen({content: {serviceConfigProperties: serviceConfigProperties}});
      installerStep8Controller.addDynamicProperties(configs);
      expect(configs.length).to.equal(0);
    });
    it('should add property', function() {
      var serviceConfigProperties = [],
        configs = [];
      installerStep8Controller.reopen({content: {serviceConfigProperties: serviceConfigProperties}});
      installerStep8Controller.addDynamicProperties(configs);
      expect(configs.length).to.equal(1);
    });
  });

  describe('#formatProperties', function() {

  });

  describe('#updateConfigurations', function() {

    beforeEach(function() {
      sinon.stub(configurationController, 'doPUTClusterConfigurationSite', Em.K);
      sinon.stub(App.router, 'get', function() {
        return configurationController;
      });
    });

    afterEach(function() {
      configurationController.doPUTClusterConfigurationSite.restore();
      App.router.get.restore();
    });

    it('empty configsToUpdate', function() {
      installerStep8Controller.updateConfigurations([]);
      expect(configurationController.doPUTClusterConfigurationSite.called).to.be.false;
    });

    it('one service, no site properties', function() {
      var configsToUpdate = [
        {serviceName: 's1', id: ''},
        {serviceName: 's1', id: ''}
      ];
      installerStep8Controller.updateConfigurations(configsToUpdate);
      expect(configurationController.doPUTClusterConfigurationSite.called).to.be.false;
    });

    it('one service, site properties, 2 files', function() {
      var configsToUpdate = [
        {serviceName: 's1', id: 'site property', filename: 'f1.xml', name: 'n1', value: 'v1'},
        {serviceName: 's1', id: 'site property', filename: 'f2.xml', name: 'n2', value: 'v2'}
      ];
      installerStep8Controller.updateConfigurations(configsToUpdate);
      expect(configurationController.doPUTClusterConfigurationSite.calledTwice).to.be.true;
    });

    it('two services, site properties, 2 files', function() {
      var configsToUpdate = [
        {serviceName: 's1', id: 'site property', filename: 'f1.xml', name: 'n1', value: 'v1'},
        {serviceName: 's1', id: 'site property', filename: 'f1.xml', name: 'n12', value: 'v12'},
        {serviceName: 's1', id: 'site property', filename: 'f2.xml', name: 'n2', value: 'v2'},
        {serviceName: 's2', id: 'site property', filename: 'f2.xml', name: 'n3', value: 'v3'}
      ];
      installerStep8Controller.updateConfigurations(configsToUpdate);
      expect(configurationController.doPUTClusterConfigurationSite.calledThrice).to.be.true;
      var firstCallArgs = configurationController.doPUTClusterConfigurationSite.args[0][0];
      expect(firstCallArgs.type).to.equal('f1');
      expect(firstCallArgs.properties).to.eql({"n1":"v1","n12":"v12"});
      var secondCallArgs = configurationController.doPUTClusterConfigurationSite.args[1][0];
      expect(secondCallArgs.type).to.equal('f2');
      expect(secondCallArgs.properties).to.eql({"n2":"v2"});
      var thirdCallArgs = configurationController.doPUTClusterConfigurationSite.args[2][0];
      expect(thirdCallArgs.type).to.equal('f2');
      expect(thirdCallArgs.properties).to.eql({"n3":"v3"});
    });

  });

  describe('#loadServices', function() {

    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'assignComponentHosts', function(obj) {
        Em.set(obj, 'component_value', 'component_value');
      });
      installerStep8Controller.set('services', []);
    });

    afterEach(function() {
      installerStep8Controller.assignComponentHosts.restore();
    });

    it('no reviewService', function() {
      installerStep8Controller.set('rawContent', []);
      installerStep8Controller.loadServices();
      expect(installerStep8Controller.get('services')).to.eql([]);
    });

    it('no reviewService 2', function() {
      installerStep8Controller.set('rawContent', [{config_name: 'services'}]);
      installerStep8Controller.loadServices();
      expect(installerStep8Controller.get('services')).to.eql([]);
    });

    it('no selectedServices', function() {
      installerStep8Controller.reopen({
        selectedServices: [],
        rawContent: [{config_name: 'services', config_value: [{}]}]
      });
      installerStep8Controller.loadServices();
      expect(installerStep8Controller.get('services')).to.eql([]);
    });

    it('no intersections selectedServices and reviewService.services', function() {
      installerStep8Controller.reopen({
        selectedServices: [{serviceName: 's1'}],
        rawContent: [{config_name: 'services', config_value: [{service_name: 's2'}]}]
      });
      installerStep8Controller.loadServices();
      expect(installerStep8Controller.get('services')).to.eql([]);
    });

    it('push some services', function() {
      installerStep8Controller.reopen({
        selectedServices: [{serviceName: 's1'}],
        rawContent: [
          {
            config_name: 'services',
            config_value: [Em.Object.create({service_name: 's1', service_components: [{}]})]
          }
        ]
      });
      installerStep8Controller.loadServices();
      expect(installerStep8Controller.get('services.length')).to.eql(1);
    });

  });

  describe('#createCoreSiteObj', function() {
    Em.A([
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.o.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.o.groups'}
          ],
          globals: [
            {name: 'oozie_user', value: 'o'}
          ],
          selectedServices: [
            {serviceName: ''}
          ],
          m: 'no OOZIE',
          e: {
            excludedConfigs: ['hadoop.proxyuser.o.hosts', 'hadoop.proxyuser.o.groups'],
            includedConfigs: []
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.o.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.o.groups'}
          ],
          globals: [
            {name: 'oozie_user', value: 'o'}
          ],
          selectedServices: [
            {serviceName: 'OOZIE'}
          ],
          m: 'OOZIE exists',
          e: {
            excludedConfigs: [],
            includedConfigs: ['hadoop.proxyuser.o.hosts', 'hadoop.proxyuser.o.groups']
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.h.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.h.groups'}
          ],
          globals: [
            {name: 'hive_user', value: 'h'}
          ],
          selectedServices: [
            {serviceName: ''}
          ],
          m: 'no HIVE',
          e: {
            excludedConfigs: ['hadoop.proxyuser.h.hosts', 'hadoop.proxyuser.h.groups'],
            includedConfigs: []
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.h.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.h.groups'}
          ],
          globals: [
            {name: 'hive_user', value: 'h'}
          ],
          selectedServices: [
            {serviceName: 'HIVE'}
          ],
          m: 'HIVE exists',
          e: {
            excludedConfigs: [],
            includedConfigs: ['hadoop.proxyuser.h.hosts', 'hadoop.proxyuser.h.groups']
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.hc.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.hc.groups'}
          ],
          globals: [
            {name: 'hcat_user', value: 'hc'}
          ],
          selectedServices: [
            {serviceName: ''}
          ],
          m: 'no WEBHCAT',
          e: {
            excludedConfigs: ['hadoop.proxyuser.hc.hosts', 'hadoop.proxyuser.hc.groups'],
            includedConfigs: []
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.hc.hosts'},
            {filename: 'core-site.xml', name: 'hadoop.proxyuser.hc.groups'}
          ],
          globals: [
            {name: 'hcat_user', value: 'hc'}
          ],
          selectedServices: [
            {serviceName: 'WEBHCAT'}
          ],
          m: 'WEBHCAT exists',
          e: {
            excludedConfigs: [],
            includedConfigs: ['hadoop.proxyuser.hc.hosts', 'hadoop.proxyuser.hc.groups']
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'fs.glusterfs.c1'},
            {filename: 'core-site.xml', name: 'fs.glusterfs.c2'}
          ],
          globals: [],
          selectedServices: [
            {serviceName: ''}
          ],
          m: 'no GLUSTERFS',
          e: {
            excludedConfigs: ['fs.glusterfs.c1', 'fs.glusterfs.c2', 'fs.default.name', 'fs.defaultFS'],
            includedConfigs: []
          }
        },
        {
          configs: [
            {filename: 'core-site.xml', name: 'fs.default.name'},
            {filename: 'core-site.xml', name: 'fs.defaultFS'}
          ],
          globals: [
            {name: 'fs_glusterfs_default_name', value: 'v1'},
            {name: 'glusterfs_defaultFS_name', value: 'v2'}
          ],
          selectedServices: [
            {serviceName: 'GLUSTERFS'}
          ],
          m: 'GLUSTERFS exists',
          e: {
            excludedConfigs: [],
            includedConfigs: ['fs.default.name', 'fs.defaultFS']
          }
        },
        {
          configs: [],
          globals: [
            {name: 'fs_glusterfs_default_name', value: 'v1'},
            {name: 'glusterfs_defaultFS_name', value: 'v2'}
          ],
          selectedServices: [
            {serviceName: 'GLUSTERFS'}
          ],
          m: 'GLUSTERFS exists 2',
          e: {
            excludedConfigs: ['fs_glusterfs_default_name', 'glusterfs_defaultFS_name'],
            includedConfigs: []
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.reopen({
            globals: test.globals,
            configs: test.configs,
            selectedServices: test.selectedServices
          });
          var coreSiteObj = installerStep8Controller.createCoreSiteObj();
          expect(coreSiteObj.type).to.equal('core-site');
          expect(coreSiteObj.tag).to.equal('version1');
          var properties = Em.keys(coreSiteObj.properties);
          test.e.excludedConfigs.forEach(function (configName) {
            expect(properties.contains(configName)).to.be.false;
          });
          test.e.includedConfigs.forEach(function (configName) {
            expect(properties.contains(configName)).to.be.true;
          });
        });
      });
  });

  describe('#createGlobalSiteObj', function() {

    it('required by agent configs should be skipped', function() {
      var globals = [{isRequiredByAgent: false, name: ''}, {isRequiredByAgent: false, name: ''}];
      installerStep8Controller.reopen({globals: globals, selectedServices: []});
      var globalSiteObj = installerStep8Controller.createGlobalSiteObj();
      expect(globalSiteObj.type).to.equal('global');
      expect(globalSiteObj.tag).to.equal('version1');
      expect(Em.keys(globalSiteObj.properties)).to.eql(['gmond_user']);
    });

    it('gluster configs should be skipped', function() {
      var globals = [{isRequiredByAgent: true, name: 'fs_glusterfs.c1'}, {isRequiredByAgent: true, name: 'fs_glusterfs.c2'}];
      installerStep8Controller.reopen({globals: globals, selectedServices: [{serviceName: ''}]});
      var globalSiteObj = installerStep8Controller.createGlobalSiteObj();
      expect(globalSiteObj.type).to.equal('global');
      expect(globalSiteObj.tag).to.equal('version1');
      expect(Em.keys(globalSiteObj.properties)).to.eql(['gmond_user']);
    });

    it('_heapsize|_newsize|_maxnewsize should add m to end', function() {
      var globals = [
        {isRequiredByAgent: true, name: 'c1_heapsize', value: '1'},
        {isRequiredByAgent: true, name: 'c1_newsize', value: '2'},
        {isRequiredByAgent: true, name: 'c1_maxnewsize', value: '3'}
      ];
      installerStep8Controller.reopen({globals: globals, selectedServices: [{serviceName: ''}]});
      var globalSiteObj = installerStep8Controller.createGlobalSiteObj();
      expect(globalSiteObj.type).to.equal('global');
      expect(globalSiteObj.tag).to.equal('version1');
      globals.forEach(function(global) {
        expect(globalSiteObj.properties[global.name]).to.equal(global.value + 'm');
      });
    });

    it('for some configs should not add  m to end', function() {
      var globals = [
        {isRequiredByAgent: true, name: 'hadoop_heapsize', value: '1'},
        {isRequiredByAgent: true, name: 'yarn_heapsize', value: '2'},
        {isRequiredByAgent: true, name: 'nodemanager_heapsize', value: '3'},
        {isRequiredByAgent: true, name: 'resourcemanager_heapsize', value: '4'},
        {isRequiredByAgent: true, name: 'apptimelineserver_heapsize', value: '5'},
        {isRequiredByAgent: true, name: 'jobhistory_heapsize', value: '6'}
      ];
      installerStep8Controller.reopen({globals: globals, selectedServices: [{serviceName: ''}]});
      var globalSiteObj = installerStep8Controller.createGlobalSiteObj();
      expect(globalSiteObj.type).to.equal('global');
      expect(globalSiteObj.tag).to.equal('version1');
      globals.forEach(function(global) {
        expect(globalSiteObj.properties[global.name]).to.equal(global.value);
      });
    });

  });

  describe('#applyInstalledServicesConfigurationGroup', function() {
    beforeEach(function() {
      sinon.stub($, 'ajax', Em.K);
      sinon.stub(App.router, 'get', function() {
        return configurationController;
      });
    });
    afterEach(function() {
      $.ajax.restore();
      App.router.get.restore();
    });
    it('should do ajax request for each config group', function() {
      var configGroups = [{ConfigGroup: {id:''}}, {ConfigGroup: {id:''}}];
      installerStep8Controller.applyInstalledServicesConfigurationGroup(configGroups);
      expect($.ajax.callCount).to.equal(configGroups.length);
    });
  });

  describe('#getExistingClusterNames', function() {
    beforeEach(function() {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function() {
      App.ajax.send.restore();
    });
    it('should do ajax request', function() {
      installerStep8Controller.getExistingClusterNames();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#loadConfigs', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'loadUiSideConfigs', function(k) {return k});
      sinon.stub(App.config, 'excludeUnsupportedConfigs', function(k) {return k;});
    });
    afterEach(function() {
      installerStep8Controller.loadUiSideConfigs.restore();
      App.config.excludeUnsupportedConfigs.restore();
    });
    it('should save configs', function() {
      var serviceConfigProperties = [
        {id: 'site property', value: true, isCanBeEmpty: true},
        {id: 'site property', value: 1, isCanBeEmpty: true},
        {id: 'site property', value: '1', isCanBeEmpty: true},
        {id: 'site property', value: null, isCanBeEmpty: false}
      ];
      installerStep8Controller.reopen({content: {services: [], serviceConfigProperties: serviceConfigProperties}, configMapping: []});
      installerStep8Controller.loadConfigs();
      var configs = installerStep8Controller.get('configs');
      expect(configs.mapProperty('value')).to.eql(['true', 1, '1']);
    });
  });

  describe('#loadUiSideConfigs', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'addDynamicProperties', Em.K);
      sinon.stub(installerStep8Controller, 'getGlobConfigValueWithOverrides', function(t, v, n) {
        return {
          value: v,
          overrides: []
        }
      });
      sinon.stub(App.config, 'setConfigValue', Em.K);
    });
    afterEach(function() {
      installerStep8Controller.addDynamicProperties.restore();
      installerStep8Controller.getGlobConfigValueWithOverrides.restore();
      App.config.setConfigValue.restore();
    });

    it('all configs witohut foreignKey', function() {
      var configMapping = [
        {foreignKey: null, templateName: 't1', value: 'v1', name: 'c1', filename: 'f1'},
        {foreignKey: null, templateName: 't2', value: 'v2', name: 'c2', filename: 'f2'},
        {foreignKey: null, templateName: 't3', value: 'v3', name: 'c3', filename: 'f2'},
        {foreignKey: null, templateName: 't4', value: 'v4', name: 'c4', filename: 'f1'}
      ];
      var uiConfigs = installerStep8Controller.loadUiSideConfigs(configMapping);
      expect(uiConfigs.length).to.equal(configMapping.length);
      expect(uiConfigs.everyProperty('id', 'site property')).to.be.true;
      uiConfigs.forEach(function(c, index) {
        expect(c.overrides).to.be.an.array;
        expect(c.value).to.equal(configMapping[index].value);
        expect(c.name).to.equal(configMapping[index].name);
        expect(c.filename).to.equal(configMapping[index].filename);
      });
    });

    it('some configs witohut foreignKey', function() {
      var configMapping = [
        {foreignKey: null, templateName: 't1', value: 'v1', name: 'c1', filename: 'f1'},
        {foreignKey: null, templateName: 't2', value: 'v2', name: 'c2', filename: 'f2'},
        {foreignKey: null, templateName: 't3', value: 'v3', name: 'c3', filename: 'f2'},
        {foreignKey: null, templateName: 't4', value: 'v4', name: 'c4', filename: 'f1'},
        {foreignKey: 'fk1', templateName: 't5', value: 'v5', name: 'c5', filename: 'f1'},
        {foreignKey: 'fk2', templateName: 't6', value: 'v6', name: 'c6', filename: 'f1'},
        {foreignKey: 'fk3', templateName: 't7', value: 'v7', name: 'c7', filename: 'f2'},
        {foreignKey: 'fk4', templateName: 't8', value: 'v8', name: 'c8', filename: 'f2'}
      ];
      var uiConfigs = installerStep8Controller.loadUiSideConfigs(configMapping);
      expect(uiConfigs.length).to.equal(configMapping.length);
      expect(uiConfigs.everyProperty('id', 'site property')).to.be.true;
      uiConfigs.forEach(function(c, index) {
        if (Em.isNone(configMapping[index].foreignKey))
          expect(c.overrides).to.be.an.array;
        expect(c.value).to.equal(configMapping[index].value);
        expect(c.name).to.equal(configMapping[index].name);
        expect(c.filename).to.equal(configMapping[index].filename);
      });
    });
  });

  describe('#getGlobConfigValueWithOverrides', function() {

    it('shouldn\t do nothing', function() {
      var r = installerStep8Controller.getGlobConfigValueWithOverrides('', 'without tags', '');
      expect(r).to.eql({value: 'without tags', overrides: []});
    });

    it('should return value with empty overrides', function() {
      installerStep8Controller.set('globals', [
        {name: 'c1', value: 'v1', overrides: []}
      ]);
      var r = installerStep8Controller.getGlobConfigValueWithOverrides(['c1'], '<templateName[0]>', '');
      expect(r).to.eql({value: 'v1', overrides: []});
    });

    it('should return value with not empty overrides', function() {
      installerStep8Controller.set('globals', [
        {name: 'c1', value: 'v1', overrides: [{value: 'v2', hosts: ['h2']}]}
      ]);
      var r = installerStep8Controller.getGlobConfigValueWithOverrides(['c1'], '<templateName[0]>', '');
      expect(r).to.eql({value: 'v1', overrides: [{value: 'v2', hosts: ['h2']}]});
    });

  });

  describe('Queued requests', function() {

    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'addRequestToAjaxQueue', Em.K);
    });

    afterEach(function() {
      installerStep8Controller.addRequestToAjaxQueue.restore();
    });

    describe('#createCluster', function() {
      it('shouldn\'t add request to queue if not installerController used', function() {
        installerStep8Controller.reopen({content: {controllerName: 'addServiceController'}});
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });
      it('App.currentStackVersion should be changed if localRepo selected', function() {
        App.set('currentStackVersion', 'HDP-1.1.1');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: true}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDPLocal-1.1.1' }})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql(data);
      });
      it('App.currentStackVersion shouldn\'t be changed if localRepo ins\'t selected', function() {
        App.set('currentStackVersion', 'HDP-1.1.1');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: false}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDP-1.1.1' }})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql(data);
      });
    });

    describe('#createSelectedServices', function() {
      it('shouldn\'t do nothing if no data', function() {
        sinon.stub(installerStep8Controller, 'createSelectedServicesData', function() {return [];});
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
        installerStep8Controller.createSelectedServicesData.restore();
      });
      it('should call addRequestToAjaxQueue with computed data', function() {
        var data = [
          {"ServiceInfo": { "service_name": 's1' }},
          {"ServiceInfo": { "service_name": 's2' }},
          {"ServiceInfo": { "service_name": 's3' }}
        ];
        sinon.stub(installerStep8Controller, 'createSelectedServicesData', function() {return data;});
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql({data: JSON.stringify(data)});
        installerStep8Controller.createSelectedServicesData.restore();
      });
    });

    describe('#registerHostsToCluster', function() {
      it('shouldn\'t do nothing if no data', function() {
        sinon.stub(installerStep8Controller, 'createRegisterHostData', function() {return [];});
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
        installerStep8Controller.createRegisterHostData.restore();
      });
      it('should call addRequestToAjaxQueue with computed data', function() {
        var data = [
          {"Hosts": { "host_name": 'h1'}},
          {"Hosts": { "host_name": 'h3'}}
        ];
        sinon.stub(installerStep8Controller, 'createRegisterHostData', function() {return data;});
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql({data: JSON.stringify(data)});
        installerStep8Controller.createRegisterHostData.restore();
      });
    });

    describe('#registerHostsToComponent', function() {

      it('shouldn\'t do request if no hosts provided', function() {
        installerStep8Controller.registerHostsToComponent([]);
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('should do request if hostNames are provided', function() {
        var hostNames = ['h1', 'h2'],
          componentName = 'c1';
        installerStep8Controller.registerHostsToComponent(hostNames, componentName);
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(data.RequestInfo.query).to.equal('Hosts/host_name=h1|Hosts/host_name=h2');
        expect(data.Body.host_components[0].HostRoles.component_name).to.equal('c1');
      });

    });

    describe('#applyConfigurationsToCluster', function() {
      it('should call addRequestToAjaxQueue', function() {
        var serviceConfigTags = [
            {
              type: 'type1',
              tag: 'tag1',
              properties: [
                {},
                {}
              ]
            }
          ],
          data = '['+JSON.stringify({
            Clusters: {
              desired_config: {
                type: serviceConfigTags[0].type,
                tag: serviceConfigTags[0].tag,
                properties: serviceConfigTags[0].properties
              }
            }
          })+']';
        installerStep8Controller.reopen({serviceConfigTags: serviceConfigTags});
        installerStep8Controller.applyConfigurationsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql({data: data});
      });
    });

    describe('#applyConfigurationGroups', function() {
      it('should call addRequestToAjaxQueue', function() {
        var data = [{}, {}];
        installerStep8Controller.applyConfigurationGroups(data);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data).to.eql({data: JSON.stringify(data)});
      });
    });

    describe('#newServiceComponentErrorCallback', function() {

      it('should add request for new component', function() {
        var serviceName = 's1',
          componentName = 'c1';
        installerStep8Controller.newServiceComponentErrorCallback({}, {}, '', {}, {serviceName: serviceName, componentName: componentName});
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.serviceName).to.equal(serviceName);
        expect(data.components[0].ServiceComponentInfo.component_name).to.equal(componentName);
      });

    });

    describe('#createComponents', function() {
      beforeEach(function() {
        installerStep8Controller.reopen({
          selectedServices: [
            Em.Object.create({serviceName: 's1'}),
            Em.Object.create({serviceName: 's2'})
          ]
        });
        sinon.stub(App.StackServiceComponent, 'find', function() {
          return Em.A([
            Em.Object.create({serviceName: 's1', componentName: 'c1'}),
            Em.Object.create({serviceName: 's1', componentName: 'c2'}),
            Em.Object.create({serviceName: 's2', componentName: 'c3'}),
            Em.Object.create({serviceName: 's2', componentName: 'c4'})
          ]);
        });
      });
      afterEach(function() {
        App.StackServiceComponent.find.restore();
      });

      it('should do two requests', function() {
        installerStep8Controller.createComponents();
        expect(installerStep8Controller.addRequestToAjaxQueue.calledTwice).to.be.true;
        var firstRequestData = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(firstRequestData.components.mapProperty('ServiceComponentInfo.component_name')).to.eql(['c1', 'c2']);
        var secondRequestData = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[1][0].data.data);
        expect(secondRequestData.components.mapProperty('ServiceComponentInfo.component_name')).to.eql(['c3', 'c4']);
      });

      it('should check App_TIMELINE_SERVER', function() {
        sinon.stub(App, 'get', function(k) {
          if ('isHadoop21Stack' === k) return true;
          if ('testMode' === k) return false;
          return Em.get(App, k);
        });
        sinon.stub(App.YARNService, 'find', function() {return [{}]});
        sinon.stub(App.ajax, 'send', Em.K);
        installerStep8Controller.set('content', {controllerName: 'addServiceController'});

        installerStep8Controller.createComponents();
        expect(App.ajax.send.calledOnce).to.equal(true);
        expect(App.ajax.send.args[0][0].data.serviceName).to.equal('YARN');
        expect(App.ajax.send.args[0][0].data.componentName).to.equal('APP_TIMELINE_SERVER');

        App.ajax.send.restore();
        App.get.restore();
        App.YARNService.find.restore();
      });

    });

    describe('#setLocalRepositories', function() {

      it('shouldn\'t do nothing', function () {
        installerStep8Controller.set('content', {controllerName: 'addServiceController'});
        sinon.stub(App, 'get', function (k) {
          if ('supports.localRepositories' === k) return false;
          return Em.get(App, k);
        });
        expect(installerStep8Controller.setLocalRepositories()).to.equal(false);
        App.get.restore();
      });

      it('shouldn\'t do requests', function() {
        installerStep8Controller.set('content', {
          controllerName: 'installerController',
          stacks: [
            {
              isSelected: true,
              operatingSystems: [
                {baseUrl: 'u1', originalBaseUrl: 'u1'},
                {baseUrl: 'u2', originalBaseUrl: 'u2'}
              ]
            }
          ]
        });
        installerStep8Controller.setLocalRepositories();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('should do 2 requests', function() {
        installerStep8Controller.set('content', {
          controllerName: 'installerController',
          stacks: [
            {
              isSelected: true,
              operatingSystems: [
                {baseUrl: 'new_u1', originalBaseUrl: 'u1', osType: 'o1', repoId: 'r1'},
                {baseUrl: 'new_u2', originalBaseUrl: 'u2', osType: 'o2', repoId: 'r2'}
              ]
            }
          ]
        });
        installerStep8Controller.setLocalRepositories();
        expect(installerStep8Controller.addRequestToAjaxQueue.calledTwice).to.equal(true);
        var firstRequestData = installerStep8Controller.addRequestToAjaxQueue.args[0][0].data;
        expect(firstRequestData.osType).to.equal('o1');
        expect(firstRequestData.repoId).to.equal('r1');
        expect(JSON.parse(firstRequestData.data).Repositories.base_url).to.equal('new_u1');

        var secondRequestData = installerStep8Controller.addRequestToAjaxQueue.args[1][0].data;
        expect(secondRequestData.osType).to.equal('o2');
        expect(secondRequestData.repoId).to.equal('r2');
        expect(JSON.parse(secondRequestData.data).Repositories.base_url).to.equal('new_u2');
      });

    });

    describe('#createMasterHostComponents', function() {
      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });
      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });
      it('should create components', function() {
        var masterComponentHosts = [
          {component: 'c1', isInstalled: false, hostName: 'h1'},
          {component: 'c1', isInstalled: true, hostName: 'h2'},
          {component: 'c2', isInstalled: false, hostName: 'h1'},
          {component: 'c2', isInstalled: false, hostName: 'h2'}
        ];
        installerStep8Controller.set('content', {masterComponentHosts: masterComponentHosts});
        installerStep8Controller.createMasterHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledTwice).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('c1');
        expect(installerStep8Controller.registerHostsToComponent.args[1][0]).to.eql(['h1', 'h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[1][1]).to.equal('c2');
      });
    });

    describe('#createAdditionalHostComponents', function() {

      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });

      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });

      it('should add GANGLIA MONITOR (1)', function() {
        installerStep8Controller.reopen({
          getRegisteredHosts: function() {
            return [{hostName: 'h1'}, {hostName: 'h2'}];
          },
          content: {
            services: [
              Em.Object.create({serviceName: 'GANGLIA', isSelected: true, isInstalled: false})
            ]
          }
        });
        installerStep8Controller.createAdditionalHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
      });

      it('should add GANGLIA MONITOR (2)', function() {
        installerStep8Controller.reopen({
          getRegisteredHosts: function() {
            return [{hostName: 'h1', isInstalled: true}, {hostName: 'h2', isInstalled: false}];
          },
          content: {
            services: [
              Em.Object.create({serviceName: 'GANGLIA', isSelected: true, isInstalled: true})
            ]
          }
        });
        installerStep8Controller.createAdditionalHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
      });

      it('should add MYSQL_SERVER', function() {
        installerStep8Controller.reopen({
          getRegisteredHosts: function() {
            return [{hostName: 'h1'}, {hostName: 'h2'}];
          },
          content: {
            masterComponentHosts: [
              {component: 'HIVE_SERVER', hostName: 'h1'},
              {component: 'HIVE_SERVER', hostName: 'h2'}
            ],
            services: [
              Em.Object.create({serviceName: 'HIVE', isSelected: true, isInstalled: false})
            ],
            serviceConfigProperties: [
              {name: 'hive_database', value: 'New MySQL Database'}
            ]
          }
        });
        installerStep8Controller.createAdditionalHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('MYSQL_SERVER');
      });

    });

    describe('#createSlaveAndClientsHostComponents', function() {

      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });

      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });

      it('each slave is not CLIENT', function() {
        installerStep8Controller.reopen({
          content: {
            slaveComponentHosts: [
              {componentName: 'c1', hosts: [{isInstalled: true, hostName: 'h1'}, {isInstalled: false, hostName: 'h2'}, {isInstalled: false, hostName: 'h3'}]}
            ]
          }
        });
        installerStep8Controller.createSlaveAndClientsHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.be.true;
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h2', 'h3']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('c1');
      });

      var clients = Em.A([
        {
          component_name: 'HDFS_CLIENT',
          masterComponentHosts: [
            {component: 'HBASE_MASTER', isInstalled: false, hostName: 'h1'},
            {component: 'HBASE_MASTER', isInstalled: true, hostName: 'h2'},
            {component: 'HBASE_REGIONSERVER', isInstalled: false, hostName: 'h3'},
            {component: 'WEBHCAT_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'HISTORYSERVER', isInstalled: false, hostName: 'h3'},
            {component: 'OOZIE_SERVER', isInstalled: true, hostName: 'h4'}
          ],
          e: ['h1', 'h3']
        },
        {
          component_name: 'MAPREDUCE_CLIENT',
          masterComponentHosts: [
            {component: 'HIVE_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'WEBHCAT_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'NAGIOS_SERVER', isInstalled: false, hostName: 'h2'},
            {component: 'OOZIE_SERVER', isInstalled: true, hostName: 'h3'}
          ],
          e: ['h1', 'h2']
        },
        {
          component_name: 'OOZIE_CLIENT',
          masterComponentHosts: [
            {component: 'NAGIOS_SERVER', isInstalled: false, hostName: 'h2'}
          ],
          e: ['h2']
        },
        {
          component_name: 'ZOOKEEPER_CLIENT',
          masterComponentHosts: [
            {component: 'WEBHCAT_SERVER', isInstalled: false, hostName: 'h1'}
          ],
          e: ['h1']
        },
        {
          component_name: 'HIVE_CLIENT',
          masterComponentHosts: [
            {component: 'WEBHCAT_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'HIVE_SERVER', isInstalled: false, hostName: 'h1'}
          ],
          e: ['h1']
        },
        {
          component_name: 'HCAT',
          masterComponentHosts: [
            {component: 'NAGIOS_SERVER', isInstalled: false, hostName: 'h1'}
          ],
          e: ['h1']
        },
        {
          component_name: 'YARN_CLIENT',
          masterComponentHosts: [
            {component: 'NAGIOS_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'HIVE_SERVER', isInstalled: false, hostName: 'h2'},
            {component: 'OOZIE_SERVER', isInstalled: false, hostName: 'h3'},
            {component: 'WEBHCAT_SERVER', isInstalled: true, hostName: 'h1'}
          ],
          e: ['h1', 'h2', 'h3']
        },
        {
          component_name: 'TEZ_CLIENT',
          masterComponentHosts: [
            {component: 'NAGIOS_SERVER', isInstalled: false, hostName: 'h1'},
            {component: 'HIVE_SERVER', isInstalled: false, hostName: 'h2'}
          ],
          e: ['h1', 'h2']
        }
      ]);

      clients.forEach(function(test) {
        it('slave is CLIENT (isInstalled false) ' + test.component_name, function() {
          installerStep8Controller.reopen({
            content: {
              clients: [
                {isInstalled: false, component_name: test.component_name}
              ],
              slaveComponentHosts: [
                {componentName: 'CLIENT', hosts: []}
              ],
              masterComponentHosts: test.masterComponentHosts
            }
          });
          installerStep8Controller.createSlaveAndClientsHostComponents();
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.be.true;
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(test.e);
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal(test.component_name);
        });
      });

      clients.forEach(function(test) {
        it('slave is CLIENT (isInstalled true, h1 - host to be excluded) ' + test.component_name, function() {
          sinon.stub(App.HostComponent, 'find', function() {
            return [
              {componentName: test.component_name, workStatus: 'INSTALLED', host: {hostName: 'h1'}}
            ];
          });
          installerStep8Controller.reopen({
            content: {
              clients: [
                {isInstalled: true, component_name: test.component_name}
              ],
              slaveComponentHosts: [
                {componentName: 'CLIENT', hosts: []}
              ],
              masterComponentHosts: test.masterComponentHosts
            }
          });
          installerStep8Controller.createSlaveAndClientsHostComponents();

          App.HostComponent.find.restore();
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.be.true;
          // Don't know why, but
          // expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(test.e.without('h1'));
          // doesn't work
          expect(JSON.stringify(installerStep8Controller.registerHostsToComponent.args[0][0])).to.equal(JSON.stringify(test.e.without('h1')));
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal(test.component_name);
        });
      });

    });

  });

});
