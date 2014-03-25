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
require('utils/ajax/ajax_queue');
require('controllers/wizard/step8_controller');

var installerStep8Controller;

describe('App.WizardStep8Controller', function() {

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
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p2', value: 'v2'})
  ]);

  beforeEach(function() {
    installerStep8Controller = App.WizardStep8Controller.create({
      configs: configs
    });
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
    {name: 'createFalconStartupSiteObj', e: {type: 'falcon-startup.properties', tag: 'version1', l: 2}},
    {name: 'createFalconRuntimeSiteObj', e: {type: 'falcon-runtime.properties', tag: 'version1', l: 2}}
  ]);

  siteObjTests.forEach(function(test) {
    describe('#' + test.name, function() {

      it(test.name, function() {

        var siteObj = installerStep8Controller.createSiteObj(test.e.type);
        expect(siteObj.tag).to.equal(test.e.tag);
        expect(Em.keys(siteObj.properties).length).to.equal(test.e.l);
      });

    });
  });

  describe('#createConfigurations', function() {

    it('verify if its installerController', function() {
      installerStep8Controller.set('content', {controllerName: 'installerController', services: Em.A([])});
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(4);
      installerStep8Controller.clearStep();
    });

    it('verify if its not installerController', function() {
      installerStep8Controller.set('content', {controllerName: 'addServiceController', services: Em.A([])});
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(2);
      installerStep8Controller.clearStep();
    });

    it('verify not App.supports.capacitySchedulerUi', function() {
      installerStep8Controller = App.WizardStep8Controller.create({
        content: {controllerName: 'addServiceController', services: Em.A([{isSelected:true,isInstalled:false,serviceName:'MAPREDUCE'}])},
        configs: configs
      });
      App.set('supports.capacitySchedulerUi', false);
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(4);
      installerStep8Controller.clearStep();
    });

    it('verify App.supports.capacitySchedulerUi', function() {
      installerStep8Controller = App.WizardStep8Controller.create({
        content: {controllerName: 'addServiceController', services: Em.A([{isSelected:true,isInstalled:false,serviceName:'MAPREDUCE'}])},
        configs: configs
      });
      App.set('supports.capacitySchedulerUi', true);
      installerStep8Controller.createConfigurations();
      expect(installerStep8Controller.get('serviceConfigTags').length).to.equal(6);
      installerStep8Controller.clearStep();
    });


    // e - without global and core!
    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']),e: 2},
      {selectedServices: Em.A(['MAPREDUCE2','YARN']),e: 5},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE']),e: 7},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE']),e: 9},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE']),e: 12},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT']),e: 13},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE']),e: 14},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG']),e: 15},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON']),e: 17},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM']),e: 18},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM','TEZ']),e: 19},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM','TEZ','ZOOKEEPER']),e: 21}

    ]);

    tests.forEach(function(test) {
      it(test.selectedServices.join(','), function() {
        var services = test.selectedServices.map(function(serviceName) {
          return Em.Object.create({isSelected:true,isInstalled:false,serviceName:serviceName});
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
    it('escape xml character for installer wizard', function() {
      var services = Em.A([Em.Object.create({isSelected:true,isInstalled:false,serviceName:'OOZIE'}),
        Em.Object.create({isSelected:true,isInstalled:false,serviceName:'FALCON'})]);

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
      var nonXmlConfigTypes = ['oozie-log4j','falcon-startup.properties','falcon-runtime.properties'];
      nonXmlConfigTypes.forEach(function(_nonXmlConfigType){
        var nonXmlConfigTypeObj = installerStep8Controller.get('serviceConfigTags').findProperty('type',_nonXmlConfigType);
        var nonXmlSitePropertyVal = nonXmlConfigTypeObj.properties['p1'];
        expect(nonXmlSitePropertyVal).to.equal("'.'v1");
      });
      installerStep8Controller.clearStep();
    });

  });

  describe('#createSelectedServicesData', function() {

    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']),e: 2},
      {selectedServices: Em.A(['MAPREDUCE2','YARN']),e: 5},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE']),e: 7},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE']),e: 9},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE']),e: 12},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT']),e: 13},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE']),e: 14},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG']),e: 15},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON']),e: 17},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM']),e: 18},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM','TEZ']),e: 19},
      {selectedServices: Em.A(['MAPREDUCE2','YARN','HBASE','OOZIE','HIVE','WEBHCAT','HUE','PIG','FALCON','STORM','TEZ','ZOOKEEPER']),e: 21}
    ]);

    tests.forEach(function(test) {
      it(test.selectedServices.join(','), function() {
        var services = test.selectedServices.map(function(serviceName) {
          return Em.Object.create({isSelected:true,isInstalled:false,serviceName:serviceName});
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

  describe('#getRegisteredHosts', function() {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'REGISTERED',name:'h1'}),
          h2: Em.Object.create({bootStatus:'OTHER',name:'h2'})
        },
        e: ['h1'],
        m: 'Two hosts, one registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'OTHER',name:'h1'}),
          h2: Em.Object.create({bootStatus:'OTHER',name:'h2'})
        },
        e: [],
        m: 'Two hosts, zero registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'REGISTERED',name:'h1'}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2'})
        },
        e: ['h1','h2'],
        m: 'Two hosts, two registered'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHosts = installerStep8Controller.getRegisteredHosts();
        expect(registeredHosts.mapProperty('hostName').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#createRegisterHostData', function() {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'REGISTERED',name:'h1',isInstalled:false}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2',isInstalled:false})
        },
        e: ['h1', 'h2'],
        m: 'two registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'OTHER',name:'h1',isInstalled:false}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2',isInstalled:false})
        },
        e: ['h2'],
        m: 'one registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'OTHER',name:'h1',isInstalled:true}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2',isInstalled:false})
        },
        e: ['h2'],
        m: 'one registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'REGISTERED',name:'h1',isInstalled:true}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2',isInstalled:false})
        },
        e: ['h2'],
        m: 'two registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'OTHER',name:'h1',isInstalled:false}),
          h2: Em.Object.create({bootStatus:'OTHER',name:'h2',isInstalled:false})
        },
        e: [],
        m: 'zero registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus:'REGISTERED',name:'h1',isInstalled:true}),
          h2: Em.Object.create({bootStatus:'REGISTERED',name:'h2',isInstalled:true})
        },
        e: [],
        m: 'two registered, zeto insInstalled false'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHostData = installerStep8Controller.createRegisterHostData();
        expect(registeredHostData.mapProperty('Hosts.host_name').toArray()).to.eql(test.e);
      });
    });

  });

});
