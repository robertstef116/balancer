import React, { useEffect, useState } from 'react';
import { Legend, Line } from 'recharts';
import { useSelector } from 'react-redux';
import LineChartWrapper from '../generic-components/LineChartWrapper';
import { getBalancingAnalyticsData, getWorkflows } from '../redux/actions';
import { defaultRange, GetLineColor } from '../constants';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

function BalancingHistoryChart({ classNames, workflowId, path, title, metric }) {
  const { apiWrapper, widgetProps, actionWrapper } = useWidgetUtils({ withCancellation: true });
  const [range, setRange] = useState(defaultRange);
  const [now, setNow] = useState(0);
  const [data, setData] = useState({});
  const workflows = useSelector((state) => state.workflows);

  const fetchData = async () => {
    const nowTime = Date.now();
    apiWrapper({
      action: getBalancingAnalyticsData,
      params: { durationMs: range.value * 1000 * 60 - 1, workflowId, path, metric },
      cb: (res) => {
        setNow(nowTime);
        const newData = {};
        const timeRangeData = new Set();
        for (const analytics of res) {
          timeRangeData.add(analytics.timeMs);
          if (analytics.key !== '') {
            if (!newData[analytics.key]) {
              newData[analytics.key] = {};
            }
            newData[analytics.key][analytics.timeMs] = analytics;
          }
        }

        const newDataArrays = {};
        for (const key of Object.keys(newData)) {
          newDataArrays[key] = [...Object.values(newData[key])];
          for (const timeMs of timeRangeData) {
            if (!newData[key][timeMs]) {
              newDataArrays[key].push({ key: '', data: 0, timeMs });
            }
          }
          newDataArrays[key].sort((a, b) => a.timeMs - b.timeMs);
        }
        setData(newDataArrays);
      },
    });
  };

  const geLineName = (key) => {
    const wid = key.substring(0, 36);
    const wPath = key.substring(37);
    for (const workflow of workflows) {
      if (workflow.id === wid) {
        return `${workflow.image} - /${wPath}`;
      }
    }
    return `${wid} - ${wPath}`;
  };

  useEffect(() => {
    actionWrapper({ action: getWorkflows });
  }, []);

  useEffect(() => {
    fetchData();
  }, [range, workflowId, path]);

  return (
    <LineChartWrapper
      className={classNames}
      title={title}
      onRefresh={fetchData}
      onRangeChanged={setRange}
      now={now}
      widgetProps={widgetProps}
      noData={Object.keys(data).length === 0}
    >
      {Object.keys(data).map((key) => (
        <Line
          key={key}
          data={data[key]}
          name={geLineName(key)}
          type="monotone"
          stroke={GetLineColor(key.substring(0, 36))}
          dataKey="data"
          dot={false}
        />
      ))}
      <Legend />
    </LineChartWrapper>
  );
}

export default BalancingHistoryChart;
