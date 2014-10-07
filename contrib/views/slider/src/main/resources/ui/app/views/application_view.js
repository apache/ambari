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

    showCreateAppButton: function () {
      if (this.get('controller.hasConfigErrors')) {
        return false;
      }
      var currentPath = this.get('controller.currentPath');
      return currentPath && (currentPath == 'slider_apps.index' || currentPath.indexOf('slider_apps.createAppWizard') != -1);
    }.property('controller.currentPath', 'controller.hasConfigErrors'),

    /**
     * Set <code>popover</code> template
     * @method sliderConfigsChecker
     */
    sliderConfigsChecker: function() {
      var template = this.createChildView(App.SliderTitleTooltipView, {
        content: App.SliderApp.store.all('sliderConfig')
      });
      $('#slider-title').data('bs.popover').options.content = template.renderToBuffer().string();
    }.observes('App.mapperTime'),

    didInsertElement: function () {
      this.createPopover();
    },

    /**
     * Create popover for Slider Title
     * @method createPopover
     */
    createPopover: function() {
      $('#slider-title').popover('destroy').popover({
        trigger: 'hover',
        placement: 'bottom',
        title: App.get('label'),
        html: true
      });
      this.sliderConfigsChecker();
    }.observes('App.label')

  })

});

App.SliderTitleTooltipView = Em.View.extend({
  templateName: 'slider_title_tooltip'
});
