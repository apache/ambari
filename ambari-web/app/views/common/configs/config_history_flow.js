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
  showLeftArrow: true,
  showRightArrow: false,
  /**
   * flag identify whether to show all versions or short list of them
   */
  showFullList: false,

  serviceName: function () {
    return this.get('controller.selectedService.serviceName');
  }.property('controller.selectedService.serviceName'),

  displayedServiceVersion: function () {
    return this.get('serviceVersions').findProperty('isDisplayed');
  }.property('serviceVersions.@each.isDisplayed'),
  /**
   * identify whether to show link that open whole content of notes
   */
  showMoreLink: function () {
    //100 is number of symbols that fit into label
    return (this.get('currentServiceVersion.notes.length') > 100);
  }.property('currentServiceVersion.notes.length'),
  /**
   * formatted notes ready to display
   */
  shortNotes: function () {
    //100 is number of symbols that fit into label
    if (this.get('showMoreLink')) {
      return this.get('currentServiceVersion.notes').slice(0, 100) + '...';
    }
    return this.get('currentServiceVersion.notes');
  }.property('currentServiceVersion'),
  /**
   * service versions which in viewport and visible to user
   */
  visibleServiceVersion: function () {
    return this.get('serviceVersions').slice(this.get('startIndex'), (this.get('startIndex') + 5));
  }.property('startIndex'),

  /**
   * list of service versions
   * by default 6 is number of items in short list
   */
  dropDownList: function () {
    var serviceVersions = this.get('serviceVersions').without(this.get('currentServiceVersion')).slice(0).reverse();
    if (this.get('showFullList')) {
      return serviceVersions;
    }
    return serviceVersions.slice(0, 6);
  }.property('serviceVersions', 'showFullList'),

  openFullList: function (event) {
    event.stopPropagation();
    this.set('showFullList', true);
  },
  hideFullList: function (event) {
    this.set('showFullList', false);
  },

  willInsertElement: function () {
    var serviceVersions = this.get('serviceVersions');
    var startIndex = 0;
    var numberOfDisplayed = 5;

    this.get('serviceVersions').findProperty('appliedTime', Math.max.apply(this, this.get('serviceVersions').mapProperty('appliedTime'))).set('isCurrent', true);
    this.get('serviceVersions').findProperty('isCurrent').set('isDisplayed', true);

    if (serviceVersions.length > numberOfDisplayed) {
      startIndex = serviceVersions.length - numberOfDisplayed;
      this.set('startIndex', startIndex);
      this.adjustFlowView();
    }
    this.keepInfoBarAtTop()
  },

  /**
   * initialize event to keep info bar position at the top of the page
   */
  keepInfoBarAtTop: function () {
    var defaultTop;
    //reset defaultTop value in closure
    $(window).unbind('scroll');

    $(window).on('scroll', function (event) {
      var infoBar = $('#config_history_flow>.version-info-bar');
      var scrollTop = $(window).scrollTop();

      //290 - default "top" property in px
      defaultTop = defaultTop || (infoBar.get(0).getBoundingClientRect() && infoBar.get(0).getBoundingClientRect().top) || 290;

      if (scrollTop > defaultTop) {
        infoBar.css('top', '10px');
      } else if (scrollTop > 0) {
        infoBar.css('top', (defaultTop - scrollTop) + 'px');
      } else {
        infoBar.css('top', 'auto');
      }
    })
  },
  /**
   *  define the first element in viewport
   *  change visibility of arrows
   */
  adjustFlowView: function () {
    var startIndex = this.get('startIndex');

    this.get('serviceVersions').forEach(function (serviceVersion, index) {
      serviceVersion.set('first', (index === startIndex));
    });
    this.set('showLeftArrow', (startIndex !== 0));
    this.set('showRightArrow', ((startIndex + 5) !== this.get('serviceVersions.length')));
  },

  /**
   * switch configs view version to chosen
   */
  switchVersion: function (event) {
    var version = event.context.get('version');
    this.get('serviceVersions').forEach(function (serviceVersion) {
      serviceVersion.set('isDisplayed', serviceVersion.get('version') === version);
    });
    //TODO implement load configs for chosen version
  },

  /**
   * add config values of chosen version to view for comparison
   */
  compare: function (event) {
    this.set('controller.compareServiceVersion', event.context);
    this.get('controller').onConfigGroupChange();
  },
  /**
   * revert config values to chosen version and apply reverted configs to server
   */
  revert: function () {
    //TODO implement put configs of chosen version to server
  },

  /**
   * cancel configuration saving
   */
  cancel: function () {

  },
  /**
   * save configuration
   * @return {object}
   */
  save: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.title'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder')
        })
      }),
      footerClass: Ember.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        this.hide();
      },
      onDiscard: function () {
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  },
  serviceVersions: [
    Em.Object.create({
      serviceName: 'HDFS',
      version: '1',
      modifiedDate: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 1
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '2',
      modifiedDate: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 2
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '3',
      modifiedDate: 'Apr 4, 2014',
      author: 'user',
      notes: 'notes',
      appliedTime: 3
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '4',
      modifiedDate: 'Apr 4, 2014',
      author: 'user',
      notes: 'notes',
      appliedTime: 4
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '5',
      modifiedDate: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 5
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '22',
      modifiedDate: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 6
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '33',
      modifiedDate: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 7
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '44',
      modifiedDate: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 8
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '55',
      modifiedDate: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 9
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '666',
      modifiedDate: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      appliedTime: 10
    })
  ],
  /**
   * move back to the previous service version
   */
  shiftBack: function () {
    this.decrementProperty('startIndex');
    this.adjustFlowView();
  },
  /**
   * move forward to the next service version
   */
  shiftForward: function () {
    this.incrementProperty('startIndex');
    this.adjustFlowView();
  }
});
