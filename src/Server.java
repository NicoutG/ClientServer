import java.net.InetAddress;

public class Server {
    private static ClientServer clients=new ClientServer(9876);
    private static int portCommunication=9877;
    
    public static void main(String[] args) throws Exception {
        while (true) {
            clients.waitClient(portCommunication);

            InetAddress addrClient=clients.getLastAddress();
            int portClient=clients.getLastPort();
            int portListener=portCommunication;
            Thread newClient = new Thread(() -> {
                treatClient(addrClient, portClient, portListener);
            });
            newClient.start();

            portCommunication++;
        }
    }

    private static void treatClient (InetAddress addrClient, int portClient, int portListener) {
        System.out.println("New client "+addrClient+" on "+portClient);
        ClientServer client=new ClientServer(portListener);

        client.connectToClient(addrClient, portClient);
        if (client.isConnected()) {
            boolean clientConnected=true;
            while (clientConnected) {
                String receivedMessage=client.receiveString();
                if (receivedMessage.equals("</%/disconnection/%/>"))
                    clientConnected=false;
                else {
                    System.out.println("Client sent : "+receivedMessage);
                    client.send("Received");
                }
            }
            System.out.println("Deconnection client "+addrClient+" on "+portClient);
        }
    }
}
