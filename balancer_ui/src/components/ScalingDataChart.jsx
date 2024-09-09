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

  const fetchData = ({ showLoadingIndicator = true } = {}) => new Promise((resolve) => {
    const nowTime = Date.now();
    apiWrapper({
      action: getAnalyticsData,
      params: { durationMs: range.value * 1000 * 60 - 1, metric, workflowId },
      showLoadingIndicator,
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
          const { length } = newDataArrays[key];
          // in case the data didn't arrive yet for the last time range slide
          if (newDataArrays[key][length - 1].data === 0 && newDataArrays[key][length - 2].data !== 0) {
            newDataArrays[key][length - 1].data = newDataArrays[key][length - 2].data;
          }
        }
        setData(newDataArrays);
        resolve();
      },
    });
  });

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
    let refreshTimer;
    const refresh = () => {
      refreshTimer = setTimeout(() => {
        fetchData({ showLoadingIndicator: false }).then(() => {
          refresh();
        });
      }, 15000);
    };

    fetchData().then(() => {
      refresh();
    });

    return () => {
      clearTimeout(refreshTimer);
    };
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
      {workflows && Object.keys(data).map((wid) => (
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
