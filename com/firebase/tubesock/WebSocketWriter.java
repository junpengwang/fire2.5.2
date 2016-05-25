/*     */ package com.firebase.tubesock;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.OutputStream;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.nio.channels.Channels;
/*     */ import java.nio.channels.WritableByteChannel;
/*     */ import java.util.Random;
/*     */ import java.util.concurrent.BlockingQueue;
/*     */ import java.util.concurrent.LinkedBlockingQueue;
/*     */ import java.util.concurrent.ThreadFactory;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ class WebSocketWriter
/*     */ {
/*     */   private BlockingQueue<ByteBuffer> pendingBuffers;
/*  25 */   private final Random random = new Random();
/*  26 */   private volatile boolean stop = false;
/*  27 */   private boolean closeSent = false;
/*     */   private WebSocket websocket;
/*     */   private WritableByteChannel channel;
/*     */   private final Thread innerThread;
/*     */   
/*     */   WebSocketWriter(WebSocket websocket, String threadBaseName, int clientId) {
/*  33 */     this.innerThread = WebSocket.getThreadFactory().newThread(new Runnable()
/*     */     {
/*     */       public void run() {
/*  36 */         WebSocketWriter.this.runWriter();
/*     */       }
/*     */       
/*  39 */     });
/*  40 */     WebSocket.getIntializer().setName(getInnerThread(), threadBaseName + "Writer-" + clientId);
/*  41 */     this.websocket = websocket;
/*  42 */     this.pendingBuffers = new LinkedBlockingQueue();
/*     */   }
/*     */   
/*     */   void setOutput(OutputStream output) {
/*  46 */     this.channel = Channels.newChannel(output);
/*     */   }
/*     */   
/*     */   private ByteBuffer frameInBuffer(byte opcode, boolean masking, byte[] data) throws IOException {
/*  50 */     int headerLength = 2;
/*  51 */     if (masking) {
/*  52 */       headerLength += 4;
/*     */     }
/*  54 */     int length = data.length;
/*  55 */     if (length >= 126)
/*     */     {
/*  57 */       if (length <= 65535) {
/*  58 */         headerLength += 2;
/*     */       } else
/*  60 */         headerLength += 8;
/*     */     }
/*  62 */     ByteBuffer frame = ByteBuffer.allocate(data.length + headerLength);
/*     */     
/*  64 */     byte fin = Byte.MIN_VALUE;
/*  65 */     byte startByte = (byte)(fin | opcode);
/*  66 */     frame.put(startByte);
/*     */     
/*     */ 
/*     */ 
/*  70 */     if (length < 126) {
/*  71 */       if (masking) {
/*  72 */         length = 0x80 | length;
/*     */       }
/*  74 */       frame.put((byte)length);
/*  75 */     } else if (length <= 65535) {
/*  76 */       int length_field = 126;
/*  77 */       if (masking) {
/*  78 */         length_field = 0x80 | length_field;
/*     */       }
/*  80 */       frame.put((byte)length_field);
/*     */       
/*  82 */       frame.putShort((short)length);
/*     */     } else {
/*  84 */       int length_field = 127;
/*  85 */       if (masking) {
/*  86 */         length_field = 0x80 | length_field;
/*     */       }
/*  88 */       frame.put((byte)length_field);
/*     */       
/*  90 */       frame.putInt(0);
/*  91 */       frame.putInt(length);
/*     */     }
/*     */     
/*     */ 
/*  95 */     if (masking) {
/*  96 */       byte[] mask = generateMask();
/*  97 */       frame.put(mask);
/*     */       
/*  99 */       for (int i = 0; i < data.length; i++) {
/* 100 */         frame.put((byte)(data[i] ^ mask[(i % 4)]));
/*     */       }
/*     */     }
/*     */     
/* 104 */     frame.flip();
/* 105 */     return frame;
/*     */   }
/*     */   
/*     */   private byte[] generateMask() {
/* 109 */     byte[] mask = new byte[4];
/* 110 */     this.random.nextBytes(mask);
/* 111 */     return mask;
/*     */   }
/*     */   
/*     */   synchronized void send(byte opcode, boolean masking, byte[] data) throws IOException {
/* 115 */     ByteBuffer frame = frameInBuffer(opcode, masking, data);
/* 116 */     if ((this.stop) && ((this.closeSent) || (opcode != 8))) {
/* 117 */       throw new WebSocketException("Shouldn't be sending");
/*     */     }
/* 119 */     if (opcode == 8) {
/* 120 */       this.closeSent = true;
/*     */     }
/* 122 */     this.pendingBuffers.add(frame);
/*     */   }
/*     */   
/*     */   private void writeMessage() throws InterruptedException, IOException {
/* 126 */     ByteBuffer msg = (ByteBuffer)this.pendingBuffers.take();
/* 127 */     this.channel.write(msg);
/*     */   }
/*     */   
/*     */   void stopIt() {
/* 131 */     this.stop = true;
/*     */   }
/*     */   
/*     */   private void handleError(WebSocketException e) {
/* 135 */     this.websocket.handleReceiverError(e);
/*     */   }
/*     */   
/*     */   private void runWriter() {
/*     */     try {
/* 140 */       while ((!this.stop) && (!Thread.interrupted())) {
/* 141 */         writeMessage();
/*     */       }
/*     */       
/* 144 */       for (int i = 0; i < this.pendingBuffers.size(); i++) {
/* 145 */         writeMessage();
/*     */       }
/*     */     } catch (IOException e) {
/* 148 */       handleError(new WebSocketException("IO Exception", e));
/*     */     }
/*     */     catch (InterruptedException e) {}
/*     */   }
/*     */   
/*     */ 
/*     */   Thread getInnerThread()
/*     */   {
/* 156 */     return this.innerThread;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/tubesock/WebSocketWriter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */