import { SomePipe } from './some.pipe';


describe('MapPipe', () => {
    
    let pipe: SomePipe;
    
    const fn = function (item: any) {
        return item === 2;
    };
    
    beforeEach(() => {
       pipe = new SomePipe(); 
    });
    
    it('Should return true', () => {
       
       const array = [0, 1, 2, 3];
       
       expect(pipe.transform(array, fn)).toEqual(true); 
       expect(array).toEqual([0, 1, 2, 3]); // Check integrity
    });
    
    it('Should return false', () => {
       
       expect(pipe.transform([1,3], fn)).toEqual(false); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', null)).toEqual('a'); 
    });
    
})