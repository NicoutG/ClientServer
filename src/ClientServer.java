import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
                    buffer=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
                    response=byteToString(buffer);
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
     * @param message String to send (max length = 1024 characters)
     */
    public void send (InetAddress addr, int port, String message) {
        byte[] buffer=StringToByte(message);
        if (buffer.length>1024) {
            System.out.println("Warning : Only 1024 characters sent out of "+message.length());
            buffer=Arrays.copyOfRange(buffer, 0, 1024);
        }
        send(addr,port,buffer);
    }

    /**
     * send a String
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message String to send (max length = 1024 characters)
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
     * @param message String to send (max length = 1024 characters)
     */
    public void send(String message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    public void send (InetAddress addr, int port, int message) {
        byte[] buffer = intToByte(message);
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
        byte[] buffer = booleanToByte(message);
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
        byte[] buffer = doubleToByte(message);
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

    public void send (InetAddress addr, int port, float message) {
        byte[] buffer = floatToByte(message);
        send(addr,port,buffer);
    }

    public void send (String addr, int port, float message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    public void send(float message) {
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
        byte[] buffer = intArrayToByte(message);
        if (buffer.length>1024) {
            System.out.println("Warning : Only 256 ints sent out of "+message.length);
            buffer=Arrays.copyOfRange(buffer, 0, 1024);
        }
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
     * @param message boolean array to send (max length = 8184 booleans)
     */
    public void send (InetAddress addr, int port, boolean [] message) {
        byte[] buffer = booleanArrayToByte(message);
        if (buffer.length>1024) {
            System.out.println("Warning : Only 8184 booleans sent out of "+message.length);
            buffer=Arrays.copyOfRange(buffer, 0, 1024);
            buffer[0]=8; // 8 booleans in the last byte
        }
        send(addr,port,buffer);
    }

    /**
     * send a boolean array
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message boolean array to send (max length = 8184 booleans)
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
     * @param message boolean array to send (max length = 8184 booleans)
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
        byte[] buffer = doubleArrayToByte(message);
        if (buffer.length>1024) {
            System.out.println("Warning : Only 128 doubles sent out of "+message.length);
            buffer=Arrays.copyOfRange(buffer, 0, 1024);
        }
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

    /**
     * send an array of floats
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message float array to send (max length = 256 floats)
     */
    public void send (InetAddress addr, int port, float [] message) {
        byte[] buffer = floatArrayToByte(message);
        if (buffer.length>1024) {
            System.out.println("Warning : Only 256 floats sent out of "+message.length);
            buffer=Arrays.copyOfRange(buffer, 0, 1024);
        }
        send(addr,port,buffer);
    }

    /**
     * send an array of floats
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message float array to send (max length = 256 floats)
     */
    public void send (String addr, int port, float [] message) {
        try {
            send(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send an array of floats to the server or client we are connected to
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message float array to send (max length = 256 floats)
     */
    public void send(float [] message) {
        if (connected)
            send(addrCom,portCom,message);
        else
            System.out.println("Impossible to send the message because you're not connected");
    }

    /**
     * send an object (must use the receiveObject function to receive it)
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message Object to send (max length = 1023 bytes (first byte to identify the object type) )
     */
    public void sendObject (InetAddress addr, int port, Object message) {
        byte[] buffer;
        if (message instanceof String) {
            buffer=StringToByte((String)message);
            if (buffer.length>1023) {
                System.out.println("Warning : Only 1023 characters sent out of "+((String)message).length());
                buffer=Arrays.copyOfRange(buffer, 0, 1023);
            }
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=80;
            send(addr,port,buffer2);
        } else if (message instanceof Integer) {
            buffer=intToByte((int)message);
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=81;
            send(addr,port,buffer2);
        } else if (message instanceof Boolean) {
            buffer=booleanToByte((boolean)message);
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=82;
            send(addr,port,buffer2);
        } else if (message instanceof Double) {
            buffer=doubleToByte((double)message);
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=83;
            send(addr,port,buffer2);
        } else if (message instanceof Float) {
            buffer=floatToByte((float)message);
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=84;
            send(addr,port,buffer2);
        } else if (message instanceof int[]) {
            buffer=intArrayToByte((int[])message);
            if (buffer.length>1023) {
                System.out.println("Warning : Only 255 ints sent out of "+((int[])message).length);
                buffer=Arrays.copyOfRange(buffer, 0, 1024-Integer.BYTES);
            }
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=85;
            send(addr,port,buffer2);
        } else if (message instanceof boolean[]) {
            buffer=booleanArrayToByte((boolean[])message);
            if (buffer.length>1023) {
                System.out.println("Warning : Only 8176 booleans sent out of "+((boolean[])message).length);
                buffer[0]=8; // 8 booleans in the last byte
                buffer=Arrays.copyOfRange(buffer, 0, 1023);
            }
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=86;
            send(addr,port,buffer2);
        } else if (message instanceof double[]) {
            buffer=doubleArrayToByte((double[])message);
            if (buffer.length>1023) {
                System.out.println("Warning : Only 127 doubles sent out of "+((double[])message).length);
                buffer=Arrays.copyOfRange(buffer, 0, 1024-Double.BYTES);
            }
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=87;
            send(addr,port,buffer2);
        } else if (message instanceof float[]) {
            buffer=floatArrayToByte((float[])message);
            if (buffer.length>1023) {
                System.out.println("Warning : Only 255 floats sent out of "+((float[])message).length);
                buffer=Arrays.copyOfRange(buffer, 0, 1024-Float.BYTES);
            }
            byte[] buffer2=new byte[buffer.length+1];
            System.arraycopy(buffer, 0, buffer2, 1, buffer.length);
            buffer2[0]=88;
            send(addr,port,buffer2);
        } else
            System.out.println("Impossible to send this type of object");
    }

    /**
     * send an object (must use the receiveObject function to receive it)
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message Object to send (max length = 1023 bytes (first byte to identify the object type) )
     */
    public void sendObject (String addr, int port, Object message) {
        try {
            sendObject(InetAddress.getByName(addr),port,message);
        } catch (Exception e) {
            System.out.println("Impossible to find the address of "+addr);
            e.printStackTrace();
        }
    }

    /**
     * send an object to the server or client we are connected to (must use the receiveObject function to receive it)
     * @param addr recipient's address
     * @param port the recipient's port
     * @param message Object to send (max length = 1023 bytes (first byte to identify the object type) )
     */
    public void sendObject(Object message) {
        if (connected)
            sendObject(addrCom,portCom,message);
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
                    res=Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
                    if (connected && receivedPacket.getAddress().equals(addrCom) && receivedPacket.getPort()==portCom) {
                        if (isDeconnectionMessage(new String(res, 0, res.length)))
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
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToString(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return "";
    }

    public int receiveInt () {
        return receiveInt(0);
    }

    public int receiveInt (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToInt(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return 0;
    }

    public boolean receiveBoolean () {
        return receiveBoolean(0);
    }

    public boolean receiveBoolean (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToBoolean(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return false;
    }

    public double receiveDouble () {
        return receiveDouble(0);
    }

    public double receiveDouble (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToDouble(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return 0;
    }

    public float receiveFloat () {
        return receiveFloat(0);
    }

    public float receiveFloat (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToFloat(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return 0;
    }

    public int [] receiveIntArray () {
        return receiveIntArray(0);
    }

    public int [] receiveIntArray (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToIntArray(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public boolean [] receiveBooleanArray () {
        return receiveBooleanArray(0);
    }

    public boolean [] receiveBooleanArray (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToBooleanArray(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public double [] receiveDoubleArray () {
        return receiveDoubleArray(0);
    }

    public double [] receiveDoubleArray (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToDoubleArray(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public float [] receiveFloatArray () {
        return receiveFloatArray(0);
    }

    public float [] receiveFloatArray (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null)
                return byteToFloatArray(buffer);
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public Object receiveObject () {
        return receiveObject(0);
    }

    public Object receiveObject (int ms) {
        if (0<=ms) {
            byte[] buffer = receive(ms);
            if (buffer!=null && buffer.length>1) {
                byte[] buffer2=Arrays.copyOfRange(buffer, 1, buffer.length);
                switch (buffer[0]) {
                    case 80: return byteToString(buffer2);
                    case 81: return byteToInt(buffer2);
                    case 82: return byteToBoolean(buffer2);
                    case 83: return byteToDouble(buffer2);
                    case 84: return byteToFloat(buffer2);
                    case 85: return byteToIntArray(buffer2);
                    case 86: return byteToBooleanArray(buffer2);
                    case 87: return byteToDoubleArray(buffer2);
                    case 88: return byteToFloatArray(buffer2);
                    default: System.out.println("Impossible to receive the message because the message type is unknown (the message must be sent using the sendObject function)");
                }
            }
        }
        else
            System.out.println("Impossible to receive message because ms isn't positive");
        return null;
    }

    public byte[] StringToByte (String message) {
        return message.getBytes();
    }

    public String byteToString (byte[] buffer) {
        return new String(buffer, 0, buffer.length);
    }

    public byte[] intToByte (int message) {
        byte[] buffer = new byte[Integer.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putInt(message);
        return buffer;
    }

    public int byteToInt (byte[] buffer) {
        if (buffer.length!=Integer.BYTES) {
            System.out.println("Impossible to convert the message to int");
            return 0;
        }
        return ByteBuffer.wrap(buffer).getInt();
    }

    public byte[] booleanToByte (boolean message) {
        byte[] buffer = new byte[1];
        buffer[0]=(byte)(message ? 1 : 0);
        return buffer;
    }

    public boolean byteToBoolean (byte[] buffer) {
        if (buffer.length!=1) {
            System.out.println("Impossible to convert the message to boolean");
            return false;
        }
        return buffer[0]!=0;
    }

    public byte[] doubleToByte (double message) {
        byte[] buffer = new byte[Double.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putDouble(message);
        return buffer;
    }

    public double byteToDouble (byte[] buffer) {
        if (buffer.length!=Double.BYTES) {
            System.out.println("Impossible to convert the message to double");
            return 0;
        }
        return ByteBuffer.wrap(buffer).getDouble();
    }

    public byte[] floatToByte (float message) {
        byte[] buffer = new byte[Float.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putFloat(message);
        return buffer;
    }

    public float byteToFloat (byte[] buffer) {
        if (buffer.length!=Float.BYTES) {
            System.out.println("Impossible to convert the message to float");
            return 0;
        }
        return ByteBuffer.wrap(buffer).getFloat();
    }

    public byte[] intArrayToByte (int [] message) {
        byte[] buffer = new byte[Integer.BYTES*message.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i=0;i<message.length;i++)
            byteBuffer.putInt(message[i]);
        return buffer;
    }

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

    public byte[] booleanArrayToByte (boolean [] message) {
        byte[] res = new byte [1+(message.length+7)/8];
        res[0]=(byte)(1+(message.length-1)%8); // number of booleans in the last byte
        for (int i = 0; i < message.length; i++)
            if (message[i])
                res[1+i/8] |= (1 << (i % 8));
        return res;
    }

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

    public byte[] doubleArrayToByte (double [] message) {
        byte[] buffer = new byte[Double.BYTES*message.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i=0;i<message.length;i++)
            byteBuffer.putDouble(message[i]);
        return buffer;
    }

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

    public byte[] floatArrayToByte (float [] message) {
        byte[] buffer = new byte[Float.BYTES*message.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i=0;i<message.length;i++)
            byteBuffer.putFloat(message[i]);
        return buffer;
    }

    public float [] byteToFloatArray (byte[] buffer) {
        if (buffer.length%Float.BYTES!=0) {
            System.out.println("Impossible to convert the message to float []");
            return null;
        }
        float [] res=new float [buffer.length/Float.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, buffer.length);
        for (int i=0;i<res.length;i++)
            res[i] = byteBuffer.getFloat();
        return res;
    }

    protected void finalize() {
        if (socket!=null && !socket.isClosed())
            socket.close();
    }

}