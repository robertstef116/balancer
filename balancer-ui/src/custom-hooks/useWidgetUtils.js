import { useState } from 'react';
import { useDispatch } from 'react-redux';

const useWidgetUtils = () => {
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const dispatch = useDispatch();

  const emitError = (err) => {
    setError(err);
  };

  const dismissError = () => {
    setError(null);
  };

  const apiWrapper = ({ action, params, cb = null }) => {
    setIsLoading(true);
    action(...params, (err, data) => {
      setIsLoading(false);
      if (err) {
        setError(err);
      } else if (cb) {
        cb(data);
      }
    });
  };

  const actionWrapper = ({ action, params = [], cb = null }) => {
    setIsLoading(true);
    dispatch(action(...params, (err) => {
      setIsLoading(false);
      if (err) {
        return setError(err);
      }
      return cb && cb();
    }));
  };

  return {
    emitError,
    actionWrapper,
    apiWrapper,
    setIsLoading,
    widgetProps: {
      error,
      isLoading,
      dismissError,
    },
  };
};

export default useWidgetUtils;
