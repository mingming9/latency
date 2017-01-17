/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.util;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.InventoryNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class InventoryUtil {

	public InventoryUtil() {
		// TODO Auto-generated constructor stub
	}
	public static Object readInventory(InstanceIdentifier<InventoryNode> nodeIId, DataBroker dataBroker) {       
        Optional<?> dataFuture = null;
		try {
			ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
			dataFuture = rx.read(LogicalDatastoreType.OPERATIONAL, nodeIId).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        return dataFuture.get();
    }
	
	public static InstanceIdentifier<NodeConnector> createNodeConnectorId(String nodeKey, String nodeConnectorKey) {
	        return InstanceIdentifier.builder(Nodes.class)
	                .child(Node.class, new NodeKey(new NodeId(nodeKey)))
	                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorKey)))
	                .build();
	}

	public static FlowCapableNodeConnectorBuilder createFlowCapableNodeConnector(MacAddress mac, long port) {
	        return createFlowCapableNodeConnector(false, false, mac, port);
	}

	public static FlowCapableNodeConnectorBuilder createFlowCapableNodeConnector(boolean linkDown, boolean adminDown,
	                                                                          MacAddress mac, long port) {
	        return new FlowCapableNodeConnectorBuilder()
	                .setHardwareAddress(mac)
	                .setPortNumber(new PortNumberUni(port))
	                .setState(new StateBuilder().setLinkDown(linkDown).build())
	                .setConfiguration(new PortConfig(false, false, false, adminDown));
	}
	
	public static NodeRef getNodeRefFromNodeConnectorRef (NodeConnectorRef nodeConnectorRef) {
        InstanceIdentifier<Node> nodeIID = nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
        return new NodeRef(nodeIID);
    }
	
	public static NodeRef getNodeRefFromNodeId (NodeId nodeId) {
		InstanceIdentifier<Node> nodeIId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId)).build();
		return new NodeRef(nodeIId);
	}
	
	public static NodeConnectorId getNodeConnectorIdFromNodeConnectorRef (NodeConnectorRef ref) {
		NodeConnectorId ncId = ref.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
		return ncId;
	}
	
	public static NodeId getNodeIdFromNodeConnectorRef (NodeConnectorRef ref) {
		NodeRef nodeRef = getNodeRefFromNodeConnectorRef(ref);
		NodeId nodeId = nodeRef.getValue().firstKeyOf(Node.class, NodeKey.class).getId();
		return nodeId;
	}
	public static Object readFlowCapableNodeConnectorFromNodeConnectorIId (InstanceIdentifier<NodeConnector> nodeConnectorIId, DataBroker dataBroker) {
		Optional<?> optional = null;
		try {
			InstanceIdentifier<FlowCapableNodeConnector> fcncIId = constructFlowCapableNodeConnectorIId(nodeConnectorIId);
			ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
			optional = rx.read(LogicalDatastoreType.OPERATIONAL, fcncIId).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        
       // System.out.println("FlowCapableNodeConnector is {}" + optional.get());
        return optional.get();
		
	}
	
	public static InstanceIdentifier<FlowCapableNodeConnector> constructFlowCapableNodeConnectorIId (InstanceIdentifier<NodeConnector> ncIId) {
		InstanceIdentifier<FlowCapableNodeConnector> fcncIId = ncIId.augmentation(FlowCapableNodeConnector.class);
		//System.out.println("FlowCapableNodeConnectorIId is {}" + fcncIId);
		return fcncIId;
	}

}
