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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class TopologyUtil {

	public TopologyUtil() {
		// TODO Auto-generated constructor stub
	}
	
	public static Object readTopo(InstanceIdentifier<Topology> topoIId, DataBroker dataBroker) {       
        Optional<?> optional = null;
		try {
			ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
			optional = rx.read(LogicalDatastoreType.OPERATIONAL, topoIId).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        return optional.get();
    }
	
	public static InstanceIdentifier<Topology> createTopoIId (String tOPO_ID){
		return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(new TopologyId(tOPO_ID))).build();
	}
	
	
	protected static TpId getTpIdFromNodeConnectorRef(NodeConnectorRef ref) {
        NodeConnectorId nci = ref.getValue()
                .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
                .getId();
        return new TpId(nci);
    }
	


    public static String getNodeString(NodeConnectorRef ref) {
        String nodeIdString = ref.getValue().firstIdentifierOf(Node.class)
                .firstKeyOf(Node.class, NodeKey.class).getId().getValue();
        return nodeIdString;
    }

    public static NodeConnectorRef getNodeConnectorRefFromTpId(TpId tpId){
        String nc_value = tpId.getValue();
        InstanceIdentifier<NodeConnector> ncid = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')))))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nc_value))).build();
        return new NodeConnectorRef(ncid);
    }
    
    public static NodeId convertTpId2NodeId(TpId tpId) {
        String nc_value = tpId.getValue();
        return new NodeId(nc_value.substring(0, nc_value.lastIndexOf(':')));
    }
}
