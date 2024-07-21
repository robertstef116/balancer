import { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { CancelToken, isCancel } from 'axios';

const useWidgetUtils = ({ withCancellation } = { withCancellation: false }) => {
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const isActive = useRef(true);
  const dispatch = useDispatch();
  const cancellationToken = useRef();
  const loadingCount = useRef(0);

  const handleCancellation = (params) => {
    if (withCancellation) {
      if (cancellationToken.current) {
        cancellationToken.current.cancel();
      }
      cancellationToken.current = CancelToken.source();
      return {
        ...params,
        cancelToken: cancellationToken.current.token,
      };
    }
    return params;
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

  const apiWrapper = ({ action, params = {}, cb = null }) => {
    startLoading();
    const actionParams = handleCancellation(params);
    action(actionParams, (err, data) => {
      if (isActive.current) {
        stopLoading();
        if (err) {
          if (!isCancel(err)) {
            setError(err);
          }
        } else {
          setError(null);
          if (cb) {
            cb(data);
          }
        }
      }
    });
  };

  const actionWrapper = ({ action, params = {}, reload = false, cb = null }) => {
    startLoading();
    const actionParams = handleCancellation({ ...params, reload });
    dispatch(action(actionParams, (err) => {
      if (isActive.current) {
        stopLoading();
        if (err) {
          if (!isCancel(err)) {
            setError(err);
          }
        } else {
          setError(null);
          if (cb) {
            cb();
          }
        }
      }
    }));
  };

  useEffect(() => () => {
    isActive.current = false;
    if (cancellationToken.current) {
      cancellationToken.current.cancel();
    }
  }, []);

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
