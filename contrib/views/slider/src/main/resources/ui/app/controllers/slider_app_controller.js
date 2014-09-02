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

App.SliderAppController = Ember.ObjectController.extend({

  /**
   * List of Slider App tabs
   * @type {{title: string, linkTo: string}[]}
   */
  sliderAppTabs: Ember.A([
    Ember.Object.create({title: Ember.I18n.t('common.summary'), linkTo: 'slider_app.summary'}),
    Ember.Object.create({title: Ember.I18n.t('common.configs'), linkTo: 'slider_app.configs'})
  ]),

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
    if ('FINISHED' !== status) {
      actions.push({
        title: 'Flex',
        action: 'flex',
        confirm: true
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
      }
    });
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
   * Do request to <strong>flex</strong> current slider's app
   * @returns {$.ajax}
   * @method flex
   */
  flex: function() {
    var model = this.get('model');
    return App.ajax.send({
      name: 'flexApp',
      sender: this,
      data: {
        id: model.get('id'),
        data: {
          id: model.get('id'),
          name: model.get('name'),
          components: this.mapComponentsForFlexRequest()
        }
      }
    });
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
    this.get('model.components').forEach(function(component) {
      components[component.get('name')] = {
        instanceCount: component.get('defaultNumInstances')
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
        Bootstrap.ModalManager.confirm(
          this,
          Ember.I18n.t('common.confirmation'),
          Ember.I18n.t('question.sure'),
          Ember.I18n.t('yes'),
          Ember.I18n.t('no')
        );
      }
      else {
        this.tryDoAction();
      }
    }
  }

});
