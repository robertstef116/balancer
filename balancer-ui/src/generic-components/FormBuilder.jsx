import React from 'react';
import FormRow from './FormRow';

function FormBuilder({
  data = {}, setData, configs, changeValidity,
}) {
  const updateValidity = (valid) => {
    changeValidity(valid);
  };

  const onValuesChanged = (key, value) => {
    const newData = {
      ...data,
      ...{ [key]: value },
    };

    if (value === '') {
      delete newData[key];
    }

    setData(newData);
    let valid = true;
    for (const config of configs) {
      valid = valid && (!config.validator || config.validator(newData));
    }
    updateValidity(valid);
  };

  return (
    <div className="container-fluid">
      {configs.map((config) => (
        <FormRow
          key={config.key}
          label={config.label}
          value={data[config.key]}
          info={config.info}
          options={config.options}
          mapValueType={config.mapValueType}
          {...(config.type && { type: config.type })}
          onChange={(value) => onValuesChanged(config.key, value)}
        />
      ))}
    </div>
  );
}

export default FormBuilder;
