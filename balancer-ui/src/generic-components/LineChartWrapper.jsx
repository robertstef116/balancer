import React, { useEffect, useRef, useState } from 'react';
import { Dropdown } from 'react-bootstrap';
import { LineChart } from 'recharts';
import WidgetWrapper from './WidgetWrapper';
import { defaultRange, ranges } from '../constants';

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
        {ranges.map((rangeConfig) => <Dropdown.Item key={rangeConfig.value} onClick={() => selectRange(rangeConfig)}>{rangeConfig.text}</Dropdown.Item>)}
      </Dropdown.Menu>
    </Dropdown>
  );
}

function LineChartWrapper({
  children, title, classname, onRefresh, onRangeChanged, data, error, isLoading, dismissError,
}) {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const chartWrapperRef = useRef(null);

  const updateSize = () => {
    setSize({
      width: chartWrapperRef.current.clientWidth,
      height: chartWrapperRef.current.clientHeight,
    });
  };

  useEffect(() => {
    window.addEventListener('resize', updateSize);
    return () => {
      window.removeEventListener('resize', updateSize);
    };
  }, []);

  useEffect(() => {
    updateSize();
  }, [chartWrapperRef]);

  return (
    <WidgetWrapper
      className={classname}
      title={title}
      onRefresh={onRefresh}
      customAction={<RangeSelector onRangeChange={(rangeConfig) => onRangeChanged(rangeConfig)} />}
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
            {children}
          </LineChart>
        </div>
      </div>
    </WidgetWrapper>
  );
}

export default LineChartWrapper;
