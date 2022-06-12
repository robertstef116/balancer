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

    setData(newData);
    let valid = true;
    for (const config of configs) {
      valid = valid && (!config.validator || config.validator(newData));
    }
    updateValidity(valid);
  };

  return (
    <div className="container">
      {configs.map((config) => (
        <FormRow
          key={config.key}
          label={config.label}
          value={data[config.key]}
          info={config.info}
          {...(config.type && { type: config.type })}
          onChange={({ target }) => onValuesChanged(config.key, target.value)}
        />
      ))}
    </div>
  );
}

export default FormBuilder;
