package com.example.modbus;

import static java.lang.System.exit;
import static com.example.demoutils.AperitivoUtils.DEFAULT_ZITI_IDENTITY_FILE;
import java.io.File;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import com.digitalpetri.modbus.requests.ReadCoilsRequest;
import com.digitalpetri.modbus.requests.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadCoilsResponse;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import com.digitalpetri.modbus.slave.ServiceRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.example.demoutils.AperitivoUtils;
import org.openziti.Ziti;
import org.openziti.ZitiAddress;
import org.openziti.ZitiContext;
import org.openziti.api.Identity;
import org.openziti.netty.ZitiChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusServer {

  private static final Logger log = LoggerFactory.getLogger(ModbusServer.class);
  private static final String APERITIVO_URL_OPTION = "aperitivoUrl";
  private static final String DEFAULT_APERITIVO_URL = "https://aperitivo.production.netfoundry.io";
  private static final String IDENTITY_OPTION = "identityFile";
  private static final String HELP_OPTION = "help";
  private final Random randomizer = new Random();

  public static void main(final String[] args) throws ExecutionException, InterruptedException {
    final CommandLine cmdLine = parseCommandLineOptions(args);
    final ZitiContext zitiContext = checkCreateIdentity(cmdLine);

    // Create and start the ModbusServer
    final ModbusServer zitiModbusServer = new ModbusServer();
    zitiModbusServer.start(zitiContext);

    Runtime.getRuntime().addShutdownHook(new Thread("modbus-server-shutdown-hook") {
      @Override
      public void run() {
        zitiModbusServer.stop();
      }
    });

    Thread.sleep(Integer.MAX_VALUE);
  }

  private final ModbusZitiPeripheral modbusZitiPeripheral;

  private ZitiContext zitiContext;

  public ModbusServer() {
    modbusZitiPeripheral = new ModbusZitiPeripheral();
  }

  public void start(final ZitiContext zitiContext) throws ExecutionException, InterruptedException {

    // Add a simple ServiceRequestHandler that can log and supply values for various modbus request types
    modbusZitiPeripheral.setRequestHandler(new ServiceRequestHandler() {
      @Override
      public void onReadHoldingRegisters(final ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse> service) {
        logReceipt(service);

        final ReadHoldingRegistersRequest request = service.getRequest();

        final ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(request.getQuantity());

        // Return a random value for the holding register
        for (int i = 0; i < request.getQuantity(); i++) {
          registers.writeShort(randomizer.nextInt(100));
        }

        service.sendResponse(new ReadHoldingRegistersResponse(registers));
        ReferenceCountUtil.release(request);
      }

      @Override
      public void onReadInputRegisters(final ServiceRequest<ReadInputRegistersRequest, ReadInputRegistersResponse> service) {
        logReceipt(service);
        ReferenceCountUtil.release(service.getRequest());
      }

      @Override
      public void onReadCoils(ServiceRequest<ReadCoilsRequest, ReadCoilsResponse> service) {
        logReceipt(service);

        final ReadCoilsRequest request = service.getRequest();

        final ByteBuf coils = PooledByteBufAllocator.DEFAULT.buffer(request.getQuantity());

        // Return a random boolean value for the coil
        for (int i = 0; i < request.getQuantity(); i++) {
          coils.writeBoolean(randomizer.nextBoolean());
        }

        service.sendResponse(new ReadCoilsResponse(coils));

        ReferenceCountUtil.release(service.getRequest());
      }

      @Override
      public void onReadDiscreteInputs(final ServiceRequest<ReadDiscreteInputsRequest, ReadDiscreteInputsResponse> service) {
        logReceipt(service);
        ReferenceCountUtil.release(service.getRequest());
      }

      private void logReceipt(final ServiceRequest<?,?> service) {
        if (service.getChannel() instanceof ZitiChannel zitiChannel) {
          final SocketAddress address = zitiChannel.remoteAddress();
          if (address instanceof ZitiAddress.Session session) {
            log.info("Received request id={} from {}:{} for {}",
              session.getId$ziti(), session.getCallerId(), session.getService(), service.getRequest().getClass().getSimpleName());
            return;
          }
        }
        log.info("Received non-ziti request for {} from {}", service.getRequest().getClass().getSimpleName(), service.getChannel().remoteAddress());
      }
    });

    // save the zitiContext for shutdown processing
    this.zitiContext = zitiContext;

    final String identityName = Optional.ofNullable(zitiContext.getId()).map(Identity::getName).orElse(null);
    // the demo identity creation also generates a modbus service.  Use it as the bind (listen) address
    log.info("Starting bind to ziti service {}-modbus", identityName);
    modbusZitiPeripheral.bindZiti(zitiContext, identityName + "-modbus").get();
  }

  public void stop() {
    modbusZitiPeripheral.shutdown();
    if (zitiContext != null) {
      zitiContext.destroy();
      Ziti.removeContext(zitiContext);
    }
  }

  // Command line processing and aperitivo stuff below
  private static CommandLine parseCommandLineOptions(final String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder().option("a").longOpt(APERITIVO_URL_OPTION).hasArg(true)
        .desc(String.format("URL for the aperitivo service. Defaults to '%s'", DEFAULT_APERITIVO_URL)).build());
    options.addOption(Option.builder().option("i").longOpt(IDENTITY_OPTION).hasArg(true)
        .desc(String.format("Identity file, json or pkcs12. Defaults to '%s'", DEFAULT_ZITI_IDENTITY_FILE)).build());
    options.addOption(Option.builder().option("h").longOpt(HELP_OPTION).desc("Show this help text").build());
    CommandLine commandLine = null;
    try {
      final CommandLineParser parser = new DefaultParser();
      commandLine = parser.parse(options, args);
    } catch (ParseException exception) {
      log.error("Parsing options failed. Reason: {}", exception.getMessage());
      exit(1);
    }
    if (commandLine.hasOption(HELP_OPTION)) {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.setWidth(120);
      formatter.printHelp("ModbusServer", options);
      exit(0);
    }
    return commandLine;
  }

  private static ZitiContext loadIdentity(final String identityFile) {
    log.info("Attempting to connect to ziti using identity stored in {}", identityFile);
    Ziti.init(identityFile, "".toCharArray(), false);
    final ZitiContext zitiContext = Ziti.getContexts().stream().findFirst().orElseThrow(() -> {
      log.error("Could not establish a Ziti context using the identity {}", identityFile);
      return new IllegalArgumentException("Could not create a ZitiContext");
    });
    if (!zitiContext.getStatus().toString().equals("Active")) {
      log.warn("Failed to establish a ziti context, status: {}.", zitiContext.getStatus());
      if (zitiContext.getStatus().toString().equals("NotAuthorized")) {
        log.error("Cannot authenticate with the configured identity. If using '{}', try deleting this saved identity file and trying again",
            DEFAULT_ZITI_IDENTITY_FILE);
      }
      throw new IllegalArgumentException("Could not authenticate the ZitiContext from: " + identityFile);
    }
    return zitiContext;
  }

  private static ZitiContext checkCreateIdentity(final CommandLine cmdLine) {
    if (cmdLine.hasOption(IDENTITY_OPTION)) {
      return loadIdentity(cmdLine.getOptionValue(IDENTITY_OPTION));
    } else if (new File(DEFAULT_ZITI_IDENTITY_FILE).exists()) {
      return loadIdentity(DEFAULT_ZITI_IDENTITY_FILE);
    } else {
      return loadIdentity(AperitivoUtils.createIdentityKeystore(
          cmdLine.getOptionValue(APERITIVO_URL_OPTION, DEFAULT_APERITIVO_URL)));
    }
  }


}
