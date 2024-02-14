'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _bluebird = require('bluebird');

var _bluebird2 = _interopRequireDefault(_bluebird);

var _loaderUtils = require('loader-utils');

var _loaderUtils2 = _interopRequireDefault(_loaderUtils);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/* eslint-disable import/no-commonjs */
var fs = _bluebird2.default.promisifyAll(require('fs-extra'));
/* eslint-enable */

exports.default = function (opts) {
    var compilation = opts.compilation;
    // ensure forward slashes
    var relFileDest = opts.relFileDest.replace(/\\/g, '/');
    var relFileSrc = opts.relFileSrc.replace(/\\/g, '/');
    var absFileSrc = opts.absFileSrc;
    var forceWrite = opts.forceWrite;
    var copyUnmodified = opts.copyUnmodified;
    var writtenAssetHashes = opts.writtenAssetHashes;

    return fs.statAsync(absFileSrc).then(function (stat) {

        // We don't write empty directories
        if (stat.isDirectory()) {
            return;
        }

        function addToAssets(content) {

            console.log('before relFileDest', relFileDest);
            relFileDest = _loaderUtils2.default.interpolateName({ resourcePath: relFileSrc }, relFileDest, { content: content });

            console.log('relFileSrc', relFileSrc);
            console.log('after relFileDest', relFileDest);

            if (compilation.assets[relFileDest] && !forceWrite) {
                return;
            }

            compilation.assets[relFileDest] = {
                size: function size() {
                    return stat.size;
                },
                source: function source() {
                    return fs.readFileSync(absFileSrc);
                }
            };

            return relFileDest;
        }

        return fs.readFileAsync(absFileSrc).then(function (content) {
            var hash = _loaderUtils2.default.getHashDigest(content);
            if (!copyUnmodified && writtenAssetHashes[relFileDest] && writtenAssetHashes[relFileDest] === hash) {
                return;
            }
            writtenAssetHashes[relFileDest] = hash;
            return addToAssets(content);
        });
    });
};