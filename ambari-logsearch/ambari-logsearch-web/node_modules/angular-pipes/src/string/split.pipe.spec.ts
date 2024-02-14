import { SplitPipe } from './split.pipe';
        


describe('SplitPipe', () => {
    
    let pipe: SplitPipe;
    
    beforeEach(() => {
       pipe = new SplitPipe(); 
    });
    
    
    it('Should return splitted string as an array', () => {
       
        const result = pipe.transform('abc', 'b');
        expect(result).toEqual(['a', 'c']);
    });
    
    it('Should return splitted string as an array', () => {
       
        const result = pipe.transform('abc', 'b', 0);
        expect(result).toEqual([]);
    });
    
    it('Should return the value unchanged', () => {
       
       expect((<any>pipe).transform(1, [null])).toEqual(1); 
    });
   
});