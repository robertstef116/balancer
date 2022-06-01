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

  const actionWrapper = ({ action, params = [], cb }) => {
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
    setIsLoading,
    widgetProps: {
      error,
      isLoading,
      dismissError,
    },
  };
};

export default useWidgetUtils;
