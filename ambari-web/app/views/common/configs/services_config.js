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
  canEdit: true, // View is editable or read-only?
  toggleRestartMessageView: function(){
    this.$('.service-body').toggle('blind', 200);
    this.set('isRestartMessageCollapsed', !this.get('isRestartMessageCollapsed'));
  },
  didInsertElement: function () {
    this.$('.service-body').hide();
    $(".restart-required-property").tooltip({html:true});
  }
});


App.ServiceConfigsByCategoryView = Ember.View.extend({

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
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name')).filterProperty('isVisible', true);
  }.property('serviceConfigs.@each', 'categoryConfigsAll.@each.isVisible').cacheable(),

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
   * When the view is in read-only mode, it marks
   * the properties as read-only.
   */
  updateReadOnlyFlags: function(){
    var configs = this.get('serviceConfigs');
    var canEdit = this.get('canEdit');
    if(!canEdit && configs){
      configs.forEach(function(config){
        config.set('isEditable', false);
      });
    }
  },

  /**
   * Filtered <code>categoryConfigs</code> array. Used to show filtered result
   */
  filteredCategoryConfigs: function() {
    var filter = this.get('parentView.filter').toLowerCase();
    var isOnlyModified = this.get('parentView.columns').length && this.get('parentView.columns')[1].get('selected');
    var isOnlyOverridden = this.get('parentView.columns').length && this.get('parentView.columns')[0].get('selected');
    var isOnlyRestartRequired = this.get('parentView.columns').length && this.get('parentView.columns')[2].get('selected');
    var filteredResult = this.get('categoryConfigs').filter(function(config){

      if(isOnlyModified && !config.get('isNotDefaultValue')){
        return false;
      }
      
      if(isOnlyOverridden && !config.get('isOverridden')){
        return false;
      }

      if(isOnlyRestartRequired && !config.get('isRestartRequired')){
        return false;
      }

      var searchString = config.get('defaultValue') + config.get('description') +
        config.get('displayName') + config.get('name');

      return searchString.toLowerCase().indexOf(filter) > -1;
    });
    filteredResult = this.sortByIndex(filteredResult);
    return filteredResult;
  }.property('categoryConfigs','parentView.filter', 'parentView.columns.@each.selected'),

  /**
   * sort configs in current category by index
   * @param configs
   * @return {*}
   */
  sortByIndex: function(configs){
    var sortedConfigs = [];
    var unSorted = [];
    if (!configs.someProperty('index')) {
      return configs;
    }
    configs.forEach(function (config) {
      var index = config.get('index');
      if ((index !== null) && isFinite(index)) {
        sortedConfigs[index] ? sortedConfigs.splice(index, 0 ,config) : sortedConfigs[index] = config;
      } else {
        unSorted.push(config);
      }
    });
    // remove undefined elements from array
    sortedConfigs = sortedConfigs.filter(function(config){
      if(config !== undefined) return true;
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
  isShowBlock: function(){
    return this.get('category.canAddProperty') || this.get('filteredCategoryConfigs').length > 0;
  }.property('category.canAddProperty', 'filteredCategoryConfigs.length'),

  didInsertElement: function () {
    var isCollapsed = (this.get('category.name').indexOf('Advanced') != -1);
    this.set('category.isCollapsed', isCollapsed);
    if(isCollapsed){
      this.$('.accordion-body').hide();
    }
    this.updateReadOnlyFlags();
  },
  childView: App.ServiceConfigsOverridesView,
  changeFlag: Ember.Object.create({
    val: 1
  }),
  isOneOfAdvancedSections: function () {
    var category = this.get('category');
    return category.indexOf("Advanced") != -1;
  },
  showAddPropertyWindow: function (event) {
    var serviceConfigNames = this.get('categoryConfigs').mapProperty('name');
    var serviceConfigObj = Ember.Object.create({
      name: '',
      value: '',
      defaultValue: null,
      filename: '',
      isUserProperty: true,
      isKeyError:false,
      errorMessage:"",
      observeAddPropertyValue:function(){
        var name = this.get('name');
        if(name.trim() != ""){
          if(validator.isValidConfigKey(name)){
            var configMappingProperty = App.config.configMapping.all().findProperty('name', name);
            if((configMappingProperty == null) && (!serviceConfigNames.contains(name))){
              this.set("isKeyError", false);
              this.set("errorMessage", "");
            } else {
              this.set("isKeyError", true);
              this.set("errorMessage", Em.I18n.t('services.service.config.addPropertyWindow.error.derivedKey'));
            }
          } else {
            this.set("isKeyError", true);
            this.set("errorMessage", Em.I18n.t('form.validator.configKey'));
          }
        } else {
          this.set("isKeyError", true);
          this.set("errorMessage", Em.I18n.t('services.service.config.addPropertyWindow.errorMessage'));
        }
      }.observes("name")
    });

    var category = this.get('category');
    serviceConfigObj.displayType = "advanced";
    serviceConfigObj.category = category.get('name');

    var serviceName = this.get('service.serviceName');
    var serviceConfigsMetaData = require('data/service_configs');
    var serviceConfigMetaData = serviceConfigsMetaData.findProperty('serviceName', serviceName);
    var categoryMetaData = serviceConfigMetaData == null ? null : serviceConfigMetaData.configCategories.findProperty('name', category.get('name'));
    if (categoryMetaData != null) {
      serviceConfigObj.filename = categoryMetaData.siteFileName;
    }
    
    var self = this;
    App.ModalPopup.show({
      // classNames: ['big-modal'],
      classNames: [ 'sixty-percent-width-modal'],
      header: "Add Property",
      primary: 'Add',
      secondary: 'Cancel',
      onPrimary: function () {
        serviceConfigObj.observeAddPropertyValue();
        /**
         * For the first entrance use this if (serviceConfigObj.name.trim() != "")
         */
        if (!serviceConfigObj.isKeyError) {
          serviceConfigObj.displayName = serviceConfigObj.name;
          serviceConfigObj.id = 'site property';
          serviceConfigObj.serviceName = serviceName;
          var serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigObj);
          self.get('serviceConfigs').pushObject(serviceConfigProperty);
          this.hide();
        }
      },
      onSecondary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/configs/addPropertyWindow'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        serviceConfigProperty: serviceConfigObj
      })
    });

  },
  
  /**
   * Removes the top-level property from list of properties.
   * Should be only called on user properties.
   */
  removeProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    this.get('serviceConfigs').removeObject(serviceConfigProperty);
  },
  
  /**
   * Restores given property's value to be its default value.
   * Does not update if there is no default value.
   */
  doRestoreDefaultValue: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var value = serviceConfigProperty.get('value');
    var dValue = serviceConfigProperty.get('defaultValue');
    if (dValue != null) {
      if(serviceConfigProperty.get('displayType') === 'password'){
        serviceConfigProperty.set('retypedPassword', dValue);
      }
      serviceConfigProperty.set('value', dValue);
    }
  },
  
  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var overrides = serviceConfigProperty.get('overrides');
    if (!overrides) {
      overrides = []; 
      serviceConfigProperty.set('overrides', overrides);
    }
    
    // create new override with new value
    var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty);
    newSCP.set('value', '');
    newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
    newSCP.set('parentSCP', serviceConfigProperty);
    newSCP.set('selectedHostOptions', Ember.A([]));
    console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty);
    overrides.pushObject(newSCP);
    
    // Launch override window
    var dummyEvent = {contexts: [newSCP]};
    this.showOverrideWindow(dummyEvent);
  },

  showOverrideWindow: function (event) {
    // argument 1
    var serviceConfigProperty = event.contexts[0];
    var parentServiceConfigProperty = serviceConfigProperty.get('parentSCP');
    var alreadyOverriddenHosts = [];
    parentServiceConfigProperty.get('overrides').forEach(function(override){
      if (override!=null && override!=serviceConfigProperty && override.get('selectedHostOptions')!=null){
        alreadyOverriddenHosts = alreadyOverriddenHosts.concat(override.get('selectedHostOptions'))
      }
    });
    var selectedHosts = serviceConfigProperty.get('selectedHostOptions');
    /**
     * Get all the hosts available for selection. Since data is dependent on
     * controller, we ask it, instead of doing regular Ember's App.Host.find().
     * This should be an array of App.Host.
     */
    var allHosts = this.get('controller.getAllHosts');
    var availableHosts = Ember.A([]);
    allHosts.forEach(function(host){
      var hostId = host.get('id');
      if(alreadyOverriddenHosts.indexOf(hostId)<0){
        availableHosts.pushObject(Ember.Object.create({
          selected: selectedHosts.indexOf(hostId)>-1,
          host: host
        }));
      }
    });
    /**
     * From the currently selected service we want the service-components.
     * We only need an array of objects which have the 'componentName' and
     * 'displayName' properties. Since each controller has its own objects,
     * we ask for a normalized array back.
     */
    var validComponents = this.get('controller.getCurrentServiceComponents');
    App.ModalPopup.show({
      classNames: [ 'sixty-percent-width-modal' ],
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      warningMessage: null,
      onPrimary: function () {
        console.debug('serviceConfigProperty.(old-selectedHosts)=' + serviceConfigProperty.get('selectedHosts'));
        var arrayOfSelectedHosts = [];
        var selectedHosts = availableHosts.filterProperty('selected', true);
        selectedHosts.forEach(function(host){
          arrayOfSelectedHosts.push(host.get('host.id'));
        });
        if(arrayOfSelectedHosts.length>0){
          this.set('warningMessage', null);
          serviceConfigProperty.set('selectedHostOptions', arrayOfSelectedHosts);
          serviceConfigProperty.validate();
          console.debug('serviceConfigProperty.(new-selectedHosts)=', arrayOfSelectedHosts);
          this.hide();
        }else{
          this.set('warningMessage', 'Atleast one host needs to be selected.');
        }
      },
      onSecondary: function () {
        // If property has no hosts already, then remove it from the parent.
        var hostCount = serviceConfigProperty.get('selectedHostOptions.length');
        if( hostCount < 1 ){
          var parentSCP = serviceConfigProperty.get('parentSCP');
          var overrides = parentSCP.get('overrides');
          overrides.removeObject(serviceConfigProperty);
        }
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/configs/overrideWindow'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        serviceConfigProperty: serviceConfigProperty,
        filterText: '',
        filterTextPlaceholder: Em.I18n.t('hosts.selectHostsDialog.filter.placeHolder'),
        availableHosts: availableHosts,
        filterColumn: Ember.Object.create({id:'ip', name:'IP Address', selected:false}),
        filterColumns: Ember.A([
           Ember.Object.create({id:'ip', name:'IP Address', selected:false}),
           Ember.Object.create({id:'cpu', name:'CPU', selected:false}),
           Ember.Object.create({id:'memory', name:'RAM', selected:false}),
           Ember.Object.create({id:'diskUsage', name:'Disk Usage', selected:false}),
           Ember.Object.create({id:'loadAvg', name:'Load Average', selected:false}),
           Ember.Object.create({id:'osArch', name:'OS Architecture', selected:false}),
           Ember.Object.create({id:'osType', name:'OS Type', selected:false})
        ]),
        showOnlySelectedHosts: false,
        filterComponents: validComponents,
        filterComponent: null,
        filteredHosts: function () {
          var hosts = this.get('availableHosts');
          var filterText = this.get('filterText');
          var showOnlySelectedHosts = this.get('showOnlySelectedHosts');
          var filteredHosts = Ember.A([]);
          var self = this;
          hosts.forEach(function (host) {
            var skip = false;
            var ahost = host.get('host');
            var filterColumn = self.get('filterColumn');
            if (filterColumn == null) {
              filterColumn = self.get('filterColumns').objectAt(0);
            }
            var value = ahost.get(filterColumn.id);
            host.set('filterColumnValue', value);
            if (filterText != null && filterText.length > 0) {
              if ((value==null || !value.match(filterText)) && !host.get('host.publicHostName').match(filterText)) {
                skip = true;
              }
            }
            var filterComponent = self.get('filterComponent');
            if (!skip && filterComponent != null) {
              var componentFound = false;
              var fcn = filterComponent.get('componentName');
              var hcs = ahost.get('hostComponents');
              if (hcs != null) {
                hcs.forEach(function (hc) {
                  if (fcn === hc.get('componentName')) {
                    componentFound = true;
                  }
                });
              }
              if (!componentFound) {
                skip = true;
              }
            }
            if (!skip && showOnlySelectedHosts && !host.get('selected')){
              skip = true;
            }
            if (!skip) {
              filteredHosts.pushObject(host);
            }
          });
          return filteredHosts;
        }.property('availableHosts', 'filterText', 'filterColumn', 'filterComponent', 'filterComponent.componentName', 'showOnlySelectedHosts'),
        hostColumnValue: function(host, column){
          return host.get(column.id);
        },
        hostSelectMessage: function () {
          var hosts = this.get('availableHosts');
          var selectedHosts = hosts.filterProperty('selected', true);
          return this.t('hosts.selectHostsDialog.selectedHostsLink').format(selectedHosts.get('length'), hosts.get('length'))
        }.property('availableHosts.@each.selected'), 
        selectFilterColumn: function(event){
          if(event!=null && event.context!=null && event.context.id!=null){
            var filterColumn = this.get('filterColumn');
            if(filterColumn!=null){
              filterColumn.set('selected', false);
            }
            event.context.set('selected', true);
            this.set('filterColumn', event.context);
          }
        },
        selectFilterComponent: function(event){
          if(event!=null && event.context!=null && event.context.componentName!=null){
            var currentFilter = this.get('filterComponent');
            if(currentFilter!=null){
              currentFilter.set('selected', false);
            }
            if(currentFilter!=null && currentFilter.componentName===event.context.componentName){
              // selecting the same filter deselects it.
              this.set('filterComponent', null);
            }else{
              this.set('filterComponent', event.context);
              event.context.set('selected', true);
            }
          }
        },
        allHostsSelected: false,
        toggleSelectAllHosts: function(event){
          if(this.get('allHostsSelected')){
            // Select all hosts
            this.get('availableHosts').setEach('selected', true);
          }else{
            // Deselect all hosts
            this.get('availableHosts').setEach('selected', false);
          }
        }.observes('allHostsSelected'),
        toggleShowSelectedHosts: function () {
          var currentFilter = this.get('filterComponent');
          if (currentFilter != null) {
            currentFilter.set('selected', false);
          }
          this.set('filterComponent', null);
          this.set('filterText', null);
          this.set('showOnlySelectedHosts', !this.get('showOnlySelectedHosts'));
        }
      })
    });
  }
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
  customConfigs: require('data/custom_configs'),
  /**
   * configs filtered by capacity-scheduler category
   */
  categoryConfigs: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'));
  }.property('queueObserver', 'serviceConfigs.@each'),
  /**
   * rewrote method to avoid incompatibility with parent
   */
  filteredCategoryConfigs: function(){
    return this.get('categoryConfigs');
  }.property(),
  advancedConfigs: function(){
    return this.get('categoryConfigs').filterProperty('isQueue', undefined) || [];
  }.property('categoryConfigs.@each'),
  didInsertElement: function(){
    this._super();
    this.createEmptyQueue(this.get('customConfigs').filterProperty('isQueue'));
  },
  //list of fields which will be populated by default in a new queue
  fieldsToPopulate: [
    "mapred.capacity-scheduler.queue.<queue-name>.minimum-user-limit-percent",
    "mapred.capacity-scheduler.queue.<queue-name>.user-limit-factor",
    "mapred.capacity-scheduler.queue.<queue-name>.supports-priority",
    "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks",
    "mapred.capacity-scheduler.queue.<queue-name>.maximum-initialized-active-tasks-per-user",
    "mapred.capacity-scheduler.queue.<queue-name>.init-accept-jobs-factor"
  ],
  /**
   * create empty queue
   * take some queue then copy it and set all config values to null
   * @param customConfigs
   */
  createEmptyQueue: function(customConfigs){
    var emptyQueue = {
      name: '<queue-name>',
      configs: []
    };
    var fieldsToPopulate = this.get('fieldsToPopulate');
    customConfigs.forEach(function(config){
      var newConfig = $.extend({}, config);
      if(fieldsToPopulate.contains(config.name)){
        App.config.setDefaultQueue(newConfig, emptyQueue.name);
      }
      newConfig = App.ServiceConfigProperty.create(newConfig);
      newConfig.validate();
      emptyQueue.configs.push(newConfig);
    });
    this.set('emptyQueue', emptyQueue);
  },
  queues: function(){
    var configs = this.get('categoryConfigs').filterProperty('isQueue', true);
    var queueNames = [];
    var queues = [];
    configs.mapProperty('name').forEach(function(name){
      var queueName = /^mapred\.capacity-scheduler\.queue\.(.*?)\./.exec(name);
      if(queueName){
        queueNames.push(queueName[1]);
      }
    });
    queueNames = queueNames.uniq();
    queueNames.forEach(function(queueName){
      queues.push({
        name: queueName,
        color: this.generateColor(queueName),
        configs: this.filterConfigsByQueue(queueName, configs)
      })
    }, this);
    return queues;
  }.property('queueObserver'),
  /**
   * filter configs by queue
   * @param queueName
   * @param configs
   */
  filterConfigsByQueue: function (queueName, configs) {
    var customConfigs = this.get('customConfigs');
    var queue = [];
    configs.forEach(function (config) {
      var customConfig = customConfigs.findProperty('name', config.name.replace('queue.' + queueName, 'queue.<queue-name>'));
      if ((config.name.indexOf('mapred.capacity-scheduler.queue.' + queueName) !== -1) ||
        (config.name.indexOf('mapred.queue.' + queueName) !== -1)) {
        if (customConfig) {
          config.set('description', customConfig.description);
          config.set('displayName', customConfig.displayName);
          config.set('isRequired', customConfig.isRequired);
          config.set('unit', customConfig.unit);
          config.set('displayType', customConfig.displayType);
          config.set('valueRange', customConfig.valueRange);
          config.set('isVisible', customConfig.isVisible);
          config.set('index', customConfig.index);
        }
        queue.push(config);
      }
    });
    //each queue consists of 10 properties if less then add missing properties
    if(queue.length < 10){
      this.addMissingProperties(queue, queueName);
    }
    return queue;
  },
  /**
   * add missing properties to queue
   * @param queue
   * @param queueName
   */
  addMissingProperties: function(queue, queueName){
    var customConfigs = this.get('customConfigs');
    customConfigs.forEach(function(_config){
      var serviceConfigProperty = $.extend({}, _config);
      serviceConfigProperty.name = serviceConfigProperty.name.replace(/<queue-name>/, queueName);
      if(!queue.someProperty('name', serviceConfigProperty.name)){
        App.config.setDefaultQueue(serviceConfigProperty, queueName);
        serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigProperty);
        serviceConfigProperty.validate();
        queue.push(serviceConfigProperty);
      }
    }, this);
  },
  /**
   * format table content from queues
   */
  tableContent: function(){
    var result = [];
    this.get('queues').forEach(function(queue){
      var usersAndGroups = queue.configs.findProperty('name', 'mapred.queue.' + queue.name + '.acl-submit-job').get('value');
      usersAndGroups = (usersAndGroups) ? usersAndGroups.split(' ') : [''];
      if(usersAndGroups.length == 1){
        usersAndGroups.push('');
      }
      var queueObject = {
        name: queue.name,
        color: 'background-color:' + queue.color + ';',
        users: usersAndGroups[0],
        groups: usersAndGroups[1],
        capacity: queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.capacity').get('value'),
        maxCapacity: queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.maximum-capacity').get('value'),
        minUserLimit: queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.minimum-user-limit-percent').get('value'),
        userLimitFactor: queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.user-limit-factor').get('value'),
        supportsPriority: queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.supports-priority').get('value')
      };
      result.push(queueObject);
    }, this);
    return result;
  }.property('queues'),
  queueObserver: null,
  /**
   * uses as template for adding new queue
   */
  emptyQueue: [],
  generateColor: function(str){
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
  addQueue: function(queue){
    var serviceConfigs = this.get('serviceConfigs');
    var admin = [];
    var submit = [];
    var submitConfig;
    var adminConfig;
    queue.name = queue.configs.findProperty('name', 'queueName').get('value');
    queue.configs.forEach(function(config){
      if(config.name == 'mapred.queue.<queue-name>.acl-administer-jobs'){
        if(config.type == 'USERS'){
          admin[0] = config.value;
        }
        if(config.type == 'GROUPS'){
          admin[1] = config.value;
        }
        if(config.isQueue){
          adminConfig = config;
        }
      }
      if(config.name == 'mapred.queue.<queue-name>.acl-submit-job'){
        if(config.type == 'USERS'){
          submit[0] = config.value;
        }
        if(config.type == 'GROUPS'){
          submit[1] = config.value;
        }
        if(config.isQueue){
          submitConfig = config;
        }
      }
      config.set('name', config.get('name').replace('<queue-name>', queue.name));
      config.set('value', config.get('value').toString());
      if(config.isQueue){
        serviceConfigs.push(config);
      }
    });
    adminConfig.set('value', admin.join(' '));
    submitConfig.set('value', submit.join(' '));
    this.set('queueObserver', new Date().getTime());
  },
  /**
   * delete queue
   * delete configs from serviceConfigs which have current queue name
   * @param queue
   */
  deleteQueue: function(queue){
    var serviceConfigs = this.get('serviceConfigs');
    var configNames = queue.configs.filterProperty('isQueue').mapProperty('name');
    for(var i = 0, l = serviceConfigs.length; i < l; i++){
      if(configNames.contains(serviceConfigs[i].name)){
        serviceConfigs.splice(i, 1);
        l--;
        i--;
      }
    }
    this.set('queueObserver', new Date().getTime());
  },
  /**
   * save changes that was made to queue
   * edit configs from serviceConfigs which have current queue name
   * @param queue
   */
  editQueue: function(queue){
    var serviceConfigs = this.get('serviceConfigs');
    var configNames = queue.configs.filterProperty('isQueue').mapProperty('name');
    serviceConfigs.forEach(function(_config){
      var configName = _config.get('name');
      var admin = [];
      var submit = [];
      if(configNames.contains(_config.get('name'))){
        if(configName == 'mapred.queue.' + queue.name + '.acl-submit-job'){
          submit = queue.configs.filterProperty('name', configName);
          submit = submit.findProperty('type', 'USERS').get('value') + ' ' + submit.findProperty('type', 'GROUPS').get('value');
          _config.set('value', submit);
        } else if(configName == 'mapred.queue.' + queue.name + '.acl-administer-jobs'){
          admin = queue.configs.filterProperty('name', configName);
          admin = admin.findProperty('type', 'USERS').get('value') + ' ' + admin.findProperty('type', 'GROUPS').get('value');
          _config.set('value', admin);
        } else {
          _config.set('value', queue.configs.findProperty('name', _config.get('name')).get('value').toString());
        }
        //comparison executes including 'queue.<queue-name>' to avoid false matches
        _config.set('name', configName.replace('queue.' + queue.name, 'queue.' + queue.configs.findProperty('name', 'queueName').get('value')));
      }
    });
    this.set('queueObserver', new Date().getTime());
  },
  pieChart: App.ChartPieView.extend({
    w: 200,
    h: 200,
    queues: null,
    didInsertElement: function(){
      this.update();
    },
    data: [{"label":"default", "value":100}],
    update: function(){
      var self = this;
      var data = [];
      var queues = this.get('queues');
      var capacitiesSum = 0;
      queues.forEach(function(queue){
        data.push({
          label: queue.name,
          value: parseInt(queue.configs.findProperty('name', 'mapred.capacity-scheduler.queue.' + queue.name + '.capacity').get('value')),
          color: queue.color
        })
      });

      data.mapProperty('value').forEach(function(capacity){
        capacitiesSum += capacity;
      });
      if(capacitiesSum < 100){
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
        .on("click", function(d,i) {
          var event = {context: d.data.label};
          if (d.data.isEmpty !== true) self.get('parentView').queuePopup(event);
        }).on('mouseover', function(d, i){
          var position = d3.svg.mouse(this);
          var label = $('#section_label');
          label.css('left', position[0] + 100);
          label.css('top', position[1] + 100);
          label.text(d.data.label);
          label.show();
        })
        .on('mouseout', function(d, i){
          $('#section_label').hide();
        })

    }.observes('queues'),
    donut:d3.layout.pie().sort(null).value(function(d) {return d.value;})
  }),
  /**
   * open popup with chosen queue
   * @param event
   */
  queuePopup: function(event){
    //if queueName was handed that means "Edit" mode, otherwise "Add" mode
    var queueName = event.context || null;
    var self = this;
    App.ModalPopup.show({
      didInsertElement: function(){
        if(queueName){
          this.set('header', Em.I18n.t('services.mapReduce.config.editQueue'));
          this.set('secondary', Em.I18n.t('common.save'));
          if(self.get('queues').length > 1 && self.get('canEdit')){
            this.set('delete', Em.I18n.t('common.delete'));
          }
        }
      },
      header: Em.I18n.t('services.mapReduce.config.addQueue'),
      secondary: Em.I18n.t('common.add'),
      primary: Em.I18n.t('common.cancel'),
      delete: null,
      isError: function(){
        if(!self.get('canEdit')){
          return true;
        }
        var content = this.get('content');
        var configs = content.configs.filter(function(config){
          if((config.name == 'mapred.queue.' + content.name + '.acl-submit-job' ||
             config.name == 'mapred.queue.' + content.name + '.acl-administer-jobs') &&
             (config.isQueue)){
            return false;
          }
          return true;
        });
        return configs.someProperty('isValid', false);
      }.property('content.configs.@each.isValid'),
      onDelete: function(){
        var view = this;
        App.ModalPopup.show({
          header: Em.I18n.t('popup.confirmation.commonHeader'),
          body: Em.I18n.t('hosts.delete.popup.body'),
          primary: Em.I18n.t('yes'),
          onPrimary: function(){
            self.deleteQueue(view.get('content'));
            view.hide();
            this.hide();
          }
        });
      },
      onSecondary: function() {
        if(queueName){
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
      content: function(){
        var content = (queueName) ? self.get('queues').findProperty('name', queueName) : self.get('emptyQueue');
        var configs = [];
        var tableContent = self.get('tableContent');
        // copy of queue configs
        content.configs.forEach(function (config, index) {
          if (config.name == 'mapred.capacity-scheduler.queue.' + content.name + '.capacity') {
            config.reopen({
              validate: function () {
                var value = this.get('value');
                var isError = false;
                var capacities = [];
                var capacitySum = 0;
                if(tableContent){
                  capacities = tableContent.mapProperty('capacity');
                  for (var i = 0, l = capacities.length; i < l; i++) {
                    capacitySum += parseInt(capacities[i]);
                  }
                  if (content.name != '<queue-name>') {
                    capacitySum = capacitySum - parseInt(tableContent.findProperty('name', content.name).capacity);
                  }
                }
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
          if(config.name == 'mapred.capacity-scheduler.queue.' + content.name + '.supports-priority'){
            if(config.get('value') == 'true' || config.get('value') === true){
              config.set('value', true);
            } else {
              config.set('value', false);
            }
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
        template: Ember.Handlebars.compile([
          '{{#if view.parentView.delete}}<div class="pull-left">',
          '<button class="btn btn-danger" {{action onDelete target="view.parentView"}}>',
          '{{view.parentView.delete}}</button></div>{{/if}}',
          '<p class="pull-right">',
          '{{#if view.parentView.primary}}<button type="button" class="btn" {{action onPrimary target="view.parentView"}}>',
          '{{view.parentView.primary}}</button>{{/if}}',
          '{{#if view.parentView.secondary}}',
          '<button type="button" {{bindAttr disabled="view.parentView.isError"}} class="btn btn-success" {{action onSecondary target="view.parentView"}}>',
          '{{view.parentView.secondary}}</button>{{/if}}',
          '</p>'
        ].join(''))
      }),
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile([
          '<form class="form-horizontal pre-scrollable">{{#each view.parentView.content.configs}}',
          '{{#if isVisible}}',
          '<div class="row-fluid control-group">',
          '   <div {{bindAttr class="errorMessage:error :control-label-span :span4"}}>',
          '     <label>{{displayName}}</label>',
          '   </div>',
          '   <div {{bindAttr class="errorMessage:error :control-group :span8"}}>',
          '     {{view viewClass serviceConfigBinding="this" categoryConfigsBinding="view.categoryConfigs" }}',
          '     <span class="help-inline">{{errorMessage}}</span>',
          '   </div>',
          '</div>',
          '{{/if}}',
          '{{/each}}</form>'
        ].join(''))
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
      insertExtraConfigs: function(content){
        var that = this;
        var admin = content.configs.findProperty('name', 'mapred.queue.' + content.name + '.acl-administer-jobs').get('value');
        var submit = content.configs.findProperty('name', 'mapred.queue.' + content.name + '.acl-submit-job').get('value');
        admin = (admin) ? admin.split(' ') : [''];
        submit = (submit) ? submit.split(' ') : [''];
        if(admin.length < 2){
          admin.push('');
        }
        if(submit.length < 2){
          submit.push('');
        }
        var newField = App.ServiceConfigProperty.create({
          name: 'queueName',
          displayName: Em.I18n.t('services.mapReduce.extraConfig.queue.name'),
          description: Em.I18n.t('services.mapReduce.description.queue.name'),
          value: (content.name == '<queue-name>') ? '' : content.name,
          validate: function(){
            var queueNames = self.get('queues').mapProperty('name');
            var value = this.get('value');
            var isError = false;
            var regExp = /^[a-z]([\_\-a-z0-9]{0,50})\$?$/i;
            if(value == ''){
              if (this.get('isRequired')) {
                this.set('errorMessage', 'This is required');
                isError = true;
              } else {
                return;
              }
            }
            if(!isError){
              if((queueNames.indexOf(value) !== -1) && (value != content.name)){
                this.set('errorMessage', 'Queue name is already used');
                isError = true;
              }
            }
            if(!isError){
              if(!regExp.test(value)){
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
        newField.validate();
        content.configs.unshift(newField);

        var submitUser = App.ServiceConfigProperty.create({
          name: 'mapred.queue.' + content.name + '.acl-submit-job',
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
          name: 'mapred.queue.' + content.name + '.acl-submit-job',
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
          name: 'mapred.queue.' + content.name + '.acl-administer-jobs',
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
          name: 'mapred.queue.' + content.name + '.acl-administer-jobs',
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
          validate: function(){
            that.userGroupValidation(this, submitGroup);
          }.observes('value')
        });
        submitGroup.reopen({
          validate: function(){
            that.userGroupValidation(this, submitUser);
          }.observes('value')
        });
        adminUser.reopen({
          validate: function(){
            that.userGroupValidation(this, adminGroup);
          }.observes('value')
        });
        adminGroup.reopen({
          validate: function(){
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
       * Users can be blank. If this is blank, Groups must not be blank.
       * Groups can be blank. If this is blank, Users must not be blank.
       * @param context
       * @param boundConfig
       */
      userGroupValidation:  function(context, boundConfig){
        if(context.get('value') == ''){
          if(boundConfig.get('value') == ''){
            context._super();
          } else {
            boundConfig.validate();
          }
        } else {
          if(boundConfig.get('value') == ''){
            boundConfig.set('errorMessage', '');
          }
          context._super();
        }
      }
    })
  }
});
