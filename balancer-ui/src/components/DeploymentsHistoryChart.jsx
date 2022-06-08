import React, { useEffect, useState } from 'react';
import { Line } from 'recharts';
import LineChartWrapper from '../generic-components/LineChartWrapper';
import { getWorkflowAnalyticsData } from '../redux/actions';
import { defaultRange } from '../constants';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

function DeploymentsHistoryChart({ classNames, workerId, workflowId }) {
  const { apiWrapper, widgetProps } = useWidgetUtils({ withCancellation: true });
  const [range, setRange] = useState(defaultRange);
  const [now, setNow] = useState(0);
  const [data, setData] = useState([]);
  const [workflowMapping, setWorkflowMapping] = useState({});

  const squashData = (nowTime, { analyticsData, currentScaling }) => {
    const _currentScaling = {};
    const _workflowMapping = {};
    for (const wid of Object.keys(currentScaling)) {
      _workflowMapping[wid] = currentScaling[wid].image;
      _currentScaling[wid] = currentScaling[wid].numberOfDeployments;
    }

    const transformedData = [];
    transformedData.unshift({
      timestamp: nowTime,
      ..._currentScaling,
    });
    if (analyticsData.length) {
      transformedData.unshift({
        timestamp: analyticsData[analyticsData.length - 1].timestamp + 1,
        ..._currentScaling,
      });
    }
    for (const entity of analyticsData.reverse()) {
      _currentScaling[entity.workflowId] = entity.numberOfDeployments;
      _workflowMapping[entity.workflowId] = entity.image;
      transformedData.unshift({
        timestamp: entity.timestamp,
        ..._currentScaling,
      });
    }
    transformedData.unshift({
      timestamp: nowTime - range.value,
      ..._currentScaling,
    });

    if (workflowId) {
      setWorkflowMapping({ [workflowId]: _workflowMapping[workflowId] });
    } else {
      setWorkflowMapping(_workflowMapping);
    }

    setData(transformedData);
  };

  const fetchData = async () => {
    const nowTime = Math.round(Date.now() / 1000);
    apiWrapper({
      action: getWorkflowAnalyticsData,
      params: [Math.round(nowTime - range.value) - 1, workerId, workflowId],
      cb: (res) => {
        setNow(nowTime);
        squashData(nowTime, res);
      },
    });
  };

  useEffect(() => {
    fetchData();
  }, [range, workerId, workflowId]);

  return (
    <LineChartWrapper
      className={classNames}
      title="Workflow history"
      onRefresh={fetchData}
      onRangeChanged={setRange}
      data={data}
      now={now}
      {...widgetProps}
    >
      {Object.keys(workflowMapping).map((wid) => <Line key={wid} name={workflowMapping[wid]} type="monotone" dataKey={wid} dot={false} />)}
    </LineChartWrapper>
  );
}

export default DeploymentsHistoryChart;
