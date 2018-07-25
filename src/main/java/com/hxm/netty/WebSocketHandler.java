package com.hxm.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @Description:接收处理响应客户端websocket请求的核心业务处理类
 * @Auther: HXM
 * @Date: 2018/7/25 17:42
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    private static final String WEB_SOCKET_URL = "ws://localhost:8888";
    //客户端与服务端创建连接的时候调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        System.out.print("客户端与服务端连接开启...");
    }

    //客户端与服务端断开连接的时候调用
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        System.out.print("客户端与服务端连接关闭...");
    }

    //服务端接收客户端发送过来的数据结束之后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    //工程出现异常的时候调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    //服务端处理客户端websocket请求的核心方法
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        //处理客户端向服务端发起http握手请求的业务
        if(msg instanceof FullHttpRequest){
            handHttpRequest(channelHandlerContext, (FullHttpRequest) msg);
        }else if(msg instanceof WebSocketFrame){ // 处理websocket连接业务
            handWebSocketFrame(channelHandlerContext ,(WebSocketFrame) msg);
        }
    }

    //处理客户端与服务端之间的websocket业务
    private void handWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
        //判断是否是关闭websobsocket指令
        if(frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
        }
        //判断是否是ping消息
        if(frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content()));
        }
        //判断是否是二进制消息，如果是，抛出异常
        if(!(frame instanceof TextWebSocketFrame)){
            System.out.println("不支持二进制消息");
            throw new RuntimeException("["+this.getClass().getName()+"]不支持该消息");
        }
        //获取应答消息
        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame)frame).text();
        System.out.println("服务端收到客户端消息->"+request);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.format(new Date());
        TextWebSocketFrame  tws = new TextWebSocketFrame(df.format(new Date()) +"收到请求内容："+request);
        //群发消息，服务端向每个连接的客户端发送消息
        NettyConfig.group.writeAndFlush(tws);
    }

    //处理客户端向服务端发起的http握手请求业务
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request){
        if(!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))){
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MULTI_STATUS.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null,  false);
        handshaker = wsFactory.newHandshaker(request);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else{
            handshaker.handshake(ctx.channel(), request);
        }

    }
    //服务端向客户端响应消息
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response){
            if(response.status().code() != 200){ //请求失败
                ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
                response.content().writeBytes(buf);
                buf.release();
            }
            //服务端向客户端发送数据
            ChannelFuture future = ctx.channel().writeAndFlush(response);
            if(response.status().code()!=200){
                future.addListener(ChannelFutureListener.CLOSE);
            }
    }
}
