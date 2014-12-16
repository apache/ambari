/** Licensed to the Apache Software Foundation (ASF) under one
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

App.OperatingSystemsView = Em.View.extend({

  templateName: require('templates/main/admin/stack_versions/os_for_repo_versions'),

  didInsertElement: function () {
    this.set('isOsCollapsed', true);
  },

  toggleOs: function(event) {
    if (this.get('hasMoreOs')) {
      this.set('isOsCollapsed', !this.get('isOsCollapsed'));
      this.$('.operating-systems').toggle();
    }
  },

  hasMoreOs: function() {
    return this.get('content.operatingSystems.length') > 1;
  }.property('content.operatingSystems.length'),

  osText: function() {
    switch (this.get('content.operatingSystems.length')) {
      case 0:
        return Em.I18n.t("none");
        break;
      case 1:
        return this.get('content.operatingSystems').getEach('osType');
        break;
      default :
        return this.get('content.operatingSystems.length') + Em.I18n.t("common.oss");
    }
  }.property('content.operatingSystems.length'),

  labels: function() {
    return this.get('content.operatingSystems') &&
      this.get('content.operatingSystems').getEach('osType').join("<br/>");
  }.property('content.operatingSystems.length')
});