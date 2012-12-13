describe('Plugin', function() {
  var plugin;

  beforeEach(function() {
    plugin = new Plugin({paths: {root: '.'}});
  });

  it('should be an object', function() {
    expect(plugin).to.be.ok;
  });

  it('should has #compile method', function() {
    expect(plugin.compile).to.be.an.instanceof(Function);
  });

  it('should compile and produce valid result', function(done) {
    var content = '@color: #4D926F; #header {color: @color;}';
    var expected = '#header {\n  color: #4d926f;\n}\n';

    plugin.compile(content, 'style.less', function(error, data) {
      expect(error).not.to.be.ok;
      expect(data).to.equal(expected)
      done();
    });
  });

  it('should handle invalid less gracefully', function(done) {
    var content = '#header {color: @color;}';
    var expected = "NameError:variable @color is undefined in 'style.less:1:16'"

    plugin.compile(content, 'style.less', function(error, data) {
      expect(error).to.be.ok;
      expect(error).to.equal(expected);
      done();
    });
  });
});
