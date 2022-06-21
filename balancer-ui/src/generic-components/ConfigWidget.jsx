import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import EditableWidget from './EditableWidget';
import { saveConfigs } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';
import FormRow from './FormRow';

function ConfigWidget({
  className, title, configs, isLoading: _isLoading, validators = [],
}) {
  const {
    widgetProps, emitError, actionWrapper, setIsLoading,
  } = useWidgetUtils();
  const savedConfigs = useSelector((state) => state.configs);
  const [configValues, setConfigValues] = useState({});
  const [valuesChanged, setValuesChanged] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    setIsLoading(_isLoading);
  }, [_isLoading]);

  useEffect(() => {
    if (!valuesChanged && savedConfigs) {
      const newConfigValues = {};
      configs.forEach((config) => {
        newConfigValues[config.key] = savedConfigs[config.key];
      });
      setConfigValues(newConfigValues);
    }
  }, [configs, savedConfigs, valuesChanged]);

  const onValueChanged = (key, value) => {
    setErrors({});
    setValuesChanged(true);
    setConfigValues({
      ...configValues,
      ...{ [key]: value },
    });
  };

  const onSave = () => {
    widgetProps.dismissError();

    for (const validator of validators) {
      const msg = validator(configValues);
      if (msg) {
        emitError(msg);
        return;
      }
    }

    const newErrors = {};

    for (const inputConfig of configs) {
      const validationError = inputConfig.validator(configValues[inputConfig.key]);
      if (validationError) {
        newErrors[inputConfig.key] = validationError;
      }
    }

    if (Object.keys(newErrors).length !== 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    actionWrapper({
      action: saveConfigs,
      params: { configs: configValues },
      cb: () => {
        setValuesChanged(false);
      },
    });
  };

  return (
    <EditableWidget
      title={title}
      changed={valuesChanged}
      onSave={onSave}
      className={className}
      widgetProps={widgetProps}
    >
      <div className="container">
        {configs.map((config) => (
          <FormRow
            key={config.key}
            label={config.label}
            error={errors[config.key]}
            value={configValues[config.key]}
            info={config.info}
            onChange={(value) => onValueChanged(config.key, value)}
          />
        ))}
      </div>
    </EditableWidget>
  );
}

export default ConfigWidget;
