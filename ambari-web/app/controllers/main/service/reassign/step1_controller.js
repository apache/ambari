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

App.ReassignMasterWizardStep1Controller = Em.Controller.extend({
  name: 'reassignMasterWizardStep1Controller',
  databaseType: null,

  dbProperty: function() {
    var componentName = this.get('content.reassign.component_name');

    var property = null;
    switch(componentName) {
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
        property = 'javax.jdo.option.ConnectionDriverName';
        break;
      case 'OOZIE_SERVER':
        property = 'oozie.service.JPAService.jdbc.driver';
        break;
    }

    return property;
  },

  loadConfigsTags: function () {
    if(!this.get('databaseType')) {
      App.ajax.send({
        name: 'config.tags',
        sender: this,
        success: 'onLoadConfigsTags',
        error: ''
      });
    }
  },

  /**
   * construct URL parameters for config call
   * @param componentName
   * @param data
   * @return {Array}
   */
  getConfigUrlParams: function (componentName, data) {
    var urlParams = [];
    switch (componentName) {
      case 'OOZIE_SERVER':
        urlParams.push('(type=oozie-site&tag=' + data.Clusters.desired_configs['oozie-site'].tag + ')');
        break;
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
        urlParams.push('(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')');
        break;
    }
    return urlParams;
  },

  onLoadConfigsTags: function (data) {
    var urlParams = this.getConfigUrlParams(this.get('content.reassign.component_name'), data);

    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: ''
    });
  },

  onLoadConfigs: function (data) {
    var databaseProperty = data.items[0].properties[this.dbProperty()];
    var databaseType = databaseProperty.match(/MySQL|PostgreS|Oracle|Derby|MSSQL/gi)[0];

    if (databaseType !== 'derby') {
      App.router.reassignMasterController.set('content.hasManualSteps', false);
    }

    this.saveDatabaseType(databaseType)
  },

  saveDatabaseType: function(type) {
    if(type) {
      this.set('databaseType', type);
      App.router.get(this.get('content.controllerName')).saveDatabaseType(type);
    }
  }
});

