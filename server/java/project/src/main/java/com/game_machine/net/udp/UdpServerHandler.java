package com.game_machine.net.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.game_machine.core.ActorUtil;
import com.game_machine.core.NetMessage;

import akka.actor.ActorSelection;

//@Sharable
public final class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	public static AtomicInteger countIn = new AtomicInteger();
	public static AtomicInteger countOut = new AtomicInteger();

	private static final Logger log = LoggerFactory.getLogger(UdpServerHandler.class);
	public ChannelHandlerContext ctx = null;
	private ActorSelection inbound;

	public UdpServerHandler() {
		this.inbound = ActorUtil.getSelectionByName("message_gateway");
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		log.info("close the connection when an exception is raised", cause);
		ctx.close();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) {
		log.info("UDP server active");
		this.ctx = ctx;
	}

	public void send(InetSocketAddress address, byte[] bytes, ChannelHandlerContext ctx) {

		ByteBuf buf = Unpooled.wrappedBuffer(bytes);
		DatagramPacket packet = new DatagramPacket(buf, address);
		ctx.writeAndFlush(packet);
		countOut.incrementAndGet();
	}

	public void echo(ChannelHandlerContext ctx, DatagramPacket m, byte[] bytes) {
		ByteBuf buf = Unpooled.copiedBuffer(bytes);
		DatagramPacket packet = new DatagramPacket(buf, new InetSocketAddress(m.sender().getHostString(), m.sender()
				.getPort()));
		ctx.writeAndFlush(packet);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket m) throws Exception {
		byte[] bytes = new byte[m.content().readableBytes()];
		m.content().readBytes(bytes);

		NetMessage netMessage = new NetMessage(NetMessage.UDP, m.sender().getHostString(), m.sender().getPort(), ctx);
		netMessage.bytes = bytes;
		netMessage.address = m.sender();
		log.debug("MessageReceived length" + bytes.length + " " + new String(bytes));
		this.inbound.tell(netMessage, null);
		countIn.incrementAndGet();

	}

}