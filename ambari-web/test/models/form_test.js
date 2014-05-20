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

require('models/form');

var form,
  field,
  formField,
  resultCases = [
    {
      text: Em.I18n.t('form.saveError'),
      result: -1
    },
    {
      text: Em.I18n.t('form.saveSuccess'),
      result: 1
    },
    {
      text: '',
      result: 0
    }
  ],
  displayTypeCases = [
    {
      type: 'checkbox',
      classString: 'Checkbox'
    },
    {
      type: 'select',
      classString: 'Select'
    },
    {
      type: 'textarea',
      classString: 'TextArea'
    },
    {
      type: 'password',
      classString: 'TextField'
    },
    {
      type: 'hidden',
      classString: 'TextField'
    }
  ],
  hiddenCases = [
    {
      displayType: 'password',
      type: 'hidden',
      value: false
    },
    {
      displayType: 'hidden',
      type: 'hidden',
      value: true
    }
  ],
  expectError = function (message) {
    formField.validate();
    expect(formField.get('errorMessage')).to.equal(message);
  };

describe('App.Form', function () {

  beforeEach(function () {
    form = App.Form.create({
      fieldsOptions: [
        {
          name: 'field0',
          value: 'value0',
          isRequired: false
        }
      ]
    });
    field = form.get('fields').objectAt(0);
  });

  describe('#fields', function () {
    it('should get data from formFields', function () {
      var fields = form.get('fields');
      expect(fields).to.have.length(1);
      expect(field.get('name')).to.equal('field0');
    });
  });

  describe('#field', function () {
    it('should get data from formFields', function () {
      var field0 = form.get('field.field0');
      expect(form.get('field')).to.not.be.empty;
      expect(field0.get('name')).to.equal('field0');
      expect(field0.get('form')).to.eql(form);
    });
  });

  describe('#getField', function () {
    it('should get field0', function () {
      expect(form.getField('field0')).to.eql(form.get('field.field0'));
    });
    it('should be empty', function () {
      form.set('fields', []);
      expect(form.getField()).to.be.empty;
    });
  });

  describe('#isValid', function () {
    it('should be true', function () {
      field.set('isRequired', false);
      expect(form.isValid()).to.be.true;
    });
    it('should be false', function () {
      field.setProperties({
        isRequired: true,
        value: ''
      });
      expect(form.isValid()).to.be.false;
    });
  });

  describe('#updateValues', function () {
    it('should update field0 value', function () {
      form.set('object', Em.Object.create({field0: 'value0upd'}));
      expect(field.get('value')).to.equal('value0upd');
    });
    it('should empty password value', function () {
      field.set('displayType', 'password');
      form.set('object', Em.Object.create());
      expect(field.get('value')).to.be.empty;
    });
    it('should clear values', function () {
      form.set('object', []);
      expect(field.get('value')).to.be.empty;
    });
  });

  describe('#clearValues', function () {
    it('should clear values', function () {
      var field0 = form.get('fields').objectAt(0);
      field0.set('value', 'value0');
      form.clearValues();
      expect(field0.get('value')).to.be.empty;
    });
  });

  describe('#resultText', function () {
    resultCases.forEach(function (item) {
      it('should be ' + item.text, function () {
        form.set('result', item.result);
        expect(form.get('resultText')).to.equal(item.text);
      });
    });
  });

});

describe('App.FormField', function () {

  beforeEach(function () {
    formField = App.FormField.create();
  });

  describe('#isValid', function () {
    it('should be true', function () {
      expect(formField.get('isValid')).to.be.true;
    });
    it('should be false', function () {
      formField.set('errorMessage', 'error');
      expect(formField.get('isValid')).to.be.false;
    });
  });

  describe('#viewClass', function () {
    displayTypeCases.forEach(function (item) {
      it('should be ' + item.classString, function () {
        formField.set('displayType', item.type);
        expect(formField.get('viewClass').toString()).to.contain(item.classString);
      });
    });
  });

  describe('#validate', function () {
    it('should return error message', function () {
      formField.set('isRequired', true);
      expectError('This is required');
    });
    it('should return empty error message', function () {
      formField.set('isRequired', false);
      expectError('');
      formField.set('value', 'value');
      expectError('');
    });
  });

  describe('#isHiddenField', function () {
    hiddenCases.forEach(function (item) {
      it('should be ' + item.value, function () {
        formField.setProperties(item);
        expect(formField.get('isHiddenField')).to.equal(item.value);
      });
    });
  });

});
