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
var validator = require('utils/validator');
var stringUtils = require('utils/string_utils');
var hostsUtils = require('utils/hosts');
var lazyLoading = require('utils/lazy_loading');

App.ServicesConfigView = Em.View.extend({
  templateName: require('templates/common/configs/services_config'),
  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
  }
});

App.ServiceConfigView = Em.View.extend({
  templateName: require('templates/common/configs/service_config'),
  isRestartMessageCollapsed: false,
  filter: '', //from template
  columns: [], //from template
  propertyFilterPopover: [Em.I18n.t('services.service.config.propertyFilterPopover.title'), Em.I18n.t('services.service.config.propertyFilterPopover.content')],
  canEdit: true, // View is editable or read-only?
  supportsHostOverrides: function () {
    switch (this.get('controller.name')) {
      case 'wizardStep7Controller':
        return App.supports.hostOverridesInstaller && (this.get('controller.selectedService.serviceName') !== 'MISC');
      case 'mainServiceInfoConfigsController':
        return App.supports.hostOverrides;
      case 'mainHostServiceConfigsController':
        return App.supports.hostOverridesHost;
      default:
        return false;
    }
  }.property('controller.name', 'controller.selectedService'),
  showConfigHistoryFeature: false,
  toggleRestartMessageView: function () {
    this.$('.service-body').toggle('blind', 200);
    this.set('isRestartMessageCollapsed', !this.get('isRestartMessageCollapsed'));
  },
  didInsertElement: function () {
    if(this.get('isNotEditable') === true) {
      this.set('canEdit', false);
    }
    if (this.$('.service-body')) {
      this.$('.service-body').hide();
    }
    App.tooltip($(".restart-required-property"), {html: true});
    App.tooltip($(".icon-lock"), {placement: 'right'});
    this.checkCanEdit();
  },

  /**
   * Check if we should show Custom Property category
   */
  checkCanEdit: function () {
    var controller = this.get('controller');
    if (!controller.get('selectedService.configCategories')) {
      return;
    }

    if (controller.get('selectedConfigGroup')) {
      controller.get('selectedService.configCategories').filterProperty('siteFileName').forEach(function (config) {
        config.set('customCanAddProperty', config.get('canAddProperty'));
      });
    }

  }.observes(
    'App.router.mainServiceInfoConfigsController.selectedConfigGroup.name',
    'App.router.wizardStep7Controller.selectedConfigGroup.name'
  )
});


App.ServiceConfigsByCategoryView = Ember.View.extend(App.UserPref, {

  templateName: require('templates/common/configs/service_config_category'),

  classNames: ['accordion-group', 'common-config-category'],
  classNameBindings: ['category.name', 'isShowBlock::hidden'],

  content: null,
  category: null,
  service: null,
  canEdit: true, // View is editable or read-only?
  serviceConfigs: null, // General, Advanced, NameNode, SNameNode, DataNode, etc.
  // total number of
  // hosts (by
  // default,
  // cacheable )
  categoryConfigs: function () {
    var categoryConfigs = this.get('categoryConfigsAll');
    return this.orderContentAtLast(categoryConfigs).filterProperty('isVisible', true);
  }.property('categoryConfigsAll.@each.isVisible').cacheable(),

  /**
   * This method provides all the properties which apply
   * to this category, irrespective of visibility. This
   * is helpful in Oozie/Hive database configuration, where
   * MySQL etc. database options don't show up, because
   * they were not visible initially.
   */
  categoryConfigsAll: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'));
  }.property('serviceConfigs.@each').cacheable(),

  /**
   * Re-order the configs to list content displayType properties at last in the category
   * @param categoryConfigs
   */
  orderContentAtLast: function(categoryConfigs) {
    var contentProperties =  categoryConfigs.filterProperty('displayType','content');
    var self = this;
    if (!contentProperties.length) {
      return categoryConfigs
    } else {
      var comparator;
      return categoryConfigs.sort(function(a,b){
        var aContent = contentProperties.someProperty('name', a.get('name'));
        var bContent = contentProperties.someProperty('name', b.get('name'));
        if (aContent && bContent) {
          return 0;
        } else if (aContent){
          return 1;
        } else {
          return -1;
        }
      });
    }
  },

  /**
   * Warn/prompt user to adjust Service props when changing user/groups in Misc
   * Is triggered when user ended editing text field
   */
  miscConfigChange: function (manuallyChangedProperty) {
    var changedProperty;
    if(manuallyChangedProperty.get("id")){
      changedProperty = [manuallyChangedProperty];
    }else{
      changedProperty = this.get("serviceConfigs").filterProperty("editDone", true);
    }

    if (changedProperty.length > 0) {
      changedProperty = changedProperty.objectAt(0);
    } else {
      return;
    }
    if (this.get('controller.selectedService.serviceName') == 'MISC') {
      var newValue = changedProperty.get("value");
      var stepConfigs = this.get("controller.stepConfigs");
      this.affectedProperties = [];
      var curConfigs = "";
      var affectedPropertyName = App.get('isHadoop2Stack') ? "dfs.permissions.superusergroup" : "dfs.permissions.supergroup";
      if (changedProperty.get("name") == "hdfs_user") {
        curConfigs = stepConfigs.findProperty("serviceName", "HDFS").get("configs");
        if (newValue != curConfigs.findProperty("name", affectedPropertyName).get("value")) {
          this.affectedProperties.push(
            {
              serviceName: "HDFS",
              propertyName: affectedPropertyName,
              propertyDisplayName: affectedPropertyName,
              newValue: newValue,
              curValue: curConfigs.findProperty("name", affectedPropertyName).get("value"),
              changedPropertyName: "hdfs_user"
            }
          );
        }
        if ($.trim(newValue) != $.trim(curConfigs.findProperty("name", "dfs.cluster.administrators").get("value"))) {
          this.affectedProperties.push(
            {
              serviceName: "HDFS",
              propertyName: "dfs.cluster.administrators",
              propertyDisplayName: "dfs.cluster.administrators",
              newValue: " " + $.trim(newValue),
              curValue: curConfigs.findProperty("name", "dfs.cluster.administrators").get("value"),
              changedPropertyName: "hdfs_user"
            }
          );
        }
      } else if (changedProperty.get("name") == "hbase_user" && !App.get('isHadoop2Stack')) {
        curConfigs = stepConfigs.findProperty("serviceName", "HDFS").get("configs");
        if (newValue != curConfigs.findProperty("name", "dfs.block.local-path-access.user").get("value")) {
          this.affectedProperties.push(
            {
              serviceName: "HDFS",
              propertyName: "dfs.block.local-path-access.user",
              propertyDisplayName: "dfs.block.local-path-access.user",
              newValue: newValue,
              curValue: curConfigs.findProperty("name", "dfs.block.local-path-access.user").get("value"),
              changedPropertyName: "hbase_user"
            }
          );
        }
        var hbaseCurConfigs = stepConfigs.findProperty("serviceName", "HBASE").get("configs");
        if (newValue != hbaseCurConfigs.findProperty("name", "hbase.superuser").get("value")) {
          this.affectedProperties.push(
            {
              serviceName: "HBASE",
              propertyName: "hbase.superuser",
              propertyDisplayName: "hbase.superuser",
              newValue: newValue,
              curValue: hbaseCurConfigs.findProperty("name", "hbase.superuser").get("value"),
              changedPropertyName: "hbase_user"
            }
          );
        }
      } else if (changedProperty.get("name") == "user_group") {
        if (!((this.get("controller.selectedServiceNames").indexOf("MAPREDUCE") >= 0) || (this.get("controller.selectedServiceNames").indexOf("YARN") >= 0))) {
          return;
        }
        if(this.get("controller.selectedServiceNames").indexOf("MAPREDUCE") >= 0) {
          curConfigs = stepConfigs.findProperty("serviceName", "MAPREDUCE").get("configs");
          if (newValue != curConfigs.findProperty("name", "mapreduce.tasktracker.group").get("value")) {
            this.affectedProperties.push(
              {
                serviceName: "MAPREDUCE",
                propertyName: "mapreduce.tasktracker.group",
                propertyDisplayName: "mapreduce.tasktracker.group",
                newValue: newValue,
                curValue: curConfigs.findProperty("name", "mapreduce.tasktracker.group").get("value"),
                changedPropertyName: "user_group"
              }
            )
          }
          if ($.trim(newValue) != $.trim(curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"))) {
            this.affectedProperties.push(
              {
                serviceName: "MAPREDUCE",
                propertyName: "mapreduce.cluster.administrators",
                propertyDisplayName: "mapreduce.cluster.administrators",
                newValue: " " + $.trim(newValue),
                curValue: curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"),
                changedPropertyName: "user_group"
              }
            );
          }
        }
        if(this.get("controller.selectedServiceNames").indexOf("MAPREDUCE2") >= 0) {
          curConfigs = stepConfigs.findProperty("serviceName", "MAPREDUCE2").get("configs");
          if ($.trim(newValue) != $.trim(curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"))) {
            this.affectedProperties.push(
              {
                serviceName: "MAPREDUCE2",
                propertyName: "mapreduce.cluster.administrators",
                propertyDisplayName: "mapreduce.cluster.administrators",
                newValue: " " + $.trim(newValue),
                curValue: curConfigs.findProperty("name", "mapreduce.cluster.administrators").get("value"),
                changedPropertyName: "user_group"
              }
            );
          }
        }
        if (this.get("controller.selectedServiceNames").indexOf("YARN") >= 0) {
        curConfigs = stepConfigs.findProperty("serviceName", "YARN").get("configs");
        if (newValue != curConfigs.findProperty("name", "yarn.nodemanager.linux-container-executor.group").get("value")) {
          this.affectedProperties.push(
            {
              serviceName: "YARN",
              propertyName: "yarn.nodemanager.linux-container-executor.group",
              propertyDisplayName: "yarn.nodemanager.linux-container-executor.group",
              newValue: newValue,
              curValue: curConfigs.findProperty("name", "yarn.nodemanager.linux-container-executor.group").get("value"),
              changedPropertyName: "user_group"
            }
          )
        }
        }
      }
      if (this.affectedProperties.length > 0 && !this.get("controller.miscModalVisible")) {
        this.newAffectedProperties = this.affectedProperties;
        var self = this;
        return App.ModalPopup.show({
          classNames: ['modal-690px-width'],
          showCloseButton: false,
          header: "Warning: you must also change these Service properties",
          onApply: function () {
            self.get("newAffectedProperties").forEach(function (item) {
              self.get("controller.stepConfigs").findProperty("serviceName", item.serviceName).get("configs")
                .findProperty("name", item.propertyName).set("value", item.newValue);
            });
            self.get("controller").set("miscModalVisible", false);
            this.hide();
          },
          onIgnore: function () {
            self.get("controller").set("miscModalVisible", false);
            this.hide();
          },
          onUndo: function () {
            var affected = self.get("newAffectedProperties").objectAt(0);
            self.get("controller.stepConfigs").findProperty("serviceName", "MISC").get("configs")
              .findProperty("name", affected.changedPropertyName).set("value", $.trim(affected.curValue));
            self.get("controller").set("miscModalVisible", false);
            this.hide();
          },
          footerClass: Ember.View.extend({
            classNames: ['modal-footer'],
            templateName: require('templates/common/configs/propertyDependence_footer')
          }),
          bodyClass: Ember.View.extend({
            templateName: require('templates/common/configs/propertyDependence'),
            controller: this,
            propertyChange: self.get("newAffectedProperties"),
            didInsertElement: function () {
              self.get("controller").set("miscModalVisible", true);
            }
          })
        });
      }
    }
  }.observes('categoryConfigs.@each.editDone'),

  /**
   * When the view is in read-only mode, it marks
   * the properties as read-only.
   */
  updateReadOnlyFlags: function () {
    var configs = this.get('serviceConfigs');
    var canEdit = this.get('canEdit');
    if (!canEdit && configs) {
      configs.forEach(function(c){
        c.set('isEditable', false);
        var overrides = c.get('overrides');
        if (overrides!=null) {
          overrides.setEach('isEditable', false);
        }
      });
    }
  },

  /**
   * Filtered <code>categoryConfigs</code> array. Used to show filtered result
   */
  filteredCategoryConfigs: function () {
    $('.popover').remove();
    var filter = this.get('parentView.filter').toLowerCase();
    var selectedFilters = this.get('parentView.columns').filterProperty('selected');
    var filteredResult = this.get('categoryConfigs')

    if (selectedFilters.length > 0 || filter.length > 0 || this.get('state') === 'inDOM') {
      filteredResult = filteredResult.filter(function (config) {
        var passesFilters = true;

        selectedFilters.forEach(function (filter) {
          if (config.get(filter.attributeName) !== filter.attributeValue) {
            passesFilters = false;
          }
        });

        if (!passesFilters) {
          return false;
        }

        var searchString = config.get('defaultValue') + config.get('description') +
          config.get('displayName') + config.get('name') + config.get('value');

        if (config.get('overrides')) {
          config.get('overrides').forEach(function (overriddenConf) {
            searchString += overriddenConf.get('value') + overriddenConf.get('group.name');
          });
        }

        if (filter != null && typeof searchString === "string") {
          return searchString.toLowerCase().indexOf(filter) > -1;
        } else {
          return true;
        }
      });
    }
    filteredResult = this.sortByIndex(filteredResult);

    if (filter && filteredResult.length ) {
      if (typeof this.get('category.collapsedByDefault') === 'undefined') {
        // Save state
        this.set('category.collapsedByDefault', this.get('category.isCollapsed'));
      }
      this.set('category.isCollapsed', false);
    } else if (filter && !filteredResult.length) {
      this.set('category.isCollapsed', true);
    } else if (!filter && typeof this.get('category.collapsedByDefault') !== 'undefined') {
      // If user clear filter -- restore defaults
      this.set('category.isCollapsed', this.get('category.collapsedByDefault'));
      this.set('category.collapsedByDefault', undefined);
    }

    var categoryBlock = $('.' + this.get('category.name').split(' ').join('.') + '>.accordion-body');
    filteredResult.length && !this.get('category.isCollapsed') ? categoryBlock.show() : categoryBlock.hide();
    return filteredResult;
  }.property('categoryConfigs', 'parentView.filter', 'parentView.columns.@each.selected').cacheable(),

  /**
   * sort configs in current category by index
   * @param configs
   * @return {*}
   */
  sortByIndex: function (configs) {
    var sortedConfigs = [];
    var unSorted = [];
    if (!configs.someProperty('index')) {
      return configs;
    }
    configs.forEach(function (config) {
      var index = config.get('index');
      if ((index !== null) && isFinite(index)) {
        sortedConfigs[index] ? sortedConfigs.splice(index, 0, config) : sortedConfigs[index] = config;
      } else {
        unSorted.push(config);
      }
    });
    // remove undefined elements from array
    sortedConfigs = sortedConfigs.filter(function (config) {
      return config !== undefined;
    });
    return sortedConfigs.concat(unSorted);
  },
  /**
   * Onclick handler for Config Group Header. Used to show/hide block
   */
  onToggleBlock: function () {
    this.$('.accordion-body').toggle('blind', 500);
    this.set('category.isCollapsed', !this.get('category.isCollapsed'));
  },

  /**
   * Should we show config group or not
   */
  isShowBlock: function () {
    return this.get('category.customCanAddProperty') || this.get('filteredCategoryConfigs').length > 0;
  }.property('category.customCanAddProperty', 'filteredCategoryConfigs.length'),

  didInsertElement: function () {
    var isCollapsed = this.get('category.isCollapsed') == undefined ? (this.get('category.name').indexOf('Advanced') != -1 || this.get('category.name').indexOf('CapacityScheduler') != -1) : this.get('category.isCollapsed');
    var self = this;
    this.set('category.isCollapsed', isCollapsed);
    if (isCollapsed) {
      this.$('.accordion-body').hide();
    } else {
      this.$('.accordion-body').show();
    }
    $('#serviceConfig').tooltip({
      selector: '[data-toggle=tooltip]',
      placement: 'top'
    });
    this.updateReadOnlyFlags();
    Em.run.next(function() {
      self.updateReadOnlyFlags();
    });
  },

  willDestroyElement: function () {
    if (this.get('parentView.controller.name') == 'mainServiceInfoConfigsController') {
      this.get('categoryConfigsAll').forEach(function (item) {
        item.set('isVisible', false);
      });
    }
  },

  /**
   * If added/removed a serverConfigObject, this property got updated.
   * Without this property, all serviceConfigs Objects will show up even if some was collapsed before.
   */
  isCategoryBodyVisible: function () {
    return this.get('category.isCollapsed')? "display: none;" : "display: block;"
  }.property('serviceConfigs.length'),

  childView: App.ServiceConfigsOverridesView,
  changeFlag: Ember.Object.create({
    val: 1
  }),
  isOneOfAdvancedSections: function () {
    var category = this.get('category');
    return category.indexOf("Advanced") != -1;
  },

  persistKey: function () {
    return 'admin-bulk-add-properties-' + App.router.get('loginName');
  },

  showAddPropertyWindow: function () {
    var persistController = this;
    var modePersistKey = this.persistKey();
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');

    persistController.getUserPref(modePersistKey).pipe(function (data) {
      return !!data;
    }, function () {
      return false;
    }).always((function (isBulkMode) {

      var category = this.get('category');
      var siteFileName = category.get('siteFileName');

      var service = this.get('service');
      var serviceName = service.get('serviceName');

      var secureConfigs = this.get('controller.secureConfigs').filterProperty('filename', siteFileName);

      function isSecureConfig(configName) {
        return !!secureConfigs.findProperty('name', configName);
      }

      var configsOfFile = service.get('configs').filterProperty('filename', siteFileName);
      var siteFileProperties = App.config.get('configMapping').all().filterProperty('filename', siteFileName);

      function shouldSupportFinal(filename) {
        var stackService = App.StackService.find().findProperty('serviceName', serviceName);
        var supportsFinal = App.config.getConfigTypesInfoFromService(stackService).supportsFinal;
        var matchingConfigType = supportsFinal.find(function (configType) {
          return filename.startsWith(configType);
        });
        return !!matchingConfigType;
      }

      var supportsFinal = shouldSupportFinal(siteFileName);

      function isDuplicatedConfigKey(name) {
        return siteFileProperties.findProperty('name', name) || configsOfFile.findProperty('name', name);
      }

      var serviceConfigs = this.get('serviceConfigs');

      function createProperty(propertyName, propertyValue) {
        serviceConfigs.pushObject(App.ServiceConfigProperty.create({
          name: propertyName,
          displayName: propertyName,
          value: propertyValue,
          displayType: stringUtils.isSingleLine(propertyValue) ? 'advanced' : 'multiLine',
          isSecureConfig: isSecureConfig(propertyName),
          category: category.get('name'),
          id: 'site property',
          serviceName: serviceName,
          defaultValue: null,
          supportsFinal: supportsFinal,
          filename: siteFileName || '',
          isUserProperty: true,
          isNotSaved: true,
          group: selectedConfigGroup.get('isDefault') ? null : selectedConfigGroup,
          isOverridable: selectedConfigGroup.get('isDefault')
        }));
      }

      var serviceConfigObj = Ember.Object.create({
        isBulkMode: isBulkMode,
        bulkConfigValue: '',
        bulkConfigError: false,
        bulkConfigErrorMessage: '',

        name: '',
        value: '',
        isKeyError: false,
        showFilterLink: false,
        errorMessage: '',
        observeAddPropertyValue: function () {
          var name = this.get('name');
          if (name.trim() != '') {
            if (validator.isValidConfigKey(name)) {
              if (!isDuplicatedConfigKey(name)) {
                this.set('showFilterLink', false);
                this.set('isKeyError', false);
                this.set('errorMessage', '');
              } else {
                this.set('showFilterLink', true);
                this.set('isKeyError', true);
                this.set('errorMessage', Em.I18n.t('services.service.config.addPropertyWindow.error.derivedKey'));
              }
            } else {
              this.set('showFilterLink', false);
              this.set('isKeyError', true);
              this.set('errorMessage', Em.I18n.t('form.validator.configKey'));
            }
          } else {
            this.set('showFilterLink', false);
            this.set('isKeyError', true);
            this.set('errorMessage', Em.I18n.t('services.service.config.addPropertyWindow.error.required'));
          }
        }.observes('name')
      });

      function processConfig(config, callback) {
        var lines = config.split('\n');
        var errorMessages = [];
        var parsedConfig = {};
        var propertyCount = 0;

        function lineNumber(index) {
          return Em.I18n.t('services.service.config.addPropertyWindow.error.lineNumber').format(index + 1);
        }

        lines.forEach(function (line, index) {
          if (line.trim() === '') {
            return;
          }
          var delimiter = '=';
          var delimiterPosition = line.indexOf(delimiter);
          if (delimiterPosition === -1) {
            errorMessages.push(lineNumber(index) + Em.I18n.t('services.service.config.addPropertyWindow.error.format'));
            return;
          }
          var key = Em.Handlebars.Utils.escapeExpression(line.slice(0, delimiterPosition).trim());
          var value = line.slice(delimiterPosition + 1);
          if (validator.isValidConfigKey(key)) {
            if (!isDuplicatedConfigKey(key) && !(key in parsedConfig)) {
              parsedConfig[key] = value;
              propertyCount++;
            } else {
              errorMessages.push(lineNumber(index) + Em.I18n.t('services.service.config.addPropertyWindow.error.derivedKey.specific').format(key));
            }
          } else {
            errorMessages.push(lineNumber(index) + Em.I18n.t('form.validator.configKey.specific').format(key));
          }
        });

        if (errorMessages.length > 0) {
          callback(errorMessages.join('<br>'), parsedConfig);
        } else if (propertyCount === 0) {
          callback(Em.I18n.t('services.service.config.addPropertyWindow.propertiesPlaceholder', parsedConfig));
        } else {
          callback(null, parsedConfig);
        }
      }

      App.ModalPopup.show({
        classNames: ['sixty-percent-width-modal'],
        header: 'Add Property',
        primary: 'Add',
        secondary: 'Cancel',
        onPrimary: function () {
          if (serviceConfigObj.isBulkMode) {
            var popup = this;
            processConfig(serviceConfigObj.bulkConfigValue, function (error, parsedConfig) {
              if (error) {
                serviceConfigObj.set('bulkConfigError', true);
                serviceConfigObj.set('bulkConfigErrorMessage', error);
              } else {
                for (var key in parsedConfig) {
                  if (parsedConfig.hasOwnProperty(key)) {
                    createProperty(key, parsedConfig[key]);
                  }
                }
                popup.hide();
              }
            });
          } else {
            serviceConfigObj.observeAddPropertyValue();
            /**
             * For the first entrance use this if (serviceConfigObj.name.trim() != '')
             */
            if (!serviceConfigObj.isKeyError) {
              createProperty(serviceConfigObj.get('name'), serviceConfigObj.get('value'));
              this.hide();
            }
          }
        },
        bodyClass: Ember.View.extend({
          fileName: siteFileName,
          templateName: require('templates/common/configs/addPropertyWindow'),
          controllerBinding: 'App.router.mainServiceInfoConfigsController',
          serviceConfigObj: serviceConfigObj,
          didInsertElement: function() {
            App.tooltip(this.$("[data-toggle=tooltip]"),{
              placement: "top"
            });
          },
          toggleBulkMode: function () {
            var newMode = !this.serviceConfigObj.get('isBulkMode');
            this.serviceConfigObj.set('isBulkMode', newMode);
            persistController.postUserPref(modePersistKey, newMode);
          },
          filterByKey: function (event) {
            var controller = (App.router.get('currentState.name') != 'configs')
              ? App.router.get('wizardStep7Controller')
              : App.router.get('mainServiceInfoConfigsController');
            this.get('parentView').onClose();
            controller.set('filter', event.view.get('serviceConfigObj.name'));
          }
        })
      });

    }).bind(this));
  },

  toggleFinalFlag: function (event) {
    var serviceConfigProperty = event.contexts[0];
    if (serviceConfigProperty.get('isNotEditable')) {
      return;
    }
    serviceConfigProperty.set('isFinal', !serviceConfigProperty.get('isFinal'));
  },

  /**
   * Removes the top-level property from list of properties.
   * Should be only called on user properties.
   */
  removeProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    this.get('serviceConfigs').removeObject(serviceConfigProperty);
    // push config's file name if this config was stored on server
    if (!serviceConfigProperty.get('isNotSaved')) {
      this.get('controller').get('modifiedFileNames').push(serviceConfigProperty.get('filename'));
    }
    Em.$('body>.tooltip').remove(); //some tooltips get frozen when their owner's DOM element is removed
  },

  /**
   * Restores given property's value to be its default value.
   * Does not update if there is no default value.
   */
  doRestoreDefaultValue: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var value = serviceConfigProperty.get('value');
    var dValue = serviceConfigProperty.get('defaultValue');
    var supportsFinal = serviceConfigProperty.get('supportsFinal');
    var defaultIsFinal = serviceConfigProperty.get('defaultIsFinal');

    if (dValue != null) {
      if (serviceConfigProperty.get('displayType') === 'password') {
        serviceConfigProperty.set('retypedPassword', dValue);
      }
      serviceConfigProperty.set('value', dValue);
    }
    if (supportsFinal) {
      serviceConfigProperty.set('isFinal', defaultIsFinal);
    }
    this.miscConfigChange(serviceConfigProperty);
    Em.$('body>.tooltip').remove(); //some tooltips get frozen when their owner's DOM element is removed
  },

  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var serviceConfigController = this.get('controller');
    var selectedConfigGroup = serviceConfigController.get('selectedConfigGroup');
    var isInstaller = (this.get('controller.name') === 'wizardStep7Controller');
    var configGroups = (isInstaller) ? serviceConfigController.get('selectedService.configGroups') : serviceConfigController.get('configGroups');

    //user property is added, and it has not been saved, not allow override
    if(serviceConfigProperty.get('isUserProperty') && serviceConfigProperty.get('isNotSaved') && !isInstaller){
      App.ModalPopup.show({
        header: Em.I18n.t('services.service.config.configOverride.head'),
        body: Em.I18n.t('services.service.config.configOverride.body'),
        secondary: false
      });
      return;
    }
    if (selectedConfigGroup.get('isDefault')) {
      // Launch dialog to pick/create Config-group
      App.config.launchConfigGroupSelectionCreationDialog(this.get('service.serviceName'),
        configGroups, serviceConfigProperty, function (selectedGroupInPopup) {
          console.log("launchConfigGroupSelectionCreationDialog(): Selected/Created:", selectedGroupInPopup);
          if (selectedGroupInPopup) {
            serviceConfigController.set('overrideToAdd', serviceConfigProperty);
            serviceConfigController.set('selectedConfigGroup', selectedGroupInPopup);
          }
        }, isInstaller);
    } else {
      serviceConfigController.addOverrideProperty(serviceConfigProperty);
    }
  }
});

App.ServiceConfigContainerView = Em.ContainerView.extend({
  view: null,
  lazyLoading: null,
  pushView: function () {
    if (this.get('controller.selectedService')) {
      var self = this;
      var controllerRoute = 'App.router.' + this.get('controller.name');
      if (!this.get('view')) {
        this.get('childViews').pushObject(App.ServiceConfigView.create({
          templateName: require('templates/common/configs/service_config_wizard'),
          controllerBinding: controllerRoute,
          isNotEditableBinding: controllerRoute + '.isNotEditable',
          filterBinding: controllerRoute + '.filter',
          columnsBinding: controllerRoute + '.filterColumns',
          selectedServiceBinding: controllerRoute + '.selectedService',
          serviceConfigsByCategoryView: Em.ContainerView.create(),
          willDestroyElement: function () {
            $('.loading').append(Em.I18n.t('app.loadingPlaceholder'));
          },
          didInsertElement: function () {
            $('.loading').empty();
            this._super();
          }
        }));
      } else {
        this.get('childViews').pushObject(this.get('view'));
      }
      var categoriesToPush = [];
      this.get('controller.selectedService.configCategories').forEach(function (item) {
        var categoryView = item.get('isCustomView') ? (App.get('supports.capacitySchedulerUi') ? item.get('customView') : null) : App.ServiceConfigsByCategoryView;
        if (categoryView !== null) {
          categoriesToPush.pushObject(categoryView.extend({
            category: item,
            controllerBinding: controllerRoute,
            canEditBinding: 'parentView.canEdit',
            serviceBinding: controllerRoute + '.selectedService',
            serviceConfigsBinding: controllerRoute + '.selectedService.configs',
            supportsHostOverridesBinding: 'parentView.supportsHostOverrides'
          }));
        }
      });
      this.set('lazyLoading', lazyLoading.run({
        destination: self.get('childViews.lastObject.serviceConfigsByCategoryView.childViews'),
        source: categoriesToPush,
        initSize: 3,
        chunkSize: 3,
        delay: 200,
        context: this
      }));
    }
  },
  selectedServiceObserver: function () {
    if (this.get('childViews.length')) {
      var view = this.get('childViews.firstObject');
      if (view.get('serviceConfigsByCategoryView.childViews.length')) {
        view.get('serviceConfigsByCategoryView.childViews').clear();
      }
      view.removeFromParent();
      this.set('view', view);
    }
    //terminate lazy loading when switch service
    if (this.get('lazyLoading')) lazyLoading.terminate(this.get('lazyLoading'));
    this.pushView();
  }.observes('controller.selectedService')
});

App.ServiceConfigTab = Ember.View.extend({

  tagName: 'li',

  selectService: function (event) {
    this.set('controller.selectedService', event.context);
  },

  didInsertElement: function () {
    var serviceName = this.get('controller.selectedService.serviceName');
    this.$('a[href="#' + serviceName + '"]').tab('show');
  }
});

/**
 * custom view for capacity scheduler category
 * @type {*}
 */
App.ServiceConfigCapacityScheduler = App.ServiceConfigsByCategoryView.extend({
  templateName: require('templates/common/configs/capacity_scheduler'),
  category: null,
  service: null,
  serviceConfigs: null,
  customConfigs: function(){
    return App.config.get('preDefinedCustomConfigs');
  }.property('App.config.preDefinedCustomConfigs'),
  /**
   * configs filtered by capacity-scheduler category
   */
  categoryConfigs: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'));
  }.property('queueObserver', 'serviceConfigs.@each'),
  /**
   * rewrote method to avoid incompatibility with parent
   */
  filteredCategoryConfigs: function () {
    return this.get('categoryConfigs');
  }.property(),
  advancedConfigs: function () {
    return this.get('categoryConfigs').filterProperty('isQueue', undefined) || [];
  }.property('categoryConfigs.@each'),
  didInsertElement: function () {
    this._super();
    this.createEmptyQueue(this.get('customConfigs').filterProperty('isQueue'));
  },
  //list of fields which will be populated by default in a new queue
  fieldsToPopulate: function(){
    if(App.get('isHadoop2Stack')){
      return ["yarn.scheduler.capacity.root.<queue-name>.user-limit-factor",
      "yarn.scheduler.capacity.root.<queue-name>.state"];
    }
    return [
      "mapred.capacity-scheduler.queue.<queue-name>.minimum-user-limit-percent",
      "mapred.capacity-scheduler.queue.<queue-name>.user-limit-factor",
      "mapred.capacity-scheduler.queue.<queue-name>.supports-priority",
      "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks",
      "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks-per-user",
      "mapred.capacity-scheduler.queue.<queue-name>.init-accept-jobs-factor"
    ];
  }.property('App.isHadoop2Stack'),
  /**
   * create empty queue
   * take some queue then copy it and set all config values to null
   * @param customConfigs
   */
  createEmptyQueue: function (customConfigs) {
    var emptyQueue = {
      name: '<queue-name>',
      configs: []
    };
    var fieldsToPopulate = this.get('fieldsToPopulate');
    customConfigs.forEach(function (config) {
      var newConfig = $.extend({}, config);
      if (fieldsToPopulate.contains(config.name)) {
        newConfig.value = config.defaultValue;
      }
      newConfig = App.ServiceConfigProperty.create(newConfig);
      newConfig.validate();
      emptyQueue.configs.push(newConfig);
    });
    this.set('emptyQueue', emptyQueue);
  },
  deriveQueueNames: function(configs){
    var queueNames = [];
    configs.mapProperty('name').forEach(function(name){
      var queueName;
      if(App.get('isHadoop2Stack')){
        queueName = /^yarn\.scheduler\.capacity\.root\.(.*?)\./.exec(name);
      } else {
        queueName = /^mapred\.capacity-scheduler\.queue\.(.*?)\./.exec(name);
      }
      if(queueName){
        queueNames.push(queueName[1]);
      }
    });
    return queueNames.uniq();
  },
  queues: function(){
    var configs = this.get('categoryConfigs').filterProperty('isQueue', true);
    var queueNames = this.deriveQueueNames(configs);
    var queues = [];

    queueNames.forEach(function(queueName){
      queues.push({
        name: queueName,
        color: this.generateColor(queueName),
        configs: this.groupConfigsByQueue(queueName, configs)
      })
    }, this);
    return queues;
  }.property('queueObserver'),
  /**
   * group configs by queue
   * @param queueName
   * @param configs
   */
  groupConfigsByQueue: function (queueName, configs) {
    var customConfigs = [];
    var queue = [];
    this.get('customConfigs').forEach(function(_config){
      var copy = $.extend({}, _config);
      copy.name = _config.name.replace('<queue-name>', queueName);
      customConfigs.push(copy);
    });
    configs.forEach(function (config) {
      var customConfig = customConfigs.findProperty('name', config.get('name'));
      if (customConfig) {
        config.set('description', customConfig.description);
        config.set('displayName', customConfig.displayName);
        config.set('isRequired', customConfig.isRequired);
        config.set('unit', customConfig.unit);
        config.set('displayType', customConfig.displayType);
        config.set('valueRange', customConfig.valueRange);
        config.set('isVisible', customConfig.isVisible);
        config.set('inTable', customConfig.inTable);
        config.set('index', customConfig.index);
        queue.push(config);
      }
    });
    if(queue.length < customConfigs.length){
      this.addMissingProperties(queue, customConfigs);
    }
    return queue;
  },
  /**
   * add missing properties to queue when they don't come from server
   * @param queue
   * @param customConfigs
   */
  addMissingProperties: function(queue, customConfigs){
    customConfigs.forEach(function(_config){
      var serviceConfigProperty;
      if(!queue.someProperty('name', _config.name)){
        _config.value = _config.defaultValue;
        serviceConfigProperty = App.ServiceConfigProperty.create(_config);
        serviceConfigProperty.validate();
        queue.push(serviceConfigProperty);
      }
    }, this);
  },
  /**
   * convert queues to table content
   */
  tableContent: function () {
    var result = [];
    this.get('queues').forEach(function (queue) {
      var usersAndGroups = queue.configs.findProperty('name', this.getUserAndGroupNames(queue.name)[0]).get('value');
      usersAndGroups = (usersAndGroups) ? usersAndGroups.split(' ') : [''];
      if (usersAndGroups.length == 1) {
        usersAndGroups.push('');
      }
      var queueObject = {
        name: queue.name,
        color: 'background-color:' + queue.color + ';',
        configs: this.sortByIndex(queue.configs.filterProperty('inTable'))
      };
      //push acl_submit_jobs users
      queueObject.configs.unshift({
        value: usersAndGroups[1],
        inTable: true,
        displayName: Em.I18n.t('common.users')
      });
      //push acl_submit_jobs groups
      queueObject.configs.unshift({
        value: usersAndGroups[0],
        inTable: true,
        displayName: Em.I18n.t('services.mapReduce.config.queue.groups')
      });
      result.push(queueObject);
    }, this);
    return result;
  }.property('queues'),
  /**
   * create headers depending on existed properties in queue
   */
  tableHeaders: function(){
    var headers = [
      Em.I18n.t('services.mapReduce.config.queue.name')
    ];
    return (this.get('tableContent').length) ?
      headers.concat(this.get('tableContent').objectAt(0).configs.filterProperty('inTable').mapProperty('displayName')):
      headers;
  }.property('tableContent'),
  queueObserver: null,
  /**
   * uses as template for adding new queue
   */
  emptyQueue: {},
  /**
   * get capacities sum of queues except of current
   * @param queueName
   * @return {Number}
   */
  getQueuesCapacitySum: function(queueName){
    var capacitySum = 0;
    this.get('queues').filter(function(queue){
      return queue.name !== queueName;
    }).forEach(function(queue){
        capacitySum = capacitySum + window.parseInt(queue.configs.find(function(config){
          return config.get('name').substr(-9, 9) === '.capacity';
        }).get('value'));
      });
    return capacitySum;
  },
  /**
   * get names of configs, for users and groups, which have different names in HDP1 and HDP2
   * @param queueName
   * @return {Array}
   */
  getUserAndGroupNames: function(queueName){
    queueName = queueName || '<queue-name>';
    if(App.get('isHadoop2Stack') && this.get('controller.selectedService')) {
      if (this.get('controller.selectedService.serviceName') == "YARN") {
        return ['yarn.scheduler.capacity.root.' + queueName + '.acl_submit_jobs',
          'yarn.scheduler.capacity.root.' + queueName + '.acl_administer_jobs']
      }
      return ['mapred.queue.' + queueName + '.acl-submit-job',
        'mapred.queue.' + queueName + '.acl-administer-jobs']
    }
  },
  generateColor: function (str) {
    var hash = 0;
    for (var i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return '#' + Number(Math.abs(hash)).toString(16).concat('00000').substr(0, 6);
  },
  /**
   * add new queue
   * add created configs to serviceConfigs with current queue name
   * @param queue
   */
  addQueue: function (queue) {
    var serviceConfigs = this.get('serviceConfigs');
    var admin = [];
    var submit = [];
    var submitConfig;
    var adminConfig;
    queue.name = queue.configs.findProperty('name', 'queueName').get('value');
    queue.configs.forEach(function (config) {
      var adminName = this.getUserAndGroupNames()[1];
      var submitName = this.getUserAndGroupNames()[0];
      if(config.name == adminName){
        if (config.type == 'USERS') {
          admin[0] = config.value;
        }
        if (config.type == 'GROUPS') {
          admin[1] = config.value;
        }
        if (config.isQueue) {
          adminConfig = config;
        }
      }
      if(config.name == submitName){
        if (config.type == 'USERS') {
          submit[0] = config.value;
        }
        if (config.type == 'GROUPS') {
          submit[1] = config.value;
        }
        if (config.isQueue) {
          submitConfig = config;
        }
      }
      config.set('name', config.get('name').replace('<queue-name>', queue.name));
      config.set('value', config.get('value').toString());
      if (config.isQueue) {
        serviceConfigs.push(config);
      }
    }, this);
    adminConfig.set('value', admin.join(' '));
    submitConfig.set('value', submit.join(' '));
    this.set('queueObserver', App.dateTime());
  },
  /**
   * delete queue
   * delete configs from serviceConfigs which have current queue name
   * @param queue
   */
  deleteQueue: function (queue) {
    var serviceConfigs = this.get('serviceConfigs');
    var configNames = queue.configs.filterProperty('isQueue').mapProperty('name');
    for (var i = 0, l = serviceConfigs.length; i < l; i++) {
      if (configNames.contains(serviceConfigs[i].name)) {
        serviceConfigs.splice(i, 1);
        l--;
        i--;
      }
    }
    this.set('queueObserver', App.dateTime());
  },
  /**
   * save changes that was made to queue
   * edit configs from serviceConfigs which have current queue name
   * @param queue
   */
  editQueue: function (queue) {
    var serviceConfigs = this.get('serviceConfigs');
    var configNames = queue.configs.filterProperty('isQueue').mapProperty('name');
    serviceConfigs.forEach(function (_config) {
      var configName = _config.get('name');
      var admin = [];
      var submit = [];
      //comparison executes including 'queue.<queue-name>' to avoid false matches
      var queueNamePrefix = App.get('isHadoop2Stack') ? 'root.' : 'queue.';
      if (configNames.contains(_config.get('name'))) {
        if(configName == this.getUserAndGroupNames(queue.name)[0]){
          submit = queue.configs.filterProperty('name', configName);
          submit = submit.findProperty('type', 'USERS').get('value') + ' ' + submit.findProperty('type', 'GROUPS').get('value');
          _config.set('value', submit);
        } else if(configName == this.getUserAndGroupNames(queue.name)[1]){
          admin = queue.configs.filterProperty('name', configName);
          admin = admin.findProperty('type', 'USERS').get('value') + ' ' + admin.findProperty('type', 'GROUPS').get('value');
          _config.set('value', admin);
        } else {
          _config.set('value', queue.configs.findProperty('name', _config.get('name')).get('value').toString());
        }
        _config.set('name', configName.replace(queueNamePrefix + queue.name, queueNamePrefix + queue.configs.findProperty('name', 'queueName').get('value')));
      }
    }, this);
    this.set('queueObserver', App.dateTime());
  },
  pieChart: App.ChartPieView.extend({
    w: 200,
    h: 200,
    queues: null,
    didInsertElement: function () {
      this.update();
    },
    data: [{"label":"default", "value":100}],
    update: function () {
      var self = this;
      var data = [];
      var queues = this.get('queues');
      var capacitiesSum = 0;
      queues.forEach(function (queue) {
        var value = window.parseInt(queue.configs.find(function(_config){
          return _config.get('name').substr(-9, 9) === '.capacity';
        }).get('value'));
        data.push({
          label: queue.name,
          value: value,
          color: queue.color
        })
      });

      data.mapProperty('value').forEach(function (capacity) {
        capacitiesSum += capacity;
      });
      if (capacitiesSum < 100) {
        data.push({
          label: Em.I18n.t('common.empty'),
          value: (100 - capacitiesSum),
          color: 'transparent',
          isEmpty: true
        })
      }
      $(d3.select(this.get('selector'))[0]).children().remove();
      this.set('data', data);
      this.set('palette', new Rickshaw.Color.Palette({
        scheme: data.mapProperty('color')
      }));
      this.appendSvg();

      this.get('arcs')
        .on("click",function (d, i) {
          var event = {context: d.data.label};
          if (d.data.isEmpty !== true) self.get('parentView').queuePopup(event);
        }).on('mouseover', function (d, i) {
          var position = d3.svg.mouse(this);
          var label = $('#section_label');
          label.css('left', position[0] + 100);
          label.css('top', position[1] + 100);
          label.text(d.data.label);
          label.show();
        })
        .on('mouseout', function (d, i) {
          $('#section_label').hide();
        })

    }.observes('queues'),
    donut: d3.layout.pie().sort(null).value(function (d) {
      return d.value;
    })
  }),
  /**
   * open popup with chosen queue
   * @param event
   */
  queuePopup: function (event) {
    //if queueName was handed that means "Edit" mode, otherwise "Add" mode
    var queueName = event.context || null;
    var self = this;
    App.ModalPopup.show({
      didInsertElement: function () {
        if (queueName) {
          this.set('header', Em.I18n.t('services.mapReduce.config.editQueue'));
          this.set('secondary', Em.I18n.t('common.save'));
          if (self.get('queues').length > 1 && self.get('canEdit')) {
            this.set('delete', Em.I18n.t('common.delete'));
          }
        }
      },
      header: Em.I18n.t('services.mapReduce.config.addQueue'),
      secondary: Em.I18n.t('common.add'),
      primary: Em.I18n.t('common.cancel'),
      delete: null,
      isError: function () {
        if (!self.get('canEdit')) {
          return true;
        }
        var content = this.get('content');
        var configs = content.configs.filter(function (config) {
          return !(config.name == self.getUserAndGroupNames(content.name)[0] ||
            config.name == self.getUserAndGroupNames(content.name)[1] &&
              config.isQueue);
        });
        return configs.someProperty('isValid', false);
      }.property('content.configs.@each.isValid'),
      onDelete: function () {
        var view = this;
        App.ModalPopup.show({
          header: Em.I18n.t('popup.confirmation.commonHeader'),
          body: Em.I18n.t('hosts.delete.popup.body'),
          primary: Em.I18n.t('yes'),
          onPrimary: function () {
            self.deleteQueue(view.get('content'));
            view.hide();
            this.hide();
          }
        });
      },
      onSecondary: function () {
        if (queueName) {
          self.editQueue(this.get('content'));
        } else {
          self.addQueue(this.get('content'));
        }
        this.hide();
      },
      /**
       * Queue properties order:
       * 1. Queue Name
       * 2. Capacity
       * 3. Max Capacity
       * 4. Users
       * 5. Groups
       * 6. Admin Users
       * 7. Admin Groups
       * 8. Support Priority
       * ...
       */
      content: function () {
        var content = (queueName) ? self.get('queues').findProperty('name', queueName) : self.get('emptyQueue');
        var configs = [];
        // copy of queue configs
        content.configs.forEach(function (config, index) {
          if(config.get('name').substr(-9, 9) === '.capacity') {
            //added validation function for capacity property
            config.reopen({
              validate: function () {
                var value = this.get('value');
                var isError = false;
                var capacitySum = self.getQueuesCapacitySum(content.name);
                if (value == '') {
                  if (this.get('isRequired')) {
                    this.set('errorMessage', 'This is required');
                    isError = true;
                  } else {
                    return;
                  }
                }
                if (!isError) {
                  if (!validator.isValidInt(value)) {
                    this.set('errorMessage', 'Must contain digits only');
                    isError = true;
                  }
                }
                if (!isError) {
                  if ((capacitySum + parseInt(value)) > 100) {
                    isError = true;
                    this.set('errorMessage', 'The sum of capacities more than 100');
                  }
                  if (!isError) {
                    this.set('errorMessage', '');
                  }
                }
              }.observes('value')
            });
          }
          if (config.name == 'mapred.capacity-scheduler.queue.' + content.name + '.supports-priority') {
            if (config.get('value') == 'true' || config.get('value') === true) {
              config.set('value', true);
            } else {
              config.set('value', false);
            }
          }
          if(config.name === 'yarn.scheduler.capacity.root.' + content.name + '.state'){
            config.reopen({
              validate: function(){
                var value = this.get('value');
                this._super();
                if(!this.get('errorMessage')){
                  if(!(value === 'STOPPED' || value === 'RUNNING')){
                    this.set('errorMessage', 'State value should be RUNNING or STOPPED');
                  }
                }
              }.observes('value')
            })
          }
          configs[index] = App.ServiceConfigProperty.create(config);
        });
        content = {
          name: content.name,
          configs: configs
        };
        content = this.insertExtraConfigs(content);
        content.configs = self.sortByIndex(content.configs);
        return content;
      }.property(),
      footerClass: Ember.View.extend({
        classNames: ['modal-footer', 'host-checks-update'],
        templateName: require('templates/common/configs/queuePopup_footer')
      }),
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/configs/queuePopup_body')
      }),
      /**
       * Insert extra config in popup according to queue
       *
       * the mapred.queue.default.acl-administer-jobs turns into two implicit configs:
       * "Admin Users" field and "Admin Groups" field
       * the mapred.queue.default.acl-submit-job turns into two implicit configs:
       * "Users" field and "Groups" field
       * Add implicit config that contain "Queue Name"
       * @param content
       * @return {*}
       */
      insertExtraConfigs: function (content) {
        var that = this;
        var admin = content.configs.findProperty('name', self.getUserAndGroupNames(content.name)[1]).get('value');
        var submit = content.configs.findProperty('name', self.getUserAndGroupNames(content.name)[0]).get('value');
        admin = (admin) ? admin.split(' ') : [''];
        submit = (submit) ? submit.split(' ') : [''];
        if (admin.length < 2) {
          admin.push('');
        }
        if (submit.length < 2) {
          submit.push('');
        }
        var nameField = App.ServiceConfigProperty.create({
          name: 'queueName',
          displayName: Em.I18n.t('services.mapReduce.extraConfig.queue.name'),
          description: Em.I18n.t('services.mapReduce.description.queue.name'),
          value: (content.name == '<queue-name>') ? '' : content.name,
          validate: function () {
            var queueNames = self.get('queues').mapProperty('name');
            var value = this.get('value');
            var isError = false;
            var regExp = /^[a-z]([\_\-a-z0-9]{0,50})\$?$/i;
            if (value == '') {
              if (this.get('isRequired')) {
                this.set('errorMessage', 'This is required');
                isError = true;
              } else {
                return;
              }
            }
            if (!isError) {
              if ((queueNames.indexOf(value) !== -1) && (value != content.name)) {
                this.set('errorMessage', 'Queue name is already used');
                isError = true;
              }
            }
            if (!isError) {
              if (!regExp.test(value)) {
                this.set('errorMessage', 'Incorrect input');
                isError = true;
              }
            }
            if (!isError) {
              this.set('errorMessage', '');
            }
          }.observes('value'),
          isRequired: true,
          isVisible: true,
          isEditable: self.get('canEdit'),
          index: 0
        });
        nameField.validate();
        content.configs.unshift(nameField);

        var submitUser = App.ServiceConfigProperty.create({
          name: self.getUserAndGroupNames(content.name)[0],
          displayName: Em.I18n.t('common.users'),
          value: submit[0],
          description: Em.I18n.t('services.mapReduce.description.queue.submit.user'),
          isRequired: true,
          isVisible: true,
          type: 'USERS',
          displayType: "UNIXList",
          isEditable: self.get('canEdit'),
          index: 3
        });

        var submitGroup = App.ServiceConfigProperty.create({
          name: self.getUserAndGroupNames(content.name)[0],
          displayName: Em.I18n.t('services.mapReduce.config.queue.groups'),
          description: Em.I18n.t('services.mapReduce.description.queue.submit.group'),
          value: submit[1],
          isRequired: true,
          isVisible: true,
          "displayType": "UNIXList",
          type: 'GROUPS',
          isEditable: self.get('canEdit'),
          index: 4
        });

        var adminUser = App.ServiceConfigProperty.create({
          name: self.getUserAndGroupNames(content.name)[1],
          displayName: Em.I18n.t('services.mapReduce.config.queue.adminUsers'),
          description: Em.I18n.t('services.mapReduce.description.queue.admin.user'),
          value: admin[0],
          isRequired: true,
          isVisible: true,
          type: 'USERS',
          displayType: "UNIXList",
          isEditable: self.get('canEdit'),
          index: 5
        });

        var adminGroup = App.ServiceConfigProperty.create({
          name: self.getUserAndGroupNames(content.name)[1],
          displayName: Em.I18n.t('services.mapReduce.config.queue.adminGroups'),
          value: admin[1],
          description: Em.I18n.t('services.mapReduce.description.queue.admin.group'),
          isRequired: true,
          isVisible: true,
          "displayType": "UNIXList",
          type: 'GROUPS',
          isEditable: self.get('canEdit'),
          index: 6
        });

        submitUser.reopen({
          validate: function () {
            that.userGroupValidation(this, submitGroup);
          }.observes('value')
        });
        submitGroup.reopen({
          validate: function () {
            that.userGroupValidation(this, submitUser);
          }.observes('value')
        });
        adminUser.reopen({
          validate: function () {
            that.userGroupValidation(this, adminGroup);
          }.observes('value')
        });
        adminGroup.reopen({
          validate: function () {
            that.userGroupValidation(this, adminUser);
          }.observes('value')
        });

        submitUser.validate();
        adminUser.validate();
        content.configs.push(submitUser);
        content.configs.push(submitGroup);
        content.configs.push(adminUser);
        content.configs.push(adminGroup);

        return content;
      },
      /**
       * Validate by follow rules:
       * Users can be blank. If it is blank, Groups must not be blank.
       * Groups can be blank. If it is blank, Users must not be blank.
       * @param context
       * @param boundConfig
       */
      userGroupValidation: function (context, boundConfig) {
        if (context.get('value') == '') {
          if (boundConfig.get('value') == '') {
            context._super();
          } else {
            boundConfig.validate();
          }
        } else {
          if (boundConfig.get('value') == '') {
            boundConfig.set('errorMessage', '');
          }
          context._super();
        }
      }
    })
  }
});
