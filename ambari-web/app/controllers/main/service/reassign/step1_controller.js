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
      App.ajax.send({
        name: 'config.tags',
        sender: this,
        success: 'onLoadConfigsTags',
        error: ''
      });
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
        urlParams.push('(type=oozie-env&tag=' + data.Clusters.desired_configs['oozie-env'].tag + ')');
        break;
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
        urlParams.push('(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')');
        urlParams.push('(type=hive-env&tag=' + data.Clusters.desired_configs['hive-env'].tag + ')');
        break;
    }
    return urlParams;
  },

  onLoadConfigsTags: function (data) {
    var urlParams = this.getConfigUrlParams(this.get('content.reassign.component_name'), data);

    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'onLoadConfigs',
        error: ''
      });
    }
  },

  onLoadConfigs: function (data) {
    var databaseProperty = null,
        databaseType = null,
        properties = {},
        isRemoteDB = null;

    data.items.forEach(function(item) {
      $.extend(properties, item.properties);
    });

    this.set('content.serviceProperties', properties);

    databaseProperty = properties[ this.dbProperty() ];
    databaseType = databaseProperty.match(/MySQL|PostgreS|Oracle|Derby|MSSQL|Anywhere/gi)[0];
    this.set('databaseType', databaseType);

    if (this.get('content.reassign.component_name') == 'OOZIE_SERVER' && databaseType !== 'derby') {
      App.router.reassignMasterController.set('content.hasManualSteps', false);
    }

    var serviceDbProp = this.get('content.reassign.service_id').toLowerCase() + "_database";
    properties['is_remote_db'] = /Existing/ig.test( properties[serviceDbProp] );

    properties['database_hostname'] = this.getDatabaseHost();

    this.saveDatabaseType(databaseType);
    this.saveServiceProperties(properties);
  },

  saveDatabaseType: function(type) {
    if(type) {
      App.router.get(this.get('content.controllerName')).saveDatabaseType(type);
    }
  },

  saveServiceProperties: function(properties) {
    if(properties) {
      App.router.get(this.get('content.controllerName')).saveServiceProperties(properties);
    }
  },

  getDatabaseHost: function() {
    var db_type = this.get('databaseType');
    var connectionURLPRops = {
      'HIVE': 'javax.jdo.option.ConnectionURL',
      'OOZIE': 'oozie.service.JPAService.jdbc.url'
    };

    var service = this.get('content.reassign.service_id');
    var connectionURL = this.get('content.serviceProperties')[connectionURLPRops[service]];

    connectionURL = connectionURL.replace("jdbc:" + db_type + "://", "");
    connectionURL = connectionURL.replace("/hive?createDatabaseIfNotExist=true", "");
    connectionURL = connectionURL.replace("/oozie", "");

    return connectionURL;
  }
});
