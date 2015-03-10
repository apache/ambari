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
   * Quick links in custom order.
   *
   * @type {Array}
   **/
  quickLinksOrdered: function() {
    var copy = this.get('model.quickLinks').slice(0);
    var toTail = ['Metrics UI', 'Metrics API'];

    if (this.get('weHaveQuicklinks')) {
      toTail.forEach(function(labelName) {
        if (copy.findBy('label', labelName)) {
          copy = copy.concat(copy.splice(copy.indexOf(copy.findBy('label', labelName)), 1));
        }
      });
    }
    return copy;
  }.property('model.quickLinks.content.content.length', 'weHaveQuicklinks'),

  /**
   * List of all possible actions for slider app
   * @type {Em.Object}
   */
  appActions: Em.Object.create({
    stop: {
      title: 'Stop',
      action: 'freeze',
      confirm: true
    },
    flex: {
      title: 'Flex',
      action: 'flex',
      confirm: false
    },
    destroy: {
      title: 'Destroy',
      action: 'destroy',
      customConfirm: 'confirmDestroy'
    },
    start: {
      title: 'Start',
      action: 'thaw',
      confirm: false
    }
  }),

  /**
   * map of available action for slider app according to its status
   * key - status, value - list of actions
   * @type {Em.Object}
   */
  statusActionsMap: Em.Object.create({
    NEW: ['stop'],
    NEW_SAVING: ['stop'],
    ACCEPTED: ['stop'],
    RUNNING: ['stop', 'flex'],
    FINISHED: ['start', 'destroy'],
    FAILED: ['destroy'],
    KILLED: ['destroy'],
    FROZEN: ['start', 'destroy']
  }),

  /**
   * List of available for model actions
   * Based on <code>model.status</code>
   * @type {Ember.Object[]}
   */
  availableActions: function() {
    var actions = Em.A([]),
      advanced = Em.A([]),
      appActions = this.get('appActions'),
      statusActionsMap = this.get('statusActionsMap'),
      status = this.get('model.status');

    if (this.get('model.isActionFinished')) this.get('model').set('isActionPerformed', false);
    statusActionsMap[status].forEach(function(action) {
      if ('destroy' === action) {
        advanced.pushObject(appActions[action]);
      }
      else {
        actions.pushObject(appActions[action]);
      }
    });

    if (advanced.length) {
      actions.pushObject({
        title: 'Advanced',
        submenu: advanced
      });
    }
    return actions;
  }.property('model.status'),

  /**
   * Checkbox in the destroy-modal
   * If true - enable "Destroy"-button
   * @type {bool}
   */
  confirmChecked: false,

  /**
   * Inverted <code>confirmChecked</code>-value
   * Used in <code>App.DestroyAppPopupFooterView</code> to enable "Destroy"-button
   * @type {bool}
   */
  destroyButtonEnabled: Ember.computed.not('confirmChecked'),

  /**
   * Method's name that should be called for model
   * @type {string}
   */
  currentAction: null,

  /**
   * Grouped components by name
   * @type {{name: string, count: number}[]}
   */
  groupedComponents: [],

  /**
   * Does new instance counts are invalid
   * @type {bool}
   */
  groupedComponentsHaveErrors: false,

  /**
   * Custom popup for "Destroy"-action
   * @method destroyConfirm
   */
  confirmDestroy: function() {
    var modalComponent = this.container.lookup('component-lookup:main').
      lookupFactory('bs-modal', this.get('container')).create();
    modalComponent.setProperties({
      name: 'confirm-modal',
      title: Ember.I18n.t('sliderApp.destroy.confirm.title'),
      manual: true,
      targetObject: this,
      body: App.DestroyAppPopupView,
      controller: this,
      footerViews: [App.DestroyAppPopupFooterView]
    });
    Bootstrap.ModalManager.register('confirm-modal', modalComponent);
  },

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
    this.setStartAction();
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
      success: 'thawSuccessCallback',
      error: 'actionErrorCallback'
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
    this.setStartAction();
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
      },
      error: 'actionErrorCallback'
    });
  },

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
      Em.A([
        Ember.Object.create({title: Em.I18n.t('common.cancel'), clicked:"closeFlex", dismiss: 'modal'}),
        Ember.Object.create({title: Em.I18n.t('common.save'), clicked:"submitFlex", type:'success'})
      ]),
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
    this.setStartAction();
    return App.ajax.send({
      name: 'destroyApp',
      sender: this,
      data: {
        model: this.get('model'),
        id: this.get('model.id')
      },
      success: 'destroySuccessCallback',
      error: 'actionErrorCallback'
    });
  },

  /**
   * Complete-callback for "destroy app"-request
   * @method destroyCompleteCallback
   */
  destroySuccessCallback: function(data, opt, params) {
    params.model.deleteRecord();
    this.store.dematerializeRecord(params.model);
    this.transitionToRoute('slider_apps');
  },

  actionErrorCallback: function() {
    this.defaultErrorHandler(arguments[0], arguments[3].url, arguments[3].type, true);
    this.get('model').set('isActionPerformed', false);
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
      this.set('confirmChecked', false);
      return Bootstrap.ModalManager.close('confirm-modal');
    },

    /**
     * Handler for "No" click in modal popup
     * @returns {*}
     * @method modalCanceled
     */
    modalCanceled: function() {
      this.set('confirmChecked', false);
      return Bootstrap.ModalManager.close('confirm-modal');
    },

    /**
     * Handler for Actions menu elements click
     * @param {{title: string, action: string, confirm: bool}} option
     * @method openModal
     */
    openModal: function(option) {
      if (!option.action) return false;
      this.set('currentAction', option.action);
      if (!Em.isNone(option.customConfirm) && Ember.typeOf(this.get(option.customConfirm)) === 'function') {
        this[option.customConfirm]();
      }
      else {
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
  },

  setStartAction: function() {
    this.get('model').set('isActionPerformed' , true);
    this.get('model').set('statusBeforeAction' , this.get('model.status'));
  }

});
