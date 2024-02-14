import { TailPipe } from './tail.pipe';


describe('TailPipe', () => {
    
    let pipe: TailPipe;
    
    beforeEach(() => {
       pipe = new TailPipe(); 
    });
    
    it('Should return []', () => {
       
       expect(pipe.transform([])).toEqual([]); 
    });
    
    it('Should return [2]', () => {
       
       expect(pipe.transform([1, 2])).toEqual([2]); 
    });
    
    it ('Should return [2, 3]', () => {
       
       expect(pipe.transform([1, 2, 3])).toEqual([2, 3]); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });
    
});