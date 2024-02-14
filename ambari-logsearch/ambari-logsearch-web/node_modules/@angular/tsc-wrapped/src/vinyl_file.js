"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function isVinylFile(obj) {
    return (typeof obj === 'object') && ('path' in obj) && ('contents' in obj);
}
exports.isVinylFile = isVinylFile;
//# sourceMappingURL=vinyl_file.js.map