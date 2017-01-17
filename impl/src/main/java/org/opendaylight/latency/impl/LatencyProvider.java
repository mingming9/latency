/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.collection.LatencyRepo;
import org.opendaylight.latency.collection.NetworkLatency;
import org.opendaylight.latency.collection.PacketInListener;
import org.opendaylight.openflowplugin.applications.lldpspeaker.LLDPSpeaker;
import org.opendaylight.openflowplugin.openflow.md.core.ConnectionConductorImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SendEchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SendEchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.LatencyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.applications.lldp.speaker.rev141023.OperStatus;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;


public class LatencyProvider implements BindingAwareProvider, AutoCloseable{

	private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
	
	//private DataBroker dataBroker;
	//private ListenerRegistration<NotificationListener> notificationReg;
	private RpcRegistration<LatencyService> rpcReg;
	//private LLDPSpeaker lldpSpeaker;
	//SalEchoServiceImpl salEchoServiceImpl;
   // private OperStatus operationalStatus = OperStatus.RUN;

	private EchoMsg echoMsg;
	
	public LatencyProvider() {
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		//get services
		PacketProcessingService pps = session.getRpcService(PacketProcessingService.class);
		DataBroker db = session.getSALService(DataBroker.class);
		NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
		//register rpc service
		LatencyRpcImpl rpcImpl = new LatencyRpcImpl(pps, db, nps);
		this.rpcReg = session.addRpcImplementation(LatencyService.class, rpcImpl);
		
		LOG.info("LatencyProvider session initiated successfully");
	}
	

	@Override
	public void close() throws Exception {
		
		
	}


}
