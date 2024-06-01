import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class ClientServer {
    private DatagramSocket socket=null;
    private InetAddress addrCom=null;
    private int portCom=-1;
    private boolean connected=false;
    private DatagramPacket receivedPacket=null;

    public ClientServer () {
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            System.out.println("Impossible to create a socket");
            e.printStackTrace();
        }
    }

    public ClientServer (int portList) {
        try {
            socket = new DatagramSocket(portList);
        } catch (Exception e) {
            System.out.println("Impossible to create a socket on the port "+portList);
            e.printStackTrace();
        }
    }

    public boolean isConnected () {
        return connected;
    }

    public InetAddress getAddressCom () {
        if (connected)
            return addrCom;
        return null;
    }

    public InetAddress getLastAddress () {
        if (receivedPacket!=null)
            return receivedPacket.getAddress();
        return null;
    }

    public int getLocalPort () {
        if (socket!=null && !socket.isClosed())
            return socket.getLocalPort();
        return -1;
    }

    public int getPortCom () {
        if (connected)
            return portCom;
        return -1;
    }

    public int getLastPort () {
        if (receivedPacket!=null)
            return receivedPacket.getPort();
        return -1;
    }

    public void connectToServer (InetAddress addrServ, int portServ) {
        try {
            send(addrServ,portServ,"</%/connectionRequest/%/>");
            String response="";

            // wait a response for 1 second
            socket.setSoTimeout(1000);
            try {
                do {
                    byte[] buffer = new byte[1024];
                    receivedPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivedPacket);
                    response=new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                }while(!addrServ.equals(getLastAddress()) || getLastPort()!=portServ || response.length()<22 || !response.substring(0,22).equals("</%/connectionEcho/%/>"));
                addrCom=addrServ;
                String portStr=response.substring(22).trim();
                portCom=Integer.parseInt(portStr);
                connected=true;
                try { // to avoid bugs
                    Thread.sleep(20);
                } catch (Exception e) {}
            } catch (SocketTimeoutException e) {
                System.out.println("No response from the server "+addrServ+" on the port "+portServ);
            } finally {
                socket.setSoTimeout(0);
            }
        } catch (Exception e) {
            System.out.println("Impossible to connect to the server "+addrServ+" on the port "+portServ);
            e.printStackTrace();
        }
    }

    public void connectToServer (String addrServ, int portServ) {
        try {
            connectToServer(InetAddress.getByName(addrServ),portServ);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addrServ);
            e.printStackTrace();
        }
    }

    public void connectToClient (InetAddress addrClient, int portClient) {
        addrCom=addrClient;
        portCom=portClient;
        connected=true;
    }

    public void waitClient (int port) {
        String message="";
        while (message.length()!=25 || !message.equals("</%/connectionRequest/%/>")) {
            message=receiveString();
        }
        send(receivedPacket.getAddress(),receivedPacket.getPort(),"</%/connectionEcho/%/>"+port);
    }

    public void disconnect () {
        if (connected) {
            send("</%/disconnection/%/>");
            connected=false;
        }
    }

    public boolean isDeconnectionMessage (String message) {
        return message.equals("</%/disconnection/%/>");
    }

    public void send (InetAddress addr, int port, byte [] message) {
        try {
            DatagramPacket packet = new DatagramPacket(message, message.length, addr, port);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Impossible to send the message to "+addr+" on the port "+port);
            e.printStackTrace();
        }
    }

    public void send(byte [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * send a String
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send (max length = 512 characters)
     */
    public void send (InetAddress addr, int port, String message) {
        send(addr,port,message.getBytes());
    }

    /**
     * send a String
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send (max length = 512 characters)
     */
    public void send (String addr, int port, String message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send a String to the server or client we are connected to
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send (max length = 512 characters)
     */
    public void send(String message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    public void send (InetAddress addr, int port, int message) {
        byte[] buffer = new byte[Integer.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putInt(message);
        send(addr,port,buffer);
    }

    public void send (String addr, int port, int message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    public void send(int message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    public void send (InetAddress addr, int port, boolean message) {
        byte[] buffer = new byte[1];
        buffer[0]=(byte)(message ? 1 : 0);
        send(addr,port,buffer);
    }

    public void send (String addr, int port, boolean message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    public void send(boolean message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    public void send (InetAddress addr, int port, double message) {
        byte[] buffer = new byte[Double.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putDouble(message);
        send(addr,port,buffer);
    }

    public void send (String addr, int port, double message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    public void send(double message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * send an array of ints
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send (max length = 256 ints)
     */
    public void send (InetAddress addr, int port, int [] message) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length * Integer.BYTES);
        for (int value : message)
            byteBuffer.putInt(value);
        byte[] buffer = byteBuffer.array();
        send(addr,port,buffer);
    }

    /**
     * send an array of ints
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send (max length = 256 ints)
     */
    public void send (String addr, int port, int [] message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send an array of ints to the server or client we are connected to
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send (max length = 256 ints)
     */
    public void send(int [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * send a boolean array
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send (max length = 8160 booleans)
     */
    public void send (InetAddress addr, int port, boolean [] message) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4+(message.length+7)/8);
        byteBuffer.putInt(message.length); // number of booleans in the array
        byte[] buffer = byteBuffer.array();
        for (int i = 0; i < message.length; i++)
            if (message[i])
                buffer[4+i/8] |= (1 << (i % 8));
        send(addr,port,buffer);
    }

    /**
     * send a boolean array
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send (max length = 8160 booleans)
     */
    public void send (String addr, int port, boolean [] message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send an array of booleans to the server or client we are connected to
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send (max length = 8160 booleans)
     */
    public void send(boolean [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * send an array of doubles
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send (max length = 128 doubles)
     */
    public void send (InetAddress addr, int port, double [] message) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length * Double.BYTES);
        for (double value : message)
            byteBuffer.putDouble(value);
        byte[] buffer = byteBuffer.array();
        send(addr,port,buffer);
    }

    /**
     * send an array of doubles
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send (max length = 128 doubles)
     */
    public void send (String addr, int port, double [] message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send an array of doubles to the server or client we are connected to
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send (max length = 128 doubles)
     */
    public void send(double [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    public byte [] receive () {
        return receive(0);
    }

    public byte [] receive (int ms) {
        byte[] res = null;
        if (0<=ms)
            try {
                socket.setSoTimeout(ms);
                try {
                    byte[] buffer = new byte[1024];
                    receivedPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivedPacket);
                    res=receivedPacket.getData();
                    if (connected && receivedPacket.getAddress().equals(addrCom) && receivedPacket.getPort()==portCom) {
                        if (isDeconnectionMessage(new String(buffer, 0, receivedPacket.getLength())))
                            connected=false;
                    }
                } catch (SocketTimeoutException e) {
                    socket.setSoTimeout(0);
                }finally {
                    socket.setSoTimeout(0);
                }
            } catch (Exception e) {
                System.out.println("Impossible to receive a message");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    public String receiveString () {
        return receiveString(0);
    }

    public String receiveString (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null)
                    return new String(buffer, 0, receivedPacket.getLength());
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to String");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return "";
    }

    public int receiveInt () {
        return receiveInt(0);
    }

    public int receiveInt (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null)
                    return ByteBuffer.wrap(buffer).getInt();
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to int");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return 0;
    }

    public boolean receiveBoolean () {
        return receiveBoolean(0);
    }

    public boolean receiveBoolean (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null)
                    return buffer[0]!=0;
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to boolean");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return false;
    }

    public double receiveDouble () {
        return receiveDouble(0);
    }

    public double receiveDouble (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null)
                    return ByteBuffer.wrap(buffer).getDouble();
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to double");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return 0;
    }

    public int [] receiveIntArray () {
        return receiveIntArray(0);
    }

    public int [] receiveIntArray (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, buffer.length);
                    int[] intArray = new int[receivedPacket.getLength() / Integer.BYTES];
                    for (int i = 0; i < intArray.length; i++)
                        intArray[i] = byteBuffer.getInt();
                    return intArray;
                }
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to int []");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public boolean [] receiveBooleanArray () {
        return receiveBooleanArray(0);
    }

    public boolean [] receiveBooleanArray (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, buffer.length);
                    int length = byteBuffer.getInt(); // number of booleans in the array
                    boolean [] booleanArray=new boolean [length];
                    for (int i=0;i<booleanArray.length;i++)
                        booleanArray[i]=( (buffer[4+i/8] & (1 << (i%8)))!=0 );
                    return booleanArray;
                }
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to boolean []");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public double [] receiveDoubleArray () {
        return receiveDoubleArray(0);
    }

    public double [] receiveDoubleArray (int ms) {
        if (0<=ms)
            try {
                byte[] buffer = receive(ms);
                if (buffer!=null) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, receivedPacket.getLength());
                    double[] doubleArray = new double[receivedPacket.getLength() / Double.BYTES];
                    for (int i = 0; i < doubleArray.length; i++)
                        doubleArray[i] = byteBuffer.getDouble();
                    return doubleArray;
                }
            } catch (Exception e) {
                System.out.println("Impossible to convert the message to double []");
                e.printStackTrace();
            }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    protected void finalize() {
        if (socket!=null && !socket.isClosed())
            socket.close();
    }

}