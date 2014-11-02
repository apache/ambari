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

require('models/service_config');

var serviceConfig,
  serviceConfigCategory,
  group,
  serviceConfigProperty,
  serviceConfigPropertyInit,
  configsData = [
    Ember.Object.create({
      overrides: [
        {
          error: true,
          errorMessage: 'error'
        },
        {
          error: true
        },
        {}
      ]
    }),
    Ember.Object.create({
      isValid: false,
      isVisible: true
    }),
      Ember.Object.create({
      isValid: true,
      isVisible: true
    }),
    Ember.Object.create({
      isValid: false,
      isVisible: false
    })
  ],
  configCategoriesData = [
    Em.Object.create({
      slaveErrorCount: 1
    }),
    Em.Object.create({
      slaveErrorCount: 2
    })
  ],
  nameCases = [
    {
      name: 'DataNode',
      primary: 'DATANODE'
    },
    {
      name: 'TaskTracker',
      primary: 'TASKTRACKER'
    },
    {
      name: 'RegionServer',
      primary: 'HBASE_REGIONSERVER'
    },
    {
      name: 'name',
      primary: null
    }
  ],
  components = [
    {
      name: 'NameNode',
      master: true
    },
    {
      name: 'SNameNode',
      master: true
    },
    {
      name: 'JobTracker',
      master: true
    },
    {
      name: 'HBase Master',
      master: true
    },
    {
      name: 'Oozie Master',
      master: true
    },
    {
      name: 'Hive Metastore',
      master: true
    },
    {
      name: 'WebHCat Server',
      master: true
    },
    {
      name: 'ZooKeeper Server',
      master: true
    },
    {
      name: 'Nagios',
      master: true
    },
    {
      name: 'Ganglia',
      master: true
    },
    {
      name: 'DataNode',
      slave: true
    },
    {
      name: 'TaskTracker',
      slave: true
    },
    {
      name: 'RegionServer',
      slave: true
    }
  ],
  masters = components.filterProperty('master'),
  slaves = components.filterProperty('slave'),
  groupsData = {
    groups: [
      Em.Object.create({
        errorCount: 1
      }),
      Em.Object.create({
        errorCount: 2
      })
    ]
  },
  groupNoErrorsData = [].concat(configsData.slice(2)),
  groupErrorsData = [configsData[1]],
  overridableFalseData = [
    {
      isOverridable: false
    },
    {
      isEditable: false,
      overrides: configsData[0].overrides
    },
    {
      displayType: 'masterHost'
    }
  ],
  overridableTrueData = [
    {
      isOverridable: true,
      isEditable: true
    },    {
      isOverridable: true,
      overrides: []
    },
    {
      isOverridable: true
    }
  ],
  overriddenFalseData = [
    {
      overrides: null,
      isOriginalSCP: true
    },
    {
      overrides: [],
      isOriginalSCP: true
    }
  ],
  overriddenTrueData = [
    {
      overrides: configsData[0].overrides
    },
    {
      isOriginalSCP: false
    }
  ],
  removableFalseData = [
    {
      isEditable: false
    },
    {
      hasOverrides: true
    },
    {
      isUserProperty: false,
      isOriginalSCP: true
    }
  ],
  removableTrueData = [
    {
      isEditable: true,
      hasOverrides: false,
      isUserProperty: true
    },
    {
      isEditable: true,
      hasOverrides: false,
      isOriginalSCP: false
    }
  ],
  initPropertyData = [
    {
      initial: {
        displayType: 'password',
        value: 'value'
      },
      result: {
        retypedPassword: 'value'
      }
    },
    {
      initial: {
        id: 'puppet var',
        value: '',
        defaultValue: 'default'
      },
      result: {
        value: 'default'
      }
    }
  ],
  notDefaultFalseData = [
    {
      isEditable: false
    },
    {
      defaultValue: null
    },
    {
      value: 'value',
      defaultValue: 'value'
    }
  ],
  notDefaultTrueData = {
    isEditable: true,
    value: 'value',
    defaultValue: 'default'
  },
  types = ['masterHost', 'slaveHosts', 'masterHosts', 'slaveHost', 'radio button'],
  classCases = [
    {
      initial: {
        displayType: 'checkbox'
      },
      viewClass: App.ServiceConfigCheckbox
    },
    {
      initial: {
        displayType: 'password'
      },
      viewClass: App.ServiceConfigPasswordField
    },
    {
      initial: {
        displayType: 'combobox'
      },
      viewClass: App.ServiceConfigComboBox
    },
    {
      initial: {
        displayType: 'radio button'
      },
      viewClass: App.ServiceConfigRadioButtons
    },
    {
      initial: {
        displayType: 'directories'
      },
      viewClass: App.ServiceConfigTextArea
    },
    {
      initial: {
        displayType: 'content'
      },
      viewClass: App.ServiceConfigTextAreaContent

    },
    {
      initial: {
        displayType: 'multiLine'
      },
      viewClass: App.ServiceConfigTextArea
    },
    {
      initial: {
        displayType: 'custom'
      },
      viewClass: App.ServiceConfigBigTextArea
    },
    {
      initial: {
        displayType: 'masterHost'
      },
      viewClass: App.ServiceConfigMasterHostView
    },
    {
      initial: {
        displayType: 'masterHosts'
      },
      viewClass: App.ServiceConfigMasterHostsView
    },
    {
      initial: {
        displayType: 'slaveHosts'
      },
      viewClass: App.ServiceConfigSlaveHostsView
    },
    {
      initial: {
        unit: true,
        displayType: 'type'
      },
      viewClass: App.ServiceConfigTextFieldWithUnit
    },
    {
      initial: {
        unit: false,
        displayType: 'type'
      },
      viewClass: App.ServiceConfigTextField
    }
  ];


describe('App.ServiceConfig', function () {

  beforeEach(function () {
    serviceConfig = App.ServiceConfig.create();
  });

  describe('#errorCount', function () {
    it('should be 0', function () {
      serviceConfig.setProperties({
        configs: [],
        configCategories: []
      });
      expect(serviceConfig.get('errorCount')).to.equal(0);
    });
    it('should sum counts of all errors', function () {
      serviceConfig.setProperties({
        configs: configsData,
        configCategories: configCategoriesData
      });
      expect(serviceConfig.get('errorCount')).to.equal(6);
    });
  });

});

describe('App.ServiceConfigCategory', function () {

  beforeEach(function () {
    serviceConfigCategory = App.ServiceConfigCategory.create();
  });

  describe('#primaryName', function () {
    nameCases.forEach(function (item) {
      it('should return ' + item.primary, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('primaryName')).to.equal(item.primary);
      })
    });
  });

  describe('#isForMasterComponent', function () {
    masters.forEach(function (item) {
      it('should be true for ' + item.name, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('isForMasterComponent')).to.be.true;
      });
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isForMasterComponent')).to.be.false;
    });
  });

  describe('#isForSlaveComponent', function () {
    slaves.forEach(function (item) {
      it('should be true for ' + item.name, function () {
        serviceConfigCategory.set('name', item.name);
        expect(serviceConfigCategory.get('isForSlaveComponent')).to.be.true;
      });
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isForSlaveComponent')).to.be.false;
    });
  });

  describe('#slaveErrorCount', function () {
    it('should be 0', function () {
      serviceConfigCategory.set('slaveConfigs', []);
      expect(serviceConfigCategory.get('slaveErrorCount')).to.equal(0);
    });
    it('should sum all errorCount values', function () {
      serviceConfigCategory.set('slaveConfigs', groupsData);
      expect(serviceConfigCategory.get('slaveErrorCount')).to.equal(3);
    });
  });

  describe('#isAdvanced', function () {
    it('should be true', function () {
      serviceConfigCategory.set('name', 'Advanced');
      expect(serviceConfigCategory.get('isAdvanced')).to.be.true;
    });
    it('should be false', function () {
      serviceConfigCategory.set('name', 'name');
      expect(serviceConfigCategory.get('isAdvanced')).to.be.false;
    });
  });

});

describe('App.Group', function () {

  beforeEach(function () {
    group = App.Group.create();
  });

  describe('#errorCount', function () {
    it('should be 0', function () {
      group.set('properties', groupNoErrorsData);
      expect(group.get('errorCount')).to.equal(0);
    });
    it('should be 1', function () {
      group.set('properties', groupErrorsData);
      expect(group.get('errorCount')).to.equal(1);
    });
  });

});

describe('App.ServiceConfigProperty', function () {

  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create();
  });

  describe('#overrideErrorTrigger', function () {
    it('should be an increment', function () {
      serviceConfigProperty.set('overrides', configsData[0].overrides);
      expect(serviceConfigProperty.get('overrideErrorTrigger')).to.equal(1);
      serviceConfigProperty.set('overrides', []);
      expect(serviceConfigProperty.get('overrideErrorTrigger')).to.equal(2);
    });
  });

  describe('#isPropertyOverridable', function () {
    overridableFalseData.forEach(function (item) {
      it('should be false', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.false;
      });
    });
    overridableTrueData.forEach(function (item) {
      it('should be true', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.true;
      });
    });
  });

  describe('#isOverridden', function () {
    overriddenFalseData.forEach(function (item) {
      it('should be false', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isOverridden')).to.be.false;
      });
    });
    overriddenTrueData.forEach(function (item) {
      it('should be true', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isOverridden')).to.be.true;
      });
    });
  });

  describe('#isRemovable', function () {
    removableFalseData.forEach(function (item) {
      it('should be false', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isRemovable')).to.be.false;
      });
    });
    removableTrueData.forEach(function (item) {
      it('should be true', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isRemovable')).to.be.true;
      });
    });
  });

  describe('#init', function () {
    initPropertyData.forEach(function (item) {
      it('should set initial data', function () {
        serviceConfigPropertyInit = App.ServiceConfigProperty.create(item.initial);
        Em.keys(item.result).forEach(function (prop) {
          expect(serviceConfigPropertyInit.get(prop)).to.equal(item.result[prop]);
        });
      });
    });
  });

  describe('#isNotDefaultValue', function () {
    notDefaultFalseData.forEach(function (item) {
      it('should be false', function () {
        Em.keys(item).forEach(function (prop) {
          serviceConfigProperty.set(prop, item[prop]);
        });
        expect(serviceConfigProperty.get('isNotDefaultValue')).to.be.false;
      });
    });
    it('should be true', function () {
      Em.keys(notDefaultTrueData).forEach(function (prop) {
        serviceConfigProperty.set(prop, notDefaultTrueData[prop]);
      });
      expect(serviceConfigProperty.get('isNotDefaultValue')).to.be.true;
    });
  });

  describe('#cantBeUndone', function () {
    types.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.set('displayType', item);
        expect(serviceConfigProperty.get('cantBeUndone')).to.be.true;
      });
    });
    it('should be false', function () {
      serviceConfigProperty.set('displayType', 'type');
      expect(serviceConfigProperty.get('cantBeUndone')).to.be.false;
    });
  });

  describe('#setDefaultValue', function () {
    it('should change the default value', function () {
      serviceConfigProperty.set('defaultValue', 'value0');
      serviceConfigProperty.setDefaultValue(/\d/, '1');
      expect(serviceConfigProperty.get('defaultValue')).to.equal('value1');
    });
  });

  describe('#isValid', function () {
    it('should be true', function () {
      serviceConfigProperty.set('errorMessage', '');
      expect(serviceConfigProperty.get('isValid')).to.be.true;
    });
    it('should be false', function () {
      serviceConfigProperty.set('errorMessage', 'message');
      expect(serviceConfigProperty.get('isValid')).to.be.false;
    });
  });

  describe('#viewClass', function () {
    classCases.forEach(function (item) {
      it ('should be ' + item.viewClass, function () {
        Em.keys(item.initial).forEach(function (prop) {
          serviceConfigProperty.set(prop, item.initial[prop]);
        });
        expect(serviceConfigProperty.get('viewClass')).to.eql(item.viewClass);
      });
    });
  });

  describe('#validate', function () {
    it('not required', function () {
      serviceConfigProperty.setProperties({
        isRequired: false,
        value: ''
      });
      expect(serviceConfigProperty.get('errorMessage')).to.be.empty;
      expect(serviceConfigProperty.get('error')).to.be.false;
    });
    it('should validate', function () {
      serviceConfigProperty.setProperties({
        isRequired: true,
        value: 'value'
      });
      expect(serviceConfigProperty.get('errorMessage')).to.be.empty;
      expect(serviceConfigProperty.get('error')).to.be.false;
    });
    it('should fail', function () {
      serviceConfigProperty.setProperties({
        isRequired: true,
        value: 'value'
      });
      serviceConfigProperty.set('value', '');
      expect(serviceConfigProperty.get('errorMessage')).to.equal('This is required');
      expect(serviceConfigProperty.get('error')).to.be.true;
    });
  });

});

describe('#initialValue - kafka.ganglia.metrics.host', function () {

  var tests = [
    {
      message: 'kafka.ganglia.metrics.host property should have the value of ganglia hostname when ganglia is selected',
      localDB: {masterComponentHosts: [
        {component: 'GANGLIA_SERVER', hostName: 'c6401'}
      ]},
      expected: 'c6401'
    },
    {
      message: 'kafka.ganglia.metrics.host property should have the value "localhost" when ganglia is not selected',
      localDB: {masterComponentHosts: [
        {component: 'NAMENODE', hostName: 'c6401'}
      ]},
      expected: 'localhost'
    }
  ];
  var serviceConfigProperty;
  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create({name: 'kafka.ganglia.metrics.host', value: 'localhost'});
  });
  tests.forEach(function(test){
    it(test.message, function () {
      serviceConfigProperty.initialValue(test.localDB);
      expect(serviceConfigProperty.get('value')).to.equal(test.expected);
    });
  });
});

describe('#initialValue - hive_database', function () {

  var cases = [
      {
        alwaysEnableManagedMySQLForHive: true,
        currentStateName: '',
        isManagedMySQLForHiveEnabled: false,
        receivedValue: 'New MySQL Database',
        value: 'New MySQL Database',
        options: [
          {
            displayName: 'New MySQL Database'
          }
        ],
        hidden: false
      },
      {
        alwaysEnableManagedMySQLForHive: false,
        currentStateName: 'configs',
        isManagedMySQLForHiveEnabled: false,
        receivedValue: 'New MySQL Database',
        value: 'New MySQL Database',
        options: [
          {
            displayName: 'New MySQL Database'
          }
        ],
        hidden: false
      },
      {
        alwaysEnableManagedMySQLForHive: false,
        currentStateName: '',
        isManagedMySQLForHiveEnabled: true,
        receivedValue: 'New MySQL Database',
        value: 'New MySQL Database',
        options: [
          {
            displayName: 'New MySQL Database'
          }
        ],
        hidden: false
      },
      {
        alwaysEnableManagedMySQLForHive: false,
        currentStateName: '',
        isManagedMySQLForHiveEnabled: false,
        receivedValue: 'New MySQL Database',
        value: 'Existing MySQL Database',
        options: [
          {
            displayName: 'New MySQL Database'
          }
        ],
        hidden: true
      },
      {
        alwaysEnableManagedMySQLForHive: false,
        currentStateName: '',
        isManagedMySQLForHiveEnabled: false,
        receivedValue: 'New PostgreSQL Database',
        value: 'New PostgreSQL Database',
        options: [
          {
            displayName: 'New MySQL Database'
          }
        ],
        hidden: true
      }
    ],
    title = 'value should be set to {0}';

  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create({
      name: 'hive_database'
    });
  });

  afterEach(function() {
    App.get.restore();
  });

  cases.forEach(function (item) {
    it(title.format(item.value), function () {
      sinon.stub(App, 'get')
        .withArgs('supports.alwaysEnableManagedMySQLForHive').returns(item.alwaysEnableManagedMySQLForHive)
        .withArgs('router.currentState.name').returns(item.currentStateName)
        .withArgs('isManagedMySQLForHiveEnabled').returns(item.isManagedMySQLForHiveEnabled);
      serviceConfigProperty.set('value', item.receivedValue);
      serviceConfigProperty.set('options', item.options);
      serviceConfigProperty.initialValue({});
      expect(serviceConfigProperty.get('value')).to.equal(item.value);
      expect(serviceConfigProperty.get('options').findProperty('displayName', 'New MySQL Database').hidden).to.equal(item.hidden);
    });
  });

});
