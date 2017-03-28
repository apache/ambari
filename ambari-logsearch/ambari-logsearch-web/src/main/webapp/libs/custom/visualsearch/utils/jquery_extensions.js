(function() {

var $ = jQuery; // Handle namespaced jQuery

$.fn.extend({

  // Makes the selector enter a mode. Modes have both a 'mode' and a 'group',
  // and are mutually exclusive with any other modes in the same group.
  // Setting will update the view's modes hash, as well as set an HTML class
  // of *[mode]_[group]* on the view's element. Convenient way to swap styles
  // and behavior.
  setMode : function(state, group) {
    group    = group || 'mode';
    var re   = new RegExp("\\w+_" + group + "(\\s|$)", 'g');
    var mode = (state === null) ? "" : state + "_" + group;
    this.each(function() {
      this.className = (this.className.replace(re, '')+' '+mode)
                       .replace(/\s\s/g, ' ');
    });
    return mode;
  },

  // When attached to an input element, this will cause the width of the input
  // to match its contents. This calculates the width of the contents of the input
  // by measuring a hidden shadow div that should match the styling of the input.
  autoGrowInput: function() {
    return this.each(function() {
      var $input  = $(this);
      var $tester = $('<div />').css({
        opacity     : 0,
        top         : -9999,
        left        : -9999,
        position    : 'absolute',
        whiteSpace  : 'nowrap'
      }).addClass('VS-input-width-tester').addClass('VS-interface');

      // Watch for input value changes on all of these events. `resize`
      // event is called explicitly when the input has been changed without
      // a single keypress.
      var events = 'keydown.autogrow keypress.autogrow ' +
                   'resize.autogrow change.autogrow';
      $input.next('.VS-input-width-tester').remove();
      $input.after($tester);
      $input.unbind(events).bind(events, function(e, realEvent) {
        if (realEvent) e = realEvent;
        var value = $input.val();

        // Watching for the backspace key is tricky because it may not
        // actually be deleting the character, but instead the key gets
        // redirected to move the cursor from facet to facet.
        if (VS.app.hotkeys.key(e) == 'backspace') {
          var position = $input.getCursorPosition();
          if (position > 0) value = value.slice(0, position-1) +
                                    value.slice(position, value.length);
        } else if (VS.app.hotkeys.printable(e) &&
                   !VS.app.hotkeys.command) {
          value += String.fromCharCode(e.which);
        }
        value = value.replace(/&/g, '&amp;')
                     .replace(/\s/g,'&nbsp;')
                     .replace(/</g, '&lt;')
                     .replace(/>/g, '&gt;');

        $tester.html(value);

        $input.width($tester.width() + 3 + parseInt($input.css('min-width')));
        $input.trigger('updated.autogrow');
      });

      // Sets the width of the input on initialization.
      $input.trigger('resize.autogrow');
    });
  },


  // Cross-browser method used for calculating where the cursor is in an
  // input field.
  getCursorPosition: function() {
    var position = 0;
    var input    = this.get(0);

    if (document.selection) { // IE
      input.focus();
      var sel    = document.selection.createRange();
      var selLen = document.selection.createRange().text.length;
      sel.moveStart('character', -input.value.length);
      position   = sel.text.length - selLen;
    } else if (input && $(input).is(':visible') &&
               input.selectionStart != null) { // Firefox/Safari
      position = input.selectionStart;
    }

    return position;
  },

  // A simple proxy for `selectRange` that sets the cursor position in an
  // input field.
  setCursorPosition: function(position) {
    return this.each(function() {
      return $(this).selectRange(position, position);
    });
  },

  // Cross-browser way to select text in an input field.
  selectRange: function(start, end) {
    return this.filter(':visible').each(function() {
      if (this.setSelectionRange) { // FF/Webkit
        this.focus();
        this.setSelectionRange(start, end);
      } else if (this.createTextRange) { // IE
        var range = this.createTextRange();
        range.collapse(true);
        range.moveEnd('character', end);
        range.moveStart('character', start);
        if (end - start >= 0) range.select();
      }
    });
  },

  // Returns an object that contains the text selection range values for
  // an input field.
  getSelection: function() {
    var input = this[0];

    if (input.selectionStart != null) { // FF/Webkit
      var start = input.selectionStart;
      var end   = input.selectionEnd;
      return {
        start   : start,
        end     : end,
        length  : end-start,
        text    : input.value.substr(start, end-start)
      };
    } else if (document.selection) { // IE
      var range = document.selection.createRange();
      if (range) {
        var textRange = input.createTextRange();
        var copyRange = textRange.duplicate();
        textRange.moveToBookmark(range.getBookmark());
        copyRange.setEndPoint('EndToStart', textRange);
        var start = copyRange.text.length;
        var end   = start + range.text.length;
        return {
          start   : start,
          end     : end,
          length  : end-start,
          text    : range.text
        };
      }
    }
    return {start: 0, end: 0, length: 0};
  }

});

// Debugging in Internet Explorer. This allows you to use 
// `console.log(['message', var1, var2, ...])`. Just remove the `false` and
// add your console.logs. This will automatically stringify objects using
// `JSON.stringify', so you can read what's going out. Think of this as a
// *Diet Firebug Lite Zero with Lemon*.
if (false) {
  window.console = {};
  var _$ied;
  window.console.log = function(msg) {
    if (_.isArray(msg)) {
      var message = msg[0];
      var vars = _.map(msg.slice(1), function(arg) {
        return JSON.stringify(arg);
      }).join(' - ');
    }
    if(!_$ied){
      _$ied = $('<div><ol></ol></div>').css({
        'position': 'fixed',
        'bottom': 10,
        'left': 10,
        'zIndex': 20000,
        'width': $('body').width() - 80,
        'border': '1px solid #000',
        'padding': '10px',
        'backgroundColor': '#fff',
        'fontFamily': 'arial,helvetica,sans-serif',
        'fontSize': '11px'
      });
      $('body').append(_$ied);
    }
    var $message = $('<li>'+message+' - '+vars+'</li>').css({
      'borderBottom': '1px solid #999999'
    });
    _$ied.find('ol').append($message);
    _.delay(function() {
      $message.fadeOut(500);
    }, 5000);
  };

}

})();
