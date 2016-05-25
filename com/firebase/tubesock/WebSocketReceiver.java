/*     */ package com.firebase.tubesock;
/*     */ 
/*     */ import java.io.DataInputStream;
/*     */ import java.io.IOException;
/*     */ import java.net.SocketTimeoutException;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ class WebSocketReceiver
/*     */ {
/*  12 */   private DataInputStream input = null;
/*  13 */   private WebSocket websocket = null;
/*  14 */   private WebSocketEventHandler eventHandler = null;
/*  15 */   private byte[] inputHeader = new byte[112];
/*     */   
/*     */   private MessageBuilderFactory.Builder pendingBuilder;
/*  18 */   private volatile boolean stop = false;
/*     */   
/*     */   WebSocketReceiver(WebSocket websocket)
/*     */   {
/*  22 */     this.websocket = websocket;
/*     */   }
/*     */   
/*     */   void setInput(DataInputStream input) {
/*  26 */     this.input = input;
/*     */   }
/*     */   
/*     */   void run() {
/*  30 */     this.eventHandler = this.websocket.getEventHandler();
/*  31 */     while (!this.stop) {
/*     */       try {
/*  33 */         int offset = 0;
/*  34 */         offset += read(this.inputHeader, offset, 1);
/*  35 */         boolean fin = (this.inputHeader[0] & 0x80) != 0;
/*  36 */         boolean rsv = (this.inputHeader[0] & 0x70) != 0;
/*  37 */         if (rsv) {
/*  38 */           throw new WebSocketException("Invalid frame received");
/*     */         }
/*  40 */         byte opcode = (byte)(this.inputHeader[0] & 0xF);
/*  41 */         offset += read(this.inputHeader, offset, 1);
/*  42 */         byte length = this.inputHeader[1];
/*  43 */         long payload_length = 0L;
/*  44 */         if (length < 126) {
/*  45 */           payload_length = length;
/*  46 */         } else if (length == 126) {
/*  47 */           offset += read(this.inputHeader, offset, 2);
/*  48 */           payload_length = (0xFF & this.inputHeader[2]) << 8 | 0xFF & this.inputHeader[3];
/*  49 */         } else if (length == Byte.MAX_VALUE)
/*     */         {
/*     */ 
/*     */ 
/*  53 */           offset += read(this.inputHeader, offset, 8);
/*     */           
/*  55 */           payload_length = parseLong(this.inputHeader, offset - 8);
/*     */         }
/*     */         
/*  58 */         byte[] payload = new byte[(int)payload_length];
/*  59 */         read(payload, 0, (int)payload_length);
/*  60 */         if (opcode == 8) {
/*  61 */           this.websocket.onCloseOpReceived();
/*  62 */         } else if (opcode != 10)
/*     */         {
/*  64 */           if ((opcode == 1) || (opcode == 2) || (opcode == 9) || (opcode == 0))
/*     */           {
/*     */ 
/*     */ 
/*     */ 
/*  69 */             appendBytes(fin, opcode, payload);
/*     */           }
/*     */           else {
/*  72 */             throw new WebSocketException("Unsupported opcode: " + opcode);
/*     */           }
/*     */         }
/*     */       }
/*     */       catch (SocketTimeoutException sto) {}catch (IOException ioe)
/*     */       {
/*  78 */         handleError(new WebSocketException("IO Error", ioe));
/*     */       } catch (WebSocketException e) {
/*  80 */         handleError(e);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void appendBytes(boolean fin, byte opcode, byte[] data)
/*     */   {
/*  87 */     if (opcode == 9) {
/*  88 */       if (fin) {
/*  89 */         handlePing(data);
/*     */       } else {
/*  91 */         throw new WebSocketException("PING must not fragment across frames");
/*     */       }
/*     */     } else {
/*  94 */       if ((this.pendingBuilder != null) && (opcode != 0))
/*  95 */         throw new WebSocketException("Failed to continue outstanding frame");
/*  96 */       if ((this.pendingBuilder == null) && (opcode == 0))
/*     */       {
/*  98 */         throw new WebSocketException("Received continuing frame, but there's nothing to continue");
/*     */       }
/* 100 */       if (this.pendingBuilder == null)
/*     */       {
/* 102 */         this.pendingBuilder = MessageBuilderFactory.builder(opcode);
/*     */       }
/* 104 */       if (!this.pendingBuilder.appendBytes(data))
/* 105 */         throw new WebSocketException("Failed to decode frame");
/* 106 */       if (fin) {
/* 107 */         WebSocketMessage message = this.pendingBuilder.toMessage();
/* 108 */         this.pendingBuilder = null;
/*     */         
/* 110 */         if (message == null) {
/* 111 */           throw new WebSocketException("Failed to decode whole message");
/*     */         }
/* 113 */         this.eventHandler.onMessage(message);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void handlePing(byte[] payload)
/*     */   {
/* 121 */     if (payload.length <= 125) {
/* 122 */       this.websocket.pong(payload);
/*     */     } else {
/* 124 */       throw new WebSocketException("PING frame too long");
/*     */     }
/*     */   }
/*     */   
/*     */   private long parseLong(byte[] buffer, int offset)
/*     */   {
/* 130 */     return (buffer[(offset + 0)] << 56) + ((buffer[(offset + 1)] & 0xFF) << 48) + ((buffer[(offset + 2)] & 0xFF) << 40) + ((buffer[(offset + 3)] & 0xFF) << 32) + ((buffer[(offset + 4)] & 0xFF) << 24) + ((buffer[(offset + 5)] & 0xFF) << 16) + ((buffer[(offset + 6)] & 0xFF) << 8) + ((buffer[(offset + 7)] & 0xFF) << 0);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int read(byte[] buffer, int offset, int length)
/*     */     throws IOException
/*     */   {
/* 141 */     this.input.readFully(buffer, offset, length);
/* 142 */     return length;
/*     */   }
/*     */   
/*     */   void stopit() {
/* 146 */     this.stop = true;
/*     */   }
/*     */   
/*     */   boolean isRunning() {
/* 150 */     return !this.stop;
/*     */   }
/*     */   
/*     */   private void handleError(WebSocketException e) {
/* 154 */     stopit();
/* 155 */     this.websocket.handleReceiverError(e);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/tubesock/WebSocketReceiver.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */