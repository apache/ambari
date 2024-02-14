import { ShufflePipe } from './shuffle.pipe';


describe('ShufflePipe', () => {
    
    let pipe: ShufflePipe;
    
    beforeEach(() => {
       pipe = new ShufflePipe(); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });

    it('Should be different', () => {
        
        const array = [];
        for (let i = 0; i < 100; ++i) { array[i] = i }
        expect(pipe.transform(array)).not.toEqual(array);
    });
    
});