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
var dateUtils = require('utils/date/date');
var fileUtils = require('utils/file_utils');

App.showLogTailPopup = function(content) {
  return App.ModalPopup.show({
    classNames: ['log-tail-popup', 'full-width-modal', 'full-height-modal'],
    header: fileUtils.fileNameFromPath(content.get('filePath')),
    primary: false,
    secondary: Em.I18n.t('common.dismiss'),
    secondaryClass: 'btn-success',
    showFooter: true,
    autoHeight: false,
    bodyClass: Em.View.extend({
      templateName: require('templates/common/modal_popups/log_tail_popup'),
      content: content,
      selectedTailCount: 50,
      isCopyActive: false,
      copyContent: null,

      logSearchUrl: function() {
        var quickLink = App.QuickLinks.find().findProperty('site', 'logsearch-env'),
            logSearchServerHost = App.HostComponent.find().findProperty('componentName', 'LOGSEARCH_SERVER').get('hostName');

        if (quickLink) {
          return quickLink.get('template').fmt('http', logSearchServerHost, quickLink.get('default_http_port')) + '?host_name=' + this.get('content.hostName') + '&file_name=' + this.get('content.filePath') + '&component_name=' + this.get('content.logComponentName');
        }
        return '#';
      }.property('content'),

      logTailViewInstance: null,

      /** actions **/
      openInNewTab: function() {
        var newWindow = window.open();
        var newDocument = newWindow.document;
        newDocument.write($('.log-tail-content.pre-styled').html());
        newDocument.close();
      },

      toggleCopy: function() {
        if (!this.get('isCopyActive')) {
          this.initCopy();
        } else {
          this.destroyCopy();
        }
      },

      initCopy: function() {
        var self = this;
        this.set('copyContent', this.logsToString());
        this.set('isCopyActive', true);
        Em.run.next(function() {
          self.$().find('.copy-textarea').select();
        });
      },

      destroyCopy: function() {
        this.set('copyContent', null);
        this.set('isCopyActive', false);
      },

      logsToString: function() {
        return this.get('logTailViewInstance.logRows').map(function(i) {
          return i.get('logtimeFormatted') + ' ' + i.get('level') + ' ' + i.get('logMessage');
        }).join('\n');
      },

      logTailContentView: App.LogTailView.extend({
        contentBinding: "parentView.content",
        autoResize: true,
        selectedTailCountBinding: "parentView.selectedTailCount",

        didInsertElement: function() {
          this._super();
          this.set('parentView.logTailViewInstance', this);
        },

        resizeHandler: function() {
          if (this.get('state') === 'destroyed') return;
          this._super();
          var newSize = $(window).height() - this.get('resizeDelta') - window.innerHeight*0.08;
          this.get('parentView').$().find('.copy-textarea').css({
            height: newSize + 'px',
            width: '100%'
          });
        },

        willDestroyElement: function() {
          this._super();
          this.set('parentView.logTailViewInstance', null);
        }
      })
    })
  });
};
