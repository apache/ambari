/// <reference types="node" />
import { Collection, CollectionDescription, EngineHost, RuleFactory, SchematicDescription, Source, TypedSchematicContext } from '@angular-devkit/schematics';
import { Url } from 'url';
import { FileSystemCollectionDescription, FileSystemSchematicDescription } from './description';
/**
 * Used to simplify typings.
 */
export declare type FileSystemCollection = Collection<FileSystemCollectionDescription, FileSystemSchematicDescription>;
export declare type FileSystemCollectionDesc = CollectionDescription<FileSystemCollectionDescription>;
export declare type FileSystemSchematicDesc = SchematicDescription<FileSystemCollectionDescription, FileSystemSchematicDescription>;
export declare type FileSystemSchematicContext = TypedSchematicContext<FileSystemCollectionDescription, FileSystemSchematicDescription>;
export declare type OptionTransform<T extends object, R extends object> = (schematic: FileSystemSchematicDescription, options: T) => R;
/**
 * A EngineHost base class that uses the file system to resolve collections. This is the base of
 * all other EngineHost provided by the tooling part of the Schematics library.
 */
export declare abstract class FileSystemEngineHostBase implements EngineHost<FileSystemCollectionDescription, FileSystemSchematicDescription> {
    protected abstract _resolveCollectionPath(name: string): string | null;
    protected abstract _resolveReferenceString(name: string, parentPath: string): {
        ref: RuleFactory<{}>;
        path: string;
    } | null;
    protected abstract _transformCollectionDescription(name: string, desc: Partial<FileSystemCollectionDesc>): FileSystemCollectionDesc | null;
    protected abstract _transformSchematicDescription(name: string, collection: FileSystemCollectionDesc, desc: Partial<FileSystemSchematicDesc>): FileSystemSchematicDesc | null;
    private _transforms;
    listSchematics(collection: FileSystemCollection): string[];
    registerOptionsTransform<T extends object, R extends object>(t: OptionTransform<T, R>): void;
    /**
     *
     * @param name
     * @return {{path: string}}
     */
    createCollectionDescription(name: string): FileSystemCollectionDesc | null;
    createSchematicDescription(name: string, collection: FileSystemCollectionDesc): FileSystemSchematicDesc | null;
    createSourceFromUrl(url: Url): Source | null;
    transformOptions<OptionT extends object, ResultT extends object>(schematic: FileSystemSchematicDesc, options: OptionT): ResultT;
    getSchematicRuleFactory<OptionT extends object>(schematic: FileSystemSchematicDesc, _collection: FileSystemCollectionDesc): RuleFactory<OptionT>;
}
