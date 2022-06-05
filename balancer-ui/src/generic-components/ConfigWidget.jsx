import React, { useEffect, useState } from 'react';
import { Form } from 'react-bootstrap';
import { useSelector } from 'react-redux';
import classNames from 'classnames';
import EditableWidget from './EditableWidget';
import { saveConfigs } from '../redux/actions';
import useWidgetUtils from '../custom-hooks/useWidgetUtils';

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
      params: [configValues],
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
      {...widgetProps}
    >
      <div className="container">
        {configs.map((config) => (
          <div className="row pt-3" key={config.key}>
            <div className="col-5">
              <Form.Label>{config.label}</Form.Label>
            </div>
            <div className={classNames('col-7', { 'has-error': !!errors[config.key] })}>
              <div className="d-flex align-items-center">
                <Form.Control
                  className="w-100"
                  type="text"
                  value={configValues[config.key] || ''}
                  onChange={({ target }) => onValueChanged(config.key, target.value)}
                />
                <i className="bi bi-info-circle text-primary ms-2" title={config.info} />
              </div>
              <div className="input-error">
                {errors[config.key]}
              </div>
            </div>
          </div>
        ))}
      </div>
    </EditableWidget>
  );
}

export default ConfigWidget;
