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

App.ConfigHistoryFlowView = Em.View.extend({
  templateName: require('templates/common/configs/config_history_flow'),

  /**
   * index of the first element(service version box) in viewport
   */
  startIndex: 0,
  showLeftArrow: false,
  showRightArrow: false,
  leftArrowTooltip: Em.computed.ifThenElse('showLeftArrow', Em.I18n.t('services.service.config.configHistory.leftArrow.tooltip'), null),
  rightArrowTooltip: Em.computed.ifThenElse('showRightArrow', Em.I18n.t('services.service.config.configHistory.rightArrow.tooltip'), null),
  VERSIONS_IN_FLOW: 6,
  VERSIONS_IN_DROPDOWN: 25,
  /**
   * flag identify whether to show all versions or short list of them
   */
  showFullList: false,
  compareServiceVersion: null,

  /**
   * types of actions that can't be done to service config versions
   */
  actionTypes: {
    SWITCH: 'switchVersion',
    COMPARE: 'compare',
    REVERT: 'revert'
  },

  /**
   * serviceVersion object that is currently being hovered in the dropdown menu
   */
  hoveredServiceVersion: null,
  /**
   * flag to check if sub-menu popup is currently being hovered
   */
  displaySubMenuFlag: false,
  /**
   * flag to check if any dropdown item is currently hovered by the user
   */
  isHovered: false,

  /**
   * In reason of absence of properties dynamic values support which passed to an action,
   * used property map to get latest values of properties for action
   */
  serviceVersionsReferences: {
    displayed: Em.Object.create({
      isReference: true,
      property: 'displayedServiceVersion'
    }),
    compare: Em.Object.create({
      isReference: true,
      property: 'compareServiceVersion'
    })
  },

  allServiceVersions: function() {
    return App.ServiceConfigVersion.find().filterProperty('serviceName', this.get('serviceName'));
  }.property('serviceName'),

  showCompareVersionBar: Em.computed.bool('compareServiceVersion'),

  isSaveDisabled: Em.computed.or('controller.isSubmitDisabled', '!controller.versionLoaded', '!controller.isPropertiesChanged'),

  serviceName: Em.computed.alias('controller.content.serviceName'),

  displayedServiceVersion: Em.computed.findBy('serviceVersions', 'isDisplayed', true),
  /**
   * identify whether to show link that open whole content of notes
   */
  showMoreLink: Em.computed.gt('displayedServiceVersion.notes.length', 100),
  /**
   * formatted notes ready to display
   */
  shortNotes: Em.computed.truncate('displayedServiceVersion.notes', 100, 100),

  serviceVersions: function () {
    var isDefaultGroupSelected = this.get('controller.selectedConfigGroup.isDefault');
    var groupId = this.get('controller.selectedConfigGroup.id');
    var self = this;

    this.get('allServiceVersions').forEach(function (version) {
      version.set('isDisabled', !(version.get('groupId') === groupId || isDefaultGroupSelected && version.get('groupName') === App.ServiceConfigGroup.defaultGroupName));
    }, this);

    var serviceVersions = this.get('allServiceVersions').filter(function(s) {
      return s.get('groupId') === groupId || s.get('groupName') === App.ServiceConfigGroup.defaultGroupName;
    });

    if (!serviceVersions.findProperty('isDisplayed')) {
      //recompute serviceVersions if displayed version absent
      Em.run.next(function() {
        self.propertyDidChange('controller.selectedConfigGroup.name');
      });
    }

    return serviceVersions.sort(function (a, b) {
      return Em.get(b, 'createTime') - Em.get(a, 'createTime');
    });
  }.property('serviceName', 'controller.selectedConfigGroup.name'),

  /**
   * disable versions visible to the user to prevent actions on them
   */
  disableVersions: function () {
    this.get('allServiceVersions').setEach('isDisabled', true);
  },

  /**
   * service versions which in viewport and visible to user
   */
  visibleServiceVersion: function () {
    return this.get('serviceVersions').slice(this.get('startIndex'), this.get('startIndex') + this.VERSIONS_IN_FLOW);
  }.property('startIndex', 'serviceVersions'),

  /**
   * enable actions to manipulate version only after it's loaded
   */
  versionActionsDisabled: Em.computed.or('!controller.versionLoaded', '!dropDownList.length'),

  /**
   * enable discard to manipulate version only after it's loaded and any property is changed
   */
  isDiscardDisabled: Em.computed.or('!controller.versionLoaded', '!controller.isPropertiesChanged'),
  /**
   * list of service versions
   * by default 6 is number of items in short list
   */
  dropDownList: function () {
    var serviceVersions = this.get('serviceVersions').slice(0);
    if (this.get('showFullList')) {
      return serviceVersions;
    }
    return serviceVersions.slice(0, this.VERSIONS_IN_DROPDOWN);
  }.property('serviceVersions', 'showFullList', 'displayedServiceVersion'),

  openFullList: function (event) {
    event.stopPropagation();
    this.set('showFullList', true);
  },

  hideFullList: function (event) {
    this.set('showFullList', !(this.get('serviceVersions.length') > this.VERSIONS_IN_DROPDOWN));
  },

  didInsertElement: function () {
    App.tooltip(this.$('[data-toggle=tooltip]'),{
      placement: 'bottom',
      html: false
    });
    App.tooltip(this.$('[data-toggle=arrow-tooltip]'),{
      placement: 'top'
    });
    this.$(".version-info-bar-wrapper").stick_in_parent({parent: '#serviceConfig', offset_top: 10});
    this.onChangeConfigGroup();
  },

  willDestroyElement: function() {
    this.$('.version-info-bar-wrapper').trigger('sticky_kit:detach').off();
    this.$('[data-toggle=tooltip]').tooltip('destroy');
    this.$('[data-toggle=arrow-tooltip]').tooltip('destroy');
  },

  willInsertElement: function () {
    this.setDisplayVersion();
  },

  setDisplayVersion: function () {
    var serviceVersions = this.get('serviceVersions');
    var startIndex = 0;
    var currentIndex = 0;
    var selectedVersion = this.get('controller.selectedVersion');

    serviceVersions.setEach('isDisplayed', false);

    serviceVersions.forEach(function (serviceVersion, index) {
      if (selectedVersion === serviceVersion.get('version')) {
        serviceVersion.set('isDisplayed', true);
        currentIndex = index;
      }
    }, this);

    // show current version as the last one
    if (currentIndex + 1 > this.VERSIONS_IN_FLOW) {
      startIndex = currentIndex + 1 - this.VERSIONS_IN_FLOW;
    }
    this.set('startIndex', startIndex);
    this.adjustFlowView();
  }.observes('allVersionsLoaded'),

  onChangeConfigGroup: function () {
    var serviceVersions = this.get('serviceVersions');
    var selectedGroupName = this.get('controller.selectedConfigGroup.name');
    var preselectedVersion = this.get('controller.selectedVersion');
    var startIndex = 0;
    var currentIndex = 0;
    var isCurrentInDefaultGroupIndex = null;


    serviceVersions.setEach('isDisplayed', false);
    // display selected version from config history
    serviceVersions.forEach(function (serviceVersion, index) {
      // find selected version in group
      if (serviceVersion.get('version') === preselectedVersion && serviceVersion.get('groupName') === selectedGroupName) {
        serviceVersion.set('isDisplayed', true);
        currentIndex = index + 1;
      }
    });
    // display current in selected group
    if (!currentIndex) {
      serviceVersions.forEach(function (serviceVersion, index) {
        // find current in selected group
        if (serviceVersion.get('isCurrent') && serviceVersion.get('groupName') === selectedGroupName) {
          serviceVersion.set('isDisplayed', true);
          currentIndex = index + 1;
        }
        if (serviceVersion.get('isCurrent') && serviceVersion.get('groupName') === App.ServiceConfigGroup.defaultGroupName) {
          isCurrentInDefaultGroupIndex = index;
        }
      });
      // if there is no current version in selected group show current version from default group
      if (!currentIndex && serviceVersions.length) {
        serviceVersions[isCurrentInDefaultGroupIndex].set('isDisplayed', true);
        currentIndex = isCurrentInDefaultGroupIndex + 1;
      }
    }
    // show current version as the last one
    if (currentIndex > this.VERSIONS_IN_FLOW) {
      startIndex = currentIndex - this.VERSIONS_IN_FLOW;
    }
    this.set('startIndex', startIndex);
    this.adjustFlowView();
  }.observes('controller.selectedConfigGroup'),

  /**
   *  define the first element in viewport
   *  change visibility of arrows
   */
  adjustFlowView: function () {
    var startIndex = this.get('startIndex');
    this.get('serviceVersions').forEach(function (serviceVersion, index) {
      serviceVersion.set('first', index === startIndex);
    });
    this.set('showLeftArrow', startIndex !== 0);
    this.set('showRightArrow', (this.get('serviceVersions.length') > this.VERSIONS_IN_FLOW) && ((startIndex + this.VERSIONS_IN_FLOW) < this.get('serviceVersions.length')));
  },

  /**
   * check action constraints prior to invoke it
   * @param event
   */
  doAction: function (event) {
    var type = event.contexts[1],
        controller = this.get('controller'),
        self = this;
    if (!controller.get('versionLoaded')) {
      return;
    }
    // action from right popup of pull down version list will have context[0] == undefined, and use 'hoveredServiceVersion'.
    // refer to AMBARI-19871 for more info
    var configVersion = event.contexts[0] || this.get('hoveredServiceVersion');
    if (type === 'switchVersion') {
      if (configVersion && configVersion.get("isDisplayed"))  return;
    } else {
      var isDisabled = configVersion ? configVersion.get('isDisabled') : false;
      if (isDisabled) return;
    }

    function callback() {
      self[type].call(self, event);
    }

    Em.run.next(function() {
      if (controller.hasUnsavedChanges()) {
        controller.showSavePopup(null, callback);
        return;
      }

      self.disableVersions();
      callback();
    });
    $("#config_version_popup").removeAttr('style');
  },

  /**
   * switch configs view version to chosen
   */
  switchVersion: function (event) {
    var configVersion = event.contexts[0] || this.get('hoveredServiceVersion');
    var version = configVersion.get('version');
    var versionIndex = 0;
    this.set('compareServiceVersion', null);
    this.get('serviceVersions').forEach(function (serviceVersion, index) {
      if (serviceVersion.get('version') === version) {
        serviceVersion.set('isDisplayed', true);
        versionIndex = index;
      } else {
        serviceVersion.set('isDisplayed', false);
      }
    });
    this.shiftFlowOnSwitch(versionIndex);
    this.get('controller').loadSelectedVersion(version);
  },

  /**
   * add config values of chosen version to view for comparison
   * add a second version-info-bar for the chosen version
   */
  compare: function (event) {
    var serviceConfigVersion = event.contexts[0] || this.get('hoveredServiceVersion');
    this.set('controller.compareServiceVersion', serviceConfigVersion);
    this.set('compareServiceVersion', serviceConfigVersion);

    var controller = this.get('controller');
    controller.get('stepConfigs').clear();
    controller.loadCompareVersionConfigs(controller.get('allConfigs')).done(function() {
      controller.onLoadOverrides(controller.get('allConfigs'));
    });
  },
  removeCompareVersionBar: function () {
    var displayedVersion = this.get('displayedServiceVersion.version');
    var versionIndex = 0;

    this.set('compareServiceVersion', null);
    this.get('serviceVersions').forEach(function (serviceVersion, index) {
      if (serviceVersion.get('version') === displayedVersion) {
        serviceVersion.set('isDisplayed', true);
        versionIndex = index;
      } else {
        serviceVersion.set('isDisplayed', false);
      }
    });
    this.set('isCompareMode', false);
    this.shiftFlowOnSwitch(versionIndex);
    this.get('controller').loadSelectedVersion(displayedVersion);
  },
  clearCompareVersionBar: function () {
    if (this.get('controller.isCompareMode') === false) {
      this.set('compareServiceVersion', null);
    }
  }.observes('controller.isCompareMode'),
  /**
   * revert config values to chosen version and apply reverted configs to server
   */
  revert: function (event) {
    var self = this;
    var serviceConfigVersion = event.contexts[0] || this.get('hoveredServiceVersion') || Em.Object.create({
      version: this.get('displayedServiceVersion.version'),
      serviceName: this.get('displayedServiceVersion.serviceName'),
      notes:''
    });
    if (serviceConfigVersion.get('isReference')) {
      serviceConfigVersion = this.get(serviceConfigVersion.get('property'));
    }
    var versionText = serviceConfigVersion.get('versionText');
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.configHistory.info-bar.makeCurrent.popup.title'),
      serviceConfigNote: Em.I18n.t('services.service.config.configHistory.makeCurrent.message').format(versionText),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          value: Em.I18n.t('services.service.config.configHistory.makeCurrent.message').format(versionText),
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      primary: Em.I18n.t('dashboard.configHistory.info-bar.revert.button'),
      secondary: Em.I18n.t('common.discard'),
      third: Em.I18n.t('common.cancel'),
      onPrimary: function () {
        serviceConfigVersion.set('serviceConfigNote', this.get('serviceConfigNote'));
        self.sendRevertCall(serviceConfigVersion);
        this.hide();
      },
      onSecondary: function () {
        // force <code>serviceVersions</code> recalculating
        self.propertyDidChange('controller.selectedConfigGroup.name');
        this._super();
      },
      onThird: function () {
        this.onSecondary();
      }
    });
  },

  /**
   * send PUT call to revert config to selected version
   * @param serviceConfigVersion
   */
  sendRevertCall: function (serviceConfigVersion) {
    App.ajax.send({
      name: 'service.serviceConfigVersion.revert',
      sender: this,
      data: {
        data: {
          "Clusters": {
            "desired_service_config_versions": {
              "service_config_version": serviceConfigVersion.get('version'),
              "service_name": serviceConfigVersion.get('serviceName'),
              "service_config_version_note": serviceConfigVersion.get('serviceConfigNote')
            }
          }
        }
      },
      success: 'sendRevertCallSuccess'
    });
  },

  sendRevertCallSuccess: function (data, opt, params) {
    // revert to an old version would generate a new version with latest version number,
    // so, need to loadStep to update
    App.router.get('updateController').updateComponentConfig(Em.K);
    this.get('controller').loadStep();
  },

  /**
   * save configuration
   * @return {object}
   */
  save: function () {
    var self = this;
    var passwordWasChanged = this.get('controller.passwordConfigsAreChanged');
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.title'),
      serviceConfigNote: '',
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        showPasswordChangeWarning: passwordWasChanged,
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          value: passwordWasChanged ? Em.I18n.t('dashboard.configHistory.info-bar.save.popup.notesForPasswordChange') : '',
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
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        var newVersionToBeCreated = App.ServiceConfigVersion.find().filterProperty('serviceName', self.get('serviceName')).get('length') + 1;
        self.get('controller').setProperties({
          saveConfigsFlag: true,
          serviceConfigVersionNote: this.get('serviceConfigNote'),
          preSelectedConfigVersion: Em.Object.create({
            version: newVersionToBeCreated,
            serviceName: self.get('displayedServiceVersion.serviceName'),
            groupName: self.get('controller.selectedConfigGroup.name')
          })
        });
        self.get('controller').saveStepConfigs();
        this.hide();
      },
      onDiscard: function () {
        this.hide();
        self.set('controller.preSelectedConfigVersion', null);
        self.get('controller').loadStep();
      },
      onCancel: function () {
        this.hide();
      }
    });
  },
  /**
   * move back to the later service version
   */
  shiftBack: function () {
    if (!this.get('showLeftArrow')) return;
    this.decrementProperty('startIndex');
    this.adjustFlowView();
  },
  /**
   * move forward to the previous service version
   */
  shiftForward: function () {
    if (!this.get('showRightArrow')) return;
    this.incrementProperty('startIndex');
    this.adjustFlowView();
  },
  /**
   * shift flow view to position where selected version is visible
   * @param versionIndex
   */
  shiftFlowOnSwitch: function (versionIndex) {
    var serviceVersions = this.get('serviceVersions');

    if ((this.get('startIndex') + this.VERSIONS_IN_FLOW) < versionIndex || versionIndex < this.get('startIndex')) {
      versionIndex = (serviceVersions.length < (versionIndex + this.VERSIONS_IN_FLOW)) ? serviceVersions.length - this.VERSIONS_IN_FLOW : versionIndex;
      this.set('startIndex', versionIndex);
      this.adjustFlowView();
    }
  }
});

App.ConfigsServiceVersionBoxView = Em.View.extend({

  /**
   * bound from template
   */
  serviceVersion: null,

  actionTypesBinding: 'parentView.actionTypes',

  disabledActionAttr: Em.computed.alias('serviceVersion.disabledActionAttr'),

  disabledActionMessages: Em.computed.alias('serviceVersion.disabledActionMessages'),

  templateName: require('templates/common/configs/service_version_box'),

  didInsertElement: function () {
    this._super();
    this.$('.version-box').hoverIntent(function() {
      if ($(this).is(':hover')) {
        $(this).find('.version-popover').delay(700).fadeIn(200).end();
      }
    }, function() {
      $(this).find('.version-popover').stop().fadeOut(200).end();
    });
    App.tooltip(this.$('[data-toggle=tooltip]'), {
      placement: 'bottom'
    });
    App.tooltip(this.$('[data-toggle=arrow-tooltip]'), {
      placement: 'top'
    });
  },

  willDestroyElement: function() {
    this.$('.version-box').off();
    this.$('[data-toggle=tooltip]').tooltip('destroy');
    this.$('[data-toggle=arrow-tooltip]').tooltip('destroy');
  }
});

App.ConfigHistoryDropdownRowView = Em.View.extend({

  templateName: require('templates/common/configs/config_history_dropdown_row'),

  tagName: "li",

  classNameBindings: [':pointer', ':dropdown-submenu', 'isDisplayed:not-allowed'],

  serviceVersion: null,

  isDisplayed: function() {
    var serviceVersion = this.get('serviceVersion');
    if(serviceVersion) {
      return serviceVersion.get('isDisplayed');
    }
    return false;
  }.property('serviceVersion'),

  actionTypesBinding: 'parentView.actionTypes',

  doAction: function(event) {
    this.get('parentView').doAction(event);
  },

  eventManager: Ember.Object.create({
    mouseEnter: function(event, view) {
      var serviceVersion = view.get('serviceVersion');
      var version = serviceVersion.get('version');
      var $el = $('#config_version_popup');
      var $currentTarget = $(event.currentTarget);
      var parentView = view.get('parentView');
      parentView.set('hoveredServiceVersion', null);
      if (!serviceVersion.get("isDisplayed"))  {
        parentView.set('hoveredServiceVersion', serviceVersion);
        parentView.set('isHovered', true);
        var elHeight = $el.outerHeight(),
          pagePosition = window.innerHeight + window.pageYOffset,
          elBottomPosition = $currentTarget[0].getBoundingClientRect().top + elHeight,
          shouldShowUp = elBottomPosition > pagePosition;
        $el.css({
          "position": "fixed",
          "top": $currentTarget[0].getBoundingClientRect().top,
          "left": $currentTarget[0].getBoundingClientRect().left + 400,
          "margin-top": -(elHeight/3),
          "display": "block"
        });
        if (shouldShowUp) {
          $el.css('margin-top', -(elHeight - $currentTarget.outerHeight()));
        }
      }
      $el = null;
    },
    mouseLeave: function(event, view) {
      var parentView = view.get('parentView');
      parentView.set('isHovered', false);
      Em.run.later(function() {
        if(!parentView.get('displaySubMenuFlag') && !parentView.get('isHovered')) {
          $('#config_version_popup').removeAttr('style');
        }
      }, 200);
    }
  })
});

App.ConfigHistoryDropdownSubMenuView = Em.View.extend({

  tagName: 'ul',

  classNameBindings: [':dropdown-menu', ':version-info-operations'],

  eventManager: Ember.Object.create({
    mouseEnter: function(event, view) {
      view.get('parentView').set('displaySubMenuFlag', true);
    },
    mouseLeave: function(event, view) {
      var parentView = view.get('parentView');
      parentView.set('displaySubMenuFlag', false);
      $("#config_version_popup").removeAttr('style');
    }
  })
});
