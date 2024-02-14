/*global require:true*/
/*global __dirname:true*/
(function(exports){
	"use strict";
	var path = require( 'path' );
	var Constructor = require( path.join('..', 'lib', 'directory-encoder') );
	var fs = require( 'fs' );
	var _ = require( 'lodash' );

	var outputFileData = '\n.icon-2,\n\n.what-bear {\n\tbackground-image: url(\'data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2262.905px%22%3E%3Cpath%20d%3D%22M11.068%2C34.558c-1.585-2.365-2.595-5.098-2.939-8.106c-0.344%2C0.092-0.666%2C0.161-1.033%2C0.161%20c-2.342%2C0-4.248-1.906-4.248-4.248c0-1.47%2C0.758-2.756%2C1.883-3.514l12.147-8.45c2.549-1.562%2C5.534-2.526%2C8.749-2.641l30.149%2C0.092%20L77.819%2C4.34c0-0.115%2C0-0.229%2C0-0.345C77.819%2C1.791%2C79.586%2C0%2C81.791%2C0c2.205%2C0%2C3.996%2C1.791%2C3.996%2C3.995%20c0%2C0.345-0.046%2C0.712-0.138%2C1.034l2.043%2C0.275c2.365%2C0.459%2C4.156%2C2.549%2C4.156%2C5.052c0%2C0.161%2C0%2C0.298-0.022%2C0.436l6.544%2C3.536%20c0.941%2C0.368%2C1.63%2C1.309%2C1.63%2C2.388c0%2C0.367-0.068%2C0.689-0.206%2C1.01l-1.631%2C3.697c-0.804%2C1.309-2.181%2C2.228-3.788%2C2.411%20l-15.041%2C1.791L65.787%2C41.527l7.738%2C13.363l5.098%2C2.365c0.803%2C0.552%2C1.354%2C1.493%2C1.354%2C2.549c0%2C1.699-1.378%2C3.078-3.101%2C3.078%20l-9.805%2C0.022c-2.525%2C0-4.707-1.424-5.809-3.49l-8.382-15.155l-18.92%2C0.023l6.682%2C10.287l4.937%2C2.25%20c0.919%2C0.551%2C1.516%2C1.538%2C1.516%2C2.664c0%2C1.699-1.378%2C3.076-3.077%2C3.076l-9.828%2C0.023c-2.388%2C0-4.5-1.286-5.649-3.215l-9.208-14.627%20l-6.429%2C6.246l-0.528%2C4.087l2.158%2C1.423c0.368%2C0.184%2C0.689%2C0.438%2C0.965%2C0.758c1.056%2C1.332%2C0.872%2C3.284-0.459%2C4.34%20c-0.574%2C0.482-1.286%2C0.713-1.975%2C0.689l-4.317%2C0.023c-1.194-0.139-2.273-0.758-2.962-1.677l-5.029-8.68C0.275%2C51.033%2C0%2C50%2C0%2C48.898%20c0-1.676%2C0.62-3.215%2C1.676-4.387L11.068%2C34.558z%22%2F%3E%3C%2Fsvg%3E\');\n\tbackground-repeat: no-repeat;\n\twidth: 100px;\n\theight: 62.905px;\n}\n\n\n\n.what-dog {\n\tbackground-image: url(\'dog.png\');\n\tbackground-repeat: no-repeat;\n\twidth: 100px;\n\theight: 62.905px;\n}\n\n\n';
	var outputFileData2 = '\n.icon-2,\n\n.icon-bear:before,\n\n.icon-bear:after,\n\n.what-bear {\n\tbackground-image: url(\'data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2262.905px%22%3E%3Cpath%20d%3D%22M11.068%2C34.558c-1.585-2.365-2.595-5.098-2.939-8.106c-0.344%2C0.092-0.666%2C0.161-1.033%2C0.161%20c-2.342%2C0-4.248-1.906-4.248-4.248c0-1.47%2C0.758-2.756%2C1.883-3.514l12.147-8.45c2.549-1.562%2C5.534-2.526%2C8.749-2.641l30.149%2C0.092%20L77.819%2C4.34c0-0.115%2C0-0.229%2C0-0.345C77.819%2C1.791%2C79.586%2C0%2C81.791%2C0c2.205%2C0%2C3.996%2C1.791%2C3.996%2C3.995%20c0%2C0.345-0.046%2C0.712-0.138%2C1.034l2.043%2C0.275c2.365%2C0.459%2C4.156%2C2.549%2C4.156%2C5.052c0%2C0.161%2C0%2C0.298-0.022%2C0.436l6.544%2C3.536%20c0.941%2C0.368%2C1.63%2C1.309%2C1.63%2C2.388c0%2C0.367-0.068%2C0.689-0.206%2C1.01l-1.631%2C3.697c-0.804%2C1.309-2.181%2C2.228-3.788%2C2.411%20l-15.041%2C1.791L65.787%2C41.527l7.738%2C13.363l5.098%2C2.365c0.803%2C0.552%2C1.354%2C1.493%2C1.354%2C2.549c0%2C1.699-1.378%2C3.078-3.101%2C3.078%20l-9.805%2C0.022c-2.525%2C0-4.707-1.424-5.809-3.49l-8.382-15.155l-18.92%2C0.023l6.682%2C10.287l4.937%2C2.25%20c0.919%2C0.551%2C1.516%2C1.538%2C1.516%2C2.664c0%2C1.699-1.378%2C3.076-3.077%2C3.076l-9.828%2C0.023c-2.388%2C0-4.5-1.286-5.649-3.215l-9.208-14.627%20l-6.429%2C6.246l-0.528%2C4.087l2.158%2C1.423c0.368%2C0.184%2C0.689%2C0.438%2C0.965%2C0.758c1.056%2C1.332%2C0.872%2C3.284-0.459%2C4.34%20c-0.574%2C0.482-1.286%2C0.713-1.975%2C0.689l-4.317%2C0.023c-1.194-0.139-2.273-0.758-2.962-1.677l-5.029-8.68C0.275%2C51.033%2C0%2C50%2C0%2C48.898%20c0-1.676%2C0.62-3.215%2C1.676-4.387L11.068%2C34.558z%22%2F%3E%3C%2Fsvg%3E\');\n\tbackground-repeat: no-repeat;\n\twidth: 100px;\n\theight: 62.905px;\n}\n\n\n\n.icon-dog:before,\n\n.icon-dog:after,\n\n.what-dog {\n\tbackground-image: url(\'dog.png\');\n\tbackground-repeat: no-repeat;\n\twidth: 100px;\n\theight: 62.905px;\n}\n\n\n';
	var encoder, output = path.resolve( path.join( __dirname, "output", "encoded.css" ) );

	exports.constructor = {
		setUp: function( done ){
			done();
		},
		tearDown: function( done ){
			done();
		},
		noargs: function( test ){
			test.expect(1);
			test.throws(function(){
				new Constructor();
			}, "Constructor should throw error if now arguments passed in");
			test.done();
		},
		onearg: function( test ){
			test.expect(1);
			test.throws(function(){
				new Constructor(["foo.svg"]);
			}, "Two arguments should be passed in, files and an output file name");
			test.done();
		},
		twoargs: function( test ){
			test.expect(8);
			var output = "testfile.css";

			var de = new Constructor( ["foo.svg", "bar.svg"], output );
			test.equal( de.output, output, "Output should be set" );
			test.equal( de.files[0], "foo.svg", "First file should match foo.svg" );
			test.equal( de.files[1], "bar.svg", "First file should match foo.svg" );

			test.equal( de.prefix, ".icon-", "Default prefix is .icon-" );
			test.equal( Object.keys(de.customselectors).length, 0, "Default custom selectors is empty object" );
			test.deepEqual( de.template, false, "Default value for a template is false" );
			test.deepEqual( de.templatePrepend, "", "Default value for templatePrepend is empty string" );
			test.deepEqual( de.templateAppend, "", "Default value for templateAppend is empty string" );

			test.done();
		},
		options: function( test ){
			test.expect(6);
			var output = "testfile.css";
			var options = {
				prefix: "foo",
				templatePrepend: "baa",
				templateAppend: "ram"
			};

			var de = new Constructor( ["foo.svg", "bar.svg"], output, options );
			test.equal( de.output, output, "Output should be set" );
			test.equal( de.files[0], "foo.svg", "First file should match foo.svg" );
			test.equal( de.files[1], "bar.svg", "First file should match foo.svg" );

			test.equal( de.prefix, "foo", "Default prefix is .icon-" );
			test.deepEqual( de.templatePrepend, "baa", "Default value for templatePrepend is empty string" );
			test.deepEqual( de.templateAppend, "ram", "Default value for templateAppend is empty string" );

			test.done();
		}

	};

	exports.encode = {
		setUp: function( done ) {
			this.encoder = new Constructor( "test/directory-files", output );
			this.encoder2 = new Constructor( [path.resolve( path.join(__dirname, "directory-files", "bear.svg")),
																		path.resolve( path.join(__dirname, "directory-files", "dog.png"))], output );
			done();
		},

		output: function( test ) {
			this.encoder.encode();
			test.ok( fs.existsSync(output) );
			test.ok( /\.bear/.test(fs.readFileSync(output)) );

			fs.unlinkSync(output);
			this.encoder2.encoder();
			test.ok( fs.existsSync(output) );
			test.ok( /\.bear/.test(fs.readFileSync(output)) );
			test.done();
		},

		selector: function( test ) {
			this.encoder._css = function( name, data ){
				test.ok( name === "bear" || name === "dog" );

				return Constructor.prototype._css(name, data);
			};

			this.encoder.encode();
			test.done();
		},

		dup: function( test ) {
			this.encoder = new Constructor( "test/encoding-dup", output );
			test.throws(function() { this.encoder.encode(); });
			test.done();
		}
	};


	var encoders;

	exports['encoderSelection'] = {
		setUp: function( done ) {
			encoder = new Constructor( "test/directory-files", output );
			encoders = _.clone( Constructor.encoders );
			done();
		},

		tearDown: function( done ) {
			Constructor.encoders = encoders;

			done();
		},

		handler: function( test ) {
			Constructor.encoders.svg = function(){};
			Constructor.encoders.svg.prototype.stats = function(){
				return {};
			};
			Constructor.encoders.svg.prototype.encode = function() {
				return "foo";
			};

			Constructor.encoders.png = function(){};
			Constructor.encoders.png.prototype.stats = function(){
				return {};
			};
			Constructor.encoders.png.prototype.encode = function() {
				return "bar";
			};

			encoder._css = function( filename, datauri ) {
				test.ok( datauri === "foo" || datauri === "bar" );
			};

			encoder.encode();

			test.done();
		}
	};


	exports['css'] = {
		setUp: function( done ) {
			this.encoder = new Constructor( "test/directory-files", "test/output/encoded.css" );
			this.encoder2 = new Constructor( "test/directory-files", "test/output/encoded2.css",
																{ template: path.resolve( "test/files/default-css.hbs" )} );
			this.encoder3 = new Constructor( "test/directory-files", "test/output/encoded3.css",
																{
																	template: path.resolve( "test/files/default-css.hbs" ),
																	customselectors: {
																		"foo": [".icon-2"]
																	}
																} );
			this.encoder4 = new Constructor( "test/directory-files", "test/output/encoded3.css",
																{
																	template: path.resolve( "test/files/default-css.hbs" ),
																	prefix: ".what-",
																	customselectors: {
																		"foo": [".icon-2"]
																	}
																} );
			this.encoder5 = new Constructor( "test/directory-files",
						"test/output/encoded3.css",
						{
							template: path.resolve( "test/files/default-css.hbs" ),
							prefix: ".icon-",
							customselectors: {
								"*": [".icon-$1:before", ".icon-$1-what", ".hey-$1"],
								"foo": [".icon-2"]
							}
						});
			this.encoder6 = new Constructor( "test/directory-files",
						"test/output/encoded3.css",
						{
							prefix: ".icon-",
							customselectors: {
								"*": [".icon-$1:before", ".icon-$1-what", ".hey-$1"],
								"foo": [".icon-2"]
							}
						});
			done();
		},
		tearDown: function( done ){
			["", "2", "3"].forEach(function( num ){
				if( fs.existsSync( "test/output/encoded" + num + ".css" ) ){
					fs.unlinkSync( "test/output/encoded" + num + ".css" );
				}
			});
			done();
		},

		rule: function( test ) {
			test.equal( this.encoder._css("foo", "bar"),
				".icon-foo { background-image: url('bar'); background-repeat: no-repeat; }" );
			test.done();
		},

		withTemplateError: function( test ) {
			test.expect(1);
			test.throws(function(){
				new Constructor( "test/directory-files", "test/output/encoded2.css",
																	{ template: path.resolve( "test/files/not-here.hbs" )} );
			});
			test.done();
		},
		withTemplate: function( test ) {
			test.equal( this.encoder2._css("foo", "bar"),
				"\n.icon-foo {\n" +
					"\tbackground-image: url('bar');\n" +
					"\tbackground-repeat: no-repeat;\n" +
				"}\n" );
			test.done();
		},

		withTemplateCustomSelectors: function( test ) {
			test.equal( this.encoder3._css("foo", "bar"),
				"\n.icon-2,\n" +
				"\n.icon-foo {\n" +
					"\tbackground-image: url('bar');\n" +
					"\tbackground-repeat: no-repeat;\n" +
				"}\n" );
			test.done();
		},

		withDifferentPrefix: function( test ) {
			test.equal( this.encoder4._css("foo", "bar"),
				"\n.icon-2,\n" +
				"\n.what-foo {\n" +
					"\tbackground-image: url('bar');\n" +
					"\tbackground-repeat: no-repeat;\n" +
				"}\n" );
			test.done();
		},

		withCustomSelectAll: function( test ){
			test.equal( this.encoder5._css("foo", "bar"),
				"\n.icon-2,\n" +
				"\n.icon-foo:before,\n" +
				"\n.icon-foo-what,\n" +
				"\n.hey-foo,\n" +
				"\n.icon-foo {\n" +
					"\tbackground-image: url('bar');\n" +
					"\tbackground-repeat: no-repeat;\n" +
				"}\n" );
			test.done();
		},

		withCustomSelectAllnoTemplate: function( test ){
			test.equal( this.encoder6._css("foo", "bar"),
				".icon-2,\n" +
				".icon-foo:before,\n" +
				".icon-foo-what,\n" +
				".hey-foo,\n" +
				".icon-foo { " +
					"background-image: url('bar'); " +
					"background-repeat: no-repeat; " +
				"}" );
			test.done();
		}
	};

	exports.encode = {
		setUp: function( done ){
			this.encoder = new Constructor( ["test/directory-files/bear.svg", "test/directory-files/dog.png"], "test/output/encode.css",
													{
														template: path.resolve( "test/files/default-css.hbs" ),
														prefix: ".what-",
														customselectors: {
															"bear": [".icon-2"]
														},
														noencodepng: true
													} );
			this.encoder2 = new Constructor( ["test/directory-files/bear.svg", "test/directory-files/dog.png"], "test/output/encode2.css",
													{
														template: path.resolve( "test/files/default-css.hbs" ),
														prefix: ".what-",
														customselectors: {
															"*": [".icon-$1:before", ".icon-$1:after"],
															"bear": [".icon-2"]
														},
														noencodepng: true
													} );
			done();
		},
		tearDown: function( done ){
			["", "2"].forEach(function( i ){
				if( fs.existsSync( "test/output/encode" + i + ".css" ) ){
					fs.unlinkSync( "test/output/encode" + i + ".css" );
				}
			});
			done();
		},
		'no args': function( test ){
			test.expect( 2 );
			var data;
			this.encoder.encode();
			test.ok( fs.existsSync( "test/output/encode.css" ) , "CSS file exists" );
			data = fs.readFileSync( "test/output/encode.css" ).toString( 'utf-8' );
			test.equal( data, outputFileData, "Encoded file matches" );

			test.done();
		},
		'no args, customselectorall': function( test ){
			test.expect( 2 );
			var data;
			this.encoder2.encode();
			test.ok( fs.existsSync( "test/output/encode2.css" ) , "CSS file exists" );
			data = fs.readFileSync( "test/output/encode2.css" ).toString( 'utf-8' );
			test.equal( data, outputFileData2, "Encoded file matches" );

			test.done();
		}
	};

}(typeof exports === 'object' && exports || this));
