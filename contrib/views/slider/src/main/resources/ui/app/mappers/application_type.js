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

/**
 * Mapper for <code>App.SliderAppType</code> and <code>App.SliderAppComponent</code> models
 * For nested models:
 * <ul>
 *   <li>
 *     Define property (P1) started with '$' in <code>map</code> (example - $components).
 *   </li>
 *   <li>
 *     Define property componentsMap (P1 without '$' and with suffix 'Map').
 *     It is used as map for nested models
 *   </li>
 *   <li>
 *     Define property componentsParentField (P1 without '$' and with suffix 'ParentField').
 *     It  is used as property in nested model to link it with parent model
 *   </li>
 * </ul>
 * @type {App.Mapper}
 */
App.ApplicationTypeMapper = App.Mapper.create({

  /**
   * Map for parsing JSON received from server
   * Format:
   *  <code>
   *    {
   *      key1: 'path1',
   *      key2: 'path2',
   *      key3: 'path3'
   *    }
   *  </code>
   *  Keys - names for properties in App
   *  Values - pathes in JSON
   * @type {object}
   */
  map: {
    id: 'id',
    configs: 'typeConfigs',
    typeName: 'typeName',
    typeVersion: 'typeVersion',
    index: 'id',
    description: 'typeDescription',
    version: 'typeVersion',
    /**
     * Map array to nested models
     * Use <code>('$components').replace('$', '') + 'Map'</code> property as map
     * Use <code>('$components').replace('$', '') + 'Model'</code> property as model to save data
     */
    $components: 'typeComponents'
  },

  /**
   * Map for <code>App.SliderAppTypeComponent</code>
   * @type {object}
   */
  componentsMap: {
    id: 'id',
    name: 'name',
    displayName: 'displayName',
    defaultNumInstances: 'instanceCount',
    defaultYARNMemory: 'yarnMemory',
    defaultYARNCPU: 'yarnCpuCores',
    priority: 'priority'
  },

  /**
   * Nested model name - <code>App.SliderAppTypeComponent</code>
   * @type {string}
   */
  componentsModel: 'sliderAppTypeComponent',

  /**
   * Field in <code>App.SliderAppTypeComponent</code> with parent model link
   * @type {string}
   */
  componentsParentField: 'appType',

  /**
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function() {
    console.log('App.ApplicationTypeMapper loading data');
    return App.ajax.send({
      name: 'mapper.applicationTypes',
      sender: this,
      success: 'parse'
    });
  },

  /**
   * Parse loaded data according to <code>map</code>
   * Load <code>App.SliderAppType</code> models
   * @param {object} data received from server data
   * @method parse
   */
  parse: function(data) {
    var map = this.get('map'),
      app_types = [],
      self = this;
    data.items.forEach(function(app_type) {
      var model = {};
      Ember.keys(map).forEach(function(key) {
        // Property should be parsed as array of nested models
        if ('$' == key[0]) {
          var k = key.replace('$', '');
          var components = self.parseNested(Ember.get(app_type, map[key]), k, app_type.id);
          // save nested models and then link them with parent model
          App.SliderApp.store.pushMany(self.get(k + 'Model'), components);
          Ember.set(model, k, components.mapProperty('id'));
        }
        else {
          Ember.set(model, key, Ember.getWithDefault(app_type, map[key], ''));
        }
      });
      app_types.pushObject(model);
    });
    App.SliderApp.store.pushMany('sliderAppType', app_types);
  },

  /**
   * Parse array of objects as list of nested models
   * @param {object[]} data data to parse
   * @param {string} k property name
   * @param {string} parentId parent model's id
   * @return {object[]} mapped models
   * @method parseNested
   */
  parseNested: function(data, k, parentId) {
    var models = [],
      map = this.get(k + 'Map'),
      parentField = this.get(k + 'ParentField');
    data.forEach(function(item) {
      var model = {id: item.id};
      model[parentField] = parentId; // link to parent model
      Ember.keys(map).forEach(function(key) {
        Ember.set(model, key, Ember.getWithDefault(item, map[key], ''));
      });
      models.pushObject(model);
    });
    return models;
  }

});
