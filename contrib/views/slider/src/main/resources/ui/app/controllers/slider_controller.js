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
 * Load Slider-properties.
 * After this set <code>App.sliderConfig</code>-models and enable/disable Slider
 * @type {Ember.Controller}
 */
App.SliderController = Ember.Controller.extend({

  /**
   *  Load resources on controller initialization
   * @method initResources
   */
  initResources: function () {
    this.getParametersFromViewProperties();
  },

  /**
   * Get Slider properties from View-parameters (set in the Ambari Admin View)
   * If parameters can't be found, use Ambari-configs to populate Slider properties
   * @returns {$.ajax}
   * @method getParametersFromViewProperties
   */
  getParametersFromViewProperties: function () {
    return App.ajax.send({
      name: 'slider.getViewParams',
      sender: this,
      success: 'getParametersFromViewPropertiesSuccessCallback',
      error: 'finishSliderConfiguration'
    });
  },

  /**
   * Check if Slider-properties exist
   * If exist - set Slider properties using view-configs
   * If not - get Ambari configs to populate Slider properties
   * @param {object} data
   * @method getParametersFromViewPropertiesSuccessCallback
   */
  getParametersFromViewPropertiesSuccessCallback: function (data) {
    var properties = Em.get(data, 'ViewInstanceInfo.properties'),
      initialValuesToLoad = this.get('initialValuesToLoad'),
      sliderConfigs = App.SliderApp.store.all('sliderConfig'),
      self = this;
    sliderConfigs.forEach(function (model) {
      var key = model.get('viewConfigName');
      model.set('value', properties[key]);
    });
    self.finishSliderConfiguration();
  },

  /**
   * After all Slider-configs are loaded, application should check self status
   * @method finishSliderConfiguration
   */
  finishSliderConfiguration: function () {
    //check if all services exist
    var errors = [];
    App.SliderApp.store.all('sliderConfig').forEach(function (model) {
      if (Em.isEmpty(model.get('value')) && model.get('required')) {
        errors.push(Em.I18n.t('error.config_is_empty').format(model.get('viewConfigName')));
      }
    });
    errors.uniq();
    App.setProperties({
      viewErrors: errors,
      viewEnabled: errors.length === 0,
      mapperTime: new Date().getTime()
    });
  }

});
