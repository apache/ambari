var Handlebars = require('handlebars');

describe('Plugin', function() {
  var plugin;

  beforeEach(function() {
    plugin = new Plugin({});
  });

  it('should be an object', function() {
    expect(plugin).to.be.ok();
  });

  it('should has #compile method', function() {
    expect(plugin.compile).to.be.a(Function);
  });

  it('should compile and produce valid result', function(done) {
    var content = '<strong>{{weak}}</strong>';
    var expected = '<strong>wat</strong>';

    plugin.compile(content, 'template.handlebars', function(error, data) {
      expect(error).not.to.be.ok();
      expect(eval(data)({weak: 'wat'})).to.equal(expected);
      done();
    });
  });
});
