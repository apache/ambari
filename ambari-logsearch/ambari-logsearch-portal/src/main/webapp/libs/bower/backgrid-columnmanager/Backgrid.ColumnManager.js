(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("_"), require("jQuery"), require("Backbone"), require("Backgrid"));
	else if(typeof define === 'function' && define.amd)
		define(["underscore", "jquery", "backbone", "backgrid"], factory);
	else if(typeof exports === 'object')
		exports["Backgrid.Extension.ColumnManager"] = factory(require("_"), require("jQuery"), require("Backbone"), require("Backgrid"));
	else
		root["Backgrid.Extension.ColumnManager"] = factory(root["_"], root["jQuery"], root["Backbone"], root["Backgrid"]);
})(this, function(__WEBPACK_EXTERNAL_MODULE_1__, __WEBPACK_EXTERNAL_MODULE_2__, __WEBPACK_EXTERNAL_MODULE_3__, __WEBPACK_EXTERNAL_MODULE_4__) {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	"use strict";

	/**
	 * A column manager for backgrid
	 *
	 * @module Backgrid.ColumnManager
	 */
	// Dependencies
	var _ = __webpack_require__(1);
	var $ = __webpack_require__(2);
	var Backbone = __webpack_require__(3);
	var Backgrid = __webpack_require__(4);

	/**
	 * Manages visibility of columns.
	 *
	 * @class Backgrid.Extension.ColumnManager ColumnManager
	 * @constructor
	 * @param {Backgrid.Columns} columns
	 * @param {Object} [options]
	 * @param {number} [options.initialColumnCount] Initial amount of columns to show. Default is null (All visible).
	 * @param {boolean} [options.trackSize]
	 * @param {boolean} [options.trackOrder]
	 * @param {boolean} [options.trackVisibility]
	 * @param {string} [options.stateChecking] can be "strict" or "loose".
	 * @param {boolean} [options.saveState]
	 * @param {string} [options.saveStateKey] Storage key. Must be unique for location. Can be left out if this plugin is only used in one place.
	 * @param {string} [options.saveStateLocation] Can be "localStorage" (default) or "sessionStorage" (be aware, session stored values are lost when window is closed)
	 * @param {boolean} [options.loadStateOnInit]
	 * @param {Array} [state]
	 */
	Backgrid.Extension.ColumnManager = function (columns, options, state) {
	  // Bind backbone events
	  _.extend(this, Backbone.Events);

	  // Save options and merge with defaults
	  var defaults = {
	    initialColumnsVisible: null,

	    // State options
	    trackSize: true,
	    trackOrder: true,
	    trackVisibility: true,
	    stateChecking: "strict",
	    saveState: false,
	    saveStateKey: "",
	    saveStateLocation: "localStorage",
	    loadStateOnInit: false
	  };
	  this.options = _.extend({}, defaults, options);
	  this.state = [];

	  // Check if columns is instance of Backgrid.Columns
	  if (columns instanceof Backgrid.Columns) {
	    // Save columns
	    this.columns = columns;

	    // Add columnManager to columns (instance)
	    columns.columnManager = this;
	    this.addManagerToColumns();

	    // Set state if provided
	    var storedState = (this.options.loadStateOnInit) ? this.loadState() : false;
	    if (state && this.checkStateValidity(state)) {
	      this.setState(state, true);
	    }
	    else if (storedState) {
	      this.setState(storedState, true);
	    }
	    else {
	      // If no initial state is provided, adhere to initial column visibility settings
	      this.setInitialColumnVisibility();

	      // Set current state
	      this.setState(this.getStateFromColumns());
	    }

	    // Listen to column events
	    if (this.options.trackVisibility || this.options.trackSize || this.options.trackOrder) {
	      //this.stateUpdateHandler = _.bind(this.stateUpdateHandler, this);
	      var events = "" +
	          ((this.options.trackVisibility) ? "change:renderable " : "") +
	          ((this.options.trackSize) ? "resize " : "") +
	          ((this.options.trackOrder) ? "ordered" : "");
	      this.columns.on(events, _.bind(this.stateUpdateHandler, this));
	    }
	  }
	  else {
	    // Issue warning
	    console.error("Backgrid.ColumnManager: options.columns is not an instance of Backgrid.Columns");
	  }
	};

	/**
	 * Loops over all columns and sets the visibility according to provided options.
	 *
	 * @method setInitialColumnVisibility
	 */
	Backgrid.Extension.ColumnManager.prototype.setInitialColumnVisibility = function () {
	  var self = this;

	  // Loop columns and set renderable property according to settings
	  var initialColumnsVisible = self.options.initialColumnsVisible;

	  if (initialColumnsVisible) {
	    self.columns.each(function (col, index) {
	      col.set("renderable", (col.get("alwaysVisible")) ? true : index < initialColumnsVisible);
	    });
	  }
	};

	/**
	 * Loops over all columns and adds the columnManager instance to VisibilityHeaderCell columns.
	 *
	 * @method addManagerToColumns
	 */
	Backgrid.Extension.ColumnManager.prototype.addManagerToColumns = function () {
	  var self = this;

	  self.columns.each(function (col) {
	    // Look for header cell
	    if (col.get("headerCell") === Backgrid.Extension.ColumnManager.ColumnVisibilityHeaderCell) {
	      col.set("headerCell", col.get("headerCell").extend({
	        columnManager: self
	      }));
	    }

	    if (col.get("headerCell") instanceof Backgrid.Extension.ColumnManager.ColumnVisibilityHeaderCell) {
	      col.get("headerCell").columnManager = self;
	    }
	  });
	};

	/**
	 * Convenience function to retrieve a column either directly or by its id.
	 * Returns false if no column is found.
	 *
	 * @method getColumn
	 * @param {string|number|Backgrid.Column} col
	 * @return {Backgrid.Column|boolean}
	 */
	Backgrid.Extension.ColumnManager.prototype.getColumn = function (col) {
	  // If column is a string or number, try to find a column which has that ID
	  if (_.isNumber(col) || _.isString(col)) {
	    col = this.columns.get(col);
	  }
	  return (col instanceof Backgrid.Column) ? col : false;
	};

	/**
	 * Hides a column
	 *
	 * @method hidecolumn
	 * @param {string|number|Backgrid.Column} col
	 */
	Backgrid.Extension.ColumnManager.prototype.hideColumn = function (col) {
	  // If column is a valid backgrid column, set the renderable property to false
	  var column = this.getColumn(col);
	  if (column) {
	    column.set("renderable", false);
	  }
	};

	/**
	 * Shows a column
	 *
	 * @method showColumn
	 * @param {string|number|Backgrid.Column} col
	 */
	Backgrid.Extension.ColumnManager.prototype.showColumn = function (col) {
	  // If column is a valid backgrid column, set the renderable property to true
	  var column = this.getColumn(col);
	  if (column) {
	    column.set("renderable", true);
	  }
	};

	/**
	 * Toggles a columns' visibility
	 *
	 * @method toggleColumnVisibility
	 * @param {string|number|Backgrid.Column} col
	 */
	Backgrid.Extension.ColumnManager.prototype.toggleColumnVisibility = function (col) {
	  // If column is a valid backgrid column, set the renderable property to true
	  var column = this.getColumn(col);
	  if (column) {
	    if (column.get("renderable")) {
	      this.hideColumn(column);
	    }
	    else {
	      this.showColumn(column);
	    }
	  }
	};

	/**
	 * Returns the managed column collection
	 *
	 * @method getColumnCollection
	 * @return {Backgrid.Columns}
	 */
	Backgrid.Extension.ColumnManager.prototype.getColumnCollection = function () {
	  return this.columns;
	};

	/**
	 *
	 * @method setState
	 * @param {Array} state
	 * @param {boolean} applyState
	 * @return {boolean}
	 */
	Backgrid.Extension.ColumnManager.prototype.setState = function (state, applyState) {
	  var self = this;

	  // Filter state
	  _.filter(state, function(columnState) {
	    if (!_.has(columnState, "name")) {
	      return false;
	    }

	    var column = self.columns.findWhere({
	      name: state.name
	    });

	    return typeof column !== "undefined";
	  });

	  // Check if state is valid
	  if (self.checkStateValidity(state) && state !== self.state) {
	    // Apply and save state
	    self.state = state;
	    self.trigger("state-changed", state);

	    if (applyState) {
	      return self.applyStateToColumns();
	    }
	    else {
	      return self.saveState();
	    }
	  }
	  return false;
	};

	/**
	 * @method getState
	 * @return {Array}
	 */
	Backgrid.Extension.ColumnManager.prototype.getState = function () {
	  return this.state;
	};

	/**
	 *
	 * @method checkStateValidity
	 * @return {boolean}
	 */
	Backgrid.Extension.ColumnManager.prototype.checkStateValidity = function (state) {
	  // Has to be array
	  if (!_.isArray(state) && _.isEmpty(state)) {
	    return false;
	  }

	  function checkValidityColumnState() {
	    return _.every(state, function(column) {
	      var valid = true;

	      // We require a name key
	      if (!_.has(column, "name")) {
	        valid = false;
	      }

	      // If renderable is set, should be boolean
	      if (_.has(column, "renderable")) {
	        if (!_.isBoolean(column.renderable)) {
	          valid = false;
	        }
	      }

	      // If displayOrder is set, should be a number
	      if (_.has(column, "displayOrder")) {
	        if (!_.isNumber(column.displayOrder)) {
	          valid = false;
	        }
	      }

	      // If width is set, should be a number or a string
	      if (_.has(column, "width")) {
	        if (!_.isNumber(column.width) && !_.isString(column.width)) {
	          valid = false;
	        }
	      }

	      return valid;
	    });
	  }

	  // Check if state is valid
	  if (this.options.stateChecking === "loose") {
	    // At least we require 'name' keys in every objec
	    return checkValidityColumnState();
	  }
	  else {
	    // Strict check
	    // Requires same length and valid name keys.
	    if (state.length !== this.columns.length && !checkValidityColumnState()) {
	      return false;
	    }

	    var columnNameKeys = this.columns.map(function (column) {
	      return column.get("name");
	    });

	    var newStateNameKeys = _.map(state, function (column) {
	      return column.name;
	    });

	    return columnNameKeys.sort().toString() === newStateNameKeys.sort().toString();
	  }
	};


	/**
	 *
	 * @method loadState
	 * @return {boolean}
	 */
	Backgrid.Extension.ColumnManager.prototype.loadState = function () {
	  // Get state from storage
	  var state = JSON.parse(this.getStorage().getItem(this.getStorageKey()));
	  if (this.checkStateValidity(state)) {
	    return state;
	  }
	  return false;
	};

	/**
	 *
	 * @method saveState
	 * @param {boolean} [force] Override save settings.
	 * @return {boolean}
	 */
	Backgrid.Extension.ColumnManager.prototype.saveState = function (force) {
	  if (this.options.saveState || force) {
	    this.getStorage().setItem(this.getStorageKey(), JSON.stringify(this.state));
	    this.trigger("state-saved");
	    return true;
	  }
	  return false;
	};

	/**
	 * @method getStorage
	 * @return {boolean|Storage}
	 * @private
	 */
	Backgrid.Extension.ColumnManager.prototype.getStorage = function () {
	  // Check if storage functionality is available
	  if (typeof Storage !== "undefined") {
	    return (this.options.saveStateLocation === "sessionStorage") ? sessionStorage : localStorage;
	  }
	  else {
	    console.error("ColMrg: No storage support detected. State won't be saved.");
	    return false;
	  }
	};

	/**
	 * @method getStorageKey
	 * @return {string}
	 * @private
	 */
	Backgrid.Extension.ColumnManager.prototype.getStorageKey = function () {
	  return (this.options.saveStateKey) ? "backgrid-colmgr-" + this.options.saveStateKey : "backgrid-colmgr";
	};

	/**
	 * @method stateUpdateHandler
	 * @return {boolean}
	 * @private
	 */
	Backgrid.Extension.ColumnManager.prototype.stateUpdateHandler = function () {
	  var state = this.getStateFromColumns();
	  return this.setState(state);
	};

	/**
	 * @method getStateFromColumn
	 * @return {Array}
	 */
	Backgrid.Extension.ColumnManager.prototype.getStateFromColumns = function() {
	  var self = this;

	  // Map state from columns
	  return this.columns.map(function(column) {
	    var columnState = {
	      name: column.get("name")
	    };

	    if (self.options.trackVisibility) {
	      columnState.renderable = column.get("renderable");
	    }
	    if (self.options.trackOrder) {
	      columnState.displayOrder = column.get("displayOrder");
	    }
	    if (self.options.trackSize) {
	      columnState.width = column.get("width");
	    }
	    return columnState;
	  });
	};

	/**
	 * @method applyStateToColumns
	 * @private
	 */
	Backgrid.Extension.ColumnManager.prototype.applyStateToColumns = function () {
	  var self = this;

	  // Loop state
	  var ordered = false;
	  _.each(this.state, function(columnState) {
	    // Find column
	    var column = self.columns.findWhere({
	      name: columnState.name
	    });

	    if (_.has(columnState, "renderable")) {
	      column.set("renderable", columnState.renderable);
	    }
	    if (_.has(columnState, "width")) {
	      var oldWidth = column.get("width");
	      column.set("width", columnState.width, {silent: true});
	      if (oldWidth !== columnState.width) {
	        column.trigger("resize", column, columnState.width, oldWidth);
	      }
	    }

	    if (_.has(columnState, "displayOrder")) {
	      if (columnState.displayOrder !== column.get("displayOrder")) {
	        ordered = true;
	      }
	      column.set("displayOrder", columnState.displayOrder, {silent: true});
	    }
	  });

	  if (ordered) {
	    self.columns.sort();
	    self.columns.trigger("ordered");
	  }
	};

	//////////////////////////////////////////////
	/////////////// UI Controls //////////////////
	//////////////////////////////////////////////

	/**
	 * A dropdown item view
	 *
	 * @class DropDownItemView
	 * @extends Backbone.View
	 */
	var DropDownItemView = Backbone.View.extend({
	  className: "columnmanager-dropdown-item",
	  tagName: "li",

	  /**
	   * @method initialize
	   * @param {object} opts
	   * @param {Backgrid.Extension.ColumnManager} opts.columnManager ColumnManager instance.
	   * @param {Backgrid.Column} opts.column A backgrid column.
	   */
	  initialize: function (opts) {
	    this.columnManager = opts.columnManager;
	    this.column = opts.column;
	    this.template = opts.template;

	    _.bindAll(this, "render", "toggleVisibility");
	    this.column.on("change:renderable", this.render, this);
	    this.el.addEventListener("click", this.toggleVisibility, true);
	  },

	  /**
	   * @method render
	   * @return {DropDownItemView}
	   */
	  render: function () {
	    this.$el.empty();

	    this.$el.append(this.template({
	      label: this.column.get("label")
	    }));

	    if (this.column.get("renderable")) {
	      this.$el.addClass((this.column.get("renderable")) ? "visible" : null);
	    }
	    else {
	      this.$el.removeClass("visible");
	    }

	    return this;
	  },

	  /**
	   * Toggles visibility of column.
	   *
	   * @method toggleVisibility
	   * @param {object} e
	   */
	  toggleVisibility: function (e) {
	    if (e) {
	      this.stopPropagation(e);
	    }
	    this.columnManager.toggleColumnVisibility(this.column);
	  },

	  /**
	   * Convenience function to stop event propagation.
	   *
	   * @method stopPropagation
	   * @param {object} e
	   * @private
	   */
	  stopPropagation: function (e) {
	    e.stopPropagation();
	    e.stopImmediatePropagation();
	    e.preventDefault();
	  }
	});


	/**
	 * Dropdown view container.
	 *
	 * @class DropDownView
	 * @extends Backbone.view
	 */
	var DropDownView = Backbone.View.extend({
	  /**
	   * @property className
	   * @type String
	   * @default "columnmanager-dropdown-container"
	   */
	  className: "columnmanager-dropdown-container",

	  /**
	   * @method initialize
	   * @param {object} opts
	   * @param {Backgrid.Extension.ColumnManager} opts.columnManager ColumnManager instance.
	   * @param {Backbone.View} opts.DropdownItemView View to be used for the items.
	   * @param {Function} opts.dropdownItemTemplate
	   */
	  initialize: function (opts) {
	    this.options = opts;
	    this.columnManager = opts.columnManager;
	    this.ItemView = (opts.DropdownItemView instanceof Backbone.View) ? opts.DropdownItemView : DropDownItemView;
	    this.$dropdownButton = opts.$dropdownButton;

	    this.on("dropdown:opened", this.open, this);
	    this.on("dropdown:closed", this.close, this);
	    this.columnManager.columns.on("add remove", this.render, this);
	  },

	  /**
	   * @method render
	   * @return {DropDownView}
	   */
	  render: function () {
	    var view = this;
	    view.$el.empty();

	    // List all columns
	    this.columnManager.columns.each(function (col) {
	      if (!col.get("alwaysVisible")) {
	        view.$el.append(new view.ItemView({
	          column: col,
	          columnManager: view.columnManager,
	          template: view.options.dropdownItemTemplate
	        }).render().el);
	      }
	    });

	    return this;
	  },

	  /**
	   * Opens the dropdown.
	   *
	   * @method open
	   */
	  open: function () {
	    this.$el.addClass("open");

	    // Get button
	    var $button = this.$dropdownButton;

	    // Align
	    var align;
	    if (this.options.align === "auto") {
	      // Determine what alignment fits
	      var viewPortWidth = document.body.clientWidth || document.body.clientWidth;
	      align = (($button.offset().left + this.$el.outerWidth()) > viewPortWidth) ? "left" : "right";
	    }
	    else {
	      align = (this.options.align === "left" || this.options.align === "right") ?
	        (this.options.align === "right" ? "right" : "left") : "right";
	    }

	    var offset;
	    if (align === "left") {
	      // Align right by default
	      offset = $button.offset().left + $button.outerWidth() - this.$el.outerWidth();
	      this.$el.css("left", offset + "px");
	    }
	    else {
	      offset = $button.offset().left;
	      this.$el.css("left", offset + "px");
	    }

	    // Height position
	    var offsetHeight = $button.offset().top + $button.outerHeight();
	    this.$el.css("top", offsetHeight + "px");
	  },

	  /**
	   * Closes the dropdown.
	   *
	   * @method close
	   */
	  close: function () {
	    this.$el.removeClass("open");
	  }
	});

	/**
	 * UI control which manages visibility of columns.
	 * Inspired by: https://github.com/kjantzer/backbonejs-dropdown-view.
	 *
	 * @class Backgrid.Extension.ColumnManagerVisibilityControl
	 * @extends Backbone.View
	 */
	Backgrid.Extension.ColumnManagerVisibilityControl = Backbone.View.extend({
	  /**
	   * @property tagName
	   * @type String
	   * @default "div"
	   */
	  tagName: "div",

	  /**
	   * @property className
	   * @type String
	   * @default "columnmanager-visibilitycontrol"
	   */
	  className: "columnmanager-visibilitycontrol",

	  /**
	   * @property defaultEvents
	   * @type Object
	   */
	  defaultEvents: {
	    "click": "stopPropagation"
	  },

	  /**
	   * @property defaultOpts
	   * @type Object
	   */
	  defaultOpts: {
	    width: null,
	    closeOnEsc: true,
	    closeOnClick: true,
	    openOnInit: false,
	    columnManager: null,

	    // Button
	    buttonTemplate: _.template("<button class='dropdown-button'>...</button>"),

	    // Container
	    DropdownView: DropDownView,
	    dropdownAlign: "auto",

	    // Item view
	    DropdownItemView: DropDownItemView,
	    dropdownItemTemplate: _.template("<span class='indicator'></span><span class='column-label'><%= label %></span>")
	  },

	  /**
	   * @method initialize
	   * @param {Object} opts
	   * @param {Backgrid.Extension.ColumnManager} opts.columnManager ColumnManager instance
	   */
	  initialize: function (opts) {
	    this.options = _.extend({}, this.defaultOpts, opts);
	    this.events = _.extend({}, this.defaultEvents, this.events || {});
	    this.columnManager = opts.columnManager;

	    // Option checking
	    if (!this.columnManager instanceof Backgrid.Extension.ColumnManager) {
	      console.error("Backgrid.ColumnManager: options.columns is not an instance of Backgrid.Columns");
	    }

	    // Bind scope to events
	    _.bindAll(this, "deferClose", "stopDeferClose", "closeOnEsc", "toggle", "render");

	    // UI events
	    document.body.addEventListener("click", this.deferClose, true);
	    this.el.addEventListener("click", this.stopDeferClose, true);
	    if (this.options.closeOnEsc) {
	      document.body.addEventListener("keyup", this.closeOnEsc, false);
	    }
	    this.el.addEventListener("click", this.toggle, false);

	    // Create elements
	    this.setup();

	    // Listen for dropdown view events indicating to open and/or close
	    this.view.on("dropdown:close", this.close, this);
	    this.view.on("dropdown:open", this.open, this);
	  },

	  /**
	   * @method delayStart
	   * @private
	   */
	  delayStart: function () {
	    clearTimeout(this.closeTimeout);
	    this.delayTimeout = setTimeout(this.open.bind(this), this.options.delay);
	  },

	  /**
	   * @method delayEnd
	   * @private
	   */
	  delayEnd: function () {
	    clearTimeout(this.delayTimeout);
	    this.closeTimeout = setTimeout(this.close.bind(this), 300);
	  },

	  /**
	   * @method setup
	   * @private
	   */
	  setup: function () {
	    // Override element width
	    if (this.options.width) {
	      this.$el.width(this.options.width + "px");
	    }

	    // Create button element
	    this.$dropdownButton = $(this.options.buttonTemplate());

	    var viewOptions = {
	      columnManager: this.columnManager,
	      DropdownItemView: this.options.DropdownItemView,
	      dropdownItemTemplate: this.options.dropdownItemTemplate,
	      align: this.options.dropdownAlign,
	      $dropdownButton: this.$dropdownButton
	    };

	    // Check if a different childView has been provided, if not, use default dropdown view
	    this.view = (this.options.DropdownView instanceof Backbone.View) ?
	      new this.options.DropdownView(viewOptions) :
	      new DropDownView(viewOptions);
	  },

	  /**
	   * @method setup
	   */
	  render: function () {
	    this.$el.empty();

	    // Render button
	    this.$el.append(this.$dropdownButton);

	    // Render inner view
	    this.view.render(); // tell the inner view to render itself
	    $(document.body).append(this.view.el);
	    return this;
	  },

	  /**
	   * Convenience function to stop event propagation
	   *
	   * @method stopPropagation
	   * @param {object} e
	   * @private
	   */
	  stopPropagation: function (e) {
	    e.stopPropagation();
	    e.stopImmediatePropagation();
	    e.preventDefault();
	  },

	  /**
	   * Toggle the dropdown visibility
	   *
	   * @method toggle
	   * @param {object} [e]
	   */
	  toggle: function (e) {
	    if (this.isOpen !== true) {
	      this.open(e);
	    }
	    else {
	      this.close(e);
	    }
	  },

	  /**
	   * Open the dropdown
	   *
	   * @method open
	   * @param {object} [e]
	   */
	  open: function (e) {
	    clearTimeout(this.closeTimeout);
	    clearTimeout(this.deferCloseTimeout);

	    if (e) {
	      if (e.stopPropagation) {
	        e.stopPropagation();
	      }
	      if (e.preventDefault) {
	        e.preventDefault();
	      }
	      e.cancelBubble = true;
	    }

	    // Don't do anything if already open
	    if (this.isOpen) {
	      return;
	    }

	    this.isOpen = true;
	    this.$el.addClass("open");
	    this.trigger("dropdown:opened");

	    // Notify child view
	    this.view.trigger("dropdown:opened");
	  },

	  /**
	   * Close the dropdown
	   *
	   * @method close
	   * @param {object} [e]
	   */
	  close: function (e) {
	    // Don't do anything if already closed
	    if (!this.isOpen) {
	      return;
	    }

	    this.isOpen = false;
	    this.$el.removeClass("open");
	    this.trigger("dropdown:closed");

	    // Notify child view
	    this.view.trigger("dropdown:closed");
	  },

	  /**
	   * Close the dropdown on esc
	   *
	   * @method closeOnEsc
	   * @param {object} e
	   * @private
	   */
	  closeOnEsc: function (e) {
	    if (e.which === 27) {
	      this.deferClose();
	    }
	  },

	  /**
	   * @method deferClose
	   * @private
	   */
	  deferClose: function () {
	    this.deferCloseTimeout = setTimeout(this.close.bind(this), 0);
	  },

	  /**
	   * @method stopDeferClose
	   * @private
	   */
	  stopDeferClose: function (e) {
	    clearTimeout(this.deferCloseTimeout);
	  },

	  /**
	   * Clean up this control
	   *
	   * @method remove
	   * @chainable
	   */
	  remove: function () {
	    // Remove event listeners
	    document.body.removeEventListener("click", this.deferClose);
	    this.el.removeEventListener("click", this.stopDeferClose);
	    if (this.options.closeOnEsc) {
	      document.body.removeEventListener("keyup", this.closeOnEsc);
	    }
	    this.el.removeEventListener("click", this.toggle);

	    // Remove DOM element
	    $(this.view.el).remove();

	    // Invoke original backbone methods
	    return Backbone.View.prototype.remove.apply(this, arguments);
	  }
	});

	/**
	 * Backgrid HeaderCell containing ColumnManagerVisibilityControl
	 *
	 * @class Backgrid.Extension.ColumnVisibilityHeaderCell
	 * @extends Backgrid.HeaderCell
	 */

	Backgrid.Extension.ColumnManager.ColumnVisibilityHeaderCell = Backgrid.HeaderCell.extend({
	  initialize: function (options) {
	    Backgrid.HeaderCell.prototype.initialize.apply(this, arguments);

	    // Add class
	    this.$el.addClass(this.column.get("name"));
	  },
	  render: function () {
	    this.$el.empty();

	    // Add control
	    var colVisibilityControl = this.colVisibilityControl = new Backgrid.Extension.ColumnManagerVisibilityControl({
	      columnManager: this.columnManager
	    });

	    // Add to header
	    this.$el.html(colVisibilityControl.render().el);

	    this.delegateEvents();
	    return this;
	  },

	  /**
	   * Clean up this cell.
	   *
	   * @method remove
	   * @chainable
	   */
	  remove: function () {
	    // Remove UI control
	    this.colVisibilityControl.remove();

	    // Invoke super
	    /*eslint no-underscore-dangle:0*/
	    return Backgrid.HeaderCell.__super__.remove.apply(this, arguments);
	  }
	});


/***/ },
/* 1 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_1__;

/***/ },
/* 2 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_2__;

/***/ },
/* 3 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_3__;

/***/ },
/* 4 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_4__;

/***/ }
/******/ ])
});
;