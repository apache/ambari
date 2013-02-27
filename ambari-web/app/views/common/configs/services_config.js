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

App.ServicesConfigView = Em.View.extend({
  templateName: require('templates/common/configs/services_config'),
  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
  }
});

App.ServiceConfigView = Em.View.extend({
  templateName: require('templates/common/configs/service_config'),
  onToggleBlock: function (event) {
    $(document.getElementById(event.context.name)).toggle('blind', 500);
    event.context.set('isCollapsed', !event.context.get('isCollapsed'));
  }
});


App.ServiceConfigsByCategoryView = Ember.View.extend({

  content: null,

  category: null,
  serviceConfigs: null, // General, Advanced, NameNode, SNameNode, DataNode, etc.
  // total number of
  // hosts (by
  // default,
  // cacheable )
  categoryConfigs: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'))
  }.property('serviceConfigs.@each'),
  didInsertElement: function () {
    var elementId = "#" + this.get('category.name');
    if (this.get('category.name').indexOf('Advanced') != -1) {
      this.set('category.isCollapsed', true);
      $(elementId).hide();
    } else {
      this.set('category.isCollapsed', false);
    }
  },
  childView: App.ServiceConfigsOverridesView,
  changeFlag: Ember.Object.create({
    val: 1
  }),
  invokeMe: function () {
    alert("parent");
  },
  isOneOfAdvancedSections: function () {
    var category = this.get('category');
    return category.indexOf("Advanced") != -1;
  },
  showAddPropertyWindow: function (event) {

    var serviceConfigObj = {
      name: '',
      value: '',
      defaultValue: '',
      filename: ''
    };

    var category = this.get('category');
    serviceConfigObj.displayType = "advanced";
    serviceConfigObj.category = category.get('name');

    var fileName = null;

    if (category.get('name') === 'AdvancedCoreSite') {
      serviceConfigObj.filename = "core-site.xml";
    } else if (category.get('name') === 'AdvancedHDFSSite') {
      serviceConfigObj.filename = "hdfs-site.xml";
    } else if (category.get('name') === 'AdvancedMapredSite') {
      serviceConfigObj.filename = "mapred-site.xml";
    }

    var self = this;
    App.ModalPopup.show({
      // classNames: ['big-modal'],
      classNames: [ 'sixty-percent-width-modal', 'arun' ],
      header: "Add Property",
      primary: 'Save',
      secondary: 'Cancel',
      onPrimary: function () {
        serviceConfigObj.displayName = serviceConfigObj.name;
        var serviceConfigProperty = App.ServiceConfigProperty.create(serviceConfigObj);
        self.get('serviceConfigs').pushObject(serviceConfigProperty);
        this.hide();
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
  
  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var self = this;
    var arrayOfSelectedHosts = [];
    var newValue = '';
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
    console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty)
    overrides.pushObject(newSCP);
  },
  
  _createDummyHosts: function(){
    var array = Ember.A([]);
    for ( var int = 0; int < 50; int++) {
      array.pushObject(App.Host.createRecord({
        hostName: 'internal-hostname-'+int+'.acme.com',
        publicHostName: 'public-host-'+int+'.acme.com',
        cpu: int,
        memory: int*1024,
        ip: int+"."+int*10+"."+int*20+"."+int*30,
        rack: 'whatrack'+(int%10),
        
      }));
    }
    return array;
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
    //TODO - remove below section
    if(allHosts.get('length')<5){
      this._createDummyHosts();
      allHosts = App.Host.find();
    }
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
    var self = this;
    App.ModalPopup.show({
      classNames: [ 'sixty-percent-width-modal' ],
      header: "Select Hosts",
      primary: 'OK',
      secondary: 'Cancel',
      onPrimary: function () {
        console.debug('serviceConfigProperty.(value)=' + serviceConfigProperty.get('value'));
        console.debug('serviceConfigProperty.(selectedHosts)=' + serviceConfigProperty.get('selectedHosts'));
        var arrayOfSelectedHosts = [];
        var selectedHosts = availableHosts.filterProperty('selected', true);
        selectedHosts.forEach(function(host){
          arrayOfSelectedHosts.push(host.get('host.id'));
        });
        serviceConfigProperty.set('selectedHostOptions', arrayOfSelectedHosts);
        serviceConfigProperty.validate();
        console.debug('Selected hosts:', arrayOfSelectedHosts);
        this.hide();
      },
      onSecondary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/configs/overrideWindow'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        message: "Host level Overrides show here",
        serviceConfigProperty: serviceConfigProperty,
        filterText: '',
        availableHosts: availableHosts,
        filterColumn: null,
        filterColumns: Ember.A([
           Ember.Object.create({id:'ip', name:'IP Address', selected:false}),
           Ember.Object.create({id:'cpu', name:'CPU', selected:false}),
           Ember.Object.create({id:'memory', name:'RAM', selected:false}),
           Ember.Object.create({id:'diskUsage', name:'Disk Usage', selected:false}),
           Ember.Object.create({id:'loadAvg', name:'Load Average', selected:false}),
           Ember.Object.create({id:'osArch', name:'OS Architecture', selected:false}),
           Ember.Object.create({id:'osType', name:'OS Type', selected:false})
        ]),
        filterComponents: validComponents,
        filterComponent: null,
        filteredHosts: function () {
          var hosts = this.get('availableHosts');
          var filterText = this.get('filterText');
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
            if (!skip) {
              filteredHosts.pushObject(host);
            }
          });
          return filteredHosts;
        }.property('availableHosts', 'filterText', 'filterColumn', 'filterComponent', 'filterComponent.componentName'),
        hostColumnValue: function(host, column){
          return host.get(column.id);
        },
        hostSelectMessage: function () {
          var hosts = this.get('availableHosts');
          var selectedHosts = hosts.filterProperty('selected', true);
          return selectedHosts.get('length') + " out of " + hosts.get('length') + " hosts selected";
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
        clearFilters: function () {
          var currentFilter = this.get('filterComponent');
          if (currentFilter != null) {
            currentFilter.set('selected', false);
          }
          this.set('filterComponent', null);
          this.set('filterText', null);
        }
      })
    });
  },
  layout: Ember.Handlebars.compile('<div {{bindAttr id="view.category.name"}} class="accordion-body collapse in"><div class="accordion-inner">{{yield}}</div></div>')
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
