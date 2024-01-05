package org.cpnsim.intrascheduler;

import org.cpnsim.request.Instance;
import org.cpnsim.statemanager.HostState;
import org.cpnsim.statemanager.SynState;
import org.cpnsim.util.ScoredHost;
import org.cpnsim.util.ScoredHostsManager;

import java.util.*;

public class IntraSchedulerLeastRequested extends IntraSchedulerSimple {
    Map<Integer, Double> scoreHostHistoryMap = new HashMap<>();
    int scoredHostNumForSameInstance = 100;

    long excludeTimeNanos = 0;

    Random random = new Random();

    public IntraSchedulerLeastRequested(int id, int firstPartitionId, int partitionNum) {
        super(id, firstPartitionId, partitionNum);
    }


    //        for (Instance instance : instances) {
//            int suitId = -1;
//
//            int synPartitionId = firstPartitionId;
//            if (datacenter.getStatesManager().isSynCostTime()) {
//                synPartitionId = (firstPartitionId + datacenter.getStatesManager().getSmallSynGapCount()) % partitionNum;
//            }
//            for (int p = 0; p < partitionNum; p++) {
//                int[] range = datacenter.getStatesManager().getPartitionRangesManager().getRange((synPartitionId + p) % partitionNum);
//                for (int i = range[0]; i <= range[1]; i++) {
//                    if (synState.isSuitable(i, instance)) {
//                        suitId = i;
//                        break;
//                    }
//                }
//                if (suitId != -1) {
//                    break;
//                }
//            }
//            if (suitId != -1) {
//                synState.allocateTmpResource(suitId, instance);
//                instance.setExpectedScheduleHostId(suitId);
//                innerSchedulerResult.addScheduledInstance(instance);
//            } else {
//                innerSchedulerResult.addFailedScheduledInstance(instance);
//            }
//        }
    @Override
    protected IntraSchedulerResult scheduleInstances(List<Instance> instances, SynState synState) {
        processBeforeSchedule();
        IntraSchedulerResult intraSchedulerResult = new IntraSchedulerResult(this, getDatacenter().getSimulation().clock());

        instances.sort(new CustomComparator().reversed());

        List<Instance> sameInstance = new ArrayList<>();
        for(Instance instance : instances){
            if(!sameInstance.isEmpty() && !isSameRequestInstance(sameInstance.get(0), instance)){
                scheduleForSameInstancesToHost(sameInstance, intraSchedulerResult, synState);

                sameInstance.clear();
                sameInstance.add(instance);
            }else{
                sameInstance.add(instance);
            }
        }

        if(!sameInstance.isEmpty()){
            scheduleForSameInstancesToHost(sameInstance, intraSchedulerResult, synState);
        }

        excludeTime = excludeTimeNanos/1_000_000;
        return intraSchedulerResult;
    }

    protected void processBeforeSchedule(){
        excludeTimeNanos = 0;
        scoreHostHistoryMap.clear();
    }

    private boolean isSameRequestInstance(Instance instance1, Instance instance2){
        return instance1.getCpu() == instance2.getCpu() && instance1.getRam() == instance2.getRam() && instance1.getStorage() == instance2.getStorage() && instance1.getBw() == instance2.getBw();
    }

    private void scheduleForSameInstancesToHost(List<Instance> sameInstances, IntraSchedulerResult intraSchedulerResult, SynState synState) {
        int hostNum = datacenter.getStatesManager().getHostNum();
        int randomStartIndex = random.nextInt(hostNum);
        int scoredHostNum = Math.min(sameInstances.size() * scoredHostNumForSameInstance, hostNum);
        Instance sameInstance = sameInstances.get(0);

        ScoredHostsManager scoredHostsManager = getScoredHostsManager(sameInstance, randomStartIndex, scoredHostNum, synState);

        scheduleSameInstancesByScoredHosts(sameInstances, scoredHostsManager, intraSchedulerResult, synState);
    }

    protected ScoredHostsManager getScoredHostsManager(Instance instance, int randomStartIndex, int scoredHostNum, SynState synState){
        ScoredHostsManager scoredHostsManager = new ScoredHostsManager(new HashMap<>(Map.of(datacenter, scoreHostHistoryMap)));
        List<Integer> innerSchedulerView = getDatacenter().getStatesManager().getIntraSchedulerView(this);
        int viewSize = innerSchedulerView.get(1)-innerSchedulerView.get(0)+1;
        for(int i=0; i<viewSize; i++){
            int hostId = (randomStartIndex + i) % viewSize + innerSchedulerView.get(0);
            double score = getScoreForHost(instance, hostId, synState);
            if(score==-1){
                continue;
            }

            scoredHostsManager.addScoredHost(hostId, datacenter, score);

            if(scoredHostsManager.getScoredHostNum() >= scoredHostNum){
                break;
            }
        }
        return scoredHostsManager;
    }

    protected double getScoreForHost(Instance instance, int hostId, SynState synState){
        long startTime = System.nanoTime();
        HostState hostState = synState.getHostState(hostId);
        long endTime  = System.nanoTime();
        excludeTimeNanos += endTime - startTime;
        if (!hostState.isSuitable(instance)) {
            return -1;
        } else {
            if(scoreHostHistoryMap.containsKey(hostId)){
                return scoreHostHistoryMap.get(hostId);
            }else{
                int cpuCapacity = datacenter.getStatesManager().getHostCapacityManager().getHostCapacity(hostId)[0];
                int ramCapacity = datacenter.getStatesManager().getHostCapacityManager().getHostCapacity(hostId)[1];
                double score = (hostState.getCpu() * 10 / (double) cpuCapacity + hostState.getRam() * 10 / (double) ramCapacity) / 2;
                scoreHostHistoryMap.put(hostId, score);
                return score;
            }
        }
    }

    private void scheduleSameInstancesByScoredHosts(List<Instance> sameInstances, ScoredHostsManager scoredHostsManager, IntraSchedulerResult intraSchedulerResult, SynState synState) {
        for(Instance instance : sameInstances){
            ScoredHost scoredHost= scoredHostsManager.pollBestScoreHost();
            while (scoredHost !=null && (instance.getRetryHostIds()!=null&&instance.getRetryHostIds().contains(scoredHost.getHostId()))){
                scoredHost= scoredHostsManager.pollBestScoreHost();
            }

            if(scoredHost == null){
                intraSchedulerResult.addFailedScheduledInstance(instance);
            }else{
                int scheduledHostId = scoredHost.getHostId();
                instance.setExpectedScheduleHostId(scheduledHostId);
                intraSchedulerResult.addScheduledInstance(instance);
                synState.allocateTmpResource(scheduledHostId, instance);
                scoreHostHistoryMap.remove(scheduledHostId);

                double score = getScoreForHost(instance, scheduledHostId, synState);
                if(score!=-1){
                    scoredHostsManager.addScoredHost(scheduledHostId, datacenter, score);
                }
            }
        }
    }

    // 自定义比较器
    class CustomComparator implements Comparator<Instance> {
        @Override
        public int compare(Instance instance1, Instance instance2) {

            int result1 = instance1.getCpu() - instance2.getCpu();
            if (result1 != 0) {
                return result1;
            }

            int result2 = instance1.getRam() - instance2.getRam();
            if (result2 != 0) {
                return result2;
            }

            int result3 = instance1.getStorage() - instance2.getStorage();
            if (result3 != 0) {
                return result3;
            }

            return instance1.getBw() - instance2.getBw();
        }
    }
}