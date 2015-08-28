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

var validators = require('utils/validator');
var stringUtils = require('utils/string_utils');
/**
 * Helper methods to process database values and properties
 * @module utils/configs/database
 */
module.exports = {
  /**
   * Database type properties with <code>options</code> attribute usually displayed as radio-button.
   * <code>options</code> attribute contains mapping of database type to special properties name such as
   * database host and database name.
   * @type {object}
   */
  dbServicePropertyMap: {
    HIVE: {
      dbType: 'hive_database',
      databaseName: 'ambari.hive.db.schema.name',
      connectionUrl: 'javax.jdo.option.ConnectionURL',
      fallbackHostName: 'hive_ambari_host'
    },
    OOZIE: {
      dbType: 'oozie_database',
      connectionUrl: 'oozie.service.JPAService.jdbc.url',
      databaseName: 'oozie.db.schema.name',
      fallbackHostName: 'oozieserver_host'
    },
    RANGER: {
      dbType: 'DB_FLAVOR',
      connectionUrl: 'ranger.jpa.jdbc.url',
      databaseName: 'db_name',
      fallbackHostName: 'db_host'
    }
  },

  /**
   * Map of jdbc url patterns related to supported database types.
   *
   * @type {object}
   */
  DB_JDBC_PATTERNS: {
    mysql: 'jdbc:mysql://{0}/{1}',
    mssql: 'jdbc:sqlserver://{0};databaseName={1}',
    postgres: 'jdbc:postgresql://{0}:5432/{1}',
    derby: 'jdbc:derby:{0}/{1}',
    oracle: 'jdbc:oracle:thin:@(?:\/?\/?){0}:1521(\:|\/){1}',
    sqla: 'jdbc:sqlanywhere:host={0};database={1}'
  },

  /**
   * Setup database related properties.
   *
   * @method bootstrapDatabaseProperties
   * @param {App.ServiceConfigProperty[]} serviceConfigs
   * @param {string} [serviceName=false]
   */
  bootstrapDatabaseProperties: function(serviceConfigs, serviceName) {
    var self = this;
    var supportedServices = Em.keys(this.dbServicePropertyMap);
    if (serviceName && !supportedServices.contains(serviceName)) return;
    var serviceNames = serviceName ? [serviceName] : serviceConfigs.mapProperty('serviceName').uniq();
    serviceNames.forEach(function(serviceName) {
      if (!supportedServices.contains(serviceName)) return;
      var configs = serviceConfigs.filterProperty('serviceName', serviceName) || [];
      var connectionConfigs = self.dbServicePropertyMap[serviceName];
      var databaseTypeProperty = configs.findProperty('name', connectionConfigs.dbType);
      if (!databaseTypeProperty) return;
      var databaseTypePropertyIndex = configs.indexOf(databaseTypeProperty);
      var generatedProperties = self.getPropsByOptions(databaseTypeProperty, configs);
      var jdbcObject = self.parseJdbcUrl(Em.get(configs.findProperty('name', connectionConfigs.connectionUrl), 'value'));
      generatedProperties.forEach(function(property) {
        if (Em.get(property, 'name').endsWith('_host')) {
          // set UI host names for each database type with value parsed from jdbc connection url
          // if value is not ip or hostname (in case of New Derby Database) for Oozie set <code>fallbackUrl</code>
          // from <code>dbServicePropertyMap</code>
          Em.setProperties(property, {
            value: jdbcObject.location || ''
          });
          self.addPropertyToServiceConfigs(property, serviceConfigs, databaseTypePropertyIndex);
        }
      });
    });
  },

  isValidHostname: function(value) {
    return validators.isHostname(value) || validators.isIpAddress(value);
  },

  /**
   * Add UI specific property to serviceConfigObject if it does not exist, and update value for existed property.
   * This code affects properties related to `_host` and `_database` which are hardcoded on UI.
   *
   * @param {object} property - property to append/update
   * @param {App.ServiceConfigProperty[]} configs - loaded and processed service configs
   * @param {integer} index of first occurrence of database type in config
   */
  addPropertyToServiceConfigs: function(property, configs, index) {
    var configProperty = configs.findProperty('name', Em.get(property, 'name'));
    if (configProperty) {
      Em.set(configProperty, 'value', Em.get(property, 'value'));
    } else {
      if (index) {
        configs.insertAt(index, App.ServiceConfigProperty.create(property));
      } else {
        configs.pushObject(App.ServiceConfigProperty.create(property));
      }
    }
  },

  /**
   * Get hardcoded properties from site_properties.js which UI use to display host name and database name.
   *
   * @method getPropsByOptions
   * @param {object} databaseTypeProperty - hardcoded property from site_properties.js usualy used as radiobutton
   * @param {App.ServiceConfigProperty[]} configs - loaded and processed configs
   * @returns {object[]} - hardcoded properties from site_properties.js related to database name and location
   */
  getPropsByOptions: function(databaseTypeProperty) {
    Em.assert('Property related to database type should contains `options` attribute', databaseTypeProperty.get('options'));
    return databaseTypeProperty.options.mapProperty('foreignKeys').reduce(function(p,c) {
      return p.concat(c);
    }).uniq().map(function(name) {
      return App.config.get('preDefinedSiteProperties').findProperty('name', name) || null;
    }).compact();
  },

  /**
   * Get database location from jdbc url value.
   *
   * @method getDBLocationFromJDBC
   * @param {string} jdbcUrl - url to parse
   * @returns {string|null} 
   */
  getDBLocationFromJDBC: function(jdbcUrl) {
    var self = this;
    var matches = Em.keys(this.DB_JDBC_PATTERNS).map(function(key) {
      var reg = new RegExp(self.DB_JDBC_PATTERNS[key].format('(.*)', '(.*)'));
      return jdbcUrl.match(reg);
    }).compact();
    if (matches.length) {
      var dbLocation = Em.get(matches, '0.1');
      if (dbLocation.startsWith('${')) {
        return Em.getWithDefault(matches, '0.0', '').match(/\${[^}]+}/)[0];
      }
      return dbLocation != '{0}' ? dbLocation : null;
    } else {
      return null;
    }
  },

  parseJdbcUrl: function(jdbcUrl) {
    var self = this;
    var result = {
      dbType: null,
      location: null,
      databaseName: null
    };
    var dbName;

    result.dbType = Em.keys(this.DB_JDBC_PATTERNS).filter(function(key) {
      var scheme = self.DB_JDBC_PATTERNS[key].match(/^jdbc:(\w+):/)[1];
      return new RegExp('jdbc:' + scheme).test(jdbcUrl);
    })[0];

    result.location = this.getDBLocationFromJDBC(jdbcUrl);
    if (!jdbcUrl.endsWith('{1}')) {
      dbName = jdbcUrl.replace(new RegExp(this.DB_JDBC_PATTERNS[result.dbType].format(stringUtils.escapeRegExp(result.location),'')), '');
      if (dbName) {
        result.databaseName = dbName.split(/[;|?]/)[0];
      }
    }
    return result;
  }
};
