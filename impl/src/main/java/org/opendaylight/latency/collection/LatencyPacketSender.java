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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class LatencyPacketSender {
	private static final Logger LOG = LoggerFactory.getLogger(LatencyPacketSender.class);
	public static Map<NodeConnectorRef, Long> pktOutTimeMap = new ConcurrentHashMap<>();
	private PacketProcessingService packetProcessingService;
	private DataBroker dataBroker;
	public static String TOPO_ID = "flow:1";
	private ListenerRegistration<NotificationListener> notificationListnerReg;
	public ListenableFuture<RpcResult<java.lang.Void>> listenablefuture;
	public Future<RpcResult<Void>> futureSend;
	private ListenerRegistration<NotificationListener> nlReg;
	private NotificationProviderService nps;
	
	
	
	public LatencyPacketSender (PacketProcessingService pps,
			DataBroker dataBroker) {
		this.packetProcessingService = pps;
		this.dataBroker = dataBroker;
		pktOutTimeMap.clear();
		
	}

	/*
	 * Network latency detecting
	 */
	public Future<RpcResult<java.lang.Void>> execute() {
		LOG.info("NetworkLatency is running");
		InstanceIdentifier<Topology> topoIId = TopologyUtil.createTopoIId(TOPO_ID);
		Topology topo = (Topology) TopologyUtil.readTopo(topoIId,dataBroker);
		List<Link> linkList = topo.getLink();
		for(Link link : linkList) {
			//Read src node and src node connector information from a link
			NodeId srcNodeId = new NodeId(link.getSource().getSourceNode().getValue());
			NodeRef srcNodeRef = InventoryUtil.getNodeRefFromNodeId(srcNodeId);
			
			//Read src node ip address from inventory data store
			FlowCapableNode srcflowCapableNode = (FlowCapableNode) InventoryUtil.readFlowCapableNodeFromNodeId(srcNodeId, dataBroker);
			IpAddress srcipAddress = srcflowCapableNode.getIpAddress();
			
			//Read src node port number from inventory data store
			NodeConnectorRef srcNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getSource().getSourceTp());
			NodeConnectorId srcnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(srcNCRef);
			InstanceIdentifier<NodeConnector> srcncIId = (InstanceIdentifier<NodeConnector>) srcNCRef.getValue();
			FlowCapableNodeConnector srcflowCapableNodeConnector = (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(srcncIId, dataBroker);
			MacAddress srcMac = srcflowCapableNodeConnector.getHardwareAddress();
			Long srcPortNo = srcflowCapableNodeConnector.getPortNumber().getUint32();
			
			//Read dst node mac address from inventory data store
			NodeId dstNodeId = new NodeId(link.getDestination().getDestNode().getValue());
			NodeConnectorRef dstNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getDestination().getDestTp());
			InstanceIdentifier<NodeConnector> dstncIId = (InstanceIdentifier<NodeConnector>) dstNCRef.getValue();
			FlowCapableNodeConnector dstflowCapableNodeConnector= (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(dstncIId, dataBroker);
			MacAddress dstMac = dstflowCapableNodeConnector.getHardwareAddress();;
			
			//Construct LLDP packet and send it out by packet_out meassage, store the sent time
			byte[] srcpayload = LatencyPacketUtil.buildLldpFrame(srcNodeId, srcnodeConnectorId, srcMac, srcPortNo, dstMac);
			TransmitPacketInput srclldppkt = LatencyUtil.createPacketOut(srcpayload, srcNodeRef, srcNCRef);	
			Date srcdate = new Date();
 		    Long srcpktOutTime = srcdate.getTime(); 
 		    pktOutTimeMap.put(srcNCRef, srcpktOutTime);
			futureSend = packetProcessingService.transmitPacket(srclldppkt);
	        
            LOG.info("Size in pktout is " + pktOutTimeMap.size());	
			
		}

		return futureSend;
		
	}
	
	/* 
	 * SWitch to switch latency detecting
	 */
public Future<RpcResult<Void>> execute(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId aNodeId,
		org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId bNodeId) throws Exception {
		
		//Read topology information from topology data store
		InstanceIdentifier<Topology> topoIId = TopologyUtil.createTopoIId(TOPO_ID);
		Topology topo = (Topology) TopologyUtil.readTopo(topoIId,dataBroker);
		List<Link> linkList = topo.getLink();
		int count = 0;
		for (Link link : linkList) {
			if (link.getSource().getSourceNode().equals(aNodeId) && link.getDestination().getDestNode().equals(bNodeId)) {
				
				//Read src node and src node connector information from a link,
				NodeId srcNodeId = new NodeId(link.getSource().getSourceNode().getValue());
				NodeRef srcNodeRef = InventoryUtil.getNodeRefFromNodeId(srcNodeId);
							
				//Read src node port number from inventory datat store
				NodeConnectorRef srcNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getSource().getSourceTp());
				NodeConnectorId srcnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(srcNCRef);
				InstanceIdentifier<NodeConnector> srcncIId = (InstanceIdentifier<NodeConnector>) srcNCRef.getValue();
				FlowCapableNodeConnector srcflowCapableNodeConnector = (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(srcncIId, dataBroker);
				MacAddress srcMac = srcflowCapableNodeConnector.getHardwareAddress();
				Long srcPortNo = srcflowCapableNodeConnector.getPortNumber().getUint32();
				
				//Read dst node mac from inventory data store
				NodeConnectorRef dstNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getDestination().getDestTp());
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


}
