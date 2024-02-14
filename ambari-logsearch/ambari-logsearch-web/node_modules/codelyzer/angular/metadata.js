"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var DirectiveMetadata = (function () {
    function DirectiveMetadata() {
    }
    return DirectiveMetadata;
}());
exports.DirectiveMetadata = DirectiveMetadata;
var ComponentMetadata = (function (_super) {
    __extends(ComponentMetadata, _super);
    function ComponentMetadata() {
        return _super.apply(this, arguments) || this;
    }
    return ComponentMetadata;
}(DirectiveMetadata));
exports.ComponentMetadata = ComponentMetadata;
