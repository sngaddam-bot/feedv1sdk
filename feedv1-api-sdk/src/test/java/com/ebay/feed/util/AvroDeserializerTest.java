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
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AvroDeserializer utility.
 */
class AvroDeserializerTest {

    private AvroDeserializer avroDeserializer;
    private Schema testSchema;
    private File testAvroFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        avroDeserializer = new AvroDeserializer();
        
        // Define a simple test schema
        String schemaString = "{"
            + "\"type\":\"record\","
            + "\"name\":\"TestRecord\","
            + "\"fields\":["
            + "{\"name\":\"id\",\"type\":\"string\"},"
            + "{\"name\":\"name\",\"type\":\"string\"},"
            + "{\"name\":\"price\",\"type\":\"double\"}"
            + "]}";
        testSchema = new Schema.Parser().parse(schemaString);
        
        // Create a test Avro file
        testAvroFile = tempDir.resolve("test.avro").toFile();
        createTestAvroFile(testAvroFile, testSchema);
    }

    @AfterEach
    void tearDown() {
        if (testAvroFile != null && testAvroFile.exists()) {
            testAvroFile.delete();
        }
    }

    /**
     * Creates a test Avro file with sample data.
     */
    private void createTestAvroFile(File file, Schema schema) throws IOException {
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        try (DataFileWriter<GenericRecord> dataFileWriter = 
                new DataFileWriter<>(datumWriter)) {
            dataFileWriter.create(schema, file);
            
            // Create test records
            GenericRecord record1 = new GenericData.Record(schema);
            record1.put("id", "123");
            record1.put("name", "Test Item 1");
            record1.put("price", 99.99);
            dataFileWriter.append(record1);
            
            GenericRecord record2 = new GenericData.Record(schema);
            record2.put("id", "456");
            record2.put("name", "Test Item 2");
            record2.put("price", 149.99);
            dataFileWriter.append(record2);
            
            GenericRecord record3 = new GenericData.Record(schema);
            record3.put("id", "789");
            record3.put("name", "Test Item 3");
            record3.put("price", 199.99);
            dataFileWriter.append(record3);
        }
    }

    @Test
    void testDeserializeAvroFileWithPath() throws IOException {
        List<GenericRecord> records = avroDeserializer.deserializeAvroFile(
            testAvroFile.getAbsolutePath());
        
        assertNotNull(records);
        assertEquals(3, records.size());
        
        // Verify first record
        GenericRecord firstRecord = records.get(0);
        assertEquals("123", firstRecord.get("id").toString());
        assertEquals("Test Item 1", firstRecord.get("name").toString());
        assertEquals(99.99, (Double) firstRecord.get("price"), 0.01);
    }

    @Test
    void testDeserializeAvroFileWithFile() throws IOException {
        List<GenericRecord> records = avroDeserializer.deserializeAvroFile(testAvroFile);
        
        assertNotNull(records);
        assertEquals(3, records.size());
        
        // Verify second record
        GenericRecord secondRecord = records.get(1);
        assertEquals("456", secondRecord.get("id").toString());
        assertEquals("Test Item 2", secondRecord.get("name").toString());
        assertEquals(149.99, (Double) secondRecord.get("price"), 0.01);
    }

    @Test
    void testProcessAvroFile() throws IOException {
        final int[] recordCount = {0};
        final double[] totalPrice = {0.0};
        
        avroDeserializer.processAvroFile(testAvroFile, record -> {
            recordCount[0]++;
            totalPrice[0] += (Double) record.get("price");
        });
        
        assertEquals(3, recordCount[0]);
        assertEquals(449.97, totalPrice[0], 0.01);
    }

    @Test
    void testRecordToJson() throws IOException {
        List<GenericRecord> records = avroDeserializer.deserializeAvroFile(testAvroFile);
        
        String json = avroDeserializer.recordToJson(records.get(0));
        
        assertNotNull(json);
        assertTrue(json.contains("123"));
        assertTrue(json.contains("Test Item 1"));
        assertTrue(json.contains("99.99"));
    }

    @Test
    void testReadSchema() throws IOException {
        Schema schema = avroDeserializer.readSchema(testAvroFile.getAbsolutePath());
        
        assertNotNull(schema);
        assertEquals("TestRecord", schema.getName());
        assertEquals(3, schema.getFields().size());
        assertNotNull(schema.getField("id"));
        assertNotNull(schema.getField("name"));
        assertNotNull(schema.getField("price"));
    }

    @Test
    void testDeserializeBinaryDataWithSchemaString() throws IOException {
        // Create a record
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("id", "999");
        record.put("name", "Binary Test Item");
        record.put("price", 299.99);
        
        // Serialize to binary
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(testSchema);
        org.apache.avro.io.Encoder encoder = 
            org.apache.avro.io.EncoderFactory.get().binaryEncoder(outputStream, null);
        datumWriter.write(record, encoder);
        encoder.flush();
        byte[] binaryData = outputStream.toByteArray();
        
        // Deserialize
        GenericRecord deserializedRecord = avroDeserializer.deserializeBinaryData(
            binaryData, testSchema.toString());
        
        assertNotNull(deserializedRecord);
        assertEquals("999", deserializedRecord.get("id").toString());
        assertEquals("Binary Test Item", deserializedRecord.get("name").toString());
        assertEquals(299.99, (Double) deserializedRecord.get("price"), 0.01);
    }

    @Test
    void testDeserializeBinaryDataWithSchema() throws IOException {
        // Create a record
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("id", "888");
        record.put("name", "Schema Test Item");
        record.put("price", 399.99);
        
        // Serialize to binary
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(testSchema);
        org.apache.avro.io.Encoder encoder = 
            org.apache.avro.io.EncoderFactory.get().binaryEncoder(outputStream, null);
        datumWriter.write(record, encoder);
        encoder.flush();
        byte[] binaryData = outputStream.toByteArray();
        
        // Deserialize
        GenericRecord deserializedRecord = avroDeserializer.deserializeBinaryData(
            binaryData, testSchema);
        
        assertNotNull(deserializedRecord);
        assertEquals("888", deserializedRecord.get("id").toString());
        assertEquals("Schema Test Item", deserializedRecord.get("name").toString());
        assertEquals(399.99, (Double) deserializedRecord.get("price"), 0.01);
    }

    @Test
    void testDeserializeNonExistentFile() {
        assertThrows(IOException.class, () -> {
            avroDeserializer.deserializeAvroFile("non_existent_file.avro");
        });
    }
}
