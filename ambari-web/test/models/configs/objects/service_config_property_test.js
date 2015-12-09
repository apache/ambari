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
var configPropertyHelper = require('utils/configs/config_property_helper');

require('models/configs/objects/service_config_category');
require('models/configs/objects/service_config_property');

var serviceConfigProperty,
  serviceConfigPropertyInit,
  configsData = [
    Ember.Object.create({
      category: 'c0',
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
      category: 'c1',
      isValid: false,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c0',
      isValid: true,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c1',
      isValid: false,
      isVisible: false
    })
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
  overridableFalseData = [
    {
      isOverridable: false
    },
    {
      isEditable: false,
      overrides: configsData[0].overrides
    },
    {
      displayType: 'componentHost'
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
        value: 'value',
        recommendedValue: 'recommended'
      },
      result: {
        retypedPassword: 'value',
        recommendedValue: ''
      }
    },
    {
      initial: {
        value: '',
        savedValue: 'default',
        recommendedValue: 'recommended'
      },
      result: {
        value: '',
        recommendedValue: 'recommended'
      }
    },
    {
      initial: {
        value: null,
        savedValue: 'default',
        recommendedValue: 'recommended'
      },
      result: {
        value: 'default',
        recommendedValue: 'recommended'
      }
    }
  ],
  notDefaultFalseData = [
    {
      isEditable: false
    },
    {
      savedValue: null
    },
    {
      value: 'value',
      savedValue: 'value'
    }
  ],
  notDefaultTrueData = {
    isEditable: true,
    value: 'value',
    savedValue: 'default'
  },
  types = ['componentHost', 'componentHosts', 'radio button'],
  classCases = [
    {
      initial: {
        displayType: 'checkbox'
      },
      viewClass: App.ServiceConfigCheckbox
    },
    {
      initial: {
        displayType: 'checkbox',
        dependentConfigPattern: 'somPattern'
      },
      viewClass: App.ServiceConfigCheckboxWithDependencies
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
        displayType: 'componentHost'
      },
      viewClass: App.ServiceConfigMasterHostView
    },
    {
      initial: {
        displayType: 'componentHosts'
      },
      viewClass: App.ServiceConfigComponentHostsView
    },
    {
      initial: {
        displayType: 'componentHosts'
      },
      viewClass: App.ServiceConfigComponentHostsView
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
    },
    {
      initial: {
        unit: false,
        displayType: 'supportTextConnection'
      },
      viewClass: App.checkConnectionView
    }
  ];

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

  describe('#undoAvailable', function () {

    Em.A([
      {
        cantBeUndone: true,
        isNotDefaultValue: true,
        e: false
      },
      {
        cantBeUndone: false,
        isNotDefaultValue: true,
        e: true
      },
      {
        cantBeUndone: true,
        isNotDefaultValue: false,
        e: false
      },
      {
        cantBeUndone: false,
        isNotDefaultValue: false,
        e: false
      }
    ]).forEach(function (test) {
      it('', function () {
        serviceConfigProperty.reopen({
          cantBeUndone: test.cantBeUndone,
          isNotDefaultValue: test.isNotDefaultValue
        });
        expect(serviceConfigProperty.get('undoAvailable')).to.equal(test.e);
      });
    });

  });

  describe('#overrideIsFinalValues', function () {
    it('should be defined as empty array', function () {
      expect(serviceConfigProperty.get('overrideIsFinalValues')).to.eql([]);
    });
  });

  describe('#updateDescription', function () {

    beforeEach(function () {
      serviceConfigProperty.setProperties({
        displayType: 'password',
        description: ''
      });
    });

    it('should add extra-message to the description for `password`-configs', function () {

      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      serviceConfigProperty.updateDescription();
      expect(serviceConfigProperty.get('description')).to.contain(extraMessage);

    });

    it('should not add extra-message to the description if it already contains it', function () {

      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      serviceConfigProperty.updateDescription();
      serviceConfigProperty.updateDescription();
      serviceConfigProperty.updateDescription();
      expect(serviceConfigProperty.get('description')).to.contain(extraMessage);
      var subd = serviceConfigProperty.get('description').replace(extraMessage, '');
      expect(subd).to.not.contain(extraMessage);
    });

    it('should add extra-message to the description if description is not defined', function () {

      serviceConfigProperty.set('description', undefined);
      var extraMessage = Em.I18n.t('services.service.config.password.additionalDescription');
      serviceConfigProperty.updateDescription();
      expect(serviceConfigProperty.get('description')).to.contain(extraMessage);
    });

  });

});
