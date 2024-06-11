import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;

/**
* class to facilitate exchanges between clients and servers
*/

public class ClientServer {

    /**
     * socket to send and receive messages
     */
    private DatagramSocket socket=null;

    /**
     * the address of the client or server being communicated with
     */
    private InetAddress addrCom=null;

    /**
     * the port of the client or server being communicated with
     */
    private int portCom=-1;

    /**
     * if we are connected to a client or a server
     */
    private boolean connected=false;

    /**
     * packet to receive messages
     */
    private DatagramPacket receivedPacket=null;

    /**
     * unread messages received
     */
    private Vector <DatagramPacket> messages=new Vector <DatagramPacket> ();

    /**
     * class constructor
     */
    public ClientServer () {
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            System.out.println("Impossible to create a socket");
            e.printStackTrace();
        }
    }

    /**
     * class constructor
     * @param portList the port number used to receive messages
     */
    public ClientServer (int portList) {
        try {
            socket = new DatagramSocket(portList);
        } catch (Exception e) {
            System.out.println("Impossible to create a socket on the port "+portList);
            e.printStackTrace();
        }
    }

    /**
     * acessor of connect
     * @return boolean true if we are connected to client or a server, false otherwise
     */
    public boolean isConnected () {
        return connected;
    }

    /**
     * acessor of addrCom
     * @return InetAddress the address of the client or server being communicated with
     */
    public InetAddress getAddressCom () {
        if (connected)
            return addrCom;
        return null;
    }

    /**
     * returns the address of the last message
     * @return InetAddress the address of the last message
     */
    public InetAddress getLastAddress () {
        if (receivedPacket!=null)
            return receivedPacket.getAddress();
        return null;
    }

    /**
     * returns the port number used to receive messages
     * @return int the port number used to receive messages
     */
    public int getLocalPort () {
        if (socket!=null && !socket.isClosed())
            return socket.getLocalPort();
        return -1;
    }

    /**
     * acessor of portCom
     * @return int the port of the client or server being communicated with
     */
    public int getPortCom () {
        if (connected)
            return portCom;
        return -1;
    }

    /**
     * returns the port of the last message
     * @return int the port of the last message
     */
    public int getLastPort () {
        if (receivedPacket!=null)
            return receivedPacket.getPort();
        return -1;
    }

    /**
     * sends a connection request to a server and waits for a response before being redirected to another port
     * @param addrServ the server address
     * @param portServ the server port
     */
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
                    buffer=Arrays.copyOfRange(receivedPacket.getData(), 5, receivedPacket.getLength());
                    response=byteToString(buffer);
                }while(!addrServ.equals(getLastAddress()) || getLastPort()!=portServ || response.length()<=23 || !response.substring(0,23).equals("</%/connectionReply/%/>"));
                addrCom=addrServ;
                String portStr=response.substring(23).trim();
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

    /**
     * sends a connection request to a server and waits for a response before being redirected to another port
     * @param addrServ the server's domain name
     * @param portServ the server port
     */
    public void connectToServer (String addrServ, int portServ) {
        try {
            connectToServer(InetAddress.getByName(addrServ),portServ);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addrServ);
            e.printStackTrace();
        }
    }

    /**
     * connects to a client
     * @param addrClient the client's domain name
     * @param portClient the client port
     */
    public void connectToClient (InetAddress addrClient, int portClient) {
        addrCom=addrClient;
        portCom=portClient;
        connected=true;
    }

    /**
     * waits for a connection request from a client and sends a response with a port number
     * @param addrServ the server's domain name
     * @param portServ the server port
     */
    public void waitClient (int port) {
        String message="";
        while (message.length()!=25 || !message.equals("</%/connectionRequest/%/>")) {
            message=receiveString();
        }
        send(receivedPacket.getAddress(),receivedPacket.getPort(),"</%/connectionReply/%/>"+port);
    }

    /**
     * sends a disconnection message to the server and interpompts the communication
     */
    public void disconnect () {
        if (connected) {
            send("</%/disconnection/%/>");
            connected=false;
        }
    }

    /**
     * returns true if the message is a deconnection message, false otherwise
     * @param message the message to compare
     * @return true if the message is a deconnection message, false otherwise
     */
    public boolean isDeconnectionMessage (String message) {
        return message.equals("</%/disconnection/%/>");
    }

    /**
     * sends bytes
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message bytes to send
     */
    public void send (InetAddress addr, int port, byte [] message) {
        byte[] buffer=new byte [message.length+1];
        buffer[0]=80;
        System.arraycopy(message, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends bytes to the client or server to which we are connected
     * @param message bytes to send
     */
    public void send(byte [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends a String
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send
     */
    public void send (InetAddress addr, int port, String message) {
        byte [] bufferPrep=StringToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=81;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends a String
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send
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
     * sends a String to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send
     */
    public void send(String message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends an int
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int to send
     */
    public void send (InetAddress addr, int port, int message) {
        byte [] bufferPrep=intToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=82;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends an int
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int to send
     */
    public void send (String addr, int port, int message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * sends an int to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int to send
     */
    public void send(int message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends a boolean
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean to send
     */
    public void send (InetAddress addr, int port, boolean message) {
        byte [] bufferPrep=booleanToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=83;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends a boolean
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean to send
     */
    public void send (String addr, int port, boolean message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * sends a boolean to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean to send
     */
    public void send(boolean message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends a double
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double to send
     */
    public void send (InetAddress addr, int port, double message) {
        byte [] bufferPrep=doubleToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=84;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends a double
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double to send
     */
    public void send (String addr, int port, double message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * sends a double to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double to send
     */
    public void send(double message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends an array of ints
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send
     */
    public void send (InetAddress addr, int port, int [] message) {
        byte [] bufferPrep=intArrayToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=85;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends an array of ints
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send
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
     * sends an array of ints to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message int array to send
     */
    public void send(int [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends a boolean array
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send
     */
    public void send (InetAddress addr, int port, boolean [] message) {
        byte [] bufferPrep=booleanArrayToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=86;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends a boolean array
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send
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
     * sends an array of booleans to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send
     */
    public void send(boolean [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * sends an array of doubles
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send
     */
    public void send (InetAddress addr, int port, double [] message) {
        byte [] bufferPrep=doubleArrayToByte(message);
        byte[] buffer=new byte [bufferPrep.length+1];
        buffer[0]=87;
        System.arraycopy(bufferPrep, 0, buffer, 1, buffer.length-1);
        sendMessages(addr,port,buffer);
    }

    /**
     * sends an array of doubles
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send
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
     * sends an array of doubles to the client or server to which we are connected
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message double array to send
     */
    public void send(double [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * waits for bytes to be received
     * @return byte [] the message received
     */
    public byte [] receiveBytes () {
        return receiveBytes(0);
    }

    /**
     * waits for bytes to be received
     * @param ms maximum waiting time
     * @return byte [] the message received
     */
    public byte [] receiveBytes (int ms) {
        byte[] res = null;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            res=Arrays.copyOfRange(buffer, 1, buffer.length);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for a String to be received
     * @return String the message received
     */
    public String receiveString () {
        return receiveString(0);
    }

    /**
     * waits for a String to be received
     * @param ms maximum waiting time
     * @return String the message received
     */
    public String receiveString (int ms) {
        String res = "";
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("String"))
                res=byteToString(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the String because the message is of type "+getType(type)+type);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for an int to be received
     * @return int the message received
     */
    public int receiveInt () {
        return receiveInt(0);
    }

    /**
     * waits for an int to be received
     * @param ms maximum waiting time
     * @return int the message received
     */
    public int receiveInt (int ms) {
        int res = 0;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("int"))
                res=byteToInt(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the int because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for a boolean to be received
     * @return boolean the message received
     */
    public boolean receiveBoolean () {
        return receiveBoolean(0);
    }

    /**
     * waits for a boolean to be received
     * @param ms maximum waiting time
     * @return boolean the message received
     */
    public boolean receiveBoolean (int ms) {
        boolean res = false;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("boolean"))
                res=byteToBoolean(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the boolean because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for a double to be received
     * @return double the message received
     */
    public double receiveDouble () {
        return receiveDouble(0);
    }

    /**
     * waits for a double to be received
     * @param ms maximum waiting time
     * @return double the message received
     */
    public double receiveDouble (int ms) {
        double res = 0;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("double"))
                res=byteToDouble(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the double because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for ints to be received
     * @return int [] the message received
     */
    public int [] receiveIntArray () {
        return receiveIntArray(0);
    }

    /**
     * waits for ints to be received
     * @param ms maximum waiting time
     * @return int [] the message received
     */
    public int [] receiveIntArray (int ms) {
        int[] res = null;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("int []"))
                res=byteToIntArray(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the int [] because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for booleans to be received
     * @return boolean [] the message received
     */
    public boolean [] receiveBooleanArray () {
        return receiveBooleanArray(0);
    }

    /**
     * waits for booleans to be received
     * @param ms maximum waiting time
     * @return boolean [] the message received
     */
    public boolean [] receiveBooleanArray (int ms) {
        boolean[] res = null;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("int"))
                res=byteToBooleanArray(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the boolean [] because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for doubles to be received
     * @return double [] the message received
     */
    public double [] receiveDoubleArray () {
        return receiveDoubleArray(0);
    }

    /**
     * waits for doubles to be received
     * @param ms maximum waiting time
     * @return double [] the message received
     */
    public double [] receiveDoubleArray (int ms) {
        double[] res = null;
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            if (getType(type).equals("double []"))
                res=byteToDoubleArray(Arrays.copyOfRange(buffer, 1, buffer.length));
            else
                System.out.println("Impossible to receive the double [] because the message is of type "+getType(type));
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for an Object to be received
     * @return Object the message received
     */
    public Object receiveObject () {
        return receiveObject(0);
    }

    /**
     * waits for an Object to be received
     * @param ms maximum waiting time
     * @return Object the message received
     */
    public Object receiveObject (int ms) {
        if (0<=ms) {
            byte[] buffer=receiveMessages(ms);
            int type=buffer[0];
            byte[] res=Arrays.copyOfRange(buffer, 1, buffer.length);
            switch (getType(type)) {
                case "byte []":return res;
                case "String":return byteToString(res);
                case "int":return byteToInt(res);
                case "boolean":return byteToBoolean(res);
                case "double":return byteToDouble(res);
                case "int []":return byteToIntArray(res);
                case "boolean []":return byteToBooleanArray(res);
                case "double []":return byteToDoubleArray(res);
                default: System.out.println("Impossible to receive the object because the message is of type unknown");
            }
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    /**
     * converts a String into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] StringToByte (String message) {
        return message.getBytes();
    }

    /**
     * converts bytes into a String
     * @param buffer the message to convert
     * @return String the converted message
     */
    public String byteToString (byte[] buffer) {
        return new String(buffer, 0, buffer.length);
    }

    /**
     * converts an int into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] intToByte (int message) {
        byte[] buffer = new byte[Integer.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putInt(message);
        return buffer;
    }

    /**
     * converts bytes into an int
     * @param buffer the message to convert
     * @return int the converted message
     */
    public int byteToInt (byte[] buffer) {
        if (buffer.length!=Integer.BYTES) {
            System.out.println("Impossible to convert the message to int");
            return 0;
        }
        return ByteBuffer.wrap(buffer).getInt();
    }

    /**
     * converts a boolean into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] booleanToByte (boolean message) {
        byte[] buffer = new byte[1];
        buffer[0]=(byte)(message ? 1 : 0);
        return buffer;
    }

    /**
     * converts bytes into a boolean
     * @param buffer the message to convert
     * @return boolean the converted message
     */
    public boolean byteToBoolean (byte[] buffer) {
        if (buffer.length!=1) {
            System.out.println("Impossible to convert the message to boolean");
            return false;
        }
        return buffer[0]!=0;
    }

    /**
     * converts a double into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] doubleToByte (double message) {
        byte[] buffer = new byte[Double.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putDouble(message);
        return buffer;
    }

    /**
     * converts bytes into a double
     * @param buffer the message to convert
     * @return double the converted message
     */
    public double byteToDouble (byte[] buffer) {
        if (buffer.length!=Double.BYTES) {
            System.out.println("Impossible to convert the message to double");
            return 0;
        }
        return ByteBuffer.wrap(buffer).getDouble();
    }

    /**
     * converts ints into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] intArrayToByte (int [] message) {
        byte[] buffer = new byte[Integer.BYTES*message.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i=0;i<message.length;i++)
            byteBuffer.putInt(message[i]);
        return buffer;
    }

    /**
     * converts bytes into ints
     * @param buffer the message to convert
     * @return int [] the converted message
     */
    public int [] byteToIntArray (byte[] buffer) {
        if (buffer.length%Integer.BYTES!=0) {
            System.out.println("Impossible to convert the message to int []");
            return null;
        }
        int [] res=new int [buffer.length/Integer.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, buffer.length);
        for (int i=0;i<res.length;i++)
            res[i] = byteBuffer.getInt();
        return res;
    }

    /**
     * converts booleans into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] booleanArrayToByte (boolean [] message) {
        byte[] res = new byte [1+(message.length+7)/8];
        res[0]=(byte)(1+(message.length-1)%8); // number of booleans in the last byte
        for (int i = 0; i < message.length; i++)
            if (message[i])
                res[1+i/8] |= (1 << (i % 8));
        return res;
    }

    /**
     * converts bytes into booleans
     * @param buffer the message to convert
     * @return boolean [] the converted message
     */
    public boolean [] byteToBooleanArray (byte[] buffer) {
        if (buffer.length<=1) {
            System.out.println("Impossible to convert the message to boolean []");
            return null;
        }
        int lengthLast = buffer[0]; // number of booleans in the last byte
        if (lengthLast<1 || 8<lengthLast) {
            System.out.println("Impossible to convert the message to boolean []");
            return null;
        }
        boolean [] res=new boolean [8*(buffer.length-2)+lengthLast];
        for (int i=0;i<res.length;i++)
            res[i]=( (buffer[1+i/8] & (1 << (i%8)))!=0 );
        return res;
    }

    /**
     * converts doubles into bytes
     * @param message the message to convert
     * @return byte [] the converted message
     */
    public byte[] doubleArrayToByte (double [] message) {
        byte[] buffer = new byte[Double.BYTES*message.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i=0;i<message.length;i++)
            byteBuffer.putDouble(message[i]);
        return buffer;
    }

    /**
     * converts bytes into doubles
     * @param buffer the message to convert
     * @return double [] the converted message
     */
    public double [] byteToDoubleArray (byte[] buffer) {
        if (buffer.length%Double.BYTES!=0) {
            System.out.println("Impossible to convert the message to double []");
            return null;
        }
        double [] res=new double [buffer.length/Double.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, buffer.length);
        for (int i=0;i<res.length;i++)
            res[i] = byteBuffer.getDouble();
        return res;
    }

    /**
     * converts the number into an object type
     * @param type the number to convert
     * @return String the corresponding type
     */
    private String getType (int type) {
        switch (type) {
            case 80:return "byte []";
            case 81:return "String";
            case 82:return "int";
            case 83:return "boolean";
            case 84:return "double";
            case 85:return "int []";
            case 86:return "boolean []";
            case 87:return "double []";
            default: return "unknown";
        }
    } 

    /**
     * sends all bytes
     * @param addr recipient's address
     * @param port the recipient's port
     * @param byte bytes to send
     */
    private void sendMessages (InetAddress addr, int port, byte[] buffer) {
        int length=0;
        int space=Math.min(buffer.length,1020);
        byte[] buffer2=new byte [4+space];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer2);
        byteBuffer.putInt(buffer.length);
        System.arraycopy(buffer, 0, buffer2, 4, space);
        sendMessage(addr,port,buffer2);
        length+=space;
        space=Math.min(buffer.length-length,1024);

        while (0<space) {
            buffer2=new byte[space];
            System.arraycopy(buffer, length, buffer2, 0, space);
            sendMessage(addr,port,buffer2);
            length+=space;
            space=Math.min(buffer.length-length,1024);
        }
    }

    /**
     * sends bytes (max length = 1024 bytes)
     * @param addr recipient's address
     * @param port the recipient's port
     * @param byte bytes to send (max length = 1024 bytes)
     */
    private void sendMessage (InetAddress addr, int port, byte[] message) {
        try {
            if (message.length>1024) {
                System.out.println("Warning : Only 1024 bytes sent out of "+message.length);
                message=Arrays.copyOfRange(message, 0, 1024);
            }
            DatagramPacket packet = new DatagramPacket(message, message.length, addr, port);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Impossible to send the message to "+addr+" on the port "+port);
            e.printStackTrace();
        }
    }

    /**
     * waits for all bytes to be received
     * @param ms maximum waiting time
     * @return byte [] the message received
     */
    private byte[] receiveMessages (int ms) {
        byte[] res = null;
        if (0<=ms) {
            byte[] buffer=receiveMessage(ms);
            if (buffer==null)
                return null;
            if (buffer.length<5) {
                System.out.println("Impossible to receive message because it is to short");
                return null;
            }
            InetAddress addr=receivedPacket.getAddress();
            int port=receivedPacket.getPort();
            int length=ByteBuffer.wrap(buffer).getInt();
            res=new byte [length];
            System.arraycopy(buffer, 4, res, 0, buffer.length-4);
            int extracted=buffer.length-4;
            while (extracted<length) {
                buffer=receiveMessageFrom(addr,port,10);
                if (buffer==null) {
                    System.out.println("Impossible to receive message because a part is missing");
                    return null;
                }
                System.arraycopy(buffer, 0, res, extracted, buffer.length);
                extracted+=buffer.length;
            }
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for bytes to be received (max length = 1024 bytes)
     * @param ms maximum waiting time
     * @return byte [] the message received (max length = 1024 bytes)
     */
    private byte [] receiveMessage (int ms) {
        byte[] res = null;
        if (0<=ms) {
            if (0<messages.size()) {
                receivedPacket=messages.get(0);
                messages.remove(0);
                res=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
            }
            else {
                try {
                    socket.setSoTimeout(ms);
                    try {
                        byte[] buffer = new byte[1024];
                        receivedPacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivedPacket);
                        res=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
                        if (res.length<=5) {
                            System.out.println("Impossible to receive message because it is to short");
                            return null;
                        }
                        if (connected && receivedPacket.getAddress().equals(addrCom) && receivedPacket.getPort()==portCom) {
                            if (isDeconnectionMessage(new String(Arrays.copyOfRange(res, 5, receivedPacket.getLength()), 0, res.length-5)))
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
            }
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * waits for the reception of bytes sent by a precise person (max length = 1024 bytes)
     * @param addr the address of the sender
     * @param addr the port of the sender
     * @param ms maximum waiting time
     * @return byte [] the message received (max length = 1024 bytes)
     */
    private byte [] receiveMessageFrom (InetAddress addr, int port, int ms) {
        byte[] res = null;
        if (0<=ms) {
            DatagramPacket oldPacket=null;
            int i=0;
            boolean found=false;
            while(i<messages.size() && !false) {
                oldPacket=messages.get(i);
                found=(oldPacket.getAddress().equals(addr) && oldPacket.getPort()==port);
                i++;
            }
            if (found) {
                receivedPacket=oldPacket;
                messages.remove(oldPacket);
                res=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
            }
            else {
                try {
                    socket.setSoTimeout(ms);
                    try {
                        DatagramPacket newPacket=null;
                        do {
                            byte[] buffer = new byte[1024];
                            newPacket = new DatagramPacket(buffer, buffer.length);
                            socket.receive(newPacket);
                            if (!newPacket.getAddress().equals(addr) || newPacket.getPort()!=port)
                                messages.add(newPacket);
                        }while(!newPacket.getAddress().equals(addr) || newPacket.getPort()!=port);
                        receivedPacket=newPacket;
                        res=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
                        if (res.length<=5) {
                            System.out.println("Impossible to receive message because it is to short");
                            return null;
                        }
                        if (connected && receivedPacket.getAddress().equals(addrCom) && receivedPacket.getPort()==portCom) {
                            if (isDeconnectionMessage(new String(Arrays.copyOfRange(res, 5, receivedPacket.getLength()), 0, res.length-5)))
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
            }
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return res;
    }

    /**
     * class destructor
     */
    protected void finalize() {
        if (socket!=null && !socket.isClosed())
            socket.close();
    }

}