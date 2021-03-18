import jade.Boot;

public class SmartGridRunner {
    public static void main(String[] args) {
        String[] param = new String[ 7 ];
        param[ 0 ] = "-gui";
        param[ 1 ] = "-name";
        param[ 2 ] = "the-platform";
        param[ 3 ] = "-agents";
        param[ 4 ] = "tony:HelloWorldAgent";
        param[ 5 ] = "-port";
        param[ 6 ] = "0";
        try {
            Boot.main(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
