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

import Ember from 'ember';

export default Ember.Component.extend({

  classNames: ['multiple-database-search', 'clearfix'],

  databases: [],

  //will make use of these in templates
  heading: 'database',
  subHeading: 'Select or search database/schema',


  selectedDatabase: Ember.computed('selectedMultiDb', function() {
    // return this.get('databases').findBy('selected', true) || {'name': "default"};
    //return  {'name': "default"};
    return  this.get('selectedMultiDb');
  }),

  filteredDatabases: Ember.computed('filterText', 'databases.@each', function() {
    return this.get('databases').filter((item) => {
        return item.get('name');
  });
  }),

  resetDatabaseSelection() {
    this.get('databases').forEach(x => {
      if (x.get('selected')) {
      x.set('selected', false);
    }
  });
  },

  allDbs: Ember.computed('selectedMultiDb','filteredDatabases', function() {
    let dblist =[];
    this.get('filteredDatabases').forEach(db => {
      dblist.push(db.get('name'));
  });

    return dblist;
  }),

  selectedDbs: Ember.computed('selectedMultiDb','filteredDatabases', function() {
    let selecteddblist =[];
    this.get('selectedMultiDb').forEach((item => {
        selecteddblist.push(item.name);
  }));
    return selecteddblist;
  }),

  focusComesFromOutside(e){
    let blurredEl = e.relatedTarget;
    return !blurredEl || !blurredEl.classList.contains('ember-power-select-search-input');
  },


  actions: {
    createOnEnter(select, e) {
      if (e.keyCode === 13 && select.isOpen &&
        !select.highlighted && !Ember.isBlank(select.searchText)) {

        let selected = this.get('selectedDbs');
        if (!selected.includes(select.searchText)) {
          this.get('options').pushObject(select.searchText);
          select.actions.choose(select.searchText);
        }
      }
    },

    handleFocus(select, e) {
      if (this.focusComesFromOutside(e)) {
        select.actions.open();
        this.$('.browse').addClass('open');
      }

    },

    handleBlur() {
      //console.log('handleBlur');
    },

    updateTables(){
      this.sendAction('changeDbHandler', this.get('selectedDbs'));
    },

    browse(){

      if(this.$('.browse').hasClass('open')){
        this.$('.browse').removeClass('open');
        this.$('.multiple-db-select input').focusout();
      } else {
        this.$('.browse').addClass('open');
        this.$('.multiple-db-select input').focus();
      }

    }
  }

});
