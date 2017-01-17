/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.util;

import static org.opendaylight.controller.liblldp.LLDPTLV.CUSTOM_TLV_SUB_TYPE_CUSTOM_SEC;
import static org.opendaylight.openflowplugin.applications.topology.lldp.utils.LLDPDiscoveryUtils.getValueForLLDPPacketIntegrityEnsuring;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.HexEncode;
//import org.opendaylight.controller.liblldp.LLDP;
//import org.opendaylight.controller.liblldp.LLDPTLV;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.latency.packet.LatencyPacket;
import org.opendaylight.latency.packet.LatencyPacketTLV;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class LatencyPacketUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LatencyPacketUtil.class);
    private static final String OF_URI_PREFIX = "openflow:";

    public static byte[] buildLldpFrame(final NodeId nodeId,
            final NodeConnectorId nodeConnectorId, final MacAddress src, final Long outPortNo,
            final MacAddress destinationAddress) {
        // Create discovery pkt
    	LatencyPacket discoveryPkt = new LatencyPacket();

        // Create LLDP ChassisID TLV
        BigInteger dataPathId = dataPathIdFromNodeId(nodeId);
        byte[] cidValue = LatencyPacketTLV
                .createChassisIDTLVValue(colonize(bigIntegerToPaddedHex(dataPathId)));
        LatencyPacketTLV chassisIdTlv = new LatencyPacketTLV();
        chassisIdTlv.setType(LatencyPacketTLV.TLVType.ChassisID.getValue());
        chassisIdTlv.setType(LatencyPacketTLV.TLVType.ChassisID.getValue())
                .setLength((short) cidValue.length).setValue(cidValue);
        discoveryPkt.setChassisId(chassisIdTlv);

        // Create LLDP PortID TL
        String hexString = Long.toHexString(outPortNo);
        byte[] pidValue = LatencyPacketTLV.createPortIDTLVValue(hexString);
        LatencyPacketTLV portIdTlv = new LatencyPacketTLV();
        portIdTlv.setType(LatencyPacketTLV.TLVType.PortID.getValue())
                .setLength((short) pidValue.length).setValue(pidValue);
        portIdTlv.setType(LatencyPacketTLV.TLVType.PortID.getValue());
        discoveryPkt.setPortId(portIdTlv);

        // Create LLDP TTL TLV
        byte[] ttl = new byte[] { (byte) 0x13, (byte) 0x37 };
        LatencyPacketTLV ttlTlv = new LatencyPacketTLV();
        ttlTlv.setType(LatencyPacketTLV.TLVType.TTL.getValue())
                .setLength((short) ttl.length).setValue(ttl);
        discoveryPkt.setTtl(ttlTlv);

        // Create LLDP SystemName TLV
        byte[] snValue = LatencyPacketTLV.createSystemNameTLVValue(nodeId.getValue());
        LatencyPacketTLV systemNameTlv = new LatencyPacketTLV();
        systemNameTlv.setType(LatencyPacketTLV.TLVType.SystemName.getValue());
        systemNameTlv.setType(LatencyPacketTLV.TLVType.SystemName.getValue())
                .setLength((short) snValue.length).setValue(snValue);
        discoveryPkt.setSystemNameId(systemNameTlv);

        // Create LLDP Custom TLV
        byte[] customValue = LatencyPacketTLV.createCustomTLVValue(nodeConnectorId
                .getValue());
        LatencyPacketTLV customTlv = new LatencyPacketTLV();
        customTlv.setType(LatencyPacketTLV.TLVType.Custom.getValue())
                .setLength((short) customValue.length).setValue(customValue);
        discoveryPkt.addCustomTLV(customTlv);

        //Create LLDP CustomSec TLV
        byte[] pureValue = new byte[1];
        try {
            pureValue = getValueForLLDPPacketIntegrityEnsuring(nodeConnectorId);
            byte[] customSecValue = LatencyPacketTLV.createCustomTLVValue(CUSTOM_TLV_SUB_TYPE_CUSTOM_SEC, pureValue);
            LatencyPacketTLV customSecTlv = new LatencyPacketTLV();
            customSecTlv.setType(LatencyPacketTLV.TLVType.Unknown.getValue())
            .setLength((short)customSecValue.length)
            .setValue(customSecValue);
            discoveryPkt.addCustomTLV(customSecTlv);
            System.out.println("custonSecTlv type is " + customSecTlv.getType());
        } catch (NoSuchAlgorithmException e1) {
            LOG.info("LLDP extra authenticator creation failed: {}", e1.getMessage());
            LOG.debug("Reason why LLDP extra authenticator creation failed: ", e1);
        }


        // Create ethernet pkt
        byte[] sourceMac = HexEncode.bytesFromHexString(src.getValue());
        Ethernet ethPkt = new Ethernet();
        ethPkt.setSourceMACAddress(sourceMac)
                .setEtherType(EtherTypes.LLDP.shortValue())
                .setPayload(discoveryPkt);
        if (destinationAddress == null) {
            ethPkt.setDestinationMACAddress(LatencyPacket.LLDPMulticastMac);
        } else {
            ethPkt.setDestinationMACAddress(HexEncode
                    .bytesFromHexString(destinationAddress.getValue()));
        }

        try {
            return ethPkt.serialize();
        } catch (PacketException e) {
            LOG.warn("Error creating LLDP packet: {}", e.getMessage());
            LOG.debug("Error creating LLDP packet.. ", e);
        }
        return null;
    }

    private static String colonize(final String orig) {
        return orig.replaceAll("(?<=..)(..)", ":$1");
    }

    private static BigInteger dataPathIdFromNodeId(final NodeId nodeId) {
        String dpids = nodeId.getValue().replace(OF_URI_PREFIX, "");
        return new BigInteger(dpids);
    }

    private static String bigIntegerToPaddedHex(final BigInteger dataPathId) {
        return StringUtils.leftPad(dataPathId.toString(16), 16, "0");
    }

    static byte[] buildLldpFrame(final NodeId nodeId,
            final NodeConnectorId nodeConnectorId, final MacAddress srcMacAddress,
            final Long outputPortNo) {
        return buildLldpFrame(nodeId, nodeConnectorId, srcMacAddress,
                outputPortNo, null);
    }
}
