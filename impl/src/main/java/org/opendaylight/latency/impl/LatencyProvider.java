/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.collection.LatencyPacketSender;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LatencyProvider implements BindingAwareProvider, AutoCloseable{

	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
	
	private RpcRegistration<LatencyService> rpcReg;
	private ListenerRegistration<NotificationListener> nlReg;

	
	public LatencyProvider() {
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		
		//get services
		PacketProcessingService pps = session.getRpcService(PacketProcessingService.class);
		DataBroker db = session.getSALService(DataBroker.class);
		NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
		
		//register rpc service
		LatencyPacketSender lps = new LatencyPacketSender(pps,db);
		LatencyRpcImpl rpcImpl = new LatencyRpcImpl(lps);
		this.rpcReg = session.addRpcImplementation(LatencyService.class, rpcImpl);
		
		//register packet in listener
		PacketInListener pktInl = new PacketInListener(lps);
		nlReg = nps.registerNotificationListener(pktInl);
		
		LOG.info("LatencyProvider session initiated successfully");
	}
	

	@Override
	public void close() throws Exception {
		
		LOG.info("LatencyProvider is closing");
		try {
			this.rpcReg.close();
			this.nlReg.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		LOG.info("LatencyProvider is closed");
	}


}
