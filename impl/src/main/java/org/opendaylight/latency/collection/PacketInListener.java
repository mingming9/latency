/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyPacketParserUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInListener implements PacketProcessingListener {

	private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);


	private LatencyPacketSender latencyPacketSender;

	public PacketInListener(LatencyPacketSender latencyPacketSender) {
		this.latencyPacketSender = latencyPacketSender;
	}

	@Override
	public void onPacketReceived(PacketReceived latencyPkt) {
		if (LatencyPacketParserUtil.checkLatencyPacket(latencyPkt.getPayload())) {
			Date date = new Date();
    		Long srcTime = date.getTime();
			NodeConnectorRef src = LatencyPacketParserUtil.lldpToNodeConnectorRef(latencyPkt.getPayload(),false);
			Map<NodeConnectorRef, Long> la = latencyPacketSender.pktOutTimeMap;
			LOG.info("Size gotten by pktIn is " + la.size());
        	if (src != null && latencyPacketSender.pktOutTimeMap.containsKey(src)) {        		
        		Long lldpTime = srcTime - latencyPacketSender.pktOutTimeMap.get(src);

        		NodeConnectorRef dst = latencyPkt.getIngress();
        		NodeId srcNodeId = InventoryUtil.getNodeIdFromNodeConnectorRef(src);
        		BigInteger srcdpId = LatencyUtil.getDpId(srcNodeId);
        		NodeId dstNodeId = InventoryUtil.getNodeIdFromNodeConnectorRef(dst);
        		BigInteger dstdpId = LatencyUtil.getDpId(dstNodeId);
        		System.out.println("LatencyPkt time from Node " + srcdpId + " to Node " + dstdpId + " is " + lldpTime);
        	} 
		}
		
    }
	



}
