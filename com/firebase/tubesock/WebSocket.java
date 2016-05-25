/*     */ package com.firebase.tubesock;
/*     */ 
/*     */ import java.io.DataInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.OutputStream;
/*     */ import java.net.Socket;
/*     */ import java.net.URI;
/*     */ import java.net.UnknownHostException;
/*     */ import java.nio.charset.Charset;
/*     */ import java.security.cert.Certificate;
/*     */ import java.security.cert.X509Certificate;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.Executors;
/*     */ import java.util.concurrent.ThreadFactory;
/*     */ import java.util.concurrent.atomic.AtomicInteger;
/*     */ import javax.net.SocketFactory;
/*     */ import javax.net.ssl.SSLException;
/*     */ import javax.net.ssl.SSLSession;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ import javax.net.ssl.SSLSocketFactory;
/*     */ import org.shaded.apache.http.conn.ssl.StrictHostnameVerifier;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class WebSocket
/*     */ {
/*     */   private static final String THREAD_BASE_NAME = "TubeSock";
/*  32 */   private static final AtomicInteger clientCount = new AtomicInteger(0);
/*     */   
/*  34 */   private static enum State { NONE,  CONNECTING,  CONNECTED,  DISCONNECTING,  DISCONNECTED;
/*     */     private State() {} }
/*  36 */   private static final Charset UTF8 = Charset.forName("UTF-8");
/*     */   
/*     */   static final byte OPCODE_NONE = 0;
/*     */   
/*     */   static final byte OPCODE_TEXT = 1;
/*     */   static final byte OPCODE_BINARY = 2;
/*     */   static final byte OPCODE_CLOSE = 8;
/*     */   static final byte OPCODE_PING = 9;
/*     */   static final byte OPCODE_PONG = 10;
/*  45 */   private volatile State state = State.NONE;
/*  46 */   private volatile Socket socket = null;
/*     */   
/*  48 */   private WebSocketEventHandler eventHandler = null;
/*     */   
/*     */   private final URI url;
/*     */   
/*     */   private final WebSocketReceiver receiver;
/*     */   private final WebSocketWriter writer;
/*     */   private final WebSocketHandshake handshake;
/*  55 */   private final int clientId = clientCount.incrementAndGet();
/*     */   
/*     */   private final Thread innerThread;
/*  58 */   private static ThreadFactory threadFactory = Executors.defaultThreadFactory();
/*  59 */   private static ThreadInitializer intializer = new ThreadInitializer()
/*     */   {
/*     */     public void setName(Thread t, String name) {
/*  62 */       t.setName(name);
/*     */     }
/*     */   };
/*     */   
/*     */   static ThreadFactory getThreadFactory() {
/*  67 */     return threadFactory;
/*     */   }
/*     */   
/*     */   static ThreadInitializer getIntializer() {
/*  71 */     return intializer;
/*     */   }
/*     */   
/*     */   public static void setThreadFactory(ThreadFactory threadFactory, ThreadInitializer intializer) {
/*  75 */     threadFactory = threadFactory;
/*  76 */     intializer = intializer;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public WebSocket(URI url)
/*     */   {
/*  84 */     this(url, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public WebSocket(URI url, String protocol)
/*     */   {
/*  93 */     this(url, protocol, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public WebSocket(URI url, String protocol, Map<String, String> extraHeaders)
/*     */   {
/* 105 */     this.innerThread = getThreadFactory().newThread(new Runnable()
/*     */     {
/*     */       public void run() {
/* 108 */         WebSocket.this.runReader();
/*     */       }
/* 110 */     });
/* 111 */     this.url = url;
/* 112 */     this.handshake = new WebSocketHandshake(url, protocol, extraHeaders);
/* 113 */     this.receiver = new WebSocketReceiver(this);
/* 114 */     this.writer = new WebSocketWriter(this, "TubeSock", this.clientId);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setEventHandler(WebSocketEventHandler eventHandler)
/*     */   {
/* 122 */     this.eventHandler = eventHandler;
/*     */   }
/*     */   
/*     */   WebSocketEventHandler getEventHandler() {
/* 126 */     return this.eventHandler;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void connect()
/*     */   {
/* 134 */     if (this.state != State.NONE) {
/* 135 */       this.eventHandler.onError(new WebSocketException("connect() already called"));
/* 136 */       close();
/* 137 */       return;
/*     */     }
/* 139 */     getIntializer().setName(getInnerThread(), "TubeSockReader-" + this.clientId);
/* 140 */     this.state = State.CONNECTING;
/* 141 */     getInnerThread().start();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void send(String data)
/*     */   {
/* 149 */     send((byte)1, data.getBytes(UTF8));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void send(byte[] data)
/*     */   {
/* 157 */     send((byte)2, data);
/*     */   }
/*     */   
/*     */   synchronized void pong(byte[] data) {
/* 161 */     send((byte)10, data);
/*     */   }
/*     */   
/*     */   private synchronized void send(byte opcode, byte[] data) {
/* 165 */     if (this.state != State.CONNECTED)
/*     */     {
/* 167 */       this.eventHandler.onError(new WebSocketException("error while sending data: not connected"));
/*     */     } else {
/*     */       try {
/* 170 */         this.writer.send(opcode, true, data);
/*     */       } catch (IOException e) {
/* 172 */         this.eventHandler.onError(new WebSocketException("Failed to send frame", e));
/* 173 */         close();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   void handleReceiverError(WebSocketException e) {
/* 179 */     this.eventHandler.onError(e);
/* 180 */     if (this.state == State.CONNECTED) {
/* 181 */       close();
/*     */     }
/* 183 */     closeSocket();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public synchronized void close()
/*     */   {
/* 191 */     switch (this.state) {
/*     */     case NONE: 
/* 193 */       this.state = State.DISCONNECTED;
/* 194 */       return;
/*     */     
/*     */     case CONNECTING: 
/* 197 */       closeSocket();
/* 198 */       return;
/*     */     
/*     */ 
/*     */     case CONNECTED: 
/* 202 */       sendCloseHandshake();
/* 203 */       return;
/*     */     case DISCONNECTING: 
/*     */       
/*     */     case DISCONNECTED: 
/*     */       
/*     */     }
/*     */   }
/*     */   
/*     */   void onCloseOpReceived() {
/* 212 */     closeSocket();
/*     */   }
/*     */   
/*     */   private synchronized void closeSocket() {
/* 216 */     if (this.state == State.DISCONNECTED) {
/* 217 */       return;
/*     */     }
/* 219 */     this.receiver.stopit();
/* 220 */     this.writer.stopIt();
/* 221 */     if (this.socket != null) {
/*     */       try {
/* 223 */         this.socket.close();
/*     */       } catch (IOException e) {
/* 225 */         throw new RuntimeException(e);
/*     */       }
/*     */     }
/* 228 */     this.state = State.DISCONNECTED;
/*     */     
/* 230 */     this.eventHandler.onClose();
/*     */   }
/*     */   
/*     */   private void sendCloseHandshake() {
/*     */     try {
/* 235 */       this.state = State.DISCONNECTING;
/*     */       
/*     */ 
/* 238 */       this.writer.stopIt();
/* 239 */       this.writer.send((byte)8, true, new byte[0]);
/*     */     } catch (IOException e) {
/* 241 */       this.eventHandler.onError(new WebSocketException("Failed to send close frame", e));
/*     */     }
/*     */   }
/*     */   
/*     */   private Socket createSocket() {
/* 246 */     String scheme = this.url.getScheme();
/* 247 */     String host = this.url.getHost();
/* 248 */     int port = this.url.getPort();
/*     */     
/*     */     Socket socket;
/*     */     
/* 252 */     if ((scheme != null) && (scheme.equals("ws"))) {
/* 253 */       if (port == -1) {
/* 254 */         port = 80;
/*     */       }
/*     */       try {
/* 257 */         socket = new Socket(host, port);
/*     */       } catch (UnknownHostException uhe) {
/* 259 */         throw new WebSocketException("unknown host: " + host, uhe);
/*     */       } catch (IOException ioe) {
/* 261 */         throw new WebSocketException("error while creating socket to " + this.url, ioe);
/*     */       }
/* 263 */     } else if ((scheme != null) && (scheme.equals("wss"))) {
/* 264 */       if (port == -1) {
/* 265 */         port = 443;
/*     */       }
/*     */       try {
/* 268 */         SocketFactory factory = SSLSocketFactory.getDefault();
/* 269 */         socket = factory.createSocket(host, port);
/*     */         
/* 271 */         verifyHost((SSLSocket)socket, host);
/*     */       } catch (UnknownHostException uhe) {
/* 273 */         throw new WebSocketException("unknown host: " + host, uhe);
/*     */       } catch (IOException ioe) {
/* 275 */         throw new WebSocketException("error while creating secure socket to " + this.url, ioe);
/*     */       }
/*     */     } else {
/* 278 */       throw new WebSocketException("unsupported protocol: " + scheme);
/*     */     }
/*     */     
/* 281 */     return socket;
/*     */   }
/*     */   
/*     */   private void verifyHost(SSLSocket socket, String host) throws SSLException {
/* 285 */     Certificate[] certs = socket.getSession().getPeerCertificates();
/* 286 */     X509Certificate peerCert = (X509Certificate)certs[0];
/* 287 */     StrictHostnameVerifier verifier = new StrictHostnameVerifier();
/* 288 */     verifier.verify(host, peerCert);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void blockClose()
/*     */     throws InterruptedException
/*     */   {
/* 298 */     if (this.writer.getInnerThread().getState() != Thread.State.NEW) {
/* 299 */       this.writer.getInnerThread().join();
/*     */     }
/* 301 */     getInnerThread().join();
/*     */   }
/*     */   
/*     */   private void runReader() {
/*     */     try {
/* 306 */       Socket socket = createSocket();
/* 307 */       synchronized (this) {
/* 308 */         this.socket = socket;
/* 309 */         if (this.state == State.DISCONNECTED)
/*     */         {
/*     */           try {
/* 312 */             this.socket.close();
/*     */           } catch (IOException e) {
/* 314 */             throw new RuntimeException(e);
/*     */           }
/* 316 */           this.socket = null; return;
/*     */         }
/*     */       }
/*     */       
/*     */ 
/* 321 */       DataInputStream input = new DataInputStream(socket.getInputStream());
/* 322 */       OutputStream output = socket.getOutputStream();
/*     */       
/* 324 */       output.write(this.handshake.getHandshake());
/*     */       
/* 326 */       boolean handshakeComplete = false;
/* 327 */       int len = 1000;
/* 328 */       byte[] buffer = new byte[len];
/* 329 */       int pos = 0;
/* 330 */       ArrayList<String> handshakeLines = new ArrayList();
/*     */       
/* 332 */       while (!handshakeComplete) {
/* 333 */         int b = input.read();
/* 334 */         if (b == -1) {
/* 335 */           throw new WebSocketException("Connection closed before handshake was complete");
/*     */         }
/* 337 */         buffer[pos] = ((byte)b);
/* 338 */         pos++;
/*     */         
/* 340 */         if ((buffer[(pos - 1)] == 10) && (buffer[(pos - 2)] == 13)) {
/* 341 */           String line = new String(buffer, UTF8);
/* 342 */           if (line.trim().equals("")) {
/* 343 */             handshakeComplete = true;
/*     */           } else {
/* 345 */             handshakeLines.add(line.trim());
/*     */           }
/*     */           
/* 348 */           buffer = new byte[len];
/* 349 */           pos = 0;
/* 350 */         } else if (pos == 1000)
/*     */         {
/* 352 */           String line = new String(buffer, UTF8);
/* 353 */           throw new WebSocketException("Unexpected long line in handshake: " + line);
/*     */         }
/*     */       }
/*     */       
/* 357 */       this.handshake.verifyServerStatusLine((String)handshakeLines.get(0));
/* 358 */       handshakeLines.remove(0);
/*     */       
/* 360 */       HashMap<String, String> headers = new HashMap();
/* 361 */       for (String line : handshakeLines) {
/* 362 */         String[] keyValue = line.split(": ", 2);
/* 363 */         headers.put(keyValue[0], keyValue[1]);
/*     */       }
/* 365 */       this.handshake.verifyServerHandshakeHeaders(headers);
/*     */       
/* 367 */       this.writer.setOutput(output);
/* 368 */       this.receiver.setInput(input);
/* 369 */       this.state = State.CONNECTED;
/* 370 */       this.writer.getInnerThread().start();
/* 371 */       this.eventHandler.onOpen();
/* 372 */       this.receiver.run();
/*     */     } catch (WebSocketException wse) {
/* 374 */       this.eventHandler.onError(wse);
/*     */     } catch (IOException ioe) {
/* 376 */       this.eventHandler.onError(new WebSocketException("error while connecting: " + ioe.getMessage(), ioe));
/*     */     }
/*     */     finally {
/* 379 */       close();
/*     */     }
/*     */   }
/*     */   
/*     */   Thread getInnerThread()
/*     */   {
/* 385 */     return this.innerThread;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/tubesock/WebSocket.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */