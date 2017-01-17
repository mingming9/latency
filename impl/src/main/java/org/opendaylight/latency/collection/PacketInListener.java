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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.latency.impl.LatencyCallback;
import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyPacketParserUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.openflowplugin.applications.topology.lldp.utils.LLDPDiscoveryUtils;
//import org.opendaylight.openflowplugin.applications.topology.lldp.utils.LLDPDiscoveryUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.Pairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.PairsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class PacketInListener implements PacketProcessingListener, AutoCloseable{

	private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);
	private Map<String, Long> outputmap = new ConcurrentHashMap<>();
	private Map<Long, Integer> index;
	private Map<Integer, Long> timeStore;
	private List<NodeConnectorRef> srcnclist = new ArrayList<>();
	private List<NodeConnectorRef> dstnclist = new ArrayList<>();
	private List<Long> lldptimelist = new ArrayList<>();
	private NetworkLatencyOutput output;
	private int size = 0;
	private NetworkLatency nl;
	public ListenerRegistration<NotificationListener> lReg = null;
	private boolean closeFlag = false;
	private final ListeningExecutorService executor =
            MoreExecutors.listeningDecorator( Executors.newCachedThreadPool() );

	
	public PacketInListener() {
		// TODO Auto-generated constructor stub
	}
/*	public PacketInListener(Map<NodeConnectorRef, Long> map){
		this.map = map;
	}
	*/
	public PacketInListener(NetworkLatency nl) {
		this.nl = nl;
	}

	@Override
	public void onPacketReceived(PacketReceived latencyPkt) {
		LOG.info("Latency onPacketReceived is invorked");
		
        	int mapSize = nl.pktOutTimeMap.size();
        	NodeConnectorRef src = LLDPDiscoveryUtils.lldpToNodeConnectorRef(latencyPkt.getPayload(),true);
        	NodeConnectorRef dst = latencyPkt.getIngress();
        	LOG.info("NodeConnectorRef is {}" , src);
        	if (src != null && nl.pktOutTimeMap.containsKey(src) && size < mapSize) {
        		LOG.info("enter pktOutTime look up");
			
        		Date srcdate = new Date();
        		Long srctime = srcdate.getTime();
        		Long lldptime = srctime - nl.pktOutTimeMap.get(src);
        		lldptimelist.add(lldptime);
        		srcnclist.add(src);
        		dstnclist.add(dst);
        		System.out.println("latencyPkt time is " + lldptime);
        		size ++;
        	}
        	if (size == nl.pktOutTimeMap.size() && !srcnclist.isEmpty() && !dstnclist.isEmpty() && !lldptimelist.isEmpty()) {
        		System.out.println("srclist is " + srcnclist);
        		System.out.println("dstlist is " + dstnclist);
        		System.out.println("lldptime is " + lldptimelist);
        		ConstructRpcOutput construct = new ConstructRpcOutput(srcnclist, dstnclist, lldptimelist);
        		output = construct.getSwSwLatencyOutput();
        		System.out.println("output is " + output);
        		nl.pktOutTimeMap.clear();
        		lldptimelist.clear();
        		srcnclist.clear();
        		dstnclist.clear();
        		System.out.println("I think pktOutTimeMap is cleared" + nl.pktOutTimeMap);
        		lReg.close();
        		size = 0;
        		System.out.println("size is " + size);
        	} else       {
			closeFlag = true;
		}
    }
	

	
	public void passRpcResult (LatencyCallback callback) {
		if(output != null) {
			callback.networklatencyreq(output);
		} 		
	}
	@Override
	public void close() throws Exception {
		if (closeFlag){
		lReg.close();
		}
		
	}

}
