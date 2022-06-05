import React, { useEffect, useState } from 'react';
import {
  CartesianGrid, Line, Tooltip, XAxis, YAxis,
} from 'recharts';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import { getAnalyticsData } from '../redux/actions';
import LineChartWrapper from '../generic-components/LineChartWrapper';
import { defaultRange } from '../constants';

const tickStyle = { fill: '#6264A7', fontWeight: 'bold' };

// https://recharts.org/en-US/api/Line
function UsageChart({
  classname, workerId, workflowId, deploymentId,
}) {
  const { apiWrapper, widgetProps } = useWidgetUtils();
  const [xAxisLimits, setXAxisLimits] = useState({ min: 0, max: 0 });
  const [range, setRange] = useState(defaultRange);
  const [now, setNow] = useState(0);
  const [data, setData] = useState([]);

  const fetchData = async () => {
    const nowTime = Math.round(Date.now() / 1000);
    apiWrapper({
      action: getAnalyticsData,
      params: [Math.round(nowTime - range.value) - 1, workerId, workflowId, deploymentId],
      cb: (res) => {
        setNow(nowTime);
        setData(res);
        setXAxisLimits({ min: nowTime - range.value, max: nowTime });
      },
    });
  };

  const tooltipLabelFormatter = (val) => new Date(val * 1000).toLocaleString();

  const xAxisTickFormatter = (val) => {
    switch (range.unit) {
      case 'm':
        return Math.round((now - val) / 60);
      case 'h':
        return Math.round((now - val) / (60 * 60));
      case 'd':
        return Math.round((now - val) / (24 * 60 * 60));
      case 'w':
        return Math.round((now - val) / (7 * 24 * 60 * 60));
      default:
        return val;
    }
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
      {...widgetProps}
    >
      <CartesianGrid strokeDasharray="3 3" />
      <Tooltip labelFormatter={tooltipLabelFormatter} />
      <XAxis
        type="number"
        dataKey="timestamp"
        interval="preserveStartEnd"
        domain={[xAxisLimits.min, xAxisLimits.max]}
        tickFormatter={xAxisTickFormatter}
        unit={range.unit}
        tickCount={6}
        tick={tickStyle}

      />
      <YAxis domain={[0, 'dataMax']} tick={tickStyle} />
      <Line name="Requests" type="monotone" dataKey="value" stroke="#FC7F03" dot={false} />
    </LineChartWrapper>
  );
}

export default UsageChart;
