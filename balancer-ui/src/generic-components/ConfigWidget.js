import React, {useEffect, useState} from 'react';
import EditableWidget from "../generic-components/EditableWidget";
import {Form} from 'react-bootstrap';
import {useSelector} from "react-redux";
import {saveConfigs} from "../redux/actions";
import useWidgetUtils from "../utils/useWidgetUtils";

const ConfigWidget = ({className, title, configs, isLoading: _isLoading, validator}) => {
  const {widgetProps, emitError, actionWrapper, setIsLoading} = useWidgetUtils();
  const savedConfigs = useSelector(state => state.configs);
  const [configValues, setConfigValues] = useState({});
  const [valuesChanged, setValuesChanged] = useState(false);

  useEffect(() => {
    setIsLoading(_isLoading);
  }, [_isLoading, setIsLoading]);

  useEffect(() => {
    if (!valuesChanged && savedConfigs) {
      const configValues = {};
      for(const config of configs) {
        configValues[config.key] = savedConfigs[config.key];
      }
      setConfigValues(configValues);
    }
  }, [configs, savedConfigs, valuesChanged])

  const onValueChanged = (key, value) => {
    setValuesChanged(true);
    setConfigValues({
      ...configValues,
      ...{[key]: value}
    })
  }

  const onSave = () => {
    if (!validator || validator(configValues)) {
      setIsLoading(true);
      actionWrapper({action: saveConfigs, params: [savedConfigs], cb: () => {
        setValuesChanged(false);
      }});
    } else {
      emitError('Invalid configurations!');
      // invalid
    }
  }

  return <EditableWidget title={title} changed={valuesChanged} onSave={onSave} className={className} {...widgetProps}>
    <div className='container'>
      {configs.map(config => <div className='row pt-3' key={config.key}>
          <div className='col-5'>
            <Form.Label>{config.label}</Form.Label>
          </div>
          <div className='col-7 d-flex align-items-center'>
            <Form.Control className='w-100' type='text' value={configValues[config.key]||''}
                          onChange={({target}) => onValueChanged(config.key, target.value)}/>
            <i className='bi bi-info-circle text-primary ms-2' title={config.info}></i>
          </div>
        </div>
      )}
    </div>
  </EditableWidget>
};

export default ConfigWidget;
