(function() {

var $ = jQuery; // Handle namespaced jQuery

// Used to extract keywords and facets from the free text search.
var QUOTES_RE   = "('[^']+'|\"[^\"]+\")";
var FREETEXT_RE = "('[^']+'|\"[^\"]+\"|[^'\"\\s]\\S*)";
var CATEGORY_RE = FREETEXT_RE +                     ':\\s*';
VS.app.SearchParser = {

  // Matches `category: "free text"`, with and without quotes.
  ALL_FIELDS : new RegExp(CATEGORY_RE + FREETEXT_RE, 'g'),

  // Matches a single category without the text. Used to correctly extract facets.
  CATEGORY   : new RegExp(CATEGORY_RE),

  // Called to parse a query into a collection of `SearchFacet` models.
  parse : function(instance, query) {
    var searchFacets = this._extractAllFacets(instance, query);
    instance.searchQuery.reset(searchFacets);
    return searchFacets;
  },

  // Walks the query and extracts facets, categories, and free text.
  _extractAllFacets : function(instance, query) {
    var facets = [];
    var originalQuery = query;
    while (query) {
      var category, value;
      originalQuery = query;
      var field = this._extractNextField(query);
      if (!field) {
        category = instance.options.remainder;
        value    = this._extractSearchText(query);
        query    = VS.utils.inflector.trim(query.replace(value, ''));
      } else if (field.indexOf(':') != -1) {
        category = field.match(this.CATEGORY)[1].replace(/(^['"]|['"]$)/g, '');
        value    = field.replace(this.CATEGORY, '').replace(/(^['"]|['"]$)/g, '');
        query    = VS.utils.inflector.trim(query.replace(field, ''));
      } else if (field.indexOf(':') == -1) {
        category = instance.options.remainder;
        value    = field;
        query    = VS.utils.inflector.trim(query.replace(value, ''));
      }

      if (category && value) {
          var searchFacet = new VS.model.SearchFacet({
            category : category,
            value    : VS.utils.inflector.trim(value),
            app      : instance
          });
          facets.push(searchFacet);
      }
      if (originalQuery == query) break;
    }

    return facets;
  },

  // Extracts the first field found, capturing any free text that comes
  // before the category.
  _extractNextField : function(query) {
    var textRe = new RegExp('^\\s*(\\S+)\\s+(?=' + QUOTES_RE + FREETEXT_RE + ')');
    var textMatch = query.match(textRe);
    if (textMatch && textMatch.length >= 1) {
      return textMatch[1];
    } else {
      return this._extractFirstField(query);
    }
  },

  // If there is no free text before the facet, extract the category and value.
  _extractFirstField : function(query) {
    var fields = query.match(this.ALL_FIELDS);
    return fields && fields.length && fields[0];
  },

  // If the found match is not a category and facet, extract the trimmed free text.
  _extractSearchText : function(query) {
    query = query || '';
    var text = VS.utils.inflector.trim(query.replace(this.ALL_FIELDS, ''));
    return text;
  }

};

})();
