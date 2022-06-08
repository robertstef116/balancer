import { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { CancelToken, isCancel } from 'axios';

const useWidgetUtils = ({ withCancellation } = { withCancellation: false }) => {
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const dispatch = useDispatch();
  const cancellationToken = useRef();
  const loadingCount = useRef(0);

  const handleCancellation = (params) => {
    if (withCancellation) {
      if (cancellationToken.current) {
        cancellationToken.current.cancel();
      }
      cancellationToken.current = CancelToken.source();
      params.push(cancellationToken.current.token);
    }
  };

  const startLoading = () => {
    loadingCount.current++;
    setIsLoading(true);
  };

  const stopLoading = () => {
    loadingCount.current--;
    if (loadingCount.current === 0) {
      setIsLoading(false);
    }
  };

  const emitError = (err) => {
    setError(err);
  };

  const dismissError = () => {
    setError(null);
  };

  const apiWrapper = ({ action, params, cb = null }) => {
    startLoading();
    handleCancellation(params);
    action(...params, (err, data) => {
      stopLoading();
      if (err) {
        if (isCancel(err)) {
          return;
        }
        setError(err);
      } else if (cb) {
        setError(null);
        cb(data);
      }
    });
  };

  const actionWrapper = ({ action, params = [], cb = null }) => {
    startLoading();
    handleCancellation(params);
    dispatch(action(...params, (err) => {
      stopLoading();
      if (err) {
        if (isCancel(err)) {
          return;
        }
        setError(err);
      } else if (cb) {
        cb();
      }
    }));
  };

  if (withCancellation) {
    useEffect(() => () => {
      if (cancellationToken.current) {
        cancellationToken.current.cancel();
      }
    }, []);
  }

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
