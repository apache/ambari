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
App.SliderController = Ember.Controller.extend(App.RunPeriodically, {

  /**
   *  Load resources on controller initialization
   * @method initResources
   */
  initResources: function () {
    this.getParametersFromViewProperties();
  },

  /**
   * Load Slider display information
   * @returns {$.ajax}
   * @method getViewDisplayParameters
   */
  getViewDisplayParameters: function() {
    return App.ajax.send({
      name: 'slider.getViewParams',
      sender: this,
      success: 'getViewDisplayParametersSuccessCallback'
    });
  },

  /**
   * Accessing /resources/status initializes the view internals
   * with the latest configs. This will help the view in staying
   * updated and recover from previous config issues.
   */
  touchViewStatus: function() {
    return App.ajax.send({
      name: 'slider.getViewParams.v2',
      sender: this
    });
  },

  /**
   * Set Slider label and description to <code>App</code> properties
   * @param {object} data
   * @method getViewDisplayParametersSuccessCallback
   */
  getViewDisplayParametersSuccessCallback: function(data) {
    App.set('description', Em.get(data, 'ViewInstanceInfo.description'));
    App.set('label', Em.get(data, 'ViewInstanceInfo.label'));
    App.set('javaHome', Em.get(data, 'ViewInstanceInfo.instance_data') && Em.get(data, 'ViewInstanceInfo.instance_data')['java.home']);
    App.set('sliderUser', Em.get(data, 'ViewInstanceInfo.instance_data') && Em.get(data, 'ViewInstanceInfo.instance_data')['slider.user']);
  },

  /**
   * Get Slider properties from View-parameters (set in the Ambari Admin View)
   * If parameters can't be found, use Ambari-configs to populate Slider properties
   * @returns {$.ajax}
   * @method getParametersFromViewProperties
   */
  getParametersFromViewProperties: function () {
    return App.ajax.send({
      name: 'slider.getViewParams.v2',
      sender: this,
      success: 'getParametersFromViewPropertiesSuccessCallback'
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
    var properties = Em.get(data, 'parameters'),
      sliderConfigs = App.SliderApp.store.all('sliderConfig');
    sliderConfigs.forEach(function (model) {
      var key = model.get('viewConfigName');
      model.set('value', properties[key]);
    });
    if (properties['view.slider.user'] != null
        && properties['view.slider.user'] != App.get('sliderUser')) {
      App.set('sliderUser', properties['view.slider.user']);
    }
    if (properties['java.home'] != null
        && properties['java.home'] != App.get('javaHome')) {
      App.set('javaHome', properties['java.home']);
    }
    this.initMetricsServerProperties();
    this.finishSliderConfiguration(data);
  },

  /**
   * initialize properties of Metrics Server that are required by Slider View
   * @method initMetricsServerProperties
   */
  initMetricsServerProperties: function () {
    var sliderConfigs = App.SliderApp.store.all('sliderConfig'),
      metricsPort = sliderConfigs.findBy('viewConfigName', 'site.global.metric_collector_port'),
      metricsHost = sliderConfigs.findBy('viewConfigName', 'site.global.metric_collector_host'),
      metricsLibPath = sliderConfigs.findBy('viewConfigName', 'site.global.metric_collector_lib');
    App.set('metricsHost', metricsHost.get('value'));
    App.set('metricsPort', metricsPort.get('value'));
    App.set('metricsLibPath', metricsLibPath.get('value'));
  },

  /**
   * After all Slider-configs are loaded, application should check self status
   * @param {object} data - received from server information about current Slider-status
   * @method finishSliderConfiguration
   */
  finishSliderConfiguration: function (data) {
    App.setProperties({
      viewErrors: data.validations,
      viewEnabled: data.validations.length === 0,
      mapperTime: new Date().getTime()
    });
  }

});
