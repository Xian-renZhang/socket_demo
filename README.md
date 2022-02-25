# ServerDemo

* **ExecutorServer**
  通过固定大小的线程池负责管理工作线程，避免频繁创建、销毁线程的开销。
* **NIOServer**
  利用单线程轮询机制，将 ServerSocketChannel 注册到 Selector 关注新连接请求，Selector 阻塞等待就绪的 Channel，当有 Channel 发生接入请求就会被唤醒。
  