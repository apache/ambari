import { JsonSchemaErrorBase } from './error';
import { Serializer, WriterFn } from './serializer';
export declare class UnknownMimetype extends JsonSchemaErrorBase {
}
export declare function createSerializerFromMimetype(mimetype: string, writer: WriterFn, ...opts: any[]): Serializer;
declare module './serializer' {
    namespace Serializer {
        let fromMimetype: typeof createSerializerFromMimetype;
    }
}
