# Amazon Kinesis Development - KCL 2.3.4

The master branch provides completed code for the [Process Real-Time Stock Data Using KPL and KCL Tutorial][learning-kinesis]  in the [Kinesis Developer Guide][kinesis-developer-guide].

[learning-kinesis]:  https://docs.aws.amazon.com/streams/latest/dev/tutorial-stock-data-kplkcl.html
[kinesis-developer-guide]: http://docs.aws.amazon.com/kinesis/latest/dev/introduction.html

Start localstack:

`./start-containers.sh`

Run the producer in the background (it logs, so you might want to open a new shell):

`AWS_ACCESS_KEY_ID=foo AWS_SECRET_ACCESS_KEY=bar ENDPOINT_OVERRIDE=http://localhost:4566 CBOR_ENABLED=false ./mvnw exec:java -Dexec.mainClass="com.amazonaws.services.kinesis.samples.stocktrades.writer.StockTradesWriter" -Dexec.args="stocks eu-central-1" &`

Run the consumer:

`AWS_ACCESS_KEY_ID=foo AWS_SECRET_ACCESS_KEY=bar ENDPOINT_OVERRIDE=http://localhost:4566 CBOR_ENABLED=false ./mvnw exec:java -Dexec.mainClass="com.amazonaws.services.kinesis.samples.stocktrades.processor.StockTradesProcessor" -Dexec.args="StockTradesProcessor stocks eu-central-1"`