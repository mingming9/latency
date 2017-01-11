/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.util.Date;
import java.util.Map;

import org.opendaylight.openflowplugin.applications.topology.lldp.utils.LLDPDiscoveryUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInListener implements PacketProcessingListener {

	private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);
	private Map<NodeConnectorRef, Long> map;
	public PacketInListener() {
		// TODO Auto-generated constructor stub
	}
	public PacketInListener(Map<NodeConnectorRef, Long> map){
		this.map = map;
	}

	@Override
	public void onPacketReceived(PacketReceived lldp) {
		LOG.info("Latency onPacketReceived is invorked");
		System.out.println("map size is " + map.size());
		NodeConnectorRef src = LLDPDiscoveryUtils.lldpToNodeConnectorRef(lldp.getPayload(), true);
		LOG.info("NodeConnectorRef is {}" , src);
		if(src != null && map.containsKey(src)) {
			LOG.info("enter pktOutTime look up");
			Date srcdate = new Date();
 		    Long srctime = srcdate.getTime();
			Long lldptime = srctime - map.get(src);
			System.out.println("lldp time is " + lldptime);
		}
		

	}

}
