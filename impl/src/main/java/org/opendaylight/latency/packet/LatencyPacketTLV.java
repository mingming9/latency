/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.packet;

import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.Packet;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class LatencyPacketTLV extends Packet {


/**
 * Class that represents the LLDPTLV objects
 */


    private static final String TYPE = "Type";
    private static final String LENGTH = "Length";
    private static final String VALUE = "Value";
    private static final int LLDPTLVFields = 3;

    /** OpenFlow OUI */
    public static final byte[] OFOUI = new byte[] { (byte) 0x00, (byte) 0x26,
        (byte) 0xe1 };

    /** Length of Organizationally defined subtype field of TLV in bytes   */
    private static final byte customTlvSubTypeLength = (byte)1;

    /** OpenFlow subtype: nodeConnectorId of source */
    public static final byte[] CUSTOM_TLV_SUB_TYPE_NODE_CONNECTOR_ID = new byte[] { 0 };

    /** OpenFlow subtype: custom sec = hash code of verification of origin of LLDP */
    public static final byte[] CUSTOM_TLV_SUB_TYPE_CUSTOM_SEC = new byte[] { 1 };

    public static final int customTlvOffset = OFOUI.length + customTlvSubTypeLength;
    public static final byte chassisIDSubType[] = new byte[] { 4 }; // MAC address for the system
    public static final byte portIDSubType[] = new byte[] { 7 }; // locally assigned

    private static final Logger LOG = LoggerFactory.getLogger(LatencyPacketTLV.class);

    public enum TLVType {
        Unknown((byte) 0), ChassisID((byte) 1), PortID((byte) 2), TTL((byte) 3), PortDesc(
                (byte) 4), SystemName((byte) 5), SystemDesc((byte) 6), Custom(
                        (byte) 127);

        private byte value;

        TLVType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    private static Map<String, Pair<Integer, Integer>> fieldCoordinates = new LinkedHashMap<String, Pair<Integer, Integer>>() {
        private static final long serialVersionUID = 1L;

        {
            put(TYPE, new MutablePair<>(0, 7));
            put(LENGTH, new MutablePair<>(7, 9));
            put(VALUE, new MutablePair<>(16, 0));
        }
    };

    protected Map<String, byte[]> fieldValues;


    public LatencyPacketTLV() {
        payload = null;
        fieldValues = new HashMap<>(LLDPTLVFields);
        hdrFieldCoordMap = fieldCoordinates;
        hdrFieldsMap = fieldValues;
    }


    public LatencyPacketTLV(LatencyPacketTLV other) {
        for (Map.Entry<String, byte[]> entry : other.hdrFieldsMap.entrySet()) {
            this.hdrFieldsMap.put(entry.getKey(), entry.getValue());
        }
    }


    public int getLength() {
        return (int) BitBufferHelper.toNumber(fieldValues.get(LENGTH),
                fieldCoordinates.get(LENGTH).getRight().intValue());
    }


    public byte getType() {
        return BitBufferHelper.getByte(fieldValues.get(TYPE));
    }


    public byte[] getValue() {
        return fieldValues.get(VALUE);
    }


    public LatencyPacketTLV setType(byte type) {
        byte[] lldpTLVtype = { type };
        fieldValues.put(TYPE, lldpTLVtype);
        return this;
    }


    public LatencyPacketTLV setLength(short length) {
        fieldValues.put(LENGTH, BitBufferHelper.toByteArray(length));
        return this;
    }

    public LatencyPacketTLV setValue(byte[] value) {
        fieldValues.put(VALUE, value);
        return this;
    }

    @Override
    public void setHeaderField(String headerField, byte[] readValue) {
        hdrFieldsMap.put(headerField, readValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((fieldValues == null) ? 0 : fieldValues.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LatencyPacketTLV other = (LatencyPacketTLV) obj;
        if (fieldValues == null) {
            if (other.fieldValues != null) {
                return false;
            }
        } else if (!fieldValues.equals(other.fieldValues)) {
            return false;
        }
        return true;
    }

    @Override
    public int getfieldnumBits(String fieldName) {
        if (fieldName.equals(VALUE)) {
            return (NetUtils.NumBitsInAByte * BitBufferHelper.getShort(
                    fieldValues.get(LENGTH), fieldCoordinates.get(LENGTH)
                    .getRight().intValue()));
        }
        return fieldCoordinates.get(fieldName).getRight();
    }


    public int getTLVSize() {
        return (LatencyPacketTLV.fieldCoordinates.get(TYPE).getRight() + // static
        		LatencyPacketTLV.fieldCoordinates.get(LENGTH).getRight() + // static
                getfieldnumBits(VALUE)); // variable
    }


    static public byte[] createSystemNameTLVValue(String nodeId) {
        byte[] nid = nodeId.getBytes();
        return nid;
    }


    static public byte[] createChassisIDTLVValue(String nodeId) {
        byte[] nid = HexEncode.bytesFromHexString(nodeId);
        byte[] cid = new byte[6];
        int srcPos = 0, dstPos = 0;

        if (nid.length > cid.length) {
            srcPos = nid.length - cid.length;
        } else {
            dstPos = cid.length - nid.length;
        }
        System.arraycopy(nid, srcPos, cid, dstPos, cid.length);

        byte[] cidValue = new byte[cid.length + chassisIDSubType.length];

        System.arraycopy(chassisIDSubType, 0, cidValue, 0,
                chassisIDSubType.length);
        System.arraycopy(cid, 0, cidValue, chassisIDSubType.length, cid.length);

        return cidValue;
    }


    static public byte[] createPortIDTLVValue(String portId) {
        byte[] pid = portId.getBytes(Charset.defaultCharset());
        byte[] pidValue = new byte[pid.length + portIDSubType.length];

        System.arraycopy(portIDSubType, 0, pidValue, 0, portIDSubType.length);
        System.arraycopy(pid, 0, pidValue, portIDSubType.length, pid.length);

        return pidValue;
    }


    static public byte[] createCustomTLVValue(String customString) {
        byte[] customByteArray = customString.getBytes(Charset.defaultCharset());
        return createCustomTLVValue(CUSTOM_TLV_SUB_TYPE_NODE_CONNECTOR_ID, customByteArray);
    }


    static public byte[] createCustomTLVValue(byte[] subtype, byte[] customByteArray) {
        byte[] customValue = new byte[customTlvOffset + customByteArray.length];

        System.arraycopy(OFOUI, 0, customValue, 0, OFOUI.length);
        System.arraycopy(subtype, 0, customValue, OFOUI.length, 1);
        System.arraycopy(customByteArray, 0, customValue, customTlvOffset,
                customByteArray.length);

        return customValue;
    }


    static public String getHexStringValue(byte[] tlvValue, int tlvLen) {
        byte[] cidBytes = new byte[tlvLen - chassisIDSubType.length];
        System.arraycopy(tlvValue, chassisIDSubType.length, cidBytes, 0,
                cidBytes.length);
        return HexEncode.bytesToHexStringFormat(cidBytes);
    }


    static public String getStringValue(byte[] tlvValue, int tlvLen) {
        byte[] pidSubType = new byte[portIDSubType.length];
        byte[] pidBytes = new byte[tlvLen - portIDSubType.length];
        System.arraycopy(tlvValue, 0, pidSubType, 0,
                pidSubType.length);
        System.arraycopy(tlvValue, portIDSubType.length, pidBytes, 0,
                pidBytes.length);
        if (pidSubType[0] == (byte) 0x3) {
            return HexEncode.bytesToHexStringFormat(pidBytes);
        } else {
            return (new String(pidBytes, Charset.defaultCharset()));
        }
    }


    static public String getCustomString(byte[] customTlvValue, int customTlvLen) {
        String customString = "";
        byte[] vendor = new byte[3];
        System.arraycopy(customTlvValue, 0, vendor, 0, vendor.length);
        if (Arrays.equals(vendor, LatencyPacketTLV.OFOUI)) {
            int customArrayLength = customTlvLen - customTlvOffset;
            byte[] customArray = new byte[customArrayLength];
            System.arraycopy(customTlvValue, customTlvOffset, customArray, 0,
                    customArrayLength);
            try {
                customString = new String(customArray, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
        }

        return customString;
    }

    public static int extractCustomOUI(final LatencyPacketTLV lldptlv) {
        byte[] value = lldptlv.getValue();
        return BitBufferHelper.getInt(ArrayUtils.subarray(value, 0, 3));
    }

    public static byte extractCustomSubtype(final LatencyPacketTLV lldptlv) {
        byte[] value = lldptlv.getValue();
        return BitBufferHelper.getByte(ArrayUtils.subarray(value, 3, 4));
    }
}
