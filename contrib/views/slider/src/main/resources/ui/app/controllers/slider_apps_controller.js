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

App.SliderAppsController = Ember.ArrayController.extend({
  /**
   * show modal popup that says apps currently unavailable
   */
  showUnavailableAppsPopup: function(message) {
    this.set('errorMessage', message || Em.I18n.t('slider.apps.undefined.issue'));
    Bootstrap.ModalManager.open(
      "apps-warning-modal",
      Em.I18n.t('common.warning'),
      'unavailable_apps',
      [
        Ember.Object.create({title: Em.I18n.t('ok'), dismiss: 'modal'})
      ],
      this
    );
  }
});
