/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.collection.NetworkLatency;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.latency.output.Pairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.latency.output.PairsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class LatencyRpcImpl implements LatencyService{
	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
	private PacketProcessingService packetProcessingService;
	private ListenerRegistration<NotificationListener> nlReg;
	private PacketInListener pktInl;
	private NotificationProviderService nps;
	private NetworkLatency nl;
	private DataBroker dataBroker;
	
	public LatencyRpcImpl(PacketProcessingService packetProcessingService, DataBroker dataBroker, NotificationProviderService nps) {
		this.packetProcessingService = packetProcessingService;
		this.dataBroker = dataBroker;
		this.nps = nps;
		
		// TODO Auto-generated constructor stub
	}

	@Override
	public Future<RpcResult<LatencyOutput>> latency(LatencyInput input) {
		LOG.info("I am in rpc");
		String type = input.getType().toString();
		String ctr = "ctr-sw-latency";		
		String sw = "sw-sw-latency";
		if (type.equals(ctr)) {
			this.nl = new NetworkLatency(packetProcessingService,dataBroker);			
			LOG.info("I am in ctr-sw now");		
			nl.execute();
			
			System.out.println("NetworkLatency is finished");
			
			PacketInListener packetInListener = new PacketInListener(nl.pktOutTimeMap);
			this.nlReg = nps.registerNotificationListener(packetInListener);
			
			
			//this.pktInl = new PacketInListener(nl.pktOutTimeMap);			
		//	this.nlReg = nps.registerNotificationListener(pktInl);			
			
			LatencyOutput latencyOutput = buildCtrSwLatencyOutput(type);
			LOG.info("finishing build output body");
			return Futures.<RpcResult<LatencyOutput>>immediateFuture(
					RpcResultBuilder.<LatencyOutput>success().withResult(latencyOutput).build());
		} else if (type.equals(sw)) {
			
			return Futures.<RpcResult<LatencyOutput>>immediateFuture(null);
		} else
			System.out.println("type input error");
		return null;
	}
	
	private LatencyOutput buildCtrSwLatencyOutput(String type) {
		LatencyOutputBuilder lab = new LatencyOutputBuilder();
		List<Pairs> pairs = new ArrayList<Pairs>();
		List<String> string = new ArrayList<String>();
		for(int i = 0; i < 10; i++) {
			string.add(String.valueOf(i));
		}
		for(String p: string) {
			
			PairsBuilder pb = new PairsBuilder();
			Pairs pair = pb.setDst(p).build();
			pairs.add(pair);
			
		}
		
		return lab.setPairs(pairs).build();
	}

	@Override
	public Future<RpcResult<LatencyRequestOutput>> latencyRequest(
			LatencyRequestInput input) {
		// TODO Auto-generated method stub
		return null;
	}

}
