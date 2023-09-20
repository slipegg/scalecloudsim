package org.cpnsim.datacenter;

import org.cloudsimplus.core.CloudSim;
import org.cloudsimplus.core.Factory;
import org.cloudsimplus.core.FactorySimple;
import org.cloudsimplus.core.Simulation;
import org.cpnsim.innerscheduler.InnerScheduler;
import org.cpnsim.statemanager.HostState;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class InitDatacenterTest {
    @Test
    void testInitDatacenter() {
        Simulation simulation = new CloudSim();
        Factory factory = new FactorySimple();
        String filePath = "src/test/resources/DatacentersConfig.json";
        InitDatacenter.initDatacenters(simulation, factory, filePath);

        assertEquals(1, simulation.getCollaborationManager().getDatacenters(1).size());
        Datacenter datacenter = simulation.getCollaborationManager().getDatacenters(1).get(0);
        assertEquals(2, datacenter.getStatesManager().getHostNum());
        HostState hostState = datacenter.getStatesManager().getNowHostState(0);
        HostState exceptedHostState = new HostState(10, 10, 10, 10);
        assertEquals(exceptedHostState, hostState);
        assertEquals(60, datacenter.getStatesManager().getSmallSynGap(), 0.1);
        assertEquals(2, datacenter.getInnerSchedulers().size());
        InnerScheduler innerScheduler0 = datacenter.getInnerSchedulers().get(0);
        assertEquals(0, innerScheduler0.getFirstPartitionId());
        InnerScheduler innerScheduler1 = datacenter.getInnerSchedulers().get(1);
        assertEquals(1, innerScheduler1.getFirstPartitionId());
        assertEquals(false, datacenter.getStatesManager().getPredictable());
    }
}