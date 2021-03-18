package com.amazonaws.services.kinesis.samples.stocktrades.writer;


import com.amazonaws.services.kinesis.samples.stocktrades.model.StockTrade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.kinesis.common.KinesisClientUtil;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * Continuously sends simulated stock trades to Kinesis
 *
 */
public class StockTradesWriter {

    private static final Log LOG = LogFactory.getLog(StockTradesWriter.class);

    private static void checkUsage(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: " + StockTradesWriter.class.getSimpleName()
                    + " <stream name> <region>");
            System.exit(1);
        }
    }

    /**
     * Checks if the stream exists and is active
     *
     * @param kinesisClient Amazon Kinesis client instance
     * @param streamName Name of stream
     */
    private static void validateStream(KinesisAsyncClient kinesisClient, String streamName) {
        try {
            DescribeStreamRequest describeStreamRequest =  DescribeStreamRequest.builder().streamName(streamName).build();
            DescribeStreamResponse describeStreamResponse = kinesisClient.describeStream(describeStreamRequest).get();
            if(!describeStreamResponse.streamDescription().streamStatus().toString().equals("ACTIVE")) {
                System.err.println("Stream " + streamName + " is not active. Please wait a few moments and try again.");
                System.exit(1);
            }
        }catch (Exception e) {
            System.err.println("Error found while describing the stream " + streamName);
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Uses the Kinesis client to send the stock trade to the given stream.
     *
     * @param trade instance representing the stock trade
     * @param kinesisClient Amazon Kinesis client
     * @param streamName Name of stream
     */
    private static void sendStockTrade(StockTrade trade, KinesisAsyncClient kinesisClient,
                                       String streamName) {
        byte[] bytes = trade.toJsonAsBytes();
        // The bytes could be null if there is an issue with the JSON serialization by the Jackson JSON library.
        if (bytes == null) {
            LOG.warn("Could not get JSON bytes for stock trade");
            return;
        }

        LOG.info("Putting trade: " + trade.toString());
        PutRecordRequest.Builder putRecord = PutRecordRequest.builder();
        putRecord.streamName(streamName);
        // We use the ticker symbol as the partition key, explained in the Supplemental Information section below.
        putRecord.partitionKey(trade.getTickerSymbol());
        putRecord.data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(bytes)));

        try {
            kinesisClient.putRecord(putRecord.build()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    // https://docs.aws.amazon.com/streams/latest/dev/tutorial-stock-data-kplkcl-download.html
    public static void main(String[] args) throws Exception {
        checkUsage(args);

        String streamName = args[0];
        String regionName = args[1];
        Region region = Region.of(regionName);
        String endpointOverride = System.getenv("ENDPOINT_OVERRIDE");
        if (region == null) {
            System.err.println(regionName + " is not a valid AWS region.");
            System.exit(1);
        }

        KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder().region(region);
        if ( endpointOverride != null ) builder.endpointOverride(URI.create(endpointOverride));

        KinesisAsyncClient kinesisClient = KinesisClientUtil.createKinesisAsyncClient(builder);

        // Validate that the stream exists and is active
        validateStream(kinesisClient, streamName);

        // Repeatedly send stock trades with a 100 milliseconds wait in between
        StockTradeGenerator stockTradeGenerator = new StockTradeGenerator();
        while(true) {
            StockTrade trade = stockTradeGenerator.getRandomTrade();
            sendStockTrade(trade, kinesisClient, streamName);
            Thread.sleep(100);
        }
    }

}
