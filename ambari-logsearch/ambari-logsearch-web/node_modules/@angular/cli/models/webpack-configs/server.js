"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Returns a partial specific to creating a bundle for node
 * @param _wco Options which are include the build options and app config
 */
exports.getServerConfig = function (_wco) {
    return {
        target: 'node',
        output: {
            libraryTarget: 'commonjs'
        },
        externals: [
            /^@angular/,
            function (_, request, callback) {
                // Absolute & Relative paths are not externals
                if (request.match(/^\.{0,2}\//)) {
                    return callback();
                }
                try {
                    // Attempt to resolve the module via Node
                    const e = require.resolve(request);
                    if (/node_modules/.test(e)) {
                        // It's a node_module
                        callback(null, request);
                    }
                    else {
                        // It's a system thing (.ie util, fs...)
                        callback();
                    }
                }
                catch (e) {
                    // Node couldn't find it, so it must be user-aliased
                    callback();
                }
            }
        ]
    };
};
//# sourceMappingURL=/users/hansl/sources/angular-cli/models/webpack-configs/server.js.map