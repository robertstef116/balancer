import React, {useEffect, useRef, useState} from 'react';import ConfigWidget from "../generic-components/ConfigWidget";import {useDispatch, useSelector} from "react-redux";import {getConfigs} from "../redux/actions";const balancingConfigs = [  {    label: 'Socket buffer length',    key: 'PROCESSING_SOCKET_BUFFER_LENGTH',    info: 'Length of load balancing redirecting buffer'  },  {    label: 'Health check timeout',    key: 'HEALTH_CHECK_TIMEOUT',    info: 'Maximum time allowed for a health check response'  },  {    label: 'Health check interval',    key: 'HEALTH_CHECK_INTERVAL',    info: 'Time between consecutive healt checks'  },  {    label: 'Max health check failures',    key: 'HEALTH_CHECK_MAX_FAILURES',    info: 'Maximum number of consecutive health check failures until the node is disabled'  },  {    label: 'Deployments scaling interval',    key: 'DEPLOYMENTS_CHECK_INTERVAL',    info: 'Interval between deployments scaling checks'  },  {    label: 'Configuration sync interval',    key: 'MASTER_CHANGES_CHECK_INTERVAL',    info: 'Interval between configuration verifications'  }];const algorithmConfigs = [  {    label: 'Recompute WRT weights',    key: 'COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL',    info: 'Recompute weighted response time after x requests'  },  {    label: 'CPU weight',    key: 'CPU_WEIGHT',    info: 'Weight of the CPU, between 0 and 1'  },  {    label: 'Memory weight',    key: 'MEMORY_WEIGHT',    info: 'Weight of the Memory, between 0 and 1'  },  {    label: 'Relevant usage data',    key: 'NUMBER_RELEVANT_PERFORMANCE_METRICS',    info: 'Number of consecutive nodes usage data info to take into consideration for weights computation'  }];const ConfigurationPage = () => {  const [isLoading, setIsLoading] = useState(false);  const configs = useSelector(state => state.configs);  const dispatch = useDispatch();  const configsRef = useRef(configs);  useEffect(() => {    if (!configsRef.current) {      setIsLoading(true);      dispatch(getConfigs((err) => {        setTimeout(() => {          setIsLoading(false);        }, 2000)      }))    }  }, [dispatch])  return <>    <div className='row mx-0 mx-md-1 mx-xl-3 mx-xxl-5'>      <ConfigWidget className='col-6 wh-1' title='Balancing configs' configs={balancingConfigs} isLoading={isLoading}/>      <ConfigWidget className='col-6 wh-1' title='Algorithms configs' configs={algorithmConfigs} isLoading={isLoading}/>    </div>  </>};export default ConfigurationPage;