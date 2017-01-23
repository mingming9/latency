/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.latency.ping.Ping;
import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyPacketUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.latency.util.TopologyUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class SwitchesLatency implements LatencyEntry{
	private static final Logger LOG = LoggerFactory.getLogger(NetworkLatency.class);
	public static Map<NodeConnectorRef, Long> pktOutTimeMap = new ConcurrentHashMap<>();
	public static Map<NodeConnectorRef, Long> pingTimeMap = new ConcurrentHashMap<>();	
	private PacketProcessingService packetProcessingService;
	private DataBroker dataBroker;
	public static String TOPO_ID = "flow:1";
	public ListenableFuture<RpcResult<java.lang.Void>> listenablefuture;
	public Future<RpcResult<Void>> futureSend;
	public static boolean flag;
	public ListenerRegistration<NotificationListener> nlReg;
			
	public SwitchesLatency(PacketProcessingService pps,
				DataBroker dataBroker) {
			this.packetProcessingService = pps;
			this.dataBroker = dataBroker;
			this.flag = false;
			this.pktOutTimeMap.clear();
			this.pingTimeMap.clear();
		}
		
	public Future<RpcResult<Void>> execute(NodeId aNodeId, NodeId bNodeId) throws Exception {
		
		//Read topology information from topology data store
		InstanceIdentifier<Topology> topoIId = TopologyUtil.createTopoIId(TOPO_ID);
		Topology topo = (Topology) TopologyUtil.readTopo(topoIId,dataBroker);
		List<Link> linkList = topo.getLink();
		int count = 0;
		for (Link link : linkList) {
			if (link.getSource().getSourceNode().equals(aNodeId) && link.getDestination().getDestNode().equals(bNodeId)) {
				//Read src node and src node connector information from a link,
				org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId srcNodeId = 
						new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
						NodeId(link.getSource().getSourceNode().getValue());
				NodeRef srcNodeRef = InventoryUtil.getNodeRefFromNodeId(srcNodeId);
				
				//Read src node ip address from inventory data store
				FlowCapableNode srcflowCapableNode = (FlowCapableNode) InventoryUtil.readFlowCapableNodeFromNodeId(srcNodeId, dataBroker);
				IpAddress srcipAddress = srcflowCapableNode.getIpAddress();			
				NodeConnectorRef srcNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getSource().getSourceTp());
				
				//Do src ping and store the time to a map whose key is node connector reference
				Long pingsrc = Ping.pingEnter(srcipAddress.getIpv4Address().getValue().toString());
				pingTimeMap.put(srcNCRef, pingsrc);
				LOG.info("pingsrc is" + pingsrc + ", size is " + pingTimeMap.size());
				
				//Do src ping and store the time to a map whose key is node connector reference
				org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId dstNodeId = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(link.getDestination().getDestNode().getValue());
				NodeRef dstNodeRef = InventoryUtil.getNodeRefFromNodeId(dstNodeId);
				FlowCapableNode dstflowCapableNode = (FlowCapableNode) InventoryUtil.readFlowCapableNodeFromNodeId(dstNodeId, dataBroker);
				IpAddress dstipAddress = srcflowCapableNode.getIpAddress();
				NodeConnectorRef dstNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getDestination().getDestTp());
				Long pingdst = Ping.pingEnter(dstipAddress.getIpv4Address().getValue().toString());
				pingTimeMap.put(dstNCRef, pingdst);
				LOG.info("pingdst is" + pingdst + ", size is " + pingTimeMap.size());
				
				//Read src node port number from inventory datat store
				NodeConnectorId srcnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(srcNCRef);
				InstanceIdentifier<NodeConnector> srcncIId = (InstanceIdentifier<NodeConnector>) srcNCRef.getValue();
				FlowCapableNodeConnector srcflowCapableNodeConnector = (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(srcncIId, dataBroker);
				MacAddress srcMac = srcflowCapableNodeConnector.getHardwareAddress();
				Long srcPortNo = srcflowCapableNodeConnector.getPortNumber().getUint32();
				
				//Read dst node mac from inventory data store
				InstanceIdentifier<NodeConnector> dstncIId = (InstanceIdentifier<NodeConnector>) dstNCRef.getValue();
				FlowCapableNodeConnector dstflowCapableNodeConnector= (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(dstncIId, dataBroker);
				MacAddress dstMac = dstflowCapableNodeConnector.getHardwareAddress();
				
				//Constract LLDP packet and sent it out by packet out message, store the sent time
				byte[] srcpayload = LatencyPacketUtil.buildLldpFrame(srcNodeId, srcnodeConnectorId, srcMac, srcPortNo, dstMac);
				TransmitPacketInput srclldppkt = LatencyUtil.createPacketOut(srcpayload, srcNodeRef, srcNCRef);
				Date srcdate = new Date();
	 		    Long srcpktOutTime = srcdate.getTime();
				futureSend = packetProcessingService.transmitPacket(srclldppkt);			
	            pktOutTimeMap.put(srcNCRef, srcpktOutTime);
	            LOG.info("pktout time is " + srcpktOutTime +"size in pktout is " + pktOutTimeMap.size());
	            count++;
			} 
			
		}
		if (count == 0) {
			System.out.println(aNodeId + " is not linkded with " + bNodeId);
			nlReg.close();
		}
		return futureSend;
	}






	@Override
	public boolean getflag() {
		return true;
	}




	@Override
	public Map<NodeConnectorRef, Long> getpktOutTimeMap() {
		return this.pktOutTimeMap;
	}




	@Override
	public Map<NodeConnectorRef, Long> getpingTimeMap() {
		return this.pingTimeMap;
	}


}
