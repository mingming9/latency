/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.latency.impl.LatencyProvider;
import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.latency.util.LldpUtil;
import org.opendaylight.latency.util.ReflectionUtil;
import org.opendaylight.latency.util.TopologyUtil;
import org.opendaylight.openflowplugin.applications.lldpspeaker.LLDPSpeaker;
import org.opendaylight.openflowplugin.applications.lldpspeaker.LLDPUtil;
import org.opendaylight.openflowplugin.applications.lldpspeaker.NodeConnectorEventsObserver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.applications.lldp.speaker.rev141023.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkLatency implements LatencyRepo{
	private static final Logger LOG = LoggerFactory.getLogger(NetworkLatency.class);
	//private Map<InstanceIdentifier<NodeConnector>, TransmitPacketInput> nodeConnectormap = new ConcurrentHashMap<>();;
	public Map<NodeConnectorId, String> pktOutTimeMap = new ConcurrentHashMap<>();
	private PacketProcessingService packetProcessingService;
	//private MacAddress addressDestionation;
	private DataBroker dataBroker;
	public static String TOPO_ID = "flow:1";

	/*public NetworkLatency(PacketProcessingService packetProcessingService,
			ScheduledExecutorService scheduledExecutorService,
			MacAddress addressDestionation) {
		super(packetProcessingService, scheduledExecutorService, addressDestionation);
		this.packetProcessingService = packetProcessingService;
		this.nodeConnectormap = getNodeConnectorMap(packetProcessingService,addressDestionation);
		this.pktOutTimeMap = null;
		// TODO Auto-generated constructor stub
	}*/
	
	
	
	public NetworkLatency(PacketProcessingService pps,
			DataBroker dataBroker) {
		this.packetProcessingService = pps;
		this.dataBroker = dataBroker;

	}

	@Override
	public void execute() {
		System.out.println("NetworkLatency is running");
		InstanceIdentifier<Topology> topoIId = TopologyUtil.createTopoIId(TOPO_ID);
		Topology topo = (Topology) TopologyUtil.readTopo(topoIId,dataBroker);
		System.out.println("NetworkLatency topo is" + topo);
		List<Link> linkList = topo.getLink();
		System.out.println("NetworkLatency links are" + linkList);
		for(Link link : linkList) {
			System.out.println("NetworkLatency link is" + link);
			//src
			NodeId srcNodeId = new NodeId(link.getSource().getSourceNode().getValue());
			NodeRef srcNodeRef = InventoryUtil.getNodeRef(srcNodeId);
			NodeConnectorRef srcNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getSource().getSourceTp());
			NodeConnectorId srcnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(srcNCRef);
			InstanceIdentifier<NodeConnector> srcncIId = (InstanceIdentifier<NodeConnector>) srcNCRef.getValue();
			FlowCapableNodeConnector srcflowCapableNodeConnector= (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(srcncIId, dataBroker);
			System.out.println("srcFlowCapableNodeConnector is {}" + srcflowCapableNodeConnector);
			MacAddress srcMac = srcflowCapableNodeConnector.getHardwareAddress();
			System.out.println("srcMac is {}" + srcMac);
			Long srcPortNo = srcflowCapableNodeConnector.getPortNumber().getUint32();
			System.out.println("srcPortNo is {}" + srcPortNo);
			
			//dst
			NodeId dstNodeId = new NodeId(link.getDestination().getDestNode().getValue());
			NodeRef dstNodeRef = InventoryUtil.getNodeRef(dstNodeId);
			NodeConnectorRef dstNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getDestination().getDestTp());
			NodeConnectorId dstnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(dstNCRef);
			InstanceIdentifier<NodeConnector> dstncIId = (InstanceIdentifier<NodeConnector>) dstNCRef.getValue();
			FlowCapableNodeConnector dstflowCapableNodeConnector= (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(dstncIId, dataBroker);
			System.out.println("dstflowCapableNodeConnector is {}" + dstflowCapableNodeConnector);
			MacAddress dstMac = dstflowCapableNodeConnector.getHardwareAddress();
			System.out.println("dstMac is {}" + dstMac);
			Long dstPortNo = dstflowCapableNodeConnector.getPortNumber().getUint32();
			System.out.println("dstPortNo is {}" + dstPortNo);
			
			//srclldp
			byte[] srcpayload = LldpUtil.buildLldpFrame(srcNodeId, srcnodeConnectorId, srcMac, srcPortNo, dstMac);
			TransmitPacketInput srclldppkt = LatencyUtil.createPacketOut(srcpayload, srcNodeRef, srcNCRef);
			System.out.println("srclldppkt is {}" + srclldppkt);
			packetProcessingService.transmitPacket(srclldppkt);
			System.out.println("srcPktout succeed");
			SimpleDateFormat srcdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String srcpktOutTime = srcdf.format(new Date());
            pktOutTimeMap.put(srcnodeConnectorId, srcpktOutTime);             
            System.out.println("NodeConnector:"+ srcnodeConnectorId + ", packet_out is sent at" + srcpktOutTime);
			
             //dstlldp
 			byte[] dstpayload = LldpUtil.buildLldpFrame(dstNodeId, dstnodeConnectorId, dstMac, dstPortNo, srcMac);
 			TransmitPacketInput dstlldppkt = LatencyUtil.createPacketOut(dstpayload, srcNodeRef, srcNCRef);
 			System.out.println("dstlldppkt is {}" + dstlldppkt);
 		    packetProcessingService.transmitPacket(dstlldppkt);
 		    System.out.println("srcPktout succeed");
 			SimpleDateFormat dstdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dstpktOutTime = dstdf.format(new Date());
            pktOutTimeMap.put(dstnodeConnectorId, dstpktOutTime);             
            System.out.println("NodeConnector:"+ dstnodeConnectorId + ", packet_out is sent at" + dstpktOutTime);
			
		}
		
		// TODO Auto-generated method stub
		
	}

/*	public Map<InstanceIdentifier<NodeConnector>, TransmitPacketInput> getNodeConnectorMap(PacketProcessingService packetProcessingService, MacAddress mac) {
		System.out.println("getNodeConnectorMap is invoked");		
		//Field fieldPassword;
		Map<InstanceIdentifier<NodeConnector>, TransmitPacketInput> map = new ConcurrentHashMap<>();
		try {
			Field fieldPassword = LLDPSpeaker.class.getDeclaredField("nodeConnectorMap");
			fieldPassword.setAccessible(true);
			LLDPSpeaker lldpSpeaker = new LLDPSpeaker(packetProcessingService, Executors.newSingleThreadScheduledExecutor(), mac);
			//System.out.println("raw getNodeConnectorMap is:" + fieldPassword.get(lldpSpeaker));
			map = (Map<InstanceIdentifier<NodeConnector>, TransmitPacketInput>) fieldPassword.get(lldpSpeaker);
			System.out.println("getNodeConnectorMap is:" + map);
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			System.out.println("getNodeConMap failed by NoSuchField");
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			System.out.println("getNodeConMap failed by SecurityException");
			e1.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("getNodeConMap failed by IllegalAccess");
			e.printStackTrace();
		}
		return map;

	}
	@Override
	public void execute() {
		System.out.println("NetWorkLatency.run is invoked");
		
		//nodeConnectormap = getNodeConnectorMap(packetProcessingService, addressDestionation);
		System.out.println("nodeConnectmap is:" + nodeConnectormap);
		Set<InstanceIdentifier<NodeConnector>> nodeConnectorIIds = nodeConnectormap.keySet();
		System.out.println("nodeConnectIIDs are:" + nodeConnectorIIds);
            LOG.debug("Sending LLDP frames to {} ports...", nodeConnectorIIds.size());

            for (InstanceIdentifier<NodeConnector> nodeConnectorInstanceId : nodeConnectorIIds) {
                NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(nodeConnectorInstanceId).getId();
                LOG.trace("Sending LLDP through port {}", nodeConnectorId.getValue());
                packetProcessingService.transmitPacket(nodeConnectormap.get(nodeConnectorInstanceId));
                // Record the time
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String pktOutTime = df.format(new Date());
                pktOutTimeMap.put(nodeConnectorId, pktOutTime);
                
                System.out.println("NodeConnector:"+ nodeConnectorId + ", packet_out is sent at" + pktOutTime);
            }

    }*/
	

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







}
