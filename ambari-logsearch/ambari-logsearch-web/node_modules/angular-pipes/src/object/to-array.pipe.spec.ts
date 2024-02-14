import { ToArrayPipe } from './to-array.pipe';
        


describe('ToArrayPipe', () => {
    
    let pipe: ToArrayPipe;
    
    beforeEach(() => {
       pipe = new ToArrayPipe(); 
    });
    
    const value = {
        a: 1,
        b: 2,
        c: 3
    };
    
    it ('should transform the object to an array', () => {
       
       expect(pipe.transform(value)).toEqual([1, 2, 3]);
    });
    
    it ('should return the input unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a');
    });
    
});
