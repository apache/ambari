"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const denodeify = require("denodeify");
const SilentError = require('silent-error');
const PortFinder = require('portfinder');
const getPort = denodeify(PortFinder.getPort);
function checkPort(port, host, basePort = 49152) {
    PortFinder.basePort = basePort;
    return getPort({ port, host })
        .then(foundPort => {
        // If the port isn't available and we weren't looking for any port, throw error.
        if (port !== foundPort && port !== 0) {
            throw new SilentError(`Port ${port} is already in use. Use '--port' to specify a different port.`);
        }
        // Otherwise, our found port is good.
        return foundPort;
    });
}
exports.checkPort = checkPort;
//# sourceMappingURL=/users/hansl/sources/angular-cli/utilities/check-port.js.map