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
var validator = require('utils/validator');

// move this to models cause some errors
App.Form = Em.View.extend({
  /**
   * generating fields from fieldsOptions
   */
  classNames:["form-horizontal"],
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

  getField:function (name) {
    var field = false;
    $.each(this.fields, function () {
      if (this.get('name') == name) {
        return field = this;
      }
    });
    return field;
  },

  isValid:function () {
    var isValid = true;
    $.each(this.fields, function () {
      this.validate();
      if (!this.get('isValid')) {
        isValid = false;
        console.warn(this.get('name') + " IS INVALID : " + this.get('errorMessage'));
      }
    })

    return isValid;
  },

  isObjectNew:function () {
    var object = this.get('object');
    if(object instanceof App.User){
      return false;
    }
    return !(object instanceof DS.Model && object.get('id'));
  }.property("object"),

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
   *
   */

  getValues:function () {
    var values = {};
    $.each(this.get('fields'), function () {
      if (!(this.get('displayType') == 'password' && validator.empty(this.get('value')))) { // if this is not empty password field
        values[this.get('name')] = this.get('value');
      }
    });
    return values;
  },

  clearValues:function () {
    $.each(this.fields, function () {
      this.set('value', '');
    });
  },

  /**
   * need to refactor for integration
   * @return {Boolean}
   */
  save:function () {
    var thisForm = this;
    var object = this.get('object');
    if (!this.get('isObjectNew')) {
      $.each(this.getValues(), function (i, v) {
        object.set(i, v);
      });
    } else {
      if (this.get('className'))
        App.store.createRecord(this.get('className'), this.getValues())
      else
        console.log("Please define class name for your form " + this.constructor);
    }

    App.store.commit();
    this.set('result', 1);

    return true;
  },

  visibleFields:function () {
    var fields = this.get('fields');
    var visible = [];
    fields.forEach(function (field) {
      if (!field.get('isHiddenField')) {
        visible.push(field);
      }
    });
    return visible;
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
  }.property('result'),

  saveButtonText:function () {
    return Em.I18n.t(this.get('i18nprefix') + (this.get('isObjectNew') ? "create" : "save"));
  }.property('isObjectNew')

//  not recommended
//  cancelButtonText:function () {
//    return Em.I18n.t(this.get('i18nprefix') + 'cancel').property();
//  }
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
    var digitsRegex = /^\d+$/;
    var numberRegex = /^[-,+]?(?:\d+|\d{1,3}(?:,\d{3})+)?(?:\.\d+)?$/;
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
      switch (this.get('validator')) {
        case 'ipaddress':
          if (!validator.isIpAddress(value) && !validator.isDomainName(value)) {
            isError = true;
            this.set('errorMessage', Em.I18n.t("form.validator.invalidIp"));
          }
          break;
        case 'passwordRetype':
          var form = this.get('form');
          var passwordField = form.getField('password');
          if (passwordField.get('isValid')
            && (passwordField.get('value') != this.get('value'))
            && passwordField.get('value') && this.get('value')
            ) {
            this.set('errorMessage', "Passwords are different");
            isError = true;
          }
          break;
        default:
          break;
      }

      switch (this.get('displayType')) {
        case 'digits':
          if (!digitsRegex.test(value)) {
            this.set('errorMessage', 'Must contain digits only');
            isError = true;
          }
          break;
        case 'number':
          if (!numberRegex.test(value)) {
            this.set('errorMessage', 'Must be a valid number');
            isError = true;
          }
          break;
        case 'directories':
          break;
        case 'custom':
          break;
        case 'password':
          break;
      }
    }
    if (!isError) {
      this.set('errorMessage', '');
    }
  },

  isHiddenField:function () {
    return this.get('displayType') == 'hidden';
  }.property('type')
});