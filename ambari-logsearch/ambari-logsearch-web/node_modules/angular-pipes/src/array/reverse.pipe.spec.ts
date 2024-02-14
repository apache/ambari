import { ReversePipe } from './reverse.pipe';


describe('ReversePipe', () => {
    
    let pipe: ReversePipe;
    
    beforeEach(() => {
       pipe = new ReversePipe(); 
    });
    
    it('Should return the reversed array', () => {
       
       const value = [1, 2, 3];
       
       expect(pipe.transform(value)).toEqual([3, 2, 1]); 
       expect(value).toEqual([1, 2, 3])
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });
    
});