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
require('controllers/main/service/reassign/step3_controller');
require('controllers/main/service/reassign_controller');
var testHelpers = require('test/helpers');
var controller;

describe('App.ReassignMasterWizardStep3Controller', function () {

  beforeEach(function(){
    controller = App.ReassignMasterWizardStep3Controller.create({
      content: Em.Object.create()
    });
  });

  describe("#submit()", function() {
    var mock = {
      getKDCSessionState: function (callback) {
        callback();
      }
    };
    beforeEach(function () {
      sinon.stub(App, 'get').returns(mock);
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App.router, 'send', Em.K);
      controller.submit();
    });
    afterEach(function () {
      App.get.restore();
      mock.getKDCSessionState.restore();
      App.router.send.restore();
    });
    it('getKDCSessionState is called once', function () {
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
    });
    it('User is moved to the next step', function () {
      expect(App.router.send.calledWith("next")).to.be.true;
    });
  });

  describe('#setAdditionalConfigs()', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isHaEnabled').returns(true);
    });

    afterEach(function () {
      App.get.restore();
    });

    it('Component is absent', function () {
      controller.set('additionalConfigsMap', []);
      var configs = {};

      expect(controller.setAdditionalConfigs(configs, 'COMP1', '')).to.be.false;
      expect(configs).to.eql({});
    });

    it('configs for Hadoop 2 is present', function () {
      controller.set('additionalConfigsMap', [
        {
          componentName: 'COMP1',
          configs: {
            'test-site': {
              'property1': '<replace-value>:1111'
            }
          },
          configs_Hadoop2: {
            'test-site': {
              'property2': '<replace-value>:2222'
            }
          }
        }
      ]);
      var configs = {
        'test-site': {}
      };

      expect(controller.setAdditionalConfigs(configs, 'COMP1', 'host1')).to.be.true;
      expect(configs).to.eql({
        'test-site': {
          'property2': 'host1:2222'
        }
      });
    });

    it('ignore some configs for NameNode after HA', function () {
      controller.set('additionalConfigsMap', [
        {
          componentName: 'NAMENODE',
          configs: {
            'test-site': {
              'fs.defaultFS': '<replace-value>:1111',
              'dfs.namenode.rpc-address': '<replace-value>:1111'
            }
          }
        }
      ]);
      var configs = {'test-site': {}};

      expect(controller.setAdditionalConfigs(configs, 'NAMENODE', 'host1')).to.be.true;
      expect(configs).to.eql({'test-site': {}});
    });
  });

  describe('#getConfigUrlParams()', function () {
    var testCases = [
      {
        componentName: 'NAMENODE',
        result: [
          "(type=hdfs-site&tag=1)",
          "(type=core-site&tag=2)"
        ]
      },
      {
        componentName: 'SECONDARY_NAMENODE',
        result: [
          "(type=hdfs-site&tag=1)",
          "(type=core-site&tag=2)"
        ]
      },
      {
        componentName: 'JOBTRACKER',
        result: [
          "(type=mapred-site&tag=4)"
        ]
      },
      {
        componentName: 'RESOURCEMANAGER',
        result: [
          "(type=yarn-site&tag=5)"
        ]
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        result: [
          "(type=yarn-site&tag=5)",
          "(type=yarn-env&tag=8)"
        ]
      },
      {
        componentName: 'OOZIE_SERVER',
        result: [
          "(type=oozie-site&tag=6)",
          "(type=core-site&tag=2)",
          "(type=oozie-env&tag=2)"
        ]
      },
      {
        componentName: 'WEBHCAT_SERVER',
        result: [
          "(type=hive-env&tag=11)",
          "(type=webhcat-site&tag=7)",
          "(type=core-site&tag=2)"
        ]
      },
      {
        componentName: 'HIVE_SERVER',
        result: [
          '(type=hive-site&tag=10)',
          '(type=webhcat-site&tag=7)',
          '(type=hive-env&tag=11)',
          '(type=core-site&tag=2)'
        ]
      },
      {
        componentName: 'HIVE_METASTORE',
        result: [
          '(type=hive-site&tag=10)',
          '(type=webhcat-site&tag=7)',
          '(type=hive-env&tag=11)',
          '(type=core-site&tag=2)'
        ]
      },
      {
        componentName: 'MYSQL_SERVER',
        result: [
          '(type=hive-site&tag=10)'
        ]
      },
      {
        componentName: 'HISTORYSERVER',
        result: [
          '(type=mapred-site&tag=4)'
        ]
      }
    ];

    var data = {
      Clusters: {
        desired_configs: {
          'hdfs-site': {tag: 1},
          'core-site': {tag: 2},
          'hbase-site': {tag: 3},
          'mapred-site': {tag: 4},
          'yarn-site': {tag: 5},
          'oozie-site': {tag: 6},
          'oozie-env': {tag: 2},
          'webhcat-site': {tag: 7},
          'yarn-env': {tag: 8},
          'accumulo-site': {tag: 9},
          'hive-site': {tag: 10},
          'hive-env': {tag: 11}
        }
      }
    };

    var services = [];

    beforeEach(function () {
      controller.set('wizardController', App.get('router.reassignMasterController'));
      sinon.stub(App.Service, 'find', function () {
        return services;
      });
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    testCases.forEach(function (test) {
      it('get config of ' + test.componentName, function () {
        expect(controller.getConfigUrlParams(test.componentName, data)).to.eql(test.result);
      });
    });
    it('get config of NAMENODE when HBASE installed', function () {
      services = [
        {
          serviceName: 'HBASE'
        }
      ];
      expect(controller.getConfigUrlParams('NAMENODE', data)).to.eql([
        "(type=hdfs-site&tag=1)",
        "(type=core-site&tag=2)",
        "(type=hbase-site&tag=3)"
      ]);
    });

    it('get config of NAMENODE when ACCUMULO installed', function () {
      services = [
        {
          serviceName: 'ACCUMULO'
        }
      ];
      expect(controller.getConfigUrlParams('NAMENODE', data)).to.eql([
        "(type=hdfs-site&tag=1)",
        "(type=core-site&tag=2)",
        "(type=accumulo-site&tag=9)"
      ]);
    });

  });

  describe('#onLoadConfigsTags()', function () {
    var dummyData = {
      Clusters: {
        desired_configs : {}
      }
    };

    beforeEach(function () {
      sinon.stub(controller, 'getConfigUrlParams', function () {
        return [];
      });
      controller.set('content', {
        reassign: {
          component_name: 'COMP1'
        }
      });
      controller.onLoadConfigsTags(dummyData);
      this.args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
    });

    afterEach(function () {
      controller.getConfigUrlParams.restore();
    });

    it('request is sent', function () {
      expect(this.args).exists;
    });

    it('getConfigUrlParams is called with correct data', function () {
      expect(controller.getConfigUrlParams.calledWith('COMP1', dummyData)).to.be.true;
    });
  });

  describe('#setSecureConfigs()', function () {

    beforeEach(function () {
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      Em.tryInvoke(App.get, 'restore');
    });

    it('undefined component and security disabled', function () {
      var secureConfigs = [];
      this.stub.withArgs('isKerberosEnabled').returns(false);
      controller.set('secureConfigsMap', []);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
    });

    it('component exist and security disabled', function () {
      var secureConfigs = [];
      this.stub.withArgs('isKerberosEnabled').returns(false);
      controller.set('secureConfigsMap', [{
        componentName: 'COMP1'
      }]);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
    });

    it('undefined component and security enabled', function () {
      var secureConfigs = [];
      this.stub.withArgs('isKerberosEnabled').returns(true);
      controller.set('secureConfigsMap', []);
      expect(controller.setSecureConfigs(secureConfigs, {}, 'COMP1')).to.be.false;
      expect(secureConfigs).to.eql([]);
    });
    it('component exist and security enabled', function () {
      var secureConfigs = [];
      this.stub.withArgs('isKerberosEnabled').returns(true);
      var configs = {'s1': {
        'k1': 'kValue',
        'p1': 'pValue'
      }};
      controller.set('secureConfigsMap', [{
        componentName: 'COMP1',
        configs: [{
          site: 's1',
          keytab: 'k1',
          principal: 'p1'
        }]
      }]);
      expect(controller.setSecureConfigs(secureConfigs, configs, 'COMP1')).to.be.true;
      expect(secureConfigs).to.eql([
        {
          "keytab": "kValue",
          "principal": "pValue"
        }
      ]);
    });
  });

  describe('#setDynamicCinfigs()', function () {

    describe('HIVE', function() {
      beforeEach(function () {
        controller.set('content', Em.Object.create({
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'host1'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'host3'
            },
            {
              component: 'HIVE_SERVER',
              hostName: 'host4'
            }
          ],
          reassignHosts: {
            source: 'host1',
            target: 'host2'
          }
        }));
      });
      it("reassign component is HIVE_METASTORE", function() {
        var configs = {
          'hive-env': {
            'hive_user': 'hive_user'
          },
          'hive-site': {
            'hive.metastore.uris': ''
          },
          'webhcat-site': {
            'templeton.hive.properties': 'thrift'
          },
          'core-site': {
            'hadoop.proxyuser.hive_user.hosts': ''
          }
        };
        App.MoveHmConfigInitializer.setup(controller._getHiveInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveHmConfigInitializer);
        expect(configs['hive-site']['hive.metastore.uris']).to.equal('thrift://host3:9083,thrift://host2:9083');
        expect(configs['webhcat-site']['templeton.hive.properties']).to.equal('thrift');
        expect(configs['core-site']['hadoop.proxyuser.hive_user.hosts']).to.equal('host2,host3,host4');
      });

      it("reassign component is HIVE_SERVER", function() {
        controller.get('content.masterComponentHosts').pushObject({component: 'HIVE_SERVER', hostName: 'host1'});
        var configs = {
          'hive-env': {
            'hive_user': 'hive_user'
          },
          'hive-site': {
            'hive.metastore.uris': ''
          },
          'webhcat-site': {
            'templeton.hive.properties': 'thrift'
          },
          'core-site': {
            'hadoop.proxyuser.hive_user.hosts': ''
          }
        };
        App.MoveHsConfigInitializer.setup(controller._getHiveInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveHsConfigInitializer);
        expect(configs['core-site']['hadoop.proxyuser.hive_user.hosts']).to.equal('host1,host2,host3,host4');
      });

      it("reassign component is WEBHCAT_SERVER", function() {
        controller.get('content.masterComponentHosts').pushObject({component: 'WEBHCAT_SERVER', hostName: 'host1'});
        var configs = {
          'hive-env': {
            'webhcat_user': 'webhcat_user'
          },
          'hive-site': {
            'hive.metastore.uris': ''
          },
          'webhcat-site': {
            'templeton.hive.properties': 'thrift'
          },
          'core-site': {
            'hadoop.proxyuser.webhcat_user.hosts': ''
          }
        };
        App.MoveWsConfigInitializer.setup(controller._getWsInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveWsConfigInitializer);
        expect(configs['core-site']['hadoop.proxyuser.webhcat_user.hosts']).to.equal('host2');
      });
    });

    describe('RESOURCEMANAGER', function () {
      beforeEach(function () {
        sinon.stub(App, 'get').withArgs('isRMHaEnabled').returns(true);
      });
      afterEach(function () {
        App.get.restore();
        App.MoveRmConfigInitializer.cleanup();
      });

      it('HA enabled and resource manager 1', function () {
        controller.set('content', Em.Object.create({
          reassignHosts: {
            source: 'host1',
            target: 'host3'
          }
        }));
        var configs = {
          'yarn-site': {
            'yarn.resourcemanager.hostname.rm1': 'host1',
            'yarn.resourcemanager.webapp.address.rm1': 'host1:8088',
            'yarn.resourcemanager.webapp.https.address.rm1': 'host1:8443',
            'yarn.resourcemanager.hostname.rm2': 'host2',
            'yarn.resourcemanager.webapp.address.rm2': 'host2:8088',
            'yarn.resourcemanager.webapp.https.address.rm2': 'host2:8443'
          }
        };
        var additionalDependencies = controller._getRmAdditionalDependencies(configs);
        App.MoveRmConfigInitializer.setup(controller._getRmInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveRmConfigInitializer, additionalDependencies);
        expect(configs['yarn-site']).to.eql({
          'yarn.resourcemanager.hostname.rm1': 'host3',
          'yarn.resourcemanager.webapp.address.rm1': 'host3:8088',
          'yarn.resourcemanager.webapp.https.address.rm1': 'host3:8443',
          'yarn.resourcemanager.hostname.rm2': 'host2',
          'yarn.resourcemanager.webapp.address.rm2': 'host2:8088',
          'yarn.resourcemanager.webapp.https.address.rm2': 'host2:8443'
        });
      });

      it('HA enabled and resource manager 2', function () {
        controller.set('content', Em.Object.create({
          reassignHosts: {
            source: 'host2',
            target: 'host3'
          }
        }));
        var configs = {
          'yarn-site': {
            'yarn.resourcemanager.hostname.rm1': 'host1',
            'yarn.resourcemanager.webapp.address.rm1': 'host1:8088',
            'yarn.resourcemanager.webapp.https.address.rm1': 'host1:8443',
            'yarn.resourcemanager.hostname.rm2': 'host2',
            'yarn.resourcemanager.webapp.address.rm2': 'host2:8088',
            'yarn.resourcemanager.webapp.https.address.rm2': 'host2:8443'
          }
        };
        var additionalDependencies = controller._getRmAdditionalDependencies(configs);
        App.MoveRmConfigInitializer.setup(controller._getRmInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveRmConfigInitializer, additionalDependencies);

        expect(configs['yarn-site']).to.eql({
          'yarn.resourcemanager.hostname.rm1': 'host1',
          'yarn.resourcemanager.webapp.address.rm1': 'host1:8088',
          'yarn.resourcemanager.webapp.https.address.rm1': 'host1:8443',
          'yarn.resourcemanager.hostname.rm2': 'host3',
          'yarn.resourcemanager.webapp.address.rm2': 'host3:8088',
          'yarn.resourcemanager.webapp.https.address.rm2': 'host3:8443'
        });
      });
    });

    describe('NAMENODE', function () {
      var isHaEnabled = false;

      beforeEach(function () {
        sinon.stub(App, 'get', function () {
          return isHaEnabled;
        });
        sinon.stub(App.Service, 'find', function () {
          return [
            {serviceName: 'HDFS'},
            {serviceName: 'ACCUMULO'},
            {serviceName: 'HBASE'},
            {serviceName: 'HAWQ'}
          ];
        });
        controller.set('content', Em.Object.create({
          reassignHosts: {
            source: 'host1'
          }
        }));
      });

      afterEach(function () {
        App.get.restore();
        App.Service.find.restore();
        App.MoveNameNodeConfigInitializer.cleanup();
      });

      it('HA isn\'t enabled and HBASE, HAWQ and ACCUMULO service', function () {
        isHaEnabled = false;
        var configs = {
          'hbase-site': {
            'hbase.rootdir': 'hdfs://localhost:8020/apps/hbase/data'
          },
          'accumulo-site': {
            'instance.volumes': 'hdfs://localhost:8020/apps/accumulo/data',
            'instance.volumes.replacements': ''
          },
          'hawq-site': {
            'hawq_dfs_url': 'localhost:8020/hawq/data'
          }
        };

        controller.set('content.reassignHosts.target', 'host2');

        App.MoveNameNodeConfigInitializer.setup(controller._getNnInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveNameNodeConfigInitializer);

        expect(configs['hbase-site']['hbase.rootdir']).to.equal('hdfs://host2:8020/apps/hbase/data');
        expect(configs['accumulo-site']['instance.volumes']).to.equal('hdfs://host2:8020/apps/accumulo/data');
        expect(configs['accumulo-site']['instance.volumes.replacements']).to.equal('hdfs://host1:8020/apps/accumulo/data hdfs://host2:8020/apps/accumulo/data');
        expect(configs['hawq-site'].hawq_dfs_url).to.equal('host2:8020/hawq/data');
      });

      it('HA enabled and namenode 1', function () {
        isHaEnabled = true;
        var configs = {
          'hdfs-site': {
            'dfs.nameservices': 's',
            'dfs.namenode.http-address.s.nn1': 'host1:50070',
            'dfs.namenode.https-address.s.nn1': 'host1:50470',
            'dfs.namenode.rpc-address.s.nn1': 'host1:8020'
          },
          'hdfs-client': {
            'dfs.namenode.rpc-address.s.nn1': '',
            'dfs.namenode.http-address.s.nn1': 'host1:50070'
          }
        };

        controller.set('content.reassignHosts.target', 'host2');
        App.MoveNameNodeConfigInitializer.setup(controller._getNnInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveNameNodeConfigInitializer);
        expect(configs['hdfs-site']).to.eql({
          "dfs.nameservices": "s",
          "dfs.namenode.http-address.s.nn1": "host2:50070",
          "dfs.namenode.https-address.s.nn1": "host2:50470",
          "dfs.namenode.rpc-address.s.nn1": "host2:8020"
        });
        expect(configs['hdfs-client']).to.eql({
          "dfs.namenode.http-address.s.nn1": "host2:50070",
          "dfs.namenode.rpc-address.s.nn1": "host2:8020"
        });
      });

      it('HA enabled and namenode 2', function () {
        isHaEnabled = true;
        var configs = {
          'hdfs-site': {
            'dfs.nameservices': 's',
            "dfs.namenode.http-address.s.nn1": "host1:50070",
            'dfs.namenode.http-address.s.nn2': 'host2:50070',
            'dfs.namenode.https-address.s.nn2': 'host2:50470',
            'dfs.namenode.rpc-address.s.nn2': 'host2:8020'
          },
          'hdfs-client': {
            'dfs.namenode.rpc-address.s.nn2': '',
            'dfs.namenode.http-address.s.nn2': 'host2:50070'
          }
        };
        controller.set('content.reassignHosts.source', 'host2');
        controller.set('content.reassignHosts.target', 'host3');

        App.MoveNameNodeConfigInitializer.setup(controller._getNnInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveNameNodeConfigInitializer);

        expect(configs['hdfs-site']).to.eql({
          "dfs.nameservices": "s",
          "dfs.namenode.http-address.s.nn1": "host1:50070",
          "dfs.namenode.http-address.s.nn2": "host3:50070",
          "dfs.namenode.https-address.s.nn2": "host3:50470",
          "dfs.namenode.rpc-address.s.nn2": "host3:8020"
        });
        expect(configs['hdfs-client']).to.eql({
          "dfs.namenode.http-address.s.nn2": "host3:50070",
          "dfs.namenode.rpc-address.s.nn2": "host3:8020"
        });
      });

    });

    describe('OOZIE_SERVER', function () {

      it('should upodate hadoop.proxyuser.${oozie_user}.hosts', function () {

        var configs = {
          'oozie-env': {
            'oozie_user': 'cool_dude'
          },
          'core-site': {
            'hadoop.proxyuser.cool_dude.hosts': ''
          }
        };

        controller.set('content', Em.Object.create({
          masterComponentHosts: [
            {
              component: 'OOZIE_SERVER',
              hostName: 'host2'
            },
            {
              component: 'OOZIE_SERVER',
              hostName: 'host3'
            },
            {
              component: 'OOZIE_SERVER',
              hostName: 'host1'
            }
          ],
          reassignHosts: {
            source: 'host1',
            target: 'host4'
          }
        }));

        App.MoveOSConfigInitializer.setup(controller._getOsInitializerSettings(configs));
        configs = controller.setDynamicConfigs(configs, App.MoveOSConfigInitializer);
        App.MoveOSConfigInitializer.cleanup();

        expect(configs['core-site']['hadoop.proxyuser.cool_dude.hosts']).to.equal('host2,host3,host4');

      });

    });

  });

  describe("#loadStep()", function () {

    it('should set isLoaded to false and send ajax request', function () {
      controller.set('wizardController', Em.Object.create({isComponentWithReconfiguration: true}));
      controller.loadStep();
      expect(controller.get('isLoaded')).to.be.false
      expect(App.ajax.send.calledOnce).to.be.true;
    });

    it('should set isLoaded to true', function () {
      controller.set('wizardController', Em.Object.create({isComponentWithReconfiguration: false}));
      controller.loadStep();
      expect(controller.get('isLoaded')).to.be.true;
    });
  });

  describe("#clearStep()", function () {

    beforeEach(function () {
      sinon.stub(controller, 'setProperties');
    });

    afterEach(function () {
      controller.setProperties.restore();
    });

    it('should set properties', function () {
      controller.clearStep();
      expect(controller.setProperties.calledOnce).to.be.true
    });
  });

  describe("#getDisplayName()", function () {

    beforeEach(function () {
      sinon.stub(App.config, 'get').withArgs('serviceByConfigTypeMap').returns({
        'fname1': Em.Object.create({
          serviceName: 's1'
        }),
        'fname2': Em.Object.create({
          serviceName: 's1'
        })
      });
    });

    afterEach(function () {
      App.config.get.restore();
    });

    it('should display name', function () {
      controller.set('propertiesToChange', {
        'fname1': [{
          name: 'prop1'
        }],
        'fname2': [{
          name: 'prop2'
        }]
      });
      expect(controller.getDisplayName('stack1', 'prop1', 'type1', 's1')).to.equal('type1/prop1')
    });
  });

  describe("#onLoadConfigs()", function () {
    var testCases = ['NAMENODE', 'RESOURCEMANAGER', 'HIVE_METASTORE', 'HIVE_SERVER', 'WEBHCAT_SERVER', 'OOZIE_SERVER'];

    beforeEach(function () {
      sinon.stub(controller, 'setAdditionalConfigs');
      sinon.stub(controller, 'setSecureConfigs');
      sinon.stub(controller, '_getNnInitializerSettings');
      sinon.stub(controller, '_getRmInitializerSettings');
      sinon.stub(controller, '_getHiveInitializerSettings');
      sinon.stub(controller, '_getWsInitializerSettings');
      sinon.stub(controller, '_getOsInitializerSettings');
      sinon.stub(controller, 'setDynamicConfigs');
      sinon.stub(controller, '_getRmAdditionalDependencies');
      sinon.stub(controller, 'renderServiceConfigs');
      sinon.stub(App.MoveNameNodeConfigInitializer, 'setup');
      sinon.stub(App.MoveNameNodeConfigInitializer, 'cleanup');
      sinon.stub(App.MoveRmConfigInitializer, 'setup');
      sinon.stub(App.MoveRmConfigInitializer, 'cleanup');
      sinon.stub(App.MoveHmConfigInitializer, 'setup');
      sinon.stub(App.MoveHmConfigInitializer, 'cleanup');
      sinon.stub(App.MoveHsConfigInitializer, 'setup');
      sinon.stub(App.MoveHsConfigInitializer, 'cleanup');
      sinon.stub(App.MoveWsConfigInitializer, 'setup');
      sinon.stub(App.MoveWsConfigInitializer, 'cleanup');
      sinon.stub(App.MoveOSConfigInitializer, 'setup');
      sinon.stub(App.MoveOSConfigInitializer, 'cleanup');
    });

    afterEach(function () {
      controller.setAdditionalConfigs.restore();
      controller.setSecureConfigs.restore();
      controller._getNnInitializerSettings.restore();
      controller._getRmInitializerSettings.restore();
      controller._getHiveInitializerSettings.restore();
      controller._getWsInitializerSettings.restore();
      controller._getOsInitializerSettings.restore();
      controller.setDynamicConfigs.restore();
      controller._getRmAdditionalDependencies.restore();
      controller.renderServiceConfigs.restore();
      App.MoveNameNodeConfigInitializer.setup.restore();
      App.MoveNameNodeConfigInitializer.cleanup.restore();
      App.MoveRmConfigInitializer.setup.restore();
      App.MoveRmConfigInitializer.cleanup.restore();
      App.MoveHmConfigInitializer.setup.restore();
      App.MoveHmConfigInitializer.cleanup.restore();
      App.MoveHsConfigInitializer.setup.restore();
      App.MoveHsConfigInitializer.cleanup.restore();
      App.MoveWsConfigInitializer.setup.restore();
      App.MoveWsConfigInitializer.cleanup.restore();
      App.MoveOSConfigInitializer.setup.restore();
      App.MoveOSConfigInitializer.cleanup.restore();
    });

    testCases.forEach(function (test) {
      it('should set configs for ' + test + ' component', function () {
        var data = {
          items: [
            {
              type: 'yarn-site',
              properties: {
                ys: 'ys'
              },
              properties_attributes: {
                ys: 'pa_ys'
              }
            },
            {
              type: 'hive-site',
              properties: {
                hs: 'hs'
              },
              properties_attributes: {
                hs: 'pa_hs'
              }
            },
            {
              type: 'webhcat-site',
              properties: {
                ws: 'ws'
              },
              properties_attributes: {
                ws: 'pa_ws'
              }
            },
            {
              type: 'hawq-site',
              properties: {
                p: 'hs',
                hawq_global_rm_type: 'yarn'
              },
              properties_attributes: {
                p: 'pa_hs'
              }
            }
          ]
        };

        controller.set('content.reassign', Em.Object.create({component_name: test}));
        controller.set('content.reassignHosts', Em.Object.create({target: 'host1'}));
        controller.set('wizardController', {relatedServicesMap: {'RESOURCEMANAGER': {append: Em.K}}});

        controller.onLoadConfigs(data);

        if (test === 'NAMENODE') {
          expect(controller._getNnInitializerSettings.calledOnce).to.be.true;
          expect(App.MoveNameNodeConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveNameNodeConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        if (test === 'RESOURCEMANAGER') {
          expect(controller._getRmInitializerSettings.calledOnce).to.be.true;
          expect(controller._getRmAdditionalDependencies.calledOnce).to.be.true;
          expect(App.MoveRmConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveRmConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        if (test === 'HIVE_METASTORE') {
          expect(controller._getHiveInitializerSettings.calledOnce).to.be.true;
          expect(App.MoveHmConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveHmConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        if (test === 'HIVE_SERVER') {
          expect(controller._getHiveInitializerSettings.calledOnce).to.be.true;
          expect(App.MoveHsConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveHsConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        if (test === 'WEBHCAT_SERVER') {
          expect(controller._getWsInitializerSettings.calledOnce).to.be.true;
          expect(App.MoveWsConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveWsConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        if (test === 'OOZIE_SERVER') {
          expect(controller._getOsInitializerSettings.calledOnce).to.be.true;
          expect(App.MoveOSConfigInitializer.setup.calledOnce).to.be.true;
          expect(App.MoveOSConfigInitializer.cleanup.calledOnce).to.be.true;
        }

        expect(controller.setAdditionalConfigs.calledOnce).to.be.true;
        expect(controller.setSecureConfigs.calledOnce).to.be.true;
        expect(controller.setDynamicConfigs.calledOnce).to.be.true;
        expect(controller.renderServiceConfigs.calledOnce).to.be.true;
      });
    });
  });

  describe("#updateServiceConfigs()", function () {

    beforeEach(function () {
      sinon.stub(App.config, 'getConfigTagFromFileName').returns('type1');
    });

    afterEach(function () {
      App.config.getConfigTagFromFileName.restore();
    });

    it('should update service configs', function () {
      controller.set('configs', {
        'type1': {
          'prop1': ''
        }
      });
      controller.set('selectedService', {
        configs: [
          {
            name: 'prop1',
            fileName: 'fname1',
            value: 'pvalue'
          }
        ]
      });
      controller.updateServiceConfigs();
      expect(JSON.stringify(controller.get('configs'))).to.equal('{"type1":{"prop1":"pvalue"}}');
    });
  });
});
