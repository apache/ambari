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

App.ApplicationView = Ember.View.extend({

  /**
   * View with popover for Slider title
   * @type {Ember.View}
   */
  SliderTitleView: Em.View.extend({

    /**
     * Popover-config
     * @type {Em.Object}
     */
    popover: Em.Object.create({
      trigger: 'hover',
      placement: 'bottom'
    }),

    /**
     * Set <code>popover</code> template
     * @method sliderConfigsChecker
     */
    sliderConfigsChecker: function() {
      var configs = App.get('sliderConfigs'),
        res = [],
        excludedConfigs = ['ambariAddress', 'clusterName'];
      if (configs) {
        Em.keys(configs).forEach(function(c) {
          if (!excludedConfigs.contains(c)) {
            res.push({name: c, value: configs[c]});
          }
        });
      }
      this.set('content', res);
      var template = this.createChildView(App.SliderTitleTooltipView, {
        content: res
      });
      this.set('popover.template', template.renderToBuffer().string());
    }.observes('App.mapperTime')

  })

});

App.SliderTitleTooltipView = Em.View.extend({
  templateName: 'slider_title_tooltip'
});
