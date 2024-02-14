/**
 * @fileoverview Helpers that TypeScript needs at runtime. __extends is only
 * used in ES5 development mode, __decorate, __metadata and __param support ES7
 * decorators, __awaiter supports async/await (when generators) are available.
 *
 * taken from https://github.com/Microsoft/TypeScript/blob/c6588d27f18fed4e290a6b22a29664963a2876a9/src/compiler/emitter.ts
 *
 * These use the literal space optimized code from TypeScript for compatibility.
 */

function __extends(d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    /** @constructor */
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
}

/**
 * @param {?} decorators
 * @param {?} target
 * @param {?=} opt_key
 * @param {?=} opt_desc
 * @suppress {undefinedVars}
 */
function __decorate(decorators, target, opt_key, opt_desc) {
    var c = arguments.length, r = c < 3 ? target : opt_desc === null ? opt_desc = Object.getOwnPropertyDescriptor(target, opt_key) : opt_desc, d;
    var reflect = (window || global)['Reflect'];  // Work around b/28176554 .
    if (typeof reflect === "object" && typeof reflect.decorate === "function") r = reflect.decorate(decorators, target, opt_key, opt_desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, opt_key, r) : d(target, opt_key)) || r;
    return c > 3 && r && Object.defineProperty(target, opt_key, r), r;
}

/**
 * @suppress {undefinedVars}
 */
function __metadata(k, v) {
    var reflect = (window || global)['Reflect'];  // Work around b/28176554 .
    if (typeof reflect === "object" && typeof reflect.metadata === "function")
        return reflect.metadata(k, v);
}

var __param = function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};

var __awaiter = function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator.throw(value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments)).next());
    });
};
