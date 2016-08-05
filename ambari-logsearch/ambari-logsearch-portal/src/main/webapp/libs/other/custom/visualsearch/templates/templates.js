(function(){
window.JST = window.JST || {};

window.JST['search_box'] = _.template('<div class="VS-search <% if (readOnly) { %>VS-readonly<% } %>">\n  <div class="VS-search-box-wrapper VS-search-box">\n    <div class="VS-icon VS-icon-search"></div>\n    <div class="VS-placeholder"></div>\n    <div class="VS-search-inner"></div>\n    <div class="VS-icon VS-icon-cancel VS-cancel-search-box" title="clear search"></div>\n  </div>\n</div>');
window.JST['search_facet'] = _.template('<% if (model.has(\'category\')) { %>\n  <div class="category"><%- model.get(\'category\') %>:</div>\n<% } %>\n\n<div class="search_facet_input_container">\n  <input type="text" class="search_facet_input ui-menu VS-interface" value="" <% if (readOnly) { %>disabled="disabled"<% } %> />\n</div>\n\n<div class="search_facet_remove VS-icon VS-icon-cancel"></div>');
window.JST['search_input'] = _.template('<input type="text" class="ui-menu" <% if (readOnly) { %>disabled="disabled"<% } %> />');
})();