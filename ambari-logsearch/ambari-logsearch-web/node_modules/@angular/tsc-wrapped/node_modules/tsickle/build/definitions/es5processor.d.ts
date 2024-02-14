/**
 * Extracts the namespace part of a goog: import, or returns null if the given
 * import is not a goog: import.
 */
export declare function extractGoogNamespaceImport(tsImport: string): string | null;
/**
 * Converts TypeScript's JS+CommonJS output to Closure goog.module etc.
 * For use as a postprocessing step *after* TypeScript emits JavaScript.
 *
 * @param fileName The source file name.
 * @param moduleId The "module id", a module-identifying string that is
 *     the value module.id in the scope of the module.
 * @param pathToModuleName A function that maps a filesystem .ts path to a
 *     Closure module name, as found in a goog.require('...') statement.
 *     The context parameter is the referencing file, used for resolving
 *     imports with relative paths like "import * as foo from '../foo';".
 * @param prelude An additional prelude to insert after the `goog.module` call,
 *     e.g. with additional imports or requires.
 */
export declare function processES5(fileName: string, moduleId: string, content: string, pathToModuleName: (context: string, fileName: string) => string, isES5?: boolean, prelude?: string): {
    output: string;
    referencedModules: string[];
};
