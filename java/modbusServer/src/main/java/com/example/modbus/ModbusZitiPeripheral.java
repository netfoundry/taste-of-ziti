package com.example.modbus;

import java.util.concurrent.CompletableFuture;
import org.openziti.ZitiAddress;
import org.openziti.ZitiContext;
import org.openziti.netty.ZitiChannel;
import org.openziti.netty.ZitiServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.digitalpetri.modbus.codec.ModbusRequestDecoder;
import com.digitalpetri.modbus.codec.ModbusResponseEncoder;
import com.digitalpetri.modbus.codec.ModbusTcpCodec;
import com.digitalpetri.modbus.slave.ModbusTcpSlave;
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Extends the com.digitalpetri.modbus.ModbusTcpSlave and adds a new bindZiti method that allows the caller to
 * specify the zitiContext and service name to listen for ModbusTCP messages instead of the traditional server name and port.
 */
public class ModbusZitiPeripheral extends ModbusTcpSlave {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ModbusTcpSlaveConfig config;

  public ModbusZitiPeripheral() {
    this(new ModbusTcpSlaveConfig.Builder().build());
  }

  public ModbusZitiPeripheral(final ModbusTcpSlaveConfig config) {
    super(config);
    this.config = config;
  }

  /**
   * Bind to an OpenZiti service
   * @param zitiContext the initialized ZitiContext
   * @param zitiService name of the OpenZiti service for the bind
   * @return CompletableFuture for this class that performs the bind to the service
   */
  public CompletableFuture<ModbusZitiPeripheral> bindZiti(final ZitiContext zitiContext, final String zitiService) {
    return this.bindZiti(zitiContext, zitiService, null);
  }

  /**
   * Bind to an OpenZiti service using the specified identity for an addressable terminator
   * @param zitiContext the initialized ZitiContext
   * @param zitiService name of the OpenZiti service for the bind
   * @param identity Optional OpenZiti identity name to use for an addressable terminator
   * @return CompletableFuture for this class that performs the bind to the service
   */
  public CompletableFuture<ModbusZitiPeripheral> bindZiti(final ZitiContext zitiContext, final String zitiService, final String identity) {
    final CompletableFuture<ModbusZitiPeripheral> bindFuture = new CompletableFuture<>();
    final ChannelInitializer<ZitiChannel> initializer = new ChannelInitializer<>() {
      protected void initChannel(ZitiChannel channel) {
        logger.debug("channel initialized: {}", channel.localAddress());
        channel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        channel.pipeline().addLast(new ModbusTcpCodec(new ModbusResponseEncoder(), new ModbusRequestDecoder()));
        channel.pipeline().addLast(newModbusTcpSlaveHandler());
      }
    };
    final ServerBootstrap bootstrap = new ServerBootstrap();
    this.config.getBootstrapConsumer().accept(bootstrap);
    bootstrap.group(this.config.getEventLoop())
        .channelFactory(new ZitiServerChannelFactory(zitiContext))
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(initializer)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .bind(new ZitiAddress.Bind(zitiService, identity)).addListener(future -> {
          if (future.isSuccess() && future instanceof ChannelFuture channelfuture) {
            final Channel channel = channelfuture.channel();
            putServerChannel(channel.localAddress(), channel);
            bindFuture.complete(this);
          } else {
            logger.error("Failed bind {}", future.cause().getMessage());
            bindFuture.completeExceptionally(future.cause());
          }
        });
    return bindFuture;
  }

}
