/*global require:true*/
(function(){
	"use strict";
	var path = require( 'path' );
	var Constructor = require( path.join('..', 'lib', 'data-uri-encoder'));
	var SvgURIEncoder = require( path.join('..', 'lib', 'svg-uri-encoder'));
	var PngURIEncoder = require( path.join('..', 'lib', 'png-uri-encoder'));
	var _ = require( 'lodash' );

	exports['Constructor'] = {
		setUp: function( done ) {
			this.encoder = new Constructor( "test/files/bear.svg" );
			done();
		},

		path: function( test ) {
			test.equal( this.encoder.path, "test/files/bear.svg" );
			test.done();
		},

		extension: function( test ) {
			test.equal( this.encoder.extension, "svg" );
			test.done();
		}
	};

	function testEncoded( test, str ) {
		str.split('').forEach(function( c ) {
			test.ok( /[a-zA-Z0-9+\/=]+/.test(c) );
		});
	}

	exports['encode'] = {
		setUp: function( done ) {
			this.encoder = new Constructor( "test/files/cat.png" );
			done();
		},

		output: function( test ) {
			testEncoded( test, this.encoder.encode());
			test.done();
		},

		callback: function( test ) {
			this.encoder.encode(function( prefix, fileData ) {
				test.equal( prefix, this.encoder.prefix );
				test.ok( fileData );
			});

			test.done();
		}
	};

	exports['stats'] = {
		setUp: function( done ) {
			this.encoder = new Constructor( "test/files/cat.png" );
			this.encoder2 = new Constructor( "test/files/bear.svg" );
			done();
		},

		stats: function( test ) {
			test.equal( this.encoder.stats().width, '100px', "Width should match" );
			test.equal( this.encoder.stats().height, '100px', "Height should match on png" );
			test.equal( this.encoder2.stats().width, '100px', "Width should match" );
			test.equal( this.encoder2.stats().height, '62.905px', "Height should match on svg" );
			test.done();
		}
	};

	exports['SvgURIEncoder'] = {
		setUp: function( done ) {
			this.encoder = new SvgURIEncoder( "test/files/bear.svg" );
			done();
		},

		encode: function( test ) {
			var datauri = this.encoder.encode();

			test.ok( datauri.indexOf(SvgURIEncoder.prefix) >= 0 );
			test.ok( datauri.indexOf( '%' ) >= 0 );
			test.done();
		}
	};

	/**
	 * Test encoding brackets '(' and ')' to generate a valid encoded SVG 
	 * in case of <g> element with functions in the 'transformation' attribute 
	 */
	exports['SvgURIEncoder_brackets'] = {
		setUp: function( done ) {
			this.encoder = new SvgURIEncoder( "test/files/bear.svg" );
			done();
		},

		encode: function( test ) {
			var datauri = this.encoder.encode();

			test.ok( datauri.indexOf(SvgURIEncoder.prefix) >= 0 );
			test.ok( datauri.indexOf( '%' ) >= 0 );
			test.ok( datauri.indexOf( '(' ) === -1 );
			test.ok( datauri.indexOf( ')' ) === -1 );
			test.done();
		}
	};


	exports['PngURIEncoder'] = {
		setUp: function( done ) {
			this.encoder = new PngURIEncoder( "test/files/cat.png" );
			this.encode = _.clone( Constructor.prototype.encode );
			done();
		},

		tearDown: function( done ) {
			Constructor.prototype.encode = this.encode;
			done();
		},

		encode: function( test ) {
			testEncoded( test, this.encoder.encode().replace(PngURIEncoder.prefix, "") );
			test.done();
		},

		pathSwitch: function( test ) {
			Constructor.prototype.encode = function(){
				var i = 32768, datauri = "";

				while( i >= 0 ) {
					datauri += "a";
					i--;
				}

				return datauri;
			};

			test.equal( this.encoder.encode({ pngfolder: "foo" }), 'foo/cat.png' );
			test.done();
		}
	};
	exports['PngURIEncoder2'] = {
		setUp: function( done ) {
			this.encoder = new PngURIEncoder( "test/files/cat.png" );
			done();
		},
		tearDown: function( done ){
			done();
		},
		noencode: function( test ){
			var options = {
				noencodepng: true,
				pngfolder: "bar"
			};

			test.equal( this.encoder.encode(options), 'bar/cat.png' );
			test.done();
		}
	};
	exports['PngURIEncoder3'] = {
		setUp: function( done ) {
			this.encoder = new PngURIEncoder( "test/files/cat.png" );
			done();
		},
		tearDown: function( done ){
			done();
		},
		noencode_custompath: function( test ){
			var options = {
				noencodepng: true,
				pngfolder: "foo",
				pngpath: "bar"
			};

			test.equal( this.encoder.encode(options), 'bar/cat.png' );
			test.done();
		}
	};
}());
