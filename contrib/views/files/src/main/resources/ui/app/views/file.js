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

App.DeleteBulkView = Em.View.extend({
  actions:{
    ask:function (argument) {
      this.get('controller').toggleProperty('isRemoving');
      return false; 
    }
  }
});

App.FilesView = Em.View.extend({
    templateName: 'files',
    didInsertElement:function () {
      $('.btn-sort').tooltip();
    },
    deleteBulkView:App.DeleteBulkView.create({
      didInsertElement:function(){
        var self = this;
        $('#bulkDropdown').on('hidden.bs.dropdown', function () {
          self.get('controller').set('isRemoving',false);
        })
      },
      templateName:'util/deleteBulk',
      tagName:'li',
      click:function  (e) {
        if (!$(e.target).hasClass('delete')) {
          e.stopPropagation();
        };
      }
    }),
    deleteSingleView: Em.View.extend({
      popoverSelector:'.df-popover',
      actions:{
        close:function () {
          $(this.get('element')).popover('toggle');
        },
        show:function () {
          $(this.get('element')).popover('toggle');
        }
      },
      didInsertElement:function () {
        var self = this,
            element = $(this.get('element'));
        $(element).popover({
          html:true,
          trigger:'manual',
          placement:'left',
          content:function() {
            var content = element.find('.df-popover');
            return content.html();
          }
        });

        $('body').on('click.popover', function (e) {
          if (!element.is(e.target) 
              && element.has(e.target).length === 0 
              && $('.popover').has(e.target).length === 0) {
            element.popover('hide');
          }
        });

        element.on('hidden.bs.popover', function () {
          element.parent().find('.popover').remove();
        });
      },
      willClearRender:function () {
        $('body').off('click.popover');
      },
      templateName:'util/deletePopover',
    }),
    checkboxAll:Em.Checkbox.extend({
      changeBinding:'selectAll',
      checkedBinding:'selectedAll',
      selectedAll:false,
      selectAll:function () {
        var checked = this.get('checked');
        var items = this.get('content');
        return items.forEach(function (item) {
          item.set('selected',checked);
        });
      },
      selection:function () {
        var selected = this.get('content').filterProperty('selected',true);
        if (selected.length == this.get('content.length') && selected.length > 0) {
          this.set('selectedAll',true);
        } else {
          this.set('selectedAll',false);
        }
      }.observes('content.@each.selected'),
    }),
    breadcrumbsView: Ember.CollectionView.extend({
      classNames: ['breadcrumb pull-left'],
      tagName: 'ul',
      content: function (argument) {
        var crumbs = [];
        var currentPath = this.get('controller.path').match(/((?!\/)\S)+/g)||[];
        currentPath.forEach(function (cur,i,array) {
          return crumbs.push({name:cur,path:'/'+array.slice(0,i+1).join('/')});
        });
        crumbs.unshift({name:'/',path:'/'});
        crumbs.get('lastObject').last = 'true';
        return crumbs;
      }.property('controller.path'),
      itemViewClass: Ember.View.extend({
        classNameBindings: ['isActive:active'],
        template: Ember.Handlebars.compile("{{#link-to 'files' (query-params path=view.content.path)}}{{view.content.name}}{{/link-to}}"),
        isActive: function () {
          return this.get('content.last');
        }.property('content')
      })
    }),
    renameInputView: Em.TextField.extend({
      controller:null,
      didInsertElement:function (argument) {
        var element = $(this.get('element'));
        element.focus().val(this.value)
      },
      keyUp: function(e) {
        var target = this.get('targetObject');
        if (e.keyCode==13) {
          return target.send('rename', 'confirm');
        };

        if (e.keyCode==27) {
          return target.send('rename', 'cancel');
        };
      }
    }),
    togglecontext:Em.View.extend({
      didInsertElement:function () {
        var self = this;
        var fileRow = $(this.get('element')).parents('.file-row');
        fileRow.contextmenu({
          target:'#context-menu',
          before: function(e) {
            if (self.get('controller.isMoving')) {
              return false;
            };
            self.get('parentView.contextMenu').set('target',self.get('context'));
            return true;
          },
          onItem:function (t,e) {
            if (e.target.className=='confirm-action') {
              console.log('set waitConfirm onItem');
              self.get('parentView.contextMenu').set('waitConfirm',e.target.dataset.action);
              return false;
            }

            if (e.target.dataset.disabled) {
              return false;
            };
          }
        });
        fileRow.find('[data-toggle=tooltip]').tooltip();
        fileRow.on('click',function(e){
          if($(e.target).is('td') || $(e.target).hasClass('allow-open')){
            self.get('controller').send('open');
          }
        });
      },
      reBind:function(){
        var row = $(this.get('element')).parents('.file-row');
        Em.run.next(function(){
          row.find('[data-toggle=tooltip]').tooltip();
        });

      }.observes('context.isMoving')
    }),
    contextMenu: App.ContextMenu.create(),
    sortArrow:Em.View.extend({
      sortProperty:null,
      asc:true,
      cur:false,
      sorting:function () {
        if (this.get('controller.sortProperties.firstObject')==this.get('sortProperty')) {
          this.set('asc',this.get('controller.sortAscending'));
          this.set('cur',true);
        } else{
          this.set('asc',true);
          this.set('cur',false);
        };
      }.observes('controller.sortProperties','controller.sortAscending').on('init'),
      tagName:'span',
      classNames:['pull-right'],
      template:Ember.Handlebars.compile('<i {{bind-attr class=":fa view.asc:fa-chevron-down:fa-chevron-up view.cur::fa-gr view.cur::fa-rotate-270" }} ></i>')
    }),
    reBind:function(){
      Em.run.next(function(){
        $('.isMoving').find('[data-toggle=tooltip]').tooltip();
      });
    }.observes('controller.hideMoving')
});

App.FilesAlertView = Em.View.extend({
  templateName:'util/errorRow'
});
