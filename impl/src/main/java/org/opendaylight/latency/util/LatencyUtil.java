/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.util;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor;
import org.opendaylight.openflowplugin.api.openflow.md.core.session.SessionContext;
import org.opendaylight.openflowplugin.api.openflow.md.core.session.SwitchSessionKeyOF;
import org.opendaylight.openflowplugin.openflow.md.core.ConnectionConductorImpl;
import org.opendaylight.openflowplugin.openflow.md.core.session.OFSessionUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public abstract class LatencyUtil {
	
	private static final String OF_URI_PREFIX = "openflow:";
	private static final byte[] LATENCY_TEST = "LATENCY_TEST".getBytes();
	private static final Short OF13 = 0x04;

	public LatencyUtil() {
		// TODO Auto-generated constructor stub
	}

	public static SessionContext getSessionContext (NodeId nodeId) {
		SwitchSessionKeyOF switchSessionKeyOF = OFSessionUtil.createSwitchSessionKey(getDpId(nodeId));
		SessionContext sessionContext = OFSessionUtil.getSessionManager().getSessionContext(switchSessionKeyOF);
		return sessionContext;
	}
	
	public static BigInteger getDpId (NodeId nodeId) {
		String dpids = nodeId.getValue().replace(OF_URI_PREFIX, "");		
		return new BigInteger(dpids);		
	}
	
	public static EchoInput constructEchoInput(NodeId nodeId) {
		
		return new EchoInputBuilder().setData(LATENCY_TEST).setVersion(OF13).setXid(getSessionContext(nodeId).getNextXid()).build();
	}
	
	public static TransmitPacketInput createPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        InstanceIdentifier<Node> egressNodePath = getNodeIId(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(payload)
                .setNode(new NodeRef(egressNodePath)).setEgress(egress).setIngress(ingress).build();
        return input;
    }
	
	public static TransmitPacketInput createPacketOut(byte[] payload, NodeRef nodeRef, NodeConnectorRef nodeConnectorRef) {
        TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(payload)
                .setNode(nodeRef).setEgress(nodeConnectorRef).build();
        return input;
    }
	
	protected static final InstanceIdentifier<Node> getNodeIId (InstanceIdentifier<?> nodeChild) {

	        return nodeChild.firstIdentifierOf(Node.class);
	}
}
