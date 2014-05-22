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

App.SliderAppController = Ember.ObjectController.extend({

  /**
   * List of Slider App tabs
   * @type {{title: string, linkTo: string}[]}
   */
  sliderAppTabs: Ember.A([
    Ember.Object.create({title: Ember.I18n.t('common.summary'), linkTo: 'slider_app.summary'}),
    Ember.Object.create({title: Ember.I18n.t('common.configs'), linkTo: 'slider_app.configs'})
  ]),

  /**
   * List of available for model actions
   * Based on <code>model.status</code>
   * @type {Ember.Object[]}
   */
  availableActions: function() {
    var actions = Em.A([]),
      status = this.get('model.status');
    if ('RUNNING' === status) {
      actions.pushObject({
        title: 'Freeze',
        confirm: true
      });
    }
    if ('FINISHED' !== status) {
      actions.push({
        title: 'Flex',
        confirm: true
      });
    }
    if ('FROZEN' === status) {
      actions.pushObjects([
        {
          title: 'Thaw',
          confirm: false
        },
        {
          title: 'Destroy',
          confirm: true
        }
      ]);
    }
    return actions;
  }.property('model.status'),

  quickLinks: function() {
    return this.get('content').get('quickLinks');
  }.property('content.quickLinks')
});
