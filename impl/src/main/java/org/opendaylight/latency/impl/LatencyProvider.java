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
import org.opendaylight.latency.collection.LatencyEntry;
import org.opendaylight.latency.collection.NetworkLatency;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.latency.collection.SwitchesLatency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LatencyProvider implements BindingAwareProvider, AutoCloseable{

	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);

	private RpcRegistration<LatencyService> rpcReg;

	
	public LatencyProvider() {
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		
		//Get services
		PacketProcessingService pps = session.getRpcService(PacketProcessingService.class);
		DataBroker db = session.getSALService(DataBroker.class);
		NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
		
		//Register rpc service		
		LatencyRpcImpl rpcImpl = new LatencyRpcImpl(pps, db, nps);
		this.rpcReg = session.addRpcImplementation(LatencyService.class, rpcImpl);
			
		LOG.info("LatencyProvider session initiated successfully");
	}
	

	@Override
	public void close() throws Exception {
		LOG.info("LatencyProvider is closing");
		try {
			this.rpcReg.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		LOG.info("LatencyProvider is closed");	
	}
	
}
