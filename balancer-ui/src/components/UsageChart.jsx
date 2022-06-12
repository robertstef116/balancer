import React, { useEffect, useState } from 'react';
import { Line } from 'recharts';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import { getAnalyticsData } from '../redux/actions';
import LineChartWrapper from '../generic-components/LineChartWrapper';
import { defaultRange } from '../constants';

// https://recharts.org/en-US/api/Line
function UsageChart({
  classname, workerId, workflowId, deploymentId,
}) {
  const { apiWrapper, widgetProps } = useWidgetUtils({ withCancellation: true });
  const [range, setRange] = useState(defaultRange);
  const [now, setNow] = useState(0);
  const [data, setData] = useState([]);

  const fetchData = async () => {
    const nowTime = Math.round(Date.now() / 1000);
    apiWrapper({
      action: getAnalyticsData,
      params: { from: Math.round(nowTime - range.value) - 1, workerId, workflowId, deploymentId },
      cb: (res) => {
        setNow(nowTime);
        setData(res);
      },
    });
  };

  useEffect(() => {
    fetchData();
  }, [range, workerId, workflowId, deploymentId]);

  return (
    <LineChartWrapper
      className={classname}
      title="System usage"
      onRefresh={fetchData}
      onRangeChanged={setRange}
      data={data}
      now={now}
      {...widgetProps}
    >
      <Line name="Requests" type="monotone" dataKey="value" stroke="#FC7F03" dot={false} />
    </LineChartWrapper>
  );
}

export default UsageChart;
