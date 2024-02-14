var path = require("path");
var fs = require("fs");
var should = require("should");
var loader = require("../");

function execLoader(filename, callback) {
	var async = false;
	var deps = [];
	var warns = [];
	var context = {
		context: path.dirname(filename),
		resolve: function(context, request, callback) {
			process.nextTick(function() {
				var p = path.join(context, request);
				if(fs.existsSync(p))
					callback(null, p);
				else
					callback(new Error("File not found"));
			});
		},
		addDependency: function(dep) {
			deps.push(dep);
		},
		emitWarning: function(warn) {
			warns.push(warn);
		},
		callback: function(err, res, map) {
			async = true;
			callback(err, res, map, deps, warns);
		},
		async: function() {
			async = true;
			return this.callback;
		}
	};
	// Remove CRs to make test line ending invariant
	var fixtureContent = fs.readFileSync(filename, "utf-8").replace(/\r/g, '');
	var res = loader.call(context, fixtureContent);
	if(!async) return callback(null, res, null, deps, warns);
}

describe("source-map-loader", function() {
	it("should leave normal files untouched", function(done) {
		execLoader(path.join(__dirname, "fixtures", "normal-file.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "without SourceMap"),
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});
	it("should process inlined SourceMaps", function(done) {
		execLoader(path.join(__dirname, "fixtures", "inline-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"inline-source-map.js",
				"sources":[
					"inline-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([]);
			done();
		});
	});
	it("should process external SourceMaps", function(done) {
		execLoader(path.join(__dirname, "fixtures", "external-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"external-source-map.js",
				"sources":[
					"external-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(__dirname, "fixtures", "external-source-map.map")
			]);
			done();
		});
	});
	it("should process external SourceMaps (external sources)", function(done) {
		execLoader(path.join(__dirname, "fixtures", "external-source-map2.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"external-source-map2.js",
				"sources":[
					path.join(__dirname, "fixtures", "external-source-map2.txt")
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(__dirname, "fixtures", "data", "external-source-map2.map"),
				path.join(__dirname, "fixtures", "external-source-map2.txt")
			]);
			done();
		});
	});
	it("should use last SourceMap directive", function (done) {
		execLoader(path.join(__dirname, "fixtures", "multi-source-map.js"), function (err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\nanInvalidDirective = \"\\n/*# sourceMappingURL=data:application/json;base64,\"+btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap))))+\" */\";\n// comment"),
				map.should.be.eql({
					"version": 3,
					"file": "inline-source-map.js",
					"sources": [
						"inline-source-map.txt"
					],
					"sourcesContent": ["with SourceMap"],
					"mappings": "AAAA"
				});
			deps.should.be.eql([]);
			done();
		});
	});
	it("should warn on missing SourceMap", function(done) {
		execLoader(path.join(__dirname, "fixtures", "missing-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([
				"Cannot find SourceMap 'missing-source-map.map': Error: File not found"
			]);
			should.equal(res, "with SourceMap\n//#sourceMappingURL=missing-source-map.map\n// comment"),
			should.equal(map, null);
			deps.should.be.eql([]);
			done();
		});
	});
	it("should warn on missing source file", function(done) {
		execLoader(path.join(__dirname, "fixtures", "missing-source-map2.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([
				"Cannot find source file 'missing-source-map2.txt': Error: File not found"
			]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"missing-source-map2.js",
				"sources":[
					"missing-source-map2.txt"
				],
				"sourcesContent":[null],
				"mappings":"AAAA"
			});
			deps.should.be.eql([
				path.join(__dirname, "fixtures", "missing-source-map2.map")
			]);
			done();
		});
	});

	it("should process inlined SourceMaps with charset", function(done) {
		execLoader(path.join(__dirname, "fixtures", "charset-inline-source-map.js"), function(err, res, map, deps, warns) {
			should.equal(err, null);
			warns.should.be.eql([]);
			should.equal(res, "with SourceMap\n// comment"),
			map.should.be.eql({
				"version":3,
				"file":"charset-inline-source-map.js",
				"sources":[
					"charset-inline-source-map.txt"
				],
				"sourcesContent":["with SourceMap"],
				"mappings":"AAAA"
			});
			deps.should.be.eql([]);
			done();
		});
	});
});