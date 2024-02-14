import * as ts from 'typescript';
import { AotPlugin } from './plugin';
import { TypeScriptFileRefactor } from './refactor';
import { LoaderContext } from './webpack';
export declare function removeModuleIdOnlyForTesting(refactor: TypeScriptFileRefactor): void;
export declare function _getModuleExports(plugin: AotPlugin, refactor: TypeScriptFileRefactor): ts.Identifier[];
export declare function _replaceExport(plugin: AotPlugin, refactor: TypeScriptFileRefactor): void;
export declare function _exportModuleMap(plugin: AotPlugin, refactor: TypeScriptFileRefactor): void;
export declare function ngcLoader(this: LoaderContext & {
    _compilation: any;
}, source: string | null): void;
