export declare class WebpackResourceLoader {
    private _parentCompilation;
    private _context;
    private _uniqueId;
    constructor(_parentCompilation: any);
    private _compile(filePath, _content);
    private _evaluate(fileName, source);
    get(filePath: string): Promise<string>;
}
