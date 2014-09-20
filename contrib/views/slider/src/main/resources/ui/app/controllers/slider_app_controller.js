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

App.SliderAppController = Ember.ObjectController.extend(App.AjaxErrorHandler, {

  /**
   * List of Slider App tabs
   * @type {{title: string, linkTo: string}[]}
   */
  sliderAppTabs: function () {
    var configs = this.get("model.configs");
    var tabs = Ember.A([
      Ember.Object.create({title: Ember.I18n.t('common.summary'), linkTo: 'slider_app.summary'})
    ]);
    if(typeof configs == "object" && Object.keys(configs).length > 0){
      tabs.pushObject(Ember.Object.create({title: Ember.I18n.t('common.configs'), linkTo: 'slider_app.configs'}));
    }
    return tabs;
  }.property('model.configs'),

  /**
   * Do we have quicklinks ?
   * @type {bool}
   */
  weHaveQuicklinks: function () {
    return (Em.get(this.get('model'), 'quickLinks.content.content.length') > 0);
  }.property('model.quickLinks.content.content.length'),

  /**
   * List of available for model actions
   * Based on <code>model.status</code>
   * @type {Ember.Object[]}
   */
  availableActions: function() {
    var actions = Em.A([]),
      status = this.get('model.status');
    if ('RUNNING' === status) {
      actions.pushObject({
        title: 'Stop',
        action: 'freeze',
        confirm: true
      });
    }
    if ('RUNNING' == status) {
      actions.push({
        title: 'Flex',
        action: 'flex',
        confirm: false
      });
    }
    if ('FROZEN' === status) {
      actions.pushObjects([
        {
          title: 'Start',
          action: 'thaw',
          confirm: false
        },
        {
          title: 'Destroy',
          action: 'destroy',
          confirm: true
        }
      ]);
    }
    return actions;
  }.property('model.status'),

  /**
   * Method's name that should be called for model
   * @type {string}
   */
  currentAction: null,

  /**
   * Try call controller's method with name stored in <code>currentAction</code>
   * @method tryDoAction
   */
  tryDoAction: function() {
    var currentAction = this.get('currentAction');
    if (Em.isNone(currentAction)) return;
    if(Em.typeOf(this[currentAction]) !== 'function') return;
    this[currentAction]();
  },

  /**
   * Do request to <strong>thaw</strong> current slider's app
   * @returns {$.ajax}
   * @method freeze
   */
  thaw: function() {
    var model = this.get('model');
    return App.ajax.send({
      name: 'changeAppState',
      sender: this,
      data: {
        id: model.get('id'),
        data: {
          id: model.get('id'),
          name: model.get('name'),
          state: "RUNNING"
        }
      },
      success: 'thawSuccessCallback'
    });
  },

  /**
   * Redirect to Slider Apps Table page on successful thawing
   * @method thawSuccessCallback
   */
  thawSuccessCallback: function () {
    this.transitionToRoute('slider_apps.index');
  },

  /**
   * Do request to <strong>freeze</strong> current slider's app
   * @returns {$.ajax}
   * @method freeze
   */
  freeze: function() {
    var model = this.get('model');
    return App.ajax.send({
      name: 'changeAppState',
      sender: this,
      data: {
        id: model.get('id'),
        data: {
          id: model.get('id'),
          name: model.get('name'),
          state: "FROZEN"
        }
      }
    });
  },

  /**
   * Buttons for Flex modal popup
   * @type {Em.Object[]}
   */
  flexModalButtons: [
    Ember.Object.create({title: Em.I18n.t('common.cancel'), clicked:"closeFlex", dismiss: 'modal'}),
    Ember.Object.create({title: Em.I18n.t('common.send'), clicked:"submitFlex", type:'success'})
  ],

  /**
   * Grouped components by name
   * @type {{name: string, count: number}[]}
   */
  groupedComponents: [],

  /**
   * Group components by <code>componentName</code> and save them to <code>groupedComponents</code>
   * @method groupComponents
   */
  groupComponents: function() {
    var groupedComponents = this.get('appType.components').map(function(c) {
      return {
        name: c.get('name'),
        count: 0
      };
    });

    this.get('components').forEach(function(component) {
      var name = component.get('componentName'),
        group = groupedComponents.findBy('name', name);
      if (group) {
        group.count++;
      }
    });
    this.set('groupedComponents', groupedComponents);
  },

  /**
   * Does new instance counts are invalid
   * @type {bool}
   */
  groupedComponentsHaveErrors: false,

  /**
   * Validate new instance counts for components (should be integer and >= 0)
   * @method validateGroupedComponents
   * @returns {boolean}
   */
  validateGroupedComponents: function() {
    var hasErrors = false;
    this.get('groupedComponents').forEach(function(c) {
      if (!/^\d+$/.test(c.count)) {
        hasErrors = true;
        return;
      }
      var count = parseInt(c.count + 0);
      if (count < 0) {
        hasErrors = true;
      }
    });
    this.set('groupedComponentsHaveErrors', hasErrors);
    return hasErrors;
  },

  /**
   * Do request to <strong>flex</strong> current slider's app
   * @method flex
   */
  flex: function() {
    this.groupComponents();
    Bootstrap.ModalManager.open(
      'flex-popup',
      'Flex',
      'slider_app/flex_popup',
      this.get('flexModalButtons'),
      this
    );
  },

  /**
   * Map <code>model.components</code> for Flex request
   * Output format:
   * <code>
   *   {
   *      COMPONENT_NAME_1: {
   *        instanceCount: 1
   *      },
   *      COMPONENT_NAME_2: {
   *        instanceCount: 2
   *      },
   *      ....
   *   }
   * </code>
   * @returns {object}
   * @method mapComponentsForFlexRequest
   */
  mapComponentsForFlexRequest: function() {
    var components = {};
    this.get('groupedComponents').forEach(function(component) {
      components[Em.get(component, 'name')] = {
        instanceCount: Em.get(component, 'count')
      }
    });
    return components;
  },

  /**
   * Do request to <strong>delete</strong> current slider's app
   * @return {$.ajax}
   * @method destroy
   */
  destroy: function() {
    return App.ajax.send({
      name: 'destroyApp',
      sender: this,
      data: {
        id: this.get('model.id')
      },
      complete: 'destroyCompleteCallback'
    });
  },

  /**
   * Complate-callback for "destroy app"-request
   * @method destroyCompleteCallback
   */
  destroyCompleteCallback: function() {
    this.transitionToRoute('slider_apps');
  },

  actions: {

    /**
     * Submit new instance counts for app components
     * @method submitFlex
     * @returns {*}
     */
    submitFlex: function() {
      if (this.validateGroupedComponents()) return;
      var model = this.get('model'),
        components = this.mapComponentsForFlexRequest();
      this.get('groupedComponents').clear();
      this.set('groupedComponentsHaveErrors', false);
      Bootstrap.ModalManager.close('flex-popup');
      return App.ajax.send({
        name: 'flexApp',
        sender: this,
        data: {
          id: model.get('id'),
          data: {
            id: model.get('id'),
            name: model.get('name'),
            components: components
          }
        }
      });
    },

    /**
     * Close flex-popup
     * @method closeFlex
     */
    closeFlex: function() {
      this.get('groupedComponents').clear();
      this.set('groupedComponentsHaveErrors', false);
    },

    /**
     * Handler for "Yes" click in modal popup
     * @returns {*}
     * @method modalConfirmed
     */
    modalConfirmed: function() {
      this.tryDoAction();
      return Bootstrap.ModalManager.close('confirm-modal');
    },

    /**
     * Handler for "No" click in modal popup
     * @returns {*}
     * @method modalCanceled
     */
    modalCanceled: function() {
      return Bootstrap.ModalManager.close('confirm-modal');
    },

    /**
     * Handler for Actions menu elements click
     * @param {{title: string, action: string, confirm: bool}} option
     * @method openModal
     */
    openModal: function(option) {
      this.set('currentAction', option.action);
      if (option.confirm) {
        Bootstrap.ModalManager.open(
          "confirm-modal",
          Ember.I18n.t('common.confirmation'),
          Ember.View.extend({
            template: Ember.Handlebars.compile('{{t question.sure}}')
          }),
          [
            Ember.Object.create({title: Em.I18n.t('common.cancel'), clicked:"modalCanceled", dismiss: 'modal'}),
            Ember.Object.create({title: Em.I18n.t('ok'), clicked:"modalConfirmed", type:'success'})
          ],
          this
        );
      }
      else {
        this.tryDoAction();
      }
    }
  }

});
