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

/**
 * Attach default error handler on error of Ajax calls
 * To correct work should be mixed with Controller or View instance
 * Example:
 *  <code>
 *    var obj = Ember.Controller.extend(App.AjaxErrorHandler, {
 *      callToServer: function() {
 *        App.ajax.send(config);
 *      }
 *    });
 *    if ajax config doesn't have error handler then the default hanlder will be established
 *  </code>
 * @type {Ember.Mixin}
 */
App.AjaxErrorHandler = Ember.Mixin.create({
  /**
   * flag to indicate whether popup with ajax already opened to avoid popup overlaying
   */
  errorPopupShown: false,
  /**
   * defaultErrorHandler function is referred from App.ajax.send function
   * @jqXHR {jqXHR Object}
   * @url {string}
   * @method {String} Http method
   * @showErrorPopup {boolean}
   */
  defaultErrorHandler: function (jqXHR, url, method, showErrorPopup) {
    var self = this;
    method = method || 'GET';
    var context = this.get('isController') ? this : (this.get('isView') && this.get('controller'));
    try {
      var json = $.parseJSON(jqXHR.responseText);
      var message = json.message;
    } catch (err) {
    }

    if (!context) {
      console.warn('WARN: App.AjaxErrorHandler should be used only for views and controllers');
      return;
    }
    if (showErrorPopup && !this.get('errorPopupShown')) {
      Bootstrap.ModalManager.open(
        "ajax-error-modal",
        Em.I18n.t('common.error'),
        Ember.View.extend({
          classNames: ['api-error'],
          templateName: 'common/ajax_error',
          api: Em.I18n.t('ajax.apiInfo').format(method, url),
          statusCode: Em.I18n.t('ajax.statusCode').format(jqXHR.status),
          message: message,
          showMessage: !!message,
          willDestroyElement: function () {
            self.set('errorPopupShown', false);
          }
        }),
        [
          Ember.Object.create({title: Em.I18n.t('ok'), dismiss: 'modal', type: 'success'})
        ],
        context
      );
      this.set('errorPopupShown', true);
    }
  }
});