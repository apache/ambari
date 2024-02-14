import { CompileReflector } from '../compile_reflector';
import * as o from '../output/output_ast';
import { SummaryResolver } from '../summary_resolver';
import { StaticSymbol } from './static_symbol';
import { StaticSymbolResolver } from './static_symbol_resolver';
/**
 * A static reflector implements enough of the Reflector API that is necessary to compile
 * templates statically.
 */
export declare class StaticReflector implements CompileReflector {
    private summaryResolver;
    private symbolResolver;
    private errorRecorder;
    private annotationCache;
    private propertyCache;
    private parameterCache;
    private methodCache;
    private conversionMap;
    private injectionToken;
    private opaqueToken;
    private ROUTES;
    private ANALYZE_FOR_ENTRY_COMPONENTS;
    private annotationForParentClassWithSummaryKind;
    private annotationNames;
    constructor(summaryResolver: SummaryResolver<StaticSymbol>, symbolResolver: StaticSymbolResolver, knownMetadataClasses?: {
        name: string;
        filePath: string;
        ctor: any;
    }[], knownMetadataFunctions?: {
        name: string;
        filePath: string;
        fn: any;
    }[], errorRecorder?: (error: any, fileName?: string) => void);
    componentModuleUrl(typeOrFunc: StaticSymbol): string;
    resolveExternalReference(ref: o.ExternalReference): StaticSymbol;
    findDeclaration(moduleUrl: string, name: string, containingFile?: string): StaticSymbol;
    tryFindDeclaration(moduleUrl: string, name: string): StaticSymbol;
    findSymbolDeclaration(symbol: StaticSymbol): StaticSymbol;
    annotations(type: StaticSymbol): any[];
    propMetadata(type: StaticSymbol): {
        [key: string]: any[];
    };
    parameters(type: StaticSymbol): any[];
    private _methodNames(type);
    private findParentType(type, classMetadata);
    hasLifecycleHook(type: any, lcProperty: string): boolean;
    private _registerDecoratorOrConstructor(type, ctor);
    private _registerFunction(type, fn);
    private initializeConversionMap();
    /**
     * getStaticSymbol produces a Type whose metadata is known but whose implementation is not loaded.
     * All types passed to the StaticResolver should be pseudo-types returned by this method.
     *
     * @param declarationFile the absolute path of the file where the symbol is declared
     * @param name the name of the type.
     */
    getStaticSymbol(declarationFile: string, name: string, members?: string[]): StaticSymbol;
    private reportError(error, context, path?);
    /**
     * Simplify but discard any errors
     */
    private trySimplify(context, value);
    private getTypeMetadata(type);
}
