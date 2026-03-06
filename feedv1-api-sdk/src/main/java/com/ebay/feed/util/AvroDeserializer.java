/*
 * *
 *  * Copyright 2024 eBay Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 */
package com.ebay.feed.util;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for deserializing Avro data.
 * Provides methods to read Avro files and convert Avro records to Java objects.
 * 
 * <p>This utility supports:
 * <ul>
 *   <li>Deserializing Avro container files</li>
 *   <li>Deserializing binary Avro data with schema</li>
 *   <li>Streaming large Avro files</li>
 *   <li>Converting Avro records to JSON</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class AvroDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(
        AvroDeserializer.class.getName());

    /**
     * Deserializes an Avro container file and returns a list of GenericRecords.
     * 
     * @param avroFilePath the path to the Avro container file
     * @return list of GenericRecord objects
     * @throws IOException if an I/O error occurs
     */
    public List<GenericRecord> deserializeAvroFile(String avroFilePath) throws IOException {
        File avroFile = new File(avroFilePath);
        return deserializeAvroFile(avroFile);
    }

    /**
     * Deserializes an Avro container file and returns a list of GenericRecords.
     * 
     * @param avroFile the Avro container file
     * @return list of GenericRecord objects
     * @throws IOException if an I/O error occurs
     */
    public List<GenericRecord> deserializeAvroFile(File avroFile) throws IOException {
        List<GenericRecord> records = new ArrayList<>();
        
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        try (DataFileReader<GenericRecord> dataFileReader = 
                new DataFileReader<>(avroFile, datumReader)) {
            
            logger.debug("Schema: {}", dataFileReader.getSchema().toString(true));
            
            while (dataFileReader.hasNext()) {
                GenericRecord record = dataFileReader.next();
                records.add(record);
            }
            
            logger.info("Successfully deserialized {} records from {}", 
                records.size(), avroFile.getName());
        }
        
        return records;
    }

    /**
     * Deserializes an Avro container file from an InputStream and returns a list of GenericRecords.
     * 
     * @param inputStream the input stream containing Avro data
     * @return list of GenericRecord objects
     * @throws IOException if an I/O error occurs
     */
    public List<GenericRecord> deserializeAvroStream(InputStream inputStream) throws IOException {
        List<GenericRecord> records = new ArrayList<>();
        
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        try (DataFileStream<GenericRecord> dataFileStream = 
                new DataFileStream<>(inputStream, datumReader)) {
            
            logger.debug("Schema: {}", dataFileStream.getSchema().toString(true));
            
            while (dataFileStream.hasNext()) {
                GenericRecord record = dataFileStream.next();
                records.add(record);
            }
            
            logger.info("Successfully deserialized {} records from stream", records.size());
        }
        
        return records;
    }

    /**
     * Deserializes binary Avro data with a provided schema.
     * 
     * @param binaryData the binary Avro data
     * @param schemaString the Avro schema as a JSON string
     * @return GenericRecord object
     * @throws IOException if an I/O error occurs
     */
    public GenericRecord deserializeBinaryData(byte[] binaryData, String schemaString) 
            throws IOException {
        Schema schema = new Schema.Parser().parse(schemaString);
        return deserializeBinaryData(binaryData, schema);
    }

    /**
     * Deserializes binary Avro data with a provided schema.
     * 
     * @param binaryData the binary Avro data
     * @param schema the Avro schema
     * @return GenericRecord object
     * @throws IOException if an I/O error occurs
     */
    public GenericRecord deserializeBinaryData(byte[] binaryData, Schema schema) 
            throws IOException {
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(binaryData, null);
        return datumReader.read(null, decoder);
    }

    /**
     * Processes an Avro file with a custom record handler (useful for large files).
     * This method allows streaming processing without loading all records into memory.
     * 
     * @param avroFilePath the path to the Avro container file
     * @param recordHandler the handler to process each record
     * @throws IOException if an I/O error occurs
     */
    public void processAvroFile(String avroFilePath, RecordHandler recordHandler) 
            throws IOException {
        File avroFile = new File(avroFilePath);
        processAvroFile(avroFile, recordHandler);
    }

    /**
     * Processes an Avro file with a custom record handler (useful for large files).
     * This method allows streaming processing without loading all records into memory.
     * 
     * @param avroFile the Avro container file
     * @param recordHandler the handler to process each record
     * @throws IOException if an I/O error occurs
     */
    public void processAvroFile(File avroFile, RecordHandler recordHandler) 
            throws IOException {
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        try (DataFileReader<GenericRecord> dataFileReader = 
                new DataFileReader<>(avroFile, datumReader)) {
            
            logger.debug("Processing file with schema: {}", 
                dataFileReader.getSchema().toString(true));
            
            int count = 0;
            while (dataFileReader.hasNext()) {
                GenericRecord record = dataFileReader.next();
                recordHandler.handle(record);
                count++;
            }
            
            logger.info("Successfully processed {} records from {}", 
                count, avroFile.getName());
        }
    }

    /**
     * Converts a GenericRecord to a properly formatted JSON string.
     * Uses Avro's JsonEncoder to ensure valid JSON output.
     * 
     * @param record the GenericRecord to convert
     * @return valid JSON string representation
     * @throws IOException if an error occurs during JSON encoding
     */
    public String recordToJson(GenericRecord record) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(record.getSchema());
        
        writer.write(record, jsonEncoder);
        jsonEncoder.flush();
        
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Reads the schema from an Avro file.
     * 
     * @param avroFilePath the path to the Avro file
     * @return the Avro schema
     * @throws IOException if an I/O error occurs
     */
    public Schema readSchema(String avroFilePath) throws IOException {
        File avroFile = new File(avroFilePath);
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        try (DataFileReader<GenericRecord> dataFileReader = 
                new DataFileReader<>(avroFile, datumReader)) {
            return dataFileReader.getSchema();
        }
    }

    /**
     * Functional interface for processing individual Avro records.
     * Useful for streaming large files without loading all records into memory.
     */
    @FunctionalInterface
    public interface RecordHandler {
        /**
         * Processes a single Avro record.
         * 
         * @param record the GenericRecord to process
         * @throws IOException if an I/O error occurs during processing
         */
        void handle(GenericRecord record) throws IOException;
    }
}
