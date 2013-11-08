/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ServiceConfigsValidator = Em.Object.extend({

  /**
   * Defaults recommended for various properties. This is to be used 
   * by this validator to validate a property. Generally this value should be 
   * set to values given by the defaults provider of the service.
   * 
   * The key is the property name, and the value is the recommended default.
   */
  recommendedDefaults: {},
  
  /**
   * Per property validators where key is config name and value 
   * is the validation function. This field is expected to be
   * overridden by extending classes.
   */
  configValidators: {},
  
  /**
   * Validate the given config property with the available  
   * {@param recommendedDefaults}. This can do cross-property
   * validations also. 
   * 
   * @param config  {App.ServiceConfigProperty}
   * @return {string}  No validation issues when <code>null</code> returned.
   */
  validateConfig: function(config) {
    var validatorFunction = this.get('configValidators')[config.get('name')];
    if (validatorFunction) {
      return this[validatorFunction](config);
    }
    return null;
  },
  
  /**
   * Check if provided <code>config.value</code> is less than <code>recommendedDefaults</code>
   * @param {object} config - configProperty name
   */
  validatorLessThenDefaultValue: function(config) {
    var defaultValue = this.get('recommendedDefaults')[config.get('name')];
    var currentValue = parseInt(config.get('value').toString().replace( /\D+/g, ''));
    if (!defaultValue) {
      return null;
    }
    defaultValue = parseInt(defaultValue.toString().replace( /\D+/g, ''));
    if (defaultValue && currentValue &&  currentValue < defaultValue) {
      return "Value is less than the recommended default of "+defaultValue;
    }
    return null;
  }

});
