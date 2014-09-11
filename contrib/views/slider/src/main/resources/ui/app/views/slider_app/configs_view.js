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

App.SliderAppConfigsView = Ember.View.extend(App.WithPanels, {

  /**
   * List of configs group by categories
   * @type {Object[]}
   */
  configsByCategories: Em.A([]),

  /**
   * Observer for model's configs
   * Updates <code>configsByCategories</code>
   * @method configsObserver
   */
  configsObserver: function() {
    var model = this.get('controller.content'),
      configs = model.get('.configs'),
      configsByCategories = this.get('configsByCategories'),
      hiddenCategories = model.get('hiddenCategories');
    Em.keys(configs).forEach(function (site) {
      if (configsByCategories.mapBy('name').contains(site)) {
        var c = configsByCategories.findBy('name', site);
        c.set('configs', model.mapObject(configs[site]));
        c.set('isVisible', !hiddenCategories.contains(site));
      }
      else {
        configsByCategories.pushObject(Em.Object.create({
          name: site,
          configs: model.mapObject(configs[site]),
          isVisible: !hiddenCategories.contains(site)
        }));
      }
    });
  }.observes('controller.content.configs.@each'),

  didInsertElement: function() {
    this.addCarets();
  }

});
