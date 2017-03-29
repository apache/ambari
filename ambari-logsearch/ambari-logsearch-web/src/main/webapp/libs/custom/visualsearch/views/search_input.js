(function() {

var $ = jQuery; // Handle namespaced jQuery

// This is the visual search input that is responsible for creating new facets.
// There is one input placed in between all facets.
VS.ui.SearchInput = Backbone.View.extend({

  type : 'text',

  className : 'search_input ui-menu',

  events : {
    'keypress input'  : 'keypress',
    'keydown input'   : 'keydown',
    'keyup input'     : 'keyup',
    'click input'     : 'maybeTripleClick',
    'dblclick input'  : 'startTripleClickTimer'
  },

  initialize : function(options) {
    this.options = _.extend({}, this.options, options);
    
    this.app = this.options.app;
    this.flags = {
      canClose : false
    };
    _.bindAll(this, 'removeFocus', 'addFocus', 'moveAutocomplete', 'deferDisableEdit');
  },

  // Rendering the input sets up autocomplete, events on focusing and blurring
  // the input, and the auto-grow of the input.
  render : function() {
    $(this.el).html(JST['search_input']({
      readOnly: this.app.options.readOnly
    }));

    this.setMode('not', 'editing');
    this.setMode('not', 'selected');
    this.box = this.$('input');
    this.box.autoGrowInput();
    this.box.bind('updated.autogrow', this.moveAutocomplete);
    this.box.bind('blur',  this.deferDisableEdit);
    this.box.bind('focus', this.addFocus);
    this.setupAutocomplete();

    return this;
  },

  // Watches the input and presents an autocompleted menu, taking the
  // remainder of the input field and adding a separate facet for it.
  //
  // See `addTextFacetRemainder` for explanation on how the remainder works.
  setupAutocomplete : function() {
    this.box.autocomplete({
      minLength : this.options.showFacets ? 0 : 1,
      delay     : 50,
      autoFocus : true,
      position  : {offset : "0 -1"},
      source    : _.bind(this.autocompleteValues, this),
      // Prevent changing the input value on focus of an option
      focus     : function() { return false; },
      create    : _.bind(function(e, ui) {
        $(this.el).find('.ui-autocomplete-input').css('z-index','auto');
      }, this),
      select    : _.bind(function(e, ui) {
        e.preventDefault();
        // stopPropogation does weird things in jquery-ui 1.9
        // e.stopPropagation();
        var remainder = this.addTextFacetRemainder(ui.item.label || ui.item.value);
        var position  = this.options.position + (remainder ? 1 : 0);
        this.app.searchBox.addFacet(ui.item instanceof String ? ui.item : ui.item.value, '', position);
        return false;
      }, this)
    });

    // Renders the results grouped by the categories they belong to.
    this.box.data('ui-autocomplete')._renderMenu = function(ul, items) {
      var category = '';
      _.each(items, _.bind(function(item, i) {
        if (item.category && item.category != category) {
          ul.append('<li class="ui-autocomplete-category">'+item.category+'</li>');
          category = item.category;
        }

        if(this._renderItemData) {
          this._renderItemData(ul, item);
        } else {
          this._renderItem(ul, item);
        }

      }, this));
    };

    this.box.autocomplete('widget').addClass('VS-interface');
  },

  // Search terms used in the autocomplete menu. The values are matched on the
  // first letter of any word in matches, and finally sorted according to the
  // value's own category. You can pass `preserveOrder` as an option in the
  // `facetMatches` callback to skip any further ordering done client-side.
  autocompleteValues : function(req, resp) {
    var searchTerm = req.term;
    var lastWord   = searchTerm.match(/\w+\*?$/); // Autocomplete only last word.
    var re         = VS.utils.inflector.escapeRegExp(lastWord && lastWord[0] || '');
    this.app.options.callbacks.facetMatches(function(prefixes, options) {
      options = options || {};
      prefixes = prefixes || [];

      // Only match from the beginning of the word.
      var matcher    = new RegExp('^' + re, 'i');
      var matches    = $.grep(prefixes, function(item) {
        return item && matcher.test(item.label || item);
      });

      if (options.preserveOrder) {
        resp(matches);
      } else {
        resp(_.sortBy(matches, function(match) {
          if (match.label) return match.category + '-' + match.label;
          else             return match;
        }));
      }
    });

  },

  // Closes the autocomplete menu. Called on disabling, selecting, deselecting,
  // and anything else that takes focus out of the facet's input field.
  closeAutocomplete : function() {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) autocomplete.close();
  },

  // As the input field grows, it may move to the next line in the
  // search box. `autoGrowInput` triggers an `updated` event on the input
  // field, which is bound to this method to move the autocomplete menu.
  moveAutocomplete : function() {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) {
      autocomplete.menu.element.position({
        my        : "left top",
        at        : "left bottom",
        of        : this.box.data('ui-autocomplete').element,
        collision : "none",
        offset    : '0 -1'
      });
    }
  },

  // When a user enters a facet and it is being edited, immediately show
  // the autocomplete menu and size it to match the contents.
  searchAutocomplete : function(e) {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) {
      var menu = autocomplete.menu.element;
      autocomplete.search();

      // Resize the menu based on the correctly measured width of what's bigger:
      // the menu's original size or the menu items' new size.
      menu.outerWidth(Math.max(
        menu.width('').outerWidth(),
        autocomplete.element.outerWidth()
      ));
    }
  },

  // If a user searches for "word word category", the category would be
  // matched and autocompleted, and when selected, the "word word" would
  // also be caught as the remainder and then added in its own facet.
  addTextFacetRemainder : function(facetValue) {
    var boxValue = this.box.val();
    var lastWord = boxValue.match(/\b(\w+)$/);

    if (!lastWord) {
      return '';
    }

    var matcher = new RegExp(lastWord[0], "i");
    if (facetValue.search(matcher) == 0) {
      boxValue = boxValue.replace(/\b(\w+)$/, '');
    }
    boxValue = boxValue.replace('^\s+|\s+$', '');

    if (boxValue) {
      this.app.searchBox.addFacet(this.app.options.remainder, boxValue, this.options.position);
    }

    return boxValue;
  },

  // Directly called to focus the input. This is different from `addFocus`
  // because this is not called by a focus event. This instead calls a
  // focus event causing the input to become focused.
  enableEdit : function(selectText) {
    this.addFocus();
    if (selectText) {
      this.selectText();
    }
    this.box.focus();
  },

  // Event called on user focus on the input. Tells all other input and facets
  // to give up focus, and starts revving the autocomplete.
  addFocus : function() {
    this.flags.canClose = false;
    if (!this.app.searchBox.allSelected()) {
      this.app.searchBox.disableFacets(this);
    }
    this.app.searchBox.addFocus();
    this.setMode('is', 'editing');
    this.setMode('not', 'selected');
    if (!this.app.searchBox.allSelected()) {
        this.searchAutocomplete();
    }
  },

  // Directly called to blur the input. This is different from `removeFocus`
  // because this is not called by a blur event.
  disableEdit : function() {
    this.box.blur();
    this.removeFocus();
  },

  // Event called when user blur's the input, either through the keyboard tabbing
  // away or the mouse clicking off. Cleans up
  removeFocus : function() {
    this.flags.canClose = false;
    this.app.searchBox.removeFocus();
    this.setMode('not', 'editing');
    this.setMode('not', 'selected');
    this.closeAutocomplete();
  },

  // When the user blurs the input, they may either be going to another input
  // or off the search box entirely. If they go to another input, this facet
  // will be instantly disabled, and the canClose flag will be turned back off.
  //
  // However, if the user clicks elsewhere on the page, this method starts a timer
  // that checks if any of the other inputs are selected or are being edited. If
  // not, then it can finally close itself and its autocomplete menu.
  deferDisableEdit : function() {
    this.flags.canClose = true;
    _.delay(_.bind(function() {
      if (this.flags.canClose &&
          !this.box.is(':focus') &&
          this.modes.editing == 'is') {
        this.disableEdit();
      }
    }, this), 250);
  },

  // Starts a timer that will cause a triple-click, which highlights all facets.
  startTripleClickTimer : function() {
    this.tripleClickTimer = setTimeout(_.bind(function() {
      this.tripleClickTimer = null;
    }, this), 500);
  },

  // Event on click that checks if a triple click is in play. The
  // `tripleClickTimer` is counting down, ready to be engaged and intercept
  // the click event to force a select all instead.
  maybeTripleClick : function(e) {
    if (this.app.options.readOnly) return;
    if (!!this.tripleClickTimer) {
      e.preventDefault();
      this.app.searchBox.selectAllFacets();
      return false;
    }
  },

  // Is the user currently focused in the input field?
  isFocused : function() {
    return this.box.is(':focus');
  },

  // When serializing the facets, the inputs need to also have their values represented,
  // in case they contain text that is not yet faceted (but will be once the search is
  // completed).
  value : function() {
    return this.box.val();
  },

  // When switching between facets and inputs, depending on the direction the cursor
  // is coming from, the cursor in this facet's input field should match the original
  // direction.
  setCursorAtEnd : function(direction) {
    if (direction == -1) {
      this.box.setCursorPosition(this.box.val().length);
    } else {
      this.box.setCursorPosition(0);
    }
  },

  // Selects the entire range of text in the input. Useful when tabbing between inputs
  // and facets.
  selectText : function() {
    this.box.selectRange(0, this.box.val().length);
    if (!this.app.searchBox.allSelected()) {
      this.box.focus();
    } else {
      this.setMode('is', 'selected');
    }
  },

  // Before the searchBox performs a search, we need to close the
  // autocomplete menu.
  search : function(e, direction) {
    if (!direction) direction = 0;
    this.closeAutocomplete();
    this.app.searchBox.searchEvent(e);
    _.defer(_.bind(function() {
      this.app.searchBox.focusNextFacet(this, direction);
    }, this));
  },

  // Callback fired on key press in the search box. We search when they hit return.
  keypress : function(e) {
    var key = VS.app.hotkeys.key(e);

    if (key == 'enter') {
      return this.search(e, 100);
    } else if (VS.app.hotkeys.colon(e)) {
      this.box.trigger('resize.autogrow', e);
      var query    = this.box.val();
      var prefixes = [];
      this.app.options.callbacks.facetMatches(function(p) {
          prefixes = p;
      });
      var labels   = _.map(prefixes, function(prefix) {
        if (prefix.label) return prefix.label;
        else              return prefix;
      });
      if (_.contains(labels, query)) {
        e.preventDefault();
        var remainder = this.addTextFacetRemainder(query);
        var position  = this.options.position + (remainder?1:0);
        this.app.searchBox.addFacet(query, '', position);
        return false;
      }
    } else if (key == 'backspace') {
      if (this.box.getCursorPosition() == 0 && !this.box.getSelection().length) {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        this.app.searchBox.resizeFacets();
        return false;
      }
    }
  },

  // Handles all keyboard inputs when in the input field. This checks
  // for movement between facets and inputs, entering a new value that needs
  // to be autocompleted, as well as stepping between facets with backspace.
  keydown : function(e) {
    var key = VS.app.hotkeys.key(e);

    if (key == 'left') {
      if (this.box.getCursorPosition() == 0) {
        e.preventDefault();
        this.app.searchBox.focusNextFacet(this, -1, {startAtEnd: -1});
      }
    } else if (key == 'right') {
      if (this.box.getCursorPosition() == this.box.val().length) {
        e.preventDefault();
        this.app.searchBox.focusNextFacet(this, 1, {selectFacet: true});
      }
    } else if (VS.app.hotkeys.shift && key == 'tab') {
      e.preventDefault();
      this.app.searchBox.focusNextFacet(this, -1, {selectText: true});
    } else if (key == 'tab') {
      var value = this.box.val();
      if (value.length) {
        e.preventDefault();
        var remainder = this.addTextFacetRemainder(value);
        var position  = this.options.position + (remainder?1:0);
        if (value != remainder) {
            this.app.searchBox.addFacet(value, '', position);
        }
      } else {
        var foundFacet = this.app.searchBox.focusNextFacet(this, 0, {
          skipToFacet: true,
          selectText: true
        });
        if (foundFacet) {
          e.preventDefault();
        }
      }
    } else if (VS.app.hotkeys.command &&
               String.fromCharCode(e.which).toLowerCase() == 'a') {
      e.preventDefault();
      this.app.searchBox.selectAllFacets();
      return false;
    } else if (key == 'backspace' && !this.app.searchBox.allSelected()) {
      if (this.box.getCursorPosition() == 0 && !this.box.getSelection().length) {
        e.preventDefault();
        this.app.searchBox.focusNextFacet(this, -1, {backspace: true});
        return false;
      }
    } else if (key == 'end') {
      var view = this.app.searchBox.inputViews[this.app.searchBox.inputViews.length-1];
      view.setCursorAtEnd(-1);
    } else if (key == 'home') {
      var view = this.app.searchBox.inputViews[0];
      view.setCursorAtEnd(-1);
    }

  },

  // We should get the value of an input should be done
  // on keyup since keydown gets the previous value and not the current one
  keyup : function(e) {
    this.box.trigger('resize.autogrow', e);
  }

});

})();
