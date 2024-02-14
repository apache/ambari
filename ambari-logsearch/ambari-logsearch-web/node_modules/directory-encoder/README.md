# directory-encoder [![Build Status](https://secure.travis-ci.org/filamentgroup/directory-encoder.png?branch=master)](http://travis-ci.org/filamentgroup/directory-encoder)

[![Filament Group](http://filamentgroup.com/images/fg-logo-positive-sm-crop.png) ](http://www.filamentgroup.com/)

Encode a directory of PNG or SVG images to CSS

## Getting Started
Install the module with: `npm install directory-encoder`

```javascript
var DirectoryEncoder = require('directory-encoder');
var de = new DirectoryEncoder( source, destinationCSSFile, {
			pngfolder: pngfolder, //in case you need to link out for PNGs,
like when the datauri is way too long
			customselectors: { "foo": [".bar", ".baz"]}, 
			prefix: ".icon-", // used to prefix the name of the file for the CSS classname, .icon- is the default
			templatePrepend: "/* start of icon-list */", // this string is prepended to the destinationCSSFile, defaults to ""
			template: template.hbs, //template in handlebars, FANCY!
			templateAppend: "/* end of icon-list */", // this string is appended to the destinationCSSFile, defaults to ""
			noencodepng: false // turn this to true if you want no datauris
for pngs, just links out to png files
		});
de.encode(); // "Guitar solo -- File outputted"
```

## Documentation

### Constructor

Takes three arguments, source directory for encoding/an array of files
you'd like to encode, destination css
file for when it writes, and an options hash that includes a spot for
customselectors, a hbs template in case you want to get nuts with your
css, where the pngs are located if you want to link out to them (or you
don't have a choice because the data uri is bigger than 32k), and a
switch to turn off datauris for pngs.

### encode

All the magic happens here.

## Examples
```
var de = new DirectoryEncoder( source, destinationCSSFile, {
			pngfolder: pngfolder, //in case you need to link out for PNGs,
like when the datauri is way too long
			customselectors: { "foo": [".bar", ".baz"]}, 
			template: template.hbs, //template in handlebars, FANCY!
			noencodepng: false // turn this to true if you want no datauris
for pngs, just links out to png files
		});
de.encode(); // "Guitar solo -- File outputted"
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint and test your code using [Grunt](http://gruntjs.com/).

## Release History

* 0.7.0  Add templateAppend and templatePrepend options
* 0.6.0  Fix encoding brackets and ability to take an array
* 0.5.0  PNGpath
* 0.4.0  Width/Height from template
* 0.3.0  Adding custom selectors that allow for wildcards
* 0.2.0  Adding switchable icon name prefixes
* 0.1.0  Woo

## License
Copyright (c) 2013 John Bender/Jeffrey Lembeck/Filament Group
Licensed under the MIT license.
