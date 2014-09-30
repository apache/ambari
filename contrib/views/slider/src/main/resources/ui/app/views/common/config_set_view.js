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

/**
 * view that display set of configs united into group
 * which can be excluded from/included into general config array via trigger(special config property)
 * @type {Em.View}
 */
App.ConfigSetView = Ember.View.extend({

  /**
   * config set data
   */
  configSet: null,

  /**
   * configs which can be included/excluded
   * @type {Array}
   */
  configs: function () {
    if (this.get('configSet.trigger.value')) {
      return this.get('configSet.configs');
    }
    return [];
  }.property('configSet.trigger.value'),

  /**
   * observe change of config values to resolve their dependencies
   * @method changeConfigValues
   */
  changeConfigValues: function () {
    var configs = this.get('configs');
    var dependencies = this.get('configSet.dependencies');

    if (configs.length > 0) {
      dependencies.forEach(function (item) {
        var origin = configs.findBy('name', item.origin);
        var dependent = configs.findBy('name', item.dependent);
        item.mapFunction(origin, dependent);
      })
    }
  }.observes('configs.@each.value')
});
