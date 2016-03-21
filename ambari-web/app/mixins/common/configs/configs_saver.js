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

/**
 * this mixin has method to save configs (used in MainServiceInfoConfigsController)
 * all methods are divided into couple groups :
 *  0. HELPERS - some helper methods
 *  1. PRE SAVE CHECKS - warning popups and validations checks
 *  2. PREPARE CONFIGS TO SAVE - filtering and formatting changed configs
 *    2.1 PREPARE DATABASE CONFIGS - modify database properties
 *    2.2 ADD DYNAMIC CONFIGS - !!!NEED INVESTIGATION
 *  3. GENERATING JSON TO SAVE - generating json data
 *  4. AJAX REQUESTS - ajax request
 *  5. AFTER SAVE INFO - after save methods like to show popup with result
 *  6. ADDITIONAL
 */
App.ConfigsSaverMixin = Em.Mixin.create({

  /**
   * @type {boolean}
   */
  saveConfigsFlag: true,

  /**
   * file names of changed configs
   * @type {string[]}
   */
  modifiedFileNames: [],

  /**
   * List of heapsize properties not to be parsed
   * @type {string[]}
   */
  heapsizeException: ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize', 'apptimelineserver_heapsize', 'jobhistory_heapsize', 'nfsgateway_heapsize', 'accumulo_master_heapsize', 'accumulo_tserver_heapsize', 'accumulo_monitor_heapsize', 'accumulo_gc_heapsize', 'accumulo_other_heapsize', 'hbase_master_heapsize', 'hbase_regionserver_heapsize', 'metrics_collector_heapsize'],

  /**
   * Regular expression for heapsize properties detection
   * @type {regexp}
   */
  heapsizeRegExp: /_heapsize|_newsize|_maxnewsize|_permsize|_maxpermsize$/,

  /**
   * clear info to default
   * @method clearSaveInfo
   */
  clearSaveInfo: function() {
    this.set('modifiedFileNames', []);
  },

  /**
   * method to run saving configs
   * @method saveStepConfigs
   */
  saveStepConfigs: function() {
    if (!this.get("isSubmitDisabled")) {
      this.startSave();
      this.showWarningPopupsBeforeSave();
    }
  },

  /**
   * get config group object for current service
   * @param serviceName
   * @returns {App.ConfigGroup}
   */
  getGroupFromModel: function(serviceName) {
    if (this.get('selectedService.serviceName') === serviceName) {
      return this.get('selectedConfigGroup');
    } else {
      var groups = App.ServiceConfigGroup.find().filterProperty('serviceName', serviceName);
      if (this.get('selectedConfigGroup.isDefault')) {
        return groups.length ? groups.findProperty('isDefault', true) : null;
      } else {
        return groups.length ? groups.findProperty('name', this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) : null;
      }
    }
  },

  /**
   * Save changed configs and config groups
   * @method saveConfigs
   */
  saveConfigs: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    if (selectedConfigGroup.get('isDefault')) {

      var data = [];
      this.get('stepConfigs').forEach(function(stepConfig) {

        var serviceConfig = this.getServiceConfigToSave(stepConfig.get('serviceName'), stepConfig.get('configs'));

        if (serviceConfig)  {
          data.push(serviceConfig);
        }

      }, this);

      if (Em.isArray(data) && data.length) {
        this.putChangedConfigurations(data, true);
      } else {
        this.onDoPUTClusterConfigurations();
      }
    } else {

      this.get('stepConfigs').forEach(function(stepConfig) {
        var serviceName = stepConfig.get('serviceName');
        var configs = stepConfig.get('configs');
        var configGroup = this.getGroupFromModel(serviceName);
        if (configGroup) {
          if (configGroup.get('isDefault')) {

            var configsToSave = this.getServiceConfigToSave(serviceName, configs);

            if (configsToSave) {
              this.putChangedConfigurations([configsToSave], false);
            }

          } else {

            var overridenConfigs = this.getConfigsForGroup(configs, configGroup.get('name'));

            if (Em.isArray(overridenConfigs)) {
              this.saveGroup(overridenConfigs, configGroup, this.get('content.serviceName') === serviceName);
            }
          }
        }
      }, this);
    }
  },

  /*********************************** 0. HELPERS ********************************************/

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
   * Are some unsaved changes available
   * @returns {boolean}
   * @method hasUnsavedChanges
   */
  hasUnsavedChanges: function () {
    return !Em.isNone(this.get('hash')) && this.get('hash') != this.getHash();
  },

  /*********************************** 1. PRE SAVE CHECKS ************************************/

  /**
   * show some warning popups before user save configs
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
      }, this.completeSave.bind(this));
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

  /*********************************** 2. GENERATING DATA TO SAVE ****************************/

  /**
   * get config properties for that fileNames that was changed
   * @param stepConfigs
   * @private
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
   * get configs that belongs to config group
   * @param stepConfigs
   * @private
   * @param configGroupName
   */
  getConfigsForGroup: function(stepConfigs, configGroupName) {
    var overridenConfigs = [];

    stepConfigs.filterProperty('overrides').forEach(function (config) {
      overridenConfigs = overridenConfigs.concat(config.get('overrides'));
    });
    // find custom original properties that assigned to selected config group
    return overridenConfigs.concat(stepConfigs.filterProperty('group')
      .filter(function (config) {
        return config.get('group.name') == configGroupName;
      }));
  },

  /**
   *
   * @param serviceName
   * @param configs
   * @private
   * @returns {*}
   */
  getServiceConfigToSave: function(serviceName, configs) {

    if (serviceName === 'YARN') {
      configs = App.config.textareaIntoFileConfigs(configs, 'capacity-scheduler.xml');
    }

    //generates list of properties that was changed
    var modifiedConfigs = this.getModifiedConfigs(configs);
    var serviceFilenames = Object.keys(App.StackService.find(serviceName).get('configTypes')).map(function (type) {
      return App.config.getOriginalFileName(type);
    });

    // save modified original configs that have no group
    modifiedConfigs = this.saveSiteConfigs(modifiedConfigs.filter(function (config) {
      return !config.get('group');
    }));

    if (!Em.isArray(modifiedConfigs) || modifiedConfigs.length == 0) return null;

    var fileNamesToSave = modifiedConfigs.mapProperty('filename').concat(this.get('modifiedFileNames')).filter(function(filename) {
      return serviceFilenames.contains(filename);
    }).uniq();

    var configsToSave = this.generateDesiredConfigsJSON(modifiedConfigs, fileNamesToSave, this.get('serviceConfigVersionNote'));

    if (configsToSave.length > 0) {
      return JSON.stringify({
        Clusters: {
          desired_config: configsToSave
        }
      });
    } else {
      return null;
    }
  },

  /**
   * save site configs
   * @param configs
   * @private
   * @method saveSiteConfigs
   */
  saveSiteConfigs: function (configs) {
    this.formatConfigValues(configs);
    return configs;
  },

  /**
   * Represent boolean value as string (true => 'true', false => 'false') and trim other values
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

  /*********************************** 3. GENERATING JSON TO SAVE *****************************/

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
        var serviceName = this.get('content.serviceName');
        var serviceType = App.StackService.find().findProperty('serviceName',serviceName).get('serviceType');
        return ['HDFS', 'GLUSTERFS', 'RANGER_KMS'].contains(this.get('content.serviceName')) || serviceType === 'HCFS';
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
    var kdcTypesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');
    //TODO check for core-site
    if (this.get('heapsizeRegExp').test(name) && !this.get('heapsizeException').contains(name) && !(value).endsWith("m")) {
      return value += "m";
    }
    if (typeof property.get('value') === "boolean") {
      return property.get('value').toString();
    }
    switch (name) {
      case 'kdc_type':
        return Em.keys(kdcTypesMap).filter(function(key) {
            return kdcTypesMap[key] === property.get('value');
        })[0];
      case 'storm.zookeeper.servers':
      case 'nimbus.seeds':
        if (Em.isArray(value)) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return App.config.trimProperty(property, true);
    }
  },

  /*********************************** 4. AJAX REQUESTS **************************************/

  /**
   * save config group
   * @param overridenConfigs
   * @param selectedConfigGroup
   * @param showPopup
   */
  saveGroup: function(overridenConfigs, selectedConfigGroup, showPopup) {
    var groupHosts = [];
    var fileNamesToSave = overridenConfigs.mapProperty('filename').uniq();
    selectedConfigGroup.get('hosts').forEach(function (hostName) {
      groupHosts.push({"host_name": hostName});
    });
    var id = selectedConfigGroup.get('configGroupId');
    id = Em.isNone(id) ? selectedConfigGroup.get('id') : id;
    this.putConfigGroupChanges({
      ConfigGroup: {
        "id": id,
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
   * Saves configuration of set of sites. The provided data
   * contains the site name and tag to be used.
   * @param {Object[]} services
   * @param {boolean} showPopup
   * @return {$.ajax}
   * @method putChangedConfigurations
   */
  putChangedConfigurations: function (services, showPopup) {
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

  /*********************************** 5. AFTER SAVE INFO ************************************/

  /**
   * @private
   * @method putConfigGroupChangesSuccess
   */
  putConfigGroupChangesSuccess: function () {
    this.set('saveConfigsFlag', true);
    this.onDoPUTClusterConfigurations();
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
   * On save configs handler. Open save configs popup with appropriate message
   * and clear config dependencies list.
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
    this.clearDependentConfigs();
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
                components[name].push(App.format.role(item.ServiceComponentInfo.component_name, false));
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

  /*********************************** 6. ADDITIONAL *******************************************/

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {String} path
   * @param {object} callback - callback with action to change configs view(change group or version)
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (path, callback) {
    var self = this;
    var passwordWasChanged = this.get('passwordConfigsAreChanged');
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        showSaveWarning: true,
        showPasswordChangeWarning: passwordWasChanged,
        notesArea: Em.TextArea.extend({
          value: passwordWasChanged ? Em.I18n.t('dashboard.configHistory.info-bar.save.popup.notesForPasswordChange') : '',
          classNames: ['full-width'],
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder'),
          didInsertElement: function () {
            if (this.get('value')) {
              this.onChangeValue();
            }
          },
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      footerClass: Em.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer'),
        isSaveDisabled: function() {
          return self.get('isSubmitDisabled');
        }.property()
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
      case 'nimbus.seeds':
        if(Em.isArray(value)) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return value;
    }
  }
});
