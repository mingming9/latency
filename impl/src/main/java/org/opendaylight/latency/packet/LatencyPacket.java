/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.packet;

import com.google.common.collect.Iterables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.liblldp.BitBufferHelper;
import org.opendaylight.controller.liblldp.BufferException;
import org.opendaylight.controller.liblldp.CustomTLVKey;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.liblldp.PacketException;

public class LatencyPacket extends Packet {

/**
 * Class that represents the LLDP frame objects
 */
    private static final String CHASSISID = "ChassisId";
    private static final String SYSTEMNAMEID = "SystemNameID";
    private static final String PORTID = "PortId";
    private static final String TTL = "TTL";
    private static final int LLDPDefaultTlvs = 3;
    private static final LatencyPacketTLV emptyTLV = new LatencyPacketTLV().setLength((short) 0).setType((byte) 0);
    public static final byte[] LLDPMulticastMac = { 1, (byte) 0x80, (byte) 0xc2, 0, 0, (byte) 0xe };

    private Map<Byte, LatencyPacketTLV> mandatoryTLVs;
    private Map<Byte, LatencyPacketTLV> optionalTLVs;
    private Map<CustomTLVKey, LatencyPacketTLV> customTLVs;


    public LatencyPacket() {
        super();
        init();
    }


    public LatencyPacket(boolean writeAccess) {
        super(writeAccess);
        init();
    }

    private void init() {
        mandatoryTLVs = new LinkedHashMap<>(LLDPDefaultTlvs);
        optionalTLVs = new LinkedHashMap<>();
        customTLVs = new LinkedHashMap<>();
    }


    private byte getType(String typeDesc) {
        if (typeDesc.equals(CHASSISID)) {
            return LatencyPacketTLV.TLVType.ChassisID.getValue();
        } else if (typeDesc.equals(PORTID)) {
            return LatencyPacketTLV.TLVType.PortID.getValue();
        } else if (typeDesc.equals(TTL)) {
            return LatencyPacketTLV.TLVType.TTL.getValue();
        } else if (typeDesc.equals(SYSTEMNAMEID)) {
            return LatencyPacketTLV.TLVType.SystemName.getValue();
        } else {
            return LatencyPacketTLV.TLVType.Unknown.getValue();
        }
    }

    private LatencyPacketTLV getFromTLVs(Byte type) {
    	LatencyPacketTLV tlv = null;
        tlv = mandatoryTLVs.get(type);
        if (tlv == null) {
            tlv = optionalTLVs.get(type);
        }
        return tlv;
    }

    private void putToTLVs(final Byte type, final LatencyPacketTLV tlv) {
        if (type == LatencyPacketTLV.TLVType.ChassisID.getValue() || type == LatencyPacketTLV.TLVType.PortID.getValue()
                || type == LatencyPacketTLV.TLVType.TTL.getValue()) {
            mandatoryTLVs.put(type, tlv);
        } else if (type != LatencyPacketTLV.TLVType.Custom.getValue()) {
            optionalTLVs.put(type, tlv);
        }
    }


    public LatencyPacketTLV getTLV(String type) {
        return getFromTLVs(getType(type));
    }

    public LatencyPacketTLV getCustomTLV(CustomTLVKey key) {
        return customTLVs.get(key);
    }


    public void setTLV(String type, LatencyPacketTLV tlv) {
        putToTLVs(getType(type), tlv);
    }


    public LatencyPacketTLV getChassisId() {
        return getTLV(CHASSISID);
    }


    public LatencyPacket setChassisId(LatencyPacketTLV chassisId) {
        setTLV(CHASSISID, chassisId);
        return this;
    }


    public LatencyPacketTLV getSystemNameId() {
        return getTLV(SYSTEMNAMEID);
    }


    public LatencyPacket setSystemNameId(LatencyPacketTLV systemNameId) {
        setTLV(SYSTEMNAMEID, systemNameId);
        return this;
    }


    public LatencyPacketTLV getPortId() {
        return getTLV(PORTID);
    }


    public LatencyPacket setPortId(LatencyPacketTLV portId) {
        setTLV(PORTID, portId);
        return this;
    }


    public LatencyPacketTLV getTtl() {
        return getTLV(TTL);
    }


    public LatencyPacket setTtl(LatencyPacketTLV ttl) {
        setTLV(TTL, ttl);
        return this;
    }


    public Iterable<LatencyPacketTLV> getOptionalTLVList() {
        return optionalTLVs.values();
    }


    public Iterable<LatencyPacketTLV> getCustomTlvList() {
        return customTLVs.values();
    }


    public LatencyPacket setOptionalTLVList(List<LatencyPacketTLV> optionalTLVList) {
        for (LatencyPacketTLV tlv : optionalTLVList) {
            optionalTLVs.put(tlv.getType(), tlv);
        }
        return this;
    }


    public LatencyPacket addCustomTLV(final LatencyPacketTLV customTLV) {
        CustomTLVKey key = new CustomTLVKey(LatencyPacketTLV.extractCustomOUI(customTLV),
        		LatencyPacketTLV.extractCustomSubtype(customTLV));
        customTLVs.put(key, customTLV);

        return this;
    }

    @Override
    public Packet deserialize(byte[] data, int bitOffset, int size) throws PacketException {
        int lldpOffset = bitOffset; // LLDP start
        int lldpSize = size; // LLDP size

        if (logger.isTraceEnabled()) {
            logger.trace("LLDP: {} (offset {} bitsize {})", new Object[] { HexEncode.bytesToHexString(data),
                    lldpOffset, lldpSize });
        }
        /*
         * Deserialize the TLVs until we reach the end of the packet
         */
        while (lldpSize > 0) {
        	LatencyPacketTLV tlv = new LatencyPacketTLV();
            tlv.deserialize(data, lldpOffset, lldpSize);
            if (tlv.getType() == 0 && tlv.getLength() == 0) {
                break;
            }
            int tlvSize = tlv.getTLVSize(); // Size of current TLV in bits
            lldpOffset += tlvSize;
            lldpSize -= tlvSize;
            if (tlv.getType() == LatencyPacketTLV.TLVType.Custom.getValue()) {
                addCustomTLV(tlv);
            } else {
                this.putToTLVs(tlv.getType(), tlv);
            }
        }
        return this;
    }

    @Override
    public byte[] serialize() throws PacketException {
        int startOffset = 0;
        byte[] serializedBytes = new byte[getLLDPPacketLength()];

        final Iterable<LatencyPacketTLV> allTlvs = Iterables.concat(mandatoryTLVs.values(), optionalTLVs.values(), customTLVs.values());
        for (LatencyPacketTLV tlv : allTlvs) {
            int numBits = tlv.getTLVSize();
            try {
                BitBufferHelper.setBytes(serializedBytes, tlv.serialize(), startOffset, numBits);
            } catch (BufferException e) {
                throw new PacketException(e.getMessage());
            }
            startOffset += numBits;
        }
        // Now add the empty LLDPTLV at the end
        try {
            BitBufferHelper.setBytes(serializedBytes, LatencyPacket.emptyTLV.serialize(), startOffset,
            		LatencyPacket.emptyTLV.getTLVSize());
        } catch (BufferException e) {
            throw new PacketException(e.getMessage());
        }

        if (logger.isTraceEnabled()) {
            logger.trace("LLDP: serialized: {}", HexEncode.bytesToHexString(serializedBytes));
        }
        return serializedBytes;
    }


    private int getLLDPPacketLength() {
        int len = 0;

        for (LatencyPacketTLV lldptlv : Iterables.concat(mandatoryTLVs.values(), optionalTLVs.values(), customTLVs.values())) {
            len += lldptlv.getTLVSize();
        }

        len += LatencyPacket.emptyTLV.getTLVSize();

        return len / NetUtils.NumBitsInAByte;
    }
}