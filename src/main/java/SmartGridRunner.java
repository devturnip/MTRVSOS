import jade.Boot;

public class SmartGridRunner {
    public static void main(String[] args) {
        String[] param = new String[ 5 ];
        param[ 0 ] = "-gui";
        param[ 1 ] = "-name";
        param[ 2 ] = "the-platform";
        param[ 3 ] = "-agents";
        param[ 4 ] = "tony:HelloWorldAgent";

        Boot.main(param);
    }
}
