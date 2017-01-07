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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
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

public class LatencyProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LatencyProvider.class);
    private ListenerRegistration<NotificationListener> notificationListener;
    private Latencymain latencymain;
    

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("LatencyProvider Session Initiated");
        PacketProcessingService packetProcessingService = session.getRpcService(PacketProcessingService.class);
        SalFlowService fs = session.getRpcService(SalFlowService.class);
        DataBroker databroker = session.getSALService(DataBroker.class);
        this.latencymain = new Latencymain(packetProcessingService, databroker);
        NotificationProviderService notificationProviderService = session.getSALService(NotificationProviderService.class);
        this.notificationListener = notificationProviderService.registerNotificationListener(latencymain);

    }

    @Override
    public void close() throws Exception {
        LOG.info("LatencyProvider Closed");
    }


}
