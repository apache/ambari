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
    },
    {
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
  types = ['componentHost', 'componentHosts', 'radio button'];


function getProperty() {
  return App.ServiceConfigProperty.create();
}

describe('App.ServiceConfigProperty', function () {

  beforeEach(function () {
    serviceConfigProperty = getProperty();
  });

  App.TestAliases.testAsComputedFirstNotBlank(getProperty(), 'placeholder', ['placeholderText', 'savedValue']);

  App.TestAliases.testAsComputedAnd(getProperty(), 'hideFinalIcon', ['!isFinal', 'isNotEditable']);

  describe('#isPropertyOverridable', function () {
    overridableFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.false;
      });
    });
    overridableTrueData.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.true;
      });
    });
  });

  describe('#isOverridden', function () {
    overriddenFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isOverridden')).to.be.false;
      });
    });
    overriddenTrueData.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isOverridden')).to.be.true;
      });
    });
  });

  describe('#isRemovable', function () {
    removableFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isRemovable')).to.be.false;
      });
    });
    removableTrueData.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isRemovable')).to.be.true;
      });
    });
  });

  describe('#init', function () {
    initPropertyData.forEach(function (item) {
      describe('should set initial data for ' + JSON.stringify(item), function () {
        beforeEach(function () {
          serviceConfigPropertyInit = App.ServiceConfigProperty.create(item.initial);
        });
        Em.keys(item.result).forEach(function (prop) {
          it(prop, function () {
            expect(serviceConfigPropertyInit.get(prop)).to.equal(item.result[prop]);
          });
        });
      });
    });
  });

  describe('#isNotDefaultValue', function () {
    notDefaultFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
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

  describe('#overrideIsFinalValues', function () {
    it('should be defined as empty array', function () {
      expect(serviceConfigProperty.get('overrideIsFinalValues')).to.eql([]);
    });
  });

});
