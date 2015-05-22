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

define(['require',
        'utils/Globals',
        'utils/Utils',
        'backgrid',
        'bootstrap.filestyle',
        'backbone.forms'], function (require, Globals, Utils) {
  'use strict';

  /**********************************************************************
   *                      Backgrid related                              *
   **********************************************************************/

  /*
   * HtmlCell renders any html code
   * @class Backgrid.HtmlCell
   * @extends Backgrid.Cell
  */
  var HtmlCell = Backgrid.HtmlCell = Backgrid.Cell.extend({

     /** @property */
     className: "html-cell",

     render: function () {
         this.$el.empty();
         var rawValue = this.model.get(this.column.get("name"));
         var formattedValue = this.formatter.fromRaw(rawValue, this.model);
         this.$el.append(formattedValue);
         this.delegateEvents();
         return this;
     }
  });

  var UriCell = Backgrid.UriCell = Backgrid.Cell.extend({
    className: "uri-cell",
    title: null,
    target: "_blank",

    initialize: function (options) {
      UriCell.__super__.initialize.apply(this, arguments);
      this.title = options.title || this.title;
      this.target = options.target || this.target;
    },

    render: function () {
      this.$el.empty();
      var rawValue = this.model.get(this.column.get("name"));
      var href = _.isFunction(this.column.get("href")) ? this.column.get('href')(this.model) : this.column.get('href');
      var klass = this.column.get("klass");
      var formattedValue = this.formatter.fromRaw(rawValue, this.model);
      this.$el.append($("<a>", {
        tabIndex: -1,
        href: href,
        title: this.title || formattedValue,
        'class' : klass
      }).text(formattedValue));

      if(this.column.has("iconKlass")){
        var iconKlass = this.column.get("iconKlass");
        var iconTitle = this.column.get("iconTitle");
        this.$el.find('a').append('<i class="'+iconKlass+'" title="'+iconTitle+'"></i>');
      }
      this.delegateEvents();
      return this;
    }

  });


  /**
     Renders a checkbox for Provision Table Cell.
     @class Backgrid.CheckboxCell
     @extends Backgrid.Cell
  */
  Backgrid.CheckboxCell = Backgrid.Cell.extend({

    /** @property */
    className: "select-cell",

    /** @property */
    tagName: "td",

    /** @property */
    events: {
      "change input[type=checkbox]": "onChange",
      "click input[type=checkbox]": "enterEditMode"
    },

    /**
       Initializer. If the underlying model triggers a `select` event, this cell
       will change its checked value according to the event's `selected` value.

       @param {Object} options
       @param {Backgrid.Column} options.column
       @param {Backbone.Model} options.model
    */
    initialize: function (options) {

      this.column = options.column;
      if (!(this.column instanceof Backgrid.Column)) {
        this.column = new Backgrid.Column(this.column);
      }

      if(!this.column.has("checkedVal")){
        this.column.set("checkedVal", "true"); // it is not a boolean value for EPM
        this.column.set("uncheckedVal", "false");
      }

      var column = this.column, model = this.model, $el = this.$el;
      this.listenTo(column, "change:renderable", function (column, renderable) {
        $el.toggleClass("renderable", renderable);
      });

      if (Backgrid.callByNeed(column.renderable(), column, model)){
        $el.addClass("renderable");
      }

      this.listenTo(model, "change:" + column.get("name"), function () {
        if (!$el.hasClass("editor")){
          this.render();
        }
      });

      this.listenTo(model, "backgrid:select", function (model, selected) {
        this.$el.find("input[type=checkbox]").prop("checked", selected).change();
      });


    },

    /**
       Focuses the checkbox.
    */
    enterEditMode: function () {
      this.$el.find("input[type=checkbox]").focus();
    },

    /**
       Unfocuses the checkbox.
    */
    exitEditMode: function () {
      this.$el.find("input[type=checkbox]").blur();
    },

    /**
       When the checkbox's value changes, this method will trigger a Backbone
       `backgrid:selected` event with a reference of the model and the
       checkbox's `checked` value.
    */
    onChange: function () {
      var checked = this.$el.find("input[type=checkbox]").prop("checked");
      this.model.set(this.column.get("name"), checked);
      this.model.trigger("backgrid:selected", this.model, checked);
    },

    /**
       Renders a checkbox in a table cell.
    */
    render: function () {
      var model = this.model, column = this.column;
      var val = (model.get(column.get("name")) === column.get("checkedVal") || model.get(column.get("name")) === true) ? true : false;
      var editable = Backgrid.callByNeed(column.editable(), column, model);

      this.$el.empty();

      this.$el.append($("<input>", {
        tabIndex: -1,
        type: "checkbox",
        checked: val,
        disabled: !editable
      }));
      this.delegateEvents();
      return this;
    }

  });

  Backbone.Form.editors.Fileupload = Backbone.Form.editors.Base.extend({
    initialize: function(options){
      Backbone.Form.editors.Base.prototype.initialize.call(this, options);
      this.template = _.template('<input type="file" name="fileInput" class="filestyle">');
    },
    render: function(){
      this.$el.html( this.template );
      this.$(":file").filestyle();
      return this;
    },
    getValue: function(){
      return $('input[name="fileInput"]')[0].files[0];
    }
  });

  Backbone.ajax = function() {
    var urlPart = arguments[0].url.split('url=')[0];
    var stormUrlPart = arguments[0].url.split('url=')[1];
    urlPart += 'url=' + encodeURIComponent(stormUrlPart);
    arguments[0].url = urlPart;
    return Backbone.$.ajax.apply(Backbone.$, arguments);
  };

});
