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

// move this to models cause some errors
App.Form = Em.View.extend({
  /**
   * generating fields from fieldsOptions
   */
  classNames:["form-horizontal"],
  attributeBindings: ['autocomplete'],
  autocomplete: 'off',
  i18nprefix:'form.',
  fields:[],
  field:{},
  messages:[],
  object:false,
  result:0, // save result var (-1 - error; 0 - init; 1 - success)
  templateName:require('templates/common/form'),
  tagName:'form',

  init:function () {
    var thisForm = this;
    if (!this.fields.length) {
      this.fieldsOptions.forEach(
        function (options) {
          var field = App.FormField.create(options);
          field.set('form', thisForm);
          thisForm.fields.push(field);
          thisForm.set("field." + field.get('name'), field);
        }
      );
    }
    this._super();
  },

  /**
   * get field of form by name
   * @param name
   * @return {Object}
   */
  getField: function (name) {
    return this.get('fields').findProperty('name', name);
  },

  isValid:function () {
    var isValid = true;
    $.each(this.fields, function () {
      this.validate();
      if (!this.get('isValid')) {
        isValid = false;
        console.warn(this.get('name') + " IS INVALID : " + this.get('errorMessage'));
      }
    });

    return isValid;
  },

  updateValues:function () {
    var object = this.get('object');
    if (object instanceof Em.Object) {
      $.each(this.fields, function () {
        this.set('value', (this.get('displayType') == 'password') ? '' : object.get(this.get('name')));
      });
    } else {
      this.clearValues();
    }

  }.observes("object"),

  /**
   * reset values to default of every field in the form
   */
  clearValues: function () {
    this.get('fields').forEach(function (field) {
      var value = (field.get('defaultValue') === undefined) ? '' : field.get('defaultValue');
      field.set('value', value);
    }, this);
  },

  visibleFields:function () {
    return this.get('fields').filterProperty('isHiddenField', false);
  }.property('fields'),

  resultText:function () {
    var text = "";
    switch (this.get('result')) {
      case -1:
        text = this.t("form.saveError");
        break;
      case 1:
        text = this.t("form.saveSuccess");
        break;
    }

    return text;
  }.property('result')
});

App.FormField = Em.Object.extend({ // try to realize this as view
  name:'',
  displayName:'',
//  defaultValue:'', NOT REALIZED YET
  description:'',
  disabled:false,
  displayType:'string', // string, digits, number, directories, textarea, checkbox
  disableRequiredOnPresent:false,
  errorMessage:'',
  warnMessage:'',
  form:false,
  isRequired:true, // by default a config property is required
  unit:'',
  value:'',

  observeValue:function () {

    if (this.get('displayType') == 'hidden')
      console.warn(" FORM FIELD VALUE: ", this.get('value'));

  }.observes('value'),

  isValid:function () {
    return this.get('errorMessage') === '';
  }.property('errorMessage'),

  viewClass:function () {
    var options = {};
    var element = Em.TextField;
    switch (this.get('displayType')) {
      case 'checkbox':
        element = Em.Checkbox;
        options.checkedBinding = "value";
        break;
      case 'select':
        element = Em.Select;
        options.content = this.get('values');
        options.valueBinding = "value";
        options.optionValuePath = "content.value";
        options.optionLabelPath = "content.label";
        break;
      case 'password':
        options['type'] = 'password';
        break;
      case 'textarea':
        element = Em.TextArea;
        break;
      case 'hidden':
        options.type = "hidden";
        break;
    }

    return element.extend(options);
  }.property('displayType'),

  validate:function () {
    var value = this.get('value');
    var isError = false;
    this.set('errorMessage', '');

    if (this.get('isRequired') && (typeof value === 'string' && value.trim().length === 0)) {
      this.set('errorMessage', 'This is required');
      isError = true;
    }

    if (typeof value === 'string' && value.trim().length === 0) { // this is not to validate empty field.
      isError = true;
    }

    if (!isError) {
      this.set('errorMessage', '');
    }
  },

  isHiddenField:function () {
    return this.get('displayType') == 'hidden';
  }.property('type')
});
