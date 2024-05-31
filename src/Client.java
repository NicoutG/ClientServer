public class Client {
    private static ClientServer server=new ClientServer();
    public static void main(String[] args) throws Exception { 
        server.connectToServer("localhost", 9876);
        if (server.isConnected()) {
            System.out.println("Connected");
            server.send("Hello");
            String response=server.receiveString();
            System.out.println("Server sent : "+response);
        }
    }
}
