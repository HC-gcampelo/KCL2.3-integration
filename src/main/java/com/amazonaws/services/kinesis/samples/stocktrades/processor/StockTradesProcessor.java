package com.amazonaws.services.kinesis.samples.stocktrades.processor;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.metrics.MetricsFactory;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.metrics.NullMetricsFactory;

/**
 * Uses the Kinesis Client Library (KCL) 2.2.9 to continuously consume and process stock trade
 * records from the stock trades stream. KCL monitors the number of shards and creates
 * record processor instances to read and process records from each shard. KCL also
 * load balances shards across all the instances of this processor.
 *
 */
public class StockTradesProcessor {

    private static final Log LOG = LogFactory.getLog(StockTradesProcessor.class);

    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static final Logger PROCESSOR_LOGGER =
            Logger.getLogger("com.amazonaws.services.kinesis.samples.stocktrades.processor.StockTradeRecordProcessor");

    private static void checkUsage(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: " + StockTradesProcessor.class.getSimpleName()
                    + " <application name> <stream name> <region>");
            System.exit(1);
        }
    }

    /**
     * Sets the global log level to WARNING and the log level for this package to INFO,
     * so that we only see INFO messages for this processor. This is just for the purpose
     * of this tutorial, and should not be considered as best practice.
     *
     */
    private static void setLogLevels() {
        ROOT_LOGGER.setLevel(Level.WARNING);
        // Set this to INFO for logging at INFO level. Suppressed for this example as it can be noisy.
        PROCESSOR_LOGGER.setLevel(Level.WARNING);
    }

    public static void main(String[] args) throws Exception {
        checkUsage(args);

        setLogLevels();

        String applicationName = args[0];
        String streamName = args[1];
        Region region = Region.of(args[2]);
        String endpointOverride = System.getenv("ENDPOINT_OVERRIDE");

        if (region == null) {
            System.err.println(args[2] + " is not a valid AWS region.");
            System.exit(1);
        }

        KinesisAsyncClientBuilder kinesisBuilder = KinesisAsyncClient.builder().region(region);
        DynamoDbAsyncClientBuilder dynamoBuilder = DynamoDbAsyncClient.builder().region(region);
        CloudWatchAsyncClientBuilder cloudWatchBuilder = CloudWatchAsyncClient.builder().region(region);
        if ( endpointOverride != null ) {
            URI endpointUri = URI.create(endpointOverride);
            kinesisBuilder.endpointOverride(endpointUri);
            dynamoBuilder.endpointOverride(endpointUri);
            cloudWatchBuilder.endpointOverride(endpointUri);
        }

        KinesisAsyncClient kinesisClient = kinesisBuilder.build();
        DynamoDbAsyncClient dynamoClient = dynamoBuilder.build();
        CloudWatchAsyncClient cloudWatchClient = cloudWatchBuilder.build();

        StockTradeRecordProcessorFactory shardRecordProcessor = new StockTradeRecordProcessorFactory();
        ConfigsBuilder configsBuilder = new ConfigsBuilder(streamName, applicationName, kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(), shardRecordProcessor);

        Scheduler scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig().metricsLevel(MetricsLevel.NONE).metricsFactory(new NullMetricsFactory()),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig()
        );
        int exitCode = 0;
        try {
            scheduler.run();
        } catch (Throwable t) {
            LOG.error("Caught throwable while processing data.", t);
            exitCode = 1;
        }
        System.exit(exitCode);

    }

}
