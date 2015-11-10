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

/**
 * Basic class for config-initializers
 * Each child should fill <code>initializers</code> or <code>uniqueInitializers</code> and <code>initializerTypes</code>
 * Usage:
 * <pre>
 * var myCoolInitializer = App.ConfigInitializerClass.create({
 *  initializers: {
 *    'my-cool-config': {
 *      type: 'some_type'
 *    }
 *  },
 *
 *  initializerTypes: {
 *    some_type: {
 *      method: '_initAsCool'
 *    }
 *  },
 *
 *  _initAsCool: function (configProperty, localDB, dependencies, initializer) {
 *    // some magic
 *    return configProperty;
 *  }
 * });
 *
 * var myConfig = { name: 'my-cool-config' };
 * var localDB = getLocalDB();
 * var dependencies = {};
 * myCoolInitializer.initialValue(myConfig, localDB, dependencies);
 * </pre>
 *
 * @type {ConfigInitializerClass}
 */
App.ConfigInitializerClass = Em.Object.extend({

  /**
   * Map with configurations for config initializers
   * It's used only for initializers which are common for some configs (if not - use <code>uniqueInitializers</code>-map)
   * Key {string} configProperty-name
   * Value {object|object[]} settings for initializer
   *
   * @type {object}
   */
  initializers: {},

  /**
   * Map with initializers types
   * Doesn't contain unique initializes, only common are included
   * Key: id
   * Value: object with method-name (prefer to start method-name with '_init' or '_initAs')
   * Each method here is called with arguments equal to <code>initialValue</code>-call args
   * Initializer-settings are added as last argument
   *
   * @type {object}
   */
  initializerTypes: {},

  /**
   * Map with initializers that are used only for one config (are unique)
   * Key: configProperty-name
   * Value: method-name
   * Every method from this map is called with same arguments as <code>initialValue</code> is (prefer to start method-name with '_init' or '_initAs')
   *
   * @type {object}
   */
  uniqueInitializers: {},

  /**
   * Wrapper for common initializers
   * Execute initializer if it is a function or throw an error otherwise
   *
   * @param {Object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _defaultInitializer: function (configProperty, localDB, dependencies) {
    var args = [].slice.call(arguments);
    var self = this;
    var initializers = this.get('initializers');
    var initializerTypes = this.get('initializerTypes');
    var initializer = initializers[Em.get(configProperty, 'name')];
    if (initializer) {
      initializer = Em.makeArray(initializer);
      initializer.forEach(function (init) {
        var _args = [].slice.call(args);
        var type = initializerTypes[init.type];
        // add initializer-settings
        _args.push(init);
        var methodName = type.method;
        Em.assert('method-initializer is not a function ' + methodName, 'function' === Em.typeOf(self[methodName]));
        configProperty = self[methodName].apply(self, _args);
      });
    }
    return configProperty;
  },

  /**
   * Entry-point for any config's value initializing
   * Before calling it, be sure that <code>initializers</code> or <code>uniqueInitializers</code>
   * contains record about needed configs
   *
   * @param {Object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   */
  initialValue: function (configProperty, localDB, dependencies) {
    var configName = Em.get(configProperty, 'name');
    var initializers = this.get('initializers');

    var initializer = initializers[configName];
    if (initializer) {
      return this._defaultInitializer(configProperty, localDB, dependencies);
    }

    var uniqueInitializers = this.get('uniqueInitializers');
    var uniqueInitializer = uniqueInitializers[configName];
    if (uniqueInitializer) {
      var args = [].slice.call(arguments);
      return this[uniqueInitializer].apply(this, args);
    }

    return configProperty;
  }

});