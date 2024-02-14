import { ReplacePipe } from './replace.pipe';
        


describe('ReplacePipe', () => {
    
    let pipe: ReplacePipe;
    
    beforeEach(() => {
       pipe = new ReplacePipe(); 
    });
    
    
    it('Should return the replaced string', () => {
       
        const value = 'aaa';
        const result = pipe.transform(value, 'aaa', 'bbb');
        expect(result).toEqual('bbb');
        expect(value).toEqual('aaa');
    });
    
    
    it('Should return the replaced string #2', () => {
       
        const result = pipe.transform('aaa', /a/g, 'b');
        expect(result).toEqual('bbb');
    });
    
    
    it('Should return the replaced string #3', () => {
       
        const result = pipe.transform('abcdeabcde', /a/g, 'f');
        expect(result).toEqual('fbcdefbcde');
    });
    
    // Just use replace function ...
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null, null)).toEqual(1); 
    });
   
});