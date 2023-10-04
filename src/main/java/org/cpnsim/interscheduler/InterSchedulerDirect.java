package org.cpnsim.interscheduler;

import org.cloudsimplus.core.Simulation;
import org.cloudsimplus.network.topologies.NetworkTopology;
import org.cpnsim.datacenter.Datacenter;
import org.cpnsim.request.Instance;
import org.cpnsim.request.InstanceGroup;

import java.util.*;

public class InterSchedulerDirect extends InterSchedulerSimple {
    Random random = new Random(1);

    public InterSchedulerDirect(int id, Simulation simulation, int collaborationId) {
        super(id, simulation, collaborationId);
    }

    @Override
    public Map<InstanceGroup, List<Datacenter>> filterSuitableDatacenter(List<InstanceGroup> instanceGroups) {
        List<Datacenter> allDatacenters = simulation.getCollaborationManager().getDatacenters(collaborationId);
        NetworkTopology networkTopology = simulation.getNetworkTopology();
        Map<InstanceGroup, List<Datacenter>> instanceGroupAvaiableDatacenters = new HashMap<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            List<Datacenter> availableDatacenters = getAvailableDatacenters(instanceGroup, allDatacenters, networkTopology);
            instanceGroupAvaiableDatacenters.put(instanceGroup, availableDatacenters);
        }
        interScheduleByNetworkTopology(instanceGroupAvaiableDatacenters, networkTopology);
        this.filterSuitableDatacenterCostTime = 0.2;//TODO 为了模拟没有随机性，先设置为每一个亲和组调度花费0.1ms
        for (InstanceGroup instanceGroup : instanceGroups) {
            List<Datacenter> datacenters = instanceGroupAvaiableDatacenters.get(instanceGroup);
            Datacenter targetDatacenter = datacenters.get(random.nextInt(datacenters.size()));
            datacenters.clear();
            datacenters.add(targetDatacenter);
        }
        return instanceGroupAvaiableDatacenters;
    }
}
