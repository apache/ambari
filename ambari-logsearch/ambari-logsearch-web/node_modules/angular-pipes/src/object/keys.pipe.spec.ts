import { KeysPipe } from './keys.pipe';
        


describe('KeysPipe', () => {
    
    let pipe: KeysPipe;
    
    beforeEach(() => {
       pipe = new KeysPipe(); 
    });
    
    const value = {
        a: 1,
        b: 2,
        c: 3
    };
    
    it ('should return the keys of the object', () => {
       
       expect(pipe.transform(value)).toEqual(['a', 'b', 'c']);
    });
    
    it ('should return the keys of the array', () => {
       
       expect(pipe.transform([1, 2, 3])).toEqual(['0', '1', '2']);
    });
    
    
    it ('should return the input unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a');
    });
    
});
