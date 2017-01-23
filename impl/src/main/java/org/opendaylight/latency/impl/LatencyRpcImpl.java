/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.impl;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.collection.LatencyEntry;
import org.opendaylight.latency.collection.NetworkLatency;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.latency.collection.SwitchesLatency;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.SwitchSwitchLatencyInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatencyRpcImpl implements LatencyService{
	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
	private PacketProcessingService packetProcessingService;
	private ListenerRegistration<NotificationListener> nlReg;
	private PacketInListener pktInl;
	private NotificationProviderService nps;
	private DataBroker dataBroker;

	
	public LatencyRpcImpl(PacketProcessingService packetProcessingService, DataBroker dataBroker,
			NotificationProviderService nps) {
		this.packetProcessingService = packetProcessingService;
		this.dataBroker = dataBroker;
		this.nps = nps;
	}
	

	@Override
	public Future<RpcResult<java.lang.Void>> networkLatency(
			final NetworkLatencyInput input) {
		
		//Register network latency packet_in listener
		NetworkLatency nl = new NetworkLatency(packetProcessingService, dataBroker);
		LatencyEntry nle = nl;
		PacketInListener npktInl = new PacketInListener(nle);
		nlReg = nps.registerNotificationListener(npktInl);
		npktInl.nlReg = nlReg;
		
		LatencyType type = input.getType();
		Future<RpcResult<java.lang.Void>> future = null;
		if (type.equals(LatencyType.SWITCHES)) {
			
			try {
				future = nl.execute();
				
			} catch (Exception e) {
				e.printStackTrace();
				nlReg.close();
			}
		
		}
		return future;
	}

	

	@Override
	public Future<RpcResult<Void>> switchSwitchLatency(
			SwitchSwitchLatencyInput input) {
		
		//Register switch to switch packet_in listener
		SwitchesLatency swl = new SwitchesLatency(packetProcessingService, dataBroker);
		LatencyEntry sle = swl;
		PacketInListener spktInl = new PacketInListener(sle);
		nlReg = nps.registerNotificationListener(spktInl);
		spktInl.nlReg = nlReg;
		swl.nlReg = nlReg;
		
		BigInteger adpId = input.getADpId();
		BigInteger bdpId = input.getBDpId();
		NodeId aNodeId = LatencyUtil.getNodeIdfromDpId(adpId);
		NodeId bNodeId = LatencyUtil.getNodeIdfromDpId(bdpId);
		Future<RpcResult<Void>> future = null;
			try {
				future = swl.execute(aNodeId, bNodeId);
				
			} catch (Exception e) {
				e.printStackTrace();
				nlReg.close();
			}

		return future;
	}

}
