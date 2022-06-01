import React, { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import ConfigWidget from '../generic-components/ConfigWidget';
import { getConfigs } from '../redux/actions';

const positiveNumberValidator = (value) => {
  if (parseFloat(value) < 0) {
    return 'Value should be positive';
  }
  return null;
};

const balancingConfigs = [
  {
    label: 'Socket buffer length',
    key: 'PROCESSING_SOCKET_BUFFER_LENGTH',
    info: 'Length of load balancing redirecting buffer',
    validator: positiveNumberValidator,
  },
  {
    label: 'Health check timeout',
    key: 'HEALTH_CHECK_TIMEOUT',
    info: 'Maximum time allowed for a health check response',
    validator: positiveNumberValidator,
  },
  {
    label: 'Health check interval',
    key: 'HEALTH_CHECK_INTERVAL',
    info: 'Time between consecutive healt checks',
    validator: positiveNumberValidator,
  },
  {
    label: 'Max health check failures',
    key: 'HEALTH_CHECK_MAX_FAILURES',
    info: 'Maximum number of consecutive health check failures until the node is disabled',
    validator: positiveNumberValidator,
  },
  {
    label: 'Deployments scaling interval',
    key: 'DEPLOYMENTS_CHECK_INTERVAL',
    info: 'Interval between deployments scaling checks',
    validator: positiveNumberValidator,
  },
  {
    label: 'Configuration sync interval',
    key: 'MASTER_CHANGES_CHECK_INTERVAL',
    info: 'Interval between configuration verifications',
    validator: positiveNumberValidator,
  },
];

const algorithmConfigs = [
  {
    label: 'Recompute WRT weights',
    key: 'COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL',
    info: 'Recompute weighted response time after x requests',
    validator: positiveNumberValidator,
  },
  {
    label: 'CPU weight',
    key: 'CPU_WEIGHT',
    info: 'Weight of the CPU, between 0 and 1',
    validator: positiveNumberValidator,
  },
  {
    label: 'Memory weight',
    key: 'MEMORY_WEIGHT',
    info: 'Weight of the Memory, between 0 and 1',
    validator: positiveNumberValidator,
  },
  {
    label: 'Relevant usage data',
    key: 'NUMBER_RELEVANT_PERFORMANCE_METRICS',
    info: 'Number of consecutive nodes usage data info to take into consideration for weights computation',
    validator: positiveNumberValidator,
  },
];

function ConfigurationPage() {
  const [isLoading, setIsLoading] = useState(false);
  const configs = useSelector((state) => state.configs);
  const dispatch = useDispatch();
  const configsRef = useRef(configs);

  useEffect(() => {
    if (!configsRef.current) {
      setIsLoading(true);
      // eslint-disable-next-line no-unused-vars
      dispatch(getConfigs((err) => {
        setIsLoading(false);
      }));
    }
  }, [dispatch]);

  const cpuMemWeightValidator = (values) => {
    if (parseFloat(values.CPU_WEIGHT) + parseFloat(values.MEMORY_WEIGHT) !== 1) {
      return 'Sum of cpu and memory weights should be 1';
    }
    return null;
  };

  return (
    <div className="row mx-0 mx-md-1 mx-xl-3 mx-xxl-5">
      <ConfigWidget className="col-6 wh-1" title="Balancing configs" configs={balancingConfigs} isLoading={isLoading} />
      <ConfigWidget
        className="col-6 wh-1"
        title="Algorithms configs"
        configs={algorithmConfigs}
        isLoading={isLoading}
        validators={[cpuMemWeightValidator]}
      />
    </div>
  );
}
export default ConfigurationPage;
