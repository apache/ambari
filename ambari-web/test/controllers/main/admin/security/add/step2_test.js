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

require('controllers/main/admin/security/add/step2');
require('models/service');

describe('App.MainAdminSecurityAddStep2Controller', function () {

  var controller = App.MainAdminSecurityAddStep2Controller.create({
    content: {}
  });

  describe('#clearStep()', function () {
    it('Info is empty', function () {
      controller.set('stepConfigs', []);
      controller.set('securityUsers', []);
      controller.clearStep();
      expect(controller.get('stepConfigs')).to.be.empty;
      expect(controller.get('securityUsers')).to.be.empty;
    });
    it('Info filled', function () {
      controller.set('stepConfigs', [1]);
      controller.set('securityUsers', [1]);
      controller.clearStep();
      expect(controller.get('stepConfigs')).to.be.empty;
      expect(controller.get('securityUsers')).to.be.empty;
    });
  });

  describe('#isSubmitDisabled', function () {
    var tests = [
      {
        config: [
          {
            showConfig: true,
            errorCount: 0
          }
        ],
        m: 'All show configs, nothing with errors',
        e: false
      },
      {
        config: [
          {
            showConfig: true,
            errorCount: 0
          },
          {
            showConfig: true,
            errorCount: 1
          }
        ],
        m: 'All show configs, 1 with errors',
        e: true
      },
      {
        config: [
          {
            showConfig: true,
            errorCount: 0
          },
          {
            showConfig: false,
            errorCount: 1
          }
        ],
        m: '1 has errors but not visible',
        e: false
      },
      {
        config: [
          {
            showConfig: false,
            errorCount: 0
          },
          {
            showConfig: false,
            errorCount: 1
          }
        ],
        m: '1 has errors, all not visible',
        e: false
      },
      {
        config: [
          {
            showConfig: true,
            errorCount: 1
          },
          {
            showConfig: true,
            errorCount: 1
          }
        ],
        m: 'All has errors, all not visible',
        e: true
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('stepConfigs', test.config);
        expect(controller.get('isSubmitDisabled')).to.equal(test.e);
      });
    });
  });

  describe('#loadStep()', function () {
    it('load step', function () {
      controller.set('stepConfigs', [
        {}
      ]);
      controller.set('securityUsers', ['user1']);
      controller.set('content.services', ['service1']);
      controller.set('content.serviceConfigProperties', ['config1']);
      sinon.stub(controller, 'clearStep', Em.K);
      sinon.stub(controller, 'loadUsers', Em.K);
      sinon.stub(controller, 'addUserPrincipals', Em.K);
      sinon.stub(controller, 'renderServiceConfigs', Em.K);
      sinon.stub(controller, 'changeCategoryOnHa', Em.K);
      sinon.stub(controller, 'setStoredConfigsValue', Em.K);
      sinon.stub(controller, 'addHostPrincipals', Em.K);
      sinon.stub(App.Service, 'find', function () {
        return [
          {serviceName: 'HDFS'}
        ];
      });

      controller.loadStep();
      expect(controller.get('installedServices')).to.eql(['HDFS']);
      expect(controller.clearStep.calledOnce).to.be.true;
      expect(controller.loadUsers.calledOnce).to.be.true;
      expect(controller.addUserPrincipals.calledWith(['service1'], ['user1'])).to.be.true;
      expect(controller.addHostPrincipals.calledOnce).to.be.true;
      expect(controller.renderServiceConfigs.calledWith(['service1'])).to.be.true;
      expect(controller.changeCategoryOnHa.calledWith(['service1'], [{}])).to.be.true;
      expect(controller.setStoredConfigsValue.calledWith(['config1'])).to.be.true;

      controller.clearStep.restore();
      controller.loadUsers.restore();
      controller.addUserPrincipals.restore();
      controller.renderServiceConfigs.restore();
      controller.changeCategoryOnHa.restore();
      controller.setStoredConfigsValue.restore();
      controller.addHostPrincipals.restore();
      App.Service.find.restore();
    });
  });

  describe('#setStoredConfigsValue()', function () {
    it('storedConfigProperties is null', function () {
      expect(controller.setStoredConfigsValue(null)).to.be.false;
    });
    it('stepConfigs is empty', function () {
      controller.set('stepConfigs', []);
      expect(controller.setStoredConfigsValue([])).to.be.true;
      expect(controller.get('stepConfigs')).to.be.empty;
    });
    it('stepConfig has no configs', function () {
      controller.set('stepConfigs', [Em.Object.create({
        configs: []
      })]);
      expect(controller.setStoredConfigsValue([])).to.be.true;
      expect(controller.get('stepConfigs')[0].get('configs')).to.be.empty;
    });
    it('stepConfig has no stored configs', function () {
      controller.set('stepConfigs', [Em.Object.create({
        configs: [Em.Object.create({
          name: 'config1',
          value: 'value1'
        })]
      })]);
      var storedConfigProperties = [
        {
          name: 'config2',
          value: "value2"
        }
      ];
      expect(controller.setStoredConfigsValue(storedConfigProperties)).to.be.true;
      expect(controller.get('stepConfigs')[0].get('configs').findProperty('name', 'config1').get('value')).to.equal('value1');
    });
    it('stepConfig has stored configs', function () {
      controller.set('stepConfigs', [Em.Object.create({
        configs: [Em.Object.create({
          name: 'config2',
          value: 'value1'
        })]
      })]);
      var storedConfigProperties = [
        {
          name: 'config2',
          value: "value2"
        }
      ];
      expect(controller.setStoredConfigsValue(storedConfigProperties)).to.be.true;
      expect(controller.get('stepConfigs')[0].get('configs').findProperty('name', 'config2').get('value')).to.equal('value2');
    });
  });

  describe('#renderServiceConfigs()', function () {
    it('serviceConfigs and stepConfigs are empty', function () {
      controller.set('stepConfigs', []);
      controller.renderServiceConfigs([]);
      expect(controller.get('selectedService')).to.be.undefined;
    });
    it('serviceConfigs is empty', function () {
      controller.set('stepConfigs', [
        {showConfig: true}
      ]);
      controller.renderServiceConfigs([]);
      expect(controller.get('selectedService')).to.eql({showConfig: true});
    });
    it('serviceConfigs has service', function () {
      var serviceConfigs = [
        {
          serviceName: 'HDFS',
          configs: []
        }
      ];
      sinon.stub(controller, 'wrapConfigProperties', function () {
        return [];
      });
      controller.set('stepConfigs', []);
      controller.renderServiceConfigs(serviceConfigs);
      expect(controller.get('selectedService').get('serviceName')).to.equal('HDFS');
      expect(controller.get('selectedService').get('showConfig')).to.be.true;
      expect(controller.get('selectedService').get('configs')).to.be.empty;
      expect(controller.wrapConfigProperties.calledWith({
        serviceName: 'HDFS',
        configs: []
      })).to.be.true;
      controller.wrapConfigProperties.restore();
    });
  });

  describe('#wrapConfigProperties()', function () {
    it('_componentConfig is empty', function () {
      expect(controller.wrapConfigProperties({configs: []})).to.be.empty;
    });
    it('serviceConfigs has service', function () {
      var mock = Em.Object.create({
        validate: Em.K,
        isReconfigurable: true,
        isEditable: false
      });
      var _componentConfig = {configs: [
        {name: 'config1'}
      ]};
      sinon.stub(App.ServiceConfigProperty, 'create', function () {
        return mock;
      });
      sinon.spy(mock, 'validate');
      expect(controller.wrapConfigProperties(_componentConfig)[0].get('isEditable')).to.be.true;
      expect(App.ServiceConfigProperty.create.calledWith({name: 'config1'})).to.be.true;
      expect(mock.validate.calledOnce).to.be.true;
      mock.validate.restore();
      App.ServiceConfigProperty.create.restore();
    });
  });

  describe('#setHostsToConfig()', function () {
    it('service is null', function () {
      expect(controller.setHostsToConfig(null)).to.be.false;
    });
    it('service.configs is empty', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: []
        }
      ]);
      expect(controller.setHostsToConfig('HDFS')).to.be.false;
    });
    it('No such config name in service.configs', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: [
            {
              name: 'config1'
            }
          ]
        }
      ]);
      expect(controller.setHostsToConfig('HDFS', 'config2')).to.be.false;
    });
    it('Correct config in service.configs', function () {
      sinon.stub(App.Service, 'find', function () {
        return Em.Object.create({
          hostComponents: [
            Em.Object.create({
              componentName: 'comp1',
              hostName: 'host1'
            })
          ]
        });
      });
      expect(controller.setHostsToConfig('HDFS', 'config1', ['comp1'])).to.be.true;
      expect(controller.get('content.services')[0].configs[0].defaultValue).to.eql(['host1']);
      App.Service.find.restore();
    });
  });

  describe('#setHostToPrincipal()', function () {
    it('service is null', function () {
      expect(controller.setHostToPrincipal(null)).to.be.false;
    });
    it('service.configs is empty', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: []
        }
      ]);
      expect(controller.setHostToPrincipal('HDFS')).to.be.false;
    });
    it('No such hostConfigName name in service.configs', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: [
            {
              name: 'config1'
            }
          ]
        }
      ]);
      expect(controller.setHostToPrincipal('HDFS', 'config2', 'config1')).to.be.false;
    });
    it('No such principalConfigName name in service.configs', function () {
      expect(controller.setHostToPrincipal('HDFS', 'config1', 'config2')).to.be.false;
    });
    it('Correct config in service.configs', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: [
            {
              name: 'config1',
              defaultValue: 'value1'
            },
            {
              name: 'principal1'
            }
          ]
        }
      ]);
      expect(controller.setHostToPrincipal('HDFS', 'config1', 'principal1', 'name1')).to.be.true;
      expect(controller.get('content.services')[0].configs[0].defaultValue).to.equal('value1');
      expect(controller.get('content.services')[0].configs[1].defaultValue).to.equal('name1value1');
    });
    it('Correct config in service.configs, defaultValue is array', function () {
      controller.set('content.services', [
        {
          serviceName: 'HDFS',
          configs: [
            {
              name: 'config1',
              defaultValue: ['Value1']
            },
            {
              name: 'principal1'
            }
          ]
        }
      ]);
      expect(controller.setHostToPrincipal('HDFS', 'config1', 'principal1', 'name1')).to.be.true;
     // expect(controller.get('content.services')[0].configs[0].defaultValue).to.equal('Value1');
      expect(controller.get('content.services')[0].configs[1].defaultValue).to.equal('name1Value1');
    });
    it('stack 2.2 `storm_principal_name` config should be set to `storm`', function() {
      sinon.stub(App, 'get').withArgs('currentStackVersionNumber').returns('2.2');
      controller.set('content.services', [
        {
          serviceName: 'STORM',
          configs: [
            {
              name: 'nimbus_host',
              defaultValue: 'Value1'
            },
            {
              name: 'storm_principal_name'
            }
          ]
        }
      ]);
      controller.setHostToPrincipal('STORM', 'nimbus_host', 'storm_principal_name', 'storm');
      App.get.restore();
      expect(controller.get('content.services')[0].configs[1].defaultValue).to.equal('storm');
    });
    it('stack 2.1 `oozie_http_principal_name` value should contains OOZIE_SERVER host', function() {
      sinon.stub(App, 'get').withArgs('currentStackVersionNumber').returns('2.1');
      controller.set('content.services', [
        {
          serviceName: 'OOZIE',
          configs: [
            {
              name: 'oozie_servername',
              defaultValue: 'host1.com'
            },
            {
              name: 'oozie_http_principal_name'
            }
          ]
        }
      ]);
      controller.setHostToPrincipal('OOZIE', 'oozie_servername', 'oozie_http_principal_name', 'HTTP/');
      App.get.restore();
      expect(controller.get('content.services')[0].configs[1].defaultValue).to.equal('HTTP/host1.com');
    });
    it('stack 2.2 `oozie_http_principal_name` value should be set to HTTP/_HOST', function() {
      sinon.stub(App, 'get').withArgs('currentStackVersionNumber').returns('2.2');
      controller.set('content.services', [
        {
          serviceName: 'OOZIE',
          configs: [
            {
              name: 'oozie_servername',
              defaultValue: 'host1.com'
            },
            {
              name: 'oozie_http_principal_name'
            }
          ]
        }
      ]);
      controller.setHostToPrincipal('OOZIE', 'oozie_servername', 'oozie_http_principal_name', 'HTTP/');
      App.get.restore();
      expect(controller.get('content.services')[0].configs[1].defaultValue).to.equal('HTTP/_HOST');
    });
  });

  describe('#loadUsers()', function () {

    afterEach(function () {
      App.router.get.restore();
    });

    it('serviceUsers is correct', function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({serviceUsers: [
          {}
        ]})
      });
      controller.loadUsers();
      expect(controller.get('securityUsers')).to.eql([
        {}
      ]);
    });
    it('serviceUsers is null, testMode = true', function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({serviceUsers: null})
      });
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        return Em.get(App, k);
      });
      controller.loadUsers();
      expect(controller.get('securityUsers').mapProperty('name')).to.eql(["hdfs_user",
        "mapred_user",
        "hbase_user",
        "hive_user",
        "smokeuser"
      ]);
      App.get.restore();
    });
    it('serviceUsers is empty, testMode = true', function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({serviceUsers: []})
      });
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        return Em.get(App, k);
      });
      controller.loadUsers();
      expect(controller.get('securityUsers').mapProperty('name')).to.eql(["hdfs_user",
        "mapred_user",
        "hbase_user",
        "hive_user",
        "smokeuser"
      ]);
      App.get.restore();
    });
    it('serviceUsers is null, testMode = false', function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({serviceUsers: null})
      });
      sinon.stub(App.db, 'getSecureUserInfo', function () {
        return [
          {}
        ];
      });
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });
      controller.loadUsers();
      expect(controller.get('securityUsers')).to.eql([
        {}
      ]);
      expect(App.db.getSecureUserInfo.calledOnce).to.be.true;
      App.db.getSecureUserInfo.restore();
      App.get.restore();
    });
    it('serviceUsers is empty, testMode = false', function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({serviceUsers: []})
      });
      sinon.stub(App.db, 'getSecureUserInfo', function () {
        return [
          {}
        ];
      });
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });
      controller.loadUsers();
      expect(controller.get('securityUsers')).to.eql([
        {}
      ]);
      expect(App.db.getSecureUserInfo.calledOnce).to.be.true;
      App.db.getSecureUserInfo.restore();
      App.get.restore();
    });
  });

  describe('#addUserPrincipals()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'setUserPrincipalValue', function () {
        return true;
      });
    });
    afterEach(function () {
      controller.setUserPrincipalValue.restore();
    });

    var generalConfigs = [
      {
        serviceName: 'GENERAL',
        configs: [
          {
            name: 'hbase_principal_name',
            isVisible: false
          },
          {
            name: 'hbase_user_keytab',
            isVisible: false
          },
          {
            name: 'hdfs_principal_name',
            isVisible: false
          },
          {
            name: 'hdfs_user_keytab',
            isVisible: false
          }
        ]
      }
    ];
    var securityUsers = [];

    it('HBASE or HDFS services are not installed neither', function () {
      var serviceConfigs = generalConfigs.slice(0);
      controller.addUserPrincipals(serviceConfigs, securityUsers);
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_principal_name').isVisible).to.be.false;
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_user_keytab').isVisible).to.be.false;
    });
    it('HBASE service is installed', function () {
      var serviceConfigs = generalConfigs.slice(0);
      serviceConfigs.push({serviceName: 'HBASE'});
      controller.addUserPrincipals(serviceConfigs, securityUsers);
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_principal_name').isVisible).to.be.true;
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_user_keytab').isVisible).to.be.true;
    });
    it('HDFS service is installed', function () {
      var serviceConfigs = generalConfigs.slice(0);
      serviceConfigs.push({serviceName: 'HDFS'});
      controller.addUserPrincipals(serviceConfigs, securityUsers);
      expect(serviceConfigs[0].configs.findProperty('name', 'hdfs_principal_name').isVisible).to.be.true;
      expect(serviceConfigs[0].configs.findProperty('name', 'hdfs_user_keytab').isVisible).to.be.true;
    });
    it('HDFS and HBASE services are installed', function () {
      var serviceConfigs = generalConfigs.slice(0);
      serviceConfigs.push({serviceName: 'HDFS'});
      serviceConfigs.push({serviceName: 'HBASE'});
      controller.addUserPrincipals(serviceConfigs, securityUsers);
      expect(serviceConfigs[0].configs.findProperty('name', 'hdfs_principal_name').isVisible).to.be.true;
      expect(serviceConfigs[0].configs.findProperty('name', 'hdfs_user_keytab').isVisible).to.be.true;
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_principal_name').isVisible).to.be.true;
      expect(serviceConfigs[0].configs.findProperty('name', 'hbase_user_keytab').isVisible).to.be.true;
    });
  });

  describe('#setUserPrincipalValue()', function () {
    it('user and userPrincipal are null', function () {
      expect(controller.setUserPrincipalValue(null, null)).to.be.false;
    });
    it('user is null', function () {
      expect(controller.setUserPrincipalValue(null, {})).to.be.false;
    });
    it('userPrincipal is null', function () {
      expect(controller.setUserPrincipalValue({}, null)).to.be.false;
    });
    it('user and userPrincipal are correct', function () {
      var user = {value: 'value1'};
      var userPrincipal = {};
      expect(controller.setUserPrincipalValue(user, userPrincipal)).to.be.true;
      expect(userPrincipal.defaultValue).to.equal('value1');
    });
  });

  describe('#addHostPrincipals()', function () {
    it('hostToPrincipalMap is empty', function () {
      sinon.stub(controller, 'setHostToPrincipal', Em.K);
      controller.set('hostToPrincipalMap', []);
      controller.addHostPrincipals();
      expect(controller.setHostToPrincipal.called).to.be.false;
      controller.setHostToPrincipal.restore();
    });
    it('Correct data', function () {
      sinon.stub(controller, 'setHostToPrincipal', Em.K);
      controller.set('hostToPrincipalMap', [
        {
          serviceName: 'HDFS',
          configName: 'datanode_hosts',
          principalName: 'principal1',
          primaryName: 'name1'
        }
      ]);
      controller.addHostPrincipals();
      expect(controller.setHostToPrincipal.calledWith('HDFS', 'datanode_hosts', 'principal1', 'name1')).to.be.true;
      controller.setHostToPrincipal.restore();
    });
  });

  describe('#changeCategoryOnHa()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'removeConfigCategory', Em.K);
    });
    afterEach(function () {
      controller.removeConfigCategory.restore();
    });

    var serviceConfigs = [{
      serviceName: 'HDFS',
      configCategories: []
    }];
    var stepConfigs = [Em.Object.create({
      serviceName: 'HDFS',
      configs: []
    })];

    it('HDFS service is absent', function () {
      expect(controller.changeCategoryOnHa([], [])).to.be.false;
    });
    it('HDFS service installed, App.testMode and App.testNameNodeHA - true', function () {
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        if ('testNameNodeHA' === k) return true;
        return Em.get(App, k);
      });
      expect(controller.changeCategoryOnHa(serviceConfigs, stepConfigs)).to.be.true;
      expect(controller.removeConfigCategory.calledWith([], [], 'SNameNode')).to.be.true;
      App.get.restore();
    });
    it('HDFS service installed, content.isNnHa = true', function () {
      controller.set('content.isNnHa', 'true');
      expect(controller.changeCategoryOnHa(serviceConfigs, stepConfigs)).to.be.true;
      expect(controller.removeConfigCategory.calledWith([], [], 'SNameNode')).to.be.true;
    });
    it('HDFS service installed, HA disabled', function () {
      controller.set('content.isNnHa', 'false');
      expect(controller.changeCategoryOnHa(serviceConfigs, stepConfigs)).to.be.true;
      expect(controller.removeConfigCategory.calledWith([], [], 'JournalNode')).to.be.true;
    });
  });

  describe('#removeConfigCategory()', function () {
    it('properties should be hidden', function () {
      var properties = [
        Em.Object.create({
          category: 'comp1',
          isVisible: true
        })
      ];
      controller.removeConfigCategory(properties, [], 'comp1');
      expect(properties[0].isVisible).to.be.false;
    });
    it('category should be removed', function () {
      var configCategories = [
        Em.Object.create({
          name: 'comp1'
        })
      ];
      controller.removeConfigCategory([], configCategories, 'comp1');
      expect(configCategories).to.be.empty;
    });
  });
});
