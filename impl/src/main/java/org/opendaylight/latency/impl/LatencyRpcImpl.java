/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.collection.ConstructRpcOutput;
import org.opendaylight.latency.collection.NetworkLatency;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.ControllerSwitchLatencyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.ControllerSwitchLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.SwitchSwitchLatencyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.SwitchSwitchLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.Pairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.PairsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class LatencyRpcImpl implements LatencyService{
	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
	private PacketProcessingService packetProcessingService;
	private ListenerRegistration<NotificationListener> nlReg;
	private PacketInListener pktInl;
	private NotificationProviderService nps;
	private NetworkLatency nl;
	private DataBroker dataBroker;
	private NetworkLatencyOutput latencyOutput;
	private final ListeningExecutorService executor =
             MoreExecutors.listeningDecorator( Executors.newCachedThreadPool() );

	
	public LatencyRpcImpl(PacketProcessingService packetProcessingService, DataBroker dataBroker, NotificationProviderService nps, NetworkLatency nl) {
		this.packetProcessingService = packetProcessingService;
		this.dataBroker = dataBroker;
		this.nps = nps;
		this.nl = nl;
		// TODO Auto-generated constructor stub
	}
	
	private NetworkLatencyOutput buildSwSwLatencyOutput() {
		NetworkLatencyOutputBuilder lab = new NetworkLatencyOutputBuilder();
		List<Pairs> pairs = new ArrayList<Pairs>();
		List<String> string = new ArrayList<String>();
		for(int i = 0; i < 10; i++) {
			string.add(String.valueOf(i));
		}
		for(String p: string) {
			
			PairsBuilder pb = new PairsBuilder();
			Pairs pair = pb.setBDpId(new BigInteger(p)).build();
			pairs.add(pair);
			
		}
		
		latencyOutput = lab.setPairs(pairs).build();
		return latencyOutput;
	}

	@Override
	public Future<RpcResult<SwitchSwitchLatencyOutput>> switchSwitchLatency(
			SwitchSwitchLatencyInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<NetworkLatencyOutput>> networkLatency(
			final NetworkLatencyInput input) {
		LOG.info("NetworkLatecy detection is invorked");
		LatencyType type = input.getType();
		if (type.equals(LatencyType.SWITCHES)) {
			
			try {
				nl = new NetworkLatency(packetProcessingService, dataBroker);
				Future<RpcResult<java.lang.Void>> future = nl.execute();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			latencyOutput = buildSwSwLatencyOutput();
			LOG.info("finishing build output body");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return Futures.<RpcResult<NetworkLatencyOutput>>immediateFuture(
					RpcResultBuilder.<NetworkLatencyOutput>success().withResult(latencyOutput).build());
			
		
	}
		return Futures.<RpcResult<NetworkLatencyOutput>>immediateFuture(
				RpcResultBuilder.<NetworkLatencyOutput>success().withResult(latencyOutput).build());
	}

	
	
	@Override
	public Future<RpcResult<ControllerSwitchLatencyOutput>> controllerSwitchLatency(
			ControllerSwitchLatencyInput input) {
		// TODO Auto-generated method stub
		return null;
	}

}
