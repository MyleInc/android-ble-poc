package com.getmyle.mylesdk;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by mikalai on 2015-11-04.
 */
public class Utils {

    /**
     * FIXME
     * Get name of scan device for advertisement.
     * Android core can't read name.
     * You can use another app on Google Play to verify
     *
     * @param scanRecord
     * @return device name
     */
    public static String getNameByScanRecord(byte[] scanRecord) {
        int nameLength = 0;
        int i = 23;	 /* Start value of name*/

        // Get name's length
        do {
            nameLength++;
            i++;
        } while (scanRecord[i] != 0);

        // Get name
        byte[] nameArr = new byte[nameLength];
        int k = 0;
        for (i = 23; i < 23 + nameLength; i++) {
            nameArr[k] = scanRecord[i];
            k++;
        }

        // Convert to string
        String name = "";
        try {
            name = new String(nameArr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return name;
    }


    /**
     * ScanFilter is not working.
     * This is workaround from http://stackoverflow.com/a/24539704/444630
     *
     * @param advertisedData
     * @return List of UUIDs
     */
    public static List<UUID> parseUuidsByAdvertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }


    public static boolean startsWith(byte[] a, byte[] b) {
        for (int i = 0; i < b.length; i += 1) {
            if (i >= a.length || a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }


    public static int toInt(byte[] byteData) {
        int dv = byteData[2] - 48;
        int ch = byteData[1] - 48;
        int ngh = byteData[0] - 48;

        return dv + ch * 10 + ngh * 100;
    }


    public static int extractInt(byte[] raw, byte[] param) {
        byte[] value = Arrays.copyOfRange(raw, param.length, raw.length);
        return toInt(value);
    }


    public static String extractString(byte[] raw, byte[] param) {
        byte[] value = Arrays.copyOfRange(raw, param.length, raw.length);
        return new String(value);
    }

}
