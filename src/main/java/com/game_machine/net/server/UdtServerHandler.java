package com.game_machine.net.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.udt.UdtMessage;
import io.netty.channel.udt.nio.NioUdtProvider;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import akka.actor.ActorSelection;

import com.game_machine.ActorUtil;
import com.game_machine.Config;
import com.game_machine.NetMessage;
import com.game_machine.game.Inbound;

@Sharable
public class UdtServerHandler extends ChannelInboundMessageHandlerAdapter<UdtMessage> {

	private static final Logger log = Logger.getLogger(UdtServerHandler.class.getName());
	private UdtServer server;
	private ChannelHandlerContext ctx = null;
	private ActorSelection inbound;
	private Integer messageEncoding;

	public UdtServerHandler(int messageEncoding) {
		log.setLevel(Level.parse(Config.logLevel));
		this.inbound = ActorUtil.getSelectionByClass(Inbound.class);
		this.messageEncoding = messageEncoding;
	}

	public void setServer(UdtServer server) {
		this.server = server;
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final UdtMessage m) {
		m.retain();

		byte[] bytes = new byte[m.data().readableBytes()];
		m.data().readBytes(bytes);

		log.fine("UDT server got " + new String(bytes));
		String host = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
		int port = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
		log.warning("PORT: " + Integer.toString(port));
		NetMessage gameMessage = new NetMessage(null,NetMessage.UDT, messageEncoding, bytes, host, port);
		this.inbound.tell(gameMessage, null);
	}

	public void sendToClient(byte[] bytes, String host, int port) {

		ByteBuf buf = Unpooled.copiedBuffer(bytes);
		UdtMessage message = new UdtMessage(buf);
		this.ctx.channel().write(message);
		log.fine("UDT server sent " + new String(bytes));
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		log.log(Level.WARNING, "close the connection when an exception is raised", cause);
		ctx.close();
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		log.info("UDT server active " + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
		this.ctx = ctx;
	}

	public void stop() {
		this.ctx.flush().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				server.shutdown();
			}
		});
	}

}
