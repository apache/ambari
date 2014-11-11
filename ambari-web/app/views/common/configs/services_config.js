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
        return this.get('controller.selectedService.serviceName') !== 'MISC';
      case 'mainServiceInfoConfigsController':
      case 'mainHostServiceConfigsController':
        return true;
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
    var filteredResult = this.get('categoryConfigs');

    if (selectedFilters.length > 0 || filter.length > 0 || this.get('state') === 'inDOM') {
      filteredResult.forEach(function (config) {
        var passesFilters = true;

        selectedFilters.forEach(function (filter) {
          if (config.get(filter.attributeName) !== filter.attributeValue) {
            passesFilters = false;
          }
        });

        if (!passesFilters) {
          config.set('isHiddenByFilter', true);
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
          config.set('isHiddenByFilter', !(searchString.toLowerCase().indexOf(filter) > -1));
        } else {
          config.set('isHiddenByFilter', false);
        }
      });
    }
    filteredResult = this.sortByIndex(filteredResult);
    filteredResult = filteredResult.filterProperty('isHiddenByFilter', false);

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
  }.observes('categoryConfigs', 'parentView.filter', 'parentView.columns.@each.selected'),

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
    return this.get('category.customCanAddProperty') || this.get('categoryConfigs').filterProperty('isHiddenByFilter', false).length > 0;
  }.property('category.customCanAddProperty', 'categoryConfigs.@each.isHiddenByFilter'),

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
