import React, { useEffect, useRef, useState } from 'react';
import { Dropdown } from 'react-bootstrap';
import {
  CartesianGrid, LineChart, Tooltip, XAxis, YAxis,
} from 'recharts';
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
  children, title, className, now, onRefresh, onRangeChanged, data, error, isLoading, dismissError,
}) {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [xAxisLimits, setXAxisLimits] = useState({ min: 0, max: 0 });
  const [range, setRange] = useState(defaultRange);
  const chartWrapperRef = useRef(null);

  const updateSize = () => {
    setSize({
      width: chartWrapperRef.current.clientWidth,
      height: chartWrapperRef.current.clientHeight,
    });
  };

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

  const internalOnRangeChange = (rangeConfig) => {
    setRange(rangeConfig);
    onRangeChanged(rangeConfig);
  };

  const tooltipLabelFormatter = (val) => new Date(val * 1000).toLocaleString();

  useEffect(() => {
    window.addEventListener('resize', updateSize);
    return () => {
      window.removeEventListener('resize', updateSize);
    };
  }, []);

  useEffect(() => {
    updateSize();
  }, [chartWrapperRef.current, error]);

  useEffect(() => {
    setXAxisLimits({ min: now - range.value, max: now });
  }, [now, range]);

  return (
    <WidgetWrapper
      className={className}
      title={title}
      onRefresh={onRefresh}
      customAction={onRangeChanged && <RangeSelector onRangeChange={(rangeConfig) => internalOnRangeChange(rangeConfig)} />}
      error={error}
      isLoading={isLoading}
      dismissError={dismissError}
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
            <Tooltip labelFormatter={tooltipLabelFormatter} />
            <YAxis domain={[0, 'dataMax + 1']} tick={tickStyle} />
            {children}
          </LineChart>
        </div>
      </div>
    </WidgetWrapper>
  );
}

export default LineChartWrapper;
