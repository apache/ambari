'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var platformPath;
switch (process.env.COPY_PLUGIN_PATH_TYPE) {
    case 'WIN32':
        platformPath = _path2.default.win32;
        break;
    case 'POSIX':
        platformPath = _path2.default.posix;
        break;
    default:
        platformPath = _path2.default;
}

exports.default = platformPath;