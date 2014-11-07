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

        var siteObj = installerStep8Controller.createSiteObj(test.e.type, test.e.tag);
        expect(siteObj.tag).to.equal(test.e.tag);
        expect(Em.keys(siteObj.properties).length).to.equal(test.e.l);
      });

    });
  });

  describe('#createSelectedServicesData', function () {

    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']), e: 2},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN']), e: 5},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE']), e: 7},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE']), e: 9},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 12},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 13},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE']), e: 14},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG']), e: 15},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON']), e: 17},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM']), e: 18},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ']), e: 19},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ', 'ZOOKEEPER']), e: 21}
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
      sinon.stub(installerStep8Controller, 'loadConfigs', Em.K);
      sinon.stub(installerStep8Controller, 'loadClusterInfo', Em.K);
      sinon.stub(installerStep8Controller, 'loadServices', Em.K);
      installerStep8Controller.set('content', {controllerName: 'installerController'});
    });
    afterEach(function () {
      installerStep8Controller.clearStep.restore();
      installerStep8Controller.formatProperties.restore();
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
  });

  describe('#removeHiveConfigs', function () {
    Em.A([
        {
          globals: [
            {name: 'hive_database', value: 'New MySQL Database'},
            {name: 'hive_database_type', value: 'mysql'},
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
            {name: 'hive_database_type', value: 'mysql'},
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
            {name: 'hive_database_type', value: 'postgresql'},
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
            {name: 'hive_database_type', value: 'oracle'},
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
            {name: 'oozie_database_type', value: 'derby'},
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
            {name: 'oozie_database_type', value: 'mysql'},
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
            {name: 'oozie_database_type', value: 'postgresql'},
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
            {name: 'oozie_database_type', value: 'oracle'},
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
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'only redhat5',
          e: {
            base_url: ['url1'],
            os_type: ['redhat5']
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
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6',
          e: {
            base_url: ['url1', 'url2'],
            os_type: ['redhat5', 'redhat6']
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
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'sles11',
                    base_url: 'url3'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6, sles11',
          e: {
            base_url: ['url1', 'url2', 'url3'],
            os_type: ['redhat5', 'redhat6', 'sles11']
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
            {name: 'hive_database', value: 'New PostgreSQL Database'}
          ],
          m: 'New PostgreSQL Database',
          e: 'Postgres (New Database)'
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
          var dbComponent = installerStep8Controller.loadHiveDbValue();
          expect(dbComponent).to.equal(test.e);
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
          var dbComponent = installerStep8Controller.loadOozieDbValue();
          expect(dbComponent).to.equal(test.e);
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
      var nagiosAdmin = installerStep8Controller.loadNagiosAdminValue();
      expect(nagiosAdmin).to.equal('admin / (admin@admin.com)');
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
        expect(App.ajax.send.getCall(i).args[0].data).to.eql({name: n, isLast: i == clusterNames.length - 1});
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


  describe('#createStormSiteObj', function() {
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
      expect(installerStep8Controller.createStormSiteObj('version1')).to.eql(expected);
    });

    it('should not escape special characters', function() {
      var configs = [
          {filename: 'storm-site.xml', value: "abc\n\t", name: 'nimbus.childopts'},
          {filename: 'storm-site.xml', value: "a\nb", name: 'supervisor.childopts'},
          {filename: 'storm-site.xml', value: "a\t\tb", name: 'worker.childopts'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            'nimbus.childopts': 'abc\n\t',
            'supervisor.childopts': 'a\nb',
            'worker.childopts': 'a\t\tb'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj('version1')).to.eql(expected);
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
              type: 'hdfs',
              tag: 'tag1',
              properties: [
                {},
                {}
              ]
            }
          ],
          data = '['+JSON.stringify({
            Clusters: {
              desired_config: [serviceConfigTags[0]]
            }
          })+']';
        installerStep8Controller.reopen({
          installedServices: [
              Em.Object.create({
                isSelected: true,
                isInstalled: false,
                configTypesRendered: {hdfs:'tag1'}
              })
            ], selectedServices: []
        });
        installerStep8Controller.applyConfigurationsToCluster(serviceConfigTags);
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

      var newDatabases = [
        {name: 'New MySQL Database',
         component: 'MYSQL_SERVER'
        },
        {name: 'New PostgreSQL Database',
          component: 'POSTGRESQL_SERVER'
        },
      ];

      newDatabases.forEach(function (db) {
        it('should add {0}'.format(db.component), function() {
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
                {name: 'hive_database', value: db.name}
              ]
            }
          });
          installerStep8Controller.createAdditionalHostComponents();
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal(db.component);
        });

      });

    });


  });

  describe("#resolveProxyuserDependecies()", function() {
    it("No core-site configs", function() {
      expect(installerStep8Controller.resolveProxyuserDependecies([], [])).to.be.empty;
    });
    it("Only proxyuser group config", function() {
      var configs = [{
        name: 'proxyuser_group'
      }];
      installerStep8Controller.set('configs', [{
        name: 'proxyuser_group',
        value: 'val1'
      }]);
      expect(installerStep8Controller.resolveProxyuserDependecies(configs, [])).to.be.empty;
    });
    it("Property should be added", function() {
      var configs = [
        {
          name: 'proxyuser_group'
        },
        {
          name: 'hadoop.proxyuser.user.hosts',
          value: 'val2'
        }
      ];
      installerStep8Controller.set('configs', [{
        name: 'proxyuser_group',
        value: 'val1'
      }]);
      expect(installerStep8Controller.resolveProxyuserDependecies(configs, [])).to.be.eql({
        'hadoop.proxyuser.user.hosts': 'val2',
        'proxyuser_group': 'val1'
      });
    });
    it("Property should be skipped", function() {
      var configs = [
        {
          name: 'proxyuser_group'
        },
        {
          name: 'hadoop.proxyuser.user.hosts',
          value: 'val2'
        }
      ];
      installerStep8Controller.set('configs', [
        {
          name: 'proxyuser_group',
          value: 'val1'
        },
        {
          name: 'user1',
          value: 'user'
        }
      ]);
      installerStep8Controller.set('optionalCoreSiteConfigs', [
        {
          serviceName: 'S1',
          user: 'user1'
        }
      ]);
      expect(installerStep8Controller.resolveProxyuserDependecies(configs, [])).to.be.empty;
    });
  });

});
