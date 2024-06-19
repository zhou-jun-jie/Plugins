package com.maxvision.plugin.udp;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class MDnsDiscovery {

    private static final String TAG = "MDnsDiscovery";
    private static final int MDNS_PORT = 5353;
    private static final String MDNS_ADDRESS = "224.0.0.251";
    private static final String SERVICE_TYPE = "_maxvision._tcp.local.";

    private MulticastSocket mSocket;
    private boolean mRunning;

    public void startDiscovery() {
        mRunning = true;
        new DiscoveryTask().execute();
    }

    public void stopDiscovery() {
        mRunning = false;
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
        }
    }

    private class DiscoveryTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String jsonString = null;
            try {
                // Create a MulticastSocket
                mSocket = new MulticastSocket(MDNS_PORT);

                // Join the multicast group
                InetAddress group = InetAddress.getByName(MDNS_ADDRESS);
                mSocket.joinGroup(group);

                // Send mDNS query for the specific service type
                byte[] sendData = createMDnsQuery();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, group, MDNS_PORT);
                mSocket.send(sendPacket);

                // Receive response
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                while (mRunning) {
                    mSocket.receive(receivePacket);

                    // Parse the response to get JSON data
                    /*setTxtRecords(receivePacket.getData());*/
                    Log.e(TAG,"ip:"+receivePacket.getAddress().getHostAddress()+",host:"+receivePacket.getSocketAddress().toString());

                    SystemClock.sleep(100);

                    parseMdnsResponse(receiveData,receiveData.length);

                    if (!mTxtRecord.isEmpty()) {
                        mTxtRecord.forEach((s, bytes) -> {
                            if (bytes != null && bytes.length > 0) {
//                                Log.e(TAG, "key:" + s + ",bytes:" + new String(bytes, StandardCharsets.US_ASCII));
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IO Exception: " + e.getMessage());
            } finally {
                if (mSocket != null && !mSocket.isClosed()) {
                    try {
                        mSocket.leaveGroup(InetAddress.getByName(MDNS_ADDRESS));
                        mSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing socket: " + e.getMessage());
                    }
                }
            }
            return jsonString;
        }

        private byte[] createMDnsQuery() throws IOException {

            // TODO: Implement mDNS query creation based on the mDNS protocol
            // For simplicity, a basic example is provided below (not complete):
            String query = "_maxvision._tcp.local.";
            return query.getBytes();

            /*ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Transaction ID
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(0); // 0x00

            // Flags
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(0); // 0x00

            // Questions
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(1); // 0x01

            // Answer RRs
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(0); // 0x00

            // Authority RRs
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(0); // 0x00

            // Additional RRs
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(0); // 0x00

            // Query name
            String[] parts = SERVICE_TYPE.split("\\.");
            for (String part : parts) {
                byteArrayOutputStream.write(part.length());
                byteArrayOutputStream.write(part.getBytes(StandardCharsets.UTF_8));
            }
            byteArrayOutputStream.write(0); // End of the QNAME

            // Query Type
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(12); // 0x0C

            // Query Class
            byteArrayOutputStream.write(0); // 0x00
            byteArrayOutputStream.write(1); // 0x01

            return byteArrayOutputStream.toByteArray();*/
        }

        private synchronized String parseMdnsResponse(byte[] data, int length) {
            try {
                int position = 12; // Skip the header (12 bytes)
                Log.d(TAG, "Parsing mDNS response");

                // Ensure position is within the received data length
                while (position < length && data[position] != 0) {
                    position += (data[position] & 0xFF) + 1;
                }
                position += 5; // Skip QTYPE and QCLASS

                // Parse the answers section
                while (position + 10 < length) { // Ensure enough bytes to read type, class, TTL, and data length
                    // Skip the Name (variable length)
                    if ((data[position] & 0xC0) == 0xC0) {
                        position += 2; // Compressed name
                    } else {
                        while (position < length && data[position] != 0) {
                            position += (data[position] & 0xFF) + 1;
                        }
                        position++; // Skip the last null byte
                    }

                    // Read Type and Class
                    int type = ((data[position] & 0xFF) << 8) | (data[position + 1] & 0xFF);
                    int cls = ((data[position + 2] & 0xFF) << 8) | (data[position + 3] & 0xFF);
                    Log.d(TAG, "Type: " + type + ", Class: " + cls);
                    position += 4; // Move to TTL

                    // Skip TTL
                    position += 4;

                    // Read Data Length
                    int dataLength = ((data[position] & 0xFF) << 8) | (data[position + 1] & 0xFF);
                    position += 2;
                    Log.d(TAG, "Data Length: " + dataLength);

                    // Check if it's a TXT record (type 16)
                    if (type == 16 && position + dataLength <= length) {
                        byte[] txtData = Arrays.copyOfRange(data, position, position + dataLength);
                        setTxtRecords(txtData);
                    }

                    // Move to the next resource record
                    position += dataLength;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing mDNS response: " + e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String jsonString) {
            // Do something with the discovered JSON data
            if (jsonString != null) {
                // Example: Parse and use the JSON data
                parseAndUseJson(jsonString);
            }
        }

        private void parseAndUseJson(String jsonString) {
            // Example: Parse and use the JSON data
            try {
                // Assuming jsonString contains JSON data, parse it here
                // Example:
                // JSONObject jsonObject = new JSONObject(jsonString);
                // String deviceName = jsonObject.getString("name");
                // String deviceType = jsonObject.getString("type");
                // ...
                Log.d(TAG, "Parsed JSON data: " + jsonString);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
            }
        }
    }

    private final ArrayMap<String, byte[]> mTxtRecord = new ArrayMap<>();

    public void setTxtRecords(byte[] txtRecordsRawBytes) {
        // There can be multiple TXT records after each other. Each record has to following format:
        //
        // byte                  type                  required   meaning
        // -------------------   -------------------   --------   ----------------------------------
        // 0                     unsigned 8 bit        yes        size of record excluding this byte
        // 1 - n                 ASCII but not '='     yes        key
        // n + 1                 '='                   optional   separator of key and value
        // n + 2 - record size   uninterpreted bytes   optional   value
        //
        // Example legal records:
        // [11, 'm', 'y', 'k', 'e', 'y', '=', 0x0, 0x4, 0x65, 0x7, 0xff]
        // [17, 'm', 'y', 'K', 'e', 'y', 'W', 'i', 't', 'h', 'N', 'o', 'V', 'a', 'l', 'u', 'e', '=']
        // [12, 'm', 'y', 'B', 'o', 'o', 'l', 'e', 'a', 'n', 'K', 'e', 'y']
        //
        // Example corrupted records
        // [3, =, 1, 2]    <- key is empty
        // [3, 0, =, 2]    <- key contains non-ASCII character. We handle this by replacing the
        //                    invalid characters instead of skipping the record.
        // [30, 'a', =, 2] <- length exceeds total left over bytes in the TXT records array, we
        //                    handle this by reducing the length of the record as needed.
        int pos = 0;
        while (pos < txtRecordsRawBytes.length) {
            // recordLen is an unsigned 8 bit value
            int recordLen = txtRecordsRawBytes[pos] & 0xff;
            pos += 1;

            try {
                if (recordLen == 0) {
                    throw new IllegalArgumentException("Zero sized txt record");
                } else if (pos + recordLen > txtRecordsRawBytes.length) {
                    Log.w(TAG, "Corrupt record length (pos = " + pos + "): " + recordLen);
                    recordLen = txtRecordsRawBytes.length - pos;
                }

                // Decode key-value records
                String key = null;
                byte[] value = null;
                int valueLen = 0;
                for (int i = pos; i < pos + recordLen; i++) {
                    if (key == null) {
                        if (txtRecordsRawBytes[i] == '=') {
                            key = new String(txtRecordsRawBytes, pos, i - pos,
                                    StandardCharsets.US_ASCII);
                        }
                    } else {
                        if (value == null) {
                            value = new byte[recordLen - key.length() - 1];
                        }
                        value[valueLen] = txtRecordsRawBytes[i];
                        valueLen++;
                    }
                }

                // If '=' was not found we have a boolean record
                if (key == null) {
                    key = new String(txtRecordsRawBytes, pos, recordLen, StandardCharsets.US_ASCII);
                }

                if (TextUtils.isEmpty(key)) {
                    // Empty keys are not allowed (RFC6763 6.4)
                    throw new IllegalArgumentException("Invalid txt record (key is empty)");
                }

                if (getAttributes().containsKey(key)) {
                    // When we have a duplicate record, the later ones are ignored (RFC6763 6.4)
                    throw new IllegalArgumentException("Invalid txt record (duplicate key \"" + key + "\")");
                }

                setAttribute(key, value);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "While parsing txt records (pos = " + pos + "): " + e.getMessage());
            }

            pos += recordLen;
        }
    }

    public void setAttribute(String key, byte[] value) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }

        // Key must be printable US-ASCII, excluding =.
        for (int i = 0; i < key.length(); ++i) {
            char character = key.charAt(i);
            if (character < 0x20 || character > 0x7E) {
                throw new IllegalArgumentException("Key strings must be printable US-ASCII");
            } else if (character == 0x3D) {
                throw new IllegalArgumentException("Key strings must not include '='");
            }
        }

        // Key length + value length must be < 255.
        if (key.length() + (value == null ? 0 : value.length) >= 255) {
            throw new IllegalArgumentException("Key length + value length must be < 255 bytes");
        }

        // Warn if key is > 9 characters, as recommended by RFC 6763 section 6.4.
        if (key.length() > 9) {
            Log.w(TAG, "Key lengths > 9 are discouraged: " + key);
        }

        // Check against total TXT record size limits.
        // Arbitrary 400 / 1300 byte limits taken from RFC 6763 section 6.2.
        int txtRecordSize = getTxtRecordSize();
        int futureSize = txtRecordSize + key.length() + (value == null ? 0 : value.length) + 2;
        if (futureSize > 1300) {
            throw new IllegalArgumentException("Total length of attributes must be < 1300 bytes");
        } else if (futureSize > 400) {
            Log.w(TAG, "Total length of all attributes exceeds 400 bytes; truncation may occur");
        }

        mTxtRecord.put(key, value);
    }

    public Map<String, byte[]> getAttributes() {
        return Collections.unmodifiableMap(mTxtRecord);
    }

    private int getTxtRecordSize() {
        int txtRecordSize = 0;
        for (Map.Entry<String, byte[]> entry : mTxtRecord.entrySet()) {
            txtRecordSize += 2;  // One for the length byte, one for the = between key and value.
            txtRecordSize += entry.getKey().length();
            byte[] value = entry.getValue();
            txtRecordSize += value == null ? 0 : value.length;
        }
        return txtRecordSize;
    }
}
