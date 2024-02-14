"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * An enumeration of basic types.
 *
 * @experimental
 */
var BuiltinType;
(function (BuiltinType) {
    /**
     * The type is a type that can hold any other type.
     */
    BuiltinType[BuiltinType["Any"] = 0] = "Any";
    /**
     * The type of a string literal.
     */
    BuiltinType[BuiltinType["String"] = 1] = "String";
    /**
     * The type of a numeric literal.
     */
    BuiltinType[BuiltinType["Number"] = 2] = "Number";
    /**
     * The type of the `true` and `false` literals.
     */
    BuiltinType[BuiltinType["Boolean"] = 3] = "Boolean";
    /**
     * The type of the `undefined` literal.
     */
    BuiltinType[BuiltinType["Undefined"] = 4] = "Undefined";
    /**
     * the type of the `null` literal.
     */
    BuiltinType[BuiltinType["Null"] = 5] = "Null";
    /**
     * the type is an unbound type parameter.
     */
    BuiltinType[BuiltinType["Unbound"] = 6] = "Unbound";
    /**
     * Not a built-in type.
     */
    BuiltinType[BuiltinType["Other"] = 7] = "Other";
})(BuiltinType = exports.BuiltinType || (exports.BuiltinType = {}));
//# sourceMappingURL=symbols.js.map