"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const error_1 = require("./error");
const serializer_1 = require("./serializer");
const json_1 = require("./serializers/json");
const dts_1 = require("./serializers/dts");
class UnknownMimetype extends error_1.JsonSchemaErrorBase {
}
exports.UnknownMimetype = UnknownMimetype;
function createSerializerFromMimetype(mimetype, writer, ...opts) {
    let Klass = null;
    switch (mimetype) {
        case 'application/json':
            Klass = json_1.JsonSerializer;
            break;
        case 'text/json':
            Klass = json_1.JsonSerializer;
            break;
        case 'text/x.typescript':
            Klass = dts_1.DTsSerializer;
            break;
        case 'text/x.dts':
            Klass = dts_1.DTsSerializer;
            break;
        default: throw new UnknownMimetype();
    }
    return new Klass(writer, ...opts);
}
exports.createSerializerFromMimetype = createSerializerFromMimetype;
serializer_1.Serializer.fromMimetype = createSerializerFromMimetype;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/mimetypes.js.map