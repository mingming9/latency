/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyPacketParserUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInListener implements PacketProcessingListener{

	private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);
	private List<NodeConnectorRef> srcnclist = new ArrayList<>();
	private List<NodeConnectorRef> dstnclist = new ArrayList<>();
	private List<Long> pktInlist = new ArrayList<>();
	public ListenerRegistration<NotificationListener> nlReg;
	private LatencyEntry le;
	
	public PacketInListener(LatencyEntry le) {
		this.le = le;
	}

	@Override
	public void onPacketReceived(PacketReceived latencyPkt) {
		
		//Check out whether this paket_in is the latency detecting packet
		if (LatencyPacketParserUtil.checkLatencyPacket(latencyPkt.getPayload())) {
			
			//Record the time. Store the src ndoe connector, dst node connector, packet_in recieved time to three list one by one.
			//Please notice that these three parameter of one packet_in are stored in three lists with the same key value.
			Date date = new Date();
    		Long srcTime = date.getTime();
    		NodeConnectorRef src = LatencyPacketParserUtil.lldpToNodeConnectorRef(latencyPkt.getPayload(),false);
        	NodeConnectorRef dst = latencyPkt.getIngress();
       		srcnclist.add(src);
       		dstnclist.add(dst);
       		pktInlist.add(srcTime);
		} 
		
		//The first packet_in packet arrived after latency detecting packets will invoke calculating the latency results.
		else if ( le.getflag() && !srcnclist.isEmpty() && !dstnclist.isEmpty() && !pktInlist.isEmpty()) {
			getresult (srcnclist, dstnclist, pktInlist, le.getpktOutTimeMap(), le.getpingTimeMap());
		}
    }
	

	private void getresult(List<NodeConnectorRef> srcnclist,
			List<NodeConnectorRef> dstnclist, List<Long> pktInlist,
			Map<NodeConnectorRef, Long> pktOutTimeMap, Map<NodeConnectorRef, Long> pingTimeMap) {
		
		//Get corresponding time value from the three lists and two maps storing time information of packet_out message and ping
		//Calculate the latency result
		for (int i = 0; i < srcnclist.size(); i++) {
			NodeConnectorRef src = srcnclist.get(i);
			
			//Get NodeId from node connector reference
			NodeId srcNodeId = InventoryUtil.getNodeIdFromNodeConnectorRef(src);
			
			//Get dpId from NodeId
    		BigInteger srcdpId = LatencyUtil.getDpId(srcNodeId);
    		NodeConnectorRef dst = dstnclist.get(i);
    		NodeId dstNodeId = InventoryUtil.getNodeIdFromNodeConnectorRef(dst);
    		BigInteger dstdpId = LatencyUtil.getDpId(dstNodeId);
			if (pktOutTimeMap.containsKey(src) && pingTimeMap.containsKey(src) && pingTimeMap.containsKey(dst)) {
				Long latency = pktInlist.get(i) - pktOutTimeMap.get(src) - pingTimeMap.get(src) - pingTimeMap.get(dst);
				System.out.println("latency from " + srcdpId + " to " + dstdpId + " is: " + latency);
			} else {
				System.out.println("We missed one packet_in from " + srcdpId + " to " + dstdpId);
			}
		}
		
		//Clear these three lists after finishing main function
		srcnclist.clear();
		dstnclist.clear();
		pktInlist.clear();
		nlReg.close();
	}
	


}
