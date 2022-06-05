import React, { useEffect, useRef, useState } from 'react';
import {
  CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis,
} from 'recharts';
import { Dropdown } from 'react-bootstrap';
import WidgetWrapper from '../generic-components/WidgetWrapper';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import { getAnalyticsData } from '../redux/actions';

const tickStyle = { fill: '#6264A7', fontWeight: 'bold' };

const ranges = [
  { text: 'Last 10 min', value: 10 * 60, unit: 'm' },
  { text: 'Last 30 min', value: 30 * 60, unit: 'm' },
  { text: 'Last hour', value: 60 * 60, unit: 'm' },
  { text: 'Last 3 hour', value: 3 * 60 * 60, unit: 'm' },
  { text: 'Last 6 hour', value: 6 * 60 * 60, unit: 'h' },
  { text: 'Last 12 hour', value: 12 * 60 * 60, unit: 'h' },
  { text: 'Last day', value: 24 * 60 * 60, unit: 'h' },
  { text: 'Last week', value: 7 * 24 * 60 * 60, unit: 'd' },
  { text: 'Last month', value: 30 * 24 * 60 * 60, unit: 'w' },
];

function RangeSelector({ onRangeChange }) {
  const [range, setRange] = useState(ranges[2]);

  const selectRange = (rangeConfig) => {
    setRange(rangeConfig);
    onRangeChange(rangeConfig);
  };

  return (
    <Dropdown className="ps-1">
      <Dropdown.Toggle>
        {range.text}
      </Dropdown.Toggle>

      <Dropdown.Menu>
        {ranges.map((rangeConfig) => <Dropdown.Item key={rangeConfig.value} onClick={() => selectRange(rangeConfig)}>{rangeConfig.text}</Dropdown.Item>)}
      </Dropdown.Menu>
    </Dropdown>
  );
}

// https://recharts.org/en-US/api/Line
function UsageChart({
  classname, workerId, workflowId, deploymentId,
}) {
  const { apiWrapper, widgetProps } = useWidgetUtils();
  const chartWrapperRef = useRef(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [xAxisLimits, setXAxisLimits] = useState({ min: 0, max: 0 });
  const [range, setRange] = useState(ranges[2]);
  const [now, setNow] = useState(0);
  const [data, setData] = useState([]);

  const updateSize = () => {
    setSize({
      width: chartWrapperRef.current.clientWidth,
      height: chartWrapperRef.current.clientHeight,
    });
  };

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
    updateSize();
  }, [chartWrapperRef.current]);

  useEffect(() => {
    fetchData();
  }, [range, workerId, workflowId, deploymentId]);

  useEffect(() => {
    const listener = window.addEventListener('resize', updateSize);
    return () => {
      window.removeEventListener('resize', listener);
    };
  }, []);

  return (
    <WidgetWrapper
      className={classname}
      title="System usage"
      onRefresh={fetchData}
      customAction={<RangeSelector onRangeChange={(rangeConfig) => setRange(rangeConfig)} />}
      {...widgetProps}
    >
      <div className="pt-3 pe-3 w-100 h-100">
        <div className="w-100 h-100" ref={chartWrapperRef}>
          <LineChart
            width={size.width}
            height={size.height}
            margin={{
              top: 0, left: 0, right: 0, bottom: 0,
            }}
            data={data}
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
          </LineChart>
        </div>
      </div>
    </WidgetWrapper>
  );
}

export default UsageChart;
