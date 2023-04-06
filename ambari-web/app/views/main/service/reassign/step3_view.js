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

App.ReassignMasterWizardStep3View = Em.View.extend({

  templateName: require('templates/main/service/reassign/step3'),

  sourceHost: Em.computed.alias('controller.content.reassignHosts.source'),

  targetHost: Em.computed.alias('controller.content.reassignHosts.target'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  jdbcSetupMessage: function() {
    if(['HIVE_SERVER', 'HIVE_METASTORE', 'OOZIE_SERVER'].contains(this.get('controller.content.reassign.component_name'))) {
      if(this.get('controller.content.reassign.component_name') === 'OOZIE_SERVER' && this.get('controller.content.databaseType') === 'derby') {
        return false;
      }

      return Em.I18n.t('services.service.config.database.msg.jdbcSetup').format(this.get('controller.content.databaseType'), this.get('controller.content.databaseType'));
    }

    return false;
  }.property('controller.content.reassign.display_name', 'controller.content.databaseType')
});
