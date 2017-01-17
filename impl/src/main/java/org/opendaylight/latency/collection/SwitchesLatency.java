/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.openflowplugin.applications.lldpspeaker.LLDPSpeaker;
import org.opendaylight.openflowplugin.applications.lldpspeaker.NodeConnectorEventsObserver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class SwitchesLatency extends LLDPSpeaker implements LatencyRepo {



	public SwitchesLatency(PacketProcessingService packetProcessingService,
			ScheduledExecutorService scheduledExecutorService,
			MacAddress addressDestionation) {
		super(packetProcessingService, scheduledExecutorService, addressDestionation);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Long getCSLatency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getSSLatency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<Void>> execute() {
		return null;
		// TODO Auto-generated method stub
		
	}

}
