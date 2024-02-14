/**
 * Represents a meta element.
 *
 * @experimental
 */
export declare type MetaDefinition = {
    charset?: string;
    content?: string;
    httpEquiv?: string;
    id?: string;
    itemprop?: string;
    name?: string;
    property?: string;
    scheme?: string;
    url?: string;
} & {
    [prop: string]: string;
};
/**
 * A service that can be used to get and add meta tags.
 *
 * @experimental
 */
export declare class Meta {
    private _doc;
    private _dom;
    constructor(_doc: any);
    addTag(tag: MetaDefinition, forceCreation?: boolean): HTMLMetaElement | null;
    addTags(tags: MetaDefinition[], forceCreation?: boolean): HTMLMetaElement[];
    getTag(attrSelector: string): HTMLMetaElement | null;
    getTags(attrSelector: string): HTMLMetaElement[];
    updateTag(tag: MetaDefinition, selector?: string): HTMLMetaElement | null;
    removeTag(attrSelector: string): void;
    removeTagElement(meta: HTMLMetaElement): void;
    private _getOrCreateElement(meta, forceCreation?);
    private _setMetaElementAttributes(tag, el);
    private _parseSelector(tag);
    private _containsAttributes(tag, elem);
}
