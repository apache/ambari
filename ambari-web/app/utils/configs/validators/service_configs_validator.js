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
   * @return {string|null}
   */
  validatorLessThenDefaultValue: function(config) {
    var defaultValue = this.get('recommendedDefaults')[config.get('name')];
    var currentValue = parseInt(config.get('value').toString().replace( /\D+/g, ''));
    if (!defaultValue) {
      return null;
    }
    defaultValue = parseInt(defaultValue.toString().replace( /\D+/g, ''));
    if (defaultValue && currentValue &&  currentValue < defaultValue) {
      return "Value is less than the recommended default of " + defaultValue;
    }
    return null;
  },

  /**
   * Check if provided <code>config.value</code> is less than <code>recommendedDefaults</code> or <code>config.defaultValue</code>
   * Value looks like "-Xmx****m"
   * @param {object} config
   * @return {string|null}
   */
  validateXmxValue: function(config) {
    var recomendedDefault = this.get('recommendedDefaults')[config.get('name')];
    var defaultValueRaw = Em.isNone(recomendedDefault) ? config.get('defaultValue') : recomendedDefault;
    Em.assert('validateXmxValue: Config\'s default value can\'t be null or undefined', !Em.isNone(defaultValueRaw));
    var currentValueRaw = config.get('value');
    if (!this._checkXmxValueFormat(currentValueRaw)) {
      return 'Invalid value format';
    }
    var currentValue = this._formatXmxSizeToBytes(this._getXmxSize(currentValueRaw));
    var defaultValueXmx = this._getXmxSize(defaultValueRaw);
    var defaultValue = this._formatXmxSizeToBytes(defaultValueXmx);
    if (currentValue < defaultValue) {
      return "Value is less than the recommended default of -Xmx" + defaultValueXmx;
    }
    return null;
  },
  /**
   * Verify if provided value has proper format (should be like "-Xmx***(b|k|m|g|p|t|B|K|M|G|P|T)")
   * @param  {string} value
   * @returns {bool}
   * @private
   */
  _checkXmxValueFormat: function(value) {
    var regex = /(^|\s)\-Xmx(\d+)(b|k|m|g|p|t|B|K|M|G|P|T)?(\s|$)/;
    if (!regex.test(value)) {
      return false;
    }
    // "-Xmx" can be only one
    return value.match(/\-Xmx/ig).length == 1;
  },
  /**
   * Parse Xmx size from raw value
   * @param {string} value
   * @returns {string}
   * @private
   */
  _getXmxSize: function(value) {
    var regex = /\-Xmx(\d+)(.?)/;
    var result = regex.exec(value);
    if (result.length > 1) {
      // result[2] - is a space or size formatter (b|k|m|g etc)
      return result[1] + result[2].toLowerCase();
    }
    return result[1];
  },
  /**
   * Calculate bytes size from value
   * @param {string} value
   * @returns {int}
   * @private
   */
  _formatXmxSizeToBytes: function(value) {
    value = value.toLowerCase();
    if (value.length == 0) {
      return 0;
    }
    var modifier = value[value.length - 1];
    if (modifier == ' ' || "0123456789".indexOf(modifier) != -1) {
      modifier = 'b';
    }
    var m = 1;
    switch (modifier) {
      case 'b': m = 1; break;
      case 'k': m = 1024; break;
      case 'm': m = 1024 * 1024; break;
      case 'g': m = 1024 * 1024 * 1024; break;
      case 't': m = 1024 * 1024 * 1024 * 1024; break;
      case 'p': m = 1024 * 1024 * 1024 * 1024 * 1024; break;
    }
    var result = parseInt(value.replace(modifier, '').trim());
    result *= m;
    return result;
  }

});
