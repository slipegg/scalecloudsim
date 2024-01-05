package org.cpnsim.datacenter;

import org.cpnsim.request.InstanceGroup;
import org.cpnsim.request.UserRequest;

import java.util.List;

/**
 * An interface to be implemented by each class that represents a instanceGroup queue.
 *
 * @author Jiawen Liu
 * @since CPNSim 1.0
 */
public interface InstanceGroupQueue {
    /**
     * Add a list of userRequests to the queue.
     *
     * @param userRequests the list of userRequests to be added to the queue
     */
    InstanceGroupQueue add(List<?> userRequestsOrInstanceGroups);

    /**
     * Add a userRequest to the queue.
     *
     * @param userRequest the userRequest to be added to the queue
     */
    InstanceGroupQueue add(UserRequest userRequest);

    /**
     * Add a instanceGroup to the queue.
     *
     * @param instanceGroup the instanceGroup to be added to the queue
     */
    InstanceGroupQueue add(InstanceGroup instanceGroup);

    /**
     * Get a batch of groupInstances from the queue.
     */
    List<InstanceGroup> getBatchItem();

    List<InstanceGroup> getItems(int num);

    /**
     * Get all groupInstances from the queue.
     */
    List<InstanceGroup> getAllItem();

    /**
     * Get the size of the queue.
     */
    int size();

    /**
     * Get the number of instanceGroups to be sent in a batch.
     */
    int getBatchNum();

    /**
     * Set the number of instanceGroups to be sent in a batch.
     */
    InstanceGroupQueue setBatchNum(int batchNum);

    boolean isEmpty();
}