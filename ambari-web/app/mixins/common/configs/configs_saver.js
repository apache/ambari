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
var dataManipulationUtils = require('utils/data_manipulation');
var lazyLoading = require('utils/lazy_loading');

App.ConfigsSaverMixin = Em.Mixin.create({

  /**
   * @type {boolean}
   */
  saveConfigsFlag: true,
  /**
   * tells controller in saving configs was started
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method startSave
   */
  startSave: function() {
    this.set("saveInProgress", true);
  },

  /**
   * tells controller that save has been finished
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method completeSave
   */
  completeSave: function() {
    this.set("saveInProgress", false);
  },


  /**
   * show some warning popups before user save configs
   * @method showWarningPopupsBeforeSave
   * @private
   * @method showWarningPopupsBeforeSave
   */
  showWarningPopupsBeforeSave: function() {
    var self = this;
    if (this.isDirChanged()) {
      App.showConfirmationPopup(function() {
          self.showChangedDependentConfigs(null, function() {
            self.restartServicePopup();
          });
        },
        Em.I18n.t('services.service.config.confirmDirectoryChange').format(self.get('content.displayName')),
        this.completeSave.bind(this)
      );
    } else {
      self.showChangedDependentConfigs(null, function() {
        self.restartServicePopup();
      });
    }
  },

  /**
   * Runs config validation before save
   * @private
   * @method restartServicePopup
   */
  restartServicePopup: function () {
    this.serverSideValidation()
      .done(this.saveConfigs.bind(this))
      .fail(this.completeSave.bind(this));
  },

  /**
   * Define if user has changed some dir properties
   * @return {Boolean}
   * @private
   * @method isDirChanged
   */
  isDirChanged: function () {
    var dirChanged = false;
    var serviceName = this.get('content.serviceName');

    if (serviceName === 'HDFS') {
      var hdfsConfigs = this.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs');
      if ((hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir') && hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir').get('isNotDefaultValue'))) {
        dirChanged = true;
      }
    }
    return dirChanged;
  },

  /**
   * get config properties for that fileNames that was changed
   * @param stepConfigs
   * @returns {Array}
   */
  getModifiedConfigs: function(stepConfigs) {
    var modifiedConfigs = stepConfigs
      // get only modified and created configs
      .filter(function (config) {
        return config.get('isNotDefaultValue') || config.get('isNotSaved');
      })
      // get file names and add file names that was modified, for example after property removing
      .mapProperty('filename').concat(this.get('modifiedFileNames')).uniq()
      // get configs by filename
      .map(function (fileName) {
        return stepConfigs.filterProperty('filename', fileName);
      });

    if (!!modifiedConfigs.length) {
      // concatenate results
      modifiedConfigs = modifiedConfigs.reduce(function (current, prev) {
        return current.concat(prev);
      });
    }
    return modifiedConfigs;
  },

  /**
   * Save changed configs and config groups
   * @method saveConfigs
   */
  saveConfigs: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');

    if (selectedConfigGroup.get('isDefault')) {
      if (this.get('content.serviceName') === 'YARN') {
        configs = App.config.textareaIntoFileConfigs(configs, 'capacity-scheduler.xml');
      }

      /**
       * generates list of properties that was changed
       * @type {Array}
       */
      var modifiedConfigs = this.getModifiedConfigs(configs);

      // save modified original configs that have no group
      this.saveSiteConfigs(modifiedConfigs.filter(function (config) {
        return !config.get('group');
      }));

      /**
       * First we put cluster configurations, which automatically creates /configurations
       * resources. Next we update host level overrides.
       */
      this.doPUTClusterConfigurations();

    } else {

      var overridenConfigs = this.getConfigsForGroup(configs, selectedConfigGroup.get('name'));

      /**
       * if there are some changes in dependent configs
       * need to save these config to in separate request
       */
      this.get('dependentServiceNames').forEach(function(serviceName) {
        var serviceConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
        if (serviceConfig && this.get('changedProperties').findProperty('serviceName', serviceName) && this.get('groupsToSave')[serviceName]) {
          var stepConfigs = serviceConfig.get('configs');

          if (this.get('groupsToSave')[serviceName].contains('Default')) {
            var data = [];

            var modifiedConfigs = this.getModifiedConfigs(stepConfigs);

            var fileNamesToSave = modifiedConfigs.mapProperty('filename').uniq();

            var dependentConfigsToSave = this.generateDesiredConfigsJSON(modifiedConfigs, fileNamesToSave, this.get('serviceConfigNote'));

            if (dependentConfigsToSave.length > 0) {
              data.pushObject(JSON.stringify({
                Clusters: {
                  desired_config: dependentConfigsToSave
                }
              }));
            }
            this.doPUTClusterConfigurationSites(data, false);
          } else {
            var overridenConfigs = this.getConfigsForGroup(stepConfigs, selectedConfigGroup.get('name'));
            var group = this.get('dependentConfigGroups').findProperty('name', this.get('groupsToSave')[serviceName]);
            this.saveGroup(overridenConfigs, group, false);
          }
        }
      }, this);

      this.saveGroup(overridenConfigs, selectedConfigGroup, true);
    }
  },

  /**
   * get configs that belongs to config group
   * @param stepConfigs
   * @param configGroupName
   */
  getConfigsForGroup: function(stepConfigs, configGroupName) {
    var overridenConfigs = [];

    stepConfigs.filterProperty('isOverridden', true).forEach(function (config) {
      overridenConfigs = overridenConfigs.concat(config.get('overrides'));
    });
    // find custom original properties that assigned to selected config group
    overridenConfigs = overridenConfigs.concat(stepConfigs.filterProperty('group')
      .filter(function (config) {
        return config.get('group.name') == configGroupName;
      }));

    this.formatConfigValues(overridenConfigs);
    return overridenConfigs;
  },

  /**
   * save config group
   * @param overridenConfigs
   * @param selectedConfigGroup
   * @param showPopup
   */
  saveGroup: function(overridenConfigs, selectedConfigGroup, showPopup) {
    var groupHosts = [];
    var fileNamesToSave = overridenConfigs.mapProperty('filename');
    selectedConfigGroup.get('hosts').forEach(function (hostName) {
      groupHosts.push({"host_name": hostName});
    });
    this.putConfigGroupChanges({
      ConfigGroup: {
        "id": selectedConfigGroup.get('id'),
        "cluster_name": App.get('clusterName'),
        "group_name": selectedConfigGroup.get('name'),
        "tag": selectedConfigGroup.get('service.id'),
        "description": selectedConfigGroup.get('description'),
        "hosts": groupHosts,
        "service_config_version_note": this.get('serviceConfigVersionNote'),
        "desired_configs": this.generateDesiredConfigsJSON(overridenConfigs, fileNamesToSave, null, true)
      }
    }, showPopup);
  },
  /**
   * On save configs handler. Open save configs popup with appropriate message
   * @private
   * @method onDoPUTClusterConfigurations
   */
  onDoPUTClusterConfigurations: function () {
    var header, message, messageClass, value, status = 'unknown', urlParams = '',
      result = {
        flag: this.get('saveConfigsFlag'),
        message: null,
        value: null
      },
      extendedModel = App.Service.extendedModel[this.get('content.serviceName')],
      currentService = extendedModel ? App[extendedModel].find(this.get('content.serviceName')) : App.Service.find(this.get('content.serviceName'));

    if (!result.flag) {
      result.message = Em.I18n.t('services.service.config.failSaveConfig');
    }

    App.router.get('clusterController').updateClusterData();
    App.router.get('updateController').updateComponentConfig(function () {
    });
    var flag = result.flag;
    if (result.flag === true) {
      header = Em.I18n.t('services.service.config.saved');
      message = Em.I18n.t('services.service.config.saved.message');
      messageClass = 'alert alert-success';
      // warn the user if any of the components are in UNKNOWN state
      urlParams += ',ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count';
      if (this.get('content.serviceName') === 'HDFS') {
        urlParams += '&ServiceComponentInfo/service_name.in(HDFS)'
      }
    } else {
      header = Em.I18n.t('common.failure');
      message = result.message;
      messageClass = 'alert alert-error';
      value = result.value;
    }
    if(currentService){
      App.QuickViewLinks.proto().set('content', currentService);
      App.QuickViewLinks.proto().loadTags();
    }
    this.showSaveConfigsPopup(header, flag, message, messageClass, value, status, urlParams);
  },

  /**
   * Show save configs popup
   * @return {App.ModalPopup}
   * @private
   * @method showSaveConfigsPopup
   */
  showSaveConfigsPopup: function (header, flag, message, messageClass, value, status, urlParams) {
    var self = this;
    if (flag) {
      this.set('forceTransition', flag);
      self.loadStep();
    }
    return App.ModalPopup.show({
      header: header,
      primary: Em.I18n.t('ok'),
      secondary: null,
      onPrimary: function () {
        this.hide();
        if (!flag) {
          self.completeSave();
        }
      },
      onClose: function () {
        this.hide();
        self.completeSave();
      },
      disablePrimary: true,
      bodyClass: Ember.View.extend({
        flag: flag,
        message: function () {
          return this.get('isLoaded') ? message : Em.I18n.t('services.service.config.saving.message');
        }.property('isLoaded'),
        messageClass: function () {
          return this.get('isLoaded') ? messageClass : 'alert alert-info';
        }.property('isLoaded'),
        setDisablePrimary: function () {
          this.get('parentView').set('disablePrimary', !this.get('isLoaded'));
        }.observes('isLoaded'),
        runningHosts: [],
        runningComponentCount: 0,
        unknownHosts: [],
        unknownComponentCount: 0,
        siteProperties: value,
        isLoaded: false,
        componentsFilterSuccessCallback: function (response) {
          var count = 0,
            view = this,
            lazyLoadHosts = function (dest) {
              lazyLoading.run({
                initSize: 20,
                chunkSize: 50,
                delay: 50,
                destination: dest,
                source: hosts,
                context: view
              });
            },
            /**
             * Map components for their hosts
             * Return format:
             * <code>
             *   {
             *    host1: [component1, component2, ...],
             *    host2: [component3, component4, ...]
             *   }
             * </code>
             * @return {object}
             */
              setComponents = function (item, components) {
              item.host_components.forEach(function (c) {
                var name = c.HostRoles.host_name;
                if (!components[name]) {
                  components[name] = [];
                }
                components[name].push(App.format.role(item.ServiceComponentInfo.component_name));
              });
              return components;
            },
            /**
             * Map result of <code>setComponents</code> to array
             * @return {{name: string, components: string}[]}
             */
              setHosts = function (components) {
              var hosts = [];
              Em.keys(components).forEach(function (key) {
                hosts.push({
                  name: key,
                  components: components[key].join(', ')
                });
              });
              return hosts;
            },
            components = {},
            hosts = [];
          switch (status) {
            case 'unknown':
              response.items.filter(function (item) {
                return (item.ServiceComponentInfo.total_count > item.ServiceComponentInfo.started_count + item.ServiceComponentInfo.installed_count);
              }).forEach(function (item) {
                var total = item.ServiceComponentInfo.total_count,
                  started = item.ServiceComponentInfo.started_count,
                  installed = item.ServiceComponentInfo.installed_count,
                  unknown = total - started + installed;
                components = setComponents(item, components);
                count += unknown;
              });
              hosts = setHosts(components);
              this.set('unknownComponentCount', count);
              lazyLoadHosts(this.get('unknownHosts'));
              break;
            case 'started':
              response.items.filterProperty('ServiceComponentInfo.started_count').forEach(function (item) {
                var started = item.ServiceComponentInfo.started_count;
                components = setComponents(item, components);
                count += started;
                hosts = setHosts(components);
              });
              this.set('runningComponentCount', count);
              lazyLoadHosts(this.get('runningHosts'));
              break;
          }
        },
        componentsFilterErrorCallback: function () {
          this.set('isLoaded', true);
        },
        didInsertElement: function () {
          return App.ajax.send({
            name: 'components.filter_by_status',
            sender: this,
            data: {
              clusterName: App.get('clusterName'),
              urlParams: urlParams
            },
            success: 'componentsFilterSuccessCallback',
            error: 'componentsFilterErrorCallback'
          });
        },
        getDisplayMessage: function () {
          var displayMsg = [];
          var siteProperties = this.get('siteProperties');
          if (siteProperties) {
            siteProperties.forEach(function (_siteProperty) {
              var displayProperty = _siteProperty.siteProperty;
              var displayNames = _siteProperty.displayNames;

              if (displayNames && displayNames.length) {
                if (displayNames.length === 1) {
                  displayMsg.push(displayProperty + Em.I18n.t('as') + displayNames[0]);
                } else {
                  var name;
                  displayNames.forEach(function (_name, index) {
                    if (index === 0) {
                      name = _name;
                    } else if (index === siteProperties.length - 1) {
                      name = name + Em.I18n.t('and') + _name;
                    } else {
                      name = name + ', ' + _name;
                    }
                  }, this);
                  displayMsg.push(displayProperty + Em.I18n.t('as') + name);

                }
              } else {
                displayMsg.push(displayProperty);
              }
            }, this);
          }
          return displayMsg;

        }.property('siteProperties'),

        runningHostsMessage: function () {
          return Em.I18n.t('services.service.config.stopService.runningHostComponents').format(this.get('runningComponentCount'), this.get('runningHosts.length'));
        }.property('runningComponentCount', 'runningHosts.length'),

        unknownHostsMessage: function () {
          return Em.I18n.t('services.service.config.stopService.unknownHostComponents').format(this.get('unknownComponentCount'), this.get('unknownHosts.length'));
        }.property('unknownComponentCount', 'unknownHosts.length'),

        templateName: require('templates/main/service/info/configs_save_popup')
      })
    })
  },

  /**
   * construct desired_configs for config groups from overriden properties
   * @param configs
   * @param timeTag
   * @return {Array}
   * @private
   * @method buildGroupDesiredConfigs
   */
  buildGroupDesiredConfigs: function (configs, timeTag) {
    var sites = [];
    var time = timeTag || (new Date).getTime();
    var siteFileNames = configs.mapProperty('filename').uniq();
    sites = siteFileNames.map(function (filename) {
      return {
        type: filename.replace('.xml', ''),
        tag: 'version' + time,
        properties: []
      };
    });

    configs.forEach(function (config) {
      var type = config.get('filename').replace('.xml', '');
      var site = sites.findProperty('type', type);
      site.properties.push(config);
    });

    return sites.map(function (site) {
      return this.createSiteObj(site.type, site.tag, site.properties);
    }, this);
  },
  /**
   * persist properties of config groups to server
   * show result popup if <code>showPopup</code> is true
   * @param data {Object}
   * @param showPopup {Boolean}
   * @method putConfigGroupChanges
   */
  putConfigGroupChanges: function (data, showPopup) {
    var ajaxOptions = {
      name: 'config_groups.update_config_group',
      sender: this,
      data: {
        id: data.ConfigGroup.id,
        configGroup: data
      }
    };
    if (showPopup) {
      ajaxOptions.success = "putConfigGroupChangesSuccess";
    }
    return App.ajax.send(ajaxOptions);
  },

  /**
   * @private
   * @method putConfigGroupChangesSuccess
   */
  putConfigGroupChangesSuccess: function () {
    this.set('saveConfigsFlag', true);
    this.onDoPUTClusterConfigurations();
  },

  /**
   * set hive hostnames in configs
   * @param configs
   * @private
   * @method setHiveHostName
   */
  setHiveHostName: function (configs) {
    var dbHostPropertyName = null, configsToRemove = [];
    if (configs.someProperty('name', 'hive_database')) {
      var hiveDb = configs.findProperty('name', 'hive_database');

      switch(hiveDb.value) {
        case 'New MySQL Database':
        case 'New PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_ambari_host') ? 'hive_ambari_host' : dbHostPropertyName;
          configsToRemove = ['hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MySQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mysql_host') ? 'hive_existing_mysql_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_postgresql_host') ? 'hive_existing_postgresql_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing Oracle Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_oracle_host') ? 'hive_existing_oracle_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with SQL authentication':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mssql_server_host') ? 'hive_existing_mssql_server_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with integrated authentication':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mssql_server_2_host') ? 'hive_existing_mssql_server_2_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host'];
          break;
      }
      configs = dataManipulationUtils.rejectPropertyValues(configs, 'name', configsToRemove);
    }
    if (dbHostPropertyName) {
      var hiveHostNameProperty = App.ServiceConfigProperty.create(App.config.get('preDefinedSiteProperties').findProperty('name', 'hive_hostname'));
      hiveHostNameProperty.set('value', configs.findProperty('name', dbHostPropertyName).get('value'));
      configs.pushObject(hiveHostNameProperty);
    }
    return configs;
  },

  /**
   * set oozie hostnames in configs
   * @param configs
   * @private
   * @method setOozieHostName
   */
  setOozieHostName: function (configs) {
    var dbHostPropertyName = null, configsToRemove = [];
    if (configs.someProperty('name', 'oozie_database')) {
      var oozieDb = configs.findProperty('name', 'oozie_database');
      switch (oozieDb.value) {
        case 'New Derby Database':
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'New MySQL Database':
          var ambariHost = configs.findProperty('name', 'oozie_ambari_host');
          if (ambariHost) {
            ambariHost.name = 'oozie_hostname';
          }
          configsToRemove = ['oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MySQL Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mysql_host') ? 'oozie_existing_mysql_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_postgresql_host') ? 'oozie_existing_postgresql_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing Oracle Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_oracle_host') ? 'oozie_existing_oracle_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_derby_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with SQL authentication':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mssql_server_host') ? 'oozie_existing_mssql_server_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with integrated authentication':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mssql_server_2_host') ? 'oozie_existing_mssql_server_2_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host'];
          break;
      }
      configs = dataManipulationUtils.rejectPropertyValues(configs, 'name', configsToRemove);
    }

    if (dbHostPropertyName) {
      var oozieHostNameProperty = App.ServiceConfigProperty.create(App.config.get('preDefinedSiteProperties').findProperty('name', 'oozie_hostname'));
      oozieHostNameProperty.set('value', configs.findProperty('name', dbHostPropertyName).get('value'));
      configs.pushObject(oozieHostNameProperty);
    }
    return configs;
  },

  /**
   * save site configs
   * @param configs
   * @private
   * @method saveSiteConfigs
   */
  saveSiteConfigs: function (configs) {
    //storedConfigs contains custom configs as well
    configs = this.setHiveHostName(configs);
    configs = this.setOozieHostName(configs);
    this.formatConfigValues(configs);
    var mappedConfigs = App.config.excludeUnsupportedConfigs(this.get('configMapping').all(), App.Service.find().mapProperty('serviceName'));
    var allUiConfigs = this.loadUiSideConfigs(mappedConfigs);
    this.set('uiConfigs', configs.concat(allUiConfigs));
  },

  /**
   * Reprecent boolean value as string (true => 'true', false => 'false') and trim other values
   * @param serviceConfigProperties
   * @private
   * @method formatConfigValues
   */
  formatConfigValues: function (serviceConfigProperties) {
    serviceConfigProperties.forEach(function (_config) {
      if (typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
      _config.set('value', App.config.trimProperty(_config, true));
    });
  },

  /**
   * return configs from the UI side
   * @param configMapping array with configs
   * @return {Array}
   * @private
   * @method loadUiSideConfigs
   */
  loadUiSideConfigs: function (configMapping) {
    var uiConfig = [];
    var configs = configMapping.filterProperty('foreignKey', null);
    this.addDynamicProperties(configs);
    configs.forEach(function (_config) {
      var valueWithOverrides = this.getGlobConfigValueWithOverrides(_config.templateName, _config.value, _config.name);
      if (valueWithOverrides !== null) {
        uiConfig.pushObject({
          "id": "site property",
          "name": _config.name,
          "value": valueWithOverrides.value,
          "filename": _config.filename,
          "overrides": valueWithOverrides.overrides
        });
      }
    }, this);
    return uiConfig;
  },

  /**
   * @param configs
   * @private
   * @method addDynamicProperties
   */
  addDynamicProperties: function (configs) {
    var allConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var templetonHiveProperty = allConfigs.someProperty('name', 'templeton.hive.properties');
    if (!templetonHiveProperty && this.get('content.serviceName') === 'HIVE') {
      configs.pushObject({
        "name": "templeton.hive.properties",
        "templateName": ["hive.metastore.uris"],
        "foreignKey": null,
        "value": "hive.metastore.local=false,hive.metastore.uris=<templateName[0]>,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse",
        "filename": "webhcat-site.xml"
      });
    }
  },

  /**
   * return config value
   * @param templateName
   * @param expression
   * @param name
   * @return {Object}
   * example: <code>{
   *   value: '...',
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }</code>
   * @private
   * @method getGlobConfigValueWithOverrides
   */
  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    var overrideHostToValue = {};
    if (express != null) {
      express.forEach(function (_express) {
        var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
        var globalObj = this.get('allConfigs').findProperty('name', templateName[index]);
        if (globalObj) {
          var globOverride = globalObj.overrides;
          if (globOverride != null) {
            for (var ov in globOverride) {
              globOverride[ov].forEach(function (host) {
                var replacedVal = (host in overrideHostToValue) ? overrideHostToValue[host] : expression;
                overrideHostToValue[host] = App.config.replaceConfigValues(name, _express, replacedVal, ov);
              }, this);
            }
          }
          value = App.config.replaceConfigValues(name, _express, expression, globalObj.value);
        } else {
          value = null;
        }
      }, this);
    }
    return this.getValueWithOverrides(value, overrideHostToValue)
  },

  /**
   * @param value
   * @param overrideHostToValue
   * @returns {{value: *, overrides: {}}}
   * @private
   * @method getValueWithOverrides
   */
  getValueWithOverrides: function (value, overrideHostToValue) {
    var valueWithOverrides = {
      value: value,
      overrides: {}
    };
    if (!jQuery.isEmptyObject(overrideHostToValue)) {
      for (var host in overrideHostToValue) {
        var hostVal = overrideHostToValue[host];
        if (!(hostVal in valueWithOverrides.overrides)) {
          valueWithOverrides.overrides[hostVal] = [];
        }
        valueWithOverrides.overrides[hostVal].push(host);
      }
    }
    return valueWithOverrides;
  },

  /**
   * Saves cluster level configurations for all necessary sites
   * PUT calls are made to /api/v1/clusters/clusterName for each site
   * @private
   * @method doPUTClusterConfigurations
   */
  doPUTClusterConfigurations: function () {
    this.set('saveConfigsFlag', true);
    var serviceConfigTags = this.get('serviceConfigTags');
    /**
     * adding config tags for dependentConfigs
     */
    for (var i = 0; i < this.get('dependentFileNames.length'); i++) {
      if (!serviceConfigTags.findProperty('siteName', this.get('dependentFileNames')[i])) {
        serviceConfigTags.pushObject({siteName: this.get('dependentFileNames')[i]});
      }
    }
    this.setNewTagNames(serviceConfigTags);
    var siteNameToServerDataMap = {};
    var configsToSave = [];
    serviceConfigTags.forEach(function (_serviceTags) {
      var configs = this.createConfigObject(_serviceTags.siteName, _serviceTags.newTagName);
      if (configs) {
        configsToSave.push(configs);
        siteNameToServerDataMap[_serviceTags.siteName] = configs;
      }
    }, this);
    configsToSave = this.filterChangedConfiguration(configsToSave);
    if (configsToSave.length > 0) {
      var data = [];
      data.pushObject(JSON.stringify({
        Clusters: {
          desired_config: configsToSave
        }
      }));
      if (App.get('supports.enhancedConfigs')) {
        /**
         * adding configs that were changed for dependent services
         * if there are such configs
         */
        this.get('dependentServiceNames').forEach(function(serviceName) {

          var serviceConfigs = this.get('stepConfigs').findProperty('serviceName', serviceName).get('configs');

          var modifiedConfigs = this.getModifiedConfigs(serviceConfigs);

          var fileNamesToSave = modifiedConfigs.mapProperty('filename').uniq();

          var dependentConfigsToSave = this.generateDesiredConfigsJSON(modifiedConfigs, fileNamesToSave, this.get('serviceConfigNote'));
          if (dependentConfigsToSave.length > 0) {
            data.pushObject(JSON.stringify({
              Clusters: {
                desired_config: dependentConfigsToSave
              }
            }));
          }
        }, this);
      }
      this.doPUTClusterConfigurationSites(data, true);
    } else {
      this.onDoPUTClusterConfigurations();
    }
  },

  /**
   * create different config object depending on siteName
   * @param {String} siteName
   * @param {String} tagName
   * @returns {Object|null}
   * @private
   * @method createConfigObject
   */
  createConfigObject: function (siteName, tagName) {
    console.log("TRACE: Inside " + siteName);
    var configObject = {};
    switch (siteName) {
      case 'core-site':
        if (this.get('content.serviceName') === 'HDFS' || this.get('content.serviceName') === 'GLUSTERFS') {
          configObject = this.createCoreSiteObj(tagName);
        } else {
          return null;
        }
        break;
      default:
        var filename = App.config.getOriginalFileName(siteName);
        if (filename === 'mapred-queue-acls.xml') {
          return null;
        }
        configObject = this.createSiteObj(siteName, tagName, this.get('uiConfigs').filterProperty('filename', filename));
        break;
    }
    configObject.service_config_version_note = this.get('serviceConfigVersionNote');
    return configObject;
  },

  /**
   * filter out unchanged configurations
   * @param {Array} configsToSave
   * @private
   * @method filterChangedConfiguration
   */
  filterChangedConfiguration: function (configsToSave) {
    var changedConfigs = [];

    configsToSave.forEach(function (configSite) {
      var oldConfig = App.router.get('configurationController').getConfigsByTags([
        {siteName: configSite.type, tagName: this.loadedClusterSiteToTagMap[configSite.type]}
      ]);
      oldConfig = oldConfig[0] || {};
      var oldProperties = oldConfig.properties || {};
      var oldAttributes = oldConfig["properties_attributes"] || {};
      var newProperties = configSite.properties || {};
      var newAttributes = configSite["properties_attributes"] || {};
      if (this.isAttributesChanged(oldAttributes, newAttributes) || this.isConfigChanged(oldProperties, newProperties) || this.get('modifiedFileNames').contains(App.config.getOriginalFileName(configSite.type))) {
        changedConfigs.push(configSite);
      }
    }, this);
    return changedConfigs;
  },

  /**
   * Compares the loaded config values with the saving config values.
   * @param {Object} loadedConfig -
   * loadedConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @param {Object} savingConfig
   * savingConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @returns {boolean}
   * @private
   * @method isConfigChanged
   */
  isConfigChanged: function (loadedConfig, savingConfig) {
    if (loadedConfig != null && savingConfig != null) {
      var seenLoadKeys = [];
      for (var loadKey in loadedConfig) {
        if (!loadedConfig.hasOwnProperty(loadKey)) continue;
        seenLoadKeys.push(loadKey);
        var loadValue = loadedConfig[loadKey];
        var saveValue = savingConfig[loadKey];
        if ("boolean" == typeof(saveValue)) {
          saveValue = saveValue.toString();
        }
        if (saveValue == null) {
          saveValue = "null";
        }
        if (loadValue !== saveValue) {
          return true;
        }
      }
      for (var saveKey in savingConfig) {
        if (seenLoadKeys.indexOf(saveKey) < 0) {
          return true;
        }
      }
    }
    return false;
  },

  /**
   * Compares the loaded config properties attributes with the saving config properties attributes.
   * @param {Object} oldAttributes -
   * oldAttributes: {
   *   supports: {
   *     final: {
   *       "configValue1" : "true",
   *       "configValue2" : "true"
   *     }
   *   }
   * }
   * @param {Object} newAttributes
   * newAttributes: {
   *   supports: {
   *     final: {
   *       "configValue1" : "true",
   *       "configValue2" : "true"
   *     }
   *   }
   * }
   * @returns {boolean}
   * @private
   * @method isAttributesChanged
   */
  isAttributesChanged: function (oldAttributes, newAttributes) {
    oldAttributes = oldAttributes.final || {};
    newAttributes = newAttributes.final || {};

    var key;
    for (key in oldAttributes) {
      if (oldAttributes.hasOwnProperty(key)
        && (!newAttributes.hasOwnProperty(key) || newAttributes[key] !== oldAttributes[key])) {
        return true;
      }
    }
    for (key in newAttributes) {
      if (newAttributes.hasOwnProperty(key)
        && (!oldAttributes.hasOwnProperty(key) || newAttributes[key] !== oldAttributes[key])) {
        return true;
      }
    }
    return false;
  },

  /**
   * Saves configuration of set of sites. The provided data
   * contains the site name and tag to be used.
   * @param {Object[]} services
   * @param {boolean} showPopup
   * @return {$.ajax}
   * @method doPUTClusterConfigurationSites
   */
  doPUTClusterConfigurationSites: function (services, showPopup) {
    var ajaxData = {
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + services.toString() + ']'
      },
      error: 'doPUTClusterConfigurationSiteErrorCallback'
    };
    if (showPopup) {
      ajaxData.success = 'doPUTClusterConfigurationSiteSuccessCallback'
    }
    return App.ajax.send(ajaxData);
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteSuccessCallback
   */
  doPUTClusterConfigurationSiteSuccessCallback: function () {
    this.onDoPUTClusterConfigurations();
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteErrorCallback
   */
  doPUTClusterConfigurationSiteErrorCallback: function () {
    this.set('saveConfigsFlag', false);
    this.doPUTClusterConfigurationSiteSuccessCallback();
  },

  /**
   * add newTagName property to each config in serviceConfigs
   * @param serviceConfigs
   * @private
   * @method setNewTagNames
   */
  setNewTagNames: function (serviceConfigs) {
    var time = (new Date).getTime();
    serviceConfigs.forEach(function (_serviceConfigs) {
      _serviceConfigs.newTagName = 'version' + time;
    }, this);
  },

  /**
   * Save "final" attribute for properties
   * @param {Array} properties - array of properties
   * @returns {Object|null}
   * @method getConfigAttributes
   */
  getConfigAttributes: function(properties) {
    var attributes = {
      final: {}
    };
    var finalAttributes = attributes.final;
    var hasAttributes = false;
    properties.forEach(function (property) {
      if (property.isRequiredByAgent !== false && property.isFinal) {
        hasAttributes = true;
        finalAttributes[property.name] = "true";
      }
    });
    if (hasAttributes) {
      return attributes;
    }
    return null;
  },

  /**
   * create core site object
   * @param tagName
   * @return {{type: string, tag: string, properties: object}}
   * @method createCoreSiteObj
   */
  createCoreSiteObj: function (tagName) {
    var coreSiteObj = this.get('uiConfigs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    coreSiteObj.forEach(function (_coreSiteObj) {
      coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      //this.recordHostOverride(_coreSiteObj, 'core-site', tagName, this);
    }, this);
    var result = {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
    var attributes = this.getConfigAttributes(coreSiteObj);
    if (attributes) {
      result['properties_attributes'] = attributes;
    }
    return result;
  },

  /**
   * create site object
   * @param {string} siteName
   * @param {string} tagName
   * @param {object[]} siteObj
   * @return {Object}
   * @method createSiteObj
   */
  createSiteObj: function (siteName, tagName, siteObj) {
    var heapsizeException = this.get('heapsizeException');
    var heapsizeRegExp = this.get('heapsizeRegExp');
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      var value = _siteObj.value;
      if (_siteObj.isRequiredByAgent == false) return;
      // site object name follow the format *permsize/*heapsize and the value NOT ends with "m"
      if (heapsizeRegExp.test(_siteObj.name) && !heapsizeException.contains(_siteObj.name) && !(_siteObj.value).endsWith("m")) {
        value += "m";
      }
      siteProperties[_siteObj.name] = value;
      switch (siteName) {
        case 'falcon-startup.properties':
        case 'falcon-runtime.properties':
        case 'pig-properties':
          siteProperties[_siteObj.name] = value;
          break;
        default:
          siteProperties[_siteObj.name] = this.setServerConfigValue(_siteObj.name, value);
      }
    }, this);
    var result = {"type": siteName, "tag": tagName, "properties": siteProperties};
    var attributes = this.getConfigAttributes(siteObj);
    if (attributes) {
      result['properties_attributes'] = attributes;
    }
    return result;
  },

  /**
   * This method will be moved to config's decorators class.
   *
   * For now, provide handling for special properties that need
   * be specified in special format required for server.
   *
   * @param configName {String} - name of config property
   * @param value {*} - value of config property
   *
   * @return {String} - formatted value
   * @method setServerConfigValue
   */
  setServerConfigValue: function (configName, value) {
    switch (configName) {
      case 'storm.zookeeper.servers':
        if( Object.prototype.toString.call( value ) === '[object Array]' ) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return value;
    }
  },

  /**
   * Are some unsaved changes available
   * @returns {boolean}
   * @method hasUnsavedChanges
   */
  hasUnsavedChanges: function () {
    return this.get('hash') != this.getHash();
  },

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {String} path
   * @param {object} callback - callback with action to change configs view(change group or version)
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (path, callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        showSaveWarning: true,
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder'),
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      footerClass: Ember.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        self.set('serviceConfigVersionNote', this.get('serviceConfigNote'));
        self.saveStepConfigs();
        this.hide();
      },
      onDiscard: function () {
        self.set('preSelectedConfigVersion', null);
        if (path) {
          self.set('forceTransition', true);
          App.router.route(path);
        } else if (callback) {
          // Prevent multiple popups
          self.set('hash', self.getHash());
          callback();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  },
  /********************************METHODS THAT GENERATES JSON TO SAVE *****************************************/

  /**
   * generating common JSON object for desired configs
   * @param configsToSave
   * @param fileNamesToSave
   * @param serviceConfigNote
   * @param {boolean} [isNotDefaultGroup=false]
   * @returns {Array}
   */
  generateDesiredConfigsJSON: function(configsToSave, fileNamesToSave, serviceConfigNote, isNotDefaultGroup) {
    var desired_config = [];
    if (Em.isArray(configsToSave) && Em.isArray(fileNamesToSave) && fileNamesToSave.length && configsToSave.length) {
      serviceConfigNote = serviceConfigNote || "";
      var tagVersion = "version" + (new Date).getTime();

      fileNamesToSave.forEach(function(fName) {
        if (this.allowSaveSite(fName)) {
          var properties = configsToSave.filterProperty('filename', fName);
          var type = App.config.getConfigTagFromFileName(fName);
          desired_config.push(this.createDesiredConfig(type, tagVersion, properties, serviceConfigNote, isNotDefaultGroup));
        }
      }, this);
    }
    return desired_config;
  },

  /**
   * for some file names we have a restriction
   * and can't save them, in this this method will return false
   * @param fName
   * @returns {boolean}
   */
  allowSaveSite: function(fName) {
    switch (fName) {
      case 'mapred-queue-acls.xml':
        return false;
      case 'core-site.xml':
        return ['HDFS', 'GLUSTERFS'].contains(this.get('content.serviceName'));
      default :
        return true;
    }
  },

  /**
   * generating common JSON object for desired config
   * @param {string} type - file name without '.xml'
   * @param {string} tagVersion - version + timestamp
   * @param {App.ConfigProperty[]} properties - array of properties from model
   * @param {string} serviceConfigNote
   * @param {boolean} [isNotDefaultGroup=false]
   * @returns {{type: string, tag: string, properties: {}, properties_attributes: {}|undefined, service_config_version_note: string|undefined}}
   */
  createDesiredConfig: function(type, tagVersion, properties, serviceConfigNote, isNotDefaultGroup) {
    Em.assert('type and tagVersion should be defined', type && tagVersion);
    var desired_config = {
      "type": type,
      "tag": tagVersion,
      "properties": {}
    };
    if (!isNotDefaultGroup) {
      desired_config.service_config_version_note = serviceConfigNote || "";
    }
    var attributes = { final: {} };
    if (Em.isArray(properties)) {
      properties.forEach(function(property) {

        if (property.get('isRequiredByAgent')) {
          desired_config.properties[property.get('name')] = this.formatValueBeforeSave(property);
          /**
           * add is final value
           */
          if (property.get('isFinal')) {
            attributes.final[property.get('name')] = "true";
          }
        }
      }, this);
    }

    if (Object.keys(attributes.final).length) {
      desired_config.properties_attributes = attributes;
    }
    return desired_config;
  },

  /**
   * format value before save performs some changing of values
   * according to the rules that includes heapsizeException trimming and some custom rules
   * @param {App.ConfigProperty} property
   * @returns {string}
   */
  formatValueBeforeSave: function(property) {
    var name = property.get('name');
    var value = property.get('value');
    //TODO check for core-site
    if (this.get('heapsizeRegExp').test(name) && !this.get('heapsizeException').contains(name) && !(value).endsWith("m")) {
      return value += "m";
    }
    if (typeof property.get('value') === "boolean") {
      return property.get('value').toString();
    }
    switch (name) {
      case 'storm.zookeeper.servers':
        if (Object.prototype.toString.call(value) === '[object Array]' ) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return App.config.trimProperty(property, true);
    }
  }
});