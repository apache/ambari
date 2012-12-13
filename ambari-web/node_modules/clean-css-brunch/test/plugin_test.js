describe('Plugin', function() {
  var plugin;

  beforeEach(function() {
    plugin = new Plugin({});
  });

  it('should be an object', function() {
    expect(plugin).to.be.ok;
  });

  it('should has #minify method', function() {
    expect(plugin.minify).to.be.an.instanceof(Function);
  });

  it('should compile and produce valid result', function(done) {
    var content = '#first { font-size: 14px; color: #b0b; }';
    var expected = '#first{font-size:14px;color:#b0b}';

    plugin.minify(content, '', function(error, data) {
      expect(error).not.to.be.ok;
      expect(data).to.equal(expected);
      done();
    });
  });
});
