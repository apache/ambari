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

App.DestroyAppPopupFooterView = Ember.View.extend({

  /**
   * targetObject should be defined for buttons and other components that may set actions
   * @type {Em.Controller}
   */
  targetObjectBinding: 'controller',

  templateName: 'slider_app/destroy/destroy_popup_footer',

  /**
   * Destroy-button
   * @type {Em.Object}
   */
  destroyButton: Em.Object.create({title: Em.I18n.t('common.destroy'), clicked: "modalConfirmed", type:'success'}),

  /**
   * Cancel-button
   * @type {Em.Object}
   */
  cancelButton: Em.Object.create({title: Em.I18n.t('common.cancel'), clicked: "modalCanceled"})

});