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


var App = require('app');

App.ReassignMasterWizardStep5View = Em.View.extend({

  bodyText: function () {
    var componentDir = this.get('controller.content.componentDir');
    var sourceHost = this.get('controller.content.reassignHosts.source');
    var targetHost = this.get('controller.content.reassignHosts.target');
    return  Em.I18n.t('services.reassign.step5.body.' + this.get('controller.content.reassign.component_name').toLowerCase()).format(componentDir, sourceHost, targetHost);
  }.property('controller.content.reassign.component_name', 'controller.content.componentDir', 'controller.content.masterComponentHosts', 'controller.content.reassign.host_id'),

  templateName: require('templates/main/service/reassign/step5')
});
