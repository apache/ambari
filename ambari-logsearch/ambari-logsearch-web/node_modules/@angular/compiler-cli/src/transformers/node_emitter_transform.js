"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var node_emitter_1 = require("./node_emitter");
function getAngularEmitterTransformFactory(generatedFiles) {
    return function () {
        var map = new Map(generatedFiles.filter(function (g) { return g.stmts && g.stmts.length; })
            .map(function (g) { return [g.genFileUrl, g]; }));
        var emitter = new node_emitter_1.TypeScriptNodeEmitter();
        return function (sourceFile) {
            var g = map.get(sourceFile.fileName);
            if (g && g.stmts) {
                var newSourceFile = emitter.updateSourceFile(sourceFile, g.stmts)[0];
                return newSourceFile;
            }
            return sourceFile;
        };
    };
}
exports.getAngularEmitterTransformFactory = getAngularEmitterTransformFactory;
//# sourceMappingURL=node_emitter_transform.js.map