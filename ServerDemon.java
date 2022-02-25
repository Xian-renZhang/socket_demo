import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerDemon {
    public static void main(String[] args) throws IOException, InterruptedException {
        Thread executorServer = new Thread() {
            @Override
            public void run() {
                ExecutorServer server = new ExecutorServer();
                server.start();

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                // 利用 Socket 模拟了一个简单的客户端，只进行连接、读取、打印。
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 1000; i++) {
                    try (Socket client = new Socket(InetAddress.getLocalHost(), server.getPort());
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(client.getInputStream()))) {
                        bufferedReader.lines().forEach(s -> System.out.println(s));
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
                System.out.println("Executor耗时：" + (System.currentTimeMillis() - startTime) + "毫秒");
            }
        };

        Thread nioServer = new Thread() {
            @Override
            public void run() {
                NIOServer server = new NIOServer();
                server.start();
                long startTime=System.currentTimeMillis();
                for(int i=0;i<1000;i++){
                    try (Socket client = new Socket(InetAddress.getLocalHost(), 8888);
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(client.getInputStream()))) {
                        bufferedReader.lines().forEach(s -> System.out.println(s));
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
                System.out.println("NIO耗时：" + (System.currentTimeMillis() - startTime) + "毫秒");
            }
        };

        executorServer.start();
        nioServer.start();

    }
}

class ExecutorServer extends Thread {
    private ServerSocket serverSocket;

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void run() {
        try {
            // 服务器端启动 ServerSocket，端口 0 表示自动绑定一个空闲端口.
            serverSocket = new ServerSocket(0);
            ExecutorService executor = Executors.newFixedThreadPool(5);
            while (true) {
                // 调用 accept 方法，阻塞等待客户端连接。
                Socket socket = serverSocket.accept();
                RequestHandler requestHandler = new RequestHandler(socket);
                executor.execute(requestHandler);
                // requestHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class RequestHandler extends Thread {
    private Socket socket;

    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (PrintWriter out = new PrintWriter(socket.getOutputStream());) {
            out.println("Hello Executor!");
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class NIOServer extends Thread {

    public void run() {
        try (Selector selector = Selector.open();
                ServerSocketChannel serverSocket = ServerSocketChannel.open();) {// 创建Selector和Channel
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);// 注册到Selector，并说明关注点为新连接请求
            while (true) {
                selector.select();// 阻塞等待就绪的Channel
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    sayHelloWorld((ServerSocketChannel) key.channel());
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sayHelloWorld(ServerSocketChannel server) throws IOException {
        try (SocketChannel client = server.accept();) {
            client.write(Charset.defaultCharset().encode("Hello NIO!"));
        }
    }
}
