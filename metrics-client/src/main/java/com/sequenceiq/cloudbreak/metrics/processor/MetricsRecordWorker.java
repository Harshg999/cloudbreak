package com.sequenceiq.cloudbreak.metrics.processor;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import org.xerial.snappy.Snappy;

import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class MetricsRecordWorker extends RecordWorker<MetricsRecordProcessor, MetricsProcessorConfiguration, MetricsRecordRequest> {

    private static final Integer STATUS_OK = 200;

    private static final Integer LAST_REDIRECT_CODE = 399;

    private final OkHttpClient client;

    private final String remoteWriteUrlUnformatted;

    public MetricsRecordWorker(String name, String serviceName, MetricsRecordProcessor recordProcessor, BlockingDeque<MetricsRecordRequest> processingQueue,
            MetricsProcessorConfiguration configuration) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        client = createClient();
        remoteWriteUrlUnformatted = configuration.getRemoteWriteUrl().replace("$accountid", "%s");
    }

    @Override
    public void processRecordInput(MetricsRecordRequest input) throws StreamProcessingException {
        try {
            byte[] compressed = Snappy.compress(input.getWriteRequest().toByteArray());
            MediaType mediaType = MediaType.parse("application/x-protobuf");
            RequestBody body = RequestBody.create(mediaType, compressed);
            Request request = new Request.Builder()
                    .url(String.format(remoteWriteUrlUnformatted, input.getAccountId()))
                    .addHeader("Content-Encoding", "snappy")
                    .addHeader("User-Agent", "cb-prometheus-java-client")
                    .addHeader("X-Prometheus-Remote-Write-Version", "0.1.0")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() < STATUS_OK || response.code() > LAST_REDIRECT_CODE) {
                throw new StreamProcessingException(String.format("Response code is not valid. (status code: %s)", response.code()));
            }
        } catch (IOException e) {
            throw new StreamProcessingException(e);
        }
    }

    @Override
    public void onInterrupt() {
    }

    private OkHttpClient createClient() {
        Integer timeout = getConfiguration().getHttpTimeout();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(timeout, TimeUnit.SECONDS);
        return okHttpClient;
    }
}
