package rose.mary.trace;



public class TestMain {
    


    public static void main(String[] args){
        String test = "한글1234567890qwertyuiop";
        byte[] bs = test.getBytes();
        for(byte b : bs){
            System.out.printf("%x ", b);
        }
    }

}
