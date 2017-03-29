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
 * @file jQuery Dialog (movable and resizable popup) wrapper for use with Backbone.
 *
 * Takes care of instantiation, fires and listen to events,
 * adds several options and removes the element from the DOM when closed.
 *
 * @borrows jquery-ui.js
 * @extends Backbone.View
 *
 * @fires dialog:rendered - Fired when the dialog view is rendered
 * @fires dialog:open - Fired when the dialog is instantiated and opened
 * @fires dialog:closing - Fired when the dialog view is about to close (before closing)
 */
define(['require','backbone'],function(require,Backbone){
  //var App      = require('App');

  /**
   * Creates instance of JBDialog
   */
  var JBDialog = Backbone.View.extend({
    /** @lends JBDialog */
    className: 'ui-dialog-content ui-widget-content',

    tagName: 'div',

    initialize: function(customOpts){
      this.options = _.extend({
        // defaults
        appendTo: "body",
        autoOpen: false,
        buttons: [],
        closeOnEscape: false,
        closeText: "close",
        dialogClass: "",
        draggable: true,
        hide: null,
        height: "auto",
        maxHeight: 600,
        maxWidth: 1000,
        minHeight: 300,
        minWidth: 250,
        modal: false,
        position: {
          my: "center",
          at: "center",
          of: window,
          collision: "fit"
        },
        resizable: true,
        show: null,
        title: null,
        width: "auto",
        autoFocus1stElement : true,
        // callbacks
        // beforeClose: null,
        // close: null,
        // drag: null,
        // dragStart: null,
        // dragStop: null,
        // focus: null,
        // open: null,
        // resize: null,
        // resizeStart: null,
        // resizeStop: null
      }, customOpts);

      this.customOpts = customOpts;

      this.bindButtonEvents();
    },

    bindButtonEvents: function(){
      var options = this.options,
        content = options.content;

      /*Bind button events on main content, if it's a view*/
      if (content && content.$el) {
        this.listenTo(content, "toggle:okBtn", function(isEnable){
          this.toggleButtonState('okBtn',!!isEnable);
        }, this);

        this.listenTo(content, "toggle:cancelBtn", function(isEnable){
          this.toggleButtonState('cancelBtn',!!isEnable);
        }, this);

        this.listenTo(content, "toggle:btn", function(isEnable, buttonId){
          this.toggleButtonState(buttonId,!!isEnable);
        }, this);
      }
    },

    render: function() {
      var self = this;
      var $el = self.$el,
          options = self.options,
          content = options.content;

      /*Create the modal container*/
      if (content && content.$el) {
        /*Insert the main content if it's a view*/
        content.render();
        $el.html(options.content.$el);
      } else {
        $el.html(options.content);
      }

      self.isRendered = true;

      self.trigger("dialog:rendered");

      return self;
    },

    /**
     * [initializing and invoking open function on dialog]
     * @return {Object} context
     */
    open: function() {
      var self = this;
      var $el = self.$el;

      if (!self.isRendered){
        self.render();
      }

      //Create it
      $el.dialog(self.options);
      if (!self.options.autoOpen) {
        $el.dialog("open");
      }
      $('.ui-dialog-titlebar-close').click(function(e){
    	  self.close();  
      });
      if (this.options.autoFocus1stElement)
        $el.find("[autofocus]:first").focus();

      self.trigger("dialog:open");

      return self;
    },

    /**
     * closing the dialog and destroying it from DOM if open
     */
    close: function() {
      var self = this;
      var $el = self.$el;

      self.trigger("dialog:closing");

      $el.hide();

      if (self.options.content) {
        /*Closing Backbone.View*/
        self.options.content.close();
      }
      if($el.dialog("isOpen")){
        $el.dialog("destroy");
      }
    },

    /**
     * toggle particular button state
     * @param  {String}  buttonId - id of the button element
     * @param  {Boolean} isEnable - flag to enable/disable
     * @return {Object} context
     */
    toggleButtonState: function(buttonId, isEnable){
      var $selector, self = this;
      if (buttonId) {
        $selector = self.$el.next().find('#'+buttonId);
        self.enableDisableBtn($selector, isEnable);
      }
      return self;
    },

    /**
     * enable/disable button
     * @param  {Object}  selector - jquery dom element
     * @param  {Boolean} isEnable - flag to enable/disable
     */
    enableDisableBtn: function(selector, isEnable) {
      if (selector && selector.length) {
        if (isEnable) {
          selector.removeAttr('disabled');
        } else {
          selector.attr('disabled', 'disabled');
        }
      }
    }

  });
  return JBDialog;
});