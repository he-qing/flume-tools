package de.codepfleger.flume.parquet.serializer;

import de.codepfleger.flume.avro.serializer.event.WindowsLogEvent;
import de.codepfleger.flume.avro.serializer.serializer.AbstractReflectionAvroEventSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.serialization.EventSerializer;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class WindowsLogSerializer implements ParquetSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsLogSerializer.class);

    private final ObjectMapper mapper;

    private ParquetWriter<GenericData.Record> writer;
    private Schema schema;

    public WindowsLogSerializer() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public void configure(Context context) {
    }

    @Override
    public void afterCreate() throws IOException {

    }

    @Override
    public void afterReopen() throws IOException {

    }

    @Override
    public void write(Event event) throws IOException {
        try {
            String message = new String(event.getBody());
            Map<String, Object> dataMap = new LinkedHashMap<>(mapper.readValue(message, Map.class));
            WindowsLogEvent windowsLogEvent = new WindowsLogEvent();
            AbstractReflectionAvroEventSerializer.setFieldsAndRemove(windowsLogEvent, dataMap);
            windowsLogEvent.dynamic.putAll(dataMap);

            GenericData.Record record = new GenericData.Record(schema);
            record.put("EventTime", windowsLogEvent.EventTime);
            record.put("Hostname", windowsLogEvent.Hostname);
            record.put("EventType", windowsLogEvent.EventType);
            record.put("Severity", windowsLogEvent.Severity);
            record.put("SourceModuleName", windowsLogEvent.SourceModuleName);
            record.put("UserID", windowsLogEvent.UserID);
            record.put("ProcessID", windowsLogEvent.ProcessID);
            record.put("Domain", windowsLogEvent.Domain);
            record.put("EventReceivedTime", windowsLogEvent.EventReceivedTime);
            record.put("Path", windowsLogEvent.Path);
            record.put("Message", windowsLogEvent.Message);
            record.put("dynamic", windowsLogEvent.dynamic);

            writeRecord(record);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void writeRecord(GenericData.Record record) throws IOException {
        writer.write(record);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void beforeClose() throws IOException {
    }

    @Override
    public boolean supportsReopen() {
        return false;
    }

    public void initialize(String filePath, Schema schema) throws IOException {
        this.schema = schema;

        Path fileToWrite = new Path(filePath);
        writer = AvroParquetWriter.<GenericData.Record>builder(fileToWrite)
                .withSchema(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build();
    }

    @Override
    public ParquetWriter<GenericData.Record> getWriter() {
        return writer;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public static class Builder implements EventSerializer.Builder {
        @Override
        public EventSerializer build(Context context, OutputStream out) {
            WindowsLogSerializer writer = new WindowsLogSerializer();
            writer.configure(context);
            return writer;
        }
    }
}