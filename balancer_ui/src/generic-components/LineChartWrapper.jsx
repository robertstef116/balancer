import React, { useEffect, useRef, useState } from 'react';
import { Dropdown } from 'react-bootstrap';
import { CartesianGrid, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import WidgetWrapper from './WidgetWrapper';
import { defaultRange, ranges } from '../constants';

const tickStyle = { fill: '#6264A7', fontWeight: 'bold' };

function RangeSelector({ onRangeChange }) {
  const [range, setRange] = useState(defaultRange);

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
        {ranges.map((rangeConfig) => (
          <Dropdown.Item
            key={rangeConfig.value}
            onClick={() => selectRange(rangeConfig)}
          >
            {rangeConfig.text}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
}

function LineChartWrapper({
  children,
  title,
  className,
  now,
  onRefresh,
  onRangeChanged,
  noData,
  widgetProps = { error: '' },
}) {
  const [xAxisLimits, setXAxisLimits] = useState({ min: 0, max: 0 });
  const [range, setRange] = useState(defaultRange);
  const chartWrapperRef = useRef(null);

  const updateSize = () => {
  };

  const xAxisTickFormatter = (val) => {
    switch (range.unit) {
      case 'm':
        return Math.round((now - val) / (60 * 1000));
      case 'h':
        return Math.round((now - val) / (60 * 60 * 1000));
      case 'd':
        return Math.round((now - val) / (24 * 60 * 60 * 1000));
      case 'w':
        return Math.round((now - val) / (7 * 24 * 60 * 60 * 1000));
      default:
        return val;
    }
  };

  const internalOnRangeChange = (rangeConfig) => {
    setRange(rangeConfig);
    onRangeChanged(rangeConfig);
  };

  const tooltipLabelFormatter = (val) => new Date(val).toLocaleString();

  useEffect(() => {
    window.addEventListener('resize', updateSize);
    return () => {
      window.removeEventListener('resize', updateSize);
    };
  }, []);

  useEffect(() => {
    updateSize();
  }, [chartWrapperRef.current, widgetProps.error]);

  useEffect(() => {
    setXAxisLimits({ min: now - range.value, max: now });
  }, [now, range]);

  const getChart = () => {
    if (noData) {
      return (
        <div className="text-primary text-center">No data available for selected time range</div>
      );
    }
    return (
      <ResponsiveContainer>
        <LineChart margin={{ top: 0, left: 0, right: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            type="number"
            dataKey="timeMs"
            interval="preserveStartEnd"
            domain={[xAxisLimits.min, xAxisLimits.max]}
            tickFormatter={xAxisTickFormatter}
            unit={range.unit}
            tickCount={8}
            tick={tickStyle}
          />
          <Tooltip labelFormatter={tooltipLabelFormatter} />
          <YAxis domain={['auto', 'auto']} tick={tickStyle} allowDecimals="true" />
          {children}
        </LineChart>
      </ResponsiveContainer>
    );
  };

  return (
    <WidgetWrapper
      className={className}
      title={title}
      onRefresh={onRefresh}
      customAction={onRangeChanged
        && <RangeSelector onRangeChange={(rangeConfig) => internalOnRangeChange(rangeConfig)} />}
      widgetProps={widgetProps}
    >
      <div className="pt-3 pe-3 w-100 h-100 d-flex justify-content-center align-items-center overflow-hidden">
        {getChart()}
      </div>
    </WidgetWrapper>
  );
}

export default LineChartWrapper;
