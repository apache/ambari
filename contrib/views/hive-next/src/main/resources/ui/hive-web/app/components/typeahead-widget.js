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

import Typeahead from 'ember-cli-selectize/components/ember-selectize';
import Ember from 'ember';

export default Typeahead.extend(Ember.I18n.TranslateableProperties, {
  didInsertElement: function () {
    this._super();

    if (!this.get('selection') && this.get('content.firstObject')) {
      this.set('selection', this.get('content.firstObject'));
    }

    this.selectize.on('dropdown_close', Ember.$.proxy(this.onClose, this));

    if($('.selectize-input')) {$('.selectize-input').addClass( "mozBoxSizeFix" );}

    var currentKeyName = this.get('safeValue');
    var currentTypehead = $('*[keyname="' + currentKeyName +'"]');

    if (currentTypehead.find($('.selectize-input')).has('.item').length == 0) {
      currentTypehead.find($('.selectize-input')).addClass("has-options has-items ");

      currentTypehead.find($('.selectized option:selected')).val(currentKeyName);
      currentTypehead.find($('.selectized option:selected')).text(currentKeyName);

      currentTypehead.find($('.selectize-input input')).css({'opacity': 0 , 'position': 'absolute' , 'left': '-10000px'});

      var itemHtml = '<div data-value=' + currentKeyName + ' class=item >' + currentKeyName + '</div>';
      currentTypehead.find($('.selectize-input')).append( itemHtml );

    }

    Selectize.prototype.onMouseDown = function(e) {

      var self = this;
      var defaultPrevented = e.isDefaultPrevented();
      var $target = $(e.target);

      if (self.isFocused) {
        if (e.target !== self.$control_input[0]) {
          if (self.settings.mode === 'single') {
            self.isOpen ? self.close() : self.open();
          } else if (!defaultPrevented) {
            self.setActiveItem(null);
          }
          return false;
        }
      } else {
        if (!defaultPrevented) {
            self.focus();
        }
      }
    };
  },
  removeExcludedObserver: function () {
    var options = this.get('content');

    if (!options) {
      options = this.removeExcluded(true);
      this.set('content', options);
    } else {
      this.removeExcluded();
    }
  }.observes('excluded.@each.key').on('init'),

  removeExcluded: function (shouldReturn) {
    var excluded        = this.get('excluded') || [];
    var options         = this.get('options');
    var selection       = this.get('selection');
    var objectToModify  = this.get('content');
    var objectsToRemove = [];
    var objectsToAdd    = [];

    if (!options) {
      return;
    }

    if (shouldReturn) {
      objectToModify = Ember.copy(options);
    }

    var valuePath = this.get('optionValuePath');
    var selectionName = selection ? selection[valuePath] : selection;

    if (options) {
      options.forEach(function (option) {
        if (excluded.contains(option) && option.name !== selectionName) {
          objectsToRemove.push(option);
        } else if (!objectToModify.contains(option)) {
          objectsToAdd.push(option);
        }
      });
    }

    objectToModify.removeObjects(objectsToRemove);
    objectToModify.pushObjects(objectsToAdd);

    return objectToModify;
  },

  onClose: function () {
    if (!this.get('selection') && this.get('prevSelection')) {
      this.set('selection', this.get('prevSelection'));
    }
  },

  _onItemAdd: function (value) {
    this._super(value);

    this.set('prevSelection', this.get('selection'));
  }
});
