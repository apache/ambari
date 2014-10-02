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
 * Mixin for views that use Bootstrap.BsPanelComponent component
 * Add caret for collapsed/expanded panels at the left of panel's title
 * Usage:
 * <code>
 *  App.SomeView = Em.View.extend(App.WithPanels, {
 *    didInsertElement: function() {
 *      this.addCarets();
 *    }
 *  });
 * </code>
 * @type {Em.Mixin}
 */
App.WithPanels = Ember.Mixin.create({

  /**
   * Add caret before panel's title and add handlers for expand/collapse events
   * Set caret-down when panel is expanded
   * Set caret-right when panel is collapsed
   * @method addArrows
   */
  addCarets: function() {
    var panel = $('.panel');
    panel.find('.panel-heading').prepend('<span class="pull-left icon icon-caret-right"></span>');
    panel.find('.panel-collapse.collapse.in').each(function() {
      $(this).parent().find('.icon.icon-caret-right:first-child').addClass('icon-caret-down').removeClass('icon-caret-right');
    });
    panel.on('hidden.bs.collapse', function (e) {
      $(e.delegateTarget).find('span.icon').addClass('icon-caret-right').removeClass('icon-caret-down');
    }).on('shown.bs.collapse', function (e) {
        $(e.delegateTarget).find('span.icon').addClass('icon-caret-down').removeClass('icon-caret-right');
      });
  }

});
