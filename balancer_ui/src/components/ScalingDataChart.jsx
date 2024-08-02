import React, { useEffect, useState } from 'react';
import { Legend, Line } from 'recharts';
import { useSelector } from 'react-redux';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import { getAnalyticsData, getWorkflows } from '../redux/actions';
import LineChartWrapper from '../generic-components/LineChartWrapper';
import { defaultRange, GetLineColor } from '../constants';

function ScalingDataChart({
  classname, title, metric, workflowId,
}) {
  const { apiWrapper, widgetProps, actionWrapper } = useWidgetUtils({ withCancellation: true });
  const [range, setRange] = useState(defaultRange);
  const [now, setNow] = useState(0);
  const [data, setData] = useState({});
  const workflows = useSelector((state) => state.workflows);

  const fetchData = async () => {
    const nowTime = Date.now();
    apiWrapper({
      action: getAnalyticsData,
      params: { durationMs: range.value * 1000 * 60 - 1, metric, workflowId },
      cb: (res) => {
        setNow(nowTime);
        const newData = {};
        for (const analytics of res) {
          if (!newData[analytics.key]) {
            newData[analytics.key] = [];
          }
          newData[analytics.key].push(analytics);
        }
        if (newData['']) {
          for (const key of Object.keys(newData)) {
            newData[key] = [...newData[''], ...newData[key]];
            newData[key].sort((a, b) => a.timeMs - b.timeMs);
            const { length } = newData[key];
            // in case the data didn't arrive yet for the last time range slide
            if (newData[key][length - 1].data === 0 && newData[key][length - 2].data !== 0) {
              newData[key][length - 1].data = newData[key][length - 2].data;
            }
          }
          delete newData[''];
        }
        setData(newData);
      },
    });
  };

  const getWorkflowName = (wid) => {
    for (const workflow of workflows) {
      if (workflow.id === wid) {
        return workflow.image;
      }
    }
    return wid;
  };

  useEffect(() => {
    actionWrapper({ action: getWorkflows });
  }, []);

  useEffect(() => {
    fetchData();
  }, [range, workflowId]);

  return (
    <LineChartWrapper
      className={classname}
      title={title}
      onRefresh={fetchData}
      onRangeChanged={setRange}
      data={data}
      now={now}
      widgetProps={widgetProps}
      noData={Object.keys(data).length === 0}
    >
      {Object.keys(data).map((wid) => (
        <Line
          key={wid}
          data={data[wid]}
          name={getWorkflowName(wid)}
          type="monotone"
          stroke={GetLineColor(wid)}
          dataKey="data"
          dot={false}
        />
      ))}
      <Legend />
    </LineChartWrapper>
  );
}

export default ScalingDataChart;
