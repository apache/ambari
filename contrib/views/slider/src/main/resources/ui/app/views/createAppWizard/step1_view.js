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

App.CreateAppWizardStep1View = Ember.View.extend({

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  availableTypesSelect: Ember.Select.extend({

    /**
     * Forbid user to select more than one App type
     * Set selected type to <code>controller.selectedType</code>
     */
    setSelection: function () {
      var content = this.get('content');
      var selection = this.get('selection');
      if (content.get('length') && !selection.length) {
        this.set('selection', content.objectAt(0));
      }
      if (selection.length > 1) {
        this.set('selection', [selection[0]])
      }
      this.set('controller.selectedType', this.get('selection')[0])
    }.observes('content.length', 'selection.length', 'selection.@each')
  })
});
